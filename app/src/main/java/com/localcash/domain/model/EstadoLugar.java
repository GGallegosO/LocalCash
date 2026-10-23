package com.localcash.domain.model;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;

/**
 * Estado de un pin del mapa (dominio cerrado).
 * <p>
 * Cerrar no elimina: el lugar sigue visible con tratamiento atenuado y puede
 * reabrirse. El valor persistido coincide con el {@code CHECK} de
 * {@code LUGARES.estado}.
 * </p>
 */
public enum EstadoLugar {

    /** Pin activo en la lista principal. */
    ABIERTO("abierto"),

    /** Pin atendido o archivado; no aparece en la lista activa. */
    CERRADO("cerrado");

    private final String valor;

    EstadoLugar(String valor) {
        this.valor = valor;
    }

    /**
     * @return valor persistido en {@code LUGARES.estado}
     */
    public String getValor() {
        return valor;
    }

    /**
     * Convierte el valor persistido al enum.
     *
     * @param valor valor de la base de datos
     * @return estado correspondiente
     * @throws BusinessRuleException si el valor no pertenece al dominio
     */
    public static EstadoLugar fromValor(String valor) {
        for (EstadoLugar estado : values()) {
            if (estado.valor.equals(valor)) {
                return estado;
            }
        }
        throw new BusinessRuleException(
                ErrorCode.LUGAR_INVALIDO,
                "Estado de lugar desconocido: " + valor);
    }
}
