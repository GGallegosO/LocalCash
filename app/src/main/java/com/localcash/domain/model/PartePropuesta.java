package com.localcash.domain.model;

/**
 * Propuesta de una parte de un egreso compartido antes de persistirse.
 * <p>
 * Es el resultado del reparto en {@code GastoCompartidoService} y la entrada de
 * la división personalizada: solo contiene el contacto y el monto que le toca,
 * sin id ni estado, porque todavía no es una fila de la base de datos.
 * </p>
 * <p>
 * Inmutable y sin dependencias de Android ni Room (`.agent.md` §5.1). El monto
 * va en <b>pesos CLP directos</b> ({@code long}, nunca {@code double}).
 * </p>
 */
public final class PartePropuesta {

    private final long contactoId;

    private final long montoAsignado;

    /**
     * Crea una parte propuesta.
     *
     * @param contactoId    id del contacto deudor
     * @param montoAsignado parte del egreso en pesos CLP (positiva)
     */
    public PartePropuesta(long contactoId, long montoAsignado) {
        this.contactoId = contactoId;
        this.montoAsignado = montoAsignado;
    }

    public long getContactoId() {
        return contactoId;
    }

    public long getMontoAsignado() {
        return montoAsignado;
    }

    @Override
    public String toString() {
        return "PartePropuesta{contactoId=" + contactoId
                + ", montoAsignado=" + montoAsignado
                + '}';
    }
}