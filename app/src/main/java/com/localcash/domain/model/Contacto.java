package com.localcash.domain.model;

import java.io.Serializable;

/**
 * Modelo de dominio de un contacto de la agenda local.
 * <p>
 * <b>NO es la entidad Room</b>: vive en {@code domain/}, es independiente de
 * Android y de Room. Modelo <b>inmutable</b>: sin setters. Estado nuevo =
 * instancia nueva.
 * </p>
 * <p>
 * Nota de arquitectura: la entidad Room se referencia por nombre completo
 * ({@code com.localcash.data.entities.Contacto}) porque el simple name es
 * idéntico al de esta clase y Java prohíbe el import duplicado (JLS §7.5.1).
 * No se importa ninguna anotación de Room; este paquete no depende de
 * {@code androidx.*}.
 * </p>
 */
public class Contacto implements Serializable {

    /** Id persistido (0 = contacto aún no guardado). */
    private final long id;

    /** Nombre visible. No vacío. */
    private final String nombre;

    /** Teléfono opcional. Puede ser {@code null}. */
    private final String telefono;

    /** Email opcional. Puede ser {@code null}. */
    private final String email;

    /** Notas libres. Puede ser {@code null}. */
    private final String notas;

    /**
     * Crea una instancia de contacto.
     *
     * @param id       Id persistido o 0 si aún no se ha guardado
     * @param nombre   Nombre del contacto (no vacío)
     * @param telefono Teléfono opcional
     * @param email    Email opcional
     * @param notas    Notas opcionales
     */
    public Contacto(long id, String nombre, String telefono, String email, String notas) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.notas = notas;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public String getNotas() {
        return notas;
    }

    /**
     * Convierte este modelo de dominio a la entidad Room para persistencia.
     *
     * @return Entidad Room equivalente
     */
    public com.localcash.data.entities.Contacto toEntity() {
        return new com.localcash.data.entities.Contacto(
                nombre, telefono, email, notas);
    }

    /**
     * Convierte una entidad Room a este modelo de dominio.
     *
     * @param entity Entidad Room leída de la base de datos
     * @return Modelo de dominio equivalente
     */
    public static Contacto fromEntity(com.localcash.data.entities.Contacto entity) {
        return new Contacto(
                entity.getId(),
                entity.getNombre(),
                entity.getTelefono(),
                entity.getEmail(),
                entity.getNotas());
    }

    @Override
    public String toString() {
        return "Contacto{id=" + id
                + ", nombre='" + nombre + '\''
                + ", telefono='" + telefono + '\''
                + ", email='" + email + '\''
                + '}';
    }
}
