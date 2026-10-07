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
     * Inserta un movimiento (ingreso o egreso). Room rellena el {@code id}.
     *
     * @param transaccion Movimiento a insertar (monto en pesos CLP, positivo)
     */
    @Insert
    void insert(Transaccion transaccion);

    /**
     * Observa los movimientos de una cuenta en orden cronológico inverso.
     *
     * @param cuentaId Id de la cuenta
     * @return LiveData con la lista de movimientos de la cuenta
     */
    @Query("SELECT * FROM transacciones WHERE cuentaId = :cuentaId ORDER BY fecha DESC")
    LiveData<List<Transaccion>> getByCuentaId(long cuentaId);

    /**
     * Observa todos los movimientos de todas las cuentas,
     * en orden cronológico inverso.
     *
     * @return LiveData con la lista completa de movimientos
     */
    @Query("SELECT * FROM transacciones ORDER BY fecha DESC")
    LiveData<List<Transaccion>> getAll();
}
