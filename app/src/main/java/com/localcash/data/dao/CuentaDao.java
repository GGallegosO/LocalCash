package com.localcash.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.localcash.data.entities.Cuenta;

import java.util.List;

/**
 * DAO de la entidad {@link Cuenta} (tabla {@code cuentas}).
 * <p>
 * Los nombres de tabla aparecen en los literales {@code @Query}, que Room
 * valida en compilación (fallo de compilación, no de ejecución).
 * </p>
 * <p>
 * El saldo se escribe únicamente con pesos CLP ({@code long}); la validación
 * de negocio (monto &gt; 0, saldo suficiente) vive en {@code domain/service},
 * nunca en esta capa.
 * </p>
 */
@Dao
public interface CuentaDao {

    /**
     * Inserta una cuenta. Room rellena el {@code id} generado en la entidad.
     *
     * @param cuenta Cuenta a insertar (saldo inicial en pesos CLP)
     */
    @Insert
    void insert(Cuenta cuenta);

    /**
     * Observa todas las cuentas <b>activas</b>. Se re-emite automáticamente
     * ante cualquier cambio en la tabla.
     * <p>
     * Las cuentas "eliminadas" son un soft delete ({@code activa = false}):
     * siguen en la BD para preservar el historial de transacciones, pero no
     * se muestran.
     * </p>
     *
     * @return LiveData con la lista de cuentas activas
     */
    @Query("SELECT * FROM cuentas WHERE activa = 1")
    LiveData<List<Cuenta>> getAll();

    /**
     * Obtiene una cuenta por su id de forma <b>bloqueante</b>.
     * <p>
     * Solo apta para hilos secundarios (se invoca dentro del
     * {@code Executor} de {@link com.localcash.data.db.AppDatabase}).
     * Devuelve {@code null} si no existe.
     * </p>
     *
     * @param cuentaId Id de la cuenta
     * @return Entidad {@link Cuenta} o {@code null} si no existe
     */
    @Query("SELECT * FROM cuentas WHERE id = :cuentaId")
    Cuenta getCuentaById(long cuentaId);

    /**
     * Actualiza una cuenta existente (nombre, tipo, saldo, moneda y el flag
     * {@code activa}). La fila se identifica por su {@code id}.
     * <p>
     * La decisión de negocio de <i>qué</i> se puede editar (p. ej. el saldo
     * solo si no hay transacciones) vive en el ViewModel/servicio de dominio;
     * este método solo persiste el estado resultante.
     * </p>
     *
     * @param cuenta Cuenta con los valores actualizados
     */
    @Update
    void update(Cuenta cuenta);

    /**
     * Cuenta las transacciones asociadas a una cuenta. Se usa para validar
     * si el saldo es editable: si la cuenta ya tiene movimientos, cambiar el
     * saldo a mano rompería el historial.
     *
     * @param cuentaId Id de la cuenta
     * @return LiveData con el número de transacciones de la cuenta
     */
    @Query("SELECT COUNT(*) FROM transacciones WHERE cuentaId = :cuentaId")
    LiveData<Integer> contarTransacciones(long cuentaId);

    /**
     * Actualiza el saldo de una cuenta con valor absoluto en pesos CLP.
     * <p>
     * El cálculo del nuevo saldo ocurre en {@code domain/service}
     * (TransaccionService); este método solo persiste el resultado.
     * </p>
     *
     * @param cuentaId Id de la cuenta
     * @param saldo    Nuevo saldo en pesos CLP (long)
     */
    @Query("UPDATE cuentas SET saldo = :saldo WHERE id = :cuentaId")
    void updateSaldo(long cuentaId, long saldo);

    /**
     * Suma (o resta, con delta negativo) un importe al saldo con
     * <b>aritmética atómica</b>: la base calcula {@code saldo = saldo + :delta}
     * en una sola sentencia, sin leer antes el valor.
     * <p>
     * Es la vía obligatoria para movimientos generados por el sistema (por
     * ejemplo, el cobro de una parte de un gasto compartido, I-16): evita la
     * condición de carrera del patrón leer-calcular-escribir. El delta va en
     * pesos CLP.
     * </p>
     *
     * @param cuentaId Id de la cuenta
     * @param delta    importe a sumar en pesos CLP (negativo para restar)
     * @return número de filas afectadas: 1 si la cuenta existe, 0 si no
     */
    @Query("UPDATE cuentas SET saldo = saldo + :delta WHERE id = :cuentaId")
    int sumarSaldo(long cuentaId, long delta);

    /**
     * Elimina una cuenta. Sus transacciones se eliminan en cascada
     * (ON DELETE CASCADE declarado en la entidad Transaccion).
     *
     * @param cuenta Cuenta a eliminar
     */
    @Delete
    void delete(Cuenta cuenta);
}
