package com.localcash.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa un contacto de la agenda local (tabla {@code CONTACTOS}).
 * <p>
 * Los contactos son terceros con los que el usuario comparte gastos. No contiene al
 * usuario principal: la app no tiene perfil propio en la base de datos
 * ({@code data-model.md} §4.1).
 * </p>
 * <p>
 * El borrado usa {@code ON DELETE RESTRICT}: no se puede eliminar un contacto que
 * tenga compras asociadas o deudas pendientes/parciales. La UI traduce el bloqueo
 * al error de negocio {@code CONTACTO_EN_USO}.
 * </p>
 */
@Entity(
        tableName = "CONTACTOS",
        indices = {
                @Index(value = {"telefono"}, unique = true),
                @Index(value = {"email"}, unique = true)
        }
)
public class Contacto {

    /** Identificador local generado por el motor. 0 = aún no persistido. */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /** Nombre visible. Obligatorio y no vacío. */
    @NonNull
    private String nombre;

    /** Teléfono opcional. Índice único parcial: múltiples {@code NULL} conviven. */
    @ColumnInfo(name = "telefono")
    private String telefono;

    /** Email opcional. Índice único parcial: múltiples {@code NULL} conviven. */
    @ColumnInfo(name = "email")
    private String email;

    /** Notas libres del contacto. */
    private String notas;

    /** Constructor vacío requerido por Room. */
    public Contacto() {
    }

    /** Constructor completo para insertar/actualizar un contacto. */
    @Ignore
    public Contacto(String nombre, String telefono, String email, String notas) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.notas = notas;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}
