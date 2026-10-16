package com.localcash.ui.cuentas;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Cuenta;

import java.math.BigDecimal;
import java.util.List;

/**
 * ViewModel de la Vista 1 (Cuentas): intermediario entre la UI y Room.
 * <p>
 * Expone el listado observable de cuentas y el saldo total consolidado, que se
 * recalcula automáticamente ante cualquier cambio en la tabla {@code cuentas}.
 * </p>
 * <p>
 * <b>Decisiones de la feature 007 (aprobadas):</b>
 * </p>
 * <ul>
 *   <li>Sin repositorio: el ViewModel observa {@code CuentaDao} directamente;
 *       desvío justificado del diagrama de capas para una feature simple, así se
 *       registra en {@code plan.md}.</li>
 *   <li>El total consolidado se calcula aquí como operación de presentación
 *       (no como regla de negocio): suma de saldos, sin reglas nuevas — YAGNI.</li>
 *   <li>La conversión {@code long → BigDecimal} reutiliza el único par canónico
 *       {@code domain.model.Cuenta.fromEntity()} / {@code toEntity()}
 *       (pesos CLP directos, factor 1; decisión #18 de {@code doc/007-cuentas.md}).</li>
 * </ul>
 */
public class CuentasViewModel extends AndroidViewModel {

    /** Todas las cuentas persistidas, reactivas a cada cambio en la tabla. */
    private final LiveData<List<Cuenta>> todasLasCuentas;

    /** Saldo total consolidado (suma de todos los saldos), derivado de la lista. */
    private final LiveData<BigDecimal> saldoTotal;

    /**
     * Construye el ViewModel y lo enlaza con Room.
     *
     * @param application Aplicación usada para obtener la base de datos
     */
    public CuentasViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        this.todasLasCuentas = db.cuentaDao().getAll();
        this.saldoTotal = Transformations.map(todasLasCuentas, cuentas -> {
            if (cuentas == null || cuentas.isEmpty()) {
                return BigDecimal.ZERO;
            }
            BigDecimal total = BigDecimal.ZERO;
            for (Cuenta cuenta : cuentas) {
                total = total.add(
                        com.localcash.domain.model.Cuenta.fromEntity(cuenta).getSaldo());
            }
            return total;
        });
    }

    /**
     * Observa todas las cuentas de la base de datos.
     *
     * @return LiveData con la lista de cuentas (entidades Room)
     */
    public LiveData<List<Cuenta>> getTodasLasCuentas() {
        return todasLasCuentas;
    }

    /**
     * Observa el saldo total consolidado en pesos CLP.
     *
     * @return LiveData con la suma de los saldos ({@code BigDecimal.ZERO} si no hay cuentas)
     */
    public LiveData<BigDecimal> getSaldoTotal() {
        return saldoTotal;
    }

    /**
     * Inserta una cuenta nueva en la base de datos, en un hilo secundario.
     * <p>
     * El {@code id} lo genera Room automáticamente ({@code autoGenerate}) y la moneda
     * es siempre {@code CLP}. Al completarse la inserción, el {@code LiveData} del DAO
     * emite el nuevo listado y la UI se actualiza sola.
     * </p>
     *
     * @param nombre       Nombre de la cuenta
     * @param tipo         Tipo de cuenta ({@code AHORRO} o {@code TARJETA})
     * @param saldoInicial Saldo inicial en pesos CLP (sin centavos)
     */
    public void insertarCuenta(String nombre, String tipo, long saldoInicial) {
        com.localcash.data.entities.Cuenta entidad = new com.localcash.data.entities.Cuenta();
        entidad.setNombre(nombre);
        entidad.setTipo(tipo);
        entidad.setSaldo(saldoInicial);
        entidad.setMoneda("CLP");

        AppDatabase db = AppDatabase.getInstance(getApplication());
        db.getExecutor().execute(() -> db.cuentaDao().insert(entidad));
    }

    /**
     * Cambia el nombre de una cuenta existente, en un hilo secundario.
     * <p>
     * Carga la entidad, actualiza su nombre y persiste con {@code update}. Al
     * completarse, el {@code LiveData} del DAO re-emite la lista y la UI se
     * actualiza sola. Si la cuenta no existe, no se hace nada.
     * </p>
     *
     * @param cuentaId    Id de la cuenta a editar
     * @param nuevoNombre Nuevo nombre visible de la cuenta
     */
    public void editarNombreCuenta(long cuentaId, String nuevoNombre) {
        AppDatabase db = AppDatabase.getInstance(getApplication());
        db.getExecutor().execute(() -> {
            Cuenta cuenta = db.cuentaDao().getCuentaById(cuentaId);
            if (cuenta == null) {
                return;
            }
            cuenta.setNombre(nuevoNombre);
            db.cuentaDao().update(cuenta);
        });
    }

    /**
     * Edita el saldo de una cuenta, en un hilo secundario.
     * <p>
     * <b>Guarda de negocio:</b> el saldo solo es editable si la cuenta no tiene
     * transacciones. Si ya tiene movimientos, el saldo es una proyección del
     * historial y editarlo a mano lo corrompería; en ese caso se emite
     * {@code false} y no se persiste nada.
     * </p>
     * <p>
     * Devuelve un {@code LiveData<Boolean>} de un solo uso: {@code true} si el
     * saldo se actualizó, {@code false} si la operación fue rechazada (cuenta
     * con transacciones o inexistente). La UI traduce ese resultado a un
     * mensaje ({@code @string/...}); el ViewModel no produce texto de usuario.
     * </p>
     *
     * @param cuentaId   Id de la cuenta a editar
     * @param nuevoSaldo Nuevo saldo en pesos CLP (sin centavos)
     * @return LiveData con {@code true} si se actualizó, {@code false} si se rechazó
     */
    public LiveData<Boolean> editarSaldoCuenta(long cuentaId, long nuevoSaldo) {
        MutableLiveData<Boolean> resultado = new MutableLiveData<>();
        AppDatabase db = AppDatabase.getInstance(getApplication());
        db.getExecutor().execute(() -> {
            if (db.transaccionDao().contarTransaccionesSync(cuentaId) > 0) {
                resultado.postValue(false);
                return;
            }
            Cuenta cuenta = db.cuentaDao().getCuentaById(cuentaId);
            if (cuenta == null) {
                resultado.postValue(false);
                return;
            }
            cuenta.setSaldo(nuevoSaldo);
            db.cuentaDao().update(cuenta);
            resultado.postValue(true);
        });
        return resultado;
    }

    /**
     * Desactiva (soft delete) una cuenta existente, en un hilo secundario.
     * <p>
     * La fila <b>nunca se elimina</b> de la base de datos: solo se marca
     * {@code activa = false}. Así se preserva el historial de transacciones
     * ({@code ON DELETE CASCADE} no se dispara). Como {@code getAll()} filtra
     * {@code activa = 1}, la cuenta desaparece de la lista automáticamente al
     * re-emitirse el {@code LiveData}. Si la cuenta no existe, no se hace nada.
     * </p>
     *
     * @param cuentaId Id de la cuenta a desactivar
     */
    public void desactivarCuenta(long cuentaId) {
        AppDatabase db = AppDatabase.getInstance(getApplication());
        db.getExecutor().execute(() -> {
            Cuenta cuenta = db.cuentaDao().getCuentaById(cuentaId);
            if (cuenta == null) {
                return;
            }
            cuenta.setActiva(false);
            db.cuentaDao().update(cuenta);
        });
    }
}
