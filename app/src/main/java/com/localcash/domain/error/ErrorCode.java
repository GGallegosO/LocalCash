package com.localcash.domain.error;

/**
 * Códigos de error canónicos del dominio de cuentas y transacciones.
 * <p>
 * Dominio cerrado de errores de negocio: la capa {@code ui/} traduce cada
 * código a un mensaje accionable para el usuario (`.agent.md` §5.4). El
 * dominio nunca muestra texto; solo propaga estos códigos.
 * </p>
 * <p>
 * Sin dependencias de Android ni de Room: este paquete pertenece al núcleo
 * de negocio (`.agent.md` §5.1).
 * </p>
 */
public enum ErrorCode {

    /** Nombre de cuenta vacío o en blanco (solo espacios). */
    CUENTA_VACIA,

    /** Monto menor o igual a cero, o nulo: no es un movimiento válido. */
    MONTO_INVALIDO,

    /** El egreso supera el saldo disponible de la cuenta seleccionada. */
    SALDO_INSUFICIENTE,

    /** La cuenta referenciada por el movimiento no existe en la base de datos. */
    CUENTA_NO_ENCONTRADA
}
