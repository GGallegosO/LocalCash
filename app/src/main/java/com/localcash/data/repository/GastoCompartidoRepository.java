package com.localcash.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.GastoCompartido;
import com.localcash.data.entities.Transaccion;
import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.PartePropuesta;
import com.localcash.domain.model.TipoTransaccion;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de las partes de egresos compartidos (Feature 012).
 * <p>
 * Es el <b>único</b> punto de escritura del cobro de una parte. Concentra la
 * secuencia atómica de I-16 y el registro de las partes al crear el egreso
 * compartido, para que la UI y el ViewModel solo invoquen operaciones y no
 * decidan reglas ni orden de escritura.
 * </p>
 * <p>
 * Todas las operaciones síncronas deben ejecutarse en un hilo secundario (el
 * {@code Executor} de {@link AppDatabase}): Room prohíbe escribir en el hilo
 * principal.
 * </p>
 */
public final class GastoCompartidoRepository {

    /**
     * Prefijo del ingreso que asienta una devolución. Se completa con el nombre
     * del contacto pagador (`"Devolución gasto compartido - Juan Pérez"`) para
     * que el historial diga quién devolvió el dinero.
     */
    public static final String DESCRIPCION_DEVOLUCION = "Devolución gasto compartido";

    /** Separador entre el prefijo de la devolución y el nombre del contacto. */
    public static final String SEPARADOR_PAGADOR = " - ";

    private final AppDatabase db;

    /**
     * @param db base de datos activa (en producción, la instancia única de
     *           {@link AppDatabase}; en tests, una base en memoria)
     */
    public GastoCompartidoRepository(@NonNull AppDatabase db) {
        this.db = db;
    }

    /**
     * Marca una parte como cobrada y asienta la devolución, todo en una sola
     * transacción de base de datos (I-16):
     * <ol>
     *   <li>valida que la parte exista y esté {@code pendiente};</li>
     *   <li>cambia su estado a {@code pagado} y registra {@code fecha_pago};</li>
     *   <li>inserta un ingreso {@value #DESCRIPCION_DEVOLUCION} en la cuenta del
     *       egreso original, con el monto asignado;</li>
     *   <li>suma ese monto al saldo con aritmética atómica
     *       ({@code saldo = saldo + :monto}).</li>
     * </ol>
     * Si cualquier paso lanza una excepción, Room revierte el bloque completo:
     * no queda ni parte cobrada, ni ingreso huérfano, ni saldo desplazado.
     * <p>
     * El ingreso mantiene verdadero el invariante I-1
     * ({@code saldo = saldo_inicial + Σ ingresos − Σ egresos}).
     * </p>
     *
     * @param parteId      id de la parte a cobrar
     * @param fechaPagoIso momento del cobro en ISO-8601 UTC, ya formateado por
     *                     el llamador (se persiste tal cual en {@code fecha_pago})
     * @return {@code null} si se cobró, o el {@link ErrorCode} de negocio:
     *         {@link ErrorCode#PARTE_INEXISTENTE} si la parte no existe y
     *         {@link ErrorCode#PARTE_YA_PAGADA} si ya estaba cobrada (INV-3)
     * @throws IllegalArgumentException si {@code fechaPagoIso} viene vacía
     *                                  (error de programación del llamador)
     * @throws IllegalStateException    si la base no confirma alguna escritura;
     *                                  la transacción se revierte completa
     */
    public ErrorCode marcarPagada(long parteId, @NonNull String fechaPagoIso) {
        if (fechaPagoIso == null || fechaPagoIso.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La fecha de pago en ISO-8601 es obligatoria");
        }

        final ErrorCode[] resultado = {null};
        db.runInTransaction(() -> resultado[0] = cobrarEnTransaccion(parteId, fechaPagoIso));
        return resultado[0];
    }

    /**
     * Concepto del asiento de devolución: prefijo + nombre del contacto.
     * <p>
     * Si la agenda ya no conserva el nombre (caso límite tras un borrado
     * permisivo) se usa un genérico, nunca {@code null} ni el separador suelto.
     * </p>
     *
     * @param pagador contacto que devolvió su parte, o {@code null}
     * @return concepto para {@code TRANSACCIONES}
     */
    private static String descripcionDevolucion(
            @Nullable com.localcash.data.entities.Contacto pagador) {
        String nombre = pagador == null ? null : pagador.getNombre();
        if (nombre == null || nombre.trim().isEmpty()) {
            return DESCRIPCION_DEVOLUCION + " (contacto no identificado)";
        }
        return DESCRIPCION_DEVOLUCION + SEPARADOR_PAGADOR + nombre.trim();
    }

    /**
     * Cobra varias partes en una sola transacción (botón "Pagar todo").
     * <p>
     * Si alguna falla, se revierte el bloque completo: no se cobra ninguna.
     * </p>
     *
     * @param partesIds ids de las partes a cobrar
     * @param fechaPagoIso fecha del cobro en ISO-8601 UTC
     * @return {@code null} si todas se cobraron, o el código del primer rechazo
     */
    public ErrorCode marcarTodasPagadas(@NonNull List<Long> partesIds,
                                        @NonNull String fechaPagoIso) {
        if (partesIds.isEmpty()) {
            return null;
        }
        final ErrorCode[] resultado = {null};
        try {
            db.runInTransaction(() -> {
                for (Long parteId : partesIds) {
                    ErrorCode error = cobrarEnTransaccion(parteId, fechaPagoIso);
                    if (error != null) {
                        resultado[0] = error;
                        throw new IllegalStateException("Cobro múltiple revertido: " + error);
                    }
                }
            });
        } catch (IllegalStateException e) {
            if (resultado[0] != null) {
                return resultado[0];
            }
            throw e;
        }
        return null;
    }

    /**
     * Inserta las partes de un egreso compartido. Debe invocarse <b>dentro</b>
     * de la transacción que registra el egreso (junto con el asiento y el
     * descuento del saldo): si una parte falla, el egreso tampoco se escribe.
     *
     * @param transaccionId    egreso recién insertado
     * @param partes           partes validadas por {@code GastoCompartidoService}
     * @param fechaCreacionIso momento del reparto en ISO-8601 UTC
     * @throws BusinessRuleException si la lista está vacía (INV-1)
     * @throws IllegalArgumentException si la fecha viene vacía
     */
    public void insertTodas(long transaccionId, @NonNull List<PartePropuesta> partes,
                            @NonNull String fechaCreacionIso) {
        if (fechaCreacionIso == null || fechaCreacionIso.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "La fecha de creación en ISO-8601 es obligatoria");
        }
        if (partes == null || partes.isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.GASTO_SIN_CONTACTOS,
                    "No hay partes que insertar para el egreso " + transaccionId);
        }

        List<GastoCompartido> entidades = new ArrayList<>(partes.size());
        for (PartePropuesta parte : partes) {
            if (parte.isDelUsuario()) {
                // Actor implícito: no existe fila "Yo" en CONTACTOS
                // (data-model.md §4.1), así que su parte no se persiste: el
                // egreso completo ya es su gasto.
                continue;
            }
            GastoCompartido entidad = new GastoCompartido(
                    transaccionId,
                    parte.getContactoId(),
                    parte.getMontoAsignado(),
                    fechaCreacionIso);
            entidades.add(entidad);
        }
        db.gastoCompartidoDao().insertTodas(entidades);
    }

    /**
     * Ejecuta la secuencia de cobro dentro de la transacción ya abierta.
     *
     * @param parteId      id de la parte
     * @param fechaPagoIso fecha del cobro en ISO-8601 UTC
     * @return {@code null} si se cobró, o el código de negocio del rechazo
     */
    private ErrorCode cobrarEnTransaccion(long parteId, String fechaPagoIso) {
        GastoCompartido parte = db.gastoCompartidoDao().getByIdSync(parteId);
        if (parte == null) {
            return ErrorCode.PARTE_INEXISTENTE;
        }
        if (!GastoCompartido.ESTADO_PENDIENTE.equals(parte.getEstado())) {
            return ErrorCode.PARTE_YA_PAGADA;
        }

        int filasParte = db.gastoCompartidoDao().marcarPagada(parteId, fechaPagoIso);
        if (filasParte != 1) {
            throw new IllegalStateException(
                    "El cobro no afectó la parte " + parteId + " (filas=" + filasParte + ")");
        }

        Transaccion egreso = db.transaccionDao().getByIdSync(parte.getTransaccionId());
        if (egreso == null) {
            throw new IllegalStateException(
                    "El egreso " + parte.getTransaccionId()
                            + " de la parte " + parteId + " no existe");
        }

        com.localcash.data.entities.Contacto pagador =
                db.contactoDao().getByIdSync(parte.getContactoId());
        Transaccion ingreso = new Transaccion(
                egreso.getCuentaId(),
                TipoTransaccion.INGRESO.name(),
                parte.getMontoAsignado(),
                descripcionDevolucion(pagador),
                System.currentTimeMillis());
        db.transaccionDao().insert(ingreso);

        int filasCuenta = db.cuentaDao().sumarSaldo(egreso.getCuentaId(),
                parte.getMontoAsignado());
        if (filasCuenta != 1) {
            throw new IllegalStateException(
                    "No se pudo acreditar el saldo de la cuenta " + egreso.getCuentaId()
                            + " al cobrar la parte " + parteId);
        }

        return null;
    }
}