package com.localcash.ui.mapa;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.localcash.R;
import com.localcash.databinding.ActivityMapaBinding;
import com.localcash.ui.BaseActivity;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

/**
 * Selector de ubicación sobre un mapa <b>offline</b> (Feature 009, T7).
 * <p>
 * Abre el mapa centrado en Santiago de Chile y permite fijar un pin tocando
 * (o manteniendo pulsado) el lienzo. "Confirmar" devuelve la coordenada elegida
 * a quien la haya lanzado mediante {@link #setResult(int, Intent)}; "Cancelar"
 * cierra sin resultado.
 * </p>
 * <p>
 * Las teselas provienen del {@link TeselasLocales TileSource local}
 * ({@code assets/tiles}, sin red). El ciclo de vida del {@code MapView} se
 * delega a la Activity ({@code onResume}/{@code onPause}/{@code onDetach}).
 * </p>
 *
 * <p><b>Contrato de resultado</b> (extras del {@link Intent} de {@code RESULT_OK}):</p>
 * <ul>
 *   <li>{@link #EXTRA_LATITUD} ({@code double}) y {@link #EXTRA_LONGITUD} ({@code double}):
 *       coordenada elegida (ambas o ninguna).</li>
 *   <li>{@link #EXTRA_DIRECCION} ({@code String}): dirección legible; hoy siempre
 *       {@code null} (no hay geocodificación inversa offline).</li>
 * </ul>
 */
public class MapaPickerActivity extends BaseActivity {

    /** Extra con la latitud elegida (grados WGS84). */
    public static final String EXTRA_LATITUD = "latitud";

    /** Extra con la longitud elegida (grados WGS84). */
    public static final String EXTRA_LONGITUD = "longitud";

    /** Extra con la dirección legible (opcional; hoy {@code null}). */
    public static final String EXTRA_DIRECCION = "direccion";

    /** Latitud de Santiago de Chile (centro inicial del mapa). */
    private static final double LATITUD_SANTIAGO = -33.4489;

    /** Longitud de Santiago de Chile (centro inicial del mapa). */
    private static final double LONGITUD_SANTIAGO = -70.6693;

    /** Zoom inicial (dentro del rango empaquetado 10-14). */
    private static final double ZOOM_INICIAL = 14d;

    /** Archivo de preferencias donde osmdroid guarda su configuración. */
    private static final String PREFS_OSMDROID = "osmdroid";

    private ActivityMapaBinding binding;

    /** Marcador del pin fijado ({@code null} mientras no se ha elegido punto). */
    private Marker marcador;

    /** Coordenada elegida por el usuario ({@code null} si aún no hay pin). */
    private GeoPoint ubicacionSeleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid: cargar configuración (rutas de caché, user-agent) ANTES de
        // inflar el MapView. Sin red: solo define dónde cachear y el user-agent.
        SharedPreferences prefs = getSharedPreferences(PREFS_OSMDROID, MODE_PRIVATE);
        Configuration.getInstance().load(getApplicationContext(), prefs);
        Configuration.getInstance().setUserAgentValue(getPackageName());

        binding = ActivityMapaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarMapa);

        configurarMapa();

        binding.btnZoomIn.setOnClickListener(v -> cambiarZoom(1));
        binding.btnZoomOut.setOnClickListener(v -> cambiarZoom(-1));

        binding.btnCancelarMapa.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        binding.btnConfirmarMapa.setOnClickListener(v -> confirmarUbicacion());

        // Hasta que no se fije un punto, no hay nada que confirmar.
        binding.btnConfirmarMapa.setEnabled(false);
    }

    /**
     * Prepara el mapa: teselas locales, controles táctiles, centrado inicial en
     * Santiago y el receptor de toques que fija el pin.
     */
    private void configurarMapa() {
        TeselasLocales.configurar(this, binding.mapaSeleccion);
        binding.mapaSeleccion.setMultiTouchControls(true);

        IMapController controller = binding.mapaSeleccion.getController();
        controller.setZoom(ZOOM_INICIAL);
        controller.setCenter(new GeoPoint(LATITUD_SANTIAGO, LONGITUD_SANTIAGO));

        binding.mapaSeleccion.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                colocarMarcador(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                colocarMarcador(p);
                return true;
            }
        }));
    }

    /**
     * Fija (o mueve) el pin en el punto indicado y habilita la confirmación.
     *
     * @param punto coordenada tocada en el mapa
     */
    private void colocarMarcador(GeoPoint punto) {
        if (marcador == null) {
            marcador = new Marker(binding.mapaSeleccion);
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marcador.setIcon(iconoPinTintado());
            binding.mapaSeleccion.getOverlays().add(marcador);
        }
        marcador.setPosition(punto);
        ubicacionSeleccionada = punto;
        binding.btnConfirmarMapa.setEnabled(true);
        binding.mapaSeleccion.invalidate();
    }

    /**
     * Devuelve el icono del pin teñido con el color de los movimientos
     * ({@link R.color#mapa_pin_movimiento}), centralizado en recursos.
     *
     * @return drawable del pin listo para el {@code Marker}
     */
    private Drawable iconoPinTintado() {
        Drawable icono = ContextCompat.getDrawable(this, R.drawable.ic_mapa_pin);
        if (icono == null) {
            return null;
        }
        Drawable tintado = DrawableCompat.wrap(icono).mutate();
        DrawableCompat.setTint(tintado, ContextCompat.getColor(this, R.color.mapa_pin_movimiento));
        return tintado;
    }

    /**
     * Acerca o aleja un nivel de zoom respetando el rango del {@code MapView}.
     *
     * @param delta {@code +1} para acercar, {@code -1} para alejar
     */
    private void cambiarZoom(int delta) {
        double zoomActual = binding.mapaSeleccion.getZoomLevelDouble();
        binding.mapaSeleccion.getController().setZoom(zoomActual + delta);
    }

    /**
     * Devuelve la coordenada elegida a la Activity que lanzó el selector.
     * Si no hay pin, no hace nada (el botón está deshabilitado).
     */
    private void confirmarUbicacion() {
        if (ubicacionSeleccionada == null) {
            return;
        }
        Intent resultado = new Intent();
        resultado.putExtra(EXTRA_LATITUD, ubicacionSeleccionada.getLatitude());
        resultado.putExtra(EXTRA_LONGITUD, ubicacionSeleccionada.getLongitude());
        // Sin geocodificación inversa offline: la dirección queda como dato opcional nulo.
        resultado.putExtra(EXTRA_DIRECCION, (String) null);
        setResult(RESULT_OK, resultado);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.mapaSeleccion.onResume();
    }

    @Override
    protected void onPause() {
        binding.mapaSeleccion.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Libera el tile provider y sus hilos (evita fugas del MapView).
        binding.mapaSeleccion.onDetach();
        super.onDestroy();
    }
}
