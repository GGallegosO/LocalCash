package com.localcash.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Cuenta;
import com.localcash.data.entities.GastoCompartido;
import com.localcash.data.entities.Transaccion;
import com.localcash.data.model.ResumenDeuda;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests de JVM (Robolectric + Room in-memory) de {@link GastoCompartidoDao}
 * (Feature 012, T9).
 * <p>
 * Cubren el ciclo de la parte compartida: inserción por lotes, consultas de
 * pendientes y su orden, resumen de deuda por contacto, cobro con
 * {@code WHERE estado='pendiente'} (y su segundo intento), la cascada al borrar
 * el egreso, el {@code RESTRICT} del contacto con partes (INV-4) y las
 * garantías del motor ({@code PRAGMA foreign_keys} y {@code foreign_key_check}).
 * </p>
 * <p>
 * Room no expresa {@code CHECK}: las reglas de monto y coherencia estado ↔
 * fecha las valida el dominio, así que aquí se verifica lo que el motor sí
 * garantiza.
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class GastoCompartidoDaoTest {

    private static final String FECHA_REPARTO = "2026-10-26T12:00:00Z";

    private AppDatabase db;
    private GastoCompartidoDao dao;

    private long cuentaId;
    private long egresoId;
    private long juanId;
    private long mariaId;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                        RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.gastoCompartidoDao();

        db.cuentaDao().insert(new Cuenta("Efectivo", "efectivo", 200_000L, "CLP"));
        cuentaId = primerId("SELECT id FROM cuentas LIMIT 1");
        egresoId = db.transaccionDao().insert(new Transaccion(
                cuentaId, "EGRESO", 100_000L, "Cena compartida", 1_761_480_000_000L));
        juanId = db.contactoDao().insert(
                new com.localcash.data.entities.Contacto("Juan Pérez", null, null, null));
        mariaId = db.contactoDao().insert(
                new com.localcash.data.entities.Contacto("María García", null, null, null));
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ---------------------------------------------------------------
    // Inserción y consultas
    // ---------------------------------------------------------------

    @Test
    public void testInsertTodasCreaLasPartesPendientes() {
        List<Long> ids = dao.insertTodas(Arrays.asList(
                parte(juanId, 60_000L), parte(mariaId, 40_000L)));

        assertEquals(2, ids.size());
        List<GastoCompartido> partes = dao.getByTransaccion(egresoId);
        assertEquals(2, partes.size());
        assertEquals(GastoCompartido.ESTADO_PENDIENTE, partes.get(0).getEstado());
        assertNull(partes.get(0).getFechaPago());
        assertEquals(100_000L, suma(partes));
    }

    @Test
    public void testIndiceUnicoRechazaContactoRepetidoEnElMismoEgreso() {
        dao.insertTodas(Arrays.asList(parte(juanId, 100_000L)));
        try {
            dao.insertTodas(Arrays.asList(parte(juanId, 10_000L)));
            fail("El índice único debe rechazar la segunda parte del mismo contacto");
        } catch (SQLiteConstraintException esperado) {
            assertEquals(1, dao.getByTransaccion(egresoId).size());
        }
    }

    @Test
    public void testPendientesPorContactoSoloPendientesYOrdenMontoDesc() {
        long segundoEgreso = db.transaccionDao().insert(new Transaccion(
                cuentaId, "EGRESO", 70_000L, "Taxi", 1_761_500_000_000L));
        dao.insertTodas(Arrays.asList(parte(juanId, 10_000L), parte(mariaId, 50_000L)));
        dao.insertTodas(Arrays.asList(
                new GastoCompartido(segundoEgreso, juanId, 30_000L, FECHA_REPARTO)));

        // Una tercera parte de Juan, ya cobrada: no debe aparecer entre pendientes.
        long cobrada = dao.insertTodas(Arrays.asList(
                new GastoCompartido(segundoEgreso, mariaId, 5_000L, FECHA_REPARTO))).get(0);
        dao.marcarPagada(cobrada, "2026-10-26T13:00:00Z");

        List<GastoCompartido> pendientes = dao.getPendientesPorContacto(juanId);

        assertEquals(2, pendientes.size());
        assertEquals(30_000L, pendientes.get(0).getMontoAsignado());
        assertEquals(10_000L, pendientes.get(1).getMontoAsignado());
        for (GastoCompartido parte : pendientes) {
            assertEquals(GastoCompartido.ESTADO_PENDIENTE, parte.getEstado());
            assertEquals(juanId, parte.getContactoId());
        }
        assertTrue(dao.getPendientesPorContacto(mariaId).size() == 1);
    }

    // ---------------------------------------------------------------
    // Cobro (INV-3 / I-14)
    // ---------------------------------------------------------------

    @Test
    public void testMarcarPagadaCambiaEstadoYFecha() {
        long parteId = dao.insertTodas(Arrays.asList(parte(juanId, 60_000L))).get(0);

        assertEquals(1, dao.marcarPagada(parteId, "2026-10-26T13:00:00Z"));

        GastoCompartido cobrada = dao.getByIdSync(parteId);
        assertNotNull(cobrada);
        assertEquals(GastoCompartido.ESTADO_PAGADO, cobrada.getEstado());
        assertEquals("2026-10-26T13:00:00Z", cobrada.getFechaPago());
    }

    @Test
    public void testSegundoCobroNoAfectaFilasNiSobrescribeLaFecha() {
        long parteId = dao.insertTodas(Arrays.asList(parte(juanId, 60_000L))).get(0);
        dao.marcarPagada(parteId, "2026-10-26T13:00:00Z");

        assertEquals(0, dao.marcarPagada(parteId, "2026-10-26T14:00:00Z"));

        GastoCompartido sigue = dao.getByIdSync(parteId);
        assertNotNull(sigue);
        assertEquals("2026-10-26T13:00:00Z", sigue.getFechaPago());
        assertTrue(dao.getPendientesPorContacto(juanId).isEmpty());
    }

    // ---------------------------------------------------------------
    // Integridad referencial
    // ---------------------------------------------------------------

    @Test
    public void testBorrarEgresoEliminaSusPartesEnCascada() {
        dao.insertTodas(Arrays.asList(parte(juanId, 60_000L), parte(mariaId, 40_000L)));

        db.getOpenHelper().getWritableDatabase()
                .delete("transacciones", "id = ?", new String[]{String.valueOf(egresoId)});

        assertEquals(0, dao.getByTransaccion(egresoId).size());
        assertTrue(foreignKeyCheckLimpio());
    }

    @Test
    public void testBorrarContactoConPartesEstaRestringido() {
        dao.insertTodas(Arrays.asList(parte(juanId, 60_000L)));

        try {
            db.contactoDao().delete(db.contactoDao().getByIdSync(juanId));
            fail("RESTRICT debe bloquear el borrado del contacto con partes");
        } catch (SQLiteConstraintException esperado) {
            assertNotNull(db.contactoDao().getByIdSync(juanId));
            assertEquals(1, dao.getByTransaccion(egresoId).size());
        }
    }

    @Test
    public void testDeleteByTransaccionEsSoloUsoInterno() {
        dao.insertTodas(Arrays.asList(parte(juanId, 60_000L), parte(mariaId, 40_000L)));

        assertEquals(2, dao.deleteByTransaccion(egresoId));
        assertEquals(0, dao.getByTransaccion(egresoId).size());
    }

    @Test
    public void testForeignKeysActivasYCheckLimpio() {
        dao.insertTodas(Arrays.asList(parte(juanId, 60_000L)));

        Cursor activadas = db.getOpenHelper().getWritableDatabase().query("PRAGMA foreign_keys");
        try {
            assertTrue(activadas.moveToFirst());
            assertEquals(1, activadas.getInt(0));
        } finally {
            activadas.close();
        }
        assertTrue(foreignKeyCheckLimpio());
    }

    // ---------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------

    private GastoCompartido parte(long contactoId, long monto) {
        return new GastoCompartido(egresoId, contactoId, monto, FECHA_REPARTO);
    }

    private static long suma(List<GastoCompartido> partes) {
        long total = 0L;
        for (GastoCompartido parte : partes) {
            total += parte.getMontoAsignado();
        }
        return total;
    }

    private long primerId(String sql) {
        Cursor cursor = db.getOpenHelper().getWritableDatabase().query(sql);
        try {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        } finally {
            cursor.close();
        }
    }

    private boolean foreignKeyCheckLimpio() {
        Cursor cursor = db.getOpenHelper().getWritableDatabase().query("PRAGMA foreign_key_check");
        try {
            return !cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }
}