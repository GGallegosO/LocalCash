package com.localcash.data.dao;

import static org.junit.Assert.assertEquals;

import androidx.room.Room;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Cuenta;
import com.localcash.data.entities.Transaccion;
import com.localcash.testing.LiveDataTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

/**
 * Tests de JVM (Robolectric + Room in-memory, sin dispositivo) para {@link TransaccionDao}.
 * <p>
 * Los montos son {@code long} en pesos CLP directos (sin centavos, sin double)
 * — `.agent.md` §6.10. Las fechas son epoch millis para poder verificar el
 * orden {@code ORDER BY fecha DESC} de forma determinista.
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class TransaccionDaoTest {

    private AppDatabase db;
    private CuentaDao cuentaDao;
    private TransaccionDao transaccionDao;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                        RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        cuentaDao = db.cuentaDao();
        transaccionDao = db.transaccionDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void testInsertAndGetByCuentaId() {
        long cuentaId = insertarCuenta();

        transaccionDao.insert(new Transaccion(
                cuentaId, "INGRESO", 50_000L, "Sueldo de prueba", 1_700_000_000_000L));

        List<Transaccion> movimientos =
                LiveDataTestUtil.awaitValue(transaccionDao.getByCuentaId(cuentaId));

        assertEquals(1, movimientos.size());
        assertEquals(cuentaId, movimientos.get(0).getCuentaId());
        assertEquals("INGRESO", movimientos.get(0).getTipo());
        assertEquals(50_000L, movimientos.get(0).getMonto());
        assertEquals("Sueldo de prueba", movimientos.get(0).getConcepto());
        assertEquals(1_700_000_000_000L, movimientos.get(0).getFecha());
    }

    @Test
    public void testGetAll() {
        long cuentaId = insertarCuenta();

        // Fechas fuera de orden a propósito: el resultado debe quedar fecha DESC.
        transaccionDao.insert(new Transaccion(
                cuentaId, "EGRESO", 10_000L, "Primero", 1_000L));
        transaccionDao.insert(new Transaccion(
                cuentaId, "INGRESO", 30_000L, "Tercero", 3_000L));
        transaccionDao.insert(new Transaccion(
                cuentaId, "EGRESO", 20_000L, "Segundo", 2_000L));

        List<Transaccion> todas =
                LiveDataTestUtil.awaitValue(transaccionDao.getAll(), lista -> lista.size() == 3);

        assertEquals(3, todas.size());
        assertEquals(3_000L, todas.get(0).getFecha());
        assertEquals(2_000L, todas.get(1).getFecha());
        assertEquals(1_000L, todas.get(2).getFecha());
        assertEquals("Tercero", todas.get(0).getConcepto());
        assertEquals("Primero", todas.get(2).getConcepto());
    }

    @Test
    public void testGetByIdDevuelveElMovimientoVinculado() {
        long cuentaId = insertarCuenta();
        long id = transaccionDao.insert(new Transaccion(
                cuentaId, "EGRESO", 25_000L, "Compra con ubicacion", 1_750_000_000_000L));

        Transaccion movimiento = LiveDataTestUtil.awaitValue(transaccionDao.getById(id));

        assertEquals(id, movimiento.getId());
        assertEquals("Compra con ubicacion", movimiento.getConcepto());
        assertEquals(1_750_000_000_000L, movimiento.getFecha());
    }

    /**
     * Inserta una cuenta de prueba y retorna su id generado.
     */
    private long insertarCuenta() {
        cuentaDao.insert(new Cuenta("Efectivo", "EFECTIVO", 1_000_000L, "CLP"));
        return LiveDataTestUtil.awaitValue(cuentaDao.getAll()).get(0).getId();
    }
}
