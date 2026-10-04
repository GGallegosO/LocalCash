package com.localcash.ui.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Gestor de temas para LocalCash.
 * Permite cambio manual de tema (claro/oscuro/sistema) persistente entre sesiones.
 * Debe llamarse ANTES de setContentView() en cada Activity.
 */
public class ThemeManager {

    private static final String TAG = "ThemeManager";
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "pref_theme_mode";

    // Modos de tema
    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";
    public static final String MODE_SYSTEM = "system";

    /**
     * Aplica el tema guardado en preferencias.
     * DEBE llamarse ANTES de setContentView() en onCreate() de la Activity.
     *
     * @param context Contexto de la aplicación/activity
     */
    public static void applyTheme(Context context) {
        String savedMode = getSavedThemeMode(context);
        int nightMode = mapThemeModeToAppCompat(savedMode);

        AppCompatDelegate.setDefaultNightMode(nightMode);
        Log.d(TAG, "Tema aplicado: " + savedMode + " (AppCompat: " + nightMode + ")");
    }

    /**
     * Cambia el modo de tema y lo persiste.
     * Recrea la Activity actual para aplicar el cambio visual.
     *
     * @param context  Contexto de la aplicación/activity
     * @param isDark   true = modo oscuro, false = modo claro
     */
    public static void setDarkMode(Context context, boolean isDark) {
        String newMode = isDark ? MODE_DARK : MODE_LIGHT;
        saveThemeMode(context, newMode);

        int nightMode = isDark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;

        AppCompatDelegate.setDefaultNightMode(nightMode);
        Log.d(TAG, "Tema cambiado a: " + newMode);
    }

    /**
     * Cambia el modo de tema con opción "system".
     *
     * @param context Contexto
     * @param mode    Modo: "light", "dark" o "system"
     */
    public static void setThemeMode(Context context, String mode) {
        if (!MODE_LIGHT.equals(mode) && !MODE_DARK.equals(mode) && !MODE_SYSTEM.equals(mode)) {
            Log.w(TAG, "Modo de tema inválido: " + mode + ", usando 'light'");
            mode = MODE_LIGHT;
        }
        saveThemeMode(context, mode);
        AppCompatDelegate.setDefaultNightMode(mapThemeModeToAppCompat(mode));
        Log.d(TAG, "Tema establecido a: " + mode);
    }

    /**
     * Verifica si el modo actual es oscuro.
     *
     * @param context Contexto
     * @return true si el tema actual es oscuro
     */
    public static boolean isDarkMode(Context context) {
        String savedMode = getSavedThemeMode(context);

        // Si está en modo "system", consultar la configuración del sistema
        if (MODE_SYSTEM.equals(savedMode)) {
            int currentNightMode = AppCompatDelegate.getDefaultNightMode();
            return currentNightMode == AppCompatDelegate.MODE_NIGHT_YES;
        }

        return MODE_DARK.equals(savedMode);
    }

    /**
     * Obtiene el modo de tema guardado (por defecto: system).
     */
    private static String getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME_MODE, MODE_SYSTEM);
    }

    /**
     * Guarda el modo de tema en SharedPreferences.
     */
    private static void saveThemeMode(Context context, String mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME_MODE, mode).apply();
    }

    /**
     * Convierte el modo de tema local a constante de AppCompatDelegate.
     */
    private static int mapThemeModeToAppCompat(String mode) {
        switch (mode) {
            case MODE_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case MODE_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case MODE_SYSTEM:
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }
}