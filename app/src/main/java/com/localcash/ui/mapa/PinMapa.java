package com.localcash.ui.mapa;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.localcash.R;

/**
 * Fábrica del icono de pin del mapa (Feature 009, T12).
 * <p>
 * Centraliza el color de los pines de <b>movimientos</b> en un único recurso
 * ({@link R.color#mapa_pin_movimiento}): es el contrato de color que la lista de
 * compras (Feature 010) reutilizará con otro color, sin redefinir este. Tener un
 * solo punto de tinte evita que un color quede hardcodeado o divergente entre el
 * selector ({@code MapaPickerActivity}) y el mapa de lectura.
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
        Drawable icono = ContextCompat.getDrawable(context, R.drawable.ic_mapa_pin);
        if (icono == null) {
            return null;
        }
        Drawable tintado = DrawableCompat.wrap(icono).mutate();
        DrawableCompat.setTint(tintado, ContextCompat.getColor(context, R.color.mapa_pin_movimiento));
        return tintado;
    }
}
