package com.localcash.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.localcash.data.entities.ItemLugar;

import java.util.List;

/**
 * DAO de la entidad {@link ItemLugar} (tabla {@code ITEMS_LUGAR}).
 * <p>
 * Cada consulta filtra por {@code lugar_id}; el elemento nunca se lee fuera
 * del contexto de su pin. La tabla se centraliza en {@link #TABLA} para no
 * repetir literales (`.agent.md` §6.12).
 * </p>
 */
@Dao
public interface ItemLugarDao {

    /** Nombre canónico de la tabla. */
    String TABLA = "ITEMS_LUGAR";

    /**
     * Inserta un elemento y devuelve el id generado.
     *
     * @param item Elemento de la lista
     * @return id autogenerado
     */
    @Insert
    long insert(ItemLugar item);

    /**
     * Actualiza descripción y estado de completado.
     *
     * @param item Elemento con los valores actualizados
     */
    @Update
    void update(ItemLugar item);

    /**
     * Elimina físicamente un elemento de la lista.
     *
     * @param item Elemento a eliminar
     */
    @Delete
    void delete(ItemLugar item);

    /**
     * Observa la lista de cosas de un pin, en orden de creación.
     *
     * @param lugarId Id del lugar
     * @return LiveData con los elementos del pin
     */
    @Query("SELECT * FROM " + TABLA + " WHERE lugar_id = :lugarId ORDER BY id ASC")
    LiveData<List<ItemLugar>> getByLugarId(long lugarId);

    /**
     * Obtiene la lista de un pin de forma bloqueante. Solo hilos secundarios.
     *
     * @param lugarId Id del lugar
     * @return lista de elementos (puede estar vacía)
     */
    @Query("SELECT * FROM " + TABLA + " WHERE lugar_id = :lugarId ORDER BY id ASC")
    List<ItemLugar> getByLugarIdSync(long lugarId);

    /**
     * Marca o desmarca un elemento sin reescribir la descripción.
     *
     * @param id         Id del elemento
     * @param completado 1 = completado, 0 = pendiente
     */
    @Query("UPDATE " + TABLA + " SET completado = :completado WHERE id = :id")
    void updateCompletado(long id, boolean completado);
}
