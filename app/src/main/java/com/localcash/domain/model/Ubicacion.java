package com.localcash.domain.model;

import java.util.Objects;

/**
 * Value object inmutable de una ubicación geográfica (WGS84) asociada a un
 * movimiento.
 * <p>
 * Vive en {@code domain/}, es independiente de Android y de Room. Encapsula la
 * <b>única</b> validación de coordenadas del proyecto, para que ni la capa de
 * datos ni la de presentación la reimplementen:
 * </p>
 * <ul>
 *   <li><b>Ambas o ninguna:</b> {@code latitud} y {@code longitud} van juntas o
 *       no van (regla del {@code CHECK} de {@code data-model.md}).</li>
 *   <li><b>Rango:</b> latitud en {@code [-90, 90]}, longitud en {@code [-180, 180]}.</li>
 * </ul>
 * <p>
 * La <b>ausencia</b> de ubicación no se modela con una instancia "vacía" sino
 * con {@code null}: {@link #crear(Double, Double, String)} y {@link #vacia()}
 * devuelven {@code null} cuando el movimiento no está geolocalizado. Así la
 * conversión a la entidad Room es directa (columnas {@code NULL}).
 * </p>
 */
public final class Ubicacion {

    /** Latitud mínima válida en grados. */
    public static final double LATITUD_MINIMA = -90.0;

    /** Latitud máxima válida en grados. */
    public static final double LATITUD_MAXIMA = 90.0;

    /** Longitud mínima válida en grados. */
    public static final double LONGITUD_MINIMA = -180.0;

    /** Longitud máxima válida en grados. */
    public static final double LONGITUD_MAXIMA = 180.0;

    /** Latitud WGS84 en grados. */
    private final double latitud;

    /** Longitud WGS84 en grados. */
    private final double longitud;

    /** Dirección legible del lugar (descriptiva, opcional); puede ser {@code null}. */
    private final String direccionLugar;

    private Ubicacion(double latitud, double longitud, String direccionLugar) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.direccionLugar = direccionLugar;
    }

    /**
     * Factory que valida e instancia una ubicación, o devuelve {@code null} si
     * no hay coordenadas.
     * <p>
     * Si {@code latitud} y {@code longitud} son {@code null} a la vez, el
     * movimiento no está geolocalizado y se devuelve {@code null} (equivalente a
     * {@link #vacia()}). Si solo una viene con valor, se incumple la regla
     * "ambas o ninguna" y se lanza {@link IllegalArgumentException}.
     * </p>
     *
     * @param latitud        Latitud en grados, o {@code null}
     * @param longitud       Longitud en grados, o {@code null}
     * @param direccionLugar Dirección legible opcional, o {@code null}
     * @return Una {@link Ubicacion} validada, o {@code null} si no hay coordenadas
     * @throws IllegalArgumentException si solo una coordenada es {@code null},
     *                                  o si alguna está fuera de rango (incluye {@code NaN})
     */
    public static Ubicacion crear(Double latitud, Double longitud, String direccionLugar) {
        if (latitud == null && longitud == null) {
            return vacia();
        }
        if (latitud == null || longitud == null) {
            throw new IllegalArgumentException(
                    "Ubicación incompleta: se requiere latitud y longitud, o ninguna (ambas o ninguna)");
        }
        // La comparación con NaN es siempre falsa, así que el rango también rechaza NaN.
        if (!(latitud >= LATITUD_MINIMA && latitud <= LATITUD_MAXIMA)) {
            throw new IllegalArgumentException(
                    "Latitud fuera de rango [-90, 90]: " + latitud);
        }
        if (!(longitud >= LONGITUD_MINIMA && longitud <= LONGITUD_MAXIMA)) {
            throw new IllegalArgumentException(
                    "Longitud fuera de rango [-180, 180]: " + longitud);
        }
        return new Ubicacion(latitud, longitud, direccionLugar);
    }

    /**
     * Representa "sin ubicación".
     *
     * @return siempre {@code null}: la ausencia de ubicación se modela con la
     *         referencia nula, que la entidad Room traduce a columnas {@code NULL}
     */
    public static Ubicacion vacia() {
        return null;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public String getDireccionLugar() {
        return direccionLugar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ubicacion)) {
            return false;
        }
        Ubicacion otra = (Ubicacion) o;
        return Double.compare(latitud, otra.latitud) == 0
                && Double.compare(longitud, otra.longitud) == 0
                && Objects.equals(direccionLugar, otra.direccionLugar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitud, longitud, direccionLugar);
    }

    @Override
    public String toString() {
        return "Ubicacion{latitud=" + latitud
                + ", longitud=" + longitud
                + ", direccionLugar='" + direccionLugar + '\''
                + '}';
    }
}
