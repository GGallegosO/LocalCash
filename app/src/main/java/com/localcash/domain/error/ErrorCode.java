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
    CUENTA_NO_ENCONTRADA,

    /** Nombre, tipo, emoji, estado o descripción de un lugar fuera de dominio. */
    LUGAR_INVALIDO,

    /** El lugar solicitado no existe en la base de datos. */
    LUGAR_NO_ENCONTRADO,

    /** Coordenada WGS84 fuera de rango o incompleta. */
    UBICACION_INVALIDA,

    /** El usuario denegó el permiso necesario para obtener su ubicación. */
    PERMISO_UBICACION_DENEGADO,

    /** Nombre de contacto vacío o en blanco (solo espacios). */
    CONTACTO_NOMBRE_VACIO,

    /** Ya existe otro contacto con el mismo teléfono (ignorando mayúsculas/minúsculas). */
    CONTACTO_TELEFONO_DUPLICADO,

    /** Ya existe otro contacto con el mismo email (ignorando mayúsculas/minúsculas). */
    CONTACTO_EMAIL_DUPLICADO,

    /** El contacto tiene compras asociadas o deudas pendientes/parciales; no se puede eliminar. */
    CONTACTO_EN_USO,

    /** Un egreso marcado como compartido no tiene ningún contacto (INV-1). */
    GASTO_SIN_CONTACTOS,

    /** La suma de las partes no iguala el monto del egreso (INV-2). */
    REPARTO_NO_CUADRA,

    /** División igual con un monto menor que el número de contactos (INV-2). */
    REPARTO_IGUAL_INSUFICIENTE,

    /** Una parte del reparto tiene monto nulo, cero o negativo. */
    MONTO_PARTE_INVALIDO,

    /** El mismo contacto aparece dos veces en el reparto del mismo egreso. */
    CONTACTO_DUPLICADO,

    /** La parte que se intenta cobrar no existe en la base de datos. */
    PARTE_INEXISTENTE,

    /** La parte ya estaba pagada: no puede volver a cobrarse (INV-3). */
    PARTE_YA_PAGADA
}
