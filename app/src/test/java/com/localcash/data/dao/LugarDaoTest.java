package com.localcash.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.room.Room;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Cuenta;
import com.localcash.data.entities.ItemLugar;
import com.localcash.data.entities.Lugar;
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
 * Tests de JVM (Robolectric + Room in-memory) de {@link LugarDao} (T28).
 * <p>
 * Verifican el CRUD, las consultas por estado y caja envolvente, la unicidad
 * del vínculo con un movimiento, la cascada de {@code ITEMS_LUGAR} y el
 * comportamiento de las claves foráneas ({@code PRAGMA foreign_keys}).
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class LugarDaoTest {

    private AppDatabase db;
    private LugarDao lugarDao;
    private ItemLugarDao itemLugarDao;
    private CuentaDao cuentaDao;
    private TransaccionDao transaccionDao;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                        RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        lugarDao = db.lugarDao();
        itemLugarDao = db.itemLugarDao();
        cuentaDao = db.cuentaDao();
        transaccionDao = db.transaccionDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void testInsertYGetById() {
        long id = lugarDao.insert(crearLugar(null, "Feria", "abierto", -33.44, -70.66));

        Lugar guardado = lugarDao.getByIdSync(id);

        assertNotNull(guardado);
        assertEquals("Feria", guardado.getNombre());
        assertEquals("otro", guardado.getTipoLugar());
        assertEquals("📍", guardado.getEmoji());
        assertEquals(-33.44, guardado.getLatitud(), 0.0000001);
        assertEquals("abierto", guardado.getEstado());
        assertNull(guardado.getTransaccionId());
    }

    @Test
    public void testGetAbiertosYCerrados() {
        lugarDao.insert(crearLugar(null, "Abierto", "abierto", -33.44, -70.66));
        lugarDao.insert(crearLugar(null, "Cerrado", "cerrado", -33.45, -70.67));

        List<Lugar> abiertos = LiveDataTestUtil.awaitValue(lugarDao.getAbiertos());
        List<Lugar> cerrados = LiveDataTestUtil.awaitValue(lugarDao.getCerrados());
        List<Lugar> todos = LiveDataTestUtil.awaitValue(lugarDao.getAll());

        assertEquals(1, abiertos.size());
        assertEquals("Abierto", abiertos.get(0).getNombre());
        assertEquals(1, cerrados.size());
        assertEquals("Cerrado", cerrados.get(0).getNombre());
        assertEquals(2, todos.size());
    }

    @Test
    public void testUpdatePersisteTodosLosCampos() {
        long id = lugarDao.insert(crearLugar(null, "Feria", "abierto", -33.44, -70.66));
        Lugar lugar = lugarDao.getByIdSync(id);
        lugar.setNombre("Feria editada");
        lugar.setTipoLugar("supermercado");
        lugar.setEmoji("🛒");
        lugar.setDescripcion("Abre hasta las 21:00");
        lugar.setLatitud(-33.45);
        lugar.setLongitud(-70.67);

        lugarDao.update(lugar);

        Lugar actualizado = lugarDao.getByIdSync(id);
        assertEquals("Feria editada", actualizado.getNombre());
        assertEquals("supermercado", actualizado.getTipoLugar());
        assertEquals("🛒", actualizado.getEmoji());
        assertEquals("Abre hasta las 21:00", actualizado.getDescripcion());
        assertEquals(-33.45, actualizado.getLatitud(), 0.0000001);
    }

    @Test
    public void testUpdateEstadoNoBorraElPin() {
        long id = lugarDao.insert(crearLugar(null, "Feria", "abierto", -33.44, -70.66));

        lugarDao.updateEstado(id, "cerrado");

        Lugar cerrado = lugarDao.getByIdSync(id);
        assertNotNull(cerrado);
        assertEquals("cerrado", cerrado.getEstado());
        assertEquals(1, LiveDataTestUtil.awaitValue(lugarDao.getCerrados()).size());
        assertEquals(0, LiveDataTestUtil.awaitValue(lugarDao.getAbiertos()).size());
    }

    @Test
    public void testGetEnRangoFiltraPorCajaEnvolvente() {
        lugarDao.insert(crearLugar(null, "Dentro", "abierto", -33.44, -70.66));
        lugarDao.insert(crearLugar(null, "Fuera", "abierto", -34.00, -71.00));

        List<Lugar> enRango = LiveDataTestUtil.awaitValue(
                lugarDao.getEnRango(-33.50, -33.40, -70.70, -70.60));

        assertEquals(1, enRango.size());
        assertEquals("Dentro", enRango.get(0).getNombre());
    }

    @Test
    public void testGetByTransaccionSync() {
        long cuentaId = insertarCuenta();
        long transaccionId = transaccionDao.insert(
                new Transaccion(cuentaId, "EGRESO", 25_000L, "Compra", 1_000L));
        long lugarId = lugarDao.insert(
                crearLugar(transaccionId, "Compra", "abierto", -33.44, -70.66));

        Lugar vinculado = lugarDao.getByTransaccionSync(transaccionId);

        assertNotNull(vinculado);
        assertEquals(lugarId, vinculado.getId());
        assertEquals(Long.valueOf(transaccionId), vinculado.getTransaccionId());
    }

    @Test
    public void testVinculoConMovimientoEsUnico() {
        long cuentaId = insertarCuenta();
        long transaccionId = transaccionDao.insert(
                new Transaccion(cuentaId, "EGRESO", 25_000L, "Compra", 1_000L));
        lugarDao.insert(crearLugar(transaccionId, "Primero", "abierto", -33.44, -70.66));

        try {
            lugarDao.insert(crearLugar(transaccionId, "Segundo", "abierto", -33.45, -70.67));
            fail("El índice único debía rechazar un segundo pin para el mismo movimiento");
        } catch (android.database.sqlite.SQLiteConstraintException esperada) {
            // Comportamiento esperado: un movimiento tiene como máximo un pin.
        }
    }

    @Test
    public void testEliminarLugarBorraSusItemsEnCascada() {
        long lugarId = lugarDao.insert(crearLugar(null, "Feria", "abierto", -33.44, -70.66));
        itemLugarDao.insert(new ItemLugar(lugarId, "Manzanas", false));
        itemLugarDao.insert(new ItemLugar(lugarId, "Peras", true));

        lugarDao.delete(lugarDao.getByIdSync(lugarId));

        assertNull(lugarDao.getByIdSync(lugarId));
        assertTrue(itemLugarDao.getByLugarIdSync(lugarId).isEmpty());
    }

    @Test
    public void testEliminarMovimientoDejaElPinIndependiente() {
        long cuentaId = insertarCuenta();
        long transaccionId = transaccionDao.insert(
                new Transaccion(cuentaId, "EGRESO", 25_000L, "Compra", 1_000L));
        long lugarId = lugarDao.insert(
                crearLugar(transaccionId, "Compra", "abierto", -33.44, -70.66));

        db.getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM transacciones WHERE id = " + transaccionId);

        Lugar huerfano = lugarDao.getByIdSync(lugarId);
        assertNotNull(huerfano);
        assertNull(huerfano.getTransaccionId());
    }

    @Test
    public void testEmojiMulticodigoSeConserva() {
        // Un emoji con selectores de variación no cabe en un solo code point.
        // La columna es TEXT: debe sobrevivir sin truncarse ni romper el pin.
        String emoji = "🏳️‍🌈";
        Lugar lugar = crearLugar(null, "Bandera", "abierto", -33.44, -70.66);
        lugar.setEmoji(emoji);

        long id = lugarDao.insert(lugar);

        assertEquals(emoji, lugarDao.getByIdSync(id).getEmoji());
    }

    @Test
    public void testForeignKeysActivasEnLaConexion() {
        android.database.Cursor cursor = db.getOpenHelper().getWritableDatabase()
                .query("PRAGMA foreign_keys");
        try {
            assertTrue(cursor.moveToFirst());
            assertEquals(1, cursor.getInt(0));
        } finally {
            cursor.close();
        }
    }

    @Test
    public void testForeignKeysCheckLimpio() {
        long cuentaId = insertarCuenta();
        long transaccionId = transaccionDao.insert(
                new Transaccion(cuentaId, "EGRESO", 25_000L, "Compra", 1_000L));
        long lugarId = lugarDao.insert(
                crearLugar(transaccionId, "Compra", "abierto", -33.44, -70.66));
        itemLugarDao.insert(new ItemLugar(lugarId, "Leche", false));

        assertTrue(db.getOpenHelper().getWritableDatabase()
                .query("PRAGMA foreign_key_check").getCount() == 0);
    }

    /**
     * Construye una entidad de prueba.
     *
     * @param transaccionId vínculo opcional con un movimiento
     * @param nombre        nombre visible
     * @param estado        {@code abierto} o {@code cerrado}
     * @param latitud       latitud WGS84
     * @param longitud      longitud WGS84
     * @return entidad lista para insertar
     */
    private Lugar crearLugar(Long transaccionId, String nombre, String estado,
                             double latitud, double longitud) {
        return new Lugar(transaccionId, nombre, "otro", "📍", null,
                latitud, longitud, estado);
    }

    /**
     * Inserta una cuenta de prueba y devuelve su id.
     *
     * @return id de la cuenta
     */
    private long insertarCuenta() {
        cuentaDao.insert(new Cuenta("Efectivo", "EFECTIVO", 1_000_000L, "CLP"));
        return LiveDataTestUtil.awaitValue(cuentaDao.getAll()).get(0).getId();
    }
}
