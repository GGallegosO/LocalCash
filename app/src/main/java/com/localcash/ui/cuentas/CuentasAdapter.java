package com.localcash.ui.cuentas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.localcash.R;
import com.localcash.data.entities.Cuenta;
import com.localcash.ui.util.CurrencyFormatter;

import java.util.Collections;
import java.util.List;

/**
 * Adapter del RecyclerView de la Vista 1 (Cuentas).
 * <p>
 * Mapea cada entidad Room {@link Cuenta} a una fila de {@code item_cuenta.xml}:
 * ícono según el tipo, nombre y saldo formateado en pesos chilenos con
 * {@link CurrencyFormatter} (operación de presentación; el cálculo del dinero
 * vive en {@code domain/}).
 * </p>
 * <p>
 * Sin DiffUtil: la pantalla muestra una lista pequeña y siempre completa, y
 * Room re-emite la lista entera ante cada cambio (YAGNI).
 * </p>
 */
public class CuentasAdapter extends RecyclerView.Adapter<CuentasAdapter.CuentasViewHolder> {

    /** Cuentas a mostrar. Lista vacía hasta que el ViewModel emita. */
    private List<Cuenta> cuentas = Collections.emptyList();

    /** Callback de long-press; lo implementa la Activity para mostrar el menú. */
    private OnCuentaLongClickListener longClickListener;

    /**
     * Callback que notifica a la UI que una fila fue mantenida presionada.
     */
    public interface OnCuentaLongClickListener {

        /**
         * @param cuenta Cuenta de la fila presionada (nunca {@code null})
         */
        void onCuentaLongClick(@NonNull Cuenta cuenta);
    }

    /**
     * Registra el listener de long-press.
     *
     * @param listener Listener (típicamente la Activity)
     */
    public void setOnCuentaLongClickListener(@Nullable OnCuentaLongClickListener listener) {
        this.longClickListener = listener;
    }

    /**
     * Contenedor de la fila de {@code item_cuenta.xml}.
     * <p>
     * Resuelve las vistas una sola vez por fila (patrón ViewHolder); reutiliza
     * referencias mientras se hace scroll.
     * </p>
     */
    static class CuentasViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivIconoCuenta;
        private final TextView tvNombreCuenta;
        private final TextView tvMontoCuenta;

        /**
         * @param itemView Vista de la fila ya inflada
         */
        CuentasViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIconoCuenta = itemView.findViewById(R.id.ivIconoCuenta);
            tvNombreCuenta = itemView.findViewById(R.id.tvNombreCuenta);
            tvMontoCuenta = itemView.findViewById(R.id.tvMontoCuenta);
        }
    }

    @NonNull
    @Override
    public CuentasViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View fila = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cuenta, parent, false);
        return new CuentasViewHolder(fila);
    }

    @Override
    public void onBindViewHolder(@NonNull CuentasViewHolder holder, int position) {
        Cuenta cuenta = cuentas.get(position);

        // Ícono por tipo (dominio cerrado: EFECTIVO / AHORRO / TARJETA, persistido con name())
        int iconoRes;
        switch (cuenta.getTipo()) {
            case "AHORRO":
                iconoRes = R.drawable.ic_savings;
                break;
            case "TARJETA":
                iconoRes = R.drawable.ic_credit_card;
                break;
            case "EFECTIVO":
            default:
                iconoRes = R.drawable.ic_account_balance_wallet;
                break;
        }
        holder.ivIconoCuenta.setImageResource(iconoRes);

        // Nombre y saldo en CLP (formato de presentación, p. ej. "$750.000 CLP")
        holder.tvNombreCuenta.setText(cuenta.getNombre());
        holder.tvMontoCuenta.setText(CurrencyFormatter.formatWithCurrency(cuenta.getSaldo()));

        // Long-press: abre el menú de gestión (editar nombre/saldo, eliminar)
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onCuentaLongClick(cuenta);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return cuentas.size();
    }

    /**
     * Reemplaza el listado mostrado. Se llama desde {@code CuentasActivity}
     * cuando el {@code LiveData} del ViewModel emite nuevas cuentas.
     * <p>
     * Room re-emite la lista completa ante cada cambio, así que se traduce en
     * eventos granulares (insertar/quitar del final + refresco de contenido)
     * en lugar de {@code notifyDataSetChanged()}: linter lo exige
     * ({@code NotifyDataSetChanged}) y evita re-animar toda la lista.
     * </p>
     *
     * @param nuevasCuentas Cuentas nuevas ({@code null} = lista vacía)
     */
    public void setCuentas(@Nullable List<Cuenta> nuevasCuentas) {
        List<Cuenta> nuevaLista = nuevasCuentas == null ? Collections.emptyList() : nuevasCuentas;
        int tamanoAnterior = cuentas.size();
        int tamanoNuevo = nuevaLista.size();
        cuentas = nuevaLista;

        if (tamanoNuevo > tamanoAnterior) {
            notifyItemRangeInserted(tamanoAnterior, tamanoNuevo - tamanoAnterior);
            notifyItemRangeChanged(0, tamanoAnterior);
        } else if (tamanoAnterior > tamanoNuevo) {
            notifyItemRangeRemoved(tamanoNuevo, tamanoAnterior - tamanoNuevo);
            notifyItemRangeChanged(0, tamanoNuevo);
        } else {
            notifyItemRangeChanged(0, tamanoNuevo);
        }
    }
}
