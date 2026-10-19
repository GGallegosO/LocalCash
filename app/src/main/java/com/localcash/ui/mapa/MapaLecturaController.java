package com.localcash.ui.mapa;

import androidx.annotation.NonNull;

import com.localcash.R;
import com.localcash.domain.model.TipoTransaccion;
import com.localcash.domain.model.Transaccion;
import com.localcash.domain.model.Ubicacion;
import com.localcash.ui.util.CurrencyFormatter;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la pestaña "Mapa" en <b>modo lectura</b>
 * (Feature 009, T10-T12).
 * <p>
 * Encapsula todo lo específico del {@link MapView} de la pestaña: monta el
 * {@link TeselasLocales TileSource local y offline}, fija el encuadre inicial,
 * gestiona el zoom y dibuja un pin por movimiento geolocalizado. Mantener esta
 * lógica aquí permite que {@code CuentasActivity} (el controlador de la pantalla)
 * solo orqueste: crear el controlador, enlazar los botones de zoom y entregarle
 * la lista de movimientos a dibujar.
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

    /**
     * Formato de fecha del popup (mismo criterio que el historial). No es
     * {@code static}: capturar {@code Locale.getDefault()} al cargar la clase
     * dispararía la advertencia {@code ConstantLocale} de lint.
     */
    private final SimpleDateFormat formatoFecha =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    /** Mapa gestionado por este controlador (no {@code null}). */
    private final MapView mapa;

    /** Pines actualmente dibujados, para retirarlos antes de redibujar. */
    private final List<Marker> marcadores = new ArrayList<>();

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

    /**
     * Redibuja los pines del mapa: retira los anteriores y coloca uno por
     * movimiento geolocalizado.
     * <p>
     * Cada pin usa el color de movimientos ({@link PinMapa}) y muestra, al
     * tocarlo, un popup con concepto, monto y fecha. Los movimientos sin
     * ubicación se ignoran (defensa; el DAO ya los filtra).
     * </p>
     *
     * @param movimientos movimientos geolocalizados a dibujar (puede ser vacía)
     */
    public void mostrarMovimientos(@NonNull List<Transaccion> movimientos) {
        // Retirar los pines previos (y cerrar cualquier popup abierto).
        for (Marker marcador : marcadores) {
            if (marcador.isInfoWindowShown()) {
                marcador.closeInfoWindow();
            }
            mapa.getOverlays().remove(marcador);
        }
        marcadores.clear();
        InfoWindow.closeAllInfoWindowsOn(mapa);

        for (Transaccion movimiento : movimientos) {
            Ubicacion ubicacion = movimiento.getUbicacion();
            if (ubicacion == null) {
                continue;
            }
            Marker marcador = new Marker(mapa);
            marcador.setPosition(new GeoPoint(ubicacion.getLatitud(), ubicacion.getLongitud()));
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marcador.setIcon(PinMapa.iconoMovimiento(mapa.getContext()));
            marcador.setTitle(movimiento.getConcepto());
            marcador.setSnippet(montoConSigno(movimiento));
            marcador.setSubDescription(formatoFecha.format(movimiento.getFecha()));
            marcador.setInfoWindow(new MarkerInfoWindow(R.layout.mapa_info_window, mapa));
            mapa.getOverlays().add(marcador);
            marcadores.add(marcador);
        }

        mapa.invalidate();
    }

    /**
     * Formatea el monto de un movimiento con signo ({@code +} ingreso,
     * {@code -} egreso), igual que el historial.
     *
     * @param movimiento movimiento a formatear
     * @return monto formateado en CLP con signo
     */
    private static String montoConSigno(@NonNull Transaccion movimiento) {
        String monto = CurrencyFormatter.format(movimiento.getMonto());
        return (movimiento.getTipo() == TipoTransaccion.INGRESO ? "+" : "-") + monto;
    }
}
