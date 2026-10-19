package com.localcash.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests unitarios JVM puros (JUnit 4, sin Android) para la validación del value
 * object {@link Ubicacion}.
 * <p>
 * Cubren las dos reglas de negocio: "ambas o ninguna" y rango de coordenadas,
 * además del estado "sin ubicación" representado por {@code null}.
 * </p>
 */
public class UbicacionTest {

    private static final double DELTA = 0.0;

    @Test
    public void crear_conAmbasCoordenadas_devuelveUbicacion() {
        Ubicacion ubicacion = Ubicacion.crear(-33.4489, -70.6693, "Santiago Centro");

        assertEquals(-33.4489, ubicacion.getLatitud(), DELTA);
        assertEquals(-70.6693, ubicacion.getLongitud(), DELTA);
        assertEquals("Santiago Centro", ubicacion.getDireccionLugar());
    }

    @Test
    public void crear_conDireccionNula_esValido() {
        Ubicacion ubicacion = Ubicacion.crear(-33.4489, -70.6693, null);

        assertNull(ubicacion.getDireccionLugar());
    }

    @Test
    public void crear_conAmbasNulas_devuelveNull() {
        assertNull(Ubicacion.crear(null, null, "sin coordenadas"));
    }

    @Test
    public void crear_conSoloLatitud_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Ubicacion.crear(-33.4489, null, null));
    }

    @Test
    public void crear_conSoloLongitud_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Ubicacion.crear(null, -70.6693, null));
    }

    @Test
    public void crear_conLatitudFueraDeRango_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Ubicacion.crear(90.0001, 0.0, null));
        assertThrows(IllegalArgumentException.class,
                () -> Ubicacion.crear(-90.0001, 0.0, null));
    }

    @Test
    public void crear_conLongitudFueraDeRango_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Ubicacion.crear(0.0, 180.0001, null));
        assertThrows(IllegalArgumentException.class,
                () -> Ubicacion.crear(0.0, -180.0001, null));
    }

    @Test
    public void crear_conLimitesExactos_esValido() {
        Ubicacion ubicacion = Ubicacion.crear(90.0, 180.0, null);

        assertEquals(90.0, ubicacion.getLatitud(), DELTA);
        assertEquals(180.0, ubicacion.getLongitud(), DELTA);
    }

    @Test
    public void crear_conNaN_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> Ubicacion.crear(Double.NaN, 0.0, null));
        assertThrows(IllegalArgumentException.class,
                () -> Ubicacion.crear(0.0, Double.NaN, null));
    }

    @Test
    public void vacia_siempreDevuelveNull() {
        assertNull(Ubicacion.vacia());
    }

    @Test
    public void equals_y_hashCode_sonCoherentes() {
        Ubicacion una = Ubicacion.crear(-33.4489, -70.6693, "Santiago");
        Ubicacion otra = Ubicacion.crear(-33.4489, -70.6693, "Santiago");

        assertEquals(una, otra);
        assertEquals(una.hashCode(), otra.hashCode());
        assertTrue(!una.equals((Ubicacion) null));
    }
}
