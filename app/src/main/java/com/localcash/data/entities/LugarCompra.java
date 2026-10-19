package com.localcash.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa un Lugar de Compra geolocalizado.
 * <p>
 * Tabla: {@code lugar_compra}. Se crea desde la pestaña "Mapa" con una pulsación
 * larga y agrupa una lista de {@link ItemCompra} (relación uno-a-muchos).
 * </p>
 * <p>
 * El {@code gastoPromedio} es dinero: se almacena como {@link Long} en
 * <b>pesos CLP directos</b> (unidad mínima 1 peso, sin centavos), nunca como
 * {@code double} ({@code .agent.md} §6.10). Es <b>opcional</b>: {@code null}
 * representa "sin gasto estimado" y se persiste como {@code NULL}. La
 * validación de negocio (tipo obligatorio, gasto &ge; 0 cuando exista) vive en
 * la capa de dominio, no aquí.
 * </p>
 */
@Entity(tableName = "lugar_compra")
public class LugarCompra {

    /** Identificador local, generado por el motor (AUTOINCREMENT). */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    /** Latitud WGS84 del lugar (grados). */
    @ColumnInfo(name = "latitud")
    private double latitud;

    /** Longitud WGS84 del lugar (grados). */
    @ColumnInfo(name = "longitud")
    private double longitud;

    /** Tipo de lugar: Supermercado, Farmacia, Ferretería u otro definido por el usuario. */
    @NonNull
    @ColumnInfo(name = "tipoLugar")
    private String tipoLugar;

    /**
     * Gasto promedio estimado, en pesos CLP (unidad mínima: 1 peso), o
     * {@code null} si el usuario no lo ingresó.
     */
    @ColumnInfo(name = "gastoPromedio")
    private Long gastoPromedio;

    /** Momento de creación en epoch millis (timestamp UTC). */
    @ColumnInfo(name = "fechaCreacion")
    private long fechaCreacion;

    /** Constructor vacío requerido por Room. */
    public LugarCompra() {
    }

    /** Constructor completo para crear un lugar de compra. */
    @Ignore
    public LugarCompra(double latitud, double longitud, String tipoLugar,
                       Long gastoPromedio, long fechaCreacion) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.tipoLugar = tipoLugar;
        this.gastoPromedio = gastoPromedio;
        this.fechaCreacion = fechaCreacion;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getTipoLugar() {
        return tipoLugar;
    }

    public void setTipoLugar(String tipoLugar) {
        this.tipoLugar = tipoLugar;
    }

    public Long getGastoPromedio() {
        return gastoPromedio;
    }

    public void setGastoPromedio(Long gastoPromedio) {
        this.gastoPromedio = gastoPromedio;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
