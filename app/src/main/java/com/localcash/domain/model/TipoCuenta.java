package com.localcash.domain.model;

/**
 * Categoría de una cuenta (dominio cerrado).
 * <p>
 * Valores alineados con el diccionario de entidades de {@code data-model.md} §3.2.
 * El nombre del enum ({@code name()}) es exactamente el String persistido en la
 * entidad Room {@code Cuenta.tipo}, por lo que la conversión es directa.
 * </p>
 */
public enum TipoCuenta {

    /** Dinero en mano: billetera, caja chica. */
    EFECTIVO,

    /** Cuenta de ahorro (banco, alcancía). */
    AHORRO,

    /** Medio de pago con límite (crédito/débito). */
    TARJETA
}
