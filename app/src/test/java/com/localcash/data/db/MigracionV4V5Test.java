package com.localcash.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;

import com.localcash.data.dao.GastoCompartidoDao;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;

/**
 * Test de JVM (Robolectric) de la migración no destructiva 4 → 5 (Feature 012).
 * <p>
 * Usa {@link MigrationTestHelper} con los esquemas exportados en
 * {@code app/schemas/}: crea una base real en la versión 4, siembra datos,
 * aplica {@link AppDatabase#MIGRATION_4_5} y valida que el esquema resultante
 * coincide exactamente con {@code 5.json} (tabla, índices y claves foráneas).
 * </p>
 * <p>
 * Se ejecuta en JVM con Robolectric (tech-stack.md §4) en lugar de
 * {@code androidTest/}: la migración se prueba contra el mismo motor SQLite sin
 * dispositivo. Los schemas se exponen como assets del test en
 * {@code app/build.gradle} ({@code sourceSets.test.assets}).
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class MigracionV4V5Test {

    private static final String NOMBRE_DB = "migracion_4_5.db";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class);

    @Test
    public void testMigracionCreaTablaEIndicesYVersion5() throws IOException {
        SupportSQLiteDatabase db = crearBaseVersion4Sembrada();
        db.execSQL("PRAGMA foreign_keys = ON");

        assertTrue(existeTabla(db, GastoCompartidoDao.TABLE_NAME));
        assertTrue(existeIndice(db, "ux_gastos_compartidos_transaccion_contacto"));
        assertTrue(existeIndice(db, "idx_gastos_compartidos_contacto_estado"));
        assertEquals(5, versionUsuario(db));
        assertTrue(foreignKeyCheckLimpio(db));
    }

    @Test
    public void testMigracionConservaDatosExistentes() throws IOException {
        SupportSQLiteDatabase db = crearBaseVersion4Sembrada();

        assertEquals(1, contar(db, "cuentas"));
        assertEquals(1, contar(db, "transacciones"));
        assertEquals(2, contar(db, "CONTACTOS"));
        assertEquals(0, contar(db, GastoCompartidoDao.TABLE_NAME));

        Cursor movimiento = db.query(
                "SELECT monto, concepto FROM transacciones WHERE id = 1");
        try {
            assertTrue(movimiento.moveToFirst());
            assertEquals(100_000L, movimiento.getLong(0));
            assertEquals("Cena compartida", movimiento.getString(1));
        } finally {
            movimiento.close();
        }
    }

    @Test
    public void testPartesFuncionanTrasLaMigracion() throws IOException {
        SupportSQLiteDatabase db = crearBaseVersion4Sembrada();
        db.execSQL("PRAGMA foreign_keys = ON");

        db.execSQL("INSERT INTO " + GastoCompartidoDao.TABLE_NAME
                + " (transaccion_id, contacto_id, monto_asignado, estado, fecha_creacion)"
                + " VALUES (1, 1, 60000, 'pendiente', '2026-10-26T12:00:00Z')");
        db.execSQL("INSERT INTO " + GastoCompartidoDao.TABLE_NAME
                + " (transaccion_id, contacto_id, monto_asignado, estado, fecha_creacion)"
                + " VALUES (1, 2, 40000, 'pendiente', '2026-10-26T12:00:00Z')");

        assertEquals(2, contar(db, GastoCompartidoDao.TABLE_NAME));

        db.execSQL("UPDATE " + GastoCompartidoDao.TABLE_NAME
                + " SET estado = 'pagado', fecha_pago = '2026-10-26T13:00:00Z'"
                + " WHERE transaccion_id = 1 AND contacto_id = 1");
        Cursor parte = db.query("SELECT estado, fecha_pago, monto_asignado FROM "
                + GastoCompartidoDao.TABLE_NAME + " WHERE contacto_id = 1");
        try {
            assertTrue(parte.moveToFirst());
            assertEquals("pagado", parte.getString(0));
            assertEquals("2026-10-26T13:00:00Z", parte.getString(1));
            assertEquals(60_000L, parte.getLong(2));
        } finally {
            parte.close();
        }
        assertTrue(foreignKeyCheckLimpio(db));
    }

    @Test
    public void testIndiceUnicoImpideContactoRepetidoEnElMismoEgreso() throws IOException {
        SupportSQLiteDatabase db = crearBaseVersion4Sembrada();
        db.execSQL("PRAGMA foreign_keys = ON");

        db.execSQL("INSERT INTO " + GastoCompartidoDao.TABLE_NAME
                + " (transaccion_id, contacto_id, monto_asignado, estado, fecha_creacion)"
                + " VALUES (1, 1, 50000, 'pendiente', '2026-10-26T12:00:00Z')");
        try {
            db.execSQL("INSERT INTO " + GastoCompartidoDao.TABLE_NAME
                    + " (transaccion_id, contacto_id, monto_asignado, estado, fecha_creacion)"
                    + " VALUES (1, 1, 50000, 'pendiente', '2026-10-26T12:00:00Z')");
            fail("El índice único debe rechazar dos partes del mismo contacto en el mismo egreso");
        } catch (SQLiteConstraintException esperado) {
            assertEquals(1, contar(db, GastoCompartidoDao.TABLE_NAME));
        }
    }

    @Test
    public void testIntegridadReferencialTrasLaMigracion() throws IOException {
        SupportSQLiteDatabase db = crearBaseVersion4Sembrada();
        db.execSQL("PRAGMA foreign_keys = ON");

        db.execSQL("INSERT INTO " + GastoCompartidoDao.TABLE_NAME
                + " (transaccion_id, contacto_id, monto_asignado, estado, fecha_creacion)"
                + " VALUES (1, 1, 60000, 'pendiente', '2026-10-26T12:00:00Z')");

        // RESTRICT: un contacto con partes no se puede eliminar (INV-4).
        try {
            db.execSQL("DELETE FROM CONTACTOS WHERE id = 1");
            fail("La clave foránea RESTRICT debe bloquear el borrado del contacto");
        } catch (SQLiteConstraintException esperado) {
            assertEquals(2, contar(db, "CONTACTOS"));
        }

        // CASCADE: borrar el egreso elimina sus partes (INV-2 / trazabilidad).
        db.execSQL("DELETE FROM transacciones WHERE id = 1");
        assertEquals(0, contar(db, GastoCompartidoDao.TABLE_NAME));
        assertEquals(0, contar(db, "transacciones"));
        assertTrue(foreignKeyCheckLimpio(db));
    }

    /**
     * Crea la base en la versión 4 desde {@code 4.json}, siembra una cuenta, un
     * egreso y dos contactos, y la migra a la versión 5 validando el esquema.
     *
     * @return base ya migrada y validada
     */
    private SupportSQLiteDatabase crearBaseVersion4Sembrada() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(NOMBRE_DB, 4);

        db.execSQL("INSERT INTO cuentas (id, nombre, tipo, saldo, moneda, activa) "
                + "VALUES (1, 'Efectivo', 'EFECTIVO', 200000, 'CLP', 1)");
        db.execSQL("INSERT INTO transacciones "
                + "(id, cuentaId, tipo, monto, concepto, fecha) "
                + "VALUES (1, 1, 'EGRESO', 100000, 'Cena compartida', 1761480000000)");
        db.execSQL("INSERT INTO CONTACTOS (id, nombre) VALUES (1, 'Juan Pérez')");
        db.execSQL("INSERT INTO CONTACTOS (id, nombre) VALUES (2, 'María García')");
        db.close();

        return helper.runMigrationsAndValidate(
                NOMBRE_DB, 5, true, AppDatabase.MIGRATION_4_5);
    }

    /**
     * @param db    base abierta
     * @param tabla nombre exacto de la tabla
     * @return {@code true} si la tabla existe
     */
    private boolean existeTabla(SupportSQLiteDatabase db, String tabla) {
        Cursor cursor = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                new Object[]{tabla});
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    /**
     * @param db      base abierta
     * @param indice  nombre exacto del índice
     * @return {@code true} si el índice existe
     */
    private boolean existeIndice(SupportSQLiteDatabase db, String indice) {
        Cursor cursor = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?",
                new Object[]{indice});
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    /**
     * @param db base abierta
     * @return valor de {@code PRAGMA user_version}
     */
    private int versionUsuario(SupportSQLiteDatabase db) {
        Cursor cursor = db.query("PRAGMA user_version");
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        } finally {
            cursor.close();
        }
    }

    /**
     * @param db    base abierta
     * @param tabla tabla a contar
     * @return número de filas
     */
    private int contar(SupportSQLiteDatabase db, String tabla) {
        Cursor cursor = db.query("SELECT COUNT(*) FROM " + tabla);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        } finally {
            cursor.close();
        }
    }

    /**
     * @param db base abierta
     * @return {@code true} si {@code PRAGMA foreign_key_check} no reporta filas
     */
    private boolean foreignKeyCheckLimpio(SupportSQLiteDatabase db) {
        Cursor cursor = db.query("PRAGMA foreign_key_check");
        try {
            assertFalse("foreign_key_check no debe devolver filas", cursor.moveToFirst());
            return true;
        } finally {
            cursor.close();
        }
    }
}