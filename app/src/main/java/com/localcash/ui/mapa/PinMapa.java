package com.localcash.ui.mapa;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.localcash.R;

/**
 * Fábrica del icono de pin del mapa (Feature 009, T12; Feature 010, Fase 2).
 * <p>
 * Centraliza el color de los pines en dos recursos del contrato visual:
 * {@link R.color#mapa_pin_movimiento} para movimientos y
 * {@link R.color#mapa_pin_compra} para lugares de compra. Tener un solo punto
 * de tinte evita que un color quede hardcodeado o divergente entre el selector
 * ({@code MapaPickerActivity}) y el mapa de lectura.
 * </p>
 */
public final class PinMapa {

    private PinMapa() {
        // Clase utilitaria: no instanciable.
    }

    /**
     * Devuelve el icono del pin de un movimiento, teñido con
     * {@link R.color#mapa_pin_movimiento}.
     *
     * @param context contexto para resolver el drawable y el color
     * @return drawable del pin listo para un {@code Marker}, o {@code null} si
     *         el recurso no existe
     */
    public static Drawable iconoMovimiento(Context context) {
        return iconoTintado(context, R.color.mapa_pin_movimiento);
    }

    /**
     * Devuelve el icono del pin de un lugar de compra, teñido con
     * {@link R.color#mapa_pin_compra} (Feature 010, Fase 2).
     *
     * @param context contexto para resolver el drawable y el color
     * @return drawable del pin listo para un {@code Marker}, o {@code null} si
     *         el recurso no existe
     */
    public static Drawable iconoCompra(Context context) {
        return iconoTintado(context, R.color.mapa_pin_compra);
    }

    /**
     * Tiñe el drawable base del pin con un color del contrato de recursos.
     *
     * @param context    contexto para resolver drawable y color
     * @param colorResId recurso de color del pin
     * @return drawable teñido, o {@code null} si el drawable no existe
     */
    private static Drawable iconoTintado(Context context, int colorResId) {
        Drawable icono = ContextCompat.getDrawable(context, R.drawable.ic_mapa_pin);
        if (icono == null) {
            return null;
        }
        Drawable tintado = DrawableCompat.wrap(icono).mutate();
        DrawableCompat.setTint(tintado, ContextCompat.getColor(context, colorResId));
        return tintado;
    }
}
