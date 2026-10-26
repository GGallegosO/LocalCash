package com.localcash.data.model;

/**
 * Proyección de una parte ya cobrada de un contacto (Feature 012).
 * <p>
 * Alimenta la sección "Historial de pagos" del detalle: muestra el concepto del
 * egreso original, el monto cobrado y la fecha de pago, para que el historial
 * sobreviva cuando ya no quedan deudas pendientes.
 * </p>
 */
public class PagoContacto {

    private long parteId;

    private String descripcion;

    private String fechaPago;

    private long montoAsignado;

    public long getParteId() {
        return parteId;
    }

    public void setParteId(long parteId) {
        this.parteId = parteId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(String fechaPago) {
        this.fechaPago = fechaPago;
    }

    public long getMontoAsignado() {
        return montoAsignado;
    }

    public void setMontoAsignado(long montoAsignado) {
        this.montoAsignado = montoAsignado;
    }
}
