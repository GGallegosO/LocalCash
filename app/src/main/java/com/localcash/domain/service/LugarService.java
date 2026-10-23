package com.localcash.domain.service;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.EstadoLugar;
import com.localcash.domain.model.ItemLugar;
import com.localcash.domain.model.Lugar;
import com.localcash.domain.model.Ubicacion;

/**
 * Reglas de negocio de los lugares/pines del mapa.
 * <p>
 * Lógica pura, sin Android ni Room (`.agent.md` §5.1). La UI y los ViewModels
 * orquestan; este servicio decide si un lugar, un elemento o una transición de
 * estado es válida.
 * </p>
 */
public final class LugarService {

    private LugarService() {
    }

    /**
     * Valida un pin antes de persistirlo.
     *
     * @param lugar lugar a validar (no nulo)
     * @throws BusinessRuleException con {@link ErrorCode#LUGAR_INVALIDO} si falta
     *                               nombre, tipo, emoji, ubicación o estado
     */
    public static void validarLugar(Lugar lugar) {
        if (lugar == null) {
            throw new BusinessRuleException(ErrorCode.LUGAR_INVALIDO, "Lugar nulo");
        }
        if (lugar.getNombre() == null || lugar.getNombre().trim().isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.LUGAR_INVALIDO, "El nombre del lugar está vacío");
        }
        if (lugar.getTipo() == null) {
            throw new BusinessRuleException(
                    ErrorCode.LUGAR_INVALIDO, "El tipo de lugar es obligatorio");
        }
        if (lugar.getEmoji() == null || lugar.getEmoji().trim().isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.LUGAR_INVALIDO, "El emoji del lugar está vacío");
        }
        if (lugar.getUbicacion() == null) {
            throw new BusinessRuleException(
                    ErrorCode.UBICACION_INVALIDA, "El lugar no tiene coordenadas");
        }
        if (lugar.getEstado() == null) {
            throw new BusinessRuleException(
                    ErrorCode.LUGAR_INVALIDO, "El estado del lugar es obligatorio");
        }
    }

    /**
     * Valida un elemento de la lista de cosas.
     *
     * @param item elemento a validar
     * @throws BusinessRuleException con {@link ErrorCode#LUGAR_INVALIDO} si la
     *                               descripción está vacía o no tiene lugar
     */
    public static void validarItem(ItemLugar item) {
        if (item == null) {
            throw new BusinessRuleException(ErrorCode.LUGAR_INVALIDO, "Item nulo");
        }
        if (item.getLugarId() <= 0) {
            throw new BusinessRuleException(
                    ErrorCode.LUGAR_INVALIDO, "El item no tiene lugar asociado");
        }
        if (item.getDescripcion() == null || item.getDescripcion().trim().isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.LUGAR_INVALIDO, "La descripción del item está vacía");
        }
    }

    /**
     * Valida si un pin puede cambiar de posición.
     * <p>
     * Los pines vinculados a un movimiento conservan la coordenada del
     * movimiento; solo los independientes se pueden mover.
     * </p>
     *
     * @param lugar         pin que se quiere mover
     * @param nuevaUbicacion nueva coordenada
     * @throws BusinessRuleException si el pin está vinculado o la ubicación es nula
     */
    public static void validarMovimiento(Lugar lugar, Ubicacion nuevaUbicacion) {
        if (lugar == null) {
            throw new BusinessRuleException(ErrorCode.LUGAR_INVALIDO, "Lugar nulo");
        }
        if (lugar.estaVinculadoAMovimiento()) {
            throw new BusinessRuleException(
                    ErrorCode.LUGAR_INVALIDO,
                    "Un pin vinculado a un movimiento no puede cambiar de posición");
        }
        if (nuevaUbicacion == null) {
            throw new BusinessRuleException(
                    ErrorCode.UBICACION_INVALIDA, "La nueva ubicación es nula");
        }
    }

    /**
     * Calcula el estado opuesto para cerrar o reabrir un pin.
     *
     * @param actual estado actual
     * @return {@link EstadoLugar#CERRADO} si estaba abierto, o
     * {@link EstadoLugar#ABIERTO} si estaba cerrado
     */
    public static EstadoLugar alternarEstado(EstadoLugar actual) {
        if (actual == null) {
            throw new BusinessRuleException(
                    ErrorCode.LUGAR_INVALIDO, "El estado actual es nulo");
        }
        return actual == EstadoLugar.ABIERTO ? EstadoLugar.CERRADO : EstadoLugar.ABIERTO;
    }
}
