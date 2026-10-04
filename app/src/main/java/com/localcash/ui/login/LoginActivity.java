package com.localcash.ui.login;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.templocalcash.R;
import com.example.templocalcash.databinding.ActivityLoginBinding;

/**
 * Pantalla de inicio de sesión de LocalCash con diseño moderno.
 * Validación hardcodeada (usuario: admin, contraseña: 123456).
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;

    private static final String SEED_USER = "admin";
    private static final String SEED_PASSWORD = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        try {
            String usuario = binding.etUsuario.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            Log.d(TAG, "Intento de login - usuario: " + usuario);

            if (usuario.isEmpty() || password.isEmpty()) {
                showMessage("Por favor ingrese usuario y contraseña");
                return;
            }

            if (SEED_USER.equals(usuario) && SEED_PASSWORD.equals(password)) {
                Log.d(TAG, "Login exitoso");
                binding.tvMessage.setText("¡Bienvenido admin!");
                binding.tvMessage.setTextColor(getColor(android.R.color.holo_green_dark));
                binding.tvMessage.setVisibility(View.VISIBLE);
                Toast.makeText(this, "¡Bienvenido admin!", Toast.LENGTH_SHORT).show();
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 500);
            } else {
                Log.d(TAG, "Login fallido");
                showMessage("Usuario o contraseña incorrectos");
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
