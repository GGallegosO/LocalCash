package com.localcash.domain.model;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;

/**
 * Tipo de lugar de un pin (dominio cerrado).
 * <p>
 * El valor persistido es minúscula y coincide con el {@code CHECK} de
 * {@code LUGARES.tipo_lugar} en {@code data-model.md} §3.9. Cada tipo aporta
 * un emoji por defecto; el usuario puede elegir otro de la paleta.
 * </p>
 */
public enum TipoLugar {

    /** Compra de alimentos y productos de limpieza. */
    SUPERMERCADO("supermercado", "🛒"),

    /** Medicamentos y productos de salud. */
    FARMACIA("farmacia", "💊"),

    /** Comida preparada. */
    RESTAURANTE("restaurante", "🍽️"),

    /** Materiales y herramientas. */
    FERRETERIA("ferreteria", "🛠️"),

    /** Pan y productos de panadería. */
    PANADERIA("panaderia", "🥖"),

    /** Café o salida breve. */
    CAFETERIA("cafeteria", "☕"),

    /** Trámites y servicios financieros. */
    BANCO("banco", "🏦"),

    /** Cualquier otro lugar. */
    OTRO("otro", "📍");

    private final String valor;
    private final String emojiPorDefecto;

    TipoLugar(String valor, String emojiPorDefecto) {
        this.valor = valor;
        this.emojiPorDefecto = emojiPorDefecto;
    }

    /**
     * @return valor persistido en {@code LUGARES.tipo_lugar}
     */
    public String getValor() {
        return valor;
    }

    /**
     * @return emoji sugerido para el pin
     */
    public String getEmojiPorDefecto() {
        return emojiPorDefecto;
    }

    /**
     * Convierte el valor persistido al enum.
     *
     * @param valor valor de la base de datos
     * @return tipo correspondiente
     * @throws BusinessRuleException si el valor no pertenece al dominio
     */
    public static TipoLugar fromValor(String valor) {
        for (TipoLugar tipo : values()) {
            if (tipo.valor.equals(valor)) {
                return tipo;
            }
        }
        throw new BusinessRuleException(
                ErrorCode.LUGAR_INVALIDO,
                "Tipo de lugar desconocido: " + valor);
    }
}
