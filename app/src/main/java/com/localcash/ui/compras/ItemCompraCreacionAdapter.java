package com.localcash.ui.compras;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.localcash.data.entities.ItemCompra;
import com.localcash.databinding.ItemCrearItemCompraBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter del RecyclerView de ítems del diálogo de creación (Feature 010,
 * Fase 2.5).
 * <p>
 * Mantiene una lista mutable de {@link ItemCompra} en memoria. Cada fila escribe
 * sus cambios (nombre y cantidad) directamente sobre el modelo mediante
 * {@link TextWatcher}; al guardar, la Activity entrega esa lista al ViewModel
 * para persistirla junto con el lugar. El adapter no persiste ni valida reglas:
 * solo edita la lista borrador.
 * </p>
 */
public class ItemCompraCreacionAdapter
        extends RecyclerView.Adapter<ItemCompraCreacionAdapter.ItemViewHolder> {

    /** Ítems en edición, en el orden en que se agregan. */
    private final List<ItemCompra> items = new ArrayList<>();

    /**
     * Agrega una fila vacía con cantidad inicial 1.
     */
    public void agregarItem() {
        items.add(new ItemCompra(0L, "", 1, false));
        notifyItemInserted(items.size() - 1);
    }

    /**
     * Elimina la fila indicada.
     *
     * @param posicion posición en el adapter
     */
    public void eliminarItem(int posicion) {
        if (posicion < 0 || posicion >= items.size()) {
            return;
        }
        items.remove(posicion);
        notifyItemRemoved(posicion);
    }

    /**
     * Devuelve una copia de la lista borrador para persistirla.
     *
     * @return ítems editados (posiblemente vacía)
     */
    @NonNull
    public List<ItemCompra> obtenerItems() {
        return new ArrayList<>(items);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCrearItemCompraBinding binding = ItemCrearItemCompraBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.enlazar(items.get(position));
        holder.binding.btnEliminarItem.setOnClickListener(v -> {
            int posicion = holder.getBindingAdapterPosition();
            if (posicion != RecyclerView.NO_POSITION) {
                eliminarItem(posicion);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder de una fila editable. Los watchers se registran una sola vez y
     * escriben sobre el ítem vinculado; {@code enlazando} evita que el
     * {@code setText} inicial dispare cambios espurios.
     */
    static final class ItemViewHolder extends RecyclerView.ViewHolder {

        private final ItemCrearItemCompraBinding binding;
        private final TextWatcher nombreWatcher;
        private final TextWatcher cantidadWatcher;

        /** Ítem vinculado a esta fila ({@code null} antes del primer bind). */
        private ItemCompra item;

        /** {@code true} mientras se cargan los valores iniciales en las vistas. */
        private boolean enlazando;

        ItemViewHolder(@NonNull ItemCrearItemCompraBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            nombreWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Sin acción.
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Sin acción.
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!enlazando && item != null) {
                        item.setNombre(s.toString());
                    }
                }
            };

            cantidadWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Sin acción.
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Sin acción.
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (enlazando || item == null) {
                        return;
                    }
                    try {
                        item.setCantidad(Integer.parseInt(s.toString().trim()));
                    } catch (NumberFormatException e) {
                        // La validación al guardar rechaza cantidades no numéricas.
                        item.setCantidad(0);
                    }
                }
            };

            binding.inputItemNombre.addTextChangedListener(nombreWatcher);
            binding.inputItemCantidad.addTextChangedListener(cantidadWatcher);
        }

        /** Carga un ítem sin disparar los watchers. */
        void enlazar(@NonNull ItemCompra item) {
            this.item = item;
            enlazando = true;
            binding.inputItemNombre.setText(item.getNombre());
            binding.inputItemCantidad.setText(String.valueOf(item.getCantidad()));
            enlazando = false;
        }
    }
}
