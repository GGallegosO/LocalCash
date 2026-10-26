package com.localcash.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.localcash.data.entities.Contacto;

import java.util.List;

/**
 * DAO de la entidad {@link Contacto} (tabla {@code CONTACTOS}).
 * <p>
 * Los nombres de tabla se centralizan en {@link #TABLA} para que un cambio de
 * esquema no deje literales dispersos (`.agent.md` §6.12); Room valida las
 * consultas en compilación.
 * </p>
 * <p>
 * Las operaciones síncronas ({@code insert}, {@code update}, {@code delete})
 * deben ejecutarse en el executor de {@code com.localcash.data.db.AppDatabase}.
 * Las consultas {@link LiveData} se gestionan en background automáticamente.
 * </p>
 * <p>
 * El borrado físico de un contacto con compras asociadas o deudas pendientes
 * falla con una restricción de clave foránea ({@code ON DELETE RESTRICT});
 * la capa de repositorio traduce ese error al código de negocio
 * {@code CONTACTO_EN_USO}.
 * </p>
 */
@Dao
public interface ContactoDao {

    /** Nombre canónico de la tabla; una sola fuente para consultas y pruebas. */
    String TABLA = "CONTACTOS";

    /**
     * Inserta un contacto y devuelve el id generado.
     *
     * @param contacto Contacto a insertar
     * @return id autogenerado
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Contacto contacto);

    /**
     * Actualiza todos los campos editables de un contacto.
     *
     * @param contacto Contacto con los valores actualizados (debe tener id)
     */
    @Update(onConflict = OnConflictStrategy.ABORT)
    void update(Contacto contacto);

    /**
     * Elimina físicamente un contacto. Si tiene compras asociadas o deudas
     * pendientes/parciales, SQLite rechaza el borrado por
     * {@code ON DELETE RESTRICT} y lanza {@code SQLiteConstraintException}.
     *
     * @param contacto Contacto a eliminar
     */
    @Delete
    void delete(Contacto contacto);

    /**
     * Observa todos los contactos ordenados por nombre.
     *
     * @return LiveData con la lista de contactos (nunca {@code null})
     */
    @Query("SELECT * FROM " + TABLA + " ORDER BY lower(nombre) ASC")
    LiveData<List<Contacto>> getAll();

    /**
     * Obtiene un contacto por id de forma bloqueante. Solo para hilos
     * secundarios.
     *
     * @param id Id del contacto
     * @return Contacto o {@code null} si no existe
     */
    @Query("SELECT * FROM " + TABLA + " WHERE id = :id")
    Contacto getByIdSync(long id);

    /**
     * Verifica si ya existe un contacto con el teléfono indicado (ignorando
     * mayúsculas/minúsculas y filas con {@code NULL}). El índice único parcial
     * de la base garantiza la unicidad en el motor; este método lo consulta
     * antes de insertar para devolver un error de negocio amigable.
     *
     * @param telefono Teléfono a verificar
     * @return {@code true} si ya existe un contacto con ese teléfono
     */
    @Query("SELECT COUNT(*) FROM " + TABLA
            + " WHERE lower(trim(telefono)) = lower(trim(:telefono))")
    int existeTelefono(String telefono);

    /**
     * Verifica si ya existe un contacto con el email indicado (ignorando
     * mayúsculas/minúsculas y filas con {@code NULL}).
     *
     * @param email Email a verificar
     * @return {@code true} si ya existe un contacto con ese email
     */
    @Query("SELECT COUNT(*) FROM " + TABLA
            + " WHERE lower(trim(email)) = lower(trim(:email))")
    int existeEmail(String email);
}
