package com.localcash.data.db;

import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Semilla de datos inicial: inserta la cuenta "Efectivo Demo" con saldo de
 * apertura $1.000.000 CLP la <b>primera vez</b> que se crea la base de datos.
 * <p>
 * {@link RoomDatabase.Callback#onCreate} se invoca solo cuando el archivo de BD
 * se crea por primera vez; en aperturas posteriores Room no lo ejecuta, por lo
 * que la semilla nunca duplica cuentas.
 * </p>
 * <p>
 * No se usa ningún DAO aquí: el callback corre <b>durante</b> la apertura de la
 * BD, momento en que los DAOs aún no están disponibles. Se usa SQL directo con
 * argumentos enlazados ({@code ?}), la forma recomendada por Room para escritura
 * en callbacks.
 * </p>
 * <p>
 * Los montos son {@code long} en pesos CLP directos (sin centavos, sin double)
 * — {@code .agent.md} §6.10. El saldo inicial es una suma escrita una sola vez,
 * no una aritmética acumulada.
 * </p>
 */
public final class SeedDataInitializer extends RoomDatabase.Callback {

    /**
     * Nombre de tabla sincronizado con {@code @Entity(tableName = "cuentas")}
     * de {@code data/entities/Cuenta}. En {@code @Query} los literales los
     * valida Room en compilación; aquí el SQL es en tiempo de ejecución, de ahí
     * la constante.
     */
    private static final String TABLA_CUENTAS = "cuentas";

    private static final String NOMBRE_CUENTA_DEMO = "Efectivo Demo";
    private static final String TIPO_CUENTA_DEMO = "EFECTIVO";
    /** Saldo de apertura: 1_000_000L = $1.000.000 CLP (unidad mínima: 1 peso). */
    private static final long SALDO_APERTURA_DEMO = 1_000_000L;
    private static final String MONEDA_DEMO = "CLP";

    /** Constructor por defecto requerido por {@link RoomDatabase.Callback}. */
    public SeedDataInitializer() {
        super();
    }

    /**
     * Inserta la cuenta demo al crear la base de datos.
     *
     * @param db Base de datos recién creada (el esquema ya está aplicado)
     */
    @Override
    public void onCreate(@NonNull SupportSQLiteDatabase db) {
        super.onCreate(db);
        db.execSQL(
                "INSERT INTO " + TABLA_CUENTAS
                        + " (nombre, tipo, saldo, moneda) VALUES (?, ?, ?, ?)",
                new Object[]{
                        NOMBRE_CUENTA_DEMO,
                        TIPO_CUENTA_DEMO,
                        SALDO_APERTURA_DEMO,
                        MONEDA_DEMO
                });
    }
}
