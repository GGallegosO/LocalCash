package com.localcash.ui.cuentas;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.localcash.databinding.ActivityCuentasBinding;
import com.localcash.databinding.BottomSheetTransaccionBinding;
import com.localcash.databinding.ViewCuentasBinding;
import com.localcash.databinding.ViewHistorialBinding;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.TipoCuenta;
import com.localcash.domain.model.TipoTransaccion;
import com.localcash.domain.model.Transaccion;
import com.localcash.ui.BaseActivity;
import com.localcash.ui.mapa.MapaConfig;
import com.localcash.ui.theme.ThemeManager;
import com.localcash.ui.util.CurrencyFormatter;

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

    private ActivityCuentasBinding binding;

    /** Binding de la Vista 1 (se infla dentro del ViewFlipper, fuera de activity_cuentas.xml). */
    private ViewCuentasBinding vistaCuentas;

    /** Binding de la Vista 1 (Historial/Movimientos), inflado dentro del ViewFlipper. */
    private ViewHistorialBinding vistaHistorial;

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

    /** Cuentas activas asociadas, en el mismo orden que el adapter del spinner. */
    private final List<Cuenta> cuentasSpinner = new ArrayList<>();

    /** Id de la cuenta seleccionada en el spinner ({@code -1} si no hay selección). */
    private long cuentaSeleccionadaId = -1L;

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

        // MapLibre debe inicializarse antes de inflar cualquier MapView.
        // La pestaña Mapa todavía usa un placeholder; la inicialización queda
        // lista para la fase 2.
        MapaConfig.inicializar(this);

        // Inflar los 4 layouts internos dentro del ViewFlipper
        // Vista 0: Cuentas (binding propio: sus vistas no forman parte de activity_cuentas.xml)
        vistaCuentas = ViewCuentasBinding.inflate(getLayoutInflater(), binding.viewFlipper, true);
        // Vista 1: Historial/Movimientos
        vistaHistorial = ViewHistorialBinding.inflate(
                getLayoutInflater(), binding.viewFlipper, true);
        // Vista 2: Mapa
        getLayoutInflater().inflate(R.layout.view_mapa, binding.viewFlipper);
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

        // Preparar el ViewModel de transacciones (alimenta el BottomSheet del FAB)
        configurarTransaccionViewModel();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Guardar el índice actual del ViewFlipper para preservar la vista seleccionada
        outState.putInt("viewFlipperIndex", binding.viewFlipper.getDisplayedChild());
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
                cuentaSeleccionadaId, tipo, montoFinal, concepto);

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
