package com.localcash.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.room.Room;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Contacto;
import com.localcash.testing.LiveDataTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

/**
 * Tests de JVM (Robolectric + Room in-memory) de {@link ContactoDao}.
 * <p>
 * Verifican el CRUD, la unicidad de teléfono y email (ignorando
 * {@code NULL}), el borrado exitoso, y que {@code PRAGMA foreign_keys} y
 * {@code foreign_key_check} están activos y limpios.
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class ContactoDaoTest {

    private AppDatabase db;
    private ContactoDao contactoDao;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                        RuntimeEnvironment.getApplication(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        contactoDao = db.contactoDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void testInsertYGetById() {
        long id = contactoDao.insert(crearContacto("Juan Pérez", "555-0100",
                "juan@correo.cl", "Vecino"));

        Contacto guardado = contactoDao.getByIdSync(id);

        assertNotNull(guardado);
        assertEquals("Juan Pérez", guardado.getNombre());
        assertEquals("555-0100", guardado.getTelefono());
        assertEquals("juan@correo.cl", guardado.getEmail());
        assertEquals("Vecino", guardado.getNotas());
    }

    @Test
    public void testGetAllOrdenadoPorNombre() {
        contactoDao.insert(crearContacto("Zeta", null, null, null));
        contactoDao.insert(crearContacto("Alpha", "111", null, null));
        contactoDao.insert(crearContacto("Beta", null, "b@test.cl", null));

        List<Contacto> todos = LiveDataTestUtil.awaitValue(contactoDao.getAll());

        assertEquals(3, todos.size());
        assertEquals("Alpha", todos.get(0).getNombre());
        assertEquals("Beta", todos.get(1).getNombre());
        assertEquals("Zeta", todos.get(2).getNombre());
    }

    @Test
    public void testUpdatePersisteTodosLosCampos() {
        long id = contactoDao.insert(crearContacto("Juan", null, null, null));
        Contacto contacto = contactoDao.getByIdSync(id);
        contacto.setNombre("Juan editado");
        contacto.setTelefono("555-999");
        contacto.setEmail("juan@nuevo.cl");
        contacto.setNotas("Nota actualizada");

        contactoDao.update(contacto);

        Contacto actualizado = contactoDao.getByIdSync(id);
        assertEquals("Juan editado", actualizado.getNombre());
        assertEquals("555-999", actualizado.getTelefono());
        assertEquals("juan@nuevo.cl", actualizado.getEmail());
        assertEquals("Nota actualizada", actualizado.getNotas());
    }

    @Test
    public void testTelefonoEsUniqueExacto() {
        contactoDao.insert(crearContacto("Primero", "555-0100", null, null));

        try {
            contactoDao.insert(crearContacto("Segundo", "555-0100", null, null));
            fail("El índice único debía rechazar teléfono duplicado");
        } catch (android.database.sqlite.SQLiteConstraintException esperada) {
            // Comportamiento esperado: teléfono duplicado exacto.
        }
    }

    @Test
    public void testVariosContactosSinTelefonoConviven() {
        contactoDao.insert(crearContacto("Primero", null, null, null));
        contactoDao.insert(crearContacto("Segundo", null, null, null));
        contactoDao.insert(crearContacto("Tercero", null, null, null));

        List<Contacto> todos = LiveDataTestUtil.awaitValue(contactoDao.getAll());
        assertEquals(3, todos.size());
    }

    @Test
    public void testEmailEsUniqueExacto() {
        contactoDao.insert(crearContacto("Primero", null, "juan@correo.cl", null));

        try {
            contactoDao.insert(crearContacto("Segundo", null, "juan@correo.cl", null));
            fail("El índice único debía rechazar email duplicado exacto");
        } catch (android.database.sqlite.SQLiteConstraintException esperada) {
            // Comportamiento esperado: email duplicado exacto.
        }
    }

    @Test
    public void testVariosContactosSinEmailConviven() {
        contactoDao.insert(crearContacto("Primero", null, null, null));
        contactoDao.insert(crearContacto("Segundo", null, null, null));

        List<Contacto> todos = LiveDataTestUtil.awaitValue(contactoDao.getAll());
        assertEquals(2, todos.size());
    }

    @Test
    public void testExisteTelefonoDevuelveTrueCuandoExiste() {
        contactoDao.insert(crearContacto("Juan", "555-0100", null, null));

        assertEquals(1, contactoDao.existeTelefono("555-0100"));
    }

    @Test
    public void testExisteTelefonoDevuelveFalseCuandoNoExiste() {
        assertEquals(0, contactoDao.existeTelefono("555-999"));
    }

    @Test
    public void testExisteEmailDevuelveTrueCuandoExiste() {
        contactoDao.insert(crearContacto("Juan", null, "juan@correo.cl", null));

        assertEquals(1, contactoDao.existeEmail("juan@correo.cl"));
        assertEquals(1, contactoDao.existeEmail("JUAN@CORREO.CL"));
    }

    @Test
    public void testExisteEmailDevuelveFalseCuandoNoExiste() {
        assertEquals(0, contactoDao.existeEmail("otro@correo.cl"));
    }

    @Test
    public void testEliminarContactoSinCompromisos() {
        long id = contactoDao.insert(crearContacto("Juan", "555-0100", null, null));

        contactoDao.delete(contactoDao.getByIdSync(id));

        assertNull(contactoDao.getByIdSync(id));
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
        contactoDao.insert(crearContacto("Juan", "555-0100", "juan@correo.cl", null));

        assertTrue(db.getOpenHelper().getWritableDatabase()
                .query("PRAGMA foreign_key_check").getCount() == 0);
    }

    /**
     * Construye una entidad de contacto de prueba.
     */
    private Contacto crearContacto(String nombre, String telefono, String email, String notas) {
        return new Contacto(nombre, telefono, email, notas);
    }
}
