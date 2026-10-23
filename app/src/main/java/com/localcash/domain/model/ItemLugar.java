package com.localcash.domain.model;

/**
 * Elemento de la lista de cosas de un pin.
 * <p>
 * Modelo inmutable del dominio. No guarda precios ni cantidades: es un
 * recordatorio, no una transacción financiera (Feature 009, plan §2.11).
 * </p>
 */
public final class ItemLugar {

    private final long id;
    private final long lugarId;
    private final String descripcion;
    private final boolean completado;

    /**
     * Crea un elemento de lista.
     *
     * @param id          id persistido o 0 si aún no se guarda
     * @param lugarId     pin al que pertenece
     * @param descripcion qué hay que recordar
     * @param completado  {@code true} si ya se completó
     */
    public ItemLugar(long id, long lugarId, String descripcion, boolean completado) {
        this.id = id;
        this.lugarId = lugarId;
        this.descripcion = descripcion;
        this.completado = completado;
    }

    public long getId() {
        return id;
    }

    public long getLugarId() {
        return lugarId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isCompletado() {
        return completado;
    }

    /**
     * Devuelve una copia con el estado de completado invertido.
     *
     * @return copia con {@code completado} alternado
     */
    public ItemLugar alternarCompletado() {
        return new ItemLugar(id, lugarId, descripcion, !completado);
    }

    /**
     * Convierte este modelo a la entidad Room.
     *
     * @return entidad Room equivalente
     */
    public com.localcash.data.entities.ItemLugar toEntity() {
        com.localcash.data.entities.ItemLugar entity =
                new com.localcash.data.entities.ItemLugar(lugarId, descripcion, completado);
        entity.setId(id);
        return entity;
    }

    /**
     * Convierte una entidad Room al modelo de dominio.
     *
     * @param entity entidad leída de la base
     * @return modelo de dominio equivalente
     */
    public static ItemLugar fromEntity(com.localcash.data.entities.ItemLugar entity) {
        return new ItemLugar(
                entity.getId(),
                entity.getLugarId(),
                entity.getDescripcion(),
                entity.isCompletado());
    }

    @Override
    public String toString() {
        return "ItemLugar{id=" + id
                + ", lugarId=" + lugarId
                + ", descripcion='" + descripcion + '\''
                + ", completado=" + completado
                + '}';
    }
}
