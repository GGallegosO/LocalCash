package com.localcash.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.localcash.R;
import com.localcash.data.security.SecureAuthManager;
import com.localcash.databinding.ActivityRegisterBinding;
import com.localcash.ui.BaseActivity;
import com.localcash.ui.cuentas.CuentasActivity;
import com.localcash.ui.theme.ThemeManager;

/**
 * Pantalla de registro de usuarios en 3 pasos (ViewFlipper).
 *
 * los pasos 1 y 2 solo escriben en variables temporales en memoria
 * ({@code tempRegion}, {@code tempCity}, {@code tempUsername}, {@code tempPassword}).
 * La UNICA escritura persistente a EncryptedSharedPreferences ocurre en
 * {@link #attemptRegister()}, invocado solo por el boton Finalizar del paso 3.
 * El boton Cancelar solo limpia los temporales y hace {@code finish()}: nunca guarda.</p>
 */
public class RegisterActivity extends BaseActivity {

    private static final String TAG = "RegisterActivity";

    // Claves para onSaveInstanceState.
    private static final String KEY_CURRENT_STEP = "current_step";
    private static final String KEY_REGION = "region";
    private static final String KEY_CITY = "city";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    private static final int STEP_REGION_CITY = 0;
    private static final int STEP_CREDENTIALS = 1;
    private static final int STEP_CONFIRM = 2;

    private ActivityRegisterBinding binding;

    // Datos temporales en memoria. NUNCA se persisten hasta attemptRegister().
    private String tempRegion = "";
    private String tempCity = "";
    private String tempUsername = "";
    private String tempPassword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // El tema se aplica automaticamente en BaseActivity.onCreate().
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Restaura paso y textos tras rotacion o recreate() por cambio de tema.
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        setupClickListeners();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Paso visible del ViewFlipper + contenido actual de los campos.
        outState.putInt(KEY_CURRENT_STEP, binding.viewFlipper.getDisplayedChild());
        outState.putString(KEY_REGION, textOf(binding.step1.tilRegisterRegion.getEditText()));
        outState.putString(KEY_CITY, textOf(binding.step1.tilRegisterCity.getEditText()));
        outState.putString(KEY_USERNAME, textOf(binding.step2.tilRegisterUsername.getEditText()));
        outState.putString(KEY_PASSWORD, textOf(binding.step2.tilRegisterPassword.getEditText()));
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        // Restaura los EditText...
        binding.step1.tilRegisterRegion.getEditText()
                .setText(savedInstanceState.getString(KEY_REGION));
        binding.step1.tilRegisterCity.getEditText()
                .setText(savedInstanceState.getString(KEY_CITY));
        binding.step2.tilRegisterUsername.getEditText()
                .setText(savedInstanceState.getString(KEY_USERNAME));
        binding.step2.tilRegisterPassword.getEditText()
                .setText(savedInstanceState.getString(KEY_PASSWORD));
        // ...y SINCRONIZA los temporales, para que attemptRegister() no los vea vacios
        // tras un recreate() (antes solo se restauraba la vista: bug de "complete campos").
        syncTempsFromUi();
        // Restaura el paso visible.
        int step = savedInstanceState.getInt(KEY_CURRENT_STEP, STEP_REGION_CITY);
        binding.viewFlipper.setDisplayedChild(step);
        if (step == STEP_CONFIRM) {
            updateConfirmSummary();
        }
        Log.d(TAG, "Estado restaurado: paso=" + step);
    }

    private void setupClickListeners() {
        binding.btnThemeToggle.setOnClickListener(v -> toggleTheme());

        // Paso 1: Atras -> Login (sin guardar nada), Continuar -> valida y avanza.
        binding.step1.btnStep1Back.setOnClickListener(v -> {
            clearTempData();
            finish();
        });
        binding.step1.btnStep1Next.setOnClickListener(v -> {
            if (validateStep1()) {
                saveStep1Data();
                binding.viewFlipper.setDisplayedChild(STEP_CREDENTIALS);
            }
        });

        // Paso 2: Atras -> Paso 1, Continuar -> valida y avanza (sin persistir).
        binding.step2.btnStep2Back.setOnClickListener(v -> {
            saveStep2Data();
            binding.viewFlipper.setDisplayedChild(STEP_REGION_CITY);
        });
        binding.step2.btnStep2Next.setOnClickListener(v -> {
            if (validateStep2()) {
                saveStep2Data();
                updateConfirmSummary();
                binding.viewFlipper.setDisplayedChild(STEP_CONFIRM);
            }
        });

        // Paso 3: Cancelar -> limpia temporales y vuelve al Login SIN guardar.
        // Finalizar -> UNICA llamada a SecureAuthManager.registerUser().
        binding.step3.btnStep3Cancel.setOnClickListener(v -> {
            clearTempData();
            finish();
        });
        binding.step3.btnStep3Finish.setOnClickListener(v -> attemptRegister());
    }

    private void toggleTheme() {
        boolean newDarkMode = !ThemeManager.isDarkMode(this);
        ThemeManager.setDarkMode(this, newDarkMode);
        // recreate() dispara onSaveInstanceState() antes de destruir: nada se pierde.
        recreate();
    }

    // ==================== VALIDACIONES POR PASO (solo lectura) ====================

    /** Paso 1: region y ciudad no vacias. No toca SecureAuthManager. */
    private boolean validateStep1() {
        String regionInput = textOf(binding.step1.tilRegisterRegion.getEditText()).trim();
        String cityInput = textOf(binding.step1.tilRegisterCity.getEditText()).trim();

        if (regionInput.isEmpty()) {
            showStepError(binding.step1.tvMessageStep1,
                    getString(R.string.aRegister_error_empty_region));
            binding.step1.tilRegisterRegion.setError(
                    getString(R.string.aRegister_error_empty_region));
            return false;
        }
        binding.step1.tilRegisterRegion.setError(null);

        if (cityInput.isEmpty()) {
            showStepError(binding.step1.tvMessageStep1,
                    getString(R.string.aRegister_error_empty_city));
            binding.step1.tilRegisterCity.setError(
                    getString(R.string.aRegister_error_empty_city));
            return false;
        }
        binding.step1.tilRegisterCity.setError(null);

        hideStepError(binding.step1.tvMessageStep1);
        return true;
    }

    /**
     * Paso 2: usuario y contrasena no vacios, usuario no reservado.
     * {@code userExists()} es SOLO lectura: verifica usuarios ya confirmados,
     * nunca crea registros para usuarios en proceso.
     */
    private boolean validateStep2() {
        String usernameInput = textOf(binding.step2.tilRegisterUsername.getEditText()).trim();
        String passwordInput = textOf(binding.step2.tilRegisterPassword.getEditText()).trim();

        if (usernameInput.isEmpty()) {
            showStepError(binding.step2.tvMessageStep2,
                    getString(R.string.aRegister_error_empty_username));
            binding.step2.tilRegisterUsername.setError(
                    getString(R.string.aRegister_error_empty_username));
            return false;
        }
        binding.step2.tilRegisterUsername.setError(null);

        if (passwordInput.isEmpty()) {
            showStepError(binding.step2.tvMessageStep2,
                    getString(R.string.aRegister_error_empty_password));
            binding.step2.tilRegisterPassword.setError(
                    getString(R.string.aRegister_error_empty_password));
            return false;
        }
        binding.step2.tilRegisterPassword.setError(null);

        if (usernameInput.equalsIgnoreCase("admin")) {
            showStepError(binding.step2.tvMessageStep2,
                    getString(R.string.aRegister_error_reserved_user));
            binding.step2.tilRegisterUsername.setError(
                    getString(R.string.aRegister_error_reserved_user));
            return false;
        }

        if (SecureAuthManager.userExists(this, usernameInput)) {
            showStepError(binding.step2.tvMessageStep2,
                    getString(R.string.aRegister_error_user_exists));
            binding.step2.tilRegisterUsername.setError(
                    getString(R.string.aRegister_error_user_exists));
            return false;
        }

        hideStepError(binding.step2.tvMessageStep2);
        return true;
    }

    // ==================== TEMPORALES EN MEMORIA (no persisten) ====================

    private void saveStep1Data() {
        tempRegion = textOf(binding.step1.tilRegisterRegion.getEditText()).trim();
        tempCity = textOf(binding.step1.tilRegisterCity.getEditText()).trim();
        Log.d(TAG, "Temporales paso 1: region=" + tempRegion + ", city=" + tempCity);
    }

    private void saveStep2Data() {
        tempUsername = textOf(binding.step2.tilRegisterUsername.getEditText()).trim();
        tempPassword = textOf(binding.step2.tilRegisterPassword.getEditText()).trim();
        Log.d(TAG, "Temporales paso 2: username=" + tempUsername);
    }

    /** Copia lo visible en los EditText a los temporales (tras recreate). */
    private void syncTempsFromUi() {
        tempRegion = textOf(binding.step1.tilRegisterRegion.getEditText()).trim();
        tempCity = textOf(binding.step1.tilRegisterCity.getEditText()).trim();
        tempUsername = textOf(binding.step2.tilRegisterUsername.getEditText()).trim();
        tempPassword = textOf(binding.step2.tilRegisterPassword.getEditText()).trim();
    }

    /** Limpia los temporales: Cancelar o registro exitoso. Nunca toca el almacenamiento. */
    private void clearTempData() {
        tempRegion = "";
        tempCity = "";
        tempUsername = "";
        tempPassword = "";
        Log.d(TAG, "Temporales limpiados");
    }

    private void updateConfirmSummary() {
        String summary = "Usuario: " + tempUsername
                + "\nRegión: " + tempRegion
                + "\nCiudad: " + tempCity;
        binding.step3.tvConfirmSummary.setText(summary);
        binding.step3.tvConfirmSummary.setVisibility(View.VISIBLE);
    }

    // ==================== REGISTRO FINAL (unica escritura) ====================

    /**
     * UNICA llamada a {@code SecureAuthManager.registerUser()} en toda la Activity.
     * Solo se ejecuta al presionar Finalizar en el paso 3.
     */
    private void attemptRegister() {
        // Re-sincroniza por si hubo recreate() estando en el paso 3.
        syncTempsFromUi();

        if (tempRegion.isEmpty() || tempCity.isEmpty()
                || tempUsername.isEmpty() || tempPassword.isEmpty()) {
            showStepError(binding.step3.tvMessageStep3,
                    getString(R.string.aRegister_error_empty_field));
            return;
        }

        if (tempUsername.equalsIgnoreCase("admin")) {
            showStepError(binding.step3.tvMessageStep3,
                    getString(R.string.aRegister_error_reserved_user));
            return;
        }

        if (SecureAuthManager.userExists(this, tempUsername)) {
            showStepError(binding.step3.tvMessageStep3,
                    getString(R.string.aRegister_error_user_exists));
            return;
        }

        try {
            // *** UNICA escritura persistente del flujo de registro ***
            SecureAuthManager.registerUser(
                    this, tempUsername, tempPassword, tempRegion, tempCity);
            clearTempData();

            Toast.makeText(this,
                    getString(R.string.aRegister_success_toast), Toast.LENGTH_LONG).show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(this, CuentasActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }, 500);

        } catch (IllegalArgumentException e) {
            showStepError(binding.step3.tvMessageStep3, e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado durante el registro", e);
            showStepError(binding.step3.tvMessageStep3,
                    getString(R.string.aRegister_error_internal));
        }
    }

    // ==================== UTILIDADES DE UI ====================

    private static String textOf(android.widget.EditText editText) {
        return editText == null || editText.getText() == null
                ? "" : editText.getText().toString();
    }

    private void showStepError(View textView, String message) {
        ((TextView) textView).setText(message);
        textView.setVisibility(View.VISIBLE);
    }

    private void hideStepError(View textView) {
        textView.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
