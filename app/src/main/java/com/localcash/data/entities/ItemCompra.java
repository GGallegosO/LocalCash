package com.localcash.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa un ítem de la lista de compras de un
 * {@link LugarCompra}.
 * <p>
 * Tabla: {@code item_compra}. FK {@code lugarCompraId → lugar_compra.id} con
 * {@code ON DELETE CASCADE}: al eliminar el lugar se eliminan sus ítems (un
 * ítem sin lista no tiene significado). Room exige un índice sobre la columna
 * hija de la clave foránea, por eso se declara {@code idx_item_compra_lugar}.
 * </p>
 */
@Entity(
        tableName = "item_compra",
        foreignKeys = @ForeignKey(
                entity = LugarCompra.class,
                parentColumns = "id",
                childColumns = "lugarCompraId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(name = "idx_item_compra_lugar", value = {"lugarCompraId"})
)
public class ItemCompra {

    /** Identificador local, generado por el motor (AUTOINCREMENT). */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    /** Lugar de compra al que pertenece el ítem (FK). */
    @ColumnInfo(name = "lugarCompraId")
    private long lugarCompraId;

    /** Nombre del producto o servicio ("Leche", "Pan"). */
    @NonNull
    @ColumnInfo(name = "nombre")
    private String nombre;

    /** Cantidad de unidades (conteo entero, nunca dinero). */
    @ColumnInfo(name = "cantidad")
    private int cantidad;

    /** Indica si el ítem ya fue comprado (persistido como 1/0). */
    @ColumnInfo(name = "estaComprado")
    private boolean estaComprado;

    /** Constructor vacío requerido por Room. */
    public ItemCompra() {
    }

    /** Constructor completo para agregar un ítem a una lista. */
    @Ignore
    public ItemCompra(long lugarCompraId, String nombre, int cantidad, boolean estaComprado) {
        this.lugarCompraId = lugarCompraId;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.estaComprado = estaComprado;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLugarCompraId() {
        return lugarCompraId;
    }

    public void setLugarCompraId(long lugarCompraId) {
        this.lugarCompraId = lugarCompraId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public boolean isEstaComprado() {
        return estaComprado;
    }

    public void setEstaComprado(boolean estaComprado) {
        this.estaComprado = estaComprado;
    }
}
