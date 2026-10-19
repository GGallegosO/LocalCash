package com.localcash.ui.mapa;

import androidx.annotation.NonNull;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

/**
 * Controlador de la pestaña "Mapa" en <b>modo lectura</b>
 * (Feature 009, T10-T12).
 * <p>
 * Encapsula todo lo específico del {@link MapView} de la pestaña: monta el
 * {@link TeselasLocales TileSource local y offline}, fija el encuadre inicial y
 * gestiona el zoom. Mantener esta lógica aquí permite que {@code CuentasActivity}
 * (el controlador de la pantalla) solo orqueste: crear el controlador, enlazar
 * los botones de zoom y entregarle la lista de movimientos a dibujar.
 * </p>
 * <p>
 * El mapa se centra en Santiago de Chile (área cubierta por las teselas
 * preempaquetadas, zoom 10-14). Hasta que T0 empaquete las teselas se verá un
 * lienzo navegable vacío: no es un error.
 * </p>
 */
public class MapaLecturaController {

    /** Latitud de Santiago de Chile (centro inicial del mapa de lectura). */
    private static final double LATITUD_SANTIAGO = -33.4489;

    /** Longitud de Santiago de Chile (centro inicial del mapa de lectura). */
    private static final double LONGITUD_SANTIAGO = -70.6693;

    /** Zoom inicial (dentro del rango empaquetado 10-14). */
    private static final double ZOOM_INICIAL = 12d;

    /** Mapa gestionado por este controlador (no {@code null}). */
    private final MapView mapa;

    /**
     * Crea el controlador y configura el mapa (teselas locales, encuadre y zoom).
     *
     * @param mapa {@link MapView} de la pestaña (no {@code null})
     */
    public MapaLecturaController(@NonNull MapView mapa) {
        this.mapa = mapa;
        configurar();
    }

    /**
     * Prepara el mapa: teselas locales offline, controles táctiles, encuadre
     * inicial en Santiago y rango de zoom empaquetado.
     */
    private void configurar() {
        TeselasLocales.configurar(mapa.getContext(), mapa);
        mapa.setMultiTouchControls(true);
        mapa.getController().setZoom(ZOOM_INICIAL);
        mapa.getController().setCenter(new GeoPoint(LATITUD_SANTIAGO, LONGITUD_SANTIAGO));
    }

    /**
     * Acerca o aleja un nivel de zoom respetando el rango del {@code MapView}.
     *
     * @param delta {@code +1} para acercar, {@code -1} para alejar
     */
    public void cambiarZoom(int delta) {
        double zoomActual = mapa.getZoomLevelDouble();
        mapa.getController().setZoom(zoomActual + delta);
    }
}
