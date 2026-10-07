package com.localcash.domain.error;

/**
 * Excepción de negocio que transporta un {@link ErrorCode} del dominio.
 * <p>
 * Se lanza cuando una operación viola una regla de negocio (`.agent.md` §5.4):
 * el dominio <b>nunca</b> produce texto para el usuario, solo el código. La
 * capa {@code ui/} traduce el {@code ErrorCode} a un mensaje accionable; el
 * mensaje de esta excepción es únicamente técnico, para log/diagnóstico.
 * </p>
 * <p>
 * Extiende {@link RuntimeException} (unchecked): las reglas de negocio son
 * invariantes que el flujo normal no debe incumplir, y un {@code catch}
 * explícito en cada llamada añadiría ruido sin valor. Sin dependencias de
 * Android ni de Room (`.agent.md` §5.1).
 * </p>
 */
public class BusinessRuleException extends RuntimeException {

    /** Código de dominio que explica el fallo. */
    private final ErrorCode errorCode;

    /**
     * Crea la excepción con el código de dominio y un mensaje técnico por defecto.
     *
     * @param errorCode código de negocio violado (nunca nulo)
     */
    public BusinessRuleException(ErrorCode errorCode) {
        this(errorCode, "Regla de negocio violada: " + errorCode.name());
    }

    /**
     * Crea la excepción con el código de dominio y un detalle técnico (para log).
     *
     * @param errorCode código de negocio violado (nunca nulo)
     * @param detail    detalle técnico del fallo; no es texto para el usuario
     */
    public BusinessRuleException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    /**
     * @return código de dominio que explica el fallo
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
