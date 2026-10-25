package com.localcash.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.Contacto;

import org.junit.Test;

/**
 * Tests de JVM pura (sin Robolectric ni dispositivo) de {@link ContactoService}.
 * <p>
 * Cubren las validaciones de nombre vacío, teléfono duplicado, email duplicado
 * y la regla de eliminación con deudas pendientes.
 * </p>
 */
public class ContactoServiceTest {

    @Test
    public void testValidarContactoAceptaContactoCompleto() {
        ContactoService.validarContacto(
                new Contacto(0L, "Juan Pérez", "555-0100", "juan@correo.cl", "Vecino"),
                false, false);
    }

    @Test
    public void testValidarContactoAceptaSoloNombre() {
        ContactoService.validarContacto(
                new Contacto(0L, "María García", null, null, null),
                false, false);
    }

    @Test
    public void testValidarContactoRechazaNombreVacio() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> ContactoService.validarContacto(
                        new Contacto(0L, "   ", null, null, null), false, false));
        assertEquals(ErrorCode.CONTACTO_NOMBRE_VACIO, error.getErrorCode());
    }

    @Test
    public void testValidarContactoRechazaNombreNulo() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> ContactoService.validarContacto(
                        new Contacto(0L, null, null, null, null), false, false));
        assertEquals(ErrorCode.CONTACTO_NOMBRE_VACIO, error.getErrorCode());
    }

    @Test
    public void testValidarContactoRechazaTelefonoDuplicado() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> ContactoService.validarContacto(
                        new Contacto(0L, "Otro", "555-0100", null, null), true, false));
        assertEquals(ErrorCode.CONTACTO_TELEFONO_DUPLICADO, error.getErrorCode());
    }

    @Test
    public void testValidarContactoRechazaEmailDuplicado() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> ContactoService.validarContacto(
                        new Contacto(0L, "Otro", null, "juan@correo.cl", null), false, true));
        assertEquals(ErrorCode.CONTACTO_EMAIL_DUPLICADO, error.getErrorCode());
    }

    @Test
    public void testValidarContactoRechazaContactoNulo() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> ContactoService.validarContacto(null, false, false));
        assertEquals(ErrorCode.CONTACTO_NOMBRE_VACIO, error.getErrorCode());
    }

    @Test
    public void testPuedeEliminarSinCompromisos() {
        assertTrue(ContactoService.puedeEliminar(false, false));
    }

    @Test
    public void testPuedeEliminarConComprasAsociadas() {
        assertFalse(ContactoService.puedeEliminar(true, false));
    }

    @Test
    public void testPuedeEliminarConDeudasPendientes() {
        assertFalse(ContactoService.puedeEliminar(false, true));
    }

    @Test
    public void testPuedeEliminarConAmbos() {
        assertFalse(ContactoService.puedeEliminar(true, true));
    }

    @Test
    public void testContactoEsInmutable() {
        Contacto original = new Contacto(0L, "Juan", "555-0100", "juan@correo.cl", null);
        // No hay setters: cualquier modificación requiere instancia nueva.
        assertEquals(0L, original.getId());
        assertEquals("Juan", original.getNombre());
        assertEquals("555-0100", original.getTelefono());
        assertEquals("juan@correo.cl", original.getEmail());
        assertNull(original.getNotas());
    }
}
