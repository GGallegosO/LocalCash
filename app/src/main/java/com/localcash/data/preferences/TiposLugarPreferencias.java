package com.localcash.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia local de los tipos de lugar personalizados (Feature 010,
 * Fase 2.5).
 * <p>
 * Los tipos predefinidos viven en {@code strings.xml}
 * ({@code tipos_lugar_predefinidos}); este helper guarda únicamente los que el
 * usuario agrega desde el diálogo. Se usa {@link SharedPreferences} con un
 * {@link JSONArray} serializado para conservar el orden de creación y evitar una
 * tabla adicional en el esquema financiero. Todo es local: no hay red.
 * </p>
 */
public final class TiposLugarPreferencias {

    /** Etiqueta de logs del helper. */
    private static final String TAG = "TiposLugarPreferencias";

    /** Archivo de preferencias de los tipos personalizados. */
    private static final String PREFS_NAME = "tipos_lugar_prefs";

    /** Clave del arreglo JSON con los tipos personalizados. */
    private static final String KEY_TIPOS = "tipos_personalizados";

    private TiposLugarPreferencias() {
        // Clase utilitaria: no instanciable.
    }

    /**
     * Devuelve los tipos personalizados guardados, en orden de creación.
     *
     * @param context contexto de la aplicación
     * @return lista mutable (posiblemente vacía) de tipos personalizados
     */
    public static List<String> obtenerPersonalizados(Context context) {
        List<String> tipos = new ArrayList<>();
        SharedPreferences prefs = prefs(context);
        String json = prefs.getString(KEY_TIPOS, null);
        if (json == null || json.isEmpty()) {
            return tipos;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String tipo = array.optString(i, "").trim();
                if (!tipo.isEmpty()) {
                    tipos.add(tipo);
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "No se pudo leer el catálogo de tipos personalizados", e);
        }
        return tipos;
    }

    /**
     * Agrega un tipo personalizado si no está vacío ni repetido (sin distinguir
     * mayúsculas).
     *
     * @param context contexto de la aplicación
     * @param tipo    nombre ingresado por el usuario
     * @return {@code true} si se guardó; {@code false} si estaba vacío o repetido
     */
    public static boolean agregarPersonalizado(Context context, String tipo) {
        String limpio = tipo == null ? "" : tipo.trim();
        if (limpio.isEmpty()) {
            return false;
        }
        List<String> tipos = obtenerPersonalizados(context);
        for (String existente : tipos) {
            if (existente.equalsIgnoreCase(limpio)) {
                return false;
            }
        }
        tipos.add(limpio);
        guardar(context, tipos);
        return true;
    }

    /**
     * Elimina un tipo personalizado, si existe.
     *
     * @param context contexto de la aplicación
     * @param tipo    nombre del tipo a eliminar
     */
    public static void eliminarPersonalizado(Context context, String tipo) {
        if (tipo == null) {
            return;
        }
        List<String> tipos = obtenerPersonalizados(context);
        boolean eliminado = false;
        for (int i = tipos.size() - 1; i >= 0; i--) {
            if (tipos.get(i).equalsIgnoreCase(tipo.trim())) {
                tipos.remove(i);
                eliminado = true;
            }
        }
        if (eliminado) {
            guardar(context, tipos);
        }
    }

    /**
     * Indica si un tipo pertenece al catálogo personalizado.
     *
     * @param context contexto de la aplicación
     * @param tipo    nombre a consultar
     * @return {@code true} si fue agregado por el usuario
     */
    public static boolean esPersonalizado(Context context, String tipo) {
        if (tipo == null) {
            return false;
        }
        for (String existente : obtenerPersonalizados(context)) {
            if (existente.equalsIgnoreCase(tipo.trim())) {
                return true;
            }
        }
        return false;
    }

    /** Guarda la lista completa como JSON. */
    private static void guardar(Context context, List<String> tipos) {
        JSONArray array = new JSONArray();
        for (String tipo : tipos) {
            array.put(tipo);
        }
        prefs(context).edit().putString(KEY_TIPOS, array.toString()).apply();
    }

    /** Resuelve las preferencias con el contexto de aplicación. */
    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
