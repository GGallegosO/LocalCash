package com.localcash.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.localcash.data.dao.ContactoDao;
import com.localcash.data.dao.CuentaDao;
import com.localcash.data.dao.ItemLugarDao;
import com.localcash.data.dao.LugarDao;
import com.localcash.data.dao.TransaccionDao;
import com.localcash.data.entities.Contacto;
import com.localcash.data.entities.Cuenta;
import com.localcash.data.entities.ItemLugar;
import com.localcash.data.entities.Lugar;
import com.localcash.data.entities.Transaccion;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base de datos Room de LocalCash.
 * <p>
 * Único punto de acceso al esquema financiero local (100% offline).
 * El esquema exportado queda en {@code app/schemas/} gracias a
 * {@code room.schemaLocation} (build.gradle) y {@code exportSchema = true}.
 * </p>
 * <p>
 * Las operaciones <b>síncronas</b> de los DAOs ({@code insert}, {@code updateSaldo},
 * {@code delete}) deben ejecutarse con {@link #getExecutor()} para no bloquear el
 * hilo principal. Las consultas con {@code LiveData} se auto-gestionan en background.
 * </p>
 * <p>
 * Singleton thread-safe con doble verificación ({@code volatile}).
 * </p>
 */
@Database(
        entities = {Cuenta.class, Transaccion.class, Lugar.class, ItemLugar.class, Contacto.class},
        version = 4,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "localcash.db";

    /**
     * Migración real del esquema 1 → 2: agrega la columna {@code activa}
     * (soft delete) a la tabla {@code cuentas}. Se usa {@code ALTER TABLE}
     * en lugar de borrar y recrear (nunca se pierde información financiera).
     * {@code fallbackToDestructiveMigration()} está prohibido ({@code .agent.md} §6.11).
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE cuentas ADD COLUMN activa INTEGER NOT NULL DEFAULT 1");
        }
    };

    /**
     * Migración no destructiva 2 → 3 (Feature 009):
     * <ul>
     *   <li>agrega las coordenadas opcionales a {@code transacciones};</li>
     *   <li>crea {@code LUGARES} y {@code ITEMS_LUGAR} con sus índices.</li>
     * </ul>
     * No se usa {@code DROP TABLE} ni {@code fallbackToDestructiveMigration()}
     * ({@code .agent.md} §6.11). La tabla de movimientos existente conserva sus
     * filas y las nuevas columnas quedan {@code NULL} hasta que el usuario
     * geolocalice un movimiento.
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transacciones ADD COLUMN latitud REAL");
            database.execSQL("ALTER TABLE transacciones ADD COLUMN longitud REAL");
            database.execSQL("ALTER TABLE transacciones ADD COLUMN direccion_lugar TEXT");

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `LUGARES` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`transaccion_id` INTEGER, "
                            + "`nombre` TEXT NOT NULL, "
                            + "`tipo_lugar` TEXT NOT NULL, "
                            + "`emoji` TEXT NOT NULL DEFAULT '📍', "
                            + "`descripcion` TEXT, "
                            + "`latitud` REAL NOT NULL, "
                            + "`longitud` REAL NOT NULL, "
                            + "`estado` TEXT NOT NULL DEFAULT 'abierto', "
                            + "FOREIGN KEY(`transaccion_id`) REFERENCES `transacciones`(`id`) "
                            + "ON UPDATE CASCADE ON DELETE SET NULL)");

            database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_LUGARES_transaccion_id` "
                            + "ON `LUGARES` (`transaccion_id`)");
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_LUGARES_latitud_longitud` "
                            + "ON `LUGARES` (`latitud`, `longitud`)");
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_LUGARES_estado_id` "
                            + "ON `LUGARES` (`estado`, `id`)");

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ITEMS_LUGAR` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`lugar_id` INTEGER NOT NULL, "
                            + "`descripcion` TEXT NOT NULL, "
                            + "`completado` INTEGER NOT NULL DEFAULT 0, "
                            + "FOREIGN KEY(`lugar_id`) REFERENCES `LUGARES`(`id`) "
                            + "ON UPDATE CASCADE ON DELETE CASCADE)");

            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ITEMS_LUGAR_lugar_id` "
                            + "ON `ITEMS_LUGAR` (`lugar_id`)");
        }
    };

    /**
     * Migración no destructiva 3 → 4 (Feature 011 - Contactos):
     * <ul>
     *   <li>crea la tabla {@code CONTACTOS} con nombre obligatorio, teléfono y
     *       email opcionales (únicos parciales), y notas;</li>
     *   <li>los índices únicos parciales permiten múltiples {@code NULL} en
     *       teléfono/email, así que varios contactos sin esos datos conviven.</li>
     * </ul>
     * No se usa {@code DROP TABLE} ni {@code fallbackToDestructiveMigration()}
     * ({@code .agent.md} §6.11).
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `CONTACTOS` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`nombre` TEXT NOT NULL, "
                            + "`telefono` TEXT, "
                            + "`email` TEXT, "
                            + "`notas` TEXT)");

            database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_CONTACTOS_telefono` "
                            + "ON `CONTACTOS` (`telefono` COLLATE NOCASE) "
                            + "WHERE `telefono` IS NOT NULL");
            database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_CONTACTOS_email` "
                            + "ON `CONTACTOS` (`email` COLLATE NOCASE) "
                            + "WHERE `email` IS NOT NULL");
        }
    };

    /** Instante única (double-checked locking). volatile: publicación segura entre hilos. */
    private static volatile AppDatabase instance;

    /**
     * Único hilo de escritura de la aplicación: serializa las operaciones
     * síncronas de los DAOs y evita condiciones de carrera en el saldo.
     */
    private static final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    /**
     * Obtiene la instancia única de la base de datos (thread-safe).
     *
     * @param context Contexto de la aplicación (se usa el applicationContext
     *                para no generar leaks de Activities)
     * @return Instante singleton de {@link AppDatabase}
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            // fallbackToDestructiveMigration() PROHIBIDO (.agent.md §6.11)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .addCallback(new SeedDataInitializer())
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * Executor para operaciones síncronas sobre los DAOs.
     *
     * @return Executor de un solo hilo compartido por toda la app
     */
    public static ExecutorService getExecutor() {
        return databaseExecutor;
    }

    /** Expone el DAO de cuentas. */
    public abstract CuentaDao cuentaDao();

    /** Expone el DAO de transacciones. */
    public abstract TransaccionDao transaccionDao();

    /** Expone el DAO de lugares/pines. */
    public abstract LugarDao lugarDao();

    /** Expone el DAO de la lista de cosas de un lugar. */
    public abstract ItemLugarDao itemLugarDao();

    /** Expone el DAO de contactos. */
    public abstract ContactoDao contactoDao();

}
