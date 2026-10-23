package com.localcash.ui.mapa;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.localcash.R;
import com.localcash.databinding.BottomSheetDetalleLugarBinding;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.EstadoLugar;
import com.localcash.domain.model.ItemLugar;
import com.localcash.domain.model.Lugar;

import java.util.List;

/**
 * Detalle de un pin (Feature 009, T19/T21/T22).
 * <p>
 * Muestra los datos del lugar, su origen (movimiento o independiente), la lista
 * de cosas interactiva y las acciones de gestión. La lista se observa desde
 * Room con el ciclo de vida del propio diálogo: al cerrar la hoja, el observer
 * se libera solo.
 * </p>
 */
public final class LugarDetalleSheet {

    private final Context context;

    private final MapaViewModel viewModel;

    private final Lugar lugar;

    private final BottomSheetDetalleLugarBinding binding;

    private final ItemsLugarAdapter itemsAdapter = new ItemsLugarAdapter();

    @NonNull
    private final BottomSheetDialog dialog;

    @NonNull
    private final OnMoverListener onMoverListener;

    /** Evento para pedir a la Activity que entre en modo "mover pin". */
    public interface OnMoverListener {
        /**
         * @param lugar pin independiente que se quiere mover
         */
        void onMover(@NonNull Lugar lugar);
    }

    /**
     * Construye la hoja de detalle.
     *
     * @param context         contexto de Activity
     * @param viewModel       ViewModel del mapa
     * @param lugar           pin a mostrar
     * @param onMoverListener callback para la acción "Mover en el mapa"
     */
    public LugarDetalleSheet(@NonNull Context context,
                             @NonNull MapaViewModel viewModel,
                             @NonNull Lugar lugar,
                             @NonNull OnMoverListener onMoverListener) {
        this.context = context;
        this.viewModel = viewModel;
        this.lugar = lugar;
        this.onMoverListener = onMoverListener;
        this.binding = BottomSheetDetalleLugarBinding.inflate(
                android.view.LayoutInflater.from(context));
        this.dialog = new BottomSheetDialog(context);
    }

    /**
     * Muestra la hoja y conecta todas sus acciones.
     */
    public void mostrar() {
        dialog.setContentView(binding.getRoot());

        binding.tvEmojiDetalle.setText(lugar.getEmoji());
        binding.tvNombreDetalle.setText(lugar.getNombre());
        binding.tvTipoDetalle.setText(
                context.getString(LugaresAdapter.etiquetaTipo(lugar.getTipo())));
        binding.tvOrigen.setText(lugar.estaVinculadoAMovimiento()
                ? R.string.aMapa_detalle_vinculado
                : R.string.aMapa_detalle_independiente);

        boolean tieneDescripcion = lugar.getDescripcion() != null
                && !lugar.getDescripcion().trim().isEmpty();
        binding.tvDescripcionDetalle.setText(lugar.getDescripcion());
        binding.tvDescripcionDetalle.setVisibility(
                tieneDescripcion ? View.VISIBLE : View.GONE);

        binding.btnMover.setVisibility(
                lugar.estaVinculadoAMovimiento() ? View.GONE : View.VISIBLE);
        binding.btnAlternarEstado.setText(
                lugar.getEstado() == EstadoLugar.ABIERTO
                        ? R.string.aMapa_btn_cerrar
                        : R.string.aMapa_btn_reabrir);

        configurarItems();
        configurarAcciones();

        dialog.show();
    }

    /**
     * Conecta la lista de cosas: observar, agregar, marcar y eliminar.
     */
    private void configurarItems() {
        binding.recyclerItemsDetalle.setLayoutManager(new LinearLayoutManager(context));
        binding.recyclerItemsDetalle.setAdapter(itemsAdapter);

        itemsAdapter.setOnItemCompletadoListener(
                (item, completado) -> viewModel.marcarItem(item, completado));
        itemsAdapter.setOnItemEliminarListener(viewModel::eliminarItem);

        viewModel.getItems(lugar.getId()).observe(dialog, this::mostrarItems);

        binding.btnAgregarCosaDetalle.setOnClickListener(v -> {
            CharSequence texto = binding.inputNuevaCosaDetalle.getText();
            String descripcion = texto != null ? texto.toString().trim() : "";
            if (descripcion.isEmpty()) {
                return;
            }
            viewModel.agregarItem(lugar.getId(), descripcion);
            binding.inputNuevaCosaDetalle.setText(null);
            ocultarTeclado();
        });
    }

    /**
     * Actualiza la lista visible y su estado vacío.
     *
     * @param items elementos emitidos por Room
     */
    private void mostrarItems(List<ItemLugar> items) {
        itemsAdapter.setItems(items);
        boolean vacio = items == null || items.isEmpty();
        binding.tvSinItemsDetalle.setVisibility(vacio ? View.VISIBLE : View.GONE);
        binding.recyclerItemsDetalle.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    /**
     * Cablea editar, mover, cerrar/reabrir y eliminar.
     */
    private void configurarAcciones() {
        binding.btnEditar.setOnClickListener(v -> {
            dialog.dismiss();
            new LugarFormSheet(context, viewModel, lugar, null, null).mostrar();
        });

        binding.btnMover.setOnClickListener(v -> {
            dialog.dismiss();
            onMoverListener.onMover(lugar);
        });

        binding.btnAlternarEstado.setOnClickListener(v -> {
            androidx.lifecycle.MutableLiveData<ErrorCode> resultado =
                    new androidx.lifecycle.MutableLiveData<>();
            resultado.observe(dialog, error -> {
                if (error == null) {
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, R.string.aMapa_toast_lugar_error,
                            Toast.LENGTH_SHORT).show();
                }
            });
            viewModel.alternarEstado(lugar, resultado);
        });

        binding.btnEliminarLugar.setOnClickListener(v -> confirmarEliminacion());
    }

    /**
     * Pide confirmación y elimina el lugar con su lista en cascada.
     */
    private void confirmarEliminacion() {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.aMapa_dialog_eliminar_titulo)
                .setMessage(R.string.aMapa_dialog_eliminar_mensaje)
                .setPositiveButton(R.string.aMapa_dialog_eliminar_confirmar,
                        (d, which) -> {
                            viewModel.eliminarLugar(lugar.getId());
                            Toast.makeText(context, R.string.aMapa_toast_lugar_eliminado,
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.aMapa_btn_cancelar, null)
                .show();
    }

    /**
     * Cierra el teclado tras agregar una cosa.
     */
    private void ocultarTeclado() {
        InputMethodManager teclado = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (teclado != null) {
            teclado.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        }
    }
}
