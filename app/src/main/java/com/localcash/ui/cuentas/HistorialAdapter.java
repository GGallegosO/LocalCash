package com.localcash.ui.cuentas;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.localcash.R;
import com.localcash.databinding.ItemTransaccionBinding;
import com.localcash.domain.model.Transaccion;
import com.localcash.domain.model.TipoTransaccion;
import com.localcash.ui.util.CurrencyFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para el RecyclerView del historial de transacciones.
 * Usa ViewBinding (ItemTransaccionBinding) para acceso seguro a las vistas.
 */
public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.TransaccionViewHolder> {

    private static final String TAG = "HistorialAdapter";

    /** Formato de fecha/hora para mostrar en la lista. */
    private static final SimpleDateFormat FORMATO_FECHA =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    /** Lista de transacciones a mostrar. */
    private final List<Transaccion> transacciones = new ArrayList<>();

    @NonNull
    @Override
    public TransaccionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransaccionBinding binding = ItemTransaccionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TransaccionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransaccionViewHolder holder, int position) {
        Transaccion transaccion = transacciones.get(position);
        holder.bind(transaccion);
    }

    @Override
    public int getItemCount() {
        return transacciones.size();
    }

    /**
     * Reemplaza la lista completa de transacciones y notifica al adapter.
     *
     * @param nuevasTransacciones Nueva lista de transacciones (puede ser vacía)
     */
    public void setTransacciones(@NonNull List<Transaccion> nuevasTransacciones) {
        transacciones.clear();
        transacciones.addAll(nuevasTransacciones);
        notifyDataSetChanged();
    }

    /**
     * ViewHolder que usa ViewBinding para la fila del historial.
     */
    static class TransaccionViewHolder extends RecyclerView.ViewHolder {

        private final ItemTransaccionBinding binding;

        TransaccionViewHolder(@NonNull ItemTransaccionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Transaccion transaccion) {
            // Descripción/concepto
            binding.tvDescripcion.setText(transaccion.getConcepto());

            // Fecha formateada
            binding.tvFecha.setText(FORMATO_FECHA.format(transaccion.getFecha()));

            // Monto formateado con signo y color según tipo
            boolean esIngreso = transaccion.getTipo() == TipoTransaccion.INGRESO;
            String montoFormateado = CurrencyFormatter.format(transaccion.getMonto());
            String signo = esIngreso ? "+" : "-";
            binding.tvMonto.setText(signo + montoFormateado);

            // Color del monto: verde para ingresos, rojo para egresos
            int colorRes = esIngreso
                    ? com.google.android.material.R.color.design_default_color_primary
                    : com.google.android.material.R.color.design_default_color_error;
            binding.tvMonto.setTextColor(binding.getRoot().getResources().getColor(colorRes, null));

            // Icono: flecha arriba para ingreso, flecha abajo para egreso
            int iconRes = esIngreso ? R.drawable.ic_arrow_upward : R.drawable.ic_arrow_downward;
            binding.ivIconoTransaccion.setImageResource(iconRes);
        }
    }
}