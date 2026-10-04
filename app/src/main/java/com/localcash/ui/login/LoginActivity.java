package com.localcash.ui.login;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.templocalcash.R;
import com.example.templocalcash.databinding.ActivityLoginBinding;

/**
 * Pantalla de inicio de sesión de LocalCash.
 * Implementación básica con validación hardcodeada (usuario: admin, contraseña: 123456).
 * En futuras iteraciones se agregará biometría y PIN.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    // Seed data hardcodeado para pruebas rápidas
    private static final String SEED_USER = "admin";
    private static final String SEED_PASSWORD = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String inputPassword = binding.etPassword.getText().toString().trim();

        if (inputPassword.isEmpty()) {
            binding.tvMessage.setText("Por favor ingrese su contraseña");
            binding.tvMessage.setTextColor(getColor(R.color.error));
            return;
        }

        if (SEED_PASSWORD.equals(inputPassword)) {
            binding.tvMessage.setText("¡Bienvenido " + SEED_USER + "!");
            binding.tvMessage.setTextColor(getColor(R.color.success));
            finish();
        } else {
            binding.tvMessage.setText("Contraseña incorrecta. Intente nuevamente.");
            binding.tvMessage.setTextColor(getColor(R.color.error));
        }
    }
}
