package com.localcash.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.Cuenta;
import com.localcash.domain.model.TipoCuenta;
import com.localcash.domain.model.TipoTransaccion;
import com.localcash.domain.model.Transaccion;

import org.junit.Test;

import java.math.BigDecimal;

/**
 * Tests unitarios JVM puros (JUnit 4, sin Android ni Robolectric) para la
 * lógica de negocio de {@link TransaccionService}.
 * <p>
 * Los 4 tests mínimos de {@code tasks.md} T8 (ingreso suma, egreso resta,
 * saldo insuficiente, monto inválido) más tests complementarios de frontera y
 * de nulos para garantizar la cobertura ≥70% del criterio de salida (`.agent.md` §5.3).
 * </p>
 * <p>
 * Todo el dinero usa {@link BigDecimal} con literales String (sin {@code double}):
 * montos en pesos CLP directos. Las aserciones comparan con {@code compareTo}
 * (por valor, no por escala) — misma regla que el propio servicio.
 * </p>
 */
public class TransaccionServiceTest {

    /** Fecha fija (epoch millis) para tests deterministas. */
    private static final long FECHA = 1_700_000_000_000L;

    // ---------------------------------------------------------------
    // Tests mínimos de tasks.md T8
    // ---------------------------------------------------------------

    @Test
    public void testIngresoAumentaSaldo() {
        Cuenta cuenta = cuentaConSaldo("1000000");
        Transaccion ingreso = transaccion(TipoTransaccion.INGRESO, "50000");

        // Registrar = validar primero (no debe lanzar) y luego calcular.
        TransaccionService.validarTransaccion(ingreso, cuenta);
        BigDecimal nuevoSaldo = TransaccionService.calcularNuevoSaldo(cuenta, ingreso);

        assertEquals("El INGRESO debe sumar exactamente", 0,
                nuevoSaldo.compareTo(new BigDecimal("1050000")));
    }

    @Test
    public void testEgresoDisminuyeSaldo() {
        Cuenta cuenta = cuentaConSaldo("1000000");
        Transaccion egreso = transaccion(TipoTransaccion.EGRESO, "50000");

        TransaccionService.validarTransaccion(egreso, cuenta);
        BigDecimal nuevoSaldo = TransaccionService.calcularNuevoSaldo(cuenta, egreso);

        assertEquals("El EGRESO debe restar exactamente", 0,
                nuevoSaldo.compareTo(new BigDecimal("950000")));
    }

    @Test
    public void testEgresoConSaldoInsuficienteLanzaExcepcion() {
        Cuenta cuenta = cuentaConSaldo("10000");
        Transaccion egreso = transaccion(TipoTransaccion.EGRESO, "50000");

        BusinessRuleException excepcion = assertThrows(
                "Un egreso mayor al saldo debe ser bloqueado",
                BusinessRuleException.class,
                () -> TransaccionService.validarTransaccion(egreso, cuenta));

        assertEquals(ErrorCode.SALDO_INSUFICIENTE, excepcion.getErrorCode());
    }

    @Test
    public void testMontoInvalidoLanzaExcepcion() {
        Cuenta cuenta = cuentaConSaldo("1000000");

        // Monto exactamente 0 → inválido.
        BusinessRuleException montoCero = assertThrows(
                "Monto 0 debe ser bloqueado",
                BusinessRuleException.class,
                () -> TransaccionService.validarTransaccion(
                        transaccion(TipoTransaccion.INGRESO, "0"), cuenta));
        assertEquals(ErrorCode.MONTO_INVALIDO, montoCero.getErrorCode());

        // Monto negativo → inválido (regla monto <= 0).
        BusinessRuleException montoNegativo = assertThrows(
                "Monto negativo debe ser bloqueado",
                BusinessRuleException.class,
                () -> TransaccionService.validarTransaccion(
                        transaccion(TipoTransaccion.EGRESO, "-50000"), cuenta));
        assertEquals(ErrorCode.MONTO_INVALIDO, montoNegativo.getErrorCode());
    }

    // ---------------------------------------------------------------
    // Tests complementarios (cobertura ≥70%: nulos, frontera, dominio)
    // ---------------------------------------------------------------

    @Test
    public void testCuentaNulaLanzaExcepcion() {
        Transaccion egreso = transaccion(TipoTransaccion.EGRESO, "50000");

        BusinessRuleException enValidacion = assertThrows(
                "Validar con cuenta nula debe ser bloqueado",
                BusinessRuleException.class,
                () -> TransaccionService.validarTransaccion(egreso, null));
        assertEquals(ErrorCode.CUENTA_NO_ENCONTRADA, enValidacion.getErrorCode());

        BusinessRuleException enCalculo = assertThrows(
                "Calcular saldo con cuenta nula debe ser bloqueado",
                BusinessRuleException.class,
                () -> TransaccionService.calcularNuevoSaldo(null, egreso));
        assertEquals(ErrorCode.CUENTA_NO_ENCONTRADA, enCalculo.getErrorCode());
    }

    @Test
    public void testTransaccionNulaLanzaExcepcion() {
        Cuenta cuenta = cuentaConSaldo("1000000");

        BusinessRuleException enValidacion = assertThrows(
                "Validar con transacción nula debe ser bloqueado",
                BusinessRuleException.class,
                () -> TransaccionService.validarTransaccion(null, cuenta));
        assertEquals(ErrorCode.MONTO_INVALIDO, enValidacion.getErrorCode());

        BusinessRuleException enCalculo = assertThrows(
                "Calcular saldo con transacción nula debe ser bloqueado",
                BusinessRuleException.class,
                () -> TransaccionService.calcularNuevoSaldo(cuenta, null));
        assertEquals(ErrorCode.MONTO_INVALIDO, enCalculo.getErrorCode());
    }

    @Test
    public void testEgresoIgualAlSaldoPermitido() {
        // Frontera de la regla: saldo >= monto → exactamente igual pasa.
        Cuenta cuenta = cuentaConSaldo("50000");
        Transaccion egreso = transaccion(TipoTransaccion.EGRESO, "50000");

        TransaccionService.validarTransaccion(egreso, cuenta);
        BigDecimal nuevoSaldo = TransaccionService.calcularNuevoSaldo(cuenta, egreso);

        assertEquals("Un egreso igual al saldo deja saldo 0 (no negativo)", 0,
                nuevoSaldo.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testTipoFueraDeDominioLanzaIllegalState() {
        // Un tipo fuera del dominio cerrado es error de programación, no de negocio.
        Cuenta cuenta = cuentaConSaldo("1000000");
        Transaccion sinTipo = new Transaccion(0L, 1L, null, new BigDecimal("50000"),
                "Movimiento sin tipo", FECHA);

        assertThrows(
                "Tipo null o fuera de dominio debe fallar ruidosamente",
                IllegalStateException.class,
                () -> TransaccionService.calcularNuevoSaldo(cuenta, sinTipo));
    }

    // ---------------------------------------------------------------
    // Helpers de construcción de modelos de prueba (BigDecimal puro)
    // ---------------------------------------------------------------

    private static Cuenta cuentaConSaldo(String saldoEnPesos) {
        return new Cuenta(1L, "Efectivo Demo", TipoCuenta.EFECTIVO, new BigDecimal(saldoEnPesos));
    }

    private static Transaccion transaccion(TipoTransaccion tipo, String montoEnPesos) {
        return new Transaccion(0L, 1L, tipo, new BigDecimal(montoEnPesos),
                "Movimiento de prueba", FECHA);
    }
}
