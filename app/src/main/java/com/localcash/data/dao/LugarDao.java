package com.localcash.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.localcash.data.entities.Lugar;

import java.util.List;

/**
 * DAO de la entidad {@link Lugar} (tabla {@code LUGARES}).
 * <p>
 * Los pines se consultan por estado y por caja envolvente. El nombre de la
 * tabla se centraliza en {@link #TABLA} para que un cambio de esquema no deje
 * literales dispersos (`.agent.md` §6.12); Room valida las consultas en
 * compilación.
 * </p>
 * <p>
 * Las operaciones síncronas ({@code insert}, {@code update}, {@code delete})
 * deben ejecutarse en el executor de {@code AppDatabase}. Las consultas
 * {@link LiveData} se gestionan en background automáticamente.
 * </p>
 */
@Dao
public interface LugarDao {

    /** Nombre canónico de la tabla; una sola fuente para consultas y pruebas. */
    String TABLA = "LUGARES";

    /**
     * Inserta un lugar y devuelve el id generado.
     *
     * @param lugar Pin a insertar
     * @return id autogenerado
     */
    @Insert
    long insert(Lugar lugar);

    /**
     * Actualiza todos los campos editables de un lugar.
     *
     * @param lugar Pin con los valores actualizados
     */
    @Update
    void update(Lugar lugar);

    /**
     * Elimina físicamente un lugar. Sus {@code ITEMS_LUGAR} se borran en
     * cascada por la FK.
     *
     * @param lugar Pin a eliminar
     */
    @Delete
    void delete(Lugar lugar);

    /**
     * Observa todos los lugares, abiertos y cerrados, del más nuevo al más
     * antiguo. Se usa para pintar el mapa completo.
     *
     * @return LiveData con todos los pines
     */
    @Query("SELECT * FROM " + TABLA + " ORDER BY id DESC")
    LiveData<List<Lugar>> getAll();

    /**
     * Observa solo los lugares activos, que son los que aparecen en la lista
     * principal.
     *
     * @return LiveData con los pines abiertos
     */
    @Query("SELECT * FROM " + TABLA + " WHERE estado = 'abierto' ORDER BY id DESC")
    LiveData<List<Lugar>> getAbiertos();

    /**
     * Observa los lugares cerrados; siguen visibles en el mapa y pueden
     * reabrirse.
     *
     * @return LiveData con los pines cerrados
     */
    @Query("SELECT * FROM " + TABLA + " WHERE estado = 'cerrado' ORDER BY id DESC")
    LiveData<List<Lugar>> getCerrados();

    /**
     * Obtiene un lugar por id de forma bloqueante. Solo para hilos secundarios.
     *
     * @param id Id del lugar
     * @return Lugar o {@code null} si no existe
     */
    @Query("SELECT * FROM " + TABLA + " WHERE id = :id")
    Lugar getByIdSync(long id);

    /**
     * Obtiene el pin vinculado a un movimiento, si existe. El índice único
     * garantiza como máximo una fila.
     *
     * @param transaccionId Id del movimiento
     * @return Lugar vinculado o {@code null}
     */
    @Query("SELECT * FROM " + TABLA + " WHERE transaccion_id = :transaccionId LIMIT 1")
    Lugar getByTransaccionSync(long transaccionId);

    /**
     * Observa los pines dentro de una caja envolvente (bounding box) para
     * consultar solo la zona visible del mapa.
     *
     * @param minLatitud  latitud mínima
     * @param maxLatitud  latitud máxima
     * @param minLongitud longitud mínima
     * @param maxLongitud longitud máxima
     * @return LiveData con los pines de la zona
     */
    @Query("SELECT * FROM " + TABLA
            + " WHERE latitud BETWEEN :minLatitud AND :maxLatitud"
            + " AND longitud BETWEEN :minLongitud AND :maxLongitud"
            + " ORDER BY id DESC")
    LiveData<List<Lugar>> getEnRango(double minLatitud, double maxLatitud,
                                     double minLongitud, double maxLongitud);

    /**
     * Cambia el estado de un pin sin borrarlo. La transición permitida se
     * valida en {@code LugarService}; este método solo persiste el valor.
     *
     * @param id     Id del lugar
     * @param estado {@code abierto} o {@code cerrado}
     */
    @Query("UPDATE " + TABLA + " SET estado = :estado WHERE id = :id")
    void updateEstado(long id, String estado);
}
