package com.localcash.ui.cuentas;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Cuenta;
import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.TipoTransaccion;
import com.localcash.domain.model.Ubicacion;
import com.localcash.domain.service.TransaccionService;

import java.math.BigDecimal;
import java.util.List;

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

    /**
     * Construye el ViewModel y lo enlaza con Room.
     *
     * @param application Aplicación usada para obtener la base de datos
     */
    public TransaccionViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        this.cuentasActivas = db.cuentaDao().getAll();
    }

    /**
     * Observa las cuentas activas de la base de datos, para poblar el selector.
     *
     * @return LiveData con la lista de cuentas activas (entidades Room)
     */
    public LiveData<List<Cuenta>> getCuentasActivas() {
        return cuentasActivas;
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
     * @param latitud  latitud WGS84 elegida, o {@code null} (sin ubicación)
     * @param longitud longitud WGS84 elegida, o {@code null} (sin ubicación)
     * @param direccion dirección legible opcional (hoy {@code null}: sin geocodificación inversa)
     * @return LiveData con {@code null} si se registró, o el {@link ErrorCode} del fallo
     */
    public LiveData<ErrorCode> registrarTransaccion(long cuentaId, String tipo,
                                                    long monto, String concepto,
                                                    Double latitud, Double longitud,
                                                    String direccion) {
        MutableLiveData<ErrorCode> resultado = new MutableLiveData<>();
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

            // Valida la ubicación con el value object (fuente única de la regla
            // "ambas o ninguna" y del rango); null = sin geolocalización.
            Ubicacion ubicacion;
            try {
                ubicacion = Ubicacion.crear(latitud, longitud, direccion);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Ubicación inválida al registrar movimiento: lat=" + latitud
                        + ", lon=" + longitud, e);
                resultado.postValue(ErrorCode.UBICACION_INVALIDA);
                return;
            }

            com.localcash.domain.model.Transaccion transaccionDominio =
                    new com.localcash.domain.model.Transaccion(
                            0L,
                            cuentaId,
                            tipoMovimiento,
                            BigDecimal.valueOf(monto),
                            concepto,
                            System.currentTimeMillis(),
                            ubicacion);

            try {
                TransaccionService.validarTransaccion(transaccionDominio, cuentaDominio);
            } catch (BusinessRuleException e) {
                Log.w(TAG, "Movimiento rechazado por regla de negocio: cuentaId=" + cuentaId
                        + ", tipo=" + tipo + ", monto=" + monto, e);
                resultado.postValue(e.getErrorCode());
                return;
            }

            db.transaccionDao().insert(transaccionDominio.toEntity());

            BigDecimal nuevoSaldo =
                    TransaccionService.calcularNuevoSaldo(cuentaDominio, transaccionDominio);
            db.cuentaDao().updateSaldo(cuentaId, nuevoSaldo.longValueExact());

            resultado.postValue(null);
        });

        return resultado;
    }
}
