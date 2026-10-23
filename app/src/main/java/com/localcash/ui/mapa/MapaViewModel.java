package com.localcash.ui.mapa;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.localcash.data.dao.ItemLugarDao;
import com.localcash.data.dao.LugarDao;
import com.localcash.domain.model.ItemLugar;
import com.localcash.domain.model.Lugar;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel de la pestaña "Mapa" (Feature 009).
 * <p>
 * Observa {@code LUGARES} e {@code ITEMS_LUGAR} y expone modelos de dominio.
 * La UI no conoce entidades Room: el único punto de conversión es
 * {@link Lugar#fromEntity(com.localcash.data.entities.Lugar)} y
 * {@link ItemLugar#fromEntity(com.localcash.data.entities.ItemLugar)}.
 * </p>
 * <p>
 * Los DAOs se inyectan por constructor (DI simple, sin Hilt), igual que
 * {@code HistorialViewModel}. La Activity los obtiene de {@code AppDatabase}.
 * </p>
 */
public class MapaViewModel extends ViewModel {

    private final ItemLugarDao itemLugarDao;

    /** Todos los lugares (abiertos y cerrados) para dibujar el mapa. */
    private final LiveData<List<Lugar>> lugares;

    /** Solo lugares abiertos para la lista "Mis lugares". */
    private final LiveData<List<Lugar>> lugaresAbiertos;

    /**
     * Construye el ViewModel cableado a los DAOs de lugares.
     *
     * @param lugarDao     DAO observable de lugares
     * @param itemLugarDao DAO de los elementos de cada lugar
     */
    public MapaViewModel(@NonNull LugarDao lugarDao, @NonNull ItemLugarDao itemLugarDao) {
        this.itemLugarDao = itemLugarDao;
        this.lugares = Transformations.map(lugarDao.getAll(), this::mapearLugares);
        this.lugaresAbiertos = Transformations.map(lugarDao.getAbiertos(), this::mapearLugares);
    }

    /**
     * Observa todos los lugares del mapa.
     *
     * @return LiveData con todos los pines en modelo de dominio
     */
    public LiveData<List<Lugar>> getLugares() {
        return lugares;
    }

    /**
     * Observa los lugares activos (estado {@code abierto}).
     *
     * @return LiveData con los pines que aparecen en "Mis lugares"
     */
    public LiveData<List<Lugar>> getLugaresAbiertos() {
        return lugaresAbiertos;
    }

    /**
     * Observa la lista de cosas de un lugar.
     *
     * @param lugarId id del lugar
     * @return LiveData con los elementos del lugar (vacía si no tiene)
     */
    public LiveData<List<ItemLugar>> getItems(long lugarId) {
        return Transformations.map(
                itemLugarDao.getByLugarId(lugarId), this::mapearItems);
    }

    /**
     * Convierte entidades Room a modelos de dominio.
     *
     * @param entidades lista emitida por Room
     * @return lista equivalente de dominio (nunca {@code null})
     */
    @NonNull
    private List<Lugar> mapearLugares(List<com.localcash.data.entities.Lugar> entidades) {
        List<Lugar> dominio = new ArrayList<>();
        if (entidades == null) {
            return dominio;
        }
        for (com.localcash.data.entities.Lugar entidad : entidades) {
            dominio.add(Lugar.fromEntity(entidad));
        }
        return dominio;
    }

    /**
     * Convierte los elementos de un lugar a modelos de dominio.
     *
     * @param entidades lista emitida por Room
     * @return lista equivalente de dominio (nunca {@code null})
     */
    @NonNull
    private List<ItemLugar> mapearItems(
            List<com.localcash.data.entities.ItemLugar> entidades) {
        List<ItemLugar> dominio = new ArrayList<>();
        if (entidades == null) {
            return dominio;
        }
        for (com.localcash.data.entities.ItemLugar entidad : entidades) {
            dominio.add(ItemLugar.fromEntity(entidad));
        }
        return dominio;
    }
}
