package com.localcash.ui.mapa;

import androidx.annotation.NonNull;

import com.localcash.R;
import com.localcash.data.entities.LugarCompra;
import com.localcash.data.relations.LugarCompraConItems;
import com.localcash.domain.model.TipoTransaccion;
import com.localcash.domain.model.Transaccion;
import com.localcash.domain.model.Ubicacion;
import com.localcash.ui.util.CurrencyFormatter;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la pestaña "Mapa" en <b>modo lectura</b>
 * (Feature 009, T10-T12; Feature 010, Fase 2).
 * <p>
 * Encapsula todo lo específico del {@link MapView} de la pestaña: monta el
 * {@link TeselasLocales TileSource local y offline}, fija el encuadre inicial,
 * gestiona el zoom, dibuja un pin por movimiento geolocalizado y otro por lugar
 * de compra, y notifica las pulsaciones largas para crear un lugar. Mantener
 * esta lógica aquí permite que {@code CuentasActivity} (el controlador de la
 * pantalla) solo orqueste: crear el controlador, enlazar los botones de zoom y
 * entregarle las listas a dibujar.
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

    /** Últimos movimientos recibidos; se conservan para redibujar sin perderlos. */
    private final List<Transaccion> movimientosActuales = new ArrayList<>();

    /** Últimos lugares de compra recibidos (Feature 010, Fase 2). */
    private final List<LugarCompraConItems> lugaresCompraActuales = new ArrayList<>();

    /** Callback que la Activity registra para atender una pulsación larga. */
    private OnLongPressListener longPressListener;

    /**
     * Notifica una pulsación larga sobre el mapa con sus coordenadas.
     */
    public interface OnLongPressListener {

        /**
         * @param latitud  latitud WGS84 del punto presionado
         * @param longitud longitud WGS84 del punto presionado
         */
        void onLongPress(double latitud, double longitud);
    }

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

        // osmdroid 6.x no expone setOnLongPressListener: la pulsación larga se
        // recibe con un MapEventsOverlay. Se agrega primero (debajo de los
        // pines) para que los marcadores sigan siendo seleccionables.
        mapa.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint punto) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint punto) {
                if (punto == null || longPressListener == null) {
                    return false;
                }
                longPressListener.onLongPress(punto.getLatitude(), punto.getLongitude());
                return true;
            }
        }));
    }

    /**
     * Registra el callback de pulsación larga (Feature 010, Fase 2).
     * <p>
     * Si no se registra, el mapa mantiene el comportamiento anterior y las
     * pulsaciones largas no tienen efecto.
     * </p>
     *
     * @param listener callback a invocar con las coordenadas del punto
     */
    public void setOnLongPressListener(OnLongPressListener listener) {
        this.longPressListener = listener;
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
        movimientosActuales.clear();
        movimientosActuales.addAll(movimientos);
        redibujar();
    }

    /**
     * Redibuja los pines de los lugares de compra (Feature 010, Fase 2).
     * <p>
     * Cada lugar usa el color {@link R.color#mapa_pin_compra} y muestra su tipo
     * como título y su gasto promedio como resumen. Los ítems y el diálogo de
     * detalle se abordan en fases posteriores.
     * </p>
     *
     * @param lugares lugares de compra con sus ítems (puede ser vacía)
     */
    public void mostrarLugaresCompra(@NonNull List<LugarCompraConItems> lugares) {
        lugaresCompraActuales.clear();
        lugaresCompraActuales.addAll(lugares);
        redibujar();
    }

    /**
     * Retira todos los pines y vuelve a dibujar movimientos y lugares de
     * compra desde las últimas listas recibidas.
     * <p>
     * Se centraliza aquí para que una actualización de cualquiera de las dos
     * fuentes no borre los pines de la otra.
     * </p>
     */
    private void redibujar() {
        for (Marker marcador : marcadores) {
            if (marcador.isInfoWindowShown()) {
                marcador.closeInfoWindow();
            }
            mapa.getOverlays().remove(marcador);
        }
        marcadores.clear();
        InfoWindow.closeAllInfoWindowsOn(mapa);

        dibujarMovimientos();
        dibujarLugaresCompra();

        mapa.invalidate();
    }

    /** Dibuja un pin por movimiento geolocalizado. */
    private void dibujarMovimientos() {
        for (Transaccion movimiento : movimientosActuales) {
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
    }

    /** Dibuja un pin por lugar de compra. */
    private void dibujarLugaresCompra() {
        for (LugarCompraConItems relacion : lugaresCompraActuales) {
            LugarCompra lugar = relacion.getLugarCompra();
            if (lugar == null) {
                continue;
            }
            Marker marcador = new Marker(mapa);
            marcador.setPosition(new GeoPoint(lugar.getLatitud(), lugar.getLongitud()));
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marcador.setIcon(PinMapa.iconoCompra(mapa.getContext()));
            marcador.setTitle(lugar.getTipoLugar());
            marcador.setSnippet(CurrencyFormatter.format(lugar.getGastoPromedio()));
            mapa.getOverlays().add(marcador);
            marcadores.add(marcador);
        }
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
