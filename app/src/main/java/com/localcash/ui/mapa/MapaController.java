package com.localcash.ui.mapa;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.localcash.R;
import com.localcash.domain.model.EstadoLugar;
import com.localcash.domain.model.Lugar;
import com.localcash.domain.model.Ubicacion;

import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.maps.UiSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controlador del mapa de la pestaña "Mapa" (Feature 009).
 * <p>
 * Encapsula el {@link MapView} de MapLibre: estilo, cámara, pines y reenvío del
 * ciclo de vida desde {@code CuentasActivity}. La Activity no conoce los tipos
 * de MapLibre; solo entrega lugares de dominio y recibe eventos.
 * </p>
 * <p>
 * Los marcadores se dibujan como un bitmap circular con el emoji del lugar. Los
 * pines cerrados se dibujan atenuados, pero no se eliminan: la lista los oculta
 * y el mapa los conserva para poder reabrirlos (CA-12).
 * </p>
 */
public final class MapaController {

    /** Centro inicial: Plaza de Armas de Santiago de Chile. */
    private static final LatLng CENTRO_INICIAL = new LatLng(-33.4489, -70.6693);

    /** Zoom inicial: permite ver la ciudad y sus alrededores. */
    private static final double ZOOM_INICIAL = 11.0;

    /** Zoom al centrar en un pin o en la ubicación actual. */
    private static final double ZOOM_DETALLE = 16.0;

    /** Tamaño del marcador en dp. */
    private static final float TAMANO_MARCADOR_DP = 44f;

    /** Tamaño del emoji dentro del marcador en sp. */
    private static final float TAMANO_EMOJI_SP = 22f;

    /** Opacidad aplicada al emoji de un pin cerrado. */
    private static final int ALFA_PIN_CERRADO = 130;

    private final MapView mapView;

    /** Mapa nativo; {@code null} hasta que MapLibre termina de cargar. */
    @Nullable
    private MapLibreMap mapa;

    /** {@code true} cuando el estilo terminó de cargar y se pueden añadir pines. */
    private boolean estiloListo;

    /** Lugares actuales indexados por id (fuente para sincronizar marcadores). */
    private final Map<Long, Lugar> lugares = new LinkedHashMap<>();

    /** Marcadores vivos indexados por id de lugar. */
    private final Map<Long, Marker> marcadores = new HashMap<>();

    /** Relación inversa para resolver el lugar desde el evento de clic. */
    private final Map<Marker, Long> idPorMarcador = new HashMap<>();

    /** Caché de iconos por emoji + estado; evita recrear bitmaps en cada emisión. */
    private final Map<String, Icon> iconos = new HashMap<>();

    @Nullable
    private OnLugarSeleccionadoListener onLugarSeleccionadoListener;

    @Nullable
    private OnPulsacionLargaListener onPulsacionLargaListener;

    /**
     * Crea el controlador. No toca el mapa todavía: {@link #crear(Bundle)} debe
     * llamarse desde {@code onCreate} de la Activity.
     *
     * @param mapView vista del mapa inflada desde {@code view_mapa.xml}
     */
    public MapaController(@NonNull MapView mapView) {
        this.mapView = mapView;
    }

    /**
     * Reenvía {@code onCreate} al {@link MapView} y solicita el mapa de forma
     * asíncrona.
     *
     * @param estadoGuardado estado de la Activity (puede ser {@code null})
     */
    public void crear(@Nullable Bundle estadoGuardado) {
        mapView.onCreate(estadoGuardado);
        mapView.getMapAsync(this::alMapaListo);
    }

    /**
     * Configura el mapa cuando MapLibre está listo: gestos, atribución, cámara
     * inicial y estilo vectorial.
     *
     * @param map mapa nativo listo para usarse
     */
    private void alMapaListo(@NonNull MapLibreMap map) {
        this.mapa = map;

        UiSettings ui = map.getUiSettings();
        ui.setCompassEnabled(true);
        ui.setAttributionEnabled(true);
        ui.setLogoEnabled(true);

        map.setCameraPosition(new CameraPosition.Builder()
                .target(CENTRO_INICIAL)
                .zoom(ZOOM_INICIAL)
                .build());

        map.addOnMapLongClickListener(latLng -> {
            if (onPulsacionLargaListener != null) {
                onPulsacionLargaListener.onPulsacionLarga(
                        new Ubicacion(latLng.getLatitude(), latLng.getLongitude()));
            }
            return true;
        });

        map.setOnMarkerClickListener(marker -> {
            Long id = idPorMarcador.get(marker);
            Lugar lugar = id != null ? lugares.get(id) : null;
            if (lugar != null && onLugarSeleccionadoListener != null) {
                onLugarSeleccionadoListener.onLugarSeleccionado(lugar);
                return true;
            }
            return false;
        });

        map.setStyle(new Style.Builder().fromUri(MapaConfig.ESTILO_LIBERTY), style -> {
            estiloListo = true;
            sincronizarMarcadores();
        });
    }

    /**
     * Reemplaza la colección de lugares del mapa. Puede llamarse antes de que
     * MapLibre esté listo: los marcadores se crean cuando el estilo carga.
     *
     * @param nuevos lista completa de lugares (abiertos y cerrados)
     */
    public void setLugares(@Nullable List<Lugar> nuevos) {
        lugares.clear();
        if (nuevos != null) {
            for (Lugar lugar : nuevos) {
                lugares.put(lugar.getId(), lugar);
            }
        }
        if (estiloListo) {
            sincronizarMarcadores();
        }
    }

    /**
     * Centra la cámara en una coordenada.
     *
     * @param ubicacion coordenada WGS84
     */
    public void centrarEn(@NonNull Ubicacion ubicacion) {
        centrarEn(ubicacion, ZOOM_DETALLE);
    }

    /**
     * Centra la cámara en una coordenada con el zoom indicado.
     *
     * @param ubicacion coordenada WGS84
     * @param zoom      nivel de zoom
     */
    public void centrarEn(@NonNull Ubicacion ubicacion, double zoom) {
        if (mapa == null) {
            return;
        }
        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(ubicacion.getLatitud(), ubicacion.getLongitud()), zoom), 600);
    }

    /**
     * Centra el mapa en un lugar y abre su ventana de información como detalle
     * ligero mientras el BottomSheet completo llega en la Fase 3 (T19).
     *
     * @param lugar lugar a mostrar
     */
    public void mostrarLugar(@NonNull Lugar lugar) {
        centrarEn(lugar.getUbicacion());
        mostrarInfoLugar(lugar);
    }

    /**
     * Abre la ventana de información de un pin sin mover la cámara.
     *
     * @param lugar lugar cuyo pin se quiere destacar
     */
    public void mostrarInfoLugar(@NonNull Lugar lugar) {
        if (mapa == null) {
            return;
        }
        Marker marcador = marcadores.get(lugar.getId());
        if (marcador != null) {
            marcador.showInfoWindow(mapa, mapView);
        }
    }

    /**
     * Registra el callback de clic sobre un pin.
     *
     * @param listener listener o {@code null} para desactivarlo
     */
    public void setOnLugarSeleccionadoListener(@Nullable OnLugarSeleccionadoListener listener) {
        this.onLugarSeleccionadoListener = listener;
    }

    /**
     * Registra el callback de pulsación larga. La Fase 3 lo usa para abrir el
     * formulario de alta de un pin (T18).
     *
     * @param listener listener o {@code null} para desactivarlo
     */
    public void setOnPulsacionLargaListener(@Nullable OnPulsacionLargaListener listener) {
        this.onPulsacionLargaListener = listener;
    }

    /**
     * Crea o actualiza los marcadores necesarios y elimina los que ya no existen.
     */
    private void sincronizarMarcadores() {
        if (mapa == null) {
            return;
        }

        Set<Long> vigentes = new HashSet<>(lugares.keySet());
        List<Long> obsoletos = new ArrayList<>();
        for (Long id : marcadores.keySet()) {
            if (!vigentes.contains(id)) {
                obsoletos.add(id);
            }
        }
        for (Long id : obsoletos) {
            Marker marcador = marcadores.remove(id);
            if (marcador != null) {
                marcador.remove();
                idPorMarcador.remove(marcador);
            }
        }

        for (Lugar lugar : lugares.values()) {
            LatLng posicion = new LatLng(
                    lugar.getUbicacion().getLatitud(), lugar.getUbicacion().getLongitud());
            Icon icono = iconoPara(lugar);
            Marker marcador = marcadores.get(lugar.getId());

            if (marcador == null) {
                marcador = mapa.addMarker(new MarkerOptions()
                        .position(posicion)
                        .icon(icono)
                        .title(lugar.getNombre())
                        .snippet(lugar.getEmoji()));
                marcadores.put(lugar.getId(), marcador);
            } else {
                marcador.setPosition(posicion);
                marcador.setIcon(icono);
                marcador.setTitle(lugar.getNombre());
                marcador.setSnippet(lugar.getEmoji());
            }
            idPorMarcador.put(marcador, lugar.getId());
        }
    }

    /**
     * Obtiene (o crea) el icono de un lugar. Los pines cerrados comparten el
     * emoji pero se dibujan atenuados.
     *
     * @param lugar lugar a representar
     * @return icono listo para el marcador
     */
    @NonNull
    private Icon iconoPara(@NonNull Lugar lugar) {
        String clave = lugar.getEmoji() + "|" + lugar.getEstado().name();
        Icon icono = iconos.get(clave);
        if (icono == null) {
            icono = IconFactory.getInstance(mapView.getContext())
                    .fromBitmap(crearBitmapPin(lugar.getEmoji(),
                            lugar.getEstado() == EstadoLugar.ABIERTO));
            iconos.put(clave, icono);
        }
        return icono;
    }

    /**
     * Dibuja el pin: círculo de color con borde blanco y el emoji centrado.
     *
     * @param emoji     emoji del lugar
     * @param abierto   {@code true} para color activo; {@code false} para gris
     * @return bitmap cuadrado listo para MapLibre
     */
    @NonNull
    private Bitmap crearBitmapPin(@NonNull String emoji, boolean abierto) {
        DisplayMetrics metricas = mapView.getResources().getDisplayMetrics();
        int tamano = Math.round(TAMANO_MARCADOR_DP * metricas.density);
        int borde = Math.max(2, Math.round(3f * metricas.density));

        Bitmap bitmap = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
        relleno.setColor(ContextCompat.getColor(mapView.getContext(),
                abierto ? R.color.mapa_pin_abierto : R.color.mapa_pin_cerrado));
        canvas.drawCircle(tamano / 2f, tamano / 2f, tamano / 2f - borde, relleno);

        Paint contorno = new Paint(Paint.ANTI_ALIAS_FLAG);
        contorno.setStyle(Paint.Style.STROKE);
        contorno.setStrokeWidth(borde);
        contorno.setColor(Color.WHITE);
        canvas.drawCircle(tamano / 2f, tamano / 2f, tamano / 2f - borde / 2f, contorno);

        Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        texto.setTypeface(Typeface.DEFAULT);
        texto.setTextAlign(Paint.Align.CENTER);
        texto.setTextSize(TAMANO_EMOJI_SP * metricas.scaledDensity);
        if (!abierto) {
            texto.setAlpha(ALFA_PIN_CERRADO);
        }
        float centroY = tamano / 2f - (texto.descent() + texto.ascent()) / 2f;
        canvas.drawText(emoji, tamano / 2f, centroY, texto);

        return bitmap;
    }

    /** Reenvía {@code onStart} al mapa. */
    public void onStart() {
        mapView.onStart();
    }

    /** Reenvía {@code onResume} al mapa. */
    public void onResume() {
        mapView.onResume();
    }

    /** Reenvía {@code onPause} al mapa. */
    public void onPause() {
        mapView.onPause();
    }

    /** Reenvía {@code onStop} al mapa. */
    public void onStop() {
        mapView.onStop();
    }

    /** Guarda el estado de cámara del mapa en el bundle de la Activity. */
    public void guardarEstado(@NonNull Bundle estado) {
        mapView.onSaveInstanceState(estado);
    }

    /** Libera los recursos nativos del mapa. */
    public void destruir() {
        marcadores.clear();
        idPorMarcador.clear();
        iconos.clear();
        lugares.clear();
        mapa = null;
        mapView.onDestroy();
    }

    /** Reenvía {@code onLowMemory} al mapa. */
    public void onLowMemory() {
        mapView.onLowMemory();
    }

    /** Evento de clic sobre un pin del mapa. */
    public interface OnLugarSeleccionadoListener {
        /**
         * @param lugar lugar seleccionado
         */
        void onLugarSeleccionado(@NonNull Lugar lugar);
    }

    /** Evento de pulsación larga sobre el mapa. */
    public interface OnPulsacionLargaListener {
        /**
         * @param ubicacion coordenada donde el usuario mantuvo presionado
         */
        void onPulsacionLarga(@NonNull Ubicacion ubicacion);
    }
}
