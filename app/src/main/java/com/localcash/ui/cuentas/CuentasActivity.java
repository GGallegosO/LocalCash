package com.localcash.ui.cuentas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.localcash.R;
import com.localcash.data.db.AppDatabase;
import com.localcash.data.model.DeudaContacto;
import com.localcash.data.entities.Cuenta;
import com.localcash.databinding.ActivityCuentasBinding;
import com.localcash.databinding.BottomSheetLugaresBinding;
import com.localcash.databinding.BottomSheetTransaccionBinding;
import com.localcash.databinding.DialogContactoBinding;
import com.localcash.databinding.ItemDeudaContactoBinding;
import com.localcash.databinding.ItemParteGastoBinding;
import com.localcash.databinding.ViewContactosBinding;
import com.localcash.databinding.ViewCuentasBinding;
import com.localcash.databinding.ViewHistorialBinding;
import com.localcash.databinding.ViewMapaBinding;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.Lugar;
import com.localcash.domain.model.TipoCuenta;
import com.localcash.domain.model.TipoTransaccion;
import com.localcash.domain.model.Transaccion;
import com.localcash.domain.model.Ubicacion;
import com.localcash.domain.model.Contacto;
import com.localcash.domain.model.EstadoReparto;
import com.localcash.domain.model.ModoDivision;
import com.localcash.domain.model.PartePropuesta;
import com.localcash.ui.BaseActivity;
import com.localcash.ui.contactos.ContactoAdapter;
import com.localcash.ui.contactos.ContactosViewModel;
import com.localcash.ui.mapa.LugarDetalleSheet;
import com.localcash.ui.mapa.LugarFormSheet;
import com.localcash.ui.mapa.LugaresAdapter;
import com.localcash.ui.mapa.MapaConfig;
import com.localcash.ui.mapa.MapaController;
import com.localcash.ui.mapa.MapaViewModel;
import com.localcash.ui.mapa.SeleccionarUbicacionActivity;
import com.localcash.ui.theme.ThemeManager;
import com.localcash.ui.util.CurrencyFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.LongConsumer;

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

    private ActivityCuentasBinding binding;

    /** Binding de la Vista 1 (se infla dentro del ViewFlipper, fuera de activity_cuentas.xml). */
    private ViewCuentasBinding vistaCuentas;

    /** Binding de la Vista 1 (Historial/Movimientos), inflado dentro del ViewFlipper. */
    private ViewHistorialBinding vistaHistorial;

    /** Binding de la Vista 3 (Contactos), inflado dentro del ViewFlipper. */
    private ViewContactosBinding vistaContactos;

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

    /** ViewModel de la Vista 4 (Contactos): observa Room y expone el dominio. */
    private ContactosViewModel contactosViewModel;

    /** Cuentas activas asociadas, en el mismo orden que el adapter del spinner. */
    private final List<Cuenta> cuentasSpinner = new ArrayList<>();

    /** Id de la cuenta seleccionada en el spinner ({@code -1} si no hay selección). */
    private long cuentaSeleccionadaId = -1L;

    /** Latitud confirmada para el movimiento en curso ({@code null} = sin ubicación). */
    private Double latitudMovimiento;

    /** Longitud confirmada para el movimiento en curso ({@code null} = sin ubicación). */
    private Double longitudMovimiento;

    /**
     * Evita que el cambio programático del interruptor "Geolocalizar" vuelva a
     * lanzar la selección de ubicación al recibir el resultado.
     */
    private boolean actualizandoSwitchUbicacion;

    /**
     * Contacto en curso para el reparto: checkbox "Compartido" deshabilitado en
     * los ingresos (solo un egreso se divide), y guardado al cambiar el tipo
     * programáticamente (Feature 012, T11).
     */
    private boolean actualizandoCheckCompartido;

    /** Contactos de la agenda disponibles para compartir un egreso (Feature 012). */
    private final List<Contacto> contactosAgenda = new ArrayList<>();

    /**
     * Firma de las filas de partes ya infladas (modo + ids seleccionados).
     * Evita reconstruir los EditText en cada tecla, lo que robaría el foco.
     */
    private String firmaFilasParte = "";

    /** Observador del bloque de división, activo solo con la hoja abierta. */
    @Nullable
    private androidx.lifecycle.Observer<EstadoReparto> observadorEstadoReparto;

    /** Lanzador de la Activity de selección de ubicación (T24). */
    private ActivityResultLauncher<Intent> lanzadorUbicacion;

    /** Tiempo máximo de espera para una fijación de ubicación (ms). */
    private static final long TIEMPO_MAXIMO_UBICACION_MS = 15_000L;

    /** Binding de la Vista 3 (Mapa), inflado dentro del ViewFlipper. */
    private ViewMapaBinding vistaMapa;

    /** Controlador del mapa: cámara, pines y ciclo de vida. */
    private MapaController mapaController;

    /** ViewModel del mapa: lugares observables desde Room. */
    private MapaViewModel mapaViewModel;

    /** Adapter de "Mis lugares". */
    private LugaresAdapter lugaresAdapter;

    /** Binding del BottomSheet de lugares mientras está abierto. */
    private BottomSheetLugaresBinding sheetLugaresBinding;

    /** Lugares activos actuales, para refrescar el BottomSheet abierto. */
    private final List<Lugar> lugaresActivos = new ArrayList<>();

    /** Señal para cancelar la petición de ubicación en curso. */
    private CancellationSignal cancelacionUbicacion;

    /**
     * Pin pendiente de reubicación: la próxima pulsación larga sobre el mapa
     * mueve este pin en vez de crear uno nuevo (T20).
     */
    private Lugar lugarEnMovimiento;

    /**
     * Solicita el permiso de ubicación solo cuando el usuario pulsa
     * "Mi ubicación". Si lo concede, centra el mapa; si lo niega, avisa sin
     * bloquear la app (CA-13/CA-14).
     */
    private final ActivityResultLauncher<String> permisoUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (Boolean.TRUE.equals(concedido)) {
                    centrarEnUbicacionActual();
                } else {
                    mostrarAviso(R.string.aMapa_error_ubicacion_denegada);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        // MapLibre debe inicializarse antes de inflar cualquier MapView. La
        // pestaña Mapa y la selección de ubicación comparten esta inicialización.
        MapaConfig.inicializar(this);
        registrarLanzadorUbicacion();

        // Inflar los 4 layouts internos dentro del ViewFlipper
        // Vista 0: Cuentas (binding propio: sus vistas no forman parte de activity_cuentas.xml)
        vistaCuentas = ViewCuentasBinding.inflate(getLayoutInflater(), binding.viewFlipper, true);
        // Vista 1: Historial/Movimientos
        vistaHistorial = ViewHistorialBinding.inflate(
                getLayoutInflater(), binding.viewFlipper, true);
        // Vista 2: Mapa (binding propio para conectar MapLibre y sus controles)
        vistaMapa = ViewMapaBinding.inflate(getLayoutInflater(), binding.viewFlipper, true);
        // Vista 3: Contactos (Feature 011)
        vistaContactos = ViewContactosBinding.inflate(
                getLayoutInflater(), binding.viewFlipper, true);

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

        // Conectar la Vista 3 (Mapa) con MapLibre y Room
        configurarVistaMapa(savedInstanceState);

        // Conectar la Vista 4 (Contactos) con Room (ViewModel + Adapter)
        configurarVistaContactos();

        // Preparar el ViewModel de transacciones (alimenta el BottomSheet del FAB)
        configurarTransaccionViewModel();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Guardar el índice actual del ViewFlipper para preservar la vista seleccionada
        outState.putInt("viewFlipperIndex", binding.viewFlipper.getDisplayedChild());
        // Guardar cámara y estado del mapa para rotación/cambio de tema
        if (mapaController != null) {
            mapaController.guardarEstado(outState);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapaController != null) {
            mapaController.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapaController != null) {
            mapaController.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapaController != null) {
            mapaController.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mapaController != null) {
            mapaController.onStop();
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapaController != null) {
            mapaController.onLowMemory();
        }
    }

    @Override
    protected void onDestroy() {
        if (cancelacionUbicacion != null) {
            cancelacionUbicacion.cancel();
            cancelacionUbicacion = null;
        }
        if (mapaController != null) {
            mapaController.destruir();
            mapaController = null;
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
     * Conecta la Vista 3 (Mapa) con MapLibre y Room.
     * <p>
     * Crea el {@link MapaController} (que reenvía el ciclo de vida al
     * {@code MapView}), observa los lugares de la base de datos y cablea los
     * controles "Mi ubicación" y "Mis lugares". El mapa vive dentro del
     * {@code ViewFlipper}: no se crea una Activity adicional (plan §2.2).
     * </p>
     *
     * @param savedInstanceState estado de la Activity (para restaurar la cámara)
     */
    private void configurarVistaMapa(Bundle savedInstanceState) {
        vistaMapa.tvAtribucion.setText(MapaConfig.ATRIBUCION);

        mapaController = new MapaController(vistaMapa.mapView);
        mapaController.crear(savedInstanceState);
        mapaController.setOnLugarSeleccionadoListener(this::mostrarDetalleLugar);
        mapaController.setOnPulsacionLargaListener(this::alPulsacionLarga);

        vistaMapa.fabMiUbicacion.setOnClickListener(v -> solicitarUbicacionActual());
        vistaMapa.fabMisLugares.setOnClickListener(v -> mostrarListaLugares());

        lugaresAdapter = new LugaresAdapter();

        mapaViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new MapaViewModel(AppDatabase.getInstance(getApplicationContext()));
            }
        }).get(MapaViewModel.class);

        // Todos los lugares (abiertos y cerrados) se dibujan en el mapa.
        mapaViewModel.getLugares().observe(this, lugares -> {
            mapaController.setLugares(lugares);
            boolean vacio = lugares == null || lugares.isEmpty();
            vistaMapa.cardEmptyLugares.setVisibility(vacio ? View.VISIBLE : View.GONE);
        });

        // Solo los lugares abiertos aparecen en "Mis lugares".
        mapaViewModel.getLugaresAbiertos().observe(this, abiertos -> {
            lugaresActivos.clear();
            if (abiertos != null) {
                lugaresActivos.addAll(abiertos);
            }
            lugaresAdapter.setLugares(lugaresActivos);
            refrescarEstadoListaLugares();
        });
    }

    /**
     * Conecta la Vista 4 (Contactos) con Room (ViewModel + Adapter).
     * <p>
     * Observa la lista de contactos desde {@link ContactosViewModel} y pinta el
     * {@code RecyclerView} de {@code view_contactos.xml}; controla el estado
     * vacío y el FAB de alta (Feature 011).
     * </p>
     */
    private void configurarVistaContactos() {
        contactosViewModel = new ViewModelProvider(this,
                new ViewModelProvider.Factory() {
                    @NonNull
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                        return (T) new ContactosViewModel(
                                AppDatabase.getInstance(getApplicationContext()));
                    }
                }).get(ContactosViewModel.class);

        ContactoAdapter adapter = new ContactoAdapter();
        adapter.setOnContactoClickListener(contacto -> mostrarDialogoDetalleContacto(contacto));
        vistaContactos.recyclerContactos.setLayoutManager(new LinearLayoutManager(this));
        vistaContactos.recyclerContactos.setAdapter(adapter);

        // La lista y el estado vacío reaccionan a la base de datos (LiveData).
        contactosViewModel.getContactos().observe(this, entidades -> {
            List<Contacto> dominio = ContactosViewModel.aDominio(entidades);
            adapter.setContactos(dominio);
            contactosAgenda.clear();
            if (dominio != null) {
                contactosAgenda.addAll(dominio);
            }
            boolean hayContactos = dominio != null && !dominio.isEmpty();
            vistaContactos.recyclerContactos.setVisibility(
                    hayContactos ? View.VISIBLE : View.GONE);
            vistaContactos.layoutEmpty.setVisibility(hayContactos ? View.GONE : View.VISIBLE);
        });

        // Deuda pendiente por contacto: pinta "Debe: $X" o "Al día" (T13).
        contactosViewModel.getResumenDeudas().observe(this,
                resumen -> adapter.setResumenDeudas(resumen));

        // FAB: abrir el diálogo de alta de contacto.
        vistaContactos.fabAgregar.setOnClickListener(v -> mostrarDialogoContacto(null));

        // Errores de negocio (contacto en uso, duplicados, nombre vacío).
        contactosViewModel.getResultado().observe(this, error -> {
            if (error != null) {
                mostrarErrorContacto(error);
            }
        });
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

        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setView(dbinding.getRoot())
                .setPositiveButton(R.string.aContacto_btn_guardar,
                        (d, w) -> {
                            String nombre = textoDe(dbinding.etNombre);
                            String telefono = textoDe(dbinding.etTelefono);
                            String email = textoDe(dbinding.etEmail);
                            String notas = textoDe(dbinding.etNotas);

                            Contacto nuevo = new Contacto(
                                    contactoExistente != null ? contactoExistente.getId() : 0L,
                                    nombre, telefono, email, notas);
                            if (contactoExistente == null) {
                                contactosViewModel.agregarContacto(nuevo);
                            } else {
                                contactosViewModel.actualizarContacto(nuevo);
                            }
                        })
                .setNegativeButton(R.string.aContacto_btn_cancelar, null)
                .show();
    }

    /** Muestra el diálogo de detalle de un contacto (editar/eliminar). */
    private void mostrarDialogoDetalleContacto(Contacto contacto) {
        LinearLayout contenedor = new LinearLayout(this);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        contenedor.setPadding(pad, pad / 2, pad, 0);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(contacto.getNombre())
                .setView(contenedor)
                .setPositiveButton(R.string.aContacto_btn_editar, (d, w) ->
                        mostrarDialogoContacto(contacto))
                .setNegativeButton(R.string.aContacto_btn_eliminar, (d, w) ->
                        confirmarEliminacionContacto(contacto))
                .setNeutralButton(android.R.string.cancel, null)
                .show();

        final Observer<List<DeudaContacto>> observador = deudas ->
                pintarDeudasDetalle(contenedor, deudas);
        contactosViewModel.getDeudasPendientes(contacto.getId()).observe(this, observador);
        final Observer<ErrorCode> observadorPago = error -> {
            if (error != null) {
                Toast.makeText(this, mensajeReparto(error), Toast.LENGTH_SHORT).show();
            }
        };
        contactosViewModel.getResultadoPago().observe(this, observadorPago);
        dialogo.setOnDismissListener(d -> {
            contactosViewModel.getDeudasPendientes(contacto.getId())
                    .removeObserver(observador);
            contactosViewModel.getResultadoPago().removeObserver(observadorPago);
        });
    }

    /**
     * Pinta las deudas pendientes del contacto en el diálogo de detalle (T13).
     *
     * @param contenedor contenedor vertical del diálogo
     * @param deudas     filas {@code (concepto, fecha, monto)}
     */
    private void pintarDeudasDetalle(@NonNull LinearLayout contenedor,
                                     @Nullable List<DeudaContacto> deudas) {
        contenedor.removeAllViews();
        if (deudas == null || deudas.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText(R.string.aGastoCompartido_sin_deudas);
            vacio.setTextColor(MaterialColors.getColor(
                    vacio, com.google.android.material.R.attr.colorOnSurfaceVariant));
            contenedor.addView(vacio);
            return;
        }

        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        long total = 0L;
        List<Long> pendientes = new ArrayList<>();
        for (DeudaContacto deuda : deudas) {
            ItemDeudaContactoBinding fila = ItemDeudaContactoBinding.inflate(
                    LayoutInflater.from(contenedor.getContext()), contenedor, false);
            String concepto = deuda.getDescripcion() == null || deuda.getDescripcion().isEmpty()
                    ? getString(R.string.aGastoCompartido_detalle_titulo)
                    : deuda.getDescripcion();
            fila.tvConcepto.setText(concepto);
            fila.tvFecha.setText(formato.format(new Date(deuda.getFechaMillis())));
            fila.tvMonto.setText(CurrencyFormatter.format(deuda.getMontoAsignado()));
            fila.btnPagar.setOnClickListener(v ->
                    contactosViewModel.pagarParte(deuda.getParteId()));
            contenedor.addView(fila.getRoot());
            total += deuda.getMontoAsignado();
            pendientes.add(Long.valueOf(deuda.getParteId()));
        }

        // Footer "Pagar todo": oculto cuando ya no hay deudas.
        com.google.android.material.button.MaterialButton pagarTodo =
                new com.google.android.material.button.MaterialButton(this);
        pagarTodo.setText(getString(R.string.aGastoCompartido_pagar_todo,
                CurrencyFormatter.format(total)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        pagarTodo.setLayoutParams(params);
        final List<Long> ids = new ArrayList<>(pendientes);
        pagarTodo.setOnClickListener(v -> contactosViewModel.pagarTodas(ids));
        contenedor.addView(pagarTodo);
    }

    /** Pide confirmación antes de eliminar un contacto. */
    private void confirmarEliminacionContacto(Contacto contacto) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.aContacto_btn_eliminar)
                .setMessage("¿Eliminar el contacto \"" + contacto.getNombre() + "\"?")
                .setPositiveButton(R.string.aContacto_btn_eliminar, (d, w) ->
                        contactosViewModel.eliminarContacto(contacto.getId()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Muestra un toast con el error de negocio de contactos. */
    private void mostrarErrorContacto(ErrorCode error) {
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

    /** Devuelve el texto de un EditText, o {@code null} si está vacío. */
    private String textoDe(com.google.android.material.textfield.TextInputEditText editText) {
        String texto = editText.getText() != null ? editText.getText().toString().trim() : null;
        return texto == null || texto.isEmpty() ? null : texto;
    }

    /**
     * Abre el BottomSheet con la lista de lugares activos. Al tocar uno, el
     * mapa se centra en su pin y muestra la ventana de información (CA-15).
     * <p>
     * Seleccionar desde la lista es solo navegación: no abre el editor del pin
     * ni cambia su estado, por lo que el lugar sigue apareciendo en "Mis
     * lugares". El editor se abre manteniendo pulsado el mapa (alta de pin) o
     * tocando un marcador en el mapa.
     * </p>
     */
    private void mostrarListaLugares() {
        BottomSheetLugaresBinding sheet = BottomSheetLugaresBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheet.getRoot());

        sheetLugaresBinding = sheet;
        sheet.recyclerLugares.setLayoutManager(new LinearLayoutManager(this));
        sheet.recyclerLugares.setAdapter(lugaresAdapter);

        lugaresAdapter.setOnLugarClickListener(lugar -> {
            dialog.dismiss();
            binding.viewFlipper.setDisplayedChild(2);
            binding.bottomNav.setSelectedItemId(R.id.nav_mapa);
            // Solo centra la cámara y resalta el pin: abrir el detalle desde
            // aquí permitía cerrar/eliminar el lugar y lo sacaba de la lista.
            mapaController.mostrarLugar(lugar);
        });

        refrescarEstadoListaLugares();
        dialog.setOnDismissListener(d -> sheetLugaresBinding = null);
        dialog.show();
    }

    /**
     * Sincroniza el estado vacío del BottomSheet con la lista actual. Se llama
     * al abrir el panel y cada vez que Room emite un cambio (un lugar cerrado
     * desaparece de la lista sin recargar la pantalla).
     */
    private void refrescarEstadoListaLugares() {
        if (sheetLugaresBinding == null) {
            return;
        }
        boolean vacio = lugaresActivos.isEmpty();
        sheetLugaresBinding.recyclerLugares.setVisibility(vacio ? View.GONE : View.VISIBLE);
        sheetLugaresBinding.tvEmptyLugares.setVisibility(vacio ? View.VISIBLE : View.GONE);
    }

    /**
     * Abre el detalle completo del pin (T19). Se llama al tocar un marcador del
     * mapa o una fila de "Mis lugares".
     *
     * @param lugar pin seleccionado
     */
    private void mostrarDetalleLugar(@NonNull Lugar lugar) {
        new LugarDetalleSheet(this, mapaViewModel, lugar, this::solicitarMoverLugar).mostrar();
    }

    /**
     * Gestiona la pulsación larga sobre el mapa (T18/T20).
     * <p>
     * Si hay un pin pendiente de reubicación, lo mueve; en caso contrario abre
     * el formulario de alta con la coordenada presionada. El pin solo se
     * inserta al confirmar el formulario.
     * </p>
     *
     * @param ubicacion coordenada donde el usuario mantuvo presionado
     */
    private void alPulsacionLarga(@NonNull Ubicacion ubicacion) {
        if (lugarEnMovimiento != null) {
            moverLugar(lugarEnMovimiento, ubicacion);
            return;
        }
        new LugarFormSheet(this, mapaViewModel, null, ubicacion,
                () -> mapaController.centrarEn(ubicacion)).mostrar();
    }

    /**
     * Marca un pin independiente para que la próxima pulsación larga lo mueva.
     *
     * @param lugar pin a mover
     */
    private void solicitarMoverLugar(@NonNull Lugar lugar) {
        lugarEnMovimiento = lugar;
        mostrarAviso(R.string.aMapa_toast_mover_hint);
    }

    /**
     * Mueve un pin independiente a la coordenada indicada y refresca el mapa.
     *
     * @param lugar     pin a mover
     * @param ubicacion nueva coordenada
     */
    private void moverLugar(@NonNull Lugar lugar, @NonNull Ubicacion ubicacion) {
        MutableLiveData<ErrorCode> resultado = new MutableLiveData<>();
        resultado.observe(this, errorCode -> {
            lugarEnMovimiento = null;
            if (errorCode == null) {
                mapaController.centrarEn(ubicacion);
                Toast.makeText(this, R.string.aMapa_toast_lugar_movido,
                        Toast.LENGTH_SHORT).show();
            } else {
                mostrarAviso(R.string.aMapa_toast_lugar_error);
            }
        });
        mapaViewModel.moverLugar(lugar, ubicacion, resultado);
    }

    /**
     * Punto de entrada de "Mi ubicación": comprueba el permiso y lo solicita
     * solo si hace falta. Denegarlo no bloquea ninguna otra función.
     */
    private void solicitarUbicacionActual() {
        boolean fina = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean gruesa = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fina || gruesa) {
            centrarEnUbicacionActual();
        } else {
            permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Centra el mapa en la ubicación actual. Usa la última posición conocida si
     * es reciente y, si no hay, pide una fijación única con un límite de tiempo.
     * Cualquier fallo se informa y el mapa queda operativo (CA-14).
     */
    private void centrarEnUbicacionActual() {
        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            mostrarAviso(R.string.aMapa_error_sin_ubicacion);
            return;
        }

        Location ultima = obtenerUltimaUbicacion(locationManager);
        if (ultima != null) {
            centrarEn(ultima);
            return;
        }

        boolean fina = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        String proveedor = null;
        try {
            if (fina && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                proveedor = LocationManager.GPS_PROVIDER;
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                proveedor = LocationManager.NETWORK_PROVIDER;
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Proveedor de ubicación no disponible", e);
        }

        if (proveedor == null) {
            mostrarAviso(R.string.aMapa_error_sin_proveedor);
            return;
        }
        solicitarUbicacionUnica(locationManager, proveedor);
    }

    /**
     * Busca la última posición conocida entre los proveedores disponibles.
     *
     * @param locationManager servicio de ubicación
     * @return la posición más reciente o {@code null} si no hay ninguna
     */
    private Location obtenerUltimaUbicacion(@NonNull LocationManager locationManager) {
        Location mejor = null;
        String[] proveedores = {
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
        };
        for (String proveedor : proveedores) {
            try {
                if (!locationManager.isProviderEnabled(proveedor)) {
                    continue;
                }
                Location candidata = locationManager.getLastKnownLocation(proveedor);
                if (candidata != null && (mejor == null || candidata.getTime() > mejor.getTime())) {
                    mejor = candidata;
                }
            } catch (SecurityException | IllegalArgumentException e) {
                Log.w(TAG, "No se pudo leer la última ubicación de " + proveedor, e);
            }
        }
        return mejor;
    }

    /**
     * Pide una fijación de ubicación única y la entrega en el hilo principal.
     * Si en {@link #TIEMPO_MAXIMO_UBICACION_MS} no hay respuesta, cancela y
     * avisa al usuario.
     *
     * @param locationManager servicio de ubicación
     * @param proveedor       proveedor habilitado a consultar
     */
    private void solicitarUbicacionUnica(@NonNull LocationManager locationManager,
                                         @NonNull String proveedor) {
        if (cancelacionUbicacion != null) {
            cancelacionUbicacion.cancel();
        }
        CancellationSignal senal = new CancellationSignal();
        cancelacionUbicacion = senal;

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable tiempoAgotado = () -> {
            if (cancelacionUbicacion == senal) {
                senal.cancel();
                cancelacionUbicacion = null;
                mostrarAviso(R.string.aMapa_error_sin_ubicacion);
            }
        };
        handler.postDelayed(tiempoAgotado, TIEMPO_MAXIMO_UBICACION_MS);

        try {
            LocationManagerCompat.getCurrentLocation(locationManager, proveedor, senal,
                    getMainExecutor(), location -> {
                        handler.removeCallbacks(tiempoAgotado);
                        if (senal.isCanceled()) {
                            return;
                        }
                        cancelacionUbicacion = null;
                        if (location != null) {
                            centrarEn(location);
                        } else {
                            mostrarAviso(R.string.aMapa_error_sin_ubicacion);
                        }
                    });
        } catch (SecurityException | IllegalArgumentException e) {
            handler.removeCallbacks(tiempoAgotado);
            cancelacionUbicacion = null;
            Log.w(TAG, "No se pudo obtener la ubicación actual", e);
            mostrarAviso(R.string.aMapa_error_sin_ubicacion);
        }
    }

    /**
     * Centra el mapa en una posición del framework.
     *
     * @param location posición obtenida de {@code LocationManager}
     */
    private void centrarEn(@NonNull Location location) {
        mapaController.centrarEn(
                new Ubicacion(location.getLatitude(), location.getLongitude()));
    }

    /**
     * Muestra un aviso breve al usuario.
     *
     * @param mensaje recurso de texto
     */
    private void mostrarAviso(int mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
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
     * Registra el contrato con {@link SeleccionarUbicacionActivity} (T24).
     * <p>
     * Con un resultado confirmado guarda las coordenadas y actualiza el
     * resumen del panel. Si el usuario cancela, apaga el interruptor y el
     * movimiento quedará sin ubicación (CA-4).
     * </p>
     */
    private void registrarLanzadorUbicacion() {
        lanzadorUbicacion = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), resultado -> {
                    if (sheetTransaccionBinding == null) {
                        return;
                    }
                    if (resultado.getResultCode() == RESULT_OK
                            && resultado.getData() != null) {
                        double latitud = resultado.getData().getDoubleExtra(
                                SeleccionarUbicacionActivity.EXTRA_LATITUD, Double.NaN);
                        double longitud = resultado.getData().getDoubleExtra(
                                SeleccionarUbicacionActivity.EXTRA_LONGITUD, Double.NaN);
                        if (Double.isNaN(latitud) || Double.isNaN(longitud)) {
                            limpiarUbicacionMovimiento(sheetTransaccionBinding);
                            return;
                        }
                        latitudMovimiento = latitud;
                        longitudMovimiento = longitud;
                        actualizandoSwitchUbicacion = true;
                        sheetTransaccionBinding.switchGeolocalizacion.setChecked(true);
                        actualizandoSwitchUbicacion = false;
                        mostrarResumenUbicacion(sheetTransaccionBinding);
                    } else {
                        limpiarUbicacionMovimiento(sheetTransaccionBinding);
                    }
                });
    }

    /**
     * Abre la Activity de selección de ubicación.
     */
    private void lanzarSeleccionUbicacion() {
        lanzadorUbicacion.launch(
                new Intent(this, SeleccionarUbicacionActivity.class));
    }

    /**
     * Descarta la ubicación elegida y apaga el interruptor sin relanzar la
     * selección.
     *
     * @param sheetBinding binding del panel abierto
     */
    private void limpiarUbicacionMovimiento(
            @NonNull BottomSheetTransaccionBinding sheetBinding) {
        latitudMovimiento = null;
        longitudMovimiento = null;
        actualizandoSwitchUbicacion = true;
        sheetBinding.switchGeolocalizacion.setChecked(false);
        actualizandoSwitchUbicacion = false;
        mostrarResumenUbicacion(sheetBinding);
    }

    /**
     * Muestra u oculta el resumen de coordenadas del panel.
     *
     * @param sheetBinding binding del panel abierto
     */
    private void mostrarResumenUbicacion(
            @NonNull BottomSheetTransaccionBinding sheetBinding) {
        if (latitudMovimiento == null || longitudMovimiento == null) {
            sheetBinding.tvUbicacionSeleccionada.setVisibility(View.GONE);
            return;
        }
        sheetBinding.tvUbicacionSeleccionada.setText(getString(
                R.string.aCuentas_transaccion_ubicacion,
                latitudMovimiento, longitudMovimiento));
        sheetBinding.tvUbicacionSeleccionada.setVisibility(View.VISIBLE);
    }

    /**
     * Prepara el bloque de gasto compartido del formulario (Feature 012, T11/T12).
     * <p>
     * La Activity solo orquesta: pinta los contactos de la agenda, captura las
     * decisiones del usuario y las manda al {@link TransaccionViewModel}, que
     * delega el cálculo y la validación en
     * {@link com.localcash.domain.service.GastoCompartidoService}. La suma en
     * vivo y el habilitado de "Registrar" salen del {@link EstadoReparto} que
     * expone el ViewModel.
     * </p>
     *
     * @param sheetBinding Binding del BottomSheet abierto
     */
    private void configurarDivision(@NonNull BottomSheetTransaccionBinding sheetBinding) {
        transaccionViewModel.reiniciarDivision();
        firmaFilasParte = "";
        sheetBinding.rgModoDivision.setOnCheckedChangeListener(null);
        sheetBinding.rbIgual.setChecked(true);
        sheetBinding.contenedorPartes.removeAllViews();
        poblarSelectorContactos(sheetBinding);

        // El reparto solo aplica a egresos: en un ingreso el checkbox se apaga
        // y se desmarca (CA-12 de la feature, regla de la misión).
        actualizandoCheckCompartido = true;
        sheetBinding.chkCompartido.setChecked(false);
        sheetBinding.chkCompartido.setEnabled(
                sheetBinding.toggleTipoTransaccion.getCheckedButtonId() == R.id.btnEgreso);
        actualizandoCheckCompartido = false;

        sheetBinding.toggleTipoTransaccion.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {
                    if (!isChecked) {
                        return;
                    }
                    boolean esEgreso = checkedId == R.id.btnEgreso;
                    actualizandoCheckCompartido = true;
                    sheetBinding.chkCompartido.setEnabled(esEgreso);
                    if (!esEgreso) {
                        sheetBinding.chkCompartido.setChecked(false);
                    }
                    actualizandoCheckCompartido = false;
                });

        sheetBinding.chkCompartido.setOnCheckedChangeListener((boton, marcado) -> {
            if (actualizandoCheckCompartido) {
                return;
            }
            transaccionViewModel.setCompartido(marcado);
        });

        sheetBinding.rbIgual.setOnClickListener(v -> {
            transaccionViewModel.setModoDivision(ModoDivision.IGUAL);
            reconstruirFilasParte(sheetBinding);
        });
        sheetBinding.rbPersonalizado.setOnClickListener(v -> {
            transaccionViewModel.setModoDivision(ModoDivision.PERSONALIZADA);
            reconstruirFilasParte(sheetBinding);
        });

        // El total del egreso entra al estado del reparto para poder comparar.
        instalarWatcherMoneda(sheetBinding.inputMonto,
                valor -> transaccionViewModel.setMontoTotal(valor));

        sheetBinding.searchContactos.setOnItemClickListener(
                (AdapterView<?> parent, View vista, int position, long id) -> {
                    String nombre = (String) parent.getItemAtPosition(position);
                    agregarContactoSeleccionado(sheetBinding, nombre);
                });

        observadorEstadoReparto = estado -> aplicarEstadoReparto(sheetBinding, estado);
        transaccionViewModel.getEstadoReparto().observe(this, observadorEstadoReparto);
        transaccionViewModel.getDivisionVisible().observe(this, visible -> {
            sheetBinding.layoutDivision.setVisibility(
                    Boolean.TRUE.equals(visible) ? View.VISIBLE : View.GONE);
            if (Boolean.TRUE.equals(visible)) {
                poblarSelectorContactos(sheetBinding);
                reconstruirFilasParte(sheetBinding);
            }
        });
    }

    /**
     * Libera el observador del reparto: la hoja se cerró y su binding ya no
     * debe recibir emisiones.
     */
    private void quitarObservadorDivision() {
        if (observadorEstadoReparto != null) {
            transaccionViewModel.getEstadoReparto().removeObserver(observadorEstadoReparto);
            observadorEstadoReparto = null;
        }
        transaccionViewModel.getDivisionVisible().removeObservers(this);
    }

    /**
     * Pinta un {@link CheckBox} por contacto de la agenda (Feature 011) y
     * conserva la selección vigente al repoblar.
     *
     * @param sheetBinding Binding del BottomSheet abierto
     */
    private void poblarSelectorContactos(@NonNull BottomSheetTransaccionBinding sheetBinding) {
        // Sugerencias del buscador (se filtran solas al escribir).
        List<String> nombres = new ArrayList<>();
        for (Contacto contacto : contactosAgenda) {
            nombres.add(contacto.getNombre());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                sheetBinding.getRoot().getContext(),
                android.R.layout.simple_dropdown_item_1line, nombres);
        sheetBinding.searchContactos.setAdapter(adapter);
        sheetBinding.searchContactos.setText("", false);

        sheetBinding.tvSinContactos.setVisibility(
                contactosAgenda.isEmpty() ? View.VISIBLE : View.GONE);

        // Chips: solo los ya elegidos, cada uno con su "X" para quitarlo.
        ChipGroup grupo = sheetBinding.chipGroupContactos;
        List<Long> seleccionActual = transaccionViewModel.getContactosSeleccionados();
        grupo.removeAllViews();
        for (Long contactoId : seleccionActual) {
            Chip chip = new Chip(grupo.getContext());
            chip.setText(nombreContacto(contactoId.longValue()));
            chip.setTag(contactoId);
            chip.setCheckable(false);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                grupo.removeView(chip);
                actualizarSeleccionContactos(sheetBinding);
            });
            grupo.addView(chip);
        }
    }

    /**
     * Agrega un contacto al reparto desde el desplegable y limpia el buscador.
     *
     * @param sheetBinding Binding del BottomSheet abierto
     * @param nombre       nombre elegido en el desplegable
     */
    private void agregarContactoSeleccionado(
            @NonNull BottomSheetTransaccionBinding sheetBinding, String nombre) {
        for (Contacto contacto : contactosAgenda) {
            if (contacto.getNombre().equals(nombre)) {
                List<Long> seleccion = transaccionViewModel.getContactosSeleccionados();
                if (!seleccion.contains(contacto.getId())) {
                    seleccion.add(contacto.getId());
                }
                transaccionViewModel.setContactosSeleccionados(seleccion);
                break;
            }
        }
        sheetBinding.searchContactos.setText("", false);
        poblarSelectorContactos(sheetBinding);
        reconstruirFilasParte(sheetBinding);
    }

    /**
     * Vuelca la selección de chips al ViewModel en el orden de la agenda.
     *
     * @param sheetBinding Binding del BottomSheet abierto
     */
    private void actualizarSeleccionContactos(
            @NonNull BottomSheetTransaccionBinding sheetBinding) {
        List<Long> seleccion = new ArrayList<>();
        ChipGroup grupo = sheetBinding.chipGroupContactos;
        for (int i = 0; i < grupo.getChildCount(); i++) {
            View hijo = grupo.getChildAt(i);
            if (hijo instanceof Chip && hijo.getTag() instanceof Long) {
                seleccion.add((Long) hijo.getTag());
            }
        }
        transaccionViewModel.setContactosSeleccionados(seleccion);
        reconstruirFilasParte(sheetBinding);
    }

    /**
     * Reconstruye las filas de monto solo cuando cambia la firma
     * (modo + contactos), para no robar el foco del EditText en cada tecla.
     * En "partes iguales" las filas muestran el monto calculado y no son
     * editables.
     *
     * @param sheetBinding Binding del BottomSheet abierto
     */
    private void reconstruirFilasParte(@NonNull BottomSheetTransaccionBinding sheetBinding) {
        List<Long> seleccion = transaccionViewModel.getContactosSeleccionados();
        boolean personalizado =
                transaccionViewModel.getModoDivision() == ModoDivision.PERSONALIZADA;

        StringBuilder firma = new StringBuilder()
                .append(personalizado ? 'P' : 'I').append('|').append(seleccion.size());
        for (Long id : seleccion) {
            firma.append(',').append(id);
        }
        String nuevaFirma = firma.toString();

        if (nuevaFirma.equals(firmaFilasParte)) {
            return;
        }
        firmaFilasParte = nuevaFirma;

        LinearLayout contenedor = sheetBinding.contenedorPartes;
        contenedor.removeAllViews();
        if (seleccion.isEmpty()) {
            contenedor.setVisibility(View.GONE);
            return;
        }
        contenedor.setVisibility(View.VISIBLE);

        java.util.Map<Long, Long> montosIguales = new java.util.HashMap<>();
        if (!personalizado) {
            EstadoReparto estadoActual = transaccionViewModel.getEstadoReparto().getValue();
            if (estadoActual != null) {
                for (PartePropuesta parte : estadoActual.getPartes()) {
                    montosIguales.put(Long.valueOf(parte.getContactoId()),
                            Long.valueOf(parte.getMontoAsignado()));
                }
            }
        }

        LayoutInflater inflater = LayoutInflater.from(contenedor.getContext());
        for (Long contactoId : seleccion) {
            ItemParteGastoBinding fila = ItemParteGastoBinding.inflate(
                    inflater, contenedor, false);
            fila.tvNombreContacto.setText(nombreContacto(contactoId));
            final long id = contactoId.longValue();

            if (personalizado) {
                long captura = transaccionViewModel.getMontoPersonalizado(id);
                fila.inputMontoParte.setText(captura > 0L
                        ? CurrencyFormatter.format(captura) : "");
                instalarWatcherMoneda(fila.inputMontoParte,
                        valor -> transaccionViewModel.setMontoPersonalizado(id, valor));
            } else {
                Long monto = montosIguales.get(Long.valueOf(id));
                fila.inputMontoParte.setText(
                        monto == null ? "" : CurrencyFormatter.format(monto.longValue()));
                fila.inputMontoParte.setEnabled(false);
            }
            fila.getRoot().setTag(Long.valueOf(id));
            contenedor.addView(fila.getRoot());
        }
    }

    /**
     * Pinta la suma en vivo (verde si cuadra, rojo si no) y habilita "Registrar"
     * solo cuando el dominio aprueba el reparto.
     *
     * @param sheetBinding Binding del BottomSheet abierto
     * @param estado       estado del reparto emitido por el ViewModel
     */
    private void aplicarEstadoReparto(@NonNull BottomSheetTransaccionBinding sheetBinding,
                                      @Nullable EstadoReparto estado) {
        if (estado == null) {
            return;
        }
        if (!estado.isCompartido()) {
            sheetBinding.btnGuardarMovimiento.setEnabled(true);
            return;
        }

        String texto = getString(R.string.aGastoCompartido_suma_en_vivo,
                CurrencyFormatter.format(estado.getSumaParcial()),
                CurrencyFormatter.format(estado.getMontoTotal()));
        if (estado.getNumeroPartes() > 0) {
            texto = texto + " · " + estado.getNumeroPartes() + " partes";
        }
        if (estado.getParteUsuario() > 0L) {
            texto = texto + " · " + getString(R.string.aGastoCompartido_tu_parte,
                    CurrencyFormatter.format(estado.getParteUsuario()));
        }
        if (!estado.isValido() && estado.getError() != null) {
            texto = texto + " · " + getString(mensajeReparto(estado.getError()));
        }
        sheetBinding.tvSumaEnVivo.setText(texto);
        sheetBinding.tvSumaEnVivo.setTextColor(MaterialColors.getColor(
                sheetBinding.tvSumaEnVivo,
                estado.isValido()
                        ? androidx.appcompat.R.attr.colorPrimary
                        : androidx.appcompat.R.attr.colorError));
        sheetBinding.btnGuardarMovimiento.setEnabled(estado.isValido());

        if (estado.getModo() == ModoDivision.IGUAL && estado.isValido()) {
            refrescarMontosIguales(sheetBinding, estado.getPartes());
        }
    }

    /**
     * Actualiza las filas de solo lectura del reparto igual sin reconstruirlas
     * (los montos cambian cuando el usuario edita el total).
     *
     * @param sheetBinding Binding del BottomSheet abierto
     * @param partes       partes calculadas por el dominio
     */
    private void refrescarMontosIguales(@NonNull BottomSheetTransaccionBinding sheetBinding,
                                        @NonNull List<PartePropuesta> partes) {
        java.util.Map<Long, Long> montos = new java.util.HashMap<>();
        for (PartePropuesta parte : partes) {
            montos.put(Long.valueOf(parte.getContactoId()),
                    Long.valueOf(parte.getMontoAsignado()));
        }
        LinearLayout contenedor = sheetBinding.contenedorPartes;
        for (int i = 0; i < contenedor.getChildCount(); i++) {
            View fila = contenedor.getChildAt(i);
            if (!(fila.getTag() instanceof Long)) {
                continue;
            }
            Long monto = montos.get((Long) fila.getTag());
            if (monto == null) {
                continue;
            }
            com.google.android.material.textfield.TextInputEditText campo =
                    fila.findViewById(R.id.inputMontoParte);
            if (campo != null) {
                campo.setText(CurrencyFormatter.format(monto.longValue()));
            }
        }
    }

    /**
     * @param contactoId id del contacto
     * @return nombre visible para la fila de monto
     */
    private String nombreContacto(long contactoId) {
        String nombre = String.valueOf(contactoId);
        for (Contacto contacto : contactosAgenda) {
            if (contacto.getId() == contactoId) {
                nombre = contacto.getNombre();
                break;
            }
        }
        return nombre;
    }

    /**
     * @param error código de negocio del reparto
     * @return recurso de texto que explica el rechazo al usuario
     */
    private int mensajeReparto(@NonNull ErrorCode error) {
        switch (error) {
            case GASTO_SIN_CONTACTOS:
                return R.string.aGastoCompartido_toast_sin_contactos;
            case REPARTO_NO_CUADRA:
                return R.string.aGastoCompartido_toast_reparto_no_cuadra;
            case REPARTO_IGUAL_INSUFICIENTE:
                return R.string.aGastoCompartido_toast_reparto_insuficiente;
            case MONTO_PARTE_INVALIDO:
                return R.string.aGastoCompartido_toast_monto_parte_invalido;
            case CONTACTO_DUPLICADO:
                return R.string.aGastoCompartido_toast_contacto_duplicado;
            case MONTO_INVALIDO:
                return R.string.aGastoCompartido_toast_monto_invalido;
            default:
                return R.string.aGastoCompartido_toast_reparto_error;
        }
    }

    /**
     * Instala el formato de moneda chilena ($5.000, sin "CLP") en un campo de
     * monto y entrega al callback el valor limpio (solo dígitos).
     *
     * @param campo      EditText de monto
     * @param alCambiar  receptor del valor en pesos CLP
     */
    private void instalarWatcherMoneda(@NonNull android.widget.EditText campo,
                                       @NonNull LongConsumer alCambiar) {
        campo.addTextChangedListener(new TextWatcher() {
            private boolean formateando;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (formateando) {
                    return;
                }
                String crudo = s == null ? "" : s.toString().replaceAll("[^0-9]", "");
                long valor = 0L;
                if (!crudo.isEmpty()) {
                    try {
                        valor = Long.parseLong(crudo);
                    } catch (NumberFormatException e) {
                        valor = 0L;
                    }
                }
                formateando = true;
                String formateado = valor > 0L ? CurrencyFormatter.format(valor) : "";
                campo.setText(formateado);
                campo.setSelection(formateado.length());
                formateando = false;
                alCambiar.accept(valor);
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
        configurarDivision(sheetBinding);
        sheetBinding.btnCerrar.setOnClickListener(v -> sheetDialog.dismiss());
        sheetBinding.btnCancelar.setOnClickListener(v -> sheetDialog.dismiss());

        sheetBinding.spinnerCuentas.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < cuentasSpinner.size()) {
                cuentaSeleccionadaId = cuentasSpinner.get(position).getId();
                sheetBinding.inputLayoutCuenta.setError(null);
            }
        });

        sheetBinding.btnGuardarMovimiento.setOnClickListener(v ->
                registrarMovimiento(sheetBinding, sheetDialog));

        // El movimiento nuevo empieza sin ubicación; si el usuario activa el
        // interruptor se abre el mapa de selección (T24).
        latitudMovimiento = null;
        longitudMovimiento = null;
        actualizandoSwitchUbicacion = true;
        sheetBinding.switchGeolocalizacion.setChecked(false);
        actualizandoSwitchUbicacion = false;
        mostrarResumenUbicacion(sheetBinding);
        sheetBinding.switchGeolocalizacion.setOnCheckedChangeListener((boton, activo) -> {
            if (actualizandoSwitchUbicacion) {
                return;
            }
            if (activo) {
                lanzarSeleccionUbicacion();
            } else {
                latitudMovimiento = null;
                longitudMovimiento = null;
                mostrarResumenUbicacion(sheetBinding);
            }
        });

        sheetDialog.setOnDismissListener(dialog -> {
            sheetTransaccionBinding = null;
            quitarObservadorDivision();
            transaccionViewModel.reiniciarDivision();
            firmaFilasParte = "";
        });
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

        // Reparto compartido: la validación la decide el dominio a través del
        // ViewModel; si rechaza el reparto, no se invoca el registro (CA-2/CA-5).
        List<PartePropuesta> partes = null;
        if (transaccionViewModel.isCompartido()) {
            ErrorCode errorReparto = transaccionViewModel.validarReparto();
            if (errorReparto != null) {
                Toast.makeText(this, mensajeReparto(errorReparto), Toast.LENGTH_SHORT).show();
                return;
            }
            partes = transaccionViewModel.partesParaRegistrar();
        }

        final long montoFinal = monto;
        final Double latitudFinal = latitudMovimiento;
        final Double longitudFinal = longitudMovimiento;
        final List<PartePropuesta> partesFinal = partes;
        LiveData<ErrorCode> resultado = transaccionViewModel.registrarTransaccion(
                cuentaSeleccionadaId, tipo, montoFinal, concepto,
                latitudFinal, longitudFinal, null, partesFinal);

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
            case GASTO_SIN_CONTACTOS:
            case REPARTO_NO_CUADRA:
            case REPARTO_IGUAL_INSUFICIENTE:
            case MONTO_PARTE_INVALIDO:
            case CONTACTO_DUPLICADO:
            case GASTO_COMPARTIDO_INVALIDO:
                // Rechazos del reparto: el mismo traductor que usa la suma en
                // vivo (Feature 012).
                mensaje = mensajeReparto(errorCode);
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
        limpiarUbicacionMovimiento(sheetBinding);

        // El reparto vuelve a su estado inicial: oculto, sin selección y con
        // el bloque de división limpio (Feature 012).
        actualizandoCheckCompartido = true;
        sheetBinding.chkCompartido.setChecked(false);
        actualizandoCheckCompartido = false;
        sheetBinding.rgModoDivision.setOnCheckedChangeListener(null);
        sheetBinding.rbIgual.setChecked(true);
        sheetBinding.chipGroupContactos.removeAllViews();
        sheetBinding.searchContactos.setText("", false);
        sheetBinding.contenedorPartes.removeAllViews();
        sheetBinding.tvSumaEnVivo.setText("");
        firmaFilasParte = "";
        transaccionViewModel.reiniciarDivision();
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
