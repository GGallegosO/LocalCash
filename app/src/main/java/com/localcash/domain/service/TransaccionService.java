package com.localcash.domain.service;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.Cuenta;
import com.localcash.domain.model.Transaccion;
import com.localcash.domain.model.TipoTransaccion;

import java.math.BigDecimal;

/**
 * Lógica de negocio pura de las transacciones: validar un movimiento contra la
 * cuenta y calcular el saldo resultante.
 * <p>
 * Sin estado (métodos {@code static}): es lógica pura, testeable sin dispositivo
 * y sin dependencias de Android ni de Room (`.agent.md` §5.1). La validación y
 * el cálculo son pasos separados: el flujo típico (repositorio) llama primero a
 * {@link #validarTransaccion(Transaccion, Cuenta)} y, si no lanza, persiste el
 * movimiento y aplica {@link #calcularNuevoSaldo(Cuenta, Transaccion)}.
 * </p>
 * <p>
 * Toda la aritmética es {@link BigDecimal}: prohibidos {@code double} y
 * {@code float} (`.agent.md` §6.10). Los montos están en pesos CLP directos
 * (sin centavos), por lo que las comparaciones usan {@code compareTo}
 * (comparación por valor, independiente de la escala).
 * </p>
 */
public final class TransaccionService {

    private TransaccionService() {
        // Utilidad pura sin estado: no debe instanciarse.
    }

    /**
     * Valida un movimiento contra la cuenta antes de persistirlo.
     * <p>
     * Reglas, en orden:
     * </p>
     * <ol>
     *   <li>{@code transaccion} no nula y con {@code monto} no nulo → si no,
     *       {@link ErrorCode#MONTO_INVALIDO}</li>
     *   <li>{@code monto > 0} → si no, {@link ErrorCode#MONTO_INVALIDO}</li>
     *   <li>{@code cuenta} no nula → si no, {@link ErrorCode#CUENTA_NO_ENCONTRADA}
     *       (referencia a cuenta inexistente)</li>
     *   <li>Si es {@link TipoTransaccion#EGRESO}: {@code saldo >= monto} → si no,
     *       {@link ErrorCode#SALDO_INSUFICIENTE}</li>
     * </ol>
     *
     * @param transaccion movimiento a validar (no nulo)
     * @param cuenta      cuenta afectada (no nula)
     * @throws BusinessRuleException con el {@code ErrorCode} de la regla violada
     */
    public static void validarTransaccion(Transaccion transaccion, Cuenta cuenta) {
        if (transaccion == null || transaccion.getMonto() == null) {
            throw new BusinessRuleException(
                    ErrorCode.MONTO_INVALIDO,
                    "Transaccion o monto nulo: no hay importe que validar");
        }

        if (transaccion.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(
                    ErrorCode.MONTO_INVALIDO,
                    "Monto debe ser mayor que 0 (monto=" + transaccion.getMonto() + ")");
        }

        if (cuenta == null) {
            throw new BusinessRuleException(
                    ErrorCode.CUENTA_NO_ENCONTRADA,
                    "Cuenta nula: referencia a cuenta inexistente");
        }

        if (transaccion.getTipo() == TipoTransaccion.EGRESO
                && cuenta.getSaldo().compareTo(transaccion.getMonto()) < 0) {
            throw new BusinessRuleException(
                    ErrorCode.SALDO_INSUFICIENTE,
                    "Egreso supera el saldo disponible (saldo=" + cuenta.getSaldo()
                            + ", monto=" + transaccion.getMonto() + ")");
        }
    }

    /**
     * Calcula el saldo de la cuenta después de aplicar el movimiento.
     * <p>
     * INGRESO suma, EGRESO resta, siempre con {@link BigDecimal} (nunca
     * {@code double}). Asume un movimiento ya validado por
     * {@link #validarTransaccion(Transaccion, Cuenta)}.
     * </p>
     *
     * @param cuenta      cuenta cuyo saldo se calcula (no nula)
     * @param transaccion movimiento a aplicar (no nulo)
     * @return saldo resultante en pesos CLP ({@code BigDecimal})
     * @throws BusinessRuleException con {@link ErrorCode#CUENTA_NO_ENCONTRADA}
     *                               si la cuenta es nula, o con
     *                               {@link ErrorCode#MONTO_INVALIDO} si el
     *                               movimiento o su monto son nulos
     * @throws IllegalStateException si el tipo de movimiento está fuera del
     *                               dominio cerrado (error de programación, no
     *                               de negocio)
     */
    public static BigDecimal calcularNuevoSaldo(Cuenta cuenta, Transaccion transaccion) {
        if (cuenta == null) {
            throw new BusinessRuleException(
                    ErrorCode.CUENTA_NO_ENCONTRADA,
                    "Cuenta nula: no hay saldo que calcular");
        }

        if (transaccion == null || transaccion.getMonto() == null) {
            throw new BusinessRuleException(
                    ErrorCode.MONTO_INVALIDO,
                    "Transaccion o monto nulo: no hay saldo que calcular");
        }

        if (transaccion.getTipo() == TipoTransaccion.INGRESO) {
            return cuenta.getSaldo().add(transaccion.getMonto());
        }

        if (transaccion.getTipo() == TipoTransaccion.EGRESO) {
            return cuenta.getSaldo().subtract(transaccion.getMonto());
        }

        // Tipo fuera del dominio (null o desconocido): error de programación.
        throw new IllegalStateException(
                "Tipo de transaccion fuera del dominio: " + transaccion.getTipo());
    }
}
