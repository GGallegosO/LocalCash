package com.localcash.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.localcash.data.entities.ItemCompra;

import java.util.List;

/**
 * DAO de los ítems de compra (tabla {@code item_compra}).
 * <p>
 * Complementa a {@link LugarCompraDao}: aquí viven las operaciones directas
 * sobre ítems; la inserción transaccional de un lugar con su lista y el
 * borrado en cascada se gestionan desde el DAO del lugar.
 * </p>
 */
@Dao
public interface ItemCompraDao {

    /**
     * Inserta un ítem. Room rellena el {@code id}.
     *
     * @param item Ítem a insertar
     * @return Id generado
     */
    @Insert
    long insert(ItemCompra item);

    /**
     * Inserta varios ítems en una sola operación.
     *
     * @param items Ítems a insertar
     */
    @Insert
    void insertAll(List<ItemCompra> items);

    /**
     * Actualiza un ítem existente (por ejemplo, para marcarlo como comprado).
     *
     * @param item Ítem con los valores actualizados
     */
    @Update
    void update(ItemCompra item);

    /**
     * Elimina un ítem concreto.
     *
     * @param item Ítem a eliminar
     */
    @Delete
    void delete(ItemCompra item);

    /**
     * Devuelve los ítems de un lugar de compra, en orden de inserción.
     *
     * @param lugarCompraId Id del lugar
     * @return Ítems del lugar
     */
    @Query("SELECT * FROM item_compra WHERE lugarCompraId = :lugarCompraId ORDER BY id ASC")
    List<ItemCompra> getByLugarCompraId(long lugarCompraId);
}
