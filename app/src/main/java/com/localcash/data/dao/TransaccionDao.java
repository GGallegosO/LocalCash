package com.localcash.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.localcash.data.entities.Transaccion;

import java.util.List;

/**
 * DAO de la entidad {@link Transaccion} (tabla {@code transacciones}).
 * <p>
 * Esta tabla es la <b>única fuente de verdad del saldo</b> (data-model.md §5.6):
 * el saldo de la cuenta es una proyección derivada de sus movimientos.
 * Toda escritura de movimiento se hace en conjunto con la actualización
 * atómica del saldo vía {@link CuentaDao#updateSaldo}, gobernada por
 * {@code domain/service/TransaccionService}.
 * </p>
 */
@Dao
public interface TransaccionDao {

    /**
     * Inserta un movimiento (ingreso o egreso). Room rellena el {@code id}
     * y lo devuelve para poder crear el pin vinculado en la misma transacción
     * de base de datos.
     *
     * @param transaccion Movimiento a insertar (monto en pesos CLP, positivo)
     * @return id autogenerado
     */
    @Insert
    long insert(Transaccion transaccion);

    /**
     * Observa los movimientos de una cuenta en orden cronológico inverso.
     *
     * @param cuentaId Id de la cuenta
     * @return LiveData con la lista de movimientos de la cuenta
     */
    @Query("SELECT * FROM transacciones WHERE cuentaId = :cuentaId ORDER BY fecha DESC")
    LiveData<List<Transaccion>> getByCuentaId(long cuentaId);

    /**
     * Cuenta las transacciones de una cuenta de forma <b>bloqueante</b>.
     * <p>
     * Solo apta para hilos secundarios (se invoca dentro del
     * {@code Executor} de {@link com.localcash.data.db.AppDatabase}). Se usa
     * como guarda transaccional antes de editar el saldo de una cuenta: si
     * ya tiene movimientos, el saldo proviene del historial y no se edita a mano.
     * </p>
     *
     * @param cuentaId Id de la cuenta
     * @return Número de transacciones de la cuenta
     */
    @Query("SELECT COUNT(*) FROM transacciones WHERE cuentaId = :cuentaId")
    int contarTransaccionesSync(long cuentaId);

    /**
     * Observa todos los movimientos de todas las cuentas,
     * en orden cronológico inverso.
     *
     * @return LiveData con la lista completa de movimientos
     */
    @Query("SELECT * FROM transacciones ORDER BY fecha DESC")
    LiveData<List<Transaccion>> getAll();

    /**
     * Observa un movimiento por su id.
     * <p>
     * Lo usa el detalle de un pin vinculado para mostrar la fecha y hora del
     * movimiento que lo originó (Feature 009). Emite {@code null} si la fila
     * ya no existe (por ejemplo, si el movimiento se eliminó y la FK dejó el
     * pin independiente).
     * </p>
     *
     * @param id Id del movimiento
     * @return LiveData con el movimiento o {@code null}
     */
    @Query("SELECT * FROM transacciones WHERE id = :id")
    LiveData<Transaccion> getById(long id);

    /**
     * Obtiene un movimiento por su id de forma <b>bloqueante</b>. Solo para
     * hilos secundarios.
     * <p>
     * Lo usa el cobro de una parte de gasto compartido: necesita la cuenta del
     * egreso original para asentar la devolución en esa misma cuenta (I-16).
     * </p>
     *
     * @param id Id del movimiento
     * @return movimiento o {@code null} si no existe
     */
    @Query("SELECT * FROM transacciones WHERE id = :id")
    Transaccion getByIdSync(long id);

    /**
     * Observa los movimientos con coordenadas completas. La regla de negocio
     * {@code ambas o ninguna} se valida antes de persistir; la consulta filtra
     * filas históricas incompletas por seguridad.
     *
     * @return LiveData con los movimientos geolocalizados
     */
    @Query("SELECT * FROM transacciones"
            + " WHERE latitud IS NOT NULL AND longitud IS NOT NULL"
            + " ORDER BY fecha DESC")
    LiveData<List<Transaccion>> getGeolocalizadas();
}
