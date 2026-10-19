package com.localcash.data.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

/**
 * Test JVM (Robolectric) del catálogo de tipos de lugar personalizados
 * (Feature 010, Fase 2.5).
 * <p>
 * Comprueba alta, lectura ordenada, rechazo de vacíos/duplicados y baja sobre
 * {@link android.content.SharedPreferences}. No toca Room ni la red.
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class TiposLugarPreferenciasTest {

    /** Nombre interno del archivo de preferencias (contrato del helper). */
    private static final String PREFS_NAME = "tipos_lugar_prefs";

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void agregaYLeePersonalizadosEnOrden() {
        assertTrue(TiposLugarPreferencias.agregarPersonalizado(context, "Feria"));
        assertTrue(TiposLugarPreferencias.agregarPersonalizado(context, "  Mayorista  "));

        List<String> tipos = TiposLugarPreferencias.obtenerPersonalizados(context);
        assertEquals(2, tipos.size());
        assertEquals("Feria", tipos.get(0));
        assertEquals("Mayorista", tipos.get(1));
    }

    @Test
    public void rechazaVaciosYDuplicadosSinDistinguirMayusculas() {
        assertFalse(TiposLugarPreferencias.agregarPersonalizado(context, "   "));
        assertTrue(TiposLugarPreferencias.agregarPersonalizado(context, "Feria"));
        assertFalse(TiposLugarPreferencias.agregarPersonalizado(context, "feria"));

        List<String> tipos = TiposLugarPreferencias.obtenerPersonalizados(context);
        assertEquals(1, tipos.size());
        assertEquals("Feria", tipos.get(0));
    }

    @Test
    public void eliminaSoloElTipoIndicado() {
        assertTrue(TiposLugarPreferencias.agregarPersonalizado(context, "Feria"));
        assertTrue(TiposLugarPreferencias.agregarPersonalizado(context, "Mayorista"));

        TiposLugarPreferencias.eliminarPersonalizado(context, "FERIA");

        List<String> tipos = TiposLugarPreferencias.obtenerPersonalizados(context);
        assertEquals(1, tipos.size());
        assertEquals("Mayorista", tipos.get(0));
        assertFalse(TiposLugarPreferencias.esPersonalizado(context, "Feria"));
        assertTrue(TiposLugarPreferencias.esPersonalizado(context, "Mayorista"));
    }
}
