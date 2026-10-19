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

/**
 * Test de la migración de esquema 4 → 5 (Feature 010, Fase 2.5) en JVM con
 * Robolectric.
 * <p>
 * Comprueba que {@code gastoPromedio} pasa a nullable sin perder lugares ni
 * ítems, que la FK y su índice sobreviven a la recreación de tablas y que el
 * borrado en cascada sigue funcionando.
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class MigracionV4V5Test {

    /** Nombre del archivo de base de datos temporal de la prueba. */
    private static final String TEST_DB = "migracion-v4-v5-test.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class.getCanonicalName(),
            new FrameworkSQLiteOpenHelperFactory());

    @Test
    public void migracion_4_a_5_haceGastoNullableYConservaDatos() throws IOException {
        // 1. Sembrar la base en la versión 4 (esquema 4.json).
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 4);
        db.execSQL("INSERT INTO lugar_compra "
                + "(latitud, longitud, tipoLugar, gastoPromedio, fechaCreacion) "
                + "VALUES (-33.4489, -70.6693, 'Supermercado', 15000, 1700000000000)");
        db.execSQL("INSERT INTO item_compra "
                + "(lugarCompraId, nombre, cantidad, estaComprado) "
                + "VALUES (1, 'Leche', 2, 0)");
        db.execSQL("INSERT INTO item_compra "
                + "(lugarCompraId, nombre, cantidad, estaComprado) "
                + "VALUES (1, 'Pan', 1, 1)");
        db.close();

        // 2. Migrar a la versión 5 y validar el esquema contra 5.json.
        db = helper.runMigrationsAndValidate(
                TEST_DB, 5, true, Migracion_4_5.MIGRATION_4_5);

        // 3. gastoPromedio ahora acepta NULL.
        assertTrue(columnaEsNullable(db, "lugar_compra", "gastoPromedio"));

        // 4. Los datos de v4 sobreviven.
        try (Cursor c = db.query(
                "SELECT tipoLugar, gastoPromedio, fechaCreacion FROM lugar_compra")) {
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals("Supermercado", c.getString(0));
            assertEquals(15000L, c.getLong(1));
            assertEquals(1700000000000L, c.getLong(2));
        }
        try (Cursor c = db.query(
                "SELECT nombre, cantidad FROM item_compra ORDER BY id")) {
            assertEquals(2, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals("Leche", c.getString(0));
            assertEquals(2, c.getInt(1));
            assertTrue(c.moveToNext());
            assertEquals("Pan", c.getString(0));
            assertEquals(1, c.getInt(1));
        }

        // 5. Se puede insertar un lugar sin gasto estimado.
        db.execSQL("INSERT INTO lugar_compra "
                + "(latitud, longitud, tipoLugar, gastoPromedio, fechaCreacion) "
                + "VALUES (-33.0, -70.0, 'Farmacia', NULL, 1700000000001)");
        try (Cursor c = db.query(
                "SELECT gastoPromedio FROM lugar_compra WHERE tipoLugar = 'Farmacia'")) {
            assertTrue(c.moveToFirst());
            assertTrue(c.isNull(0));
        }

        // 6. La FK y su índice sobreviven; no hay violaciones de integridad.
        assertTrue(indiceExiste(db, "idx_item_compra_lugar"));
        try (Cursor c = db.query("PRAGMA foreign_key_check")) {
            assertEquals(0, c.getCount());
        }

        // 7. El borrado en cascada sigue eliminando los ítems del lugar.
        db.execSQL("PRAGMA foreign_keys=ON");
        db.execSQL("DELETE FROM lugar_compra WHERE id = 1");
        try (Cursor c = db.query("SELECT COUNT(*) FROM item_compra")) {
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(0));
        }

        db.close();
    }

    /** Indica si una columna acepta NULL según {@code PRAGMA table_info}. */
    private static boolean columnaEsNullable(SupportSQLiteDatabase db,
                                              String tabla, String columna) {
        try (Cursor c = db.query("PRAGMA table_info('" + tabla + "')")) {
            int nombreIdx = c.getColumnIndexOrThrow("name");
            int notNullIdx = c.getColumnIndexOrThrow("notnull");
            while (c.moveToNext()) {
                if (columna.equals(c.getString(nombreIdx))) {
                    return c.getInt(notNullIdx) == 0;
                }
            }
        }
        return false;
    }

    /** Indica si un índice existe en {@code item_compra}. */
    private static boolean indiceExiste(SupportSQLiteDatabase db, String nombre) {
        try (Cursor c = db.query("PRAGMA index_list('item_compra')")) {
            int idx = c.getColumnIndexOrThrow("name");
            while (c.moveToNext()) {
                if (nombre.equals(c.getString(idx))) {
                    return true;
                }
            }
        }
        return false;
    }
}
