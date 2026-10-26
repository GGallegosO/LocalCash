package com.localcash.domain.service;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.PartePropuesta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas de negocio del reparto de un egreso compartido (Feature 012).
 * <p>
 * Lógica pura, sin Android ni Room (`.agent.md` §5.1): recibe montos en
 * <b>pesos CLP</b> ({@code long}) y contactos, y decide si el reparto es válido
 * o cómo se divide. La UI y el repositorio orquestan; este servicio no escribe
 * en la base de datos.
 * </p>
 * <p>
 * Invariantes que hace cumplir ({@code spec.md} §5):
 * </p>
 * <ul>
 *   <li><b>INV-1:</b> un egreso compartido sin contactos no es válido.</li>
 *   <li><b>INV-2:</b> la suma de las partes iguala exactamente el total, al
 *       peso; el residuo de la división entera se reparte de a 1 CLP entre los
 *       primeros contactos de la lista.</li>
 * </ul>
 * <p>
 * Los fallos se expresan con {@link BusinessRuleException} que transporta el
 * {@link ErrorCode}; nunca con texto para el usuario (`.agent.md` §5.4).
 * </p>
 */
public final class GastoCompartidoService {

    private GastoCompartidoService() {
    }

    /**
     * Valida lo común a cualquier reparto: monto positivo, al menos un
     * contacto, ids identificables y sin repetir.
     *
     * @param montoTotal   importe del egreso en pesos CLP (positivo)
     * @param contactosIds contactos que participan del reparto
     * @throws BusinessRuleException {@link ErrorCode#MONTO_INVALIDO} si el monto
     *                               no es positivo, {@link ErrorCode#GASTO_SIN_CONTACTOS}
     *                               si no hay ningún contacto válido, o
     *                               {@link ErrorCode#CONTACTO_DUPLICADO} si un
     *                               contacto aparece más de una vez
     */
    public static void validarReparto(long montoTotal, List<Long> contactosIds) {
        if (montoTotal <= 0) {
            throw new BusinessRuleException(
                    ErrorCode.MONTO_INVALIDO,
                    "El monto del egreso compartido debe ser positivo: " + montoTotal);
        }
        if (contactosIds == null || contactosIds.isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.GASTO_SIN_CONTACTOS,
                    "Un egreso compartido necesita al menos un contacto");
        }

        Set<Long> vistos = new HashSet<>();
        for (Long contactoId : contactosIds) {
            if (contactoId == null || contactoId <= 0) {
                throw new BusinessRuleException(
                        ErrorCode.GASTO_SIN_CONTACTOS,
                        "Contacto sin identificar en el reparto: " + contactoId);
            }
            if (!vistos.add(contactoId)) {
                throw new BusinessRuleException(
                        ErrorCode.CONTACTO_DUPLICADO,
                        "Contacto repetido en el reparto: " + contactoId);
            }
        }
    }

    /**
     * Divide el egreso en partes iguales al peso.
     * <p>
     * La aritmética es entera: {@code base = montoTotal / n} y
     * {@code residuo = montoTotal % n}. Las primeras {@code residuo} partes de la
     * lista reciben 1 CLP adicional, de modo que la suma de las partes es
     * exactamente {@code montoTotal} (INV-2) sin usar {@code double}.
     * </p>
     *
     * @param montoTotal   importe del egreso en pesos CLP
     * @param contactosIds contactos que participan, en el orden de la lista
     * @return una parte por contacto, en el mismo orden de entrada
     * @throws BusinessRuleException con el código de la validación común, o
     *                               {@link ErrorCode#REPARTO_IGUAL_INSUFICIENTE}
     *                               si el monto no alcanza para 1 CLP por contacto
     */
    public static List<PartePropuesta> dividirIgual(long montoTotal, List<Long> contactosIds) {
        validarReparto(montoTotal, contactosIds);

        int numeroContactos = contactosIds.size();
        if (montoTotal < numeroContactos) {
            throw new BusinessRuleException(
                    ErrorCode.REPARTO_IGUAL_INSUFICIENTE,
                    "El monto " + montoTotal + " no alcanza para " + numeroContactos + " contactos");
        }

        long base = montoTotal / numeroContactos;
        long residuo = montoTotal % numeroContactos;

        List<PartePropuesta> partes = new ArrayList<>(numeroContactos);
        for (int i = 0; i < numeroContactos; i++) {
            long monto = base + (i < residuo ? 1L : 0L);
            partes.add(new PartePropuesta(contactosIds.get(i), monto));
        }
        return partes;
    }

    /**
     * Valida una división personalizada: montos positivos, un contacto a lo
     * sumo una vez y suma exacta del total (INV-2).
     *
     * @param montoTotal importe del egreso en pesos CLP
     * @param partes     partes propuestas por el usuario
     * @throws BusinessRuleException {@link ErrorCode#MONTO_INVALIDO} si el total
     *                               no es positivo, {@link ErrorCode#GASTO_SIN_CONTACTOS}
     *                               si no hay partes o un contacto no se identifica,
     *                               {@link ErrorCode#MONTO_PARTE_INVALIDO} si alguna
     *                               parte no es positiva, {@link ErrorCode#CONTACTO_DUPLICADO}
     *                               si un contacto se repite, o
     *                               {@link ErrorCode#REPARTO_NO_CUADRA} si la suma
     *                               no iguala el total
     */
    public static void validarPersonalizado(long montoTotal, List<PartePropuesta> partes) {
        if (montoTotal <= 0) {
            throw new BusinessRuleException(
                    ErrorCode.MONTO_INVALIDO,
                    "El monto del egreso compartido debe ser positivo: " + montoTotal);
        }
        if (partes == null || partes.isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.GASTO_SIN_CONTACTOS,
                    "Un egreso compartido necesita al menos una parte");
        }

        Set<Long> vistos = new HashSet<>();
        long suma = 0L;
        for (PartePropuesta parte : partes) {
            if (parte == null || parte.getMontoAsignado() <= 0) {
                throw new BusinessRuleException(
                        ErrorCode.MONTO_PARTE_INVALIDO,
                        "Parte sin monto positivo: " + parte);
            }
            long contactoId = parte.getContactoId();
            if (contactoId <= 0) {
                throw new BusinessRuleException(
                        ErrorCode.GASTO_SIN_CONTACTOS,
                        "Contacto sin identificar en el reparto: " + contactoId);
            }
            if (!vistos.add(contactoId)) {
                throw new BusinessRuleException(
                        ErrorCode.CONTACTO_DUPLICADO,
                        "Contacto repetido en el reparto: " + contactoId);
            }
            suma += parte.getMontoAsignado();
        }

        if (suma != montoTotal) {
            throw new BusinessRuleException(
                    ErrorCode.REPARTO_NO_CUADRA,
                    "Las partes suman " + suma + " y el egreso es " + montoTotal);
        }
    }
}