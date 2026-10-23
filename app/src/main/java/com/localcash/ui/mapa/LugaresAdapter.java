package com.localcash.ui.mapa;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.localcash.R;
import com.localcash.databinding.ItemLugarBinding;
import com.localcash.domain.model.Lugar;
import com.localcash.domain.model.TipoLugar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapter de la lista "Mis lugares" (Feature 009).
 * <p>
 * Muestra solo lugares activos: emoji, nombre y tipo. El clic delega en la
 * Activity, que centra el mapa y abre el detalle del pin (CA-15).
 * </p>
 */
public class LugaresAdapter extends RecyclerView.Adapter<LugaresAdapter.LugarViewHolder> {

    private final List<Lugar> lugares = new ArrayList<>();

    private OnLugarClickListener onLugarClickListener;

    /**
     * Reemplaza la lista completa.
     * <p>
     * Usa {@link DiffUtil} para emitir cambios granulares: solo se redibujan
     * las filas afectadas, sin parpadeos al recibir cada emisión de Room.
     * </p>
     *
     * @param nuevos lugares activos (puede ser {@code null})
     */
    public void setLugares(List<Lugar> nuevos) {
        List<Lugar> siguiente = new ArrayList<>();
        if (nuevos != null) {
            siguiente.addAll(nuevos);
        }
        DiffUtil.DiffResult diferencias =
                DiffUtil.calculateDiff(new LugarDiffCallback(lugares, siguiente));
        lugares.clear();
        lugares.addAll(siguiente);
        diferencias.dispatchUpdatesTo(this);
    }

    /**
     * Registra el callback de clic en una fila.
     *
     * @param listener listener o {@code null}
     */
    public void setOnLugarClickListener(OnLugarClickListener listener) {
        this.onLugarClickListener = listener;
    }

    @NonNull
    @Override
    public LugarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLugarBinding binding = ItemLugarBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LugarViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LugarViewHolder holder, int position) {
        holder.bind(lugares.get(position));
    }

    @Override
    public int getItemCount() {
        return lugares.size();
    }

    /**
     * Traduce el tipo de lugar a su etiqueta visible.
     *
     * @param tipo tipo de dominio
     * @return recurso de texto correspondiente
     */
    public static int etiquetaTipo(@NonNull TipoLugar tipo) {
        switch (tipo) {
            case SUPERMERCADO:
                return R.string.aMapa_tipo_supermercado;
            case FARMACIA:
                return R.string.aMapa_tipo_farmacia;
            case RESTAURANTE:
                return R.string.aMapa_tipo_restaurante;
            case FERRETERIA:
                return R.string.aMapa_tipo_ferreteria;
            case PANADERIA:
                return R.string.aMapa_tipo_panaderia;
            case CAFETERIA:
                return R.string.aMapa_tipo_cafeteria;
            case BANCO:
                return R.string.aMapa_tipo_banco;
            case OTRO:
            default:
                return R.string.aMapa_tipo_otro;
        }
    }

    /** ViewHolder de una fila de la lista de lugares. */
    final class LugarViewHolder extends RecyclerView.ViewHolder {

        private final ItemLugarBinding binding;

        LugarViewHolder(@NonNull ItemLugarBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Vuelca un lugar en la fila.
         *
         * @param lugar lugar a mostrar
         */
        void bind(@NonNull Lugar lugar) {
            binding.tvEmoji.setText(lugar.getEmoji());
            binding.tvNombre.setText(lugar.getNombre());
            binding.tvTipo.setText(etiquetaTipo(lugar.getTipo()));
            binding.getRoot().setOnClickListener(v -> {
                if (onLugarClickListener != null) {
                    onLugarClickListener.onLugarClick(lugar);
                }
            });
        }
    }

    /** Evento de clic sobre un lugar de la lista. */
    public interface OnLugarClickListener {
        /**
         * @param lugar lugar seleccionado
         */
        void onLugarClick(@NonNull Lugar lugar);
    }

    /**
     * Comparador de lugares para {@link DiffUtil}.
     * <p>
     * La identidad es el id del lugar; el contenido compara los campos que
     * realmente se pintan en la fila.
     * </p>
     */
    private static final class LugarDiffCallback extends DiffUtil.Callback {

        @NonNull
        private final List<Lugar> anterior;

        @NonNull
        private final List<Lugar> siguiente;

        LugarDiffCallback(@NonNull List<Lugar> anterior, @NonNull List<Lugar> siguiente) {
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
            return anterior.get(oldItemPosition).getId() == siguiente.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Lugar viejo = anterior.get(oldItemPosition);
            Lugar nuevo = siguiente.get(newItemPosition);
            return viejo.getEstado() == nuevo.getEstado()
                    && viejo.getTipo() == nuevo.getTipo()
                    && Objects.equals(viejo.getNombre(), nuevo.getNombre())
                    && Objects.equals(viejo.getEmoji(), nuevo.getEmoji());
        }
    }
}
