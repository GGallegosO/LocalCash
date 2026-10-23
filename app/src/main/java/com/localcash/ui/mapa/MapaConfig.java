package com.localcash.ui.mapa;

import android.content.Context;

import org.maplibre.android.MapLibre;

/**
 * Configuración central del mapa (Feature 009).
 * <p>
 * El estilo, la atribución y la inicialización de MapLibre viven en un único
 * lugar para que no haya URLs de teselas dispersas en Activities, layouts o
 * controladores. `tech-stack.md` §2 registra la dependencia y el proveedor.
 * </p>
 */
public final class MapaConfig {

    /** Estilo vectorial público de OpenFreeMap sobre datos de OpenStreetMap. */
    public static final String ESTILO_LIBERTY =
            "https://tiles.openfreemap.org/styles/liberty";

    /** Atribución obligatoria y visible en la esquina del mapa. */
    public static final String ATRIBUCION =
            "© OpenStreetMap contributors · © OpenFreeMap";

    private MapaConfig() {
    }

    /**
     * Inicializa MapLibre antes de inflar un {@code MapView}. Debe ejecutarse
     * en el hilo de UI ({@code MapLibre.getInstance} está anotado con
     * {@code @UiThread}).
     *
     * @param context contexto de la aplicación o de la Activity
     */
    public static void inicializar(Context context) {
        MapLibre.getInstance(context.getApplicationContext());
    }
}
