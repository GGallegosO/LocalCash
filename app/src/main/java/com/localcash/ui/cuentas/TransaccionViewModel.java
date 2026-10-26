package com.localcash.ui.cuentas;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Cuenta;
import com.localcash.data.repository.GastoCompartidoRepository;
import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.EstadoLugar;
import com.localcash.domain.model.EstadoReparto;
import com.localcash.domain.model.Lugar;
import com.localcash.domain.model.ModoDivision;
import com.localcash.domain.model.PartePropuesta;
import com.localcash.domain.model.TipoLugar;
import com.localcash.domain.model.TipoTransaccion;
import com.localcash.domain.model.Ubicacion;
import com.localcash.domain.service.GastoCompartidoService;
import com.localcash.domain.service.LugarService;
import com.localcash.domain.service.TransaccionService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel de la Vista 2 (Transacción): registra movimientos (ingreso/egreso)
 * aplicando la lógica de negocio y persistiendo el cambio de saldo.
 * <p>
 * <b>Arquitectura (`.agent.md` §5.1):</b> este ViewModel solo orquesta. La
 * validación de las reglas de negocio (monto &gt; 0, cuenta existente, saldo
 * suficiente) vive en {@link TransaccionService} ({@code domain/}), y la
 * persistencia en Room se ejecuta siempre fuera del hilo principal mediante el
 * {@code Executor} de un solo hilo de {@link AppDatabase} (serializa las
 * escrituras y evita la condición de carrera leer-restar-escribir del saldo).
 * </p>
 * <p>
 * <b>Errores (`.agent.md` §5.4):</b> el ViewModel no produce texto de usuario.
 * {@link #registrarTransaccion(long, String, long, String)} devuelve un
 * {@link LiveData} con el {@link ErrorCode} del fallo, o {@code null} si el
 * movimiento se registró correctamente. La capa {@code ui/} traduce ese código
 * a un mensaje.
 * </p>
 */
public class TransaccionViewModel extends AndroidViewModel {

    /** Etiqueta de logs del ViewModel. */
    private static final String TAG = "TransaccionViewModel";

    /**
     * Cuentas activas observables ({@code WHERE activa = 1}, ya filtrado por
     * {@code CuentaDao.getAll()}), usadas para llenar el selector de cuentas.
     */
    private final LiveData<List<Cuenta>> cuentasActivas;

    /** Repositorio de partes compartidas: único punto de escritura del cobro (I-16). */
    private final GastoCompartidoRepository gastoCompartidoRepository;

    /**
     * Construye el ViewModel y lo enlaza con Room.
     *
     * @param application Aplicación usada para obtener la base de datos
     */
    public TransaccionViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        this.cuentasActivas = db.cuentaDao().getAll();
        this.gastoCompartidoRepository = new GastoCompartidoRepository(db);
    }

    /**
     * Observa las cuentas activas de la base de datos, para poblar el selector.
     *
     * @return LiveData con la lista de cuentas activas (entidades Room)
     */
    public LiveData<List<Cuenta>> getCuentasActivas() {
        return cuentasActivas;
    }

    // ------------------------------------------------------------------
    // Estado del reparto compartido (Feature 012, T11/T12)
    // ------------------------------------------------------------------

    /**
     * Estado de la pantalla de registro: si el egreso es compartido, con qué
     * contactos, en qué modo y con qué montos personalizados. Lo manipula solo
     * el hilo de UI (los setters usan {@code setValue}); el cálculo del reparto
     * y su validación delegan en {@link GastoCompartidoService}, nunca en la
     * Activity (`.agent.md` §5.1).
     */
    private boolean compartido;

    /** Modo de división elegido (IGUAL por defecto). */
    private ModoDivision modoDivision = ModoDivision.IGUAL;

    /** Monto del egreso capturado en el formulario, en pesos CLP. */
    private long montoTotal;

    /** Contactos elegidos, en el orden en que se marcaron. */
    private final List<Long> contactosSeleccionados = new ArrayList<>();

    /** Montos de la división personalizada por contacto, en pesos CLP. */
    private final Map<Long, Long> montosPersonalizados = new LinkedHashMap<>();

    /** Controla la visibilidad del bloque de división en la hoja inferior. */
    private final MutableLiveData<Boolean> divisionVisible = new MutableLiveData<>(Boolean.FALSE);

    /** Suma en vivo y veredicto del dominio sobre el reparto actual. */
    private final MutableLiveData<EstadoReparto> estadoReparto =
            new MutableLiveData<>(EstadoReparto.sinReparto(0L));

    /**
     * Observa si el bloque de división debe estar visible.
     *
     * @return LiveData {@code true} cuando el egreso está marcado compartido
     */
    public LiveData<Boolean> getDivisionVisible() {
        return divisionVisible;
    }

    /**
     * Observa el estado del reparto (suma de partes contra el total y veredicto
     * del dominio) para pintar la suma en vivo y habilitar "Registrar".
     *
     * @return LiveData con el {@link EstadoReparto} actual
     */
    public LiveData<EstadoReparto> getEstadoReparto() {
        return estadoReparto;
    }

    /**
     * Marca o desmarca el egreso como compartido.
     *
     * @param activo {@code true} para mostrar y validar el bloque de división
     */
    public void setCompartido(boolean activo) {
        this.compartido = activo;
        notificarEstado();
    }

    /**
     * @return {@code true} si el egreso en curso está marcado compartido
     */
    public boolean isCompartido() {
        return compartido;
    }

    /**
     * Actualiza el total del egreso capturado en el formulario.
     *
     * @param montoTotal importe en pesos CLP
     */
    public void setMontoTotal(long montoTotal) {
        this.montoTotal = montoTotal;
        notificarEstado();
    }

    /**
     * Cambia el modo de división elegido por el usuario.
     *
     * @param modo {@link ModoDivision#IGUAL} o {@link ModoDivision#PERSONALIZADA}
     */
    public void setModoDivision(@NonNull ModoDivision modo) {
        this.modoDivision = modo;
        notificarEstado();
    }

    /**
     * @return modo de división actual
     */
    @NonNull
    public ModoDivision getModoDivision() {
        return modoDivision;
    }

    /**
     * Reemplaza los contactos elegidos para el reparto, conservando los montos
     * personalizados de los que siguen seleccionados.
     *
     * @param ids contactos elegidos, en orden
     */
    public void setContactosSeleccionados(@NonNull List<Long> ids) {
        this.contactosSeleccionados.clear();
        this.contactosSeleccionados.addAll(ids);
        this.montosPersonalizados.keySet().retainAll(this.contactosSeleccionados);
        notificarEstado();
    }

    /**
     * @return copia de los contactos elegidos, en orden
     */
    @NonNull
    public List<Long> getContactosSeleccionados() {
        return new ArrayList<>(this.contactosSeleccionados);
    }

    /**
     * Registra el monto escrito para un contacto en la división personalizada.
     *
     * @param contactoId id del contacto
     * @param monto      importe en pesos CLP (0 si el campo está vacío)
     */
    public void setMontoPersonalizado(long contactoId, long monto) {
        this.montosPersonalizados.put(contactoId, monto);
        notificarEstado();
    }

    /**
     * @param contactoId id del contacto
     * @return monto capturado para ese contacto, o 0 si todavía no escribió
     */
    public long getMontoPersonalizado(long contactoId) {
        Long monto = this.montosPersonalizados.get(contactoId);
        return monto == null ? 0L : monto;
    }

    /**
     * Devuelve el estado de división al abrir una hoja de movimiento nueva.
     */
    public void reiniciarDivision() {
        this.compartido = false;
        this.modoDivision = ModoDivision.IGUAL;
        this.montoTotal = 0L;
        this.contactosSeleccionados.clear();
        this.montosPersonalizados.clear();
        notificarEstado();
    }

    /**
     * Valida el reparto tal como está en pantalla.
     *
     * @return {@code null} si es válido (incluye "no compartido"), o el
     *         {@link ErrorCode} que lo rechaza
     */
    public ErrorCode validarReparto() {
        return estadoReparto.getValue() == null ? null : estadoReparto.getValue().getError();
    }

    /**
     * Partes listas para persistir junto con el egreso.
     *
     * @return {@code null} si el movimiento no es compartido; de lo contrario,
     *         las partes calculadas/validadas por el dominio (lista vacía si el
     *         reparto aún no es válido)
     */
    public List<PartePropuesta> partesParaRegistrar() {
        if (!compartido) {
            return null;
        }
        EstadoReparto estado = estadoReparto.getValue();
        return estado == null
                ? new ArrayList<PartePropuesta>()
                : new ArrayList<>(estado.getPartes());
    }

    /**
     * Recalcula el estado y publica visibilidad + suma en vivo.
     */
    private void notificarEstado() {
        divisionVisible.setValue(compartido);
        estadoReparto.setValue(calcularEstado());
    }

    /**
     * Aplica el servicio de dominio al estado de pantalla. No lanza: el rechazo
     * viaja dentro del {@link EstadoReparto} como {@link ErrorCode}.
     *
     * @return estado válido o rechazado del reparto actual
     */
    private EstadoReparto calcularEstado() {
        if (!compartido) {
            return EstadoReparto.sinReparto(montoTotal);
        }

        if (modoDivision == ModoDivision.IGUAL) {
            try {
                // N+1: el usuario es un actor implícito y su parte nace pagada.
                List<PartePropuesta> partes = GastoCompartidoService.dividirConUsuario(
                        montoTotal, contactosSeleccionados);
                return EstadoReparto.validoCon(montoTotal, ModoDivision.IGUAL, partes);
            } catch (BusinessRuleException e) {
                return EstadoReparto.rechazado(montoTotal, ModoDivision.IGUAL,
                        new ArrayList<PartePropuesta>(), 0L, e.getErrorCode());
            }
        }

        List<PartePropuesta> partes = new ArrayList<>(contactosSeleccionados.size());
        for (Long contactoId : contactosSeleccionados) {
            partes.add(new PartePropuesta(contactoId, getMontoPersonalizado(contactoId)));
        }
        long suma = 0L;
        for (PartePropuesta parte : partes) {
            suma += parte.getMontoAsignado();
        }
        try {
            GastoCompartidoService.validarPersonalizado(montoTotal, partes);
            return EstadoReparto.validoCon(montoTotal, ModoDivision.PERSONALIZADA, partes);
        } catch (BusinessRuleException e) {
            return EstadoReparto.rechazado(montoTotal, ModoDivision.PERSONALIZADA,
                    partes, suma, e.getErrorCode());
        }
    }

    /**
     * Registra un movimiento (ingreso o egreso) sobre una cuenta, en un hilo
     * secundario.
     * <p>
     * Flujo:
     * </p>
     * <ol>
     *   <li>Carga la cuenta por id; si no existe,
     *       {@link ErrorCode#CUENTA_NO_ENCONTRADA}.</li>
     *   <li>Construye el modelo de dominio del movimiento y valida con
     *       {@link TransaccionService#validarTransaccion} (propaga su
     *       {@link ErrorCode} si falla).</li>
     *   <li>Si es válido: inserta el movimiento y actualiza el saldo con el
     *       valor calculado por {@link TransaccionService#calcularNuevoSaldo}.</li>
     * </ol>
     * <p>
     * El {@link LiveData} devuelto emite <b>{@code null} en caso de éxito</b> o
     * el {@link ErrorCode} del fallo. La UI decide cómo traducirlo a texto.
     * </p>
     *
     * @param cuentaId id de la cuenta afectada
     * @param tipo     tipo de movimiento ({@code INGRESO} o {@code EGRESO},
     *                 como {@code name()} del enum {@link TipoTransaccion})
     * @param monto    importe en pesos CLP (positivo; sin centavos)
     * @param concepto concepto libre del movimiento
     * @return LiveData con {@code null} si se registró, o el {@link ErrorCode} del fallo
     */
    public LiveData<ErrorCode> registrarTransaccion(long cuentaId, String tipo,
                                                    long monto, String concepto) {
        return registrarTransaccion(cuentaId, tipo, monto, concepto, null, null, null);
    }

    /**
     * Registra un movimiento y, si trae coordenadas, crea su pin vinculado en
     * la misma transacción de base de datos (T25).
     * <p>
     * El pin hereda del movimiento el nombre (concepto) y la ubicación; nace
     * abierto, de tipo {@link com.localcash.domain.model.TipoLugar#OTRO} y sin
     * lista de cosas. Si cualquier escritura falla, Room revierte todo: no
     * queda ni el movimiento ni un pin a medias (CA-3). Sin coordenadas no se
     * crea pin y el movimiento se guarda normal (CA-4).
     * </p>
     *
     * @param cuentaId       id de la cuenta afectada
     * @param tipo           tipo de movimiento ({@code INGRESO} o {@code EGRESO})
     * @param monto          importe en pesos CLP (positivo)
     * @param concepto       concepto libre del movimiento
     * @param latitud        latitud WGS84 o {@code null} si no hay ubicación
     * @param longitud       longitud WGS84 o {@code null} si no hay ubicación
     * @param direccionLugar referencia textual opcional del punto
     * @return LiveData con {@code null} si se registró, o el {@link ErrorCode} del fallo
     */
    public LiveData<ErrorCode> registrarTransaccion(long cuentaId, String tipo,
                                                    long monto, String concepto,
                                                    Double latitud, Double longitud,
                                                    String direccionLugar) {
        return registrarTransaccion(cuentaId, tipo, monto, concepto,
                latitud, longitud, direccionLugar, null);
    }

    /**
     * Registra un movimiento sin geolocalización que puede ser un egreso
     * compartido (Feature 012).
     *
     * @param cuentaId          id de la cuenta afectada
     * @param tipo              tipo de movimiento ({@code INGRESO} o {@code EGRESO})
     * @param monto             importe en pesos CLP (positivo)
     * @param concepto          concepto libre del movimiento
     * @param partesCompartidas partes del reparto, o {@code null} si el
     *                          movimiento no es compartido
     * @return LiveData con {@code null} si se registró, o el {@link ErrorCode} del fallo
     */
    public LiveData<ErrorCode> registrarTransaccion(long cuentaId, String tipo,
                                                    long monto, String concepto,
                                                    List<PartePropuesta> partesCompartidas) {
        return registrarTransaccion(cuentaId, tipo, monto, concepto,
                null, null, null, partesCompartidas);
    }

    /**
     * Registra un movimiento, su pin opcional y —si es un egreso compartido—
     * sus partes, todo en una sola transacción de base de datos.
     * <p>
     * Las partes se validan <b>antes</b> de abrir la transacción con
     * {@link GastoCompartidoService#validarReparto} (INV-1, contactos sin
     * repetir) y {@link GastoCompartidoService#validarPersonalizado} (INV-2,
     * suma exacta y montos positivos). Si la validación o cualquier escritura
     * falla, Room revierte el bloque completo: no queda ni el egreso, ni las
     * partes, ni el saldo modificado (todo o nada).
     * </p>
     * <p>
     * Solo un {@code EGRESO} puede compartirse; un ingreso con partes se
     * rechaza con {@link ErrorCode#GASTO_COMPARTIDO_INVALIDO}.
     * </p>
     *
     * @param cuentaId          id de la cuenta afectada
     * @param tipo              tipo de movimiento ({@code INGRESO} o {@code EGRESO})
     * @param monto             importe en pesos CLP (positivo)
     * @param concepto          concepto libre del movimiento
     * @param latitud           latitud WGS84 o {@code null} si no hay ubicación
     * @param longitud          longitud WGS84 o {@code null} si no hay ubicación
     * @param direccionLugar    referencia textual opcional del punto
     * @param partesCompartidas partes del reparto ({@code null} = no compartido;
     *                          lista vacía = compartido sin contactos, se rechaza)
     * @return LiveData con {@code null} si se registró, o el {@link ErrorCode} del fallo
     */
    public LiveData<ErrorCode> registrarTransaccion(long cuentaId, String tipo,
                                                    long monto, String concepto,
                                                    Double latitud, Double longitud,
                                                    String direccionLugar,
                                                    List<PartePropuesta> partesCompartidas) {
        MutableLiveData<ErrorCode> resultado = new MutableLiveData<>();

        final Ubicacion ubicacion;
        if (latitud == null && longitud == null) {
            ubicacion = null;
        } else if (latitud == null || longitud == null) {
            Log.w(TAG, "Coordenada incompleta: latitud=" + latitud + ", longitud=" + longitud);
            resultado.setValue(ErrorCode.UBICACION_INVALIDA);
            return resultado;
        } else {
            try {
                ubicacion = new Ubicacion(latitud, longitud);
            } catch (BusinessRuleException e) {
                Log.w(TAG, "Ubicación inválida para el movimiento", e);
                resultado.setValue(e.getErrorCode());
                return resultado;
            }
        }

        AppDatabase db = AppDatabase.getInstance(getApplication());

        db.getExecutor().execute(() -> {
            Cuenta entidad = db.cuentaDao().getCuentaById(cuentaId);
            if (entidad == null) {
                Log.w(TAG, "Cuenta no encontrada al registrar movimiento: id=" + cuentaId);
                resultado.postValue(ErrorCode.CUENTA_NO_ENCONTRADA);
                return;
            }

            TipoTransaccion tipoMovimiento;
            try {
                tipoMovimiento = TipoTransaccion.valueOf(tipo);
            } catch (IllegalArgumentException | NullPointerException e) {
                Log.w(TAG, "Tipo de movimiento inválido: " + tipo, e);
                resultado.postValue(ErrorCode.MONTO_INVALIDO);
                return;
            }

            com.localcash.domain.model.Cuenta cuentaDominio =
                    com.localcash.domain.model.Cuenta.fromEntity(entidad);

            if (partesCompartidas != null) {
                if (tipoMovimiento != TipoTransaccion.EGRESO) {
                    resultado.postValue(ErrorCode.GASTO_COMPARTIDO_INVALIDO);
                    return;
                }
                try {
                    GastoCompartidoService.validarReparto(
                            monto, contactosDe(partesCompartidas));
                    GastoCompartidoService.validarPersonalizado(monto, partesCompartidas);
                } catch (BusinessRuleException e) {
                    resultado.postValue(e.getErrorCode());
                    return;
                }
            }

            com.localcash.domain.model.Transaccion transaccionDominio =
                    new com.localcash.domain.model.Transaccion(
                            0L,
                            cuentaId,
                            tipoMovimiento,
                            BigDecimal.valueOf(monto),
                            concepto,
                            System.currentTimeMillis());

            try {
                TransaccionService.validarTransaccion(transaccionDominio, cuentaDominio);
            } catch (BusinessRuleException e) {
                Log.w(TAG, "Movimiento rechazado por regla de negocio: cuentaId=" + cuentaId
                        + ", tipo=" + tipo + ", monto=" + monto, e);
                resultado.postValue(e.getErrorCode());
                return;
            }

            try {
                db.runInTransaction(() -> {
                    com.localcash.data.entities.Transaccion entidadTransaccion =
                            transaccionDominio.toEntity();
                    if (ubicacion != null) {
                        entidadTransaccion.setLatitud(ubicacion.getLatitud());
                        entidadTransaccion.setLongitud(ubicacion.getLongitud());
                        entidadTransaccion.setDireccionLugar(direccionLugar);
                    }
                    long transaccionId = db.transaccionDao().insert(entidadTransaccion);

                    if (ubicacion != null) {
                        insertarPinVinculado(db, transaccionId, concepto, ubicacion);
                    }

                    BigDecimal nuevoSaldo = TransaccionService.calcularNuevoSaldo(
                            cuentaDominio, transaccionDominio);
                    db.cuentaDao().updateSaldo(cuentaId, nuevoSaldo.longValueExact());

                    if (partesCompartidas != null) {
                        gastoCompartidoRepository.insertTodas(
                                transaccionId, partesCompartidas, Instant.now().toString());
                    }
                });
                resultado.postValue(null);
            } catch (BusinessRuleException e) {
                Log.w(TAG, "Movimiento revertido por regla de negocio", e);
                resultado.postValue(e.getErrorCode());
            } catch (RuntimeException e) {
                Log.e(TAG, "No se pudo persistir el movimiento, su pin o su reparto", e);
                resultado.postValue(partesCompartidas != null
                        ? ErrorCode.GASTO_COMPARTIDO_INVALIDO
                        : ErrorCode.LUGAR_INVALIDO);
            }
        });

        return resultado;
    }

    /**
     * Extrae los ids de contacto de las partes para la validación común de
     * INV-1 (los nulos se dejan pasar para que el servicio los rechace).
     *
     * @param partes partes del reparto
     * @return ids en el mismo orden, con {@code null} donde la parte no lo trae
     */
    private static List<Long> contactosDe(List<PartePropuesta> partes) {
        List<Long> ids = new ArrayList<>(partes.size());
        for (PartePropuesta parte : partes) {
            ids.add(parte == null ? null : parte.getContactoId());
        }
        return ids;
    }

    /**
     * Inserta el pin vinculado a un movimiento recién creado. Se ejecuta dentro
     * de la misma transacción de Room que el movimiento y el saldo (T25).
     *
     * @param db            base de datos activa
     * @param transaccionId id del movimiento recién insertado
     * @param concepto      concepto del movimiento, usado como nombre del pin
     * @param ubicacion     coordenada confirmada por el usuario
     */
    private void insertarPinVinculado(@NonNull AppDatabase db, long transaccionId,
                                      @Nullable String concepto, @NonNull Ubicacion ubicacion) {
        String nombre = concepto != null ? concepto.trim() : "";
        if (nombre.isEmpty()) {
            nombre = "Movimiento";
        }
        Lugar pin = new Lugar(
                0L,
                transaccionId,
                nombre,
                TipoLugar.OTRO,
                TipoLugar.OTRO.getEmojiPorDefecto(),
                null,
                ubicacion,
                EstadoLugar.ABIERTO);
        LugarService.validarLugar(pin);
        db.lugarDao().insert(pin.toEntity());
    }
}
