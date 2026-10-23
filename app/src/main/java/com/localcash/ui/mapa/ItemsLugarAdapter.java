package com.localcash.ui.mapa;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.localcash.databinding.ItemListaLugarBinding;
import com.localcash.domain.model.ItemLugar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapter de la lista de cosas de un lugar (Feature 009, T21).
 * <p>
 * Cada fila muestra la casilla de completado, la descripción y el botón de
 * eliminar. El adapter no decide nada: delega el cambio de estado y el borrado
 * en la hoja que lo contiene, que a su vez llama al ViewModel.
 * </p>
 * <p>
 * Se reutiliza tanto en el formulario (lista inicial en memoria, antes de
 * guardar el lugar) como en el detalle (lista persistida y observable).
 * </p>
 */
public class ItemsLugarAdapter extends RecyclerView.Adapter<ItemsLugarAdapter.ItemViewHolder> {

    private final List<ItemLugar> items = new ArrayList<>();

    private OnItemCompletadoListener onItemCompletadoListener;

    private OnItemEliminarListener onItemEliminarListener;

    /**
     * Reemplaza la lista completa emitiendo solo las diferencias.
     *
     * @param nuevos elementos a mostrar (puede ser {@code null})
     */
    public void setItems(List<ItemLugar> nuevos) {
        List<ItemLugar> siguiente = new ArrayList<>();
        if (nuevos != null) {
            siguiente.addAll(nuevos);
        }
        DiffUtil.DiffResult diferencias =
                DiffUtil.calculateDiff(new ItemDiffCallback(items, siguiente));
        items.clear();
        items.addAll(siguiente);
        diferencias.dispatchUpdatesTo(this);
    }

    /**
     * Copia de la lista actual, para el formulario que aún no persiste.
     *
     * @return copia defensiva de los elementos
     */
    @NonNull
    public List<ItemLugar> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Agrega un elemento al final de la lista en memoria.
     *
     * @param item elemento nuevo
     */
    public void agregar(ItemLugar item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    /**
     * Actualiza en memoria el estado de completado de un elemento. Se usa en el
     * formulario, donde la lista todavía no está persistida.
     *
     * @param item       elemento afectado
     * @param completado nuevo estado
     */
    public void actualizarCompletado(@NonNull ItemLugar item, boolean completado) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == item.getId()
                    && items.get(i).getDescripcion().equals(item.getDescripcion())) {
                items.set(i, new ItemLugar(
                        item.getId(), item.getLugarId(), item.getDescripcion(), completado));
                notifyItemChanged(i);
                return;
            }
        }
    }

    /**
     * Quita un elemento de la lista en memoria.
     *
     * @param item elemento a quitar
     */
    public void quitar(@NonNull ItemLugar item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == item.getId()
                    && items.get(i).getDescripcion().equals(item.getDescripcion())) {
                items.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    /**
     * Registra el callback de marcado/desmarcado.
     *
     * @param listener listener o {@code null}
     */
    public void setOnItemCompletadoListener(OnItemCompletadoListener listener) {
        this.onItemCompletadoListener = listener;
    }

    /**
     * Registra el callback de eliminación.
     *
     * @param listener listener o {@code null}
     */
    public void setOnItemEliminarListener(OnItemEliminarListener listener) {
        this.onItemEliminarListener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListaLugarBinding binding = ItemListaLugarBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** ViewHolder de una cosa de la lista. */
    final class ItemViewHolder extends RecyclerView.ViewHolder {

        private final ItemListaLugarBinding binding;

        ItemViewHolder(@NonNull ItemListaLugarBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Vuelca un elemento en la fila.
         *
         * @param item elemento a mostrar
         */
        void bind(@NonNull ItemLugar item) {
            binding.tvDescripcion.setText(item.getDescripcion());
            binding.checkCompletado.setOnCheckedChangeListener(null);
            binding.checkCompletado.setChecked(item.isCompletado());
            aplicarTachado(item.isCompletado());

            binding.checkCompletado.setOnCheckedChangeListener((button, checked) -> {
                aplicarTachado(checked);
                if (onItemCompletadoListener != null) {
                    onItemCompletadoListener.onItemCompletado(item, checked);
                }
            });
            binding.btnEliminarItem.setOnClickListener(v -> {
                if (onItemEliminarListener != null) {
                    onItemEliminarListener.onItemEliminar(item);
                }
            });
        }

        /**
         * Aplica o quita el tachado según el estado de completado.
         *
         * @param completado {@code true} para tachar la descripción
         */
        private void aplicarTachado(boolean completado) {
            int banderas = binding.tvDescripcion.getPaintFlags();
            if (completado) {
                binding.tvDescripcion.setPaintFlags(
                        banderas | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvDescripcion.setAlpha(0.6f);
            } else {
                binding.tvDescripcion.setPaintFlags(
                        banderas & ~Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvDescripcion.setAlpha(1f);
            }
        }
    }

    /** Evento de marcado/desmarcado de una cosa. */
    public interface OnItemCompletadoListener {
        /**
         * @param item     elemento afectado
         * @param completado nuevo estado
         */
        void onItemCompletado(@NonNull ItemLugar item, boolean completado);
    }

    /** Evento de eliminación de una cosa. */
    public interface OnItemEliminarListener {
        /**
         * @param item elemento a eliminar
         */
        void onItemEliminar(@NonNull ItemLugar item);
    }

    /** Comparador para {@link DiffUtil}. La identidad es el id temporal o real. */
    private static final class ItemDiffCallback extends DiffUtil.Callback {

        @NonNull
        private final List<ItemLugar> anterior;

        @NonNull
        private final List<ItemLugar> siguiente;

        ItemDiffCallback(@NonNull List<ItemLugar> anterior, @NonNull List<ItemLugar> siguiente) {
            this.anterior = anterior;
            this.siguiente = siguiente;
        }

        @Override
        public int getOldListSize() {
            return anterior.size();
        }

        @Override
        public int getNewListSize() {
            return siguiente.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            ItemLugar viejo = anterior.get(oldItemPosition);
            ItemLugar nuevo = siguiente.get(newItemPosition);
            if (viejo.getId() > 0 || nuevo.getId() > 0) {
                return viejo.getId() == nuevo.getId();
            }
            return oldItemPosition == newItemPosition;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ItemLugar viejo = anterior.get(oldItemPosition);
            ItemLugar nuevo = siguiente.get(newItemPosition);
            return viejo.isCompletado() == nuevo.isCompletado()
                    && Objects.equals(viejo.getDescripcion(), nuevo.getDescripcion());
        }
    }
}
