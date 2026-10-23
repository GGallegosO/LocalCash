package com.localcash.ui.mapa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.localcash.R;
import com.localcash.databinding.ActivitySeleccionarUbicacionBinding;
import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.model.Ubicacion;

import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.maps.UiSettings;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;

/**
 * Activity de selección de ubicación para un movimiento (Feature 009, T23).
 * <p>
 * Muestra un mapa MapLibre y permite elegir un punto con un toque. El marcador
 * se mueve con cada toque y el botón "Confirmar" solo se habilita cuando hay un
 * punto válido. Al confirmar devuelve {@link #EXTRA_LATITUD} y
 * {@link #EXTRA_LONGITUD} en el {@code Intent} de resultado; cancelar deja el
 * movimiento sin coordenadas (CA-2/CA-4).
 * </p>
 * <p>
 * La Activity no persiste nada: el registro y la creación del pin vinculado
 * ocurren atómicamente en {@code TransaccionViewModel} (T25).
 * </p>
 */
public class SeleccionarUbicacionActivity extends AppCompatActivity {

    /** Extra de resultado con la latitud WGS84 elegida. */
    public static final String EXTRA_LATITUD = "latitud";

    /** Extra de resultado con la longitud WGS84 elegida. */
    public static final String EXTRA_LONGITUD = "longitud";

    /** Centro inicial mientras el usuario elige un punto. */
    private static final LatLng CENTRO_INICIAL = new LatLng(-33.4489, -70.6693);

    /** Zoom inicial que permite reconocer el entorno sin perder contexto. */
    private static final double ZOOM_INICIAL = 13.0;

    private ActivitySeleccionarUbicacionBinding binding;

    private MapView mapView;

    @Nullable
    private MapLibreMap mapa;

    @Nullable
    private Marker marcador;

    @Nullable
    private Ubicacion ubicacionElegida;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapaConfig.inicializar(this);
        binding = ActivitySeleccionarUbicacionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapView = binding.mapView;
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::alMapaListo);

        binding.tvAtribucion.setText(MapaConfig.ATRIBUCION);
        binding.btnConfirmar.setOnClickListener(v -> confirmar());
        binding.btnCancelar.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    /**
     * Configura el mapa cuando la superficie está lista.
     *
     * @param map mapa nativo
     */
    private void alMapaListo(@NonNull MapLibreMap map) {
        mapa = map;
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(CENTRO_INICIAL, ZOOM_INICIAL));

        mapa.setStyle(new Style.Builder().fromUri(MapaConfig.ESTILO_LIBERTY), style -> {
            configurarCredenciales(map);
            map.addOnMapClickListener(this::seleccionarPunto);
        });
    }

    /**
     * Desactiva los créditos nativos: la Activity pinta la atribución textual.
     *
     * @param map mapa nativo
     */
    private void configurarCredenciales(@NonNull MapLibreMap map) {
        UiSettings ui = map.getUiSettings();
        ui.setCompassEnabled(true);
        ui.setAttributionEnabled(false);
        ui.setLogoEnabled(false);
    }

    /**
     * Registra el punto tocado, mueve el marcador y habilita la confirmación.
     *
     * @param punto coordenada tocada en el mapa
     * @return {@code true} para consumir el evento
     */
    private boolean seleccionarPunto(@NonNull LatLng punto) {
        try {
            ubicacionElegida = new Ubicacion(punto.getLatitude(), punto.getLongitude());
        } catch (BusinessRuleException e) {
            Toast.makeText(this, R.string.aMapa_error_ubicacion,
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        if (marcador == null) {
            marcador = mapa.addMarker(new MarkerOptions()
                    .position(punto)
                    .title(getString(R.string.aMapa_seleccionar_elegido)));
        } else {
            marcador.setPosition(punto);
        }

        binding.tvPuntoElegido.setText(R.string.aMapa_seleccionar_elegido);
        binding.btnConfirmar.setEnabled(true);
        return true;
    }

    /**
     * Devuelve el punto elegido al panel de registro y cierra la Activity.
     */
    private void confirmar() {
        if (ubicacionElegida == null) {
            return;
        }
        Intent resultado = new Intent();
        resultado.putExtra(EXTRA_LATITUD, ubicacionElegida.getLatitud());
        resultado.putExtra(EXTRA_LONGITUD, ubicacionElegida.getLongitud());
        setResult(RESULT_OK, resultado);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}
