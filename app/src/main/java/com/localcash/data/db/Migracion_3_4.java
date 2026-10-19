package com.localcash.data.db;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Migración no destructiva del esquema 3 → 4 (Feature 010, Fase 1).
 * <p>
 * Crea las tablas {@code lugar_compra} e {@code item_compra} y el índice de la
 * clave foránea {@code idx_item_compra_lugar}. No toca ninguna tabla existente:
 * los datos de cuentas y movimientos se conservan intactos.
 * </p>
 * <p>
 * {@code fallbackToDestructiveMigration()} y {@code DROP TABLE} están prohibidos
 * ({@code .agent.md} §6.11). El DDL reproduce el esquema que Room exporta en
 * {@code app/schemas/com.localcash.data.db.AppDatabase/4.json}.
 * </p>
 */
public final class Migracion_3_4 {

    private Migracion_3_4() {
        // Clase de utilidad: no se instancia.
    }

    /** Migración registrada en {@link AppDatabase}. */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `lugar_compra` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`latitud` REAL NOT NULL, "
                            + "`longitud` REAL NOT NULL, "
                            + "`tipoLugar` TEXT NOT NULL, "
                            + "`gastoPromedio` INTEGER NOT NULL, "
                            + "`fechaCreacion` INTEGER NOT NULL)");

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
                    "CREATE INDEX IF NOT EXISTS `idx_item_compra_lugar` "
                            + "ON `item_compra` (`lugarCompraId`)");
        }
    };
}
