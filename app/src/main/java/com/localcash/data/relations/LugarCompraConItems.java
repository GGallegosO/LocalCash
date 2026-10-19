package com.localcash.data.relations;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.localcash.data.entities.ItemCompra;
import com.localcash.data.entities.LugarCompra;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO de relación Room que agrupa un {@link LugarCompra} con sus
 * {@link ItemCompra} asociados (relación uno-a-muchos).
 * <p>
 * Se usa como resultado de consultas anotadas con {@code @Transaction} en
 * {@code LugarCompraDao}; no es una tabla.
 * </p>
 */
public class LugarCompraConItems {

    /** Cabecera del lugar de compra. */
    @Embedded
    private LugarCompra lugarCompra;

    /** Ítems de la lista, ordenados por id ascendente en el DAO. */
    @Relation(parentColumn = "id", entityColumn = "lugarCompraId")
    private List<ItemCompra> items = new ArrayList<>();

    /** Constructor vacío requerido por Room. */
    public LugarCompraConItems() {
    }

    public LugarCompra getLugarCompra() {
        return lugarCompra;
    }

    public void setLugarCompra(LugarCompra lugarCompra) {
        this.lugarCompra = lugarCompra;
    }

    public List<ItemCompra> getItems() {
        return items;
    }

    public void setItems(List<ItemCompra> items) {
        this.items = items;
    }
}
