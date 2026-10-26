package com.localcash.domain.model;

import com.localcash.domain.error.ErrorCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado de validar el reparto que el usuario está construyendo en pantalla.
 * <p>
 * Objeto de valor inmutable que el ViewModel calcula <b>en vivo</b> para que la
 * UI muestre la suma de las partes contra el total del egreso y habilite o no
 * el botón "Registrar". La Activity no decide reglas: lee este estado
 * (`.agent.md` §5.1).
 * </p>
 * <p>
 * Los montos son {@code long} en pesos CLP; nunca se calcula en la capa de
 * presentación.
 * </p>
 */
public final class EstadoReparto {

    private final boolean compartido;
    private final ModoDivision modo;
    private final long montoTotal;
    private final long sumaParcial;
    private final List<PartePropuesta> partes;
    private final boolean valido;
    private final ErrorCode error;

    private EstadoReparto(boolean compartido, ModoDivision modo, long montoTotal,
                          long sumaParcial, List<PartePropuesta> partes,
                          boolean valido, ErrorCode error) {
        this.compartido = compartido;
        this.modo = modo;
        this.montoTotal = montoTotal;
        this.sumaParcial = sumaParcial;
        this.partes = partes == null
                ? Collections.<PartePropuesta>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(partes));
        this.valido = valido;
        this.error = error;
    }

    /**
     * El movimiento no es compartido: no hay reparto que validar.
     *
     * @param montoTotal importe del movimiento en pesos CLP
     * @return estado válido sin partes
     */
    public static EstadoReparto sinReparto(long montoTotal) {
        return new EstadoReparto(false, ModoDivision.IGUAL, montoTotal, 0L,
                Collections.<PartePropuesta>emptyList(), true, null);
    }

    /**
     * Reparto aceptado por el dominio: la suma de las partes iguala el total.
     *
     * @param montoTotal importe del egreso en pesos CLP
     * @param modo       división usada
     * @param partes     partes calculadas/validadas
     * @return estado válido con las partes
     */
    public static EstadoReparto validoCon(long montoTotal, ModoDivision modo,
                                          List<PartePropuesta> partes) {
        long suma = 0L;
        if (partes != null) {
            for (PartePropuesta parte : partes) {
                suma += parte.getMontoAsignado();
            }
        }
        return new EstadoReparto(true, modo, montoTotal, suma, partes, true, null);
    }

    /**
     * Reparto rechazado por el dominio.
     *
     * @param montoTotal   importe del egreso en pesos CLP
     * @param modo         división intentada
     * @param partes       partes parciales capturadas (para no perder lo escrito)
     * @param sumaParcial  suma de lo capturado hasta ahora
     * @param error        código que explica el rechazo (nunca {@code null})
     * @return estado inválido
     */
    public static EstadoReparto rechazado(long montoTotal, ModoDivision modo,
                                          List<PartePropuesta> partes, long sumaParcial,
                                          ErrorCode error) {
        return new EstadoReparto(true, modo, montoTotal, sumaParcial, partes, false, error);
    }

    public boolean isCompartido() {
        return compartido;
    }

    public ModoDivision getModo() {
        return modo;
    }

    public long getMontoTotal() {
        return montoTotal;
    }

    public long getSumaParcial() {
        return sumaParcial;
    }

    public List<PartePropuesta> getPartes() {
        return partes;
    }

    /**
     * @return cantidad de contactos con parte asignada (o intentada)
     */
    public int getNumeroPartes() {
        return partes.size();
    }

    public boolean isValido() {
        return valido;
    }

    /**
     * @return el {@link ErrorCode} del rechazo, o {@code null} si el reparto es
     *         válido (incluye el caso "no compartido")
     */
    public ErrorCode getError() {
        return error;
    }
}