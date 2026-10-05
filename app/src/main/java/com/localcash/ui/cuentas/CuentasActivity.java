package com.localcash.ui.cuentas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;

import com.localcash.R;
import com.localcash.databinding.ActivityCuentasBinding;
import com.localcash.ui.BaseActivity;
import com.localcash.ui.theme.ThemeManager;

/**
 * Vista dummy "Módulo 1: Cuentas y Movimientos".
 * Destino de navegación tras login exitoso.
 * Permite cerrar sesión y volver a LoginActivity.
 */
public class CuentasActivity extends BaseActivity {

    private static final String TAG = "CuentasActivity";

    private ActivityCuentasBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ThemeManager.applyTheme(this) se llama automáticamente en BaseActivity.onCreate()
        super.onCreate(savedInstanceState);

        binding = ActivityCuentasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    /**
     * Configura los listeners de los botones de la UI.
     */
    private void setupClickListeners() {
        // Botón "Cerrar sesión": termina esta Activity para volver al Login
        binding.btnLogout.setOnClickListener(v -> {
            // No se llama a finish() aquí porque el flujo natural al cerrar sesión
            // es simplemente terminar esta Activity; LoginActivity ya hizo finish()
            // al navegar aquí, así que no hay pila de retorno.
            // Si se quiere forzar volver a Login como nueva instancia:
            Intent intent = new Intent(this, com.localcash.ui.login.LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Botón de cambio de tema (reutiliza lógica de LoginActivity)
        binding.btnThemeToggle.setOnClickListener(v -> toggleTheme());
    }

    /**
     * Alterna entre tema claro y oscuro, guarda la preferencia y recrea la Activity.
     */
    private void toggleTheme() {
        boolean isCurrentlyDark = ThemeManager.isDarkMode(this);
        boolean newDarkMode = !isCurrentlyDark;

        ThemeManager.setDarkMode(this, newDarkMode);

        // Recrear la Activity para aplicar el nuevo tema visualmente
        recreate();
    }
}