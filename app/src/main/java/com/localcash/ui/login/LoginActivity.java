package com.localcash.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.localcash.R;
import com.localcash.data.security.SecureAuthManager;
import com.localcash.databinding.ActivityLoginBinding;
import com.localcash.ui.BaseActivity;
import com.localcash.ui.cuentas.CuentasActivity;
import com.localcash.ui.theme.ThemeManager;

import java.util.concurrent.Executor;

/**
 * Pantalla de inicio de sesión de LocalCash.
 * Extiende BaseActivity para aplicar automáticamente el tema guardado.
 * Incluye toggle de tema manual (claro/oscuro) con ícono nativo Material Design.
 * Implementa bloqueo temporal tras 3 intentos fallidos consecutivos (10 segundos).
 * El bloqueo persiste en SharedPreferences para sobrevivir a rotación de pantalla.
 * Implementa autenticación biométrica (Huella/Rostro) con fallback a PIN.
 * Valida credenciales contra EncryptedSharedPreferences (SecureAuthManager).
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";

    // Constantes para bloqueo temporal
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 10_000L; // 10 segundos
    private static final long COUNTDOWN_INTERVAL_MS = 1_000L; // 1 segundo

    // Claves para SharedPreferences (persistencia del bloqueo)
    private static final String PREFS_NAME = "login_security_prefs";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_END_TIMESTAMP = "lockout_end_timestamp";

    private ActivityLoginBinding binding;
    private CountDownTimer lockoutCountDownTimer;
    private int failedAttempts = 0;
    private long lockoutEndTimestamp = 0L;

    // BiometricPrompt components
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private Executor executor;

    // Handler para garantizar ejecución en hilo principal
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ThemeManager.applyTheme(this) se llama automáticamente en BaseActivity.onCreate()
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializar seed data cifrado (admin/123456) en primer lanzamiento
        SecureAuthManager.initializeSeedData(this);

        // Inicializar componentes de biometría
        initBiometricPrompt();

        // Restaurar estado de bloqueo desde SharedPreferences
        restoreLockoutState();

        // Verificar si hay un bloqueo activo al iniciar
        if (isLockoutActive()) {
            applyLockoutUI();
            startLockoutCountdown(getRemainingLockoutTime());
        } else {
            // No hay bloqueo activo: asegurar UI habilitada
            enableLoginUI();
        }

        setupClickListeners();
        setupBiometricButtonVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver a la Activity (ej. tras rotación o cambio de tema),
        // verificar estado de bloqueo y actualizar UI del botón biométrico.
        if (isLockoutActive()) {
            applyLockoutUI();
            startLockoutCountdown(getRemainingLockoutTime());
        } else {
            // Si no hay bloqueo, asegurar que el botón biométrico refleje disponibilidad actual
            setupBiometricButtonVisibility();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancelar timer si la Activity se destruye para evitar leaks
        if (lockoutCountDownTimer != null) {
            lockoutCountDownTimer.cancel();
            lockoutCountDownTimer = null;
        }
    }

    /**
     * Inicializa el BiometricPrompt y su configuración.
     * Se ejecuta una sola vez en onCreate.
     */
    private void initBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this);

        // Callback de autenticación biométrica
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                // Autenticación biométrica exitosa
                Log.d(TAG, "Biometric authentication succeeded");
                // Asegurar ejecución en hilo principal para navegación
                mainHandler.post(() -> handleSuccessfulLogin());
            }

            @Override
            public void onAuthenticationFailed() {
                // Autenticación biométrica fallida (huella/rostro no reconocido)
                // Este callback SÍ se ejecuta en hilo principal gracias al executor
                Log.d(TAG, "Biometric authentication failed");
                handleFailedBiometricAttempt();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                // Error del sistema biométrico (ej. sensor no disponible, usuario canceló, etc.)
                Log.d(TAG, "Biometric authentication error: " + errorCode + " - " + errString);
                // No contar como intento fallido si el usuario canceló explícitamente
                // ERROR_CANCELED (10) = usuario canceló (back gesture, tap outside)
                // ERROR_NEGATIVE_BUTTON = usuario presionó "Usar PIN"
                // ERROR_LOCKOUT / ERROR_LOCKOUT_PERMANENT = sistema bloqueó por muchos intentos
                if (errorCode != BiometricPrompt.ERROR_CANCELED
                        && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        && errorCode != BiometricPrompt.ERROR_LOCKOUT
                        && errorCode != BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    showMessage(getString(R.string.aLogin_biometric_error_generic) + ": " + errString);
                }
                // Si es ERROR_LOCKOUT del sistema, también activar nuestro bloqueo local
                if (errorCode == BiometricPrompt.ERROR_LOCKOUT
                        || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    mainHandler.post(() -> {
                        if (!isLockoutActive()) {
                            // Forzar bloqueo local si el sistema detectó demasiados intentos
                            failedAttempts = MAX_FAILED_ATTEMPTS;
                            saveFailedAttempts();
                            initiateLockout();
                        }
                    });
                }
                // Si presionó "Usar PIN" (NEGATIVE_BUTTON), no hacer nada - la UI del PIN ya está visible
            }
        });

        // Configuración del diálogo biométrico
        // BIOMETRIC_STRONG = autenticadores biométricos fuertes (huella, rostro, iris)
        // Excluye PIN/patrón/contraseña del dispositivo
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.aLogin_biometric_title))
                .setSubtitle(getString(R.string.aLogin_biometric_subtitle))
                .setNegativeButtonText(getString(R.string.aLogin_biometric_negative_button)) // "Usar PIN"
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
    }

    /**
     * Configura la visibilidad del botón biométrico según disponibilidad de hardware.
     * Solo se muestra si el dispositivo soporta biometría y hay al menos una biometría registrada.
     */
    private void setupBiometricButtonVisibility() {
        BiometricManager biometricManager = BiometricManager.from(this);
        // Verificar si hay autenticadores biométricos fuertes disponibles y registrados
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        // BIOMETRIC_SUCCESS (0) = hardware disponible y biometría registrada
        // BIOMETRIC_ERROR_NONE_ENROLLED (11) = hardware disponible pero sin biometría registrada
        // BIOMETRIC_ERROR_HW_UNAVAILABLE (12) = sin hardware biométrico
        boolean isAvailable = (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS);

        mainHandler.post(() -> {
            if (isAvailable) {
                binding.btnBiometric.setVisibility(View.VISIBLE);
                binding.btnBiometric.setEnabled(true);
                Log.d(TAG, "Biometric authentication available");
            } else {
                binding.btnBiometric.setVisibility(View.GONE);
                binding.btnBiometric.setEnabled(false);
                Log.d(TAG, "Biometric authentication not available: " + canAuthenticate);
            }
        });
    }

    /**
     * Configura los listeners de los botones de la UI.
     */
    private void setupClickListeners() {
        // Botón de login tradicional (PIN/contraseña)
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        // Botón de autenticación biométrica
        binding.btnBiometric.setOnClickListener(v -> {
            if (!isLockoutActive()) {
                showBiometricPrompt();
            }
        });

        // Botón de cambio de tema
        binding.btnThemeToggle.setOnClickListener(v -> toggleTheme());
    }

    /**
     * Muestra el diálogo de autenticación biométrica.
     * Solo se llama si no hay bloqueo activo.
     */
    private void showBiometricPrompt() {
        if (biometricPrompt != null && promptInfo != null) {
            biometricPrompt.authenticate(promptInfo);
        }
    }

    /**
     * Alterna entre tema claro y oscuro, guarda la preferencia y recrea la Activity.
     * El ícono ic_brightness_medium ya muestra sol y luna combinados, no requiere cambio dinámico.
     */
    private void toggleTheme() {
        boolean isCurrentlyDark = ThemeManager.isDarkMode(this);
        boolean newDarkMode = !isCurrentlyDark;

        ThemeManager.setDarkMode(this, newDarkMode);

        // Recrear la Activity para aplicar el nuevo tema visualmente
        recreate();
    }

    /**
     * Intenta iniciar sesión validando credenciales contra EncryptedSharedPreferences.
     * Maneja contador de intentos fallidos y bloqueo temporal.
     */
    private void attemptLogin() {
        // Si hay bloqueo activo, no procesar el intento
        if (isLockoutActive()) {
            return;
        }

        try {
            String usuario = binding.etUsuario.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            Log.d(TAG, "Intento de login - usuario: " + usuario);

            // Validación de campos vacíos
            if (usuario.isEmpty() || password.isEmpty()) {
                showMessage(getString(R.string.aLogin_error_empty));
                return;
            }

            // Validación de credenciales usando SecureAuthManager (EncryptedSharedPreferences)
            if (SecureAuthManager.validateCredentials(this, usuario, password)) {
                Log.d(TAG, "Login exitoso");
                handleSuccessfulLogin();
            } else {
                Log.d(TAG, "Login fallido");
                handleFailedLogin();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage(), e);
            Toast.makeText(this, "Error interno. Intente nuevamente", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Maneja login exitoso (tanto PIN como biometría):
     * - Resetea contador de intentos fallidos
     * - Muestra Toast de bienvenida
     * - Navega a CuentasActivity
     * - Finaliza LoginActivity (no retorno con botón atrás)
     */
    private void handleSuccessfulLogin() {
        // Resetear contador de intentos fallidos
        resetFailedAttempts();

        // Mostrar mensaje de éxito en UI
        binding.tvMessage.setText(getString(R.string.aLogin_success_welcome));
        binding.tvMessage.setTextColor(getColor(android.R.color.holo_green_dark));
        binding.tvMessage.setVisibility(View.VISIBLE);

        // Toast de bienvenida
        Toast.makeText(this, getString(R.string.aLogin_success_welcome), Toast.LENGTH_SHORT).show();

        // Navegar a CuentasActivity
        Intent intent = new Intent(this, CuentasActivity.class);
        // Flags: limpiar pila y crear nueva tarea para evitar volver a Login con botón atrás
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // Finalizar LoginActivity para que no se pueda volver atrás
        finish();
    }

    /**
     * Maneja login fallido con PIN/contraseña:
     * - Incrementa contador de intentos
     * - Si llega a 3, inicia bloqueo de 10 segundos
     * - Muestra mensaje de error
     */
    private void handleFailedLogin() {
        failedAttempts++;
        saveFailedAttempts();

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            // Iniciar bloqueo de 10 segundos
            initiateLockout();
        } else {
            // Mostrar error normal, intentos restantes
            int remaining = MAX_FAILED_ATTEMPTS - failedAttempts;
            String errorMsg = getString(R.string.aLogin_error_invalid) + " (" + remaining + " intentos restantes)";
            showMessage(errorMsg);
        }
    }

    /**
     * Maneja intento fallido de autenticación biométrica.
     * Reutiliza la misma lógica de contador y bloqueo que el PIN.
     * Se ejecuta en hilo principal (gracias al executor del BiometricPrompt).
     */
    private void handleFailedBiometricAttempt() {
        failedAttempts++;
        saveFailedAttempts();

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            // Iniciar bloqueo de 10 segundos
            Log.d(TAG, "3er intento fallido biométrico - iniciando bloqueo");
            initiateLockout();
        } else {
            // Mostrar error biométrico con intentos restantes
            int remaining = MAX_FAILED_ATTEMPTS - failedAttempts;
            String errorMsg = getString(R.string.aLogin_biometric_error_failed) + " (" + remaining + " intentos restantes)";
            showMessage(errorMsg);
        }
    }

    /**
     * Inicia el bloqueo temporal de 10 segundos:
     * - Guarda timestamp de fin de bloqueo
     * - Deshabilita UI (botones y campos)
     * - Inicia CountDownTimer visible
     */
    private void initiateLockout() {
        lockoutEndTimestamp = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
        saveLockoutEndTimestamp();

        // Asegurar ejecución en hilo principal para cambios de UI
        mainHandler.post(() -> {
            applyLockoutUI();
            startLockoutCountdown(LOCKOUT_DURATION_MS);
        });
    }

    /**
     * Aplica estado visual de bloqueo: deshabilita botones y campos, muestra mensaje.
     * Debe llamarse desde hilo principal.
     */
    private void applyLockoutUI() {
        binding.btnLogin.setEnabled(false);
        binding.btnBiometric.setEnabled(false);
        binding.etUsuario.setEnabled(false);
        binding.etPassword.setEnabled(false);
        binding.btnLogin.setAlpha(0.5f);
        binding.btnBiometric.setAlpha(0.5f);

        long remainingSeconds = getRemainingLockoutTime() / 1000;
        String lockoutMsg = getString(R.string.aLogin_error_locked, (int) remainingSeconds);
        binding.tvMessage.setText(lockoutMsg);
        binding.tvMessage.setTextColor(getColor(android.R.color.holo_red_dark));
        binding.tvMessage.setVisibility(View.VISIBLE);
    }

    /**
     * Habilita la UI de login (botones, campos, limpia mensaje).
     * Debe llamarse desde hilo principal.
     */
    private void enableLoginUI() {
        binding.btnLogin.setEnabled(true);
        binding.etUsuario.setEnabled(true);
        binding.etPassword.setEnabled(true);
        binding.btnLogin.setAlpha(1.0f);

        // El botón biométrico se habilita solo si el hardware está disponible
        setupBiometricButtonVisibility();
        if (binding.btnBiometric.getVisibility() == View.VISIBLE) {
            binding.btnBiometric.setAlpha(1.0f);
        }

        binding.tvMessage.setVisibility(View.GONE);
        binding.etUsuario.setText("");
        binding.etPassword.setText("");
    }

    /**
     * Inicia CountDownTimer para actualizar mensaje cada segundo durante el bloqueo.
     *
     * @param remainingMs Tiempo restante en milisegundos
     */
    private void startLockoutCountdown(long remainingMs) {
        // Cancelar timer anterior si existe
        if (lockoutCountDownTimer != null) {
            lockoutCountDownTimer.cancel();
        }

        lockoutCountDownTimer = new CountDownTimer(remainingMs, COUNTDOWN_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                String countdownMsg = getString(R.string.aLogin_error_locked_short, (int) secondsLeft);
                binding.tvMessage.setText(countdownMsg);
            }

            @Override
            public void onFinish() {
                // Bloqueo terminado: resetear estado y habilitar UI
                mainHandler.post(() -> onLockoutFinished());
            }
        }.start();
    }

    /**
     * Se llama cuando termina el countdown del bloqueo.
     * Resetea contador de intentos, limpia timestamp y habilita UI.
     */
    private void onLockoutFinished() {
        resetFailedAttempts();
        clearLockoutEndTimestamp();
        enableLoginUI();
    }

    /**
     * Verifica si hay un bloqueo activo comparando timestamp actual con fin de bloqueo.
     *
     * @return true si el bloqueo está activo
     */
    private boolean isLockoutActive() {
        return lockoutEndTimestamp > 0 && System.currentTimeMillis() < lockoutEndTimestamp;
    }

    /**
     * Obtiene el tiempo restante de bloqueo en milisegundos.
     *
     * @return Milisegundos restantes (0 si no hay bloqueo activo)
     */
    private long getRemainingLockoutTime() {
        if (!isLockoutActive()) {
            return 0L;
        }
        return lockoutEndTimestamp - System.currentTimeMillis();
    }

    // ==================== PERSISTENCIA EN SHAREDPREFERENCES ====================

    /**
     * Restaura el estado de bloqueo desde SharedPreferences al crear la Activity.
     * Permite que el bloqueo sobreviva a rotación de pantalla y cambios de tema.
     */
    private void restoreLockoutState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0);
        lockoutEndTimestamp = prefs.getLong(KEY_LOCKOUT_END_TIMESTAMP, 0L);
        Log.d(TAG, "Estado restaurado: attempts=" + failedAttempts + ", lockoutEnd=" + lockoutEndTimestamp);
    }

    /**
     * Guarda el contador de intentos fallidos en SharedPreferences.
     */
    private void saveFailedAttempts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failedAttempts).apply();
    }

    /**
     * Resetea el contador de intentos fallidos a 0 y lo persiste.
     */
    private void resetFailedAttempts() {
        failedAttempts = 0;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply();
    }

    /**
     * Guarda el timestamp de fin de bloqueo en SharedPreferences.
     */
    private void saveLockoutEndTimestamp() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putLong(KEY_LOCKOUT_END_TIMESTAMP, lockoutEndTimestamp).apply();
    }

    /**
     * Limpia el timestamp de fin de bloqueo (establece a 0).
     */
    private void clearLockoutEndTimestamp() {
        lockoutEndTimestamp = 0L;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putLong(KEY_LOCKOUT_END_TIMESTAMP, 0L).apply();
    }

    /**
     * Muestra un mensaje de error en el TextView tvMessage.
     *
     * @param message Mensaje a mostrar
     */
    private void showMessage(String message) {
        mainHandler.post(() -> {
            binding.tvMessage.setText(message);
            binding.tvMessage.setTextColor(getColor(android.R.color.holo_red_dark));
            binding.tvMessage.setVisibility(View.VISIBLE);
        });
    }
}