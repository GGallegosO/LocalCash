package com.localcash.domain.model;

import java.math.BigDecimal;

/**
 * Modelo de dominio de un movimiento financiero (ingreso o egreso).
 * <p>
 * <b>NO es la entidad Room</b>: vive en {@code domain/}, es independiente de
 * Android y de Room, y representa el monto como {@link BigDecimal} (`.agent.md`
 * §5.1/§6.10). El monto es siempre positivo: el signo lo determina {@code tipo}.
 * La conversión con la entidad Room ocurre en este único par de métodos
 * ({@link #toEntity()} / {@link #fromEntity()}), con factor 1 (pesos CLP directos).
 * </p>
 * <p>
 * Modelo <b>inmutable</b>: sin setters. Estado nuevo = instancia nueva.
 * </p>
 * <p>
 * Nota de arquitectura: la entidad Room se referencia por nombre completo
 * ({@code com.localcash.data.entities.Transaccion}) porque el simple name es
 * idéntico al de esta clase y Java prohíbe el import duplicado (JLS §7.5.1).
 * No se importa ninguna anotación de Room; este paquete no depende de {@code androidx.*}.
 * </p>
 */
public class Transaccion {

    /** Id persistido (0 = movimiento aún no guardado). */
    private final long id;

    /** Cuenta a la que pertenece el movimiento (FK lógica). */
    private final long cuentaId;

    /** Naturaleza: INGRESO (suma) o EGRESO (resta). */
    private final TipoTransaccion tipo;

    /** Importe en pesos CLP, siempre positivo (BigDecimal para cálculos exactos). */
    private final BigDecimal monto;

    /** Concepto libre ("Supermercado del barrio"). */
    private final String concepto;

    /** Momento del movimiento en epoch millis (UTC). */
    private final long fecha;

    /**
     * Ubicación del movimiento ({@code null} si no está geolocalizado).
     * La regla "ambas o ninguna" y el rango los garantiza {@link Ubicacion}.
     */
    private final Ubicacion ubicacion;

    /**
     * Crea un movimiento sin geolocalización (equivale a {@code ubicacion = null}).
     *
     * @param id       Id persistido o 0 si aún no se ha guardado
     * @param cuentaId Id de la cuenta afectada
     * @param tipo     INGRESO / EGRESO
     * @param monto    Importe positivo en pesos CLP (BigDecimal)
     * @param concepto Concepto libre
     * @param fecha    Epoch millis (UTC)
     */
    public Transaccion(long id, long cuentaId, TipoTransaccion tipo,
                       BigDecimal monto, String concepto, long fecha) {
        this(id, cuentaId, tipo, monto, concepto, fecha, Ubicacion.vacia());
    }

    /**
     * Crea una instancia de movimiento, opcionalmente geolocalizado.
     *
     * @param id        Id persistido o 0 si aún no se ha guardado
     * @param cuentaId  Id de la cuenta afectada
     * @param tipo      INGRESO / EGRESO
     * @param monto     Importe positivo en pesos CLP (BigDecimal)
     * @param concepto  Concepto libre
     * @param fecha     Epoch millis (UTC)
     * @param ubicacion Ubicación WGS84 ya validada, o {@code null} si no está geolocalizado
     */
    public Transaccion(long id, long cuentaId, TipoTransaccion tipo,
                       BigDecimal monto, String concepto, long fecha, Ubicacion ubicacion) {
        this.id = id;
        this.cuentaId = cuentaId;
        this.tipo = tipo;
        this.monto = monto;
        this.concepto = concepto;
        this.fecha = fecha;
        this.ubicacion = ubicacion;
    }

    public long getId() {
        return id;
    }

    public long getCuentaId() {
        return cuentaId;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getConcepto() {
        return concepto;
    }

    public long getFecha() {
        return fecha;
    }

    /**
     * Ubicación del movimiento, o {@code null} si no está geolocalizado.
     *
     * @return la {@link Ubicacion} WGS84, o {@code null}
     */
    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    /**
     * Convierte este modelo de dominio a la entidad Room para persistencia.
     * <p>
     * Único punto de paso {@code BigDecimal → long}: pesos CLP directos (factor 1).
     * {@code longValueExact()} falla con {@link ArithmeticException} si el monto
     * tuviera fracción — los pesos no admiten centavos, debe fallar ruidosamente.
     * </p>
     *
     * @return Entidad Room equivalente
     */
    public com.localcash.data.entities.Transaccion toEntity() {
        Double latitud = ubicacion != null ? ubicacion.getLatitud() : null;
        Double longitud = ubicacion != null ? ubicacion.getLongitud() : null;
        String direccionLugar = ubicacion != null ? ubicacion.getDireccionLugar() : null;
        return new com.localcash.data.entities.Transaccion(
                cuentaId,
                tipo.name(),
                monto.longValueExact(),
                concepto,
                fecha,
                latitud,
                longitud,
                direccionLugar
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
    public static Transaccion fromEntity(com.localcash.data.entities.Transaccion entity) {
        return new Transaccion(
                entity.getId(),
                entity.getCuentaId(),
                TipoTransaccion.valueOf(entity.getTipo()),
                BigDecimal.valueOf(entity.getMonto()),
                entity.getConcepto(),
                entity.getFecha(),
                Ubicacion.crear(entity.getLatitud(), entity.getLongitud(),
                        entity.getDireccionLugar())
        );
    }

    @Override
    public String toString() {
        return "Transaccion{id=" + id
                + ", cuentaId=" + cuentaId
                + ", tipo=" + tipo
                + ", monto=" + monto
                + ", concepto='" + concepto + '\''
                + ", fecha=" + fecha
                + ", ubicacion=" + ubicacion
                + '}';
    }
}
