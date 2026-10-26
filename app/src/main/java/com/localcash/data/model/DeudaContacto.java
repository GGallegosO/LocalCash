package com.localcash.data.model;

/**
 * Proyección de una parte pendiente con los datos del egreso que la originó
 * (Feature 012, T13). Solo lectura: alimenta el detalle de deudas del contacto.
 */
public class DeudaContacto {

    private String descripcion;

    private long fechaMillis;

    private long montoAsignado;

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public long getFechaMillis() {
        return fechaMillis;
    }

    public void setFechaMillis(long fechaMillis) {
        this.fechaMillis = fechaMillis;
    }

    public long getMontoAsignado() {
        return montoAsignado;
    }

    public void setMontoAsignado(long montoAsignado) {
        this.montoAsignado = montoAsignado;
    }
}