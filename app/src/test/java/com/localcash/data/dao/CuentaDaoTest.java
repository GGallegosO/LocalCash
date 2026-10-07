package com.localcash.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Cuenta;
import com.localcash.testing.LiveDataTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

/**
 * Tests de JVM (Robolectric + Room in-memory, sin dispositivo) para {@link CuentaDao}.
 * <p>
 * Ver tech-stack.md §4 fila "Integración": Robolectric permite verificar el CRUD
 * de DAOs sin emulador. Los montos son {@code long} en pesos CLP directos
 * (sin centavos, sin double) — `.agent.md` §6.10.
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class CuentaDaoTest {

    private AppDatabase db;
    private CuentaDao cuentaDao;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                        RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        cuentaDao = db.cuentaDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void testInsertAndGetAll() {
        cuentaDao.insert(new Cuenta("Efectivo", "EFECTIVO", 1_000_000L, "CLP"));

        List<Cuenta> cuentas = LiveDataTestUtil.awaitValue(cuentaDao.getAll());

        assertEquals(1, cuentas.size());
        assertEquals("Efectivo", cuentas.get(0).getNombre());
        assertEquals("EFECTIVO", cuentas.get(0).getTipo());
        assertEquals(1_000_000L, cuentas.get(0).getSaldo());
        assertEquals("CLP", cuentas.get(0).getMoneda());
    }

    @Test
    public void testUpdateSaldo() {
        cuentaDao.insert(new Cuenta("Ahorro", "AHORRO", 500_000L, "CLP"));
        List<Cuenta> antes = LiveDataTestUtil.awaitValue(cuentaDao.getAll());
        long cuentaId = antes.get(0).getId();

        cuentaDao.updateSaldo(cuentaId, 750_000L);

        List<Cuenta> despues = LiveDataTestUtil.awaitValue(
                cuentaDao.getAll(),
                cuentas -> !cuentas.isEmpty() && cuentas.get(0).getSaldo() == 750_000L);

        assertEquals(750_000L, despues.get(0).getSaldo());
        assertEquals(cuentaId, despues.get(0).getId());
    }

    @Test
    public void testDelete() {
        cuentaDao.insert(new Cuenta("Temporal", "EFECTIVO", 10_000L, "CLP"));
        List<Cuenta> insertadas = LiveDataTestUtil.awaitValue(cuentaDao.getAll());
        assertEquals(1, insertadas.size());

        cuentaDao.delete(insertadas.get(0));

        List<Cuenta> vacias = LiveDataTestUtil.awaitValue(cuentaDao.getAll(), List::isEmpty);
        assertTrue(vacias.isEmpty());
    }
}
