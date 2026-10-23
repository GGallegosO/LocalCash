package com.localcash.ui.mapa;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.localcash.R;
import com.localcash.databinding.BottomSheetLugarBinding;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.EstadoLugar;
import com.localcash.domain.model.ItemLugar;
import com.localcash.domain.model.Lugar;
import com.localcash.domain.model.TipoLugar;
import com.localcash.domain.model.Ubicacion;

import java.util.List;

/**
 * Formulario de alta y edición de un lugar (Feature 009, T17/T20).
 * <p>
 * Encapsula {@code bottom_sheet_lugar.xml} y su {@link BottomSheetDialog}. En
 * el alta incluye la lista inicial de cosas, que permanece en memoria hasta
 * que el usuario guarda; cancelar no persiste nada. En la edición oculta esa
 * lista porque su gestión vive en el detalle (T21).
 * </p>
 * <p>
 * No contiene reglas de negocio: construye el modelo de dominio y delega la
 * validación y la persistencia en {@link MapaViewModel} y {@code LugarService}.
 * </p>
 */
public final class LugarFormSheet {

    /** Paleta de emojis disponibles para el pin. */
    private static final String[] EMOJIS = {
            "🛒", "💊", "🍽️", "🛠️", "🥖", "☕", "🏦", "📍",
            "🏠", "🏢", "🏥", "⛽", "🍺", "🎬", "🏫", "📦"
    };

    private final Context context;

    private final MapaViewModel viewModel;

    /** Lugar a editar o {@code null} para un alta. */
    @Nullable
    private final Lugar existente;

    /** Coordenada del alta (pulsación larga); ignorada al editar. */
    @Nullable
    private final Ubicacion ubicacion;

    /** Acción a ejecutar tras guardar (por ejemplo, centrar el mapa). */
    @Nullable
    private final Runnable alGuardar;

    private final BottomSheetLugarBinding binding;

    private final ItemsLugarAdapter itemsAdapter = new ItemsLugarAdapter();

    private TipoLugar tipoSeleccionado;

    private String emojiSeleccionado;

    /** {@code true} si el usuario eligió el emoji a mano. */
    private boolean emojiManual;

    /**
     * Construye la hoja. No la muestra todavía: llama a {@link #mostrar()}.
     *
     * @param context    contexto de Activity
     * @param viewModel  ViewModel del mapa
     * @param existente  lugar a editar o {@code null} para crear
     * @param ubicacion  coordenada del alta (obligatoria si {@code existente} es nulo)
     * @param alGuardar  acción opcional tras guardar
     */
    public LugarFormSheet(@NonNull Context context,
                          @NonNull MapaViewModel viewModel,
                          @Nullable Lugar existente,
                          @Nullable Ubicacion ubicacion,
                          @Nullable Runnable alGuardar) {
        this.context = context;
        this.viewModel = viewModel;
        this.existente = existente;
        this.ubicacion = ubicacion;
        this.alGuardar = alGuardar;
        this.binding = BottomSheetLugarBinding.inflate(
                android.view.LayoutInflater.from(context));
    }

    /**
     * Infla y muestra el formulario, precargado si se trata de una edición.
     */
    public void mostrar() {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(binding.getRoot());

        configurarTipo();
        configurarEmoji();
        configurarItems();
        precargar();

        binding.btnGuardar.setOnClickListener(v -> guardar(dialog));
        binding.btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Puebla el selector de tipos con las etiquetas visibles del dominio.
     */
    private void configurarTipo() {
        TipoLugar[] tipos = TipoLugar.values();
        String[] etiquetas = new String[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            etiquetas[i] = context.getString(LugaresAdapter.etiquetaTipo(tipos[i]));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, android.R.layout.simple_list_item_1, etiquetas);
        MaterialAutoCompleteTextView spinner = binding.spinnerTipo;
        spinner.setAdapter(adapter);
        spinner.setThreshold(0);
        spinner.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < tipos.length) {
                cambiarTipo(tipos[position]);
            }
        });
    }

    /**
     * Cablea el botón de emoji con una paleta simple.
     */
    private void configurarEmoji() {
        binding.btnEmoji.setOnClickListener(v -> new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.aMapa_emoji_picker_titulo)
                .setItems(EMOJIS, (dialog, which) -> {
                    emojiManual = true;
                    emojiSeleccionado = EMOJIS[which];
                    binding.btnEmoji.setText(emojiSeleccionado);
                })
                .setNegativeButton(R.string.aMapa_btn_cancelar, null)
                .show());
    }

    /**
     * Configura la lista inicial de cosas (solo alta).
     */
    private void configurarItems() {
        binding.recyclerItems.setLayoutManager(new LinearLayoutManager(context));
        binding.recyclerItems.setAdapter(itemsAdapter);
        actualizarEstadoItems();

        itemsAdapter.setOnItemEliminarListener(item -> {
            itemsAdapter.quitar(item);
            actualizarEstadoItems();
        });
        itemsAdapter.setOnItemCompletadoListener(itemsAdapter::actualizarCompletado);

        binding.btnAgregarCosa.setOnClickListener(v -> {
            CharSequence texto = binding.inputNuevaCosa.getText();
            String descripcion = texto != null ? texto.toString().trim() : "";
            if (descripcion.isEmpty()) {
                return;
            }
            itemsAdapter.agregar(new ItemLugar(0L, 0L, descripcion, false));
            binding.inputNuevaCosa.setText(null);
            actualizarEstadoItems();
            ocultarTeclado();
        });
    }

    /**
     * Vuelca el lugar existente en los campos, o los valores por defecto de un
     * alta.
     */
    private void precargar() {
        if (existente != null) {
            tipoSeleccionado = existente.getTipo();
            emojiSeleccionado = existente.getEmoji();
            emojiManual = true;
            binding.tvTituloFormulario.setText(R.string.aMapa_titulo_editar_lugar);
            binding.inputNombre.setText(existente.getNombre());
            binding.inputDescripcion.setText(existente.getDescripcion());
            binding.groupItemsFormulario.setVisibility(android.view.View.GONE);
        } else {
            tipoSeleccionado = TipoLugar.SUPERMERCADO;
            emojiSeleccionado = tipoSeleccionado.getEmojiPorDefecto();
            binding.tvTituloFormulario.setText(R.string.aMapa_titulo_nuevo_lugar);
            binding.inputNombre.setText(nombreTipo(tipoSeleccionado));
        }
        binding.spinnerTipo.setText(nombreTipo(tipoSeleccionado), false);
        binding.btnEmoji.setText(emojiSeleccionado);
    }

    /**
     * Aplica un tipo nuevo: actualiza emoji y, si el nombre seguía siendo el
     * automático del tipo anterior, lo reemplaza por el del tipo nuevo.
     *
     * @param nuevo tipo elegido
     */
    private void cambiarTipo(@NonNull TipoLugar nuevo) {
        CharSequence texto = binding.inputNombre.getText();
        String nombreActual = texto != null ? texto.toString().trim() : "";
        boolean nombreAutomatico = nombreActual.isEmpty()
                || nombreActual.equals(nombreTipo(tipoSeleccionado));

        tipoSeleccionado = nuevo;
        if (nombreAutomatico) {
            binding.inputNombre.setText(nombreTipo(nuevo));
        }
        if (!emojiManual) {
            emojiSeleccionado = nuevo.getEmojiPorDefecto();
            binding.btnEmoji.setText(emojiSeleccionado);
        }
        binding.inputLayoutNombre.setError(null);
    }

    /**
     * Valida el formulario y persiste el lugar.
     *
     * @param dialog hoja que se cierra al guardar correctamente
     */
    private void guardar(@NonNull BottomSheetDialog dialog) {
        CharSequence texto = binding.inputNombre.getText();
        String nombre = texto != null ? texto.toString().trim() : "";
        if (nombre.isEmpty()) {
            binding.inputLayoutNombre.setError(
                    context.getString(R.string.aMapa_error_nombre_vacio));
            return;
        }
        binding.inputLayoutNombre.setError(null);

        CharSequence descTexto = binding.inputDescripcion.getText();
        String descripcion = descTexto != null ? descTexto.toString().trim() : "";

        if (existente != null) {
            Lugar actualizado = new Lugar(
                    existente.getId(),
                    existente.getTransaccionId(),
                    nombre,
                    tipoSeleccionado,
                    emojiSeleccionado,
                    descripcion,
                    existente.getUbicacion(),
                    existente.getEstado());
            viewModel.actualizarLugar(actualizado);
            Toast.makeText(context, R.string.aMapa_toast_lugar_guardado,
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (alGuardar != null) {
                alGuardar.run();
            }
            return;
        }

        if (ubicacion == null) {
            Toast.makeText(context, R.string.aMapa_toast_lugar_error,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Lugar nuevo = new Lugar(
                0L,
                null,
                nombre,
                tipoSeleccionado,
                emojiSeleccionado,
                descripcion,
                ubicacion,
                EstadoLugar.ABIERTO);
        List<ItemLugar> items = itemsAdapter.getItems();

        viewModel.crearLugar(nuevo, items).observe(dialog, new Observer<ErrorCode>() {
            @Override
            public void onChanged(ErrorCode errorCode) {
                if (errorCode == null) {
                    Toast.makeText(context, R.string.aMapa_toast_lugar_guardado,
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (alGuardar != null) {
                        alGuardar.run();
                    }
                } else {
                    Toast.makeText(context, R.string.aMapa_toast_lugar_error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Muestra u oculta el estado vacío de la lista inicial.
     */
    private void actualizarEstadoItems() {
        boolean vacio = itemsAdapter.getItemCount() == 0;
        binding.tvSinItems.setVisibility(vacio ? android.view.View.VISIBLE
                : android.view.View.GONE);
        binding.recyclerItems.setVisibility(vacio ? android.view.View.GONE
                : android.view.View.VISIBLE);
    }

    /**
     * @param tipo tipo de lugar
     * @return etiqueta visible del tipo
     */
    @NonNull
    private String nombreTipo(@NonNull TipoLugar tipo) {
        return context.getString(LugaresAdapter.etiquetaTipo(tipo));
    }

    /**
     * Cierra el teclado tras agregar una cosa, para no tapar la lista.
     */
    private void ocultarTeclado() {
        InputMethodManager teclado = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (teclado != null) {
            teclado.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        }
    }
}
