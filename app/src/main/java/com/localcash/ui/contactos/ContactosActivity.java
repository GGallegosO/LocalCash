package com.localcash.ui.contactos;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.localcash.R;
import com.localcash.databinding.ActivityContactosBinding;
import com.localcash.databinding.DialogContactoBinding;
import com.localcash.data.db.AppDatabase;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.Contacto;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity de la lista de contactos (Feature 011).
 * <p>
 * Permite agregar, editar y eliminar contactos. Al tocar un contacto se abre
 * un diálogo de detalle con opciones de editar o eliminar.
 * </p>
 */
public class ContactosActivity extends AppCompatActivity {

    private ActivityContactosBinding binding;
    private ContactoAdapter adapter;
    private ContactosViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContactosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this,
                new ViewModelProvider.Factory() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T extends androidx.lifecycle.ViewModel> T create(
                            @androidx.annotation.NonNull Class<T> modelClass) {
                        return (T) new ContactosViewModel(
                                AppDatabase.getInstance(getApplicationContext()));
                    }
                }).get(ContactosViewModel.class);

        adapter = new ContactoAdapter();
        adapter.setOnContactoClickListener(contacto -> mostrarDialogoDetalle(contacto));
        binding.recyclerContactos.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerContactos.setAdapter(adapter);

        viewModel.getContactos().observe(this, entidades -> {
            List<Contacto> dominio = ContactosViewModel.aDominio(entidades);
            adapter.setContactos(dominio);
            boolean vacio = dominio == null || dominio.isEmpty();
            binding.recyclerContactos.setVisibility(vacio ? View.GONE : View.VISIBLE);
            binding.tvEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
            binding.tvEmptySub.setVisibility(vacio ? View.VISIBLE : View.GONE);
        });

        viewModel.getResultado().observe(this, error -> {
            if (error != null) {
                mostrarError(error);
            }
        });

        binding.fabAgregar.setOnClickListener(v -> mostrarDialogoContacto(null));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** Muestra el diálogo de agregar o editar contacto. */
    private void mostrarDialogoContacto(Contacto contactoExistente) {
        DialogContactoBinding dbinding =
                DialogContactoBinding.inflate(getLayoutInflater());

        String titulo = contactoExistente == null
                ? getString(R.string.aContacto_dialog_titulo)
                : getString(R.string.aContacto_dialog_editar_titulo);

        if (contactoExistente != null) {
            dbinding.etNombre.setText(contactoExistente.getNombre());
            dbinding.etTelefono.setText(contactoExistente.getTelefono());
            dbinding.etEmail.setText(contactoExistente.getEmail());
            dbinding.etNotas.setText(contactoExistente.getNotas());
        }

        dbinding.etNombre.requestFocus();

        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setView(dbinding.getRoot())
                .setPositiveButton(R.string.aContacto_btn_guardar,
                        (d, w) -> {
                            String nombre = obtenerTexto(dbinding.etNombre);
                            String telefono = obtenerTexto(dbinding.etTelefono);
                            String email = obtenerTexto(dbinding.etEmail);
                            String notas = obtenerTexto(dbinding.etNotas);

                            Contacto nuevo = new Contacto(
                                    contactoExistente != null ? contactoExistente.getId() : 0L,
                                    nombre, telefono, email, notas);
                            if (contactoExistente == null) {
                                viewModel.agregarContacto(nuevo);
                            } else {
                                viewModel.actualizarContacto(nuevo);
                            }
                        })
                .setNegativeButton(R.string.aContacto_btn_cancelar, null)
                .show();
    }

    /** Muestra el diálogo de detalle de un contacto (ver/editar/eliminar). */
    private void mostrarDialogoDetalle(Contacto contacto) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(contacto.getNombre())
                .setMessage(contacto.getTelefono() != null ? contacto.getTelefono() : "")
                .setPositiveButton(R.string.aContacto_btn_editar, (d, w) ->
                        mostrarDialogoContacto(contacto))
                .setNegativeButton(R.string.aContacto_btn_eliminar, (d, w) ->
                        confirmarEliminacion(contacto))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    /** Pide confirmación antes de eliminar un contacto. */
    private void confirmarEliminacion(Contacto contacto) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.aContacto_btn_eliminar)
                .setMessage("¿Eliminar el contacto \"" + contacto.getNombre() + "\"?")
                .setPositiveButton(R.string.aContacto_btn_eliminar, (d, w) ->
                        viewModel.eliminarContacto(contacto.getId()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Muestra un toast con el error de negocio. */
    private void mostrarError(ErrorCode error) {
        int mensaje;
        switch (error) {
            case CONTACTO_NOMBRE_VACIO:
                mensaje = R.string.aContacto_error_nombre_vacio;
                break;
            case CONTACTO_TELEFONO_DUPLICADO:
                mensaje = R.string.aContacto_error_telefono_duplicado;
                break;
            case CONTACTO_EMAIL_DUPLICADO:
                mensaje = R.string.aContacto_error_email_duplicado;
                break;
            case CONTACTO_EN_USO:
                mensaje = R.string.aContacto_error_en_uso;
                break;
            default:
                mensaje = R.string.aContacto_error_nombre_vacio;
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    /** Obtiene el texto de un EditText, o null si está vacío. */
    private String obtenerTexto(com.google.android.material.textfield.TextInputEditText editText) {
        String texto = editText.getText() != null ? editText.getText().toString().trim() : null;
        return texto.isEmpty() ? null : texto;
    }
}
