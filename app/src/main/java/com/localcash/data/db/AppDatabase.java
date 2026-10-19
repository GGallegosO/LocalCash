package com.localcash.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.localcash.data.dao.CuentaDao;
import com.localcash.data.dao.ItemCompraDao;
import com.localcash.data.dao.LugarCompraDao;
import com.localcash.data.dao.TransaccionDao;
import com.localcash.data.entities.Cuenta;
import com.localcash.data.entities.ItemCompra;
import com.localcash.data.entities.LugarCompra;
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
        entities = {Cuenta.class, Transaccion.class, LugarCompra.class, ItemCompra.class},
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
     * Migración real del esquema 2 → 3: agrega las columnas de geolocalización
     * ({@code latitud}, {@code longitud}, {@code direccionLugar}) a la tabla
     * {@code transacciones} y su índice de caja envolvente (bbox).
     * <p>
     * Se usa {@code ALTER TABLE} (no destructiva): los movimientos existentes
     * conservan sus datos y quedan con las columnas geográficas en {@code NULL}
     * (sin ubicación). El índice se crea con la forma <b>parcial</b> del DDL
     * canónico ({@code data-model.md} Apéndice A); Room no puede declararlo
     * parcial en la entidad, pero su validación ignora la cláusula {@code WHERE}.
     * </p>
     * <p>
     * La regla "ambas o ninguna" y el rango de coordenadas no se pueden expresar
     * como {@code CHECK} al añadir columnas con {@code ALTER TABLE}; los hace
     * cumplir el dominio ({@code Ubicacion}) y, en su momento, el DAO.
     * {@code fallbackToDestructiveMigration()} está prohibido ({@code .agent.md} §6.11).
     * </p>
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transacciones ADD COLUMN latitud REAL");
            database.execSQL("ALTER TABLE transacciones ADD COLUMN longitud REAL");
            database.execSQL("ALTER TABLE transacciones ADD COLUMN direccionLugar TEXT");
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_transacciones_geo "
                            + "ON transacciones (latitud, longitud) "
                            + "WHERE latitud IS NOT NULL AND longitud IS NOT NULL");
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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3,
                                    Migracion_3_4.MIGRATION_3_4)
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

    /** Expone el DAO de lugares de compra. */
    public abstract LugarCompraDao lugarCompraDao();

    /** Expone el DAO de ítems de compra. */
    public abstract ItemCompraDao itemCompraDao();
}
