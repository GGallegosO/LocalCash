package com.localcash.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.Room;

import com.localcash.data.dao.GastoCompartidoDao;
import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Cuenta;
import com.localcash.data.entities.GastoCompartido;
import com.localcash.data.entities.Transaccion;
import com.localcash.data.model.DeudaContacto;
import com.localcash.data.model.ResumenDeuda;
import com.localcash.data.repository.GastoCompartidoRepository;
import com.localcash.domain.model.PartePropuesta;
import com.localcash.domain.service.GastoCompartidoService;
import com.localcash.testing.LiveDataTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Test de integración end-to-end <b>sin UI</b> de la Feature 012 (T10).
 * <p>
 * Reproducen el flujo real sobre Room en memoria: cuenta con saldo, egreso
 * pagado por el usuario, reparto igual con <b>usuario implícito (N+1)</b> a
 * través del dominio y del repositorio, cobro de una parte y comprobación de
 * que el asiento contable sostiene el invariante I-1
 * ({@code saldo = saldo_inicial + Σ ingresos − Σ egresos}).
 * </p>
 * <p>
 * No depende de Activities ni ViewModels: pasa por
 * {@link GastoCompartidoService} (cálculo), {@link GastoCompartidoRepository}
 * (atomicidad) y {@link GastoCompartidoDao} (lecturas que consume la UI).
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class GastoCompartidoIntegrationTest {

    private static final long SALDO_INICIAL = 200_000L;
    private static final long EGRESO = 100_000L;
    private static final String FECHA_REPARTO = "2026-10-26T12:00:00Z";
    private static final String FECHA_COBRO = "2026-10-26T13:00:00Z";

    private AppDatabase db;
    private GastoCompartidoRepository repository;
    private GastoCompartidoDao dao;

    private long cuentaId;
    private long juanId;
    private long mariaId;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                        RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new GastoCompartidoRepository(db);
        dao = db.gastoCompartidoDao();

        db.cuentaDao().insert(new Cuenta("Efectivo", "efectivo", SALDO_INICIAL, "CLP"));
        cuentaId = primerId("SELECT id FROM cuentas LIMIT 1");
        juanId = db.contactoDao().insert(
                new com.localcash.data.entities.Contacto("Juan Pérez", null, null, null));
        mariaId = db.contactoDao().insert(
                new com.localcash.data.entities.Contacto("María García", null, null, null));
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void testFlujoCompletoRepartoCobroYSaldoCuadrado() {
        // 1) El usuario paga el egreso completo: asiento + saldo (flujo 007/008).
        long egresoId = db.transaccionDao().insert(new Transaccion(
                cuentaId, "EGRESO", EGRESO, "Cena compartida", 1_761_480_000_000L));
        db.cuentaDao().sumarSaldo(cuentaId, -EGRESO);
        assertEquals(SALDO_INICIAL - EGRESO, saldo());

        // 2) Reparto N+1 calculado por el dominio: usuario + Juan + María.
        List<PartePropuesta> partes = GastoCompartidoService.dividirConUsuario(
                EGRESO, java.util.Arrays.asList(juanId, mariaId));
        assertEquals(3, partes.size());
        assertTrue(partes.get(0).isDelUsuario());

        // Se registran dentro de la misma transacción del egreso.
        db.runInTransaction(() ->
                repository.insertTodas(egresoId, partes, FECHA_REPARTO));

        // Solo las partes de contactos se persisten: la del usuario es implícita.
        List<GastoCompartido> guardadas = dao.getByTransaccion(egresoId);
        assertEquals(2, guardadas.size());
        assertEquals(EGRESO - partes.get(0).getMontoAsignado(), sumaPartes(guardadas));
        for (GastoCompartido guardada : guardadas) {
            assertEquals(GastoCompartido.ESTADO_PENDIENTE, guardada.getEstado());
        }

        // El resumen de deudas muestra las dos partes pendientes.
        List<ResumenDeuda> resumenInicial = dao.resumenDeudasPorContacto();
        assertEquals(2, resumenInicial.size());

        // 3) Cobro de la primera parte.
        long parteJuan = guardadas.get(0).getId();
        long montoJuan = guardadas.get(0).getMontoAsignado();
        assertNull(repository.marcarPagada(parteJuan, FECHA_COBRO));

        // 4) La parte queda pagada con su fecha.
        GastoCompartido cobrada = dao.getByIdSync(parteJuan);
        assertNotNull(cobrada);
        assertEquals(GastoCompartido.ESTADO_PAGADO, cobrada.getEstado());
        assertEquals(FECHA_COBRO, cobrada.getFechaPago());

        // 5) El ingreso automático se asentó en la cuenta del egreso.
        Transaccion ingreso = ultimoIngreso();
        assertNotNull(ingreso);
        assertEquals("INGRESO", ingreso.getTipo());
        // El historial debe decir QUIÉN devolvió: prefijo + nombre del contacto.
        assertEquals(GastoCompartidoRepository.DESCRIPCION_DEVOLUCION
                        + GastoCompartidoRepository.SEPARADOR_PAGADOR + "Juan Pérez",
                ingreso.getConcepto());
        assertEquals(cuentaId, ingreso.getCuentaId());
        assertEquals(montoJuan, ingreso.getMonto());

        // 6) Saldo = inicial − egreso + parte cobrada, y sigue vigente I-1.
        assertEquals(SALDO_INICIAL - EGRESO + montoJuan, saldo());
        assertEquals(saldo(),
                SALDO_INICIAL + sumaPorTipo("INGRESO") - sumaPorTipo("EGRESO"));

        // 7) Cobrar la misma parte otra vez no mueve nada (INV-3).
        assertEquals(com.localcash.domain.error.ErrorCode.PARTE_YA_PAGADA,
                repository.marcarPagada(parteJuan, "2026-10-26T14:00:00Z"));
        assertEquals(SALDO_INICIAL - EGRESO + montoJuan, saldo());

        // 8) El resumen y el detalle solo reflejan la parte pendiente restante.
        List<ResumenDeuda> resumenFinal = dao.resumenDeudasPorContacto();
        assertEquals(1, resumenFinal.size());
        assertEquals(mariaId, resumenFinal.get(0).getContactoId());

        List<DeudaContacto> deudasMaría = LiveDataTestUtil.awaitValue(
                dao.getDeudasPendientesLive(mariaId));
        assertEquals(1, deudasMaría.size());
        assertEquals("Cena compartida", deudasMaría.get(0).getDescripcion());
        assertEquals(guardadas.get(1).getMontoAsignado(),
                deudasMaría.get(0).getMontoAsignado());

        // 9) El historial de pagos del contacto ya figura, aunque solo quede una deuda.
        List<com.localcash.data.model.PagoContacto> pagosJuan =
                LiveDataTestUtil.awaitValue(dao.getPagosPorContactoLive(juanId));
        assertEquals(1, pagosJuan.size());
        assertEquals("Cena compartida", pagosJuan.get(0).getDescripcion());
        assertEquals(montoJuan, pagosJuan.get(0).getMontoAsignado());
        assertEquals(FECHA_COBRO, pagosJuan.get(0).getFechaPago());
        assertTrue(LiveDataTestUtil
                .awaitValue(dao.getPagosPorContactoLive(mariaId)).isEmpty());

        // 10) Nada rompió la integridad referencial.
        assertTrue(foreignKeyCheckLimpio());
    }

    @Test
    public void testPagoMultipleRotulaCadaDevolucionConSuContacto() {
        long egresoId = db.transaccionDao().insert(new Transaccion(
                cuentaId, "EGRESO", EGRESO, "Asado del sábado", 1_761_480_000_000L));
        db.runInTransaction(() -> repository.insertTodas(egresoId,
                GastoCompartidoService.dividirConUsuario(
                        EGRESO, java.util.Arrays.asList(juanId, mariaId)),
                FECHA_REPARTO));
        db.cuentaDao().sumarSaldo(cuentaId, -EGRESO);
        List<Long> ids = new ArrayList<>();
        for (GastoCompartido guardada : dao.getByTransaccion(egresoId)) {
            ids.add(Long.valueOf(guardada.getId()));
        }
        assertEquals(2, ids.size());

        assertNull(repository.marcarTodasPagadas(ids, FECHA_COBRO));

        Cursor cursor = db.getOpenHelper().getWritableDatabase().query(
                "SELECT concepto FROM transacciones WHERE tipo = 'INGRESO' ORDER BY id ASC");
        List<String> conceptos = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                conceptos.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        assertEquals(2, conceptos.size());
        assertTrue(conceptos.get(0).endsWith("Juan Pérez"));
        assertTrue(conceptos.get(1).endsWith("María García"));
        // 100.000 entre 3 partes (usuario + 2): 33.334 para el usuario, 33.333
        // para cada contacto. Se acreditan solo las dos devoluciones reales.
        assertEquals(SALDO_INICIAL - EGRESO + 33_333L + 33_333L, saldo());
    }

    @Test
    public void testBorrarElEgresoEliminaElRepartoYElResumenQuedaVacio() {
        long egresoId = db.transaccionDao().insert(new Transaccion(
                cuentaId, "EGRESO", EGRESO, "Cena compartida", 1_761_480_000_000L));
        db.runInTransaction(() -> repository.insertTodas(egresoId,
                GastoCompartidoService.dividirConUsuario(
                        EGRESO, java.util.Arrays.asList(juanId, mariaId)),
                FECHA_REPARTO));
        assertEquals(2, dao.getByTransaccion(egresoId).size());

        db.getOpenHelper().getWritableDatabase()
                .delete("transacciones", "id = ?", new String[]{String.valueOf(egresoId)});

        assertEquals(0, dao.getByTransaccion(egresoId).size());
        assertTrue(dao.resumenDeudasPorContacto().isEmpty());
        assertTrue(foreignKeyCheckLimpio());
    }

    // ---------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------

    private long saldo() {
        return db.cuentaDao().getCuentaById(cuentaId).getSaldo();
    }

    private Transaccion ultimoIngreso() {
        Cursor cursor = db.getOpenHelper().getWritableDatabase().query(
                "SELECT * FROM transacciones WHERE tipo = 'INGRESO' ORDER BY id DESC LIMIT 1");
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            Transaccion movimiento = new Transaccion();
            movimiento.setCuentaId(cursor.getLong(cursor.getColumnIndexOrThrow("cuentaId")));
            movimiento.setTipo(cursor.getString(cursor.getColumnIndexOrThrow("tipo")));
            movimiento.setMonto(cursor.getLong(cursor.getColumnIndexOrThrow("monto")));
            movimiento.setConcepto(cursor.getString(cursor.getColumnIndexOrThrow("concepto")));
            return movimiento;
        } finally {
            cursor.close();
        }
    }

    private long sumaPorTipo(String tipo) {
        Cursor cursor = db.getOpenHelper().getWritableDatabase().query(
                "SELECT COALESCE(SUM(monto), 0) FROM transacciones WHERE tipo = ?",
                new Object[]{tipo});
        try {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0L;
        } finally {
            cursor.close();
        }
    }

    private long sumaPartes(List<GastoCompartido> partes) {
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