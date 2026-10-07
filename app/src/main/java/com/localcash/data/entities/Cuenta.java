package com.localcash.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa una cuenta financiera del usuario.
 * <p>
 * Tabla: {@code cuentas}. El saldo se almacena como {@code long} en
 * <b>pesos CLP directos</b> (sin centavos, ver {@code .agent.md} §6.10 y
 * {@code data-model.md} §3.2): 1_000_000L = $1.000.000 CLP.
 * </p>
 * <p>
 * Esta entidad vive en la capa {@code data/}; los cálculos de negocio usan
 * el modelo de dominio {@code domain/model/Cuenta} con {@code BigDecimal}.
 * </p>
 */
@Entity(tableName = "cuentas")
public class Cuenta {

    /** Identificador local, generado por el motor (AUTOINCREMENT). */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Nombre visible de la cuenta ("Efectivo", "Ahorro BCC"). */
    private String nombre;

    /** Categoría: EFECTIVO / AHORRO / TARJETA (dominio cerrado). */
    private String tipo;

    /**
     * Saldo actual en pesos CLP (unidad mínima: 1 peso).
     * Nunca {@code double}: la aritmética acumulada exige enteros exactos.
     */
    private long saldo;

    /** Código ISO 4217 de la moneda. Por defecto "CLP". */
    @ColumnInfo(defaultValue = "CLP")
    private String moneda;

    /** Constructor vacío requerido por Room. */
    public Cuenta() {
    }

    /** Constructor completo para crear una cuenta con saldo de apertura. */
    public Cuenta(String nombre, String tipo, long saldo, String moneda) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.saldo = saldo;
        this.moneda = moneda;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public long getSaldo() {
        return saldo;
    }

    public void setSaldo(long saldo) {
        this.saldo = saldo;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }
}
