package com.localcash.ui.cuentas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.localcash.R;
import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Cuenta;
import com.localcash.data.entities.ItemCompra;
import com.localcash.data.entities.LugarCompra;
import com.localcash.data.preferences.TiposLugarPreferencias;
import com.localcash.data.relations.LugarCompraConItems;
import com.localcash.databinding.ActivityCuentasBinding;
import com.localcash.databinding.BottomSheetTransaccionBinding;
import com.localcash.databinding.DialogAgregarTipoLugarBinding;
import com.localcash.databinding.DialogCrearLugarCompraBinding;
import com.localcash.databinding.ViewCuentasBinding;
import com.localcash.databinding.ViewHistorialBinding;
import com.localcash.databinding.ViewMapaBinding;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.TipoCuenta;
import com.localcash.domain.model.TipoTransaccion;
import com.localcash.domain.model.Transaccion;
import com.localcash.ui.BaseActivity;
import com.localcash.ui.compras.ItemCompraCreacionAdapter;
import com.localcash.ui.compras.LugarCompraDetailActivity;
import com.localcash.ui.mapa.MapaLecturaController;
import com.localcash.ui.mapa.MapaPickerActivity;
import com.localcash.ui.mapa.MapaViewModel;
import com.localcash.ui.theme.ThemeManager;
import com.localcash.ui.util.CurrencyFormatter;

import org.osmdroid.config.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity principal de LocalCash (Módulo 1): 4 vistas en un {@code ViewFlipper}
 * controladas por un {@code BottomNavigationView}.
 * <p>
 * La Vista 1 (Cuentas) está conectada a Room vía {@link CuentasViewModel}:
 * lista de cuentas y saldo total consolidado se actualizan reactivamente. La
 * Vista 2 (Movimientos) está conectada a Room vía {@link HistorialViewModel}:
 * el historial de movimientos se actualiza reactivamente.
 * </p>
 */
public class CuentasActivity extends BaseActivity
        implements CuentasAdapter.OnCuentaLongClickListener {

    /** Etiqueta de logs de la Activity. */
    private static final String TAG = "CuentasActivity";

    /** Archivo de preferencias donde osmdroid guarda su configuración. */
    private static final String PREFS_OSMDROID = "osmdroid";

    private ActivityCuentasBinding binding;

    /** Binding de la Vista 1 (se infla dentro del ViewFlipper, fuera de activity_cuentas.xml). */
    private ViewCuentasBinding vistaCuentas;

    /** Binding de la Vista 1 (Historial/Movimientos), inflado dentro del ViewFlipper. */
    private ViewHistorialBinding vistaHistorial;

    /** Binding de la Vista 2 (Mapa), inflado dentro del ViewFlipper. */
    private ViewMapaBinding vistaMapa;

    /** Controlador del mapa de lectura (Vista 2); {@code null} hasta configurarla. */
    private MapaLecturaController mapaController;

    /** Binding del BottomSheet de transacción, mientras está abierto ({@code null} si cerrado). */
    private BottomSheetTransaccionBinding sheetTransaccionBinding;

    /** Adapter para el RecyclerView del historial. */
    private HistorialAdapter historialAdapter;

    /** ViewModel de la Vista 1: usado para insertar cuentas desde el diálogo. */
    private CuentasViewModel viewModel;

    /** ViewModel de la Vista 2: registra movimientos (ingreso/egreso). */
    private TransaccionViewModel transaccionViewModel;

    /** ViewModel del historial (Movimientos): observa Room y expone el dominio. */
    private HistorialViewModel historialViewModel;

    /** ViewModel del mapa (Vista 2): observa los movimientos geolocalizados. */
    private MapaViewModel mapaViewModel;

    /** Cuentas activas asociadas, en el mismo orden que el adapter del spinner. */
    private final List<Cuenta> cuentasSpinner = new ArrayList<>();

    /** Id de la cuenta seleccionada en el spinner ({@code -1} si no hay selección). */
    private long cuentaSeleccionadaId = -1L;

    /**
     * Coordenada elegida en el mapa ({@code null} = sin ubicación). Se conserva
     * en memoria durante el registro; su persistencia se aborda en T9. Rige la
     * regla "ambas o ninguna": si una es {@code null}, la otra también.
     */
    private Double latitudSeleccionada;
    private Double longitudSeleccionada;

    /** Lanzador al selector de ubicación ({@link MapaPickerActivity}). */
    private ActivityResultLauncher<Intent> launcherUbicacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid: cargar configuración (rutas de caché, user-agent) ANTES de
        // inflar cualquier MapView (la Vista 2 lo hace más abajo, dentro del
        // ViewFlipper). Sin red: solo define dónde cachear y el user-agent.
        SharedPreferences prefs = getSharedPreferences(PREFS_OSMDROID, MODE_PRIVATE);
        Configuration.getInstance().load(getApplicationContext(), prefs);
        Configuration.getInstance().setUserAgentValue(getPackageName());

        binding = ActivityCuentasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurar Toolbar como ActionBar
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.aCuentas_toolbar_title);
        }

        // Toggle de tema: misma lógica persistente que Login/Register (ThemeManager)
        binding.btnThemeToggle.setOnClickListener(v ->
                ThemeManager.setDarkMode(this, !ThemeManager.isDarkMode(this)));

        // Configurar BottomNavigationView para cambiar entre vistas del ViewFlipper
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_cuentas) {
                binding.viewFlipper.setDisplayedChild(0);
                actualizarTituloToolbar(0);
                return true;
            } else if (itemId == R.id.nav_movimientos) {
                binding.viewFlipper.setDisplayedChild(1);
                actualizarTituloToolbar(1);
                return true;
            } else if (itemId == R.id.nav_mapa) {
                binding.viewFlipper.setDisplayedChild(2);
                actualizarTituloToolbar(2);
                return true;
            } else if (itemId == R.id.nav_contactos) {
                binding.viewFlipper.setDisplayedChild(3);
                actualizarTituloToolbar(3);
                return true;
            }
            return false;
        });

        // Inflar los 4 layouts internos dentro del ViewFlipper
        // Vista 0: Cuentas (binding propio: sus vistas no forman parte de activity_cuentas.xml)
        vistaCuentas = ViewCuentasBinding.inflate(getLayoutInflater(), binding.viewFlipper, true);
        // Vista 1: Historial/Movimientos
        vistaHistorial = ViewHistorialBinding.inflate(
                getLayoutInflater(), binding.viewFlipper, true);
        // Vista 2: Mapa
        vistaMapa = ViewMapaBinding.inflate(getLayoutInflater(), binding.viewFlipper, true);
        // Vista 3: Contactos
        getLayoutInflater().inflate(R.layout.view_contactos, binding.viewFlipper);

        // Restaurar el índice del ViewFlipper desde el estado guardado (preserva la vista actual al cambiar tema)
        if (savedInstanceState != null) {
            int savedViewIndex = savedInstanceState.getInt("viewFlipperIndex", 0);
            // Sanea el índice al rango actual (0..3): un estado guardado con el
            // índice de la extinta vista "Nuevo Movimiento" (2) no debe romper
            // la restauración.
            if (savedViewIndex < 0 || savedViewIndex > 3) {
                savedViewIndex = 0;
            }
            // Aplicar después de inflar las vistas pero antes de establecer los listeners
            binding.viewFlipper.setDisplayedChild(savedViewIndex);
            // También actualizar el elemento seleccionado del BottomNavigationView para mantener la sincronización
            switch (savedViewIndex) {
                case 0:
                    binding.bottomNav.setSelectedItemId(R.id.nav_cuentas);
                    break;
                case 1:
                    binding.bottomNav.setSelectedItemId(R.id.nav_movimientos);
                    break;
                case 2:
                    binding.bottomNav.setSelectedItemId(R.id.nav_mapa);
                    break;
                case 3:
                    binding.bottomNav.setSelectedItemId(R.id.nav_contactos);
                    break;
            }
            // Actualizar el título del Toolbar según la vista restaurada
            actualizarTituloToolbar(savedViewIndex);
        } else {
            // Establecer la vista inicial si no hay estado guardado
            binding.viewFlipper.setDisplayedChild(0);
            actualizarTituloToolbar(0);
        }

        // Conectar la Vista 1 con Room (ViewModel + Adapter)
        configurarVistaCuentas();

        // Conectar la Vista 2 (Historial/Movimientos) con Room (ViewModel + Adapter)
        configurarVistaHistorial();

        // Configurar el mapa de lectura (Vista 2 del ViewFlipper): teselas locales y zoom
        configurarVistaMapa();

        // Preparar el ViewModel de transacciones (alimenta el BottomSheet del FAB)
        configurarTransaccionViewModel();

        // Lanzador al selector de ubicación (debe registrarse antes de STARTED)
        registrarLauncherUbicacion();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Guardar el índice actual del ViewFlipper para preservar la vista seleccionada
        outState.putInt("viewFlipperIndex", binding.viewFlipper.getDisplayedChild());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // El MapView de la Vista 2 debe seguir el ciclo de vida de la Activity.
        if (vistaMapa != null) {
            vistaMapa.mapaLectura.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (vistaMapa != null) {
            vistaMapa.mapaLectura.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Libera el tile provider y sus hilos (evita fugas del MapView).
        if (vistaMapa != null) {
            vistaMapa.mapaLectura.onDetach();
        }
        super.onDestroy();
    }

    /**
     * Conecta el RecyclerView de cuentas y la card de saldo total con el
     * {@link CuentasViewModel}: ambos se actualizan solos ante cada cambio en
     * la tabla {@code cuentas} (LiveData).
     */
    private void configurarVistaCuentas() {
        viewModel = new ViewModelProvider(this).get(CuentasViewModel.class);

        CuentasAdapter adapter = new CuentasAdapter();
        adapter.setOnCuentaLongClickListener(this);
        vistaCuentas.recyclerCuentas.setLayoutManager(new LinearLayoutManager(this));
        vistaCuentas.recyclerCuentas.setAdapter(adapter);

        viewModel.getTodasLasCuentas().observe(this, cuentas -> {
            boolean hayCuentas = cuentas != null && !cuentas.isEmpty();
            vistaCuentas.recyclerCuentas.setVisibility(hayCuentas ? View.VISIBLE : View.GONE);
            vistaCuentas.tvEmptyState.setVisibility(hayCuentas ? View.GONE : View.VISIBLE);
            adapter.setCuentas(cuentas);
        });

        viewModel.getSaldoTotal().observe(this, saldoTotal ->
                vistaCuentas.tvSaldoTotal.setText(CurrencyFormatter.format(saldoTotal)));

        // FAB: abrir diálogo para agregar una cuenta nueva (T15.5)
        vistaCuentas.fabNuevaCuenta.setOnClickListener(v -> mostrarDialogNuevaCuenta());
    }

    /**
     * Configura la Vista 2 (Historial/Movimientos): conecta el RecyclerView con
     * el {@link HistorialViewModel}, que observa Room y entrega la lista de
     * movimientos en modelo de dominio. El FAB abre el BottomSheet de registro.
     * <p>
     * La lista y el estado vacío se actualizan solos ante cada cambio en la tabla
     * {@code transacciones} (LiveData): un movimiento guardado aparece sin
     * recargar la pantalla a mano.
     * </p>
     */
    private void configurarVistaHistorial() {
        historialAdapter = new HistorialAdapter();
        vistaHistorial.recyclerHistorial.setLayoutManager(new LinearLayoutManager(this));
        vistaHistorial.recyclerHistorial.setAdapter(historialAdapter);

        // El ViewModel recibe el DAO por constructor: no sirve la fábrica por
        // defecto, se necesita una ViewModelProvider.Factory que lo construya.
        historialViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new HistorialViewModel(
                        AppDatabase.getInstance(getApplicationContext()).transaccionDao());
            }
        }).get(HistorialViewModel.class);

        // La lista y el estado vacío reaccionan a la base de datos (LiveData).
        historialViewModel.getTransacciones().observe(this, transacciones -> {
            List<Transaccion> lista = transacciones != null ? transacciones : new ArrayList<>();
            boolean hayTransacciones = !lista.isEmpty();
            historialAdapter.setTransacciones(lista);
            vistaHistorial.recyclerHistorial.setVisibility(
                    hayTransacciones ? View.VISIBLE : View.GONE);
            vistaHistorial.tvEmptyHistorial.setVisibility(
                    hayTransacciones ? View.GONE : View.VISIBLE);
        });

        // FAB: abrir el BottomSheet para registrar un movimiento nuevo.
        vistaHistorial.fabNuevaTransaccion.setOnClickListener(v -> mostrarBottomSheetTransaccion());
    }

    /**
     * Configura la Vista 2 (Mapa) en modo lectura: monta el
     * {@link MapaLecturaController} sobre su {@code MapView} (teselas locales
     * offline y encuadre inicial) y enlaza los botones de zoom.
     * <p>
     * El ciclo de vida del {@code MapView} lo delega la Activity
     * ({@code onResume}/{@code onPause}/{@code onDetach}).
     * </p>
     */
    private void configurarVistaMapa() {
        mapaController = new MapaLecturaController(vistaMapa.mapaLectura);

        // El ViewModel recibe el DAO por constructor: necesita una
        // ViewModelProvider.Factory, igual que HistorialViewModel.
        mapaViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                AppDatabase baseDatos = AppDatabase.getInstance(getApplicationContext());
                return (T) new MapaViewModel(
                        baseDatos.transaccionDao(), baseDatos.lugarCompraDao());
            }
        }).get(MapaViewModel.class);

        // Un pin por movimiento geolocalizado. Sin movimientos no se superpone
        // ningún mensaje: el mapa queda limpio para que el usuario lo explore.
        mapaViewModel.getTransaccionesConUbicacion().observe(this, transacciones -> {
            List<Transaccion> lista = transacciones != null ? transacciones : new ArrayList<>();
            mapaController.mostrarMovimientos(lista);
        });

        // Un pin verde por lugar de compra (Feature 010). Room reejecuta la
        // consulta al insertar, así que el pin recién creado aparece solo.
        mapaViewModel.getLugaresCompra().observe(this, lugares -> {
            List<LugarCompraConItems> lista =
                    lugares != null ? lugares : new ArrayList<>();
            mapaController.mostrarLugaresCompra(lista);
        });

        // Pulsación larga sobre el mapa vacío: abre el diálogo de creación con
        // las coordenadas del punto presionado (Feature 010, Fase 2).
        mapaController.setOnLongPressListener(this::mostrarDialogCrearLugarCompra);

        // Interacción con pines de compra: "Ver detalle" y long press abren el
        // detalle (placeholder hasta la Fase 4).
        mapaController.setOnLugarCompraInteractionListener(
                new MapaLecturaController.OnLugarCompraInteractionListener() {
                    @Override
                    public void onVerDetalle(long lugarCompraId) {
                        abrirDetalleLugar(lugarCompraId);
                    }

                    @Override
                    public void onLongPress(long lugarCompraId) {
                        abrirDetalleLugar(lugarCompraId);
                    }
                });
    }

    /**
     * Abre la Activity de detalle de un lugar de compra.
     *
     * @param lugarCompraId id del lugar
     */
    private void abrirDetalleLugar(long lugarCompraId) {
        startActivity(LugarCompraDetailActivity.crearIntent(this, lugarCompraId));
    }

    /**
     * Muestra el diálogo para crear un lugar de compra en las coordenadas de
     * una pulsación larga sobre el mapa (Feature 010, Fases 2 y 2.5).
     * <p>
     * El diálogo se infla con ViewBinding. El tipo se elige de un Spinner que
     * combina los predefinidos ({@code strings.xml}) con los personalizados
     * persistidos en {@link TiposLugarPreferencias}. El gasto es opcional
     * ({@code null} = sin gasto estimado) y la lista de ítems se edita en un
     * RecyclerView. La persistencia se delega en
     * {@link MapaViewModel#insertarLugar(LugarCompra, List)} para no tocar Room
     * desde la vista.
     * </p>
     *
     * @param latitud  latitud del punto presionado
     * @param longitud longitud del punto presionado
     */
    private void mostrarDialogCrearLugarCompra(double latitud, double longitud) {
        DialogCrearLugarCompraBinding dialogBinding =
                DialogCrearLugarCompraBinding.inflate(getLayoutInflater());

        ItemCompraCreacionAdapter itemsAdapter = new ItemCompraCreacionAdapter();
        dialogBinding.rvItemsCompra.setLayoutManager(new LinearLayoutManager(this));
        dialogBinding.rvItemsCompra.setHasFixedSize(true);
        dialogBinding.rvItemsCompra.setAdapter(itemsAdapter);
        poblarSpinnerTipos(dialogBinding, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_crear_lugar_titulo)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.dialog_crear_lugar_guardar, null)
                .setNegativeButton(R.string.dialog_crear_lugar_cancelar, null)
                .create();
        dialog.show();

        // El teclado numérico no debe tapar los campos en pantallas pequeñas.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialogBinding.spinnerTipoLugar.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        Object seleccionado = parent.getItemAtPosition(position);
                        boolean personalizado = seleccionado instanceof String
                                && TiposLugarPreferencias.esPersonalizado(
                                        CuentasActivity.this, (String) seleccionado);
                        dialogBinding.btnEliminarTipo.setEnabled(personalizado);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        dialogBinding.btnEliminarTipo.setEnabled(false);
                    }
                });

        dialogBinding.btnAgregarTipo.setOnClickListener(
                v -> mostrarDialogAgregarTipo(dialogBinding));
        dialogBinding.btnEliminarTipo.setOnClickListener(
                v -> eliminarTipoSeleccionado(dialogBinding));
        dialogBinding.btnAgregarItem.setOnClickListener(v -> {
            itemsAdapter.agregarItem();
            dialogBinding.rvItemsCompra.scrollToPosition(itemsAdapter.getItemCount() - 1);
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Object seleccion = dialogBinding.spinnerTipoLugar.getSelectedItem();
            String tipo = seleccion instanceof String ? ((String) seleccion).trim() : "";
            if (tipo.isEmpty()) {
                Toast.makeText(this, R.string.dialog_crear_lugar_error_tipo,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Gasto opcional: vacío se persiste como NULL.
            Long gastoPromedio = null;
            String textoGasto = dialogBinding.inputGasto.getText() != null
                    ? dialogBinding.inputGasto.getText().toString().trim() : "";
            if (!textoGasto.isEmpty()) {
                try {
                    gastoPromedio = Long.parseLong(textoGasto);
                } catch (NumberFormatException e) {
                    dialogBinding.inputLayoutGasto.setError(
                            getString(R.string.dialog_crear_lugar_error_gasto));
                    return;
                }
                if (gastoPromedio < 0) {
                    dialogBinding.inputLayoutGasto.setError(
                            getString(R.string.dialog_crear_lugar_error_gasto));
                    return;
                }
            }
            dialogBinding.inputLayoutGasto.setError(null);

            // Lista de ítems: nombre obligatorio y cantidad ≥ 1.
            List<ItemCompra> items = itemsAdapter.obtenerItems();
            for (int i = 0; i < items.size(); i++) {
                ItemCompra item = items.get(i);
                String nombre = item.getNombre() == null ? "" : item.getNombre().trim();
                if (nombre.isEmpty() || item.getCantidad() < 1) {
                    dialogBinding.rvItemsCompra.scrollToPosition(i);
                    Toast.makeText(this, R.string.dialog_crear_lugar_error_item,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                item.setNombre(nombre);
            }

            LugarCompra lugar = new LugarCompra(
                    latitud, longitud, tipo, gastoPromedio, System.currentTimeMillis());

            v.setEnabled(false);
            mapaViewModel.insertarLugar(lugar, items).observe(this, guardado -> {
                if (Boolean.TRUE.equals(guardado)) {
                    Toast.makeText(this, R.string.dialog_crear_lugar_toast_creado,
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else if (Boolean.FALSE.equals(guardado)) {
                    v.setEnabled(true);
                    Toast.makeText(this, R.string.dialog_crear_lugar_toast_error,
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Repuebla el Spinner de tipos con los predefinidos y los personalizados.
     *
     * @param dialogBinding binding del diálogo de creación
     * @param seleccionado  tipo a seleccionar tras repoblar ({@code null} = primero)
     */
    private void poblarSpinnerTipos(@NonNull DialogCrearLugarCompraBinding dialogBinding,
                                    @Nullable String seleccionado) {
        List<String> tipos = new ArrayList<>();
        for (String tipo : getResources().getStringArray(R.array.tipos_lugar_predefinidos)) {
            tipos.add(tipo);
        }
        tipos.addAll(TiposLugarPreferencias.obtenerPersonalizados(this));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, tipos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spinnerTipoLugar.setAdapter(adapter);

        String tipoActual = seleccionado;
        if (tipoActual == null && !tipos.isEmpty()) {
            tipoActual = tipos.get(0);
        }
        if (tipoActual != null) {
            int posicion = tipos.indexOf(tipoActual);
            if (posicion >= 0) {
                dialogBinding.spinnerTipoLugar.setSelection(posicion);
            }
            dialogBinding.btnEliminarTipo.setEnabled(
                    TiposLugarPreferencias.esPersonalizado(this, tipoActual));
        } else {
            dialogBinding.btnEliminarTipo.setEnabled(false);
        }
    }

    /**
     * Abre el mini-diálogo para agregar un tipo personalizado y, si se guarda,
     * lo agrega al Spinner del diálogo de creación.
     *
     * @param dialogBinding binding del diálogo de creación
     */
    private void mostrarDialogAgregarTipo(@NonNull DialogCrearLugarCompraBinding dialogBinding) {
        DialogAgregarTipoLugarBinding tipoBinding =
                DialogAgregarTipoLugarBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_agregar_tipo_titulo)
                .setView(tipoBinding.getRoot())
                .setPositiveButton(R.string.dialog_agregar_tipo_guardar, null)
                .setNegativeButton(R.string.dialog_agregar_tipo_cancelar, null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String tipo = tipoBinding.inputNuevoTipo.getText() != null
                    ? tipoBinding.inputNuevoTipo.getText().toString().trim() : "";
            if (tipo.isEmpty()) {
                tipoBinding.inputLayoutNuevoTipo.setError(
                        getString(R.string.dialog_agregar_tipo_error_vacio));
                return;
            }
            if (existeTipoEnSpinner(dialogBinding, tipo)
                    || !TiposLugarPreferencias.agregarPersonalizado(this, tipo)) {
                tipoBinding.inputLayoutNuevoTipo.setError(
                        getString(R.string.dialog_agregar_tipo_error_duplicado));
                return;
            }
            poblarSpinnerTipos(dialogBinding, tipo);
            dialog.dismiss();
        });
    }

    /**
     * Elimina el tipo personalizado seleccionado en el Spinner. Los
     * predefinidos no se pueden eliminar.
     *
     * @param dialogBinding binding del diálogo de creación
     */
    private void eliminarTipoSeleccionado(@NonNull DialogCrearLugarCompraBinding dialogBinding) {
        Object seleccion = dialogBinding.spinnerTipoLugar.getSelectedItem();
        if (!(seleccion instanceof String)) {
            return;
        }
        String tipo = ((String) seleccion).trim();
        if (!TiposLugarPreferencias.esPersonalizado(this, tipo)) {
            Toast.makeText(this, R.string.dialog_crear_lugar_tipo_solo_personalizados,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        TiposLugarPreferencias.eliminarPersonalizado(this, tipo);
        poblarSpinnerTipos(dialogBinding, null);
        Toast.makeText(this, R.string.dialog_crear_lugar_tipo_eliminado,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Indica si un tipo ya existe en el Spinner (predefinido o personalizado).
     *
     * @param dialogBinding binding del diálogo de creación
     * @param tipo          nombre a buscar (sin distinguir mayúsculas)
     * @return {@code true} si ya está en la lista
     */
    private boolean existeTipoEnSpinner(@NonNull DialogCrearLugarCompraBinding dialogBinding,
                                        @NonNull String tipo) {
        for (int i = 0; i < dialogBinding.spinnerTipoLugar.getCount(); i++) {
            Object item = dialogBinding.spinnerTipoLugar.getItemAtPosition(i);
            if (item instanceof String && ((String) item).equalsIgnoreCase(tipo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prepara el {@link TransaccionViewModel} que alimenta el BottomSheet de
     * registro de movimientos: observa las cuentas activas y mantiene la lista
     * {@link #cuentasSpinner} sincronizada. Si el BottomSheet está abierto,
     * refresca su selector de cuentas.
     */
    private void configurarTransaccionViewModel() {
        transaccionViewModel = new ViewModelProvider(this).get(TransaccionViewModel.class);

        transaccionViewModel.getCuentasActivas().observe(this, cuentas -> {
            cuentasSpinner.clear();
            if (cuentas != null) {
                cuentasSpinner.addAll(cuentas);
            }
            if (sheetTransaccionBinding != null) {
                poblarSpinnerCuentas(sheetTransaccionBinding);
            }
        });
    }

    /**
     * Abre el BottomSheet para registrar un movimiento nuevo.
     * <p>
     * Infla {@code bottom_sheet_transaccion.xml}, puebla el selector de cuentas
     * activas y enlaza el botón "Guardar Movimiento". Al cerrarse, libera la
     * referencia al binding.
     * </p>
     */
    private void mostrarBottomSheetTransaccion() {
        BottomSheetTransaccionBinding sheetBinding =
                BottomSheetTransaccionBinding.inflate(getLayoutInflater());
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        sheetDialog.setContentView(sheetBinding.getRoot());

        sheetTransaccionBinding = sheetBinding;
        poblarSpinnerCuentas(sheetBinding);
        configurarConceptosPredefinidos(sheetBinding);

        // Ubicación: el panel arranca sin coordenada y el interruptor abre el mapa.
        latitudSeleccionada = null;
        longitudSeleccionada = null;
        sheetBinding.tvUbicacionFijada.setVisibility(View.GONE);
        sheetBinding.switchGeolocalizacion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                launcherUbicacion.launch(new Intent(this, MapaPickerActivity.class));
            } else {
                limpiarUbicacion(sheetBinding);
            }
        });

        sheetBinding.spinnerCuentas.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < cuentasSpinner.size()) {
                cuentaSeleccionadaId = cuentasSpinner.get(position).getId();
                sheetBinding.inputLayoutCuenta.setError(null);
            }
        });

        sheetBinding.btnGuardarMovimiento.setOnClickListener(v ->
                registrarMovimiento(sheetBinding, sheetDialog));

        sheetDialog.setOnDismissListener(dialog -> sheetTransaccionBinding = null);
        sheetDialog.show();
    }

    /**
     * Vuelca las cuentas activas actuales ({@link #cuentasSpinner}) en el
     * selector de cuentas del BottomSheet. Invalida la selección anterior porque
     * la lista pudo haber cambiado.
     *
     * @param sheetBinding Binding del BottomSheet abierto
     */
    private void poblarSpinnerCuentas(@NonNull BottomSheetTransaccionBinding sheetBinding) {
        List<String> nombres = new ArrayList<>();
        for (Cuenta cuenta : cuentasSpinner) {
            nombres.add(cuenta.getNombre());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, nombres);
        sheetBinding.spinnerCuentas.setAdapter(adapter);
        // La lista cambió: la selección anterior ya no es válida.
        cuentaSeleccionadaId = -1L;
        sheetBinding.spinnerCuentas.setText("", false);
    }

    /**
     * Rellena el campo de concepto del BottomSheet con las sugerencias
     * predefinidas de {@code R.array.aCuentas_conceptos_predefinidos}. El campo
     * sigue siendo editable: el usuario puede elegir una sugerencia o escribir
     * un concepto propio. Se muestra la lista completa al enfocar el campo
     * ({@code threshold(0)}), sin exigir texto previo.
     *
     * @param sheetBinding Binding del BottomSheet abierto
     */
    private void configurarConceptosPredefinidos(@NonNull BottomSheetTransaccionBinding sheetBinding) {
        String[] conceptos = getResources().getStringArray(R.array.aCuentas_conceptos_predefinidos);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, conceptos);
        sheetBinding.inputConcepto.setAdapter(adapter);
        sheetBinding.inputConcepto.setThreshold(0);
    }

    /**
     * Valida los campos del formulario del BottomSheet (monto &gt; 0, concepto no
     * vacío y cuenta seleccionada). Si la validación de UI pasa, delega el
     * registro en el ViewModel y reacciona al {@link ErrorCode} resultante. Si
     * hay errores de UI, <b>no</b> se llama al ViewModel.
     *
     * @param sheetBinding Binding del BottomSheet que contiene el formulario
     * @param sheetDialog  Diálogo del BottomSheet (se cierra al registrar con éxito)
     */
    private void registrarMovimiento(@NonNull BottomSheetTransaccionBinding sheetBinding,
                                     @NonNull BottomSheetDialog sheetDialog) {
        // Limpiar errores previos
        sheetBinding.inputLayoutMonto.setError(null);
        sheetBinding.inputLayoutConcepto.setError(null);
        sheetBinding.inputLayoutCuenta.setError(null);

        // Monto: no vacío y > 0
        CharSequence montoTexto = sheetBinding.inputMonto.getText();
        String montoStr = montoTexto != null ? montoTexto.toString().trim() : "";
        long monto = 0L;
        if (!montoStr.isEmpty()) {
            try {
                monto = Long.parseLong(montoStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Monto no numérico en el formulario: " + montoStr, e);
                monto = 0L;
            }
        }
        if (monto <= 0) {
            sheetBinding.inputLayoutMonto.setError(
                    getString(R.string.aCuentas_error_monto_vacio));
            return;
        }

        // Concepto: no vacío
        CharSequence conceptoTexto = sheetBinding.inputConcepto.getText();
        String concepto = conceptoTexto != null ? conceptoTexto.toString().trim() : "";
        if (concepto.isEmpty()) {
            sheetBinding.inputLayoutConcepto.setError(
                    getString(R.string.aCuentas_error_concepto_vacio));
            return;
        }

        // Cuenta: seleccionada en el spinner
        if (cuentaSeleccionadaId <= 0) {
            sheetBinding.inputLayoutCuenta.setError(
                    getString(R.string.aCuentas_error_cuenta_no_seleccionada));
            return;
        }

        String tipo = (sheetBinding.toggleTipoTransaccion.getCheckedButtonId() == R.id.btnEgreso)
                ? TipoTransaccion.EGRESO.name()
                : TipoTransaccion.INGRESO.name();

        final long montoFinal = monto;
        LiveData<ErrorCode> resultado = transaccionViewModel.registrarTransaccion(
                cuentaSeleccionadaId, tipo, montoFinal, concepto,
                latitudSeleccionada, longitudSeleccionada, null);

        resultado.observe(this, new Observer<ErrorCode>() {
            @Override
            public void onChanged(ErrorCode errorCode) {
                resultado.removeObserver(this);
                manejarResultadoTransaccion(errorCode, sheetBinding, sheetDialog);
            }
        });
    }

    /**
     * Traduce el resultado del registro a feedback de usuario:
     * {@code null} = éxito (Toast + limpieza del formulario + cierre del
     * BottomSheet); un {@link ErrorCode} = fallo de dominio (Toast con el mensaje
     * correspondiente desde {@code strings.xml}, sin cerrar el BottomSheet para
     * permitir corregir).
     *
     * @param errorCode    código devuelto por el ViewModel ({@code null} si tuvo éxito)
     * @param sheetBinding Binding del BottomSheet abierto
     * @param sheetDialog  Diálogo del BottomSheet
     */
    private void manejarResultadoTransaccion(ErrorCode errorCode,
                                             @NonNull BottomSheetTransaccionBinding sheetBinding,
                                             @NonNull BottomSheetDialog sheetDialog) {
        if (errorCode == null) {
            Toast.makeText(this, R.string.aCuentas_toast_movimiento_registrado,
                    Toast.LENGTH_SHORT).show();
            limpiarFormularioTransaccion(sheetBinding);
            sheetDialog.dismiss();
            return;
        }

        int mensaje;
        switch (errorCode) {
            case SALDO_INSUFICIENTE:
                mensaje = R.string.aCuentas_toast_error_saldo_insuficiente;
                break;
            case CUENTA_NO_ENCONTRADA:
                mensaje = R.string.aCuentas_toast_error_cuenta_no_encontrada;
                break;
            case MONTO_INVALIDO:
                mensaje = R.string.aCuentas_toast_error_monto_invalido;
                break;
            case UBICACION_INVALIDA:
                mensaje = R.string.aCuentas_toast_error_ubicacion_invalida;
                break;
            default:
                mensaje = R.string.aCuentas_toast_movimiento_error;
                break;
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    /**
     * Limpia el formulario del BottomSheet tras un registro exitoso: monto,
     * concepto, errores de validación y selección del spinner.
     *
     * @param sheetBinding Binding del BottomSheet a limpiar
     */
    private void limpiarFormularioTransaccion(@NonNull BottomSheetTransaccionBinding sheetBinding) {
        sheetBinding.inputMonto.setText(null);
        sheetBinding.inputConcepto.setText(null);
        sheetBinding.inputLayoutMonto.setError(null);
        sheetBinding.inputLayoutConcepto.setError(null);
        sheetBinding.inputLayoutCuenta.setError(null);
        sheetBinding.spinnerCuentas.setText("", false);
        cuentaSeleccionadaId = -1L;
        // Al desactivar el interruptor, su listener limpia la ubicación elegida.
        sheetBinding.switchGeolocalizacion.setChecked(false);
    }

    /**
     * Registra el lanzador al selector de ubicación ({@link MapaPickerActivity}).
     * Debe registrarse antes de que la Activity llegue a {@code STARTED}.
     * <p>
     * Si el selector devuelve una coordenada, se guarda y se muestra la
     * confirmación en el panel. Si el usuario cancela, se revierte el interruptor
     * (su listener limpia el estado) sin reabrir el mapa.
     * </p>
     */
    private void registrarLauncherUbicacion() {
        launcherUbicacion = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent datos = result.getData();
                    if (result.getResultCode() == RESULT_OK && datos != null
                            && datos.hasExtra(MapaPickerActivity.EXTRA_LATITUD)
                            && datos.hasExtra(MapaPickerActivity.EXTRA_LONGITUD)) {
                        establecerUbicacion(
                                datos.getDoubleExtra(MapaPickerActivity.EXTRA_LATITUD, 0d),
                                datos.getDoubleExtra(MapaPickerActivity.EXTRA_LONGITUD, 0d));
                    } else if (sheetTransaccionBinding != null) {
                        sheetTransaccionBinding.switchGeolocalizacion.setChecked(false);
                    }
                });
    }

    /**
     * Guarda la coordenada elegida en el mapa y la muestra como confirmación.
     *
     * @param latitud  latitud WGS84 elegida
     * @param longitud longitud WGS84 elegida
     */
    private void establecerUbicacion(double latitud, double longitud) {
        latitudSeleccionada = latitud;
        longitudSeleccionada = longitud;
        if (sheetTransaccionBinding != null) {
            sheetTransaccionBinding.tvUbicacionFijada.setText(
                    getString(R.string.aCuentas_transaccion_ubicacion_fijada, latitud, longitud));
            sheetTransaccionBinding.tvUbicacionFijada.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Descarta la ubicación elegida y oculta su confirmación.
     *
     * @param sheetBinding Binding del BottomSheet abierto
     */
    private void limpiarUbicacion(@NonNull BottomSheetTransaccionBinding sheetBinding) {
        latitudSeleccionada = null;
        longitudSeleccionada = null;
        sheetBinding.tvUbicacionFijada.setText(null);
        sheetBinding.tvUbicacionFijada.setVisibility(View.GONE);
    }

    /**
     * Abre un diálogo para registrar una cuenta nueva.
     * <p>
     * Valida que el nombre no esté vacío antes de persistir; si la validación falla,
     * el diálogo permanece abierto (comportamiento estándar para permitir corregir).
     * El saldo se lee de forma tolerante a fallos: vacío o no numérico equivale a 0.
     * La inserción la ejecuta {@link CuentasViewModel#insertarCuenta} en un hilo
     * secundario; al completarse, el {@code LiveData} actualiza la UI sola.
     * </p>
     */
    private void mostrarDialogNuevaCuenta() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nueva_cuenta, null);

        TextInputLayout inputLayoutNombre = dialogView.findViewById(R.id.inputLayoutNombre);
        TextInputEditText inputNombre = dialogView.findViewById(R.id.inputNombre);
        MaterialButtonToggleGroup toggleTipo = dialogView.findViewById(R.id.toggleTipo);
        TextInputEditText inputSaldo = dialogView.findViewById(R.id.inputSaldo);

        // Se pasa el listener del botón positivo como null y se sobreescribe tras
        // show(): así el diálogo NO se cierra solo si la validación falla.
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.aCuentas_dialog_titulo)
                .setView(dialogView)
                .setPositiveButton(R.string.aCuentas_dialog_guardar, null)
                .setNegativeButton(R.string.aCuentas_dialog_cancelar, null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = inputNombre.getText() != null
                    ? inputNombre.getText().toString().trim() : "";

            if (nombre.isEmpty()) {
                inputLayoutNombre.setError(getString(R.string.aCuentas_error_nombre_vacio));
                return;
            }
            inputLayoutNombre.setError(null);

            String tipo = (toggleTipo.getCheckedButtonId() == R.id.btnTarjeta)
                    ? TipoCuenta.TARJETA.name() : TipoCuenta.AHORRO.name();

            long saldo = 0L;
            CharSequence saldoTexto = inputSaldo.getText();
            if (saldoTexto != null && !saldoTexto.toString().trim().isEmpty()) {
                try {
                    saldo = Long.parseLong(saldoTexto.toString().trim());
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Saldo inicial no numérico, se usa 0: " + saldoTexto, e);
                }
            }

            viewModel.insertarCuenta(nombre, tipo, saldo);
            Toast.makeText(this, R.string.aCuentas_toast_cuenta_creada,
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    /**
     * Callback del long-press en una fila del RecyclerView
     * ({@link CuentasAdapter.OnCuentaLongClickListener}): abre el menú de
     * gestión de la cuenta presionada.
     *
     * @param cuenta Cuenta de la fila presionada
     */
    @Override
    public void onCuentaLongClick(@NonNull Cuenta cuenta) {
        mostrarOpcionesCuenta(cuenta);
    }

    /**
     * Menú de acciones de una cuenta (editar nombre, editar saldo, eliminar).
     * El título es el nombre de la cuenta (dato, no texto fijo).
     *
     * @param cuenta Cuenta sobre la que se actúa
     */
    private void mostrarOpcionesCuenta(@NonNull Cuenta cuenta) {
        CharSequence[] opciones = {
                getString(R.string.aCuentas_menu_editar_nombre),
                getString(R.string.aCuentas_menu_editar_saldo),
                getString(R.string.aCuentas_menu_eliminar)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(cuenta.getNombre())
                .setItems(opciones, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            mostrarDialogEditarNombre(cuenta);
                            break;
                        case 1:
                            mostrarDialogEditarSaldo(cuenta);
                            break;
                        case 2:
                            confirmarEliminarCuenta(cuenta);
                            break;
                        default:
                            break;
                    }
                })
                .show();
    }

    /**
     * Diálogo para editar el nombre de una cuenta. El campo se precarga con el
     * nombre actual y solo se persiste si no queda vacío (el diálogo permanece
     * abierto ante error de validación).
     *
     * @param cuenta Cuenta a editar
     */
    private void mostrarDialogEditarNombre(@NonNull Cuenta cuenta) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_editar_nombre_cuenta, null);
        TextInputLayout inputLayoutNombre = dialogView.findViewById(R.id.inputLayoutEditarNombre);
        TextInputEditText inputNombre = dialogView.findViewById(R.id.inputEditarNombre);

        inputNombre.setText(cuenta.getNombre());
        if (inputNombre.getText() != null) {
            inputNombre.setSelection(inputNombre.getText().length());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.aCuentas_dialog_editar_nombre_titulo)
                .setView(dialogView)
                .setPositiveButton(R.string.aCuentas_guardar, null)
                .setNegativeButton(R.string.aCuentas_cancelar, null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nuevoNombre = inputNombre.getText() != null
                    ? inputNombre.getText().toString().trim() : "";
            if (nuevoNombre.isEmpty()) {
                inputLayoutNombre.setError(getString(R.string.aCuentas_error_nombre_vacio));
                return;
            }
            inputLayoutNombre.setError(null);
            viewModel.editarNombreCuenta(cuenta.getId(), nuevoNombre);
            dialog.dismiss();
        });
    }

    /**
     * Diálogo para editar el saldo de una cuenta. El campo se precarga con el
     * saldo actual (valor numérico en pesos CLP). Observa el {@code LiveData}
     * del ViewModel: {@code true} → Toast de éxito; {@code false} → mensaje de
     * error (cuenta con transacciones registradas).
     *
     * @param cuenta Cuenta a editar
     */
    private void mostrarDialogEditarSaldo(@NonNull Cuenta cuenta) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_editar_saldo_cuenta, null);
        TextInputLayout inputLayoutSaldo = dialogView.findViewById(R.id.inputLayoutEditarSaldo);
        TextInputEditText inputSaldo = dialogView.findViewById(R.id.inputEditarSaldo);

        inputSaldo.setText(String.valueOf(cuenta.getSaldo()));
        if (inputSaldo.getText() != null) {
            inputSaldo.setSelection(inputSaldo.getText().length());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.aCuentas_dialog_editar_saldo_titulo)
                .setView(dialogView)
                .setPositiveButton(R.string.aCuentas_guardar, null)
                .setNegativeButton(R.string.aCuentas_cancelar, null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long nuevoSaldo;
            try {
                CharSequence texto = inputSaldo.getText();
                nuevoSaldo = Long.parseLong(texto != null ? texto.toString().trim() : "");
            } catch (NumberFormatException e) {
                inputLayoutSaldo.setError(getString(R.string.aCuentas_error_saldo_invalido));
                return;
            }
            inputLayoutSaldo.setError(null);

            LiveData<Boolean> resultado = viewModel.editarSaldoCuenta(cuenta.getId(), nuevoSaldo);
            resultado.observe(this, new Observer<Boolean>() {
                @Override
                public void onChanged(Boolean actualizado) {
                    resultado.removeObserver(this);
                    int mensaje = Boolean.TRUE.equals(actualizado)
                            ? R.string.aCuentas_toast_saldo_actualizado
                            : R.string.aCuentas_error_saldo_con_transacciones;
                    Toast.makeText(CuentasActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                }
            });
            dialog.dismiss();
        });
    }

    /**
     * Diálogo de confirmación para eliminar (soft delete) una cuenta.
     * No se borra la fila: {@code CuentasViewModel.desactivarCuenta} marca
     * {@code activa = false} y la cuenta desaparece de la lista.
     *
     * @param cuenta Cuenta a eliminar
     */
    private void confirmarEliminarCuenta(@NonNull Cuenta cuenta) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.aCuentas_dialog_eliminar_titulo)
                .setMessage(getString(R.string.aCuentas_dialog_eliminar_mensaje, cuenta.getNombre()))
                .setPositiveButton(R.string.aCuentas_eliminar,
                        (dialog, which) -> viewModel.desactivarCuenta(cuenta.getId()))
                .setNegativeButton(R.string.aCuentas_cancelar, null)
                .show();
    }

    /**
     * Actualiza el título del Toolbar/ActionBar según la vista activa del ViewFlipper.
     *
     * @param viewIndex índice de la vista mostrada
     *                  0=Cuentas, 1=Movimientos/Historial, 2=Mapa, 3=Contactos
     */
    private void actualizarTituloToolbar(int viewIndex) {
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            int tituloRes;
            switch (viewIndex) {
                case 1:
                    tituloRes = R.string.aCuentas_toolbar_movimientos;
                    break;
                case 2:
                    tituloRes = R.string.aCuentas_toolbar_mapa;
                    break;
                case 3:
                    tituloRes = R.string.aCuentas_toolbar_contactos;
                    break;
                case 0:
                default:
                    tituloRes = R.string.aCuentas_toolbar_cuentas;
                    break;
            }
            ab.setTitle(tituloRes);
        }
    }
}
