package com.localcash.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

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
     * Observa todas las cuentas. Se re-emite automáticamente ante cualquier
     * cambio en la tabla.
     *
     * @return LiveData con la lista de cuentas
     */
    @Query("SELECT * FROM cuentas")
    LiveData<List<Cuenta>> getAll();

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
     * Elimina una cuenta. Sus transacciones se eliminan en cascada
     * (ON DELETE CASCADE declarado en la entidad Transaccion).
     *
     * @param cuenta Cuenta a eliminar
     */
    @Delete
    void delete(Cuenta cuenta);
}
