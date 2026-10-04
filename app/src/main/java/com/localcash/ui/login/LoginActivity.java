package com.localcash.ui.login;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.localcash.R;
import com.localcash.databinding.ActivityLoginBinding;
import com.localcash.ui.BaseActivity;
import com.localcash.ui.theme.ThemeManager;

/**
 * Pantalla de inicio de sesión de LocalCash.
 * Extiende BaseActivity para aplicar automáticamente el tema guardado.
 * Incluye toggle de tema manual (claro/oscuro) con íconos nativos de Material Design.
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private ImageButton btnThemeToggle;

    private static final String SEED_USER = "admin";
    private static final String SEED_PASSWORD = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ThemeManager.applyTheme(this) se llama automáticamente en BaseActivity.onCreate()
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Referencia al botón de cambio de tema
        btnThemeToggle = binding.btnThemeToggle;

        // Configurar ícono inicial del botón según tema actual
        updateThemeToggleIcon();

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Botón de login
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        // Botón de cambio de tema
        btnThemeToggle.setOnClickListener(v -> toggleTheme());
    }

    /**
     * Alterna entre tema claro y oscuro, guarda la preferencia y recrea la Activity.
     */
    private void toggleTheme() {
        boolean isCurrentlyDark = ThemeManager.isDarkMode(this);
        boolean newDarkMode = !isCurrentlyDark;

        ThemeManager.setDarkMode(this, newDarkMode);
        updateThemeToggleIcon();

        // Recrear la Activity para aplicar el nuevo tema visualmente
        recreate();
    }

    /**
     * Actualiza el ícono del botón de tema según el modo actual usando íconos nativos de Material Design.
     * - Tema claro: ic_brightness_medium (sol)
     * - Tema oscuro: ic_brightness_low (luna)
     */
    private void updateThemeToggleIcon() {
        boolean isDark = ThemeManager.isDarkMode(this);
        int iconRes = isDark ? R.drawable.ic_brightness_low : R.drawable.ic_brightness_medium;
        btnThemeToggle.setImageResource(iconRes);
        btnThemeToggle.setContentDescription(getString(R.string.aLogin_theme_toggle_desc));
    }

    private void attemptLogin() {
        try {
            String usuario = binding.etUsuario.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            Log.d(TAG, "Intento de login - usuario: " + usuario);

            if (usuario.isEmpty() || password.isEmpty()) {
                showMessage(getString(R.string.aLogin_error_empty));
                return;
            }

            if (SEED_USER.equals(usuario) && SEED_PASSWORD.equals(password)) {
                Log.d(TAG, "Login exitoso");
                binding.tvMessage.setText(getString(R.string.aLogin_success_welcome));
                binding.tvMessage.setTextColor(getColor(android.R.color.holo_green_dark));
                binding.tvMessage.setVisibility(View.VISIBLE);
                Toast.makeText(this, getString(R.string.aLogin_success_welcome), Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 500);
            } else {
                Log.d(TAG, "Login fallido");
                showMessage(getString(R.string.aLogin_error_invalid));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage(), e);
            Toast.makeText(this, "Error interno. Intente nuevamente", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMessage(String message) {
        binding.tvMessage.setText(message);
        binding.tvMessage.setTextColor(getColor(android.R.color.holo_red_dark));
        binding.tvMessage.setVisibility(View.VISIBLE);
    }
}