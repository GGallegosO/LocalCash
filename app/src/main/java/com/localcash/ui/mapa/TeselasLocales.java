package com.localcash.ui.mapa;

import android.content.Context;
import android.util.TypedValue;

import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileAssetsProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;

/**
 * Origen de teselas propio, <b>100% offline</b>: sirve los mapas
 * preempaquetados en {@code assets/tiles/{z}/{x}/{y}.png} (esquema XYZ),
 * sin usar la red.
 * <p>
 * El pilar "100% offline" del proyecto prohíbe declarar el permiso
 * {@code INTERNET}; por eso el mapa no descarga teselas en runtime, sino que
 * lee las del APK con el {@link MapTileAssetsProvider} de osmdroid. El nombre
 * del origen ({@link #NOMBRE}) es también la carpeta usada en {@code assets/}
 * (osmdroid resuelve {@code assets/<nombre>/<z>/<x>/<y><extension>}).
 * </p>
 * <p>
 * La cobertura se limita a la Región Metropolitana de Santiago en zoom
 * {@value #ZOOM_MINIMO}-{@value #ZOOM_MAXIMO} (ver {@code plan.md}); fuera de
 * esa zona se verá un lienzo navegable sin teselas (no un error).
 * </p>
 */
public class TeselasLocales extends XYTileSource {

    /** Nombre del origen y, a la vez, carpeta dentro de {@code assets/}. */
    public static final String NOMBRE = "tiles";

    /** Zoom mínimo empaquetado. */
    public static final int ZOOM_MINIMO = 10;

    /** Zoom máximo empaquetado. */
    public static final int ZOOM_MAXIMO = 14;

    /** Lado de la tesela en píxeles (estándar de OpenStreetMap). */
    private static final int TAMANO_TESELA_PX = 256;

    /** Extensión de los archivos de tesela. */
    private static final String EXTENSION = ".png";

    /**
     * Crea el origen de teselas local. No requiere URLs base porque nunca se
     * descarga: las teselas se leen desde {@code assets/}.
     */
    public TeselasLocales() {
        // Array de URLs base vacío: el proveedor de red no se usa (ver configurar).
        super(NOMBRE, ZOOM_MINIMO, ZOOM_MAXIMO, TAMANO_TESELA_PX, EXTENSION, new String[]{});
    }

    /**
     * Configura un {@link MapView} para servir exclusivamente las teselas
     * locales.
     * <p>
     * Monta un {@link MapTileProviderArray} con <b>solo</b> el
     * {@link MapTileAssetsProvider} (nada de descargador de red, que fallaría
     * sin permiso {@code INTERNET}) y fija el rango de zoom al empaquetado.
     * </p>
     *
     * @param context contexto usado para acceder a {@code assets/}
     * @param mapView mapa a configurar (no {@code null})
     */
    public static void configurar(Context context, MapView mapView) {
        TeselasLocales tileSource = new TeselasLocales();
        SimpleRegisterReceiver registerReceiver = new SimpleRegisterReceiver(context);
        MapTileModuleProviderBase[] proveedores = {
                new MapTileAssetsProvider(registerReceiver, context.getAssets(), tileSource)
        };
        MapTileProviderArray provider =
                new MapTileProviderArray(tileSource, registerReceiver, proveedores);

        mapView.setTileProvider(provider);
        mapView.setTileSource(tileSource);
        mapView.setMinZoomLevel((double) ZOOM_MINIMO);
        mapView.setMaxZoomLevel((double) ZOOM_MAXIMO);

        // Controles de zoom nativos de osmdroid: son los únicos en la pestaña
        // "Mapa" (se retiraron los botones flotantes duplicados).
        mapView.setBuiltInZoomControls(true);

        // Fondo del mapa acorde al tema activo: evita el destello negro tanto
        // mientras faltan teselas como al recrear la Activity por cambio de tema.
        TypedValue fondo = new TypedValue();
        context.getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorSurface, fondo, true);
        mapView.setBackgroundColor(fondo.data);
        TilesOverlay teselas = mapView.getOverlayManager().getTilesOverlay();
        if (teselas != null) {
            teselas.setLoadingBackgroundColor(fondo.data);
            teselas.setLoadingLineColor(fondo.data);
        }
    }
}
