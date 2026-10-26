package com.localcash.ui.cuentas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.EstadoReparto;
import com.localcash.domain.model.ModoDivision;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

/**
 * Tests JVM (Robolectric, sin dispositivo) del estado de reparto compartido
 * que expone {@link TransaccionViewModel} para el formulario de egreso
 * (Feature 012, T11/T12).
 * <p>
 * La hoja inferior no se infla aquí: su verificación es la prueba manual en
 * emulador. Lo que se comprueba es el contrato que la UI consume: visibilidad,
 * suma en vivo y el veredicto del dominio propagado como {@code ErrorCode}.
 * </p>
 */
@RunWith(RobolectricTestRunner.class)
public class TransaccionViewModelDivisionTest {

    @Test
    public void elViewModelCalculaLaSumaEnVivo() {
        TransaccionViewModel vm = new TransaccionViewModel(RuntimeEnvironment.getApplication());

        vm.setMontoTotal(100_000L);
        vm.setCompartido(true);
        vm.setContactosSeleccionados(Arrays.asList(1L, 2L, 3L));

        EstadoReparto igual = vm.getEstadoReparto().getValue();
        assertNotNull(igual);
        assertTrue(igual.isValido());
        // N+1: usuario implícito + 3 contactos = 4 partes de 25.000.
        assertEquals(4, igual.getNumeroPartes());
        assertEquals(100_000L, igual.getSumaParcial());
        assertEquals(ModoDivision.IGUAL, igual.getModo());
        assertEquals(25_000L, igual.getPartes().get(0).getMontoAsignado());
        assertEquals(true, igual.getPartes().get(0).isDelUsuario());

        vm.setModoDivision(ModoDivision.PERSONALIZADA);
        vm.setMontoPersonalizado(1L, 30_000L);
        vm.setMontoPersonalizado(2L, 15_000L);
        vm.setMontoPersonalizado(3L, 0L);
        EstadoReparto incompleto = vm.getEstadoReparto().getValue();
        assertNotNull(incompleto);
        assertEquals(45_000L, incompleto.getSumaParcial());
        assertEquals(false, incompleto.isValido());
        assertEquals(ErrorCode.MONTO_PARTE_INVALIDO, incompleto.getError());

        vm.setMontoPersonalizado(2L, 15_000L);
        vm.setMontoPersonalizado(3L, 55_000L);
        EstadoReparto cuadrado = vm.getEstadoReparto().getValue();
        assertNotNull(cuadrado);
        assertTrue(cuadrado.isValido());
        assertEquals(100_000L, cuadrado.getSumaParcial());
        assertNull(cuadrado.getError());
        // La personalizada no reparte con el usuario: 3 partes suman el total.
        assertEquals(3, vm.partesParaRegistrar().size());

        // Suma menor: el resto es la parte implícita del usuario.
        vm.setMontoPersonalizado(3L, 10_000L);
        EstadoReparto conResto = vm.getEstadoReparto().getValue();
        assertNotNull(conResto);
        assertTrue(conResto.isValido());
        assertEquals(55_000L, conResto.getSumaParcial());
        assertEquals(45_000L, conResto.getParteUsuario());
    }

    @Test
    public void desmarcarCompartidoDevuelveEstadoSinReparto() {
        TransaccionViewModel vm = new TransaccionViewModel(RuntimeEnvironment.getApplication());
        vm.setMontoTotal(50_000L);
        vm.setCompartido(true);
        vm.setContactosSeleccionados(Arrays.asList(7L));
        vm.setCompartido(false);

        EstadoReparto estado = vm.getEstadoReparto().getValue();
        assertNotNull(estado);
        assertEquals(false, estado.isCompartido());
        assertTrue(estado.isValido());
        assertNull(vm.partesParaRegistrar());
    }
}