package com.localcash.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa un pin del mapa (tabla {@code LUGARES}).
 * <p>
 * Un lugar puede estar vinculado a un movimiento financiero
 * ({@code transaccion_id} no nulo) o ser un pin independiente creado desde la
 * pestaña "Mapa" ({@code transaccion_id} nulo). Las coordenadas se almacenan
 * como {@code double} en grados WGS84, el mismo tipo que consume MapLibre
 * ({@code LatLng}); no se usa {@code double} para dinero
 * ({@code .agent.md} §6.10).
 * </p>
 * <p>
 * El borrado del movimiento vinculado usa {@code ON DELETE SET NULL}: la lista
 * de cosas del usuario no debe desaparecer sin su decisión. Al borrar el
 * lugar, sus {@link ItemLugar} se eliminan en cascada.
 * </p>
 */
@Entity(
        tableName = "LUGARES",
        foreignKeys = @ForeignKey(
                entity = Transaccion.class,
                parentColumns = "id",
                childColumns = "transaccion_id",
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {
                @Index(value = {"transaccion_id"}, unique = true),
                @Index(value = {"latitud", "longitud"}),
                @Index(value = {"estado", "id"})
        }
)
public class Lugar {

    /** Identificador local generado por el motor. 0 = aún no persistido. */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Movimiento que originó el pin. {@code null} = lugar independiente.
     * El índice único parcial implícito de Room se declara como
     * {@code unique = true}; SQLite permite múltiples {@code NULL} en un
     * índice único, así que varios pines independientes conviven.
     */
    @ColumnInfo(name = "transaccion_id")
    private Long transaccionId;

    /** Nombre visible del lugar. La UI usa el nombre del tipo si queda vacío. */
    @NonNull
    private String nombre;

    /** Tipo de lugar persistido: supermercado, farmacia, restaurante, etc. */
    @NonNull
    @ColumnInfo(name = "tipo_lugar")
    private String tipoLugar;

    /** Emoji visible en el pin y en la lista. */
    @NonNull
    @ColumnInfo(defaultValue = "📍")
    private String emoji;

    /** Nota libre del lugar ("abre hasta las 21:00"). */
    private String descripcion;

    /** Latitud WGS84 del pin. */
    private double latitud;

    /** Longitud WGS84 del pin. */
    private double longitud;

    /** Estado del pin: {@code abierto} o {@code cerrado}. */
    @NonNull
    @ColumnInfo(defaultValue = "abierto")
    private String estado;

    /** Constructor vacío requerido por Room. */
    public Lugar() {
    }

    /** Constructor completo para insertar/actualizar un lugar. */
    @Ignore
    public Lugar(Long transaccionId, String nombre, String tipoLugar, String emoji,
                 String descripcion, double latitud, double longitud, String estado) {
        this.transaccionId = transaccionId;
        this.nombre = nombre;
        this.tipoLugar = tipoLugar;
        this.emoji = emoji;
        this.descripcion = descripcion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.estado = estado;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(Long transaccionId) {
        this.transaccionId = transaccionId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipoLugar() {
        return tipoLugar;
    }

    public void setTipoLugar(String tipoLugar) {
        this.tipoLugar = tipoLugar;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
