package com.localcash.data.model;

/**
 * Proyección de solo lectura del resumen de deuda por contacto.
 * <p>
 * No es una entidad: es el resultado de una consulta agregada sobre
 * {@code GASTOS_COMPARTIDOS} que suma las partes pendientes de cada contacto
 * ({@code data-model.md} §3.11). Vive en {@code data/} porque describe la forma
 * de una consulta, no una regla de negocio.
 * </p>
 * <p>
 * {@code totalPendiente} va en pesos CLP directos ({@code long}): es una suma de
 * montos enteros y nunca debe pasar por {@code double}.
 * </p>
 */
public class ResumenDeuda {

    /** Id del contacto deudor. */
    private long contactoId;

    /** Nombre visible del contacto (el que la UI muestra en el resumen). */
    private String nombreContacto;

    /** Suma de las partes {@code pendiente} de ese contacto, en pesos CLP. */
    private long totalPendiente;

    /** Constructor vacío requerido por Room para poblar la proyección. */
    public ResumenDeuda() {
    }

    public long getContactoId() {
        return contactoId;
    }

    public void setContactoId(long contactoId) {
        this.contactoId = contactoId;
    }

    public String getNombreContacto() {
        return nombreContacto;
    }

    public void setNombreContacto(String nombreContacto) {
        this.nombreContacto = nombreContacto;
    }

    public long getTotalPendiente() {
        return totalPendiente;
    }

    public void setTotalPendiente(long totalPendiente) {
        this.totalPendiente = totalPendiente;
    }
}