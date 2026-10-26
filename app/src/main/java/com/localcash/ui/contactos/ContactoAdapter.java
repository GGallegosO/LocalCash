package com.localcash.ui.contactos;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.localcash.R;
import com.localcash.databinding.ItemContactoBinding;
import com.localcash.domain.model.Contacto;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de la lista de contactos (Feature 011).
 * <p>
 * Muestra iniciales, nombre y teléfono/email de cada contacto. El clic delega
 * en la Activity para abrir el detalle.
 * </p>
 */
public class ContactoAdapter extends RecyclerView.Adapter<ContactoAdapter.ContactoViewHolder> {

    private final List<Contacto> contactos = new ArrayList<>();

    private OnContactoClickListener onContactoClickListener;

    /**
     * Reemplaza la lista completa con DiffUtil.
     */
    public void setContactos(List<Contacto> nuevos) {
        List<Contacto> siguiente = new ArrayList<>();
        if (nuevos != null) {
            siguiente.addAll(nuevos);
        }
        DiffUtil.DiffResult diff =
                DiffUtil.calculateDiff(new ContactoDiffCallback(contactos, siguiente));
        contactos.clear();
        contactos.addAll(siguiente);
        diff.dispatchUpdatesTo(this);
    }

    /**
     * Registra el callback de clic en una fila.
     */
    public void setOnContactoClickListener(OnContactoClickListener listener) {
        this.onContactoClickListener = listener;
    }

    @NonNull
    @Override
    public ContactoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContactoBinding binding =
                ItemContactoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ContactoViewHolder(binding, onContactoClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactoViewHolder holder, int position) {
        holder.bind(contactos.get(position));
    }

    @Override
    public int getItemCount() {
        return contactos.size();
    }

    /**
     * ViewHolder de una fila de contacto.
     */
    static class ContactoViewHolder extends RecyclerView.ViewHolder {

        private final ItemContactoBinding binding;
        private final OnContactoClickListener listener;

        ContactoViewHolder(ItemContactoBinding binding, OnContactoClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(Contacto contacto) {
            binding.tvNombre.setText(contacto.getNombre());

            StringBuilder detalle = new StringBuilder();
            if (contacto.getTelefono() != null && !contacto.getTelefono().isEmpty()) {
                detalle.append(contacto.getTelefono());
            }
            if (contacto.getEmail() != null && !contacto.getEmail().isEmpty()) {
                if (detalle.length() > 0) {
                    detalle.append(" · ");
                }
                detalle.append(contacto.getEmail());
            }
            binding.tvContacto.setText(detalle.length() > 0 ? detalle.toString() : "-");

            // Iniciales: primera letra del nombre y del apellido (si hay)
            String iniciales = inicialesDe(contacto.getNombre());
            binding.tvIniciales.setText(iniciales);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactoClick(contacto);
                }
            });
        }

        private String inicialesDe(String nombre) {
            if (nombre == null || nombre.isEmpty()) {
                return "?";
            }
            String[] partes = nombre.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            sb.append(Character.toUpperCase(partes[0].charAt(0)));
            if (partes.length > 1) {
                sb.append(Character.toUpperCase(partes[partes.length - 1].charAt(0)));
            }
            return sb.toString();
        }
    }

    /**
     * Callback de clic en un contacto.
     */
    public interface OnContactoClickListener {
        void onContactoClick(Contacto contacto);
    }

    /**
     * DiffUtil para comparar contactos por id.
     */
    private static class ContactoDiffCallback extends DiffUtil.Callback {

        private final List<Contacto> viejo;
        private final List<Contacto> nuevo;

        ContactoDiffCallback(List<Contacto> viejo, List<Contacto> nuevo) {
            this.viejo = viejo;
            this.nuevo = nuevo;
        }

        @Override
        public int getOldListSize() {
            return viejo.size();
        }

        @Override
        public int getNewListSize() {
            return nuevo.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return viejo.get(oldPos).getId() == nuevo.get(newPos).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Contacto v = viejo.get(oldPos);
            Contacto n = nuevo.get(newPos);
            return v.getId() == n.getId()
                    && java.util.Objects.equals(v.getNombre(), n.getNombre())
                    && java.util.Objects.equals(v.getTelefono(), n.getTelefono())
                    && java.util.Objects.equals(v.getEmail(), n.getEmail())
                    && java.util.Objects.equals(v.getNotas(), n.getNotas());
        }
    }
}
