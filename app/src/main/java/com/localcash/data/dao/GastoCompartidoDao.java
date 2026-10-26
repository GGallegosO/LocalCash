package com.localcash.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.localcash.data.entities.GastoCompartido;
import com.localcash.data.model.ResumenDeuda;

import java.util.List;

/**
 * DAO de la entidad {@link GastoCompartido} (tabla {@code GASTOS_COMPARTIDOS}).
 * <p>
 * El nombre de tabla se centraliza en {@link #TABLE_NAME} para no dejar
 * literales dispersos (regla de AGENTS.md). Las consultas se validan en
 * compilación contra el esquema de Room.
 * </p>
 * <p>
 * <b>Superficie de escritura deliberadamente mínima (INV-3).</b> Este DAO no
 * expone {@code update(GastoCompartido)} ni {@code delete(GastoCompartido)}:
 * la única forma de mover una parte de {@code pendiente} a {@code pagado} es
 * {@link #marcarPagada(long, String)}, que filtra por el estado actual. Un
 * paquete {@code pagado} no se puede reabrir por ninguna vía pública.
 * </p>
 * <p>
 * Las operaciones síncronas ({@code insertTodas}, {@code marcarPagada},
 * {@code deleteByTransaccion}) deben ejecutarse en el executor de
 * {@code com.localcash.data.db.AppDatabase}, nunca en el hilo principal.
 * </p>
 */
@Dao
public interface GastoCompartidoDao {

    /** Nombre canónico de la tabla; una sola fuente para consultas y pruebas. */
    String TABLE_NAME = "GASTOS_COMPARTIDOS";

    /**
     * Inserta todas las partes de un reparto en una sola llamada.
     * <p>
     * Se invoca dentro de la transacción que registra el egreso compartido:
     * si una inserción falla, Room revierte también el asiento del egreso
     * (todo o nada). La unicidad {@code (transaccion_id, contacto_id)} impide
     * repetir al mismo deudor en el mismo egreso.
     * </p>
     *
     * @param partes partes a insertar, todas del mismo egreso
     * @return ids autogenerados, en el mismo orden de la lista
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    List<Long> insertTodas(List<GastoCompartido> partes);

    /**
     * Obtiene las partes de un egreso, ordenadas por id (orden de captura).
     * Solo para hilos secundarios.
     *
     * @param transaccionId id del egreso compartido
     * @return lista de partes; vacía si el egreso no se compartió
     */
    @Query("SELECT * FROM " + TABLE_NAME
            + " WHERE transaccion_id = :transaccionId ORDER BY id ASC")
    List<GastoCompartido> getByTransaccion(long transaccionId);

    /**
     * Obtiene las partes que un contacto todavía debe, de la más reciente a la
     * más antigua. Alimenta el detalle del contacto (Feature 012).
     *
     * @param contactoId id del contacto deudor
     * @return lista de partes pendientes; vacía si no debe nada
     */
    @Query("SELECT * FROM " + TABLE_NAME
            + " WHERE contacto_id = :contactoId AND estado = '"
            + GastoCompartido.ESTADO_PENDIENTE + "'"
            + " ORDER BY fecha_creacion DESC, id DESC")
    List<GastoCompartido> getPendientesPorContacto(long contactoId);

    /**
     * Resumen de deuda de todos los contactos con partes pendientes: el
     * "me deben" de la Feature 012, ordenado de mayor a menor deuda.
     *
     * @return proyecciones {@code (contacto, nombre, total pendiente)}
     */
    @Query("SELECT gc.contacto_id AS contactoId,"
            + "       c.nombre AS nombreContacto,"
            + "       SUM(gc.monto_asignado) AS totalPendiente"
            + " FROM " + TABLE_NAME + " gc"
            + " JOIN CONTACTOS c ON c.id = gc.contacto_id"
            + " WHERE gc.estado = '" + GastoCompartido.ESTADO_PENDIENTE + "'"
            + " GROUP BY gc.contacto_id, c.nombre"
            + " ORDER BY totalPendiente DESC, c.nombre ASC")
    List<ResumenDeuda> resumenDeudasPorContacto();

    /**
     * Resumen de deuda de un contacto concreto (fila única).
     *
     * @param contactoId id del contacto deudor
     * @return lista con cero o una proyección de deuda
     */
    @Query("SELECT gc.contacto_id AS contactoId,"
            + "       c.nombre AS nombreContacto,"
            + "       SUM(gc.monto_asignado) AS totalPendiente"
            + " FROM " + TABLE_NAME + " gc"
            + " JOIN CONTACTOS c ON c.id = gc.contacto_id"
            + " WHERE gc.estado = '" + GastoCompartido.ESTADO_PENDIENTE + "'"
            + "   AND gc.contacto_id = :contactoId"
            + " GROUP BY gc.contacto_id, c.nombre")
    List<ResumenDeuda> resumenDeudasPorContacto(long contactoId);

    /**
     * Marca una parte como cobrada. Única escritura de estado permitida
     * (INV-3): el filtro {@code estado = 'pendiente'} hace que un segundo
     * intento afecte 0 filas y no altere la fecha del primer cobro.
     * <p>
     * El asiento del ingreso y el aumento atómico del saldo se ejecutan en la
     * misma transacción desde el repositorio (I-16); este método solo cierra la
     * parte.
     * </p>
     *
     * @param parteId      id de la parte a cobrar
     * @param fechaPagoIso momento del cobro en ISO-8601 UTC
     * @return número de filas afectadas: 1 si se cobró, 0 si ya estaba pagada
     *         o no existe
     */
    @Query("UPDATE " + TABLE_NAME
            + " SET estado = '" + GastoCompartido.ESTADO_PAGADO + "',"
            + "     fecha_pago = :fechaPagoIso"
            + " WHERE id = :parteId AND estado = '"
            + GastoCompartido.ESTADO_PENDIENTE + "'")
    int marcarPagada(long parteId, String fechaPagoIso);

    /**
     * Elimina las partes de un egreso de forma explícita.
     * <p>
     * <b>Uso interno exclusivo</b> (reparación/migración y pruebas): el borrado
     * normal ocurre por la cascada de la clave foránea al eliminar la
     * transacción. No debe invocarse desde {@code ui/}.
     * </p>
     *
     * @param transaccionId id del egreso cuyas partes se eliminan
     * @return número de filas borradas
     */
    @Query("DELETE FROM " + TABLE_NAME + " WHERE transaccion_id = :transaccionId")
    int deleteByTransaccion(long transaccionId);
}