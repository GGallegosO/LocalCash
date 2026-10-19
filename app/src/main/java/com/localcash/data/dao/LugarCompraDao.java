package com.localcash.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.localcash.data.entities.ItemCompra;
import com.localcash.data.entities.LugarCompra;
import com.localcash.data.relations.LugarCompraConItems;

import java.util.List;

/**
 * DAO de los lugares de compra (tablas {@code lugar_compra} e
 * {@code item_compra}).
 * <p>
 * Las operaciones síncronas deben ejecutarse fuera del hilo principal con el
 * {@code Executor} de {@link com.localcash.data.db.AppDatabase}.
 * </p>
 */
@Dao
public interface LugarCompraDao {

    /**
     * Inserta la cabecera de un lugar de compra. Room rellena el {@code id}.
     *
     * @param lugarCompra Lugar a insertar
     * @return Id generado (rowid)
     */
    @Insert
    long insertLugar(LugarCompra lugarCompra);

    /**
     * Inserta los ítems de una lista. Cada ítem debe traer su
     * {@code lugarCompraId} ya asignado.
     *
     * @param items Ítems a insertar
     */
    @Insert
    void insertItems(List<ItemCompra> items);

    /**
     * Inserta un lugar con su lista de ítems en una sola transacción.
     * <p>
     * Asigna el id generado del lugar a cada ítem antes de insertarlo, de modo
     * que nunca queden ítems huérfanos ni una cabecera sin sus ítems.
     * </p>
     *
     * @param lugarCompra Lugar a insertar
     * @param items       Ítems asociados (puede ser vacío)
     * @return Id generado del lugar
     */
    @Transaction
    default long insertLugarConItems(LugarCompra lugarCompra, List<ItemCompra> items) {
        long lugarId = insertLugar(lugarCompra);
        if (items != null && !items.isEmpty()) {
            for (ItemCompra item : items) {
                item.setLugarCompraId(lugarId);
            }
            insertItems(items);
        }
        return lugarId;
    }

    /**
     * Devuelve todos los lugares de compra con sus ítems, del más reciente al
     * más antiguo.
     * <p>
     * {@code @Transaction} garantiza que la cabecera y sus ítems se lean de
     * forma consistente; {@code @Relation} resuelve la relación uno-a-muchos.
     * Método bloqueante: invocar desde un hilo secundario.
     * </p>
     *
     * @return Lista de lugares con sus ítems
     */
    @Transaction
    @Query("SELECT * FROM lugar_compra ORDER BY fechaCreacion DESC")
    List<LugarCompraConItems> getAllLugaresConItems();

    /**
     * Variante observable de {@link #getAllLugaresConItems()} para la UI.
     * <p>
     * Room reejecuta la consulta cada vez que cambia {@code lugar_compra} o
     * {@code item_compra}, de modo que el mapa recibe el pin recién creado sin
     * que la vista consulte la base de datos. La consulta se ejecuta en el
     * executor de Room, nunca en el hilo principal.
     * </p>
     *
     * @return LiveData con los lugares de compra y sus ítems, del más reciente
     *         al más antiguo
     */
    @Transaction
    @Query("SELECT * FROM lugar_compra ORDER BY fechaCreacion DESC")
    LiveData<List<LugarCompraConItems>> getAllLugaresConItemsLive();

    /**
     * Marca un ítem como comprado o pendiente (actualiza la fila completa).
     *
     * @param item Ítem con los valores actualizados
     */
    @Update
    void actualizarItem(ItemCompra item);

    /**
     * Elimina un lugar de compra. Sus ítems se eliminan en cascada por la FK
     * {@code ON DELETE CASCADE} declarada en {@link ItemCompra}.
     *
     * @param id Id del lugar a eliminar
     */
    @Query("DELETE FROM lugar_compra WHERE id = :id")
    void eliminarLugarCompra(long id);
}
