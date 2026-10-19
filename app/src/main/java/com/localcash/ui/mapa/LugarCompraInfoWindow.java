package com.localcash.ui.mapa;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;

import com.localcash.R;
import com.localcash.data.entities.LugarCompra;
import com.localcash.data.relations.LugarCompraConItems;
import com.localcash.databinding.MapaInfoWindowLugarCompraBinding;
import com.localcash.ui.util.CurrencyFormatter;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

/**
 * InfoWindow personalizado del pin de un lugar de compra (Feature 010, Fase 3).
 * <p>
 * Muestra el tipo, el gasto estimado (o "Sin gasto estimado" cuando es
 * {@code null}), la cantidad de ítems y el botón "Ver detalle". El botón cierra
 * la burbuja y delega la navegación en {@link OnVerDetalleListener}; así el
 * controlador del mapa no conoce la Activity de detalle.
 * </p>
 */
public class LugarCompraInfoWindow extends InfoWindow {

    /** Callback para abrir el detalle del lugar. */
    public interface OnVerDetalleListener {

        /**
         * @param lugarCompraId id del lugar cuyo detalle se quiere abrir
         */
        void onVerDetalle(long lugarCompraId);
    }

    private final MapaInfoWindowLugarCompraBinding binding;
    private final LugarCompraConItems relacion;
    private final OnVerDetalleListener listener;

    /**
     * Construye la burbuja para un lugar concreto.
     *
     * @param mapView   mapa al que pertenece la burbuja
     * @param relacion  lugar con sus ítems
     * @param listener  callback de "Ver detalle" (puede ser {@code null})
     */
    public LugarCompraInfoWindow(@NonNull MapView mapView,
                                 @NonNull LugarCompraConItems relacion,
                                 OnVerDetalleListener listener) {
        super(inflar(mapView), mapView);
        this.binding = MapaInfoWindowLugarCompraBinding.bind(mView);
        this.relacion = relacion;
        this.listener = listener;
    }

    /** Infla el layout del InfoWindow con ViewBinding. */
    private static View inflar(@NonNull MapView mapView) {
        return LayoutInflater.from(mapView.getContext())
                .inflate(R.layout.mapa_info_window_lugar_compra, mapView, false);
    }

    @Override
    public void onOpen(Object item) {
        LugarCompra lugar = relacion.getLugarCompra();
        if (lugar == null) {
            return;
        }
        Context context = mMapView.getContext();

        binding.tvInfoTitulo.setText(
                context.getString(R.string.mapa_compra_info_titulo, lugar.getTipoLugar()));

        Long gastoPromedio = lugar.getGastoPromedio();
        if (gastoPromedio == null) {
            binding.tvInfoGasto.setText(R.string.mapa_compra_info_sin_gasto);
        } else {
            binding.tvInfoGasto.setText(context.getString(
                    R.string.mapa_compra_info_gasto,
                    CurrencyFormatter.format(gastoPromedio)));
        }

        int cantidadItems = relacion.getItems() == null ? 0 : relacion.getItems().size();
        binding.tvInfoItems.setText(context.getResources().getQuantityString(
                R.plurals.mapa_compra_info_items, cantidadItems, cantidadItems));

        binding.btnVerDetalle.setOnClickListener(v -> {
            close();
            if (listener != null) {
                listener.onVerDetalle(lugar.getId());
            }
        });
    }

    @Override
    public void onClose() {
        // Sin recursos que liberar: el binding se reutiliza mientras viva el pin.
    }
}
