package com.localcash.domain.model;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;

import java.util.Objects;

/**
 * Coordenada geográfica del dominio (grados WGS84).
 * <p>
 * Value object inmutable. No depende de Android ni de MapLibre: la conversión
 * a {@code LatLng} ocurre en la capa de UI. Se conserva {@code double} porque
 * es el tipo de las coordenadas; el dinero sigue siendo {@code long}/
 * {@code BigDecimal} (`.agent.md` §6.10).
 * </p>
 */
public final class Ubicacion {

    private final double latitud;
    private final double longitud;

    /**
     * Crea una ubicación válida.
     *
     * @param latitud  grados entre -90 y 90
     * @param longitud grados entre -180 y 180
     * @throws BusinessRuleException si alguna coordenada está fuera de rango o
     *                               no es un número finito
     */
    public Ubicacion(double latitud, double longitud) {
        if (!esFinita(latitud) || !esFinita(longitud)
                || latitud < -90.0 || latitud > 90.0
                || longitud < -180.0 || longitud > 180.0) {
            throw new BusinessRuleException(
                    ErrorCode.UBICACION_INVALIDA,
                    "Coordenada fuera de rango WGS84: " + latitud + ", " + longitud);
        }
        this.latitud = latitud;
        this.longitud = longitud;
    }

    private static boolean esFinita(double valor) {
        return !Double.isNaN(valor) && !Double.isInfinite(valor);
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Ubicacion)) {
            return false;
        }
        Ubicacion ubicacion = (Ubicacion) otro;
        return Double.compare(ubicacion.latitud, latitud) == 0
                && Double.compare(ubicacion.longitud, longitud) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitud, longitud);
    }

    @Override
    public String toString() {
        return "Ubicacion{latitud=" + latitud + ", longitud=" + longitud + '}';
    }
}
