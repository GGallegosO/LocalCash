package com.localcash.domain.model;

import java.math.BigDecimal;

/**
 * Modelo de dominio de una cuenta financiera (núcleo de negocio).
 * <p>
 * <b>NO es la entidad Room</b>: vive en {@code domain/}, es independiente de
 * Android y de Room, y representa el saldo como {@link BigDecimal} para
 * cálculos precisos (`.agent.md` §5.1/§6.10). La persistencia usa {@code long}
 * en pesos CLP; la conversión ocurre en este único par de métodos
 * ({@link #toEntity()} / {@link #fromEntity}), con factor 1 (pesos directos).
 * </p>
 * <p>
 * Modelo <b>inmutable</b>: sin setters. Un estado nuevo se crea como una
 * instancia nueva (así ninguna referencia compartida puede mutar un saldo
 * ya calculado).
 * </p>
 * <p>
 * Nota de arquitectura: los métodos de conversión referencian la entidad Room
 * por nombre completo ({@code com.localcash.data.entities.Cuenta}) porque el
 * simple name es idéntico al de esta clase y Java prohíbe el import duplicado
 * (JLS §7.5.1). No se importa ninguna anotación de Room ({@code @Entity},
 * {@code @Dao}, etc.); este paquete no depende de {@code androidx.*}.
 * </p>
 */
public class Cuenta {

    /** Única moneda de la feature: pesos chilenos (sin conversión de divisa). */
    public static final String MONEDA_CLP = "CLP";

    /** Id persistido (0 = cuenta aún no guardada). */
    private final long id;

    /** Nombre visible ("Efectivo", "Ahorro BCC"). */
    private final String nombre;

    /** Categoría de la cuenta (dominio cerrado). */
    private final TipoCuenta tipo;

    /** Saldo en pesos CLP como BigDecimal para cálculos exactos. */
    private final BigDecimal saldo;

    /**
     * Crea una instancia de cuenta.
     *
     * @param id     Id persistido o 0 si aún no se ha guardado
     * @param nombre Nombre visible
     * @param tipo   Categoría (EFECTIVO / AHORRO / TARJETA)
     * @param saldo  Saldo en pesos CLP (BigDecimal)
     */
    public Cuenta(long id, String nombre, TipoCuenta tipo, BigDecimal saldo) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.saldo = saldo;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoCuenta getTipo() {
        return tipo;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    /**
     * Convierte este modelo de dominio a la entidad Room para persistencia.
     * <p>
     * Único punto de paso {@code BigDecimal → long}: pesos CLP directos
     * (factor 1). {@code longValueExact()} falla con {@link ArithmeticException}
     * si el saldo tuviera fracción — los pesos no admiten centavos, así que una
     * fracción es error de programación y debe fallar ruidosamente.
     * </p>
     *
     * @return Entidad Room equivalente (moneda CLP)
     */
    public com.localcash.data.entities.Cuenta toEntity() {
        return new com.localcash.data.entities.Cuenta(
                nombre,
                tipo.name(),
                saldo.longValueExact(),
                MONEDA_CLP
        );
    }

    /**
     * Convierte una entidad Room a este modelo de dominio.
     * <p>
     * Único punto de paso {@code long → BigDecimal}: factor 1 (pesos directos).
     * </p>
     *
     * @param entity Entidad Room leída de la base de datos
     * @return Modelo de dominio equivalente
     */
    public static Cuenta fromEntity(com.localcash.data.entities.Cuenta entity) {
        return new Cuenta(
                entity.getId(),
                entity.getNombre(),
                TipoCuenta.valueOf(entity.getTipo()),
                BigDecimal.valueOf(entity.getSaldo())
        );
    }

    @Override
    public String toString() {
        return "Cuenta{id=" + id
                + ", nombre='" + nombre + '\''
                + ", tipo=" + tipo
                + ", saldo=" + saldo + ' ' + MONEDA_CLP
                + '}';
    }
}
