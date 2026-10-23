package com.localcash.domain.model;

/**
 * Modelo de dominio de un pin del mapa.
 * <p>
 * Inmutable y sin dependencias de Android ni Room (`.agent.md` §5.1). Un pin
 * puede estar vinculado a un movimiento ({@code transaccionId != null}) o ser
 * independiente ({@code null}). La lista de cosas se modela por separado en
 * {@link ItemLugar}, igual que en la base de datos.
 * </p>
 */
public final class Lugar {

    private final long id;
    private final Long transaccionId;
    private final String nombre;
    private final TipoLugar tipo;
    private final String emoji;
    private final String descripcion;
    private final Ubicacion ubicacion;
    private final EstadoLugar estado;

    /**
     * Crea un lugar del dominio.
     *
     * @param id           id persistido o 0 si aún no se guarda
     * @param transaccionId movimiento vinculado o {@code null}
     * @param nombre       nombre visible
     * @param tipo         tipo de lugar
     * @param emoji        emoji visible
     * @param descripcion  nota libre opcional
     * @param ubicacion    coordenada WGS84
     * @param estado       abierto o cerrado
     */
    public Lugar(long id, Long transaccionId, String nombre, TipoLugar tipo,
                 String emoji, String descripcion, Ubicacion ubicacion, EstadoLugar estado) {
        this.id = id;
        this.transaccionId = transaccionId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.emoji = emoji;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.estado = estado;
    }

    public long getId() {
        return id;
    }

    public Long getTransaccionId() {
        return transaccionId;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoLugar getTipo() {
        return tipo;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public EstadoLugar getEstado() {
        return estado;
    }

    /**
     * @return {@code true} si el pin nació de un movimiento financiero
     */
    public boolean estaVinculadoAMovimiento() {
        return transaccionId != null;
    }

    /**
     * Devuelve una copia con otro estado. No muta la instancia original.
     *
     * @param nuevoEstado nuevo estado del pin
     * @return copia con el estado indicado
     */
    public Lugar conEstado(EstadoLugar nuevoEstado) {
        return new Lugar(id, transaccionId, nombre, tipo, emoji, descripcion, ubicacion, nuevoEstado);
    }

    /**
     * Convierte este modelo a la entidad Room. El id se copia para permitir
     * actualizaciones; para inserciones vale 0 y Room lo autogenera.
     *
     * @return entidad Room equivalente
     */
    public com.localcash.data.entities.Lugar toEntity() {
        com.localcash.data.entities.Lugar entity =
                new com.localcash.data.entities.Lugar(
                        transaccionId,
                        nombre,
                        tipo.getValor(),
                        emoji,
                        descripcion,
                        ubicacion.getLatitud(),
                        ubicacion.getLongitud(),
                        estado.getValor());
        entity.setId(id);
        return entity;
    }

    /**
     * Convierte una entidad Room al modelo de dominio.
     *
     * @param entity entidad leída de la base
     * @return modelo de dominio equivalente
     */
    public static Lugar fromEntity(com.localcash.data.entities.Lugar entity) {
        return new Lugar(
                entity.getId(),
                entity.getTransaccionId(),
                entity.getNombre(),
                TipoLugar.fromValor(entity.getTipoLugar()),
                entity.getEmoji(),
                entity.getDescripcion(),
                new Ubicacion(entity.getLatitud(), entity.getLongitud()),
                EstadoLugar.fromValor(entity.getEstado()));
    }

    @Override
    public String toString() {
        return "Lugar{id=" + id
                + ", transaccionId=" + transaccionId
                + ", nombre='" + nombre + '\''
                + ", tipo=" + tipo
                + ", estado=" + estado
                + ", ubicacion=" + ubicacion
                + '}';
    }
}
