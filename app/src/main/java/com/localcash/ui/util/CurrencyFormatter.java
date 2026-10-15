package com.localcash.ui.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilidad de presentación para formatear montos en pesos chilenos (CLP).
 * <p>
 * Vive en {@code ui/} porque es una preocupación de presentación, no una regla de
 * negocio: los cálculos de dinero ({@link BigDecimal}) permanecen en {@code domain/}.
 * No depende del Android SDK; solo de {@link Locale} y {@link NumberFormat}.
 * </p>
 * <p>
 * Los montos se expresan en pesos CLP directos (sin centavos), conforme a
 * {@code .agent.md} §6.10 y {@code data-model.md} §1.4.
 * </p>
 */
public final class CurrencyFormatter {

    /**
     * Locale local del producto: pesos chilenos.
     * <p>
     * Se obtiene con {@link Locale#forLanguageTag(String)} ({@code "es-CL"}) en lugar de
     * {@code new Locale("es", "CL")}: el constructor está deprecado desde Java 19 y el
     * daemon de Gradle compila con JDK 21, lo que emitía la nota de API deprecada que el
     * DoD prohíbe. El locale resultante es el mismo (idioma {@code es}, región {@code CL}).
     * </p>
     */
    private static final Locale LOCALE_CL = Locale.forLanguageTag("es-CL");

    /** Sufijo explícito de moneda cuando el formato del locale no lo incluye. */
    private static final String SUFIJO_CLP = " CLP";

    /** Monto de respaldo cuando no hay valor que formatear. */
    private static final long CERO_PESOS = 0L;

    private CurrencyFormatter() {
        // Clase utilitaria: no instanciable.
    }

    /**
     * Formatea un monto en pesos CLP.
     *
     * @param pesosCLP Monto en pesos CLP (ej. {@code 750000L}); {@code 0} produce {@code "$0"}
     * @return Monto formateado con el símbolo del locale (ej. {@code "$750.000"})
     */
    public static String format(long pesosCLP) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(LOCALE_CL);
        return formato.format(pesosCLP);
    }

    /**
     * Formatea un monto de dominio en pesos CLP.
     * <p>
     * Un {@link BigDecimal} nulo se trata como {@code 0}. Un valor con fracción lanza
     * {@link ArithmeticException} en lugar de truncarse en silencio (decisión #18 de
     * {@code doc/007-cuentas.md}: una fracción en CLP es error de programación).
     * </p>
     *
     * @param pesosCLP Monto en pesos CLP; {@code null} equivale a cero
     * @return Monto formateado con el símbolo del locale
     */
    public static String format(BigDecimal pesosCLP) {
        if (pesosCLP == null) {
            return format(CERO_PESOS);
        }
        return format(pesosCLP.longValueExact());
    }

    /**
     * Formatea un monto en pesos CLP incluyendo el sufijo {@code " CLP"} cuando el
     * formato del locale no lo incluye ya.
     *
     * @param pesosCLP Monto en pesos CLP (ej. {@code 750000L})
     * @return Monto formateado con moneda explícita (ej. {@code "$750.000 CLP"})
     */
    public static String formatWithCurrency(long pesosCLP) {
        String formateado = format(pesosCLP);
        return formateado.contains("CLP") ? formateado : formateado + SUFIJO_CLP;
    }
}
