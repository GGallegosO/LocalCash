package com.localcash.domain.model;

/**
 * Modo en que el usuario decide dividir un egreso compartido.
 * <p>
 * {@link #IGUAL} reparte el total al peso entre todos los contactos
 * ({@code GastoCompartidoService.dividirIgual}); {@link #PERSONALIZADA} usa un
 * monto por contacto escrito por el usuario
 * ({@code GastoCompartidoService.validarPersonalizado}). Es estado de la
 * pantalla, no una columna: {@code GASTOS_COMPARTIDOS} solo guarda el resultado
 * ({@code monto_asignado}).
 * </p>
 */
public enum ModoDivision {

    /** Partes iguales al peso, con el residuo de a 1 CLP. */
    IGUAL,

    /** Monto individual por contacto, decidido por el usuario. */
    PERSONALIZADA
}