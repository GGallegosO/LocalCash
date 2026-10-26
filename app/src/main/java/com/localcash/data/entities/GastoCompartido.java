package com.localcash.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa la parte de un egreso compartido que debe un
 * contacto (tabla {@code GASTOS_COMPARTIDOS}).
 * <p>
 * Es la tabla de unión entre un egreso del libro mayor y la agenda de
 * contactos: cada fila responde a "el contacto X debe Y pesos por el egreso Z"
 * ({@code data-model.md} §3.11). El monto va en <b>pesos CLP directos</b>
 * ({@code long}, nunca {@code double}, {@code .agent.md} §6.10).
 * </p>
 * <p>
 * Claves foráneas: borrar el egreso elimina sus partes ({@code CASCADE}: un
 * reparto sin gasto no significa nada) y borrar un contacto con partes está
 * bloqueado ({@code RESTRICT}: la identidad de una persona y su historial de
 * cobros no se recuperan). Los índices son exactamente dos: el único
 * {@code (transaccion_id, contacto_id)} —que además sirve las consultas por
 * egreso por su columna izquierda— y {@code (contacto_id, estado)} para el
 * resumen de deudas.
 * </p>
 * <p>
 * Room no expresa {@code CHECK} ni {@code DEFAULT} de esquema: el estado es un
 * dominio cerrado de dos valores ({@link #ESTADO_PENDIENTE} /
 * {@link #ESTADO_PAGADO}), el monto positivo y la coherencia
 * {@code estado} ↔ {@code fecha_pago} los valida
 * {@code domain/service/GastoCompartidoService} y, en el cobro, el repositorio
 * (I-14/I-16). El DAO no expone ninguna escritura de estado distinta de
 * {@code marcarPagada()} (INV-3).
 * </p>
 */
@Entity(
        tableName = "GASTOS_COMPARTIDOS",
        foreignKeys = {
                @ForeignKey(
                        entity = Transaccion.class,
                        parentColumns = "id",
                        childColumns = "transaccion_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Contacto.class,
                        parentColumns = "id",
                        childColumns = "contacto_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index(
                        name = "ux_gastos_compartidos_transaccion_contacto",
                        value = {"transaccion_id", "contacto_id"},
                        unique = true
                ),
                @Index(
                        name = "idx_gastos_compartidos_contacto_estado",
                        value = {"contacto_id", "estado"}
                )
        }
)
public class GastoCompartido {

    /** Estado de una parte que todavía no fue cobrada. */
    public static final String ESTADO_PENDIENTE = "pendiente";

    /** Estado terminal de una parte ya cobrada. No hay vuelta atrás (INV-3). */
    public static final String ESTADO_PAGADO = "pagado";

    /** Identificador local generado por el motor. 0 = aún no persistido. */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Egreso del que nace la parte. Obligatorio: sin gasto no hay reparto. */
    @ColumnInfo(name = "transaccion_id")
    private long transaccionId;

    /** Contacto deudor. Obligatorio y siempre un tercero, nunca el usuario. */
    @ColumnInfo(name = "contacto_id")
    private long contactoId;

    /** Parte del egreso asignada a este contacto, en pesos CLP (> 0). */
    @ColumnInfo(name = "monto_asignado")
    private long montoAsignado;

    /** Dominio cerrado {@link #ESTADO_PENDIENTE} / {@link #ESTADO_PAGADO}. */
    @NonNull
    private String estado;

    /** Momento del reparto en ISO-8601 UTC (por ejemplo {@code 2026-10-26T12:00:00Z}). */
    @NonNull
    @ColumnInfo(name = "fecha_creacion")
    private String fechaCreacion;

    /** Momento del cobro en ISO-8601 UTC; {@code null} mientras esté pendiente. */
    @ColumnInfo(name = "fecha_pago")
    private String fechaPago;

    /** Constructor vacío requerido por Room. */
    public GastoCompartido() {
    }

    /**
     * Constructor completo para una parte recién repartida: nace
     * {@link #ESTADO_PENDIENTE} y sin fecha de pago.
     *
     * @param transaccionId egreso que origina la parte
     * @param contactoId    contacto deudor
     * @param montoAsignado parte en pesos CLP (positiva)
     * @param fechaCreacion momento del reparto en ISO-8601 UTC
     */
    @Ignore
    public GastoCompartido(long transaccionId, long contactoId, long montoAsignado,
                           String fechaCreacion) {
        this.transaccionId = transaccionId;
        this.contactoId = contactoId;
        this.montoAsignado = montoAsignado;
        this.estado = ESTADO_PENDIENTE;
        this.fechaCreacion = fechaCreacion;
        this.fechaPago = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(long transaccionId) {
        this.transaccionId = transaccionId;
    }

    public long getContactoId() {
        return contactoId;
    }

    public void setContactoId(long contactoId) {
        this.contactoId = contactoId;
    }

    public long getMontoAsignado() {
        return montoAsignado;
    }

    public void setMontoAsignado(long montoAsignado) {
        this.montoAsignado = montoAsignado;
    }

    @NonNull
    public String getEstado() {
        return estado;
    }

    public void setEstado(@NonNull String estado) {
        this.estado = estado;
    }

    @NonNull
    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(@NonNull String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(String fechaPago) {
        this.fechaPago = fechaPago;
    }
}