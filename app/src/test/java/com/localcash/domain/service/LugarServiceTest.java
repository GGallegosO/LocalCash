package com.localcash.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.EstadoLugar;
import com.localcash.domain.model.ItemLugar;
import com.localcash.domain.model.Lugar;
import com.localcash.domain.model.TipoLugar;
import com.localcash.domain.model.Ubicacion;

import org.junit.Test;

/**
 * Tests de JVM pura (sin Robolectric ni dispositivo) de {@link LugarService}.
 * <p>
 * Cubren las validaciones de T27: nombre vacío, tipo nulo, emoji vacío,
 * coordenadas fuera de rango, transiciones de estado y la regla de posición
 * inmutable de un pin vinculado a un movimiento.
 * </p>
 */
public class LugarServiceTest {

    private static final Ubicacion SANTIAGO = new Ubicacion(-33.4489, -70.6693);

    @Test
    public void testValidarLugarAceptaUnPinCompleto() {
        LugarService.validarLugar(lugar(null, "Feria", TipoLugar.OTRO, "📍",
                EstadoLugar.ABIERTO));
    }

    @Test
    public void testValidarLugarRechazaNombreVacio() {
        assertThrows(BusinessRuleException.class, () -> LugarService.validarLugar(
                lugar(null, "   ", TipoLugar.OTRO, "📍", EstadoLugar.ABIERTO)));
    }

    @Test
    public void testValidarLugarRechazaNombreNulo() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> LugarService.validarLugar(
                        lugar(null, null, TipoLugar.OTRO, "📍", EstadoLugar.ABIERTO)));
        assertEquals(ErrorCode.LUGAR_INVALIDO, error.getErrorCode());
    }

    @Test
    public void testValidarLugarRechazaTipoNulo() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> LugarService.validarLugar(
                        lugar(null, "Feria", null, "📍", EstadoLugar.ABIERTO)));
        assertEquals(ErrorCode.LUGAR_INVALIDO, error.getErrorCode());
    }

    @Test
    public void testValidarLugarRechazaEmojiVacio() {
        assertThrows(BusinessRuleException.class, () -> LugarService.validarLugar(
                lugar(null, "Feria", TipoLugar.OTRO, " ", EstadoLugar.ABIERTO)));
    }

    @Test
    public void testValidarLugarRechazaEstadoNulo() {
        assertThrows(BusinessRuleException.class, () -> LugarService.validarLugar(
                lugar(null, "Feria", TipoLugar.OTRO, "📍", null)));
    }

    @Test
    public void testValidarLugarRechazaUbicacionNula() {
        Lugar sinUbicacion = new Lugar(0L, null, "Feria", TipoLugar.OTRO, "📍",
                null, null, EstadoLugar.ABIERTO);
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> LugarService.validarLugar(sinUbicacion));
        assertEquals(ErrorCode.UBICACION_INVALIDA, error.getErrorCode());
    }

    @Test
    public void testUbicacionRechazaLatitudFueraDeRango() {
        assertThrows(BusinessRuleException.class, () -> new Ubicacion(91.0, -70.0));
        assertThrows(BusinessRuleException.class, () -> new Ubicacion(-91.0, -70.0));
    }

    @Test
    public void testUbicacionRechazaLongitudFueraDeRango() {
        assertThrows(BusinessRuleException.class, () -> new Ubicacion(-33.0, 181.0));
        assertThrows(BusinessRuleException.class, () -> new Ubicacion(-33.0, -181.0));
    }

    @Test
    public void testUbicacionRechazaValoresNoFinitos() {
        assertThrows(BusinessRuleException.class,
                () -> new Ubicacion(Double.NaN, -70.0));
        assertThrows(BusinessRuleException.class,
                () -> new Ubicacion(-33.0, Double.POSITIVE_INFINITY));
    }

    @Test
    public void testAlternarEstado() {
        assertEquals(EstadoLugar.CERRADO, LugarService.alternarEstado(EstadoLugar.ABIERTO));
        assertEquals(EstadoLugar.ABIERTO, LugarService.alternarEstado(EstadoLugar.CERRADO));
        assertThrows(BusinessRuleException.class, () -> LugarService.alternarEstado(null));
    }

    @Test
    public void testPinIndependientePuedeMoverse() {
        LugarService.validarMovimiento(
                lugar(null, "Feria", TipoLugar.OTRO, "📍", EstadoLugar.ABIERTO),
                new Ubicacion(-33.45, -70.66));
    }

    @Test
    public void testPinVinculadoNoPuedeMoverse() {
        Lugar vinculado = lugar(7L, "Compra", TipoLugar.OTRO, "📍", EstadoLugar.ABIERTO);
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> LugarService.validarMovimiento(vinculado, new Ubicacion(-33.45, -70.66)));
        assertEquals(ErrorCode.LUGAR_INVALIDO, error.getErrorCode());
    }

    @Test
    public void testMovimientoConUbicacionNulaEsInvalido() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> LugarService.validarMovimiento(
                        lugar(null, "Feria", TipoLugar.OTRO, "📍", EstadoLugar.ABIERTO), null));
        assertEquals(ErrorCode.UBICACION_INVALIDA, error.getErrorCode());
    }

    @Test
    public void testValidarItemAceptaDescripcion() {
        LugarService.validarItem(new ItemLugar(0L, 5L, "Leche", false));
    }

    @Test
    public void testValidarItemRechazaDescripcionVacia() {
        assertThrows(BusinessRuleException.class,
                () -> LugarService.validarItem(new ItemLugar(0L, 5L, "  ", false)));
    }

    @Test
    public void testValidarItemRechazaLugarInvalido() {
        assertThrows(BusinessRuleException.class,
                () -> LugarService.validarItem(new ItemLugar(0L, 0L, "Leche", false)));
    }

    @Test
    public void testLugarVinculadoSeReconocePorTransaccion() {
        assertTrue(lugar(9L, "Compra", TipoLugar.OTRO, "📍", EstadoLugar.ABIERTO)
                .estaVinculadoAMovimiento());
        assertFalse(lugar(null, "Feria", TipoLugar.OTRO, "📍", EstadoLugar.ABIERTO)
                .estaVinculadoAMovimiento());
    }

    @Test
    public void testConEstadoNoMutaElOriginal() {
        Lugar abierto = lugar(null, "Feria", TipoLugar.OTRO, "📍", EstadoLugar.ABIERTO);
        Lugar cerrado = abierto.conEstado(EstadoLugar.CERRADO);

        assertEquals(EstadoLugar.ABIERTO, abierto.getEstado());
        assertEquals(EstadoLugar.CERRADO, cerrado.getEstado());
        assertEquals(abierto.getId(), cerrado.getId());
        assertNotNull(cerrado.getUbicacion());
    }

    /**
     * Construye un pin de prueba con ubicación fija.
     *
     * @param transaccionId vínculo opcional con un movimiento
     * @param nombre        nombre visible
     * @param tipo          tipo de lugar (puede ser {@code null} a propósito)
     * @param emoji         emoji visible
     * @param estado        estado del pin (puede ser {@code null} a propósito)
     * @return pin de prueba
     */
    private Lugar lugar(Long transaccionId, String nombre, TipoLugar tipo,
                        String emoji, EstadoLugar estado) {
        return new Lugar(0L, transaccionId, nombre, tipo, emoji, null, SANTIAGO, estado);
    }
}
