package com.localcash.ui.mapa;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

/**
 * Marcador de un lugar de compra con long press propio (Feature 010, Fase 3).
 * <p>
 * osmdroid no ofrece un listener de long press específico para marcadores:
 * {@link Marker#onLongPress(MotionEvent, MapView)} es el punto de extensión.
 * Este marcador comprueba que el evento caiga sobre su icono
 * ({@link #hitTest(MotionEvent, MapView)}) y, si es así, notifica al listener y
 * consume el evento. Así el long press sobre un pin abre el detalle y no el
 * diálogo de creación del mapa vacío.
 * </p>
 */
final class MarcadorLugarCompra extends Marker {

    /** Callback de long press sobre un lugar de compra. */
    interface OnLongPressListener {

        /**
         * @param lugarCompraId id del lugar presionado
         */
        void onLugarCompraLongPress(long lugarCompraId);
    }

    private final long lugarCompraId;
    private final OnLongPressListener listener;

    MarcadorLugarCompra(@NonNull MapView mapView, long lugarCompraId,
                        @Nullable OnLongPressListener listener) {
        super(mapView);
        this.lugarCompraId = lugarCompraId;
        this.listener = listener;
    }

    @Override
    public boolean onLongPress(MotionEvent event, MapView mapView) {
        if (listener != null && hitTest(event, mapView)) {
            listener.onLugarCompraLongPress(lugarCompraId);
            return true;
        }
        return super.onLongPress(event, mapView);
    }
}
