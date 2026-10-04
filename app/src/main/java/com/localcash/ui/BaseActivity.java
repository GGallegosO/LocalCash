package com.localcash.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.localcash.ui.theme.ThemeManager;

/**
 * Activity base para LocalCash.
 * Aplica automáticamente el tema guardado antes de inflar la vista.
 * Todas las Activities del proyecto deben extender esta clase.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // IMPORTANTE: Aplicar tema ANTES de llamar a super.onCreate()
        // para que el tema se aplique correctamente antes de inflar las vistas.
        ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);
    }
}