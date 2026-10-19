package com.localcash.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test de la migración de esquema 2 → 3 (Feature 009, T15) en JVM con Robolectric.
 * <p>
 * Usa {@link MigrationTestHelper} de Room, que siembra la base en la versión 2,
 * ejecuta {@link AppDatabase#MIGRATION_2_3} y valida el esquema resultante contra
 * el exportado ({@code app/schemas/.../3.json}, expuesto a los assets del test vía
 * {@code sourceSets.test.assets} en {@code app/build.gradle}).
 * </p>
 * <p>
 * Comprueba que la migración es <b>no destructiva</b>: las columnas geográficas
 * se agregan, el índice {@code idx_transacciones_geo} se crea sobre
 * ({@code latitud}, {@code longitud}) y los datos sembrados en v2 sobreviven con
 * las columnas nuevas en {@code NULL}.
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class MigracionV2V3Test {

    /** Nombre del archivo de base de datos temporal de la prueba. */
    private static final String TEST_DB = "migracion-v2-v3-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class.getCanonicalName(),
            new FrameworkSQLiteOpenHelperFactory());

    @Test
    public void migracion_2_a_3_agregaColumnasYIndiceYConservaDatos() throws IOException {
        // 1. Sembrar la base en la versión 2 (esquema 2.json).
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);
        db.execSQL("INSERT INTO cuentas (nombre, tipo, saldo, moneda, activa) "
                + "VALUES ('Efectivo', 'EFECTIVO', 1000000, 'CLP', 1)");
        db.execSQL("INSERT INTO transacciones (cuentaId, tipo, monto, concepto, fecha) "
                + "VALUES (1, 'INGRESO', 50000, 'Sueldo', 1700000000000)");
        db.close();

        // 2. Migrar a la versión 3 y validar el esquema contra 3.json.
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3);

        // 3. Las columnas geográficas existen.
        assertTrue(columnaExiste(db, "transacciones", "latitud"));
        assertTrue(columnaExiste(db, "transacciones", "longitud"));
        assertTrue(columnaExiste(db, "transacciones", "direccionLugar"));

        // 4. El índice geo existe, sobre las dos columnas.
        assertTrue(indiceExiste(db, "idx_transacciones_geo"));
        assertEquals(Arrays.asList("latitud", "longitud"),
                columnasDelIndice(db, "idx_transacciones_geo"));

        // 5. Los datos de v2 sobreviven y las columnas nuevas quedan en NULL.
        try (Cursor c = db.query(
                "SELECT concepto, monto, latitud, longitud, direccionLugar FROM transacciones")) {
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals("Sueldo", c.getString(0));
            assertEquals(50000L, c.getLong(1));
            assertTrue(c.isNull(2));
            assertTrue(c.isNull(3));
            assertTrue(c.isNull(4));
        }

        db.close();
    }

    /**
     * Indica si una columna existe en la tabla dada (vía {@code PRAGMA table_info}).
     */
    private static boolean columnaExiste(SupportSQLiteDatabase db, String tabla, String columna) {
        try (Cursor c = db.query("PRAGMA table_info('" + tabla + "')")) {
            int idx = c.getColumnIndexOrThrow("name");
            while (c.moveToNext()) {
                if (columna.equals(c.getString(idx))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Indica si un índice existe en la tabla {@code transacciones} (vía {@code PRAGMA index_list}).
     */
    private static boolean indiceExiste(SupportSQLiteDatabase db, String nombre) {
        try (Cursor c = db.query("PRAGMA index_list('transacciones')")) {
            int idx = c.getColumnIndexOrThrow("name");
            while (c.moveToNext()) {
                if (nombre.equals(c.getString(idx))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Devuelve las columnas de un índice, en su orden (vía {@code PRAGMA index_info}).
     */
    private static List<String> columnasDelIndice(SupportSQLiteDatabase db, String nombre) {
        List<String> columnas = new ArrayList<>();
        try (Cursor c = db.query("PRAGMA index_info('" + nombre + "')")) {
            int idx = c.getColumnIndexOrThrow("name");
            while (c.moveToNext()) {
                columnas.add(c.getString(idx));
            }
        }
        return columnas;
    }
}
