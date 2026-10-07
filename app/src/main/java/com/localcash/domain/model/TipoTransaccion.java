package com.localcash.domain.model;

/**
 * Naturaleza de un movimiento financiero (dominio cerrado).
 * <p>
 * Valores alineados con {@code data-model.md} §3.4: no existe
 * {@code transferencia} (fuera de alcance de la misión). El nombre del enum es
 * exactamente el String persistido en la entidad Room {@code Transaccion.tipo}.
 * </p>
 */
public enum TipoTransaccion {

    /** Entrada de dinero: aumenta el saldo de la cuenta. */
    INGRESO,

    /** Salida de dinero: disminuye el saldo de la cuenta. */
    EGRESO
}
