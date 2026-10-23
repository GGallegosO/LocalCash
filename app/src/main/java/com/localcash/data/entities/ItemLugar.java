package com.localcash.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room de un elemento de la lista de cosas de un pin
 * (tabla {@code ITEMS_LUGAR}).
 * <p>
 * {@code completado} se persiste como {@code INTEGER} 0/1 porque SQLite no
 * tiene un tipo booleano nativo. La relación con {@link Lugar} es obligatoria:
 * un elemento sin pin no tiene contexto y se elimina en cascada.
 * </p>
 */
@Entity(
        tableName = "ITEMS_LUGAR",
        foreignKeys = @ForeignKey(
                entity = Lugar.class,
                parentColumns = "id",
                childColumns = "lugar_id",
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("lugar_id")}
)
public class ItemLugar {

    /** Identificador local generado por el motor. */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Lugar al que pertenece el elemento. */
    @ColumnInfo(name = "lugar_id")
    private long lugarId;

    /** Qué hay que recordar ("leche", "pan", "llamar al taller"). */
    @NonNull
    private String descripcion;

    /** {@code true} = completado, {@code false} = pendiente. */
    @ColumnInfo(defaultValue = "0")
    private boolean completado;

    /** Constructor vacío requerido por Room. */
    public ItemLugar() {
    }

    /** Constructor completo para insertar un elemento. */
    @Ignore
    public ItemLugar(long lugarId, String descripcion, boolean completado) {
        this.lugarId = lugarId;
        this.descripcion = descripcion;
        this.completado = completado;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLugarId() {
        return lugarId;
    }

    public void setLugarId(long lugarId) {
        this.lugarId = lugarId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isCompletado() {
        return completado;
    }

    public void setCompletado(boolean completado) {
        this.completado = completado;
    }
}
