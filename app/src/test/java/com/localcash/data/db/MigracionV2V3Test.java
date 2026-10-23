package com.localcash.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import com.localcash.data.dao.ItemLugarDao;
import com.localcash.data.dao.LugarDao;
import com.localcash.data.entities.ItemLugar;
import com.localcash.data.entities.Lugar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Test de JVM (Robolectric) de la migración no destructiva 2 → 3 (T29).
 * <p>
 * Crea un archivo con el esquema real de la versión 2 y datos sembrados, lo
 * abre con Room aplicando {@link AppDatabase#MIGRATION_2_3} y comprueba que
 * cuentas y movimientos sobreviven, que las coordenadas nuevas quedan nulas y
 * que {@code LUGARES}/{@code ITEMS_LUGAR} funcionan con sus claves foráneas.
 * </p>
 * <p>
 * Se ejecuta en JVM con Robolectric (tech-stack.md §4) en lugar de
 * {@code androidTest/}: la constitución prefiere tests de persistencia sin
 * dispositivo, y la migración se prueba contra el mismo motor SQLite.
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class MigracionV2V3Test {

    private static final String NOMBRE_DB = "migracion_2_3.db";

    private Context context;

    private AppDatabase db;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase(NOMBRE_DB);
        crearBaseVersion2();
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
        context.deleteDatabase(NOMBRE_DB);
    }

    @Test
    public void testMigracionConservaCuentasYMovimientos() {
        db = abrirConMigracion();
        SupportSQLiteDatabase sqlite = db.getOpenHelper().getWritableDatabase();

        Cursor cuenta = sqlite.query(
                "SELECT nombre, saldo, activa FROM cuentas WHERE id = 1");
        try {
            assertTrue(cuenta.moveToFirst());
            assertEquals("Cuenta Corriente", cuenta.getString(0));
            assertEquals(500_000L, cuenta.getLong(1));
            assertEquals(1, cuenta.getInt(2));
        } finally {
            cuenta.close();
        }

        Cursor movimiento = sqlite.query(
                "SELECT cuentaId, tipo, monto, concepto, fecha, "
                        + "latitud, longitud, direccion_lugar "
                        + "FROM transacciones WHERE id = 2");
        try {
            assertTrue(movimiento.moveToFirst());
            assertEquals(1L, movimiento.getLong(0));
            assertEquals("EGRESO", movimiento.getString(1));
            assertEquals(25_000L, movimiento.getLong(2));
            assertEquals("Compra sembrada", movimiento.getString(3));
            assertEquals(1_700_000_000_000L, movimiento.getLong(4));
            assertTrue(movimiento.isNull(5));
            assertTrue(movimiento.isNull(6));
            assertTrue(movimiento.isNull(7));
        } finally {
            movimiento.close();
        }
    }

    @Test
    public void testMigracionCreaTablasDeLugares() {
        db = abrirConMigracion();
        SupportSQLiteDatabase sqlite = db.getOpenHelper().getWritableDatabase();

        assertTrue(existeTabla(sqlite, LugarDao.TABLA));
        assertTrue(existeTabla(sqlite, ItemLugarDao.TABLA));
        assertEquals(3, versionUsuario(sqlite));
    }

    @Test
    public void testLugaresFuncionanTrasLaMigracion() {
        db = abrirConMigracion();
        SupportSQLiteDatabase sqlite = db.getOpenHelper().getWritableDatabase();

        long lugarId = db.lugarDao().insert(new Lugar(
                2L, "Compra sembrada", "otro", "📍", null,
                -33.44, -70.66, "abierto"));
        db.itemLugarDao().insert(new ItemLugar(lugarId, "Leche", false));

        assertEquals(1, db.lugarDao().getByIdSync(lugarId).getId());
        assertEquals(1, db.itemLugarDao().getByLugarIdSync(lugarId).size());

        db.lugarDao().delete(db.lugarDao().getByIdSync(lugarId));
        assertTrue(db.itemLugarDao().getByLugarIdSync(lugarId).isEmpty());
        assertTrue(foreignKeyCheckLimpio(sqlite));
    }

    @Test
    public void testMigracionNoPierdeFilasExistentes() {
        db = abrirConMigracion();
        SupportSQLiteDatabase sqlite = db.getOpenHelper().getWritableDatabase();

        assertEquals(2, contar(sqlite, "cuentas"));
        assertEquals(1, contar(sqlite, "transacciones"));
        assertEquals(0, contar(sqlite, LugarDao.TABLA));
        assertEquals(0, contar(sqlite, ItemLugarDao.TABLA));
    }

    /**
     * Crea el archivo de base de datos con el esquema exacto de la versión 2
     * (tomado de {@code app/schemas/.../2.json}) y siembra datos.
     */
    private void crearBaseVersion2() {
        SupportSQLiteOpenHelper.Configuration configuracion =
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(NOMBRE_DB)
                        .callback(new SupportSQLiteOpenHelper.Callback(2) {
                            @Override
                            public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE IF NOT EXISTS `cuentas` ("
                                        + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                                        + "`nombre` TEXT, "
                                        + "`tipo` TEXT, "
                                        + "`saldo` INTEGER NOT NULL, "
                                        + "`moneda` TEXT DEFAULT 'CLP', "
                                        + "`activa` INTEGER NOT NULL DEFAULT 1)");
                                db.execSQL("CREATE TABLE IF NOT EXISTS `transacciones` ("
                                        + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                                        + "`cuentaId` INTEGER NOT NULL, "
                                        + "`tipo` TEXT, "
                                        + "`monto` INTEGER NOT NULL, "
                                        + "`concepto` TEXT, "
                                        + "`fecha` INTEGER NOT NULL, "
                                        + "FOREIGN KEY(`cuentaId`) REFERENCES `cuentas`(`id`) "
                                        + "ON UPDATE NO ACTION ON DELETE CASCADE )");
                                db.execSQL("CREATE INDEX IF NOT EXISTS "
                                        + "`index_transacciones_cuentaId` "
                                        + "ON `transacciones` (`cuentaId`)");

                                db.execSQL("INSERT INTO cuentas "
                                        + "(id, nombre, tipo, saldo, moneda, activa) VALUES "
                                        + "(1, 'Cuenta Corriente', 'CORRIENTE', 500000, 'CLP', 1)");
                                db.execSQL("INSERT INTO cuentas "
                                        + "(id, nombre, tipo, saldo, moneda, activa) VALUES "
                                        + "(2, 'Tarjeta Visa', 'CREDITO', 100000, 'CLP', 0)");
                                db.execSQL("INSERT INTO transacciones "
                                        + "(id, cuentaId, tipo, monto, concepto, fecha) VALUES "
                                        + "(2, 1, 'EGRESO', 25000, 'Compra sembrada', 1700000000000)");
                            }

                            @Override
                            public void onUpgrade(SupportSQLiteDatabase db, int anterior,
                                                  int nueva) {
                                throw new IllegalStateException(
                                        "La base de prueba debe nacer en la versión 2");
                            }
                        })
                        .build();

        SupportSQLiteOpenHelper helper =
                new FrameworkSQLiteOpenHelperFactory().create(configuracion);
        try {
            helper.getWritableDatabase();
        } finally {
            helper.close();
        }
    }

    /**
     * Abre la base sembrada con Room y la migración real.
     *
     * @return base ya migrada
     */
    private AppDatabase abrirConMigracion() {
        return Room.databaseBuilder(context, AppDatabase.class, NOMBRE_DB)
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
                .allowMainThreadQueries()
                .build();
    }

    /**
     * Comprueba si una tabla existe en {@code sqlite_master}.
     *
     * @param db    base abierta
     * @param tabla nombre exacto de la tabla
     * @return {@code true} si existe
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
     * Cuenta filas de una tabla.
     *
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
            return !cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }
}
