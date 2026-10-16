package com.localcash.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.localcash.data.dao.CuentaDao;
import com.localcash.data.dao.TransaccionDao;
import com.localcash.data.entities.Cuenta;
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
        entities = {Cuenta.class, Transaccion.class},
        version = 2,
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
                            .addMigrations(MIGRATION_1_2)
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
}
