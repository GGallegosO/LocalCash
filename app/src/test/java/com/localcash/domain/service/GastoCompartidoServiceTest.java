package com.localcash.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.PartePropuesta;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests unitarios JVM puros (JUnit 4, sin Android ni Robolectric) de
 * {@link GastoCompartidoService} (Feature 012, T8).
 * <p>
 * Cubren los criterios CA-1..CA-6 de {@code spec.md} y los casos borde 1–6 de
 * su §6: reparto igual exacto y con residuo, monto insuficiente, cero
 * contactos, división personalizada que cuadra y que no, partes no positivas y
 * contactos duplicados.
 * </p>
 * <p>
 * Todo el dinero es {@code long} en pesos CLP directos: nunca {@code double}.
 * Las aserciones de monto son igualdades exactas, que es justo lo que la
 * aritmética entera del servicio garantiza (INV-2).
 * </p>
 */
public class GastoCompartidoServiceTest {

    // ---------------------------------------------------------------
    // CA-1 / CA-3 — reparto igual exacto y con residuo (INV-2)
    // ---------------------------------------------------------------

    @Test
    public void testRepartoIgualExactoEntreDos() {
        List<PartePropuesta> partes = GastoCompartidoService.dividirIgual(
                100_000L, Arrays.asList(10L, 20L));

        assertEquals(2, partes.size());
        assertEquals(10L, partes.get(0).getContactoId());
        assertEquals(50_000L, partes.get(0).getMontoAsignado());
        assertEquals(20L, partes.get(1).getContactoId());
        assertEquals(50_000L, partes.get(1).getMontoAsignado());
        assertEquals(100_000L, sumar(partes));
    }

    @Test
    public void testRepartoIgualConResiduoEntreTres() {
        List<PartePropuesta> partes = GastoCompartidoService.dividirIgual(
                100_000L, Arrays.asList(10L, 20L, 30L));

        assertEquals(3, partes.size());
        assertEquals(33_334L, partes.get(0).getMontoAsignado());
        assertEquals(33_333L, partes.get(1).getMontoAsignado());
        assertEquals(33_333L, partes.get(2).getMontoAsignado());
        assertEquals("La suma de las partes debe igualar el total", 100_000L, sumar(partes));
    }

    @Test
    public void testRepartoIgualReparteElResiduoDeAUnoPorContacto() {
        // 100.000 = 7 × 14.285 + 5 → los cinco primeros reciben 1 peso extra.
        List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L);
        List<PartePropuesta> partes = GastoCompartidoService.dividirIgual(100_000L, ids);

        long[] esperados = {14_286L, 14_286L, 14_286L, 14_286L, 14_286L, 14_285L, 14_285L};
        assertEquals(7, partes.size());
        for (int i = 0; i < esperados.length; i++) {
            assertEquals("Parte " + i, esperados[i], partes.get(i).getMontoAsignado());
        }
        assertEquals(100_000L, sumar(partes));
    }

    @Test
    public void testRepartoIgualUnSoloContactoRecibeTodo() {
        List<PartePropuesta> partes = GastoCompartidoService.dividirIgual(
                90_000L, Collections.singletonList(7L));

        assertEquals(1, partes.size());
        assertEquals(7L, partes.get(0).getContactoId());
        assertEquals(90_000L, partes.get(0).getMontoAsignado());
    }

    @Test
    public void testRepartoIgualConservaElOrdenDeLosContactos() {
        List<PartePropuesta> partes = GastoCompartidoService.dividirIgual(
                10L, Arrays.asList(30L, 10L, 20L));

        assertEquals(30L, partes.get(0).getContactoId());
        assertEquals(10L, partes.get(1).getContactoId());
        assertEquals(20L, partes.get(2).getContactoId());
        // 10 / 3 = 3 con residuo 1: el primero de la lista recibe el peso extra.
        assertEquals(4L, partes.get(0).getMontoAsignado());
        assertEquals(3L, partes.get(1).getMontoAsignado());
        assertEquals(3L, partes.get(2).getMontoAsignado());
    }

    @Test
    public void testSumaDePartesIgualAlTotalEnVariosMontosYContactos() {
        long[] montos = {5L, 99L, 100L, 101L, 9_999L, 1_000_000L, 92_147_483_647L};
        for (long monto : montos) {
            for (int n = 1; n <= 5; n++) {
                if (monto < n) {
                    continue;
                }
                List<PartePropuesta> partes = GastoCompartidoService.dividirIgual(
                        monto, idsDe(n));
                assertEquals("monto=" + monto + ", n=" + n, monto, sumar(partes));
                for (PartePropuesta parte : partes) {
                    assertTrue(parte.getMontoAsignado() > 0);
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // CA-4 — monto insuficiente para un peso por contacto
    // ---------------------------------------------------------------

    @Test
    public void testRepartoIgualRechazaMontoMenorQueLosContactos() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.dividirIgual(2L, Arrays.asList(1L, 2L, 3L)));

        assertEquals(ErrorCode.REPARTO_IGUAL_INSUFICIENTE, excepcion.getErrorCode());
    }

    // ---------------------------------------------------------------
    // CA-2 — INV-1: el reparto necesita contactos identificables
    // ---------------------------------------------------------------

    @Test
    public void testValidarRepartoRechazaListaNula() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarReparto(10_000L, null));

        assertEquals(ErrorCode.GASTO_SIN_CONTACTOS, excepcion.getErrorCode());
    }

    @Test
    public void testValidarRepartoRechazaListaVacia() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarReparto(
                        10_000L, Collections.emptyList()));

        assertEquals(ErrorCode.GASTO_SIN_CONTACTOS, excepcion.getErrorCode());
    }

    @Test
    public void testValidarRepartoRechazaContactoNulo() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarReparto(
                        10_000L, Arrays.asList(1L, null)));

        assertEquals(ErrorCode.GASTO_SIN_CONTACTOS, excepcion.getErrorCode());
    }

    @Test
    public void testValidarRepartoRechazaContactoNoPositivo() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarReparto(
                        10_000L, Arrays.asList(1L, 0L)));

        assertEquals(ErrorCode.GASTO_SIN_CONTACTOS, excepcion.getErrorCode());
    }

    @Test
    public void testValidarRepartoRechazaContactoDuplicado() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarReparto(
                        10_000L, Arrays.asList(5L, 5L)));

        assertEquals(ErrorCode.CONTACTO_DUPLICADO, excepcion.getErrorCode());
    }

    @Test
    public void testValidarRepartoRechazaMontoNoPositivo() {
        BusinessRuleException cero = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarReparto(
                        0L, Collections.singletonList(1L)));
        assertEquals(ErrorCode.MONTO_INVALIDO, cero.getErrorCode());

        BusinessRuleException negativo = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarReparto(
                        -1L, Collections.singletonList(1L)));
        assertEquals(ErrorCode.MONTO_INVALIDO, negativo.getErrorCode());
    }

    @Test
    public void testValidarRepartoAceptaUnRepartoValido() {
        // No lanza: es la única aserción posible para un método void.
        GastoCompartidoService.validarReparto(10_000L, Arrays.asList(1L, 2L, 3L));
    }

    @Test
    public void testDividirIgualPropagaLaValidacionComun() {
        BusinessRuleException sinContactos = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.dividirIgual(10_000L, Collections.emptyList()));
        assertEquals(ErrorCode.GASTO_SIN_CONTACTOS, sinContactos.getErrorCode());

        BusinessRuleException duplicado = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.dividirIgual(
                        10_000L, Arrays.asList(9L, 9L)));
        assertEquals(ErrorCode.CONTACTO_DUPLICADO, duplicado.getErrorCode());
    }

    // ---------------------------------------------------------------
    // CA-5 / CA-6 — división personalizada (INV-2)
    // ---------------------------------------------------------------

    @Test
    public void testPersonalizadoQueCuadraEsAceptado() {
        GastoCompartidoService.validarPersonalizado(50_000L, Arrays.asList(
                new PartePropuesta(1L, 30_000L),
                new PartePropuesta(2L, 20_000L)));
    }

    @Test
    public void testPersonalizadoRechazaSumaQueNoIgualaElTotal() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarPersonalizado(50_000L, Arrays.asList(
                        new PartePropuesta(1L, 30_000L),
                        new PartePropuesta(2L, 15_000L))));

        assertEquals(ErrorCode.REPARTO_NO_CUADRA, excepcion.getErrorCode());
    }

    @Test
    public void testPersonalizadoRechazaParteConMontoCero() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarPersonalizado(10_000L, Arrays.asList(
                        new PartePropuesta(1L, 0L),
                        new PartePropuesta(2L, 10_000L))));

        assertEquals(ErrorCode.MONTO_PARTE_INVALIDO, excepcion.getErrorCode());
    }

    @Test
    public void testPersonalizadoRechazaParteConMontoNegativo() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarPersonalizado(10_000L,
                        Collections.singletonList(new PartePropuesta(1L, -5_000L))));

        assertEquals(ErrorCode.MONTO_PARTE_INVALIDO, excepcion.getErrorCode());
    }

    @Test
    public void testPersonalizadoRechazaParteNula() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarPersonalizado(10_000L, Arrays.asList(
                        new PartePropuesta(1L, 10_000L),
                        null)));

        assertEquals(ErrorCode.MONTO_PARTE_INVALIDO, excepcion.getErrorCode());
    }

    @Test
    public void testPersonalizadoRechazaListaNulaOVacia() {
        BusinessRuleException nula = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarPersonalizado(10_000L, null));
        assertEquals(ErrorCode.GASTO_SIN_CONTACTOS, nula.getErrorCode());

        BusinessRuleException vacia = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarPersonalizado(
                        10_000L, Collections.emptyList()));
        assertEquals(ErrorCode.GASTO_SIN_CONTACTOS, vacia.getErrorCode());
    }

    @Test
    public void testPersonalizadoRechazaContactoRepetido() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarPersonalizado(10_000L, Arrays.asList(
                        new PartePropuesta(4L, 5_000L),
                        new PartePropuesta(4L, 5_000L))));

        assertEquals(ErrorCode.CONTACTO_DUPLICADO, excepcion.getErrorCode());
    }

    @Test
    public void testPersonalizadoRechazaContactoSinIdentificador() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarPersonalizado(10_000L,
                        Collections.singletonList(new PartePropuesta(0L, 10_000L))));

        assertEquals(ErrorCode.GASTO_SIN_CONTACTOS, excepcion.getErrorCode());
    }

    @Test
    public void testPersonalizadoRechazaMontoTotalNoPositivo() {
        BusinessRuleException excepcion = assertThrows(
                BusinessRuleException.class,
                () -> GastoCompartidoService.validarPersonalizado(0L,
                        Collections.singletonList(new PartePropuesta(1L, 1L))));

        assertEquals(ErrorCode.MONTO_INVALIDO, excepcion.getErrorCode());
    }

    @Test
    public void testPersonalizadoAceptaElResultadoDelRepartoIgual() {
        List<PartePropuesta> partes = GastoCompartidoService.dividirIgual(
                100_000L, Arrays.asList(1L, 2L, 3L));

        GastoCompartidoService.validarPersonalizado(100_000L, partes);
        assertEquals(100_000L, sumar(partes));
    }

    // ---------------------------------------------------------------
    // Apoyo
    // ---------------------------------------------------------------

    /**
     * @param partes partes del reparto
     * @return suma de los montos asignados en pesos CLP
     */
    private static long sumar(List<PartePropuesta> partes) {
        long suma = 0L;
        for (PartePropuesta parte : partes) {
            suma += parte.getMontoAsignado();
        }
        return suma;
    }

    /**
     * @param n cantidad de contactos
     * @return ids correlativos {@code 1..n}
     */
    private static List<Long> idsDe(int n) {
        List<Long> ids = new ArrayList<>(n);
        for (long i = 1; i <= n; i++) {
            ids.add(i);
        }
        return ids;
    }
}