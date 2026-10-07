package com.localcash.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa un movimiento financiero (ingreso o egreso).
 * <p>
 * Tabla: {@code transacciones}. El monto se almacena como {@code long} en
 * <b>pesos CLP directos</b> (sin centavos, ver {@code .agent.md} §6.10):
 * 50_000L = $50.000 CLP. El monto es <b>siempre positivo</b>: el signo lo
 * determina el campo {@code tipo}.
 * </p>
 * <p>
 * FK {@code cuentaId → cuentas.id} con {@code ON DELETE CASCADE}: borrar una
 * cuenta elimina sus movimientos (no puede existir historial huérfano).
 * Room exige un índice sobre la columna hija de la clave foránea, por eso
 * {@code cuentaId} se declara con {@code @Index}.
 * </p>
 */
@Entity(
        tableName = "transacciones",
        foreignKeys = @ForeignKey(
                entity = Cuenta.class,
                parentColumns = "id",
                childColumns = "cuentaId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("cuentaId")}
)
public class Transaccion {

    /** Identificador local, generado por el motor (AUTOINCREMENT). */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Cuenta afectada. Sin cuenta no hay movimiento (NOT NULL implícito por FK). */
    private long cuentaId;

    /** Naturaleza del movimiento: INGRESO / EGRESO (dominio cerrado). */
    private String tipo;

    /**
     * Importe en pesos CLP, siempre positivo (unidad mínima: 1 peso).
     * Nunca {@code double}: el saldo es una suma acumulada en IEEE 754 deriva.
     */
    private long monto;

    /** Concepto libre ("Supermercado del barrio"). */
    private String concepto;

    /** Momento del movimiento en epoch millis (timestamp UTC). */
    private long fecha;

    /** Constructor vacío requerido por Room. */
    public Transaccion() {
    }

    /** Constructor completo para registrar un movimiento. */
    @Ignore
    public Transaccion(long cuentaId, String tipo, long monto, String concepto, long fecha) {
        this.cuentaId = cuentaId;
        this.tipo = tipo;
        this.monto = monto;
        this.concepto = concepto;
        this.fecha = fecha;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public long getMonto() {
        return monto;
    }

    public void setMonto(long monto) {
        this.monto = monto;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }
}
