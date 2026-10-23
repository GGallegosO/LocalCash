package com.localcash.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.ItemLugar;
import com.localcash.data.entities.Lugar;
import com.localcash.testing.LiveDataTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

/**
 * Tests de JVM (Robolectric + Room in-memory) de {@link ItemLugarDao} (T28).
 * <p>
 * La lista de cosas de un pin es un recordatorio: no guarda montos ni afecta
 * saldos. Se verifica el alta, la edición, el completado y el borrado, siempre
 * acotados al lugar al que pertenecen.
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class ItemLugarDaoTest {

    private AppDatabase db;
    private LugarDao lugarDao;
    private ItemLugarDao itemLugarDao;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                        RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        lugarDao = db.lugarDao();
        itemLugarDao = db.itemLugarDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void testInsertYGetByLugarIdEnOrdenDeCreacion() {
        long lugarId = insertarLugar("Feria");

        itemLugarDao.insert(new ItemLugar(lugarId, "Manzanas", false));
        itemLugarDao.insert(new ItemLugar(lugarId, "Peras", false));
        itemLugarDao.insert(new ItemLugar(lugarId, "Leche", true));

        List<ItemLugar> items = LiveDataTestUtil.awaitValue(
                itemLugarDao.getByLugarId(lugarId));

        assertEquals(3, items.size());
        assertEquals("Manzanas", items.get(0).getDescripcion());
        assertEquals("Peras", items.get(1).getDescripcion());
        assertEquals("Leche", items.get(2).getDescripcion());
        assertTrue(items.get(2).isCompletado());
    }

    @Test
    public void testItemsSeAislanPorLugar() {
        long feria = insertarLugar("Feria");
        long farmacia = insertarLugar("Farmacia");
        itemLugarDao.insert(new ItemLugar(feria, "Manzanas", false));
        itemLugarDao.insert(new ItemLugar(farmacia, "Ibuprofeno", false));

        List<ItemLugar> deFeria = itemLugarDao.getByLugarIdSync(feria);

        assertEquals(1, deFeria.size());
        assertEquals("Manzanas", deFeria.get(0).getDescripcion());
    }

    @Test
    public void testUpdateCompletadoNoCambiaLaDescripcion() {
        long lugarId = insertarLugar("Feria");
        long itemId = itemLugarDao.insert(new ItemLugar(lugarId, "Manzanas", false));

        itemLugarDao.updateCompletado(itemId, true);

        ItemLugar item = itemLugarDao.getByLugarIdSync(lugarId).get(0);
        assertTrue(item.isCompletado());
        assertEquals("Manzanas", item.getDescripcion());
    }

    @Test
    public void testUpdateDescripcion() {
        long lugarId = insertarLugar("Feria");
        long itemId = itemLugarDao.insert(new ItemLugar(lugarId, "Manzanas", false));
        ItemLugar item = itemLugarDao.getByLugarIdSync(lugarId).get(0);
        item.setDescripcion("Manzanas verdes");
        item.setCompletado(true);

        itemLugarDao.update(item);

        ItemLugar actualizado = itemLugarDao.getByLugarIdSync(lugarId).get(0);
        assertEquals(itemId, actualizado.getId());
        assertEquals("Manzanas verdes", actualizado.getDescripcion());
        assertTrue(actualizado.isCompletado());
    }

    @Test
    public void testDeleteEliminaSoloElItem() {
        long lugarId = insertarLugar("Feria");
        long manzanas = itemLugarDao.insert(new ItemLugar(lugarId, "Manzanas", false));
        itemLugarDao.insert(new ItemLugar(lugarId, "Peras", false));

        itemLugarDao.delete(itemLugarDao.getByLugarIdSync(lugarId).get(0));

        List<ItemLugar> restantes = itemLugarDao.getByLugarIdSync(lugarId);
        assertEquals(1, restantes.size());
        assertEquals("Peras", restantes.get(0).getDescripcion());
        assertFalse(restantes.get(0).getId() == manzanas);
    }

    @Test
    public void testAlternarCompletadoEnDominio() {
        com.localcash.domain.model.ItemLugar pendiente =
                new com.localcash.domain.model.ItemLugar(3L, 7L, "Pan", false);

        com.localcash.domain.model.ItemLugar completado = pendiente.alternarCompletado();

        assertFalse(pendiente.isCompletado());
        assertTrue(completado.isCompletado());
        assertEquals("Pan", completado.getDescripcion());
        assertEquals(7L, completado.getLugarId());
    }

    /**
     * Inserta un pin de prueba y devuelve su id.
     *
     * @param nombre nombre del lugar
     * @return id del lugar
     */
    private long insertarLugar(String nombre) {
        return lugarDao.insert(new Lugar(null, nombre, "otro", "📍", null,
                -33.44, -70.66, "abierto"));
    }
}
