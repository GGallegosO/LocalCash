package com.localcash.data.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Gestor de autenticación segura usando EncryptedSharedPreferences.
 * <p>
 * Esta clase es la única responsable de manejar las credenciales de usuario de forma cifrada.
 * Utiliza Android Keystore para generar/almacenar la MasterKey y
 * EncryptedSharedPreferences para cifrar automáticamente keys y values.
 * </p>
 * <p>
 * Características de seguridad:
 * - Cifrado AES256-GCM para valores
 * - Cifrado AES256-SIV para claves (keys)
 * - MasterKey almacenada en Android Keystore (hardware-backed si disponible)
 * - Integridad verificada en cada lectura
 * </p>
 */
public class SecureAuthManager {

    private static final String TAG = "SecureAuthManager";

    // Nombres de preferencias y claves
    private static final String ENCRYPTED_PREFS_NAME = "secure_auth_prefs";
    private static final String MASTER_KEY_ALIAS = "localcash_master_key";

    // Claves dentro de EncryptedSharedPreferences (seed data admin)
    private static final String KEY_USERNAME = "enc_username";
    private static final String KEY_PASSWORD_HASH = "enc_password_hash";

    // Prefijos para claves dinámicas de usuarios registrados
    private static final String PREFIX_USER_PASSWORD_HASH = "user_";
    private static final String SUFFIX_PASSWORD_HASH = "_password_hash";
    private static final String SUFFIX_REGION = "_region";
    private static final String SUFFIX_CITY = "_city";

    // SharedPreferences normal (no cifrado) solo para flag de inicialización
    private static final String INIT_PREFS_NAME = "auth_init_prefs";
    private static final String KEY_IS_INITIALIZED = "is_initialized";

    // Seed data por defecto
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "123456";

    /**
     * Inicializa el seed data (admin/123456) de forma cifrada en el primer lanzamiento.
     * <p>
     * Verifica si ya existe la flag de inicialización en SharedPreferences normal.
     * Si no existe, crea la MasterKey, abre EncryptedSharedPreferences y guarda
     * el usuario y el hash de la contraseña.
     * </p>
     *
     * @param context Contexto de la aplicación/activity
     * @throws RuntimeException si hay error criptográfico irrecuperable
     */
    public static void initializeSeedData(Context context) {
        // Verificar si ya fue inicializado (usando SharedPreferences normal, no cifrado)
        SharedPreferences initPrefs = context.getSharedPreferences(INIT_PREFS_NAME, Context.MODE_PRIVATE);
        boolean isInitialized = initPrefs.getBoolean(KEY_IS_INITIALIZED, false);

        if (!isInitialized) {
            Log.d(TAG, "Primer lanzamiento: inicializando seed data cifrado...");
            try {
                saveCredentials(context, DEFAULT_USERNAME, DEFAULT_PASSWORD);
                // Marcar como inicializado
                initPrefs.edit().putBoolean(KEY_IS_INITIALIZED, true).apply();
                Log.d(TAG, "Seed data guardado correctamente en EncryptedSharedPreferences");
            } catch (GeneralSecurityException | IOException e) {
                Log.e(TAG, "Error crítico al inicializar seed data: " + e.getMessage(), e);
                throw new RuntimeException("No se pudo inicializar el almacenamiento seguro", e);
            }
        } else {
            Log.d(TAG, "Seed data ya inicializado anteriormente");
        }
    }

    /**
     * Guarda credenciales de forma cifrada.
     * <p>
     * NO guarda la contraseña en texto plano. Calcula un hash SHA-256 con salt
     * y guarda solo el hash. El salt se genera aleatoriamente y se almacena
     * junto al hash (formato: salt:hash en Base64).
     * </p>
     *
     * @param context  Contexto
     * @param username Nombre de usuario
     * @param password Contraseña en texto plano (solo para hashear)
     * @throws GeneralSecurityException Error criptográfico
     * @throws IOException              Error de I/O
     */
    public static void saveCredentials(Context context, String username, String password)
            throws GeneralSecurityException, IOException {

        // Generar salt aleatorio (16 bytes = 128 bits)
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);

        // Calcular hash SHA-256(password + salt)
        String passwordHash = hashPassword(password, salt);

        // Codificar salt + hash en Base64 para almacenamiento
        String saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String storedValue = saltBase64 + ":" + passwordHash;

        // Obtener EncryptedSharedPreferences
        SharedPreferences encryptedPrefs = getEncryptedSharedPreferences(context);

        // Guardar username (cifrado automáticamente) y password hash
        encryptedPrefs.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD_HASH, storedValue)
                .apply();

        Log.d(TAG, "Credenciales guardadas para usuario: " + username);
    }

    /**
     * Valida credenciales comparando con las almacenadas de forma cifrada.
     * <p>
     * Lee el username y el hash almacenado, extrae el salt, recalcula el hash
     * del password proporcionado y compara en tiempo constante para evitar
     * timing attacks.
     * </p>
     *
     * @param context     Contexto
     * @param inputUser   Usuario ingresado
     * @param inputPass   Contraseña ingresada
     * @return true si coinciden, false si no
     */
    public static boolean validateCredentials(Context context, String inputUser, String inputPass) {
        try {
            SharedPreferences encryptedPrefs = getEncryptedSharedPreferences(context);

            String storedUser = encryptedPrefs.getString(KEY_USERNAME, null);
            String storedValue = encryptedPrefs.getString(KEY_PASSWORD_HASH, null);

            if (storedUser == null || storedValue == null) {
                Log.w(TAG, "No hay credenciales almacenadas");
                return false;
            }

            // Verificar username (comparación simple, no sensible a timing)
            if (!storedUser.equals(inputUser)) {
                Log.d(TAG, "Usuario incorrecto");
                return false;
            }

            // Extraer salt y hash almacenado
            String[] parts = storedValue.split(":");
            if (parts.length != 2) {
                Log.e(TAG, "Formato de hash almacenado inválido");
                return false;
            }

            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            String storedHash = parts[1];

            // Calcular hash del password ingresado con el mismo salt
            String computedHash = hashPassword(inputPass, salt);

            // Comparación en tiempo constante para evitar timing attacks
            return MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                    computedHash.getBytes(StandardCharsets.UTF_8));

        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error al validar credenciales: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado validando credenciales: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Registra un nuevo usuario con datos personales completos.
     * <p>
     * Guarda las credenciales cifradas usando claves dinámicas basadas en el nombre de usuario.
     * Las claves dentro de EncryptedSharedPreferences tienen el formato:
     * - `user_<username>_password_hash` para la contraseña
     * - `user_<username>_name` para el nombre
     * - `user_<username>_region` para la región
     * - `user_<username>_city` para la ciudad
     * </p>
     *
     * @param context   Contexto de la aplicación/activity
     * @param username  Nombre de usuario (no puede ser "admin", case-insensitive)
     * @param password  Contraseña en texto plano (se hasheará y cifrará)
     * @param region    Región geográfica
     * @param city      Ciudad
     * @throws GeneralSecurityException Error criptográfico
     * @throws IOException              Error de I/O
     */
    public static void registerUser(Context context, String username, String password,
                                    String region, String city)
            throws GeneralSecurityException, IOException {
        // Bloquear registro del usuario "admin" (case-insensitive)
        if (username.equalsIgnoreCase(DEFAULT_USERNAME)) {
            Log.w(TAG, "Intento de registro del usuario reservado: " + username);
            throw new IllegalArgumentException("El usuario 'admin' está reservado para demostración");
        }

        Log.d(TAG, "Registrando nuevo usuario: " + username);

        // Guardar contraseña con clave dinámica
        saveUserCredentials(context, username, password);

        // Guardar datos personales con claves dinámicas
        saveUserField(context, username, region, SUFFIX_REGION);
        saveUserField(context, username, city, SUFFIX_CITY);

        Log.d(TAG, "Usuario registrado exitosamente: " + username);
    }

    /**
     * Valida credenciales de un usuario registrado (no el seed "admin").
     *
     * @param context Contexto
     * @param username Usuario ingresado
     * @param password Contraseña ingresada
     * @return true si las credenciales son válidas, false en otro caso
     */
    public static boolean validateUser(Context context, String username, String password) {
        try {
            SharedPreferences encryptedPrefs = getEncryptedSharedPreferences(context);

            // Usar la clave dinámica: user_<username>_password_hash
            String key = PREFIX_USER_PASSWORD_HASH + username + SUFFIX_PASSWORD_HASH;
            String storedValue = encryptedPrefs.getString(key, null);

            if (storedValue == null) {
                Log.d(TAG, "No hay credenciales registradas para el usuario: " + username);
                return false;
            }

            // Extraer salt y hash almacenado
            String[] parts = storedValue.split(":");
            if (parts.length != 2) {
                Log.e(TAG, "Formato de hash almacenado inválido para usuario: " + username);
                return false;
            }

            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            String storedHash = parts[1];

            // Calcular hash del password ingresado con el mismo salt
            String computedHash = hashPassword(password, salt);

            // Comparación en tiempo constante para evitar timing attacks
            boolean result = MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                    computedHash.getBytes(StandardCharsets.UTF_8));

            if (result) {
                Log.d(TAG, "Validación exitosa para usuario registrado: " + username);
            } else {
                Log.d(TAG, "Validación fallida para usuario registrado: " + username + " - hash no coincide");
            }

            return result;

        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error al validar usuario registrado: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error inesperado validando usuario registrado: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verifica si un usuario ya existe en el almacenamiento.
     *
     * @param context Contexto
     * @param username Nombre de usuario a verificar
     * @return true si el usuario existe, false en otro caso
     */
    public static boolean userExists(Context context, String username) {
        try {
            SharedPreferences encryptedPrefs = getEncryptedSharedPreferences(context);

            // Verificar la clave dinámica: user_<username>_password_hash
            String key = PREFIX_USER_PASSWORD_HASH + username + SUFFIX_PASSWORD_HASH;
            return encryptedPrefs.contains(key);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error al verificar existencia de usuario: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Guarda credenciales de usuario usando una clave dinámica.
     *
     * @param context Contexto
     * @param username Nombre de usuario
     * @param password Contraseña en texto plano (se hasheará)
     * @throws GeneralSecurityException Error criptográfico
     * @throws IOException              Error de I/O
     */
    private static void saveUserCredentials(Context context, String username, String password)
            throws GeneralSecurityException, IOException {
        // Generar salt aleatorio (16 bytes = 128 bits)
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);

        // Calcular hash SHA-256(password + salt)
        String passwordHash = hashPassword(password, salt);

        // Codificar salt + hash en Base64 para almacenamiento
        String saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String storedValue = saltBase64 + ":" + passwordHash;

        // Obtener EncryptedSharedPreferences
        SharedPreferences encryptedPrefs = getEncryptedSharedPreferences(context);

        // Guardar usando clave dinámica: user_<username>_password_hash
        String key = PREFIX_USER_PASSWORD_HASH + username + SUFFIX_PASSWORD_HASH;
        encryptedPrefs.edit().putString(key, storedValue).apply();

        Log.d(TAG, "Credenciales guardadas para usuario dinámico: " + username);
    }

    /**
     * Guarda un campo de usuario (nombre, región, ciudad) usando clave dinámica.
     *
     * @param context Contexto
     * @param username Nombre de usuario
     * @param value    Valor a guardar
     * @param suffix   Sufijo de la clave (SUFFIX_REGION, SUFFIX_CITY)
     * @throws GeneralSecurityException Error criptográfico
     * @throws IOException              Error de I/O
     */
    private static void saveUserField(Context context, String username, String value, String suffix)
            throws GeneralSecurityException, IOException {
        // Obtener EncryptedSharedPreferences
        SharedPreferences encryptedPrefs = getEncryptedSharedPreferences(context);

        // Guardar usando clave dinámica: user_<username>_<suffix>
        String key = PREFIX_USER_PASSWORD_HASH + username + suffix;
        encryptedPrefs.edit().putString(key, value).apply();

        Log.d(TAG, "Campo de usuario guardado: " + key + " = " + value);
    }

    /**
     * Método existente que mantiene compatibilidad con el seed "admin/123456".
     * Sobrescribe todas las credenciales (seed).
     *
     * @param context   Contexto
     * @param username  Nombre de usuario
     * @param password  Contraseña
     * @throws GeneralSecurityException Error criptográfico
     * @throws IOException              Error de I/O
     */
    /**
     * Registra un nuevo usuario (método legacy de 3 parámetros).
     * <p>
     * Sobrescribe las credenciales existentes. Este método bloquea el registro
     * del usuario "admin" (case-insensitive) y delega al método de 5 parámetros.
     * </p>
     *
     * @param context   Contexto
     * @param newUser   Nuevo nombre de usuario
     * @param newPass   Nueva contraseña
     * @throws GeneralSecurityException Error criptográfico
     * @throws IOException              Error de I/O
     */
    public static void registerUser(Context context, String newUser, String newPass)
            throws GeneralSecurityException, IOException {
        // Bloquear registro del usuario "admin" (case-insensitive)
        if (newUser.equalsIgnoreCase(DEFAULT_USERNAME)) {
            Log.w(TAG, "Intento de registro del usuario reservado: " + newUser);
            throw new IllegalArgumentException("El usuario 'admin' está reservado para demostración");
        }

        // Delegar al método de 5 parámetros con datos por defecto
        try {
            registerUser(context, newUser, newPass, "", "");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El usuario 'admin' está reservado para demostración");
        }
    }

    /**
     * Obtiene el nombre de usuario almacenado (para mostrar en UI, no para auth).
     *
     * @param context Contexto
     * @return Username almacenado o null si no existe
     */
    public static String getStoredUsername(Context context) {
        try {
            SharedPreferences encryptedPrefs = getEncryptedSharedPreferences(context);
            return encryptedPrefs.getString(KEY_USERNAME, null);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error al obtener username: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Verifica si ya existen credenciales configuradas.
     *
     * @param context Contexto
     * @return true si hay credenciales guardadas
     */
    public static boolean hasCredentials(Context context) {
        try {
            SharedPreferences encryptedPrefs = getEncryptedSharedPreferences(context);
            return encryptedPrefs.contains(KEY_USERNAME) && encryptedPrefs.contains(KEY_PASSWORD_HASH);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error al verificar credenciales: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Elimina todas las credenciales (para logout total o reset).
     *
     * @param context Contexto
     */
    public static void clearCredentials(Context context) {
        try {
            SharedPreferences encryptedPrefs = getEncryptedSharedPreferences(context);
            encryptedPrefs.edit().clear().apply();

            // Resetear flag de inicialización
            SharedPreferences initPrefs = context.getSharedPreferences(INIT_PREFS_NAME, Context.MODE_PRIVATE);
            initPrefs.edit().putBoolean(KEY_IS_INITIALIZED, false).apply();

            Log.d(TAG, "Credenciales eliminadas completamente");
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error al limpiar credenciales: " + e.getMessage(), e);
        }
    }

    // ==================== MÉTODOS PRIVADOS AUXILIARES ====================

    /**
     * Obtiene una instancia de EncryptedSharedPreferences.
     * Crea la MasterKey en Android Keystore si no existe.
     */
    private static SharedPreferences getEncryptedSharedPreferences(Context context)
            throws GeneralSecurityException, IOException {

        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    /**
     * Calcula hash SHA-256(password + salt).
     *
     * @param password Contraseña en texto plano
     * @param salt     Salt aleatorio
     * @return Hash en hexadecimal (64 caracteres)
     */
    private static String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // password + salt
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            digest.update(salt);
            byte[] hash = digest.digest();

            // Convertir a hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre existe en Android
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }
}