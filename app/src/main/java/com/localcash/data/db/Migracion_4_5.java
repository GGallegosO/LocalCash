package com.localcash.data.db;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migración no destructiva del esquema 4 → 5 (Feature 010, Fase 2.5).
 * <p>
 * Cambia {@code lugar_compra.gastoPromedio} de {@code INTEGER NOT NULL} a
 * {@code INTEGER} nullable, porque el gasto estimado pasa a ser opcional
 * ({@code null} = sin gasto estimado).
 * </p>
 * <p>
 * SQLite no permite retirar {@code NOT NULL} con {@code ALTER TABLE}, así que se
 * aplica el patrón de doble fase documentado en {@code data-model.md}:
 * <b>crear tablas nuevas → copiar datos → retirar las viejas → renombrar</b>.
 * La copia ocurre antes de cualquier {@code DROP TABLE}; el {@code DROP} solo
 * retira las tablas ya copiadas y nunca es una estrategia de actualización
 * destructiva ({@code .agent.md} §6.11).
 * </p>
 * <p>
 * {@code item_compra} también se recrea porque su clave foránea apunta a
 * {@code lugar_compra}. Se copian los ítems a una tabla de paso <b>sin FK</b>
 * antes de retirar las tablas viejas, de modo que la cascada no borra datos
 * durante la migración. Después de renombrar la tabla del lugar se crea la
 * {@code item_compra} definitiva con su FK explícita: así la migración no
 * depende de que SQLite actualice (o no) las referencias al renombrar, un
 * comportamiento que varía entre versiones y con {@code legacy_alter_table}.
 * </p>
 */
public final class Migracion_4_5 {

    private Migracion_4_5() {
        // Clase de utilidad: no se instancia.
    }

    /** Migración registrada en {@link AppDatabase}. */
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. Tabla nueva del lugar, con gastoPromedio nullable.
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `lugar_compra_nueva` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`latitud` REAL NOT NULL, "
                            + "`longitud` REAL NOT NULL, "
                            + "`tipoLugar` TEXT NOT NULL, "
                            + "`gastoPromedio` INTEGER, "
                            + "`fechaCreacion` INTEGER NOT NULL)");
            database.execSQL(
                    "INSERT INTO `lugar_compra_nueva` "
                            + "(`id`, `latitud`, `longitud`, `tipoLugar`, "
                            + "`gastoPromedio`, `fechaCreacion`) "
                            + "SELECT `id`, `latitud`, `longitud`, `tipoLugar`, "
                            + "`gastoPromedio`, `fechaCreacion` FROM `lugar_compra`");

            // 2. Tabla de paso de ítems, SIN FK: solo conserva los datos mientras
            //    se recrean las tablas definitivas. La FK se declara al final,
            //    cuando el padre ya tiene su nombre definitivo.
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `item_compra_nueva` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`lugarCompraId` INTEGER NOT NULL, "
                            + "`nombre` TEXT NOT NULL, "
                            + "`cantidad` INTEGER NOT NULL, "
                            + "`estaComprado` INTEGER NOT NULL)");
            database.execSQL(
                    "INSERT INTO `item_compra_nueva` "
                            + "(`id`, `lugarCompraId`, `nombre`, `cantidad`, `estaComprado`) "
                            + "SELECT `id`, `lugarCompraId`, `nombre`, `cantidad`, `estaComprado` "
                            + "FROM `item_compra`");

            // 3. Retirar las tablas viejas una vez copiados sus datos. Primero la
            //    hija: así el DROP del padre no dispara la cascada sobre los ítems.
            database.execSQL("DROP TABLE `item_compra`");
            database.execSQL("DROP TABLE `lugar_compra`");

            // 4. El lugar nuevo pasa a tener su nombre definitivo.
            database.execSQL("ALTER TABLE `lugar_compra_nueva` RENAME TO `lugar_compra`");

            // 5. Crear la tabla definitiva de ítems con la FK explícita al padre ya
            //    renombrado y copiar los datos de paso.
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `item_compra` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`lugarCompraId` INTEGER NOT NULL, "
                            + "`nombre` TEXT NOT NULL, "
                            + "`cantidad` INTEGER NOT NULL, "
                            + "`estaComprado` INTEGER NOT NULL, "
                            + "FOREIGN KEY(`lugarCompraId`) REFERENCES `lugar_compra`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL(
                    "INSERT INTO `item_compra` "
                            + "(`id`, `lugarCompraId`, `nombre`, `cantidad`, `estaComprado`) "
                            + "SELECT `id`, `lugarCompraId`, `nombre`, `cantidad`, `estaComprado` "
                            + "FROM `item_compra_nueva`");
            database.execSQL("DROP TABLE `item_compra_nueva`");

            // 6. Recrear el índice de la FK hija.
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_item_compra_lugar` "
                            + "ON `item_compra` (`lugarCompraId`)");
        }
    };
}
