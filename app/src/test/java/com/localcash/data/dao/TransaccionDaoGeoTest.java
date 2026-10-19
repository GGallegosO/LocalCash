package com.localcash.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.room.Room;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.Cuenta;
import com.localcash.data.entities.Transaccion;
import com.localcash.domain.model.TipoTransaccion;
import com.localcash.domain.model.Ubicacion;
import com.localcash.testing.LiveDataTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tests de persistencia geográfica (Feature 009, T14) en JVM con Robolectric y
 * Room <b>in-memory</b>, sin dispositivo.
 * <p>
 * Verifican el par canónico {@link com.localcash.domain.model.Transaccion#toEntity()}
 * / {@link com.localcash.domain.model.Transaccion#fromEntity(Transaccion)} y la
 * consulta {@link TransaccionDao#getConUbicacion()}:
 * </p>
 * <ul>
 *   <li>Con ubicación válida: las columnas se persisten y se recuperan de vuelta.</li>
 *   <li>Sin ubicación: las columnas quedan {@code NULL} y el movimiento no entra en la consulta geo.</li>
 *   <li>Coordenada incompleta (fila corrupta): la consulta geo la excluye y el mapeo a
 *       dominio la rechaza ({@link IllegalArgumentException}).</li>
 * </ul>
 * <p>
 * Nota de nombres: la entidad Room y el modelo de dominio comparten el simple name
 * {@code Transaccion}; aquí se importa la entidad y se referencia el dominio por su nombre
 * completo (mismo criterio que {@code domain/model/Transaccion}).
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class TransaccionDaoGeoTest {

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
    public void conUbicacion_persisteYRecuperaCoordenadas() {
        long cuentaId = insertarCuenta();
        Ubicacion ubicacion = Ubicacion.crear(-33.4489, -70.6693, null);
        com.localcash.domain.model.Transaccion dominio =
                new com.localcash.domain.model.Transaccion(
                        0L, cuentaId, TipoTransaccion.EGRESO, new BigDecimal("12000"),
                        "Supermercado", 1_700_000_000_000L, ubicacion);

        // domain -> entity -> Room
        transaccionDao.insert(dominio.toEntity());

        List<Transaccion> geo = LiveDataTestUtil.awaitValue(
                transaccionDao.getConUbicacion(), lista -> lista.size() == 1);
        Transaccion entidad = geo.get(0);
        assertEquals(Double.valueOf(-33.4489), entidad.getLatitud());
        assertEquals(Double.valueOf(-70.6693), entidad.getLongitud());
        assertNull(entidad.getDireccionLugar());

        // Room -> entity -> domain (ida y vuelta completa)
        com.localcash.domain.model.Transaccion recuperado =
                com.localcash.domain.model.Transaccion.fromEntity(entidad);
        assertEquals(ubicacion, recuperado.getUbicacion());
    }

    @Test
    public void sinUbicacion_columnasNulasYFueraDeLaConsultaGeo() {
        long cuentaId = insertarCuenta();
        com.localcash.domain.model.Transaccion dominio =
                new com.localcash.domain.model.Transaccion(
                        0L, cuentaId, TipoTransaccion.INGRESO, new BigDecimal("50000"),
                        "Sueldo", 1_700_000_000_000L);

        transaccionDao.insert(dominio.toEntity());

        // La consulta geo excluye el movimiento sin coordenadas...
        List<Transaccion> geo = LiveDataTestUtil.awaitValue(transaccionDao.getConUbicacion());
        assertTrue(geo.isEmpty());

        // ...pero el movimiento sí existe, con las columnas geográficas en NULL.
        Transaccion entidad = LiveDataTestUtil.awaitValue(transaccionDao.getAll()).get(0);
        assertNull(entidad.getLatitud());
        assertNull(entidad.getLongitud());
        assertNull(entidad.getDireccionLugar());
        assertNull(com.localcash.domain.model.Transaccion.fromEntity(entidad).getUbicacion());
    }

    @Test
    public void coordenadaIncompleta_esExcluidaYRechazadaAlMapearADominio() {
        long cuentaId = insertarCuenta();
        // Fila corrupta: latitud sin longitud. Room no impone el CHECK; lo rechaza el dominio.
        transaccionDao.insert(new Transaccion(
                cuentaId, "EGRESO", 1000L, "Corrupta", 1_700_000_000_000L,
                -33.4489, null, null));

        // La consulta geo exige ambas coordenadas: la fila incompleta no aparece.
        List<Transaccion> geo = LiveDataTestUtil.awaitValue(transaccionDao.getConUbicacion());
        assertTrue(geo.isEmpty());

        // El mapeo a dominio falla ruidosamente ante coordenadas incompletas.
        Transaccion entidad = LiveDataTestUtil.awaitValue(transaccionDao.getAll()).get(0);
        assertThrows(IllegalArgumentException.class,
                () -> com.localcash.domain.model.Transaccion.fromEntity(entidad));
    }

    /**
     * Inserta una cuenta de prueba y retorna su id generado.
     */
    private long insertarCuenta() {
        cuentaDao.insert(new Cuenta("Efectivo", "EFECTIVO", 1_000_000L, "CLP"));
        return LiveDataTestUtil.awaitValue(cuentaDao.getAll()).get(0).getId();
    }
}
