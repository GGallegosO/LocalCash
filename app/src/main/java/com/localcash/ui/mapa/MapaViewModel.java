package com.localcash.ui.mapa;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.localcash.data.dao.LugarCompraDao;
import com.localcash.data.dao.TransaccionDao;
import com.localcash.data.db.AppDatabase;
import com.localcash.data.entities.ItemCompra;
import com.localcash.data.entities.LugarCompra;
import com.localcash.data.relations.LugarCompraConItems;
import com.localcash.domain.model.Transaccion;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel de la pestaña "Mapa" en modo lectura (Feature 009, T11; Feature 010,
 * Fase 2).
 * <p>
 * <b>Arquitectura (`.agent.md` §5.1):</b> pertenece a la capa {@code ui/}, que
 * solo orquesta y traduce; no contiene reglas de negocio. Observa la base de
 * datos a través de {@link TransaccionDao} y {@link LugarCompraDao}, y expone a
 * la vista listas <b>observables</b>: movimientos geolocalizados (modelo de
 * dominio) y lugares de compra con sus ítems, de modo que los pines del mapa se
 * actualicen solos sin consultar Room desde la vista.
 * </p>
 * <p>
 * <b>Estado (cableado a los DAOs):</b> los DAOs se inyectan por constructor (DI
 * simple, sin Hilt/Dagger): la Activity necesita una
 * {@code ViewModelProvider.Factory}, igual que {@code HistorialViewModel}. El
 * {@code LiveData} de entidades que emite
 * {@link TransaccionDao#getConUbicacion()} se transforma a dominio en un único
 * punto de mapeo ({@link #mapearADominio(List)}).
 * </p>
 */
public class MapaViewModel extends ViewModel {

    /** Etiqueta de logs del ViewModel. */
    private static final String TAG = "MapaViewModel";

    /** Lista observable de movimientos geolocalizados, en modelo de dominio. */
    private final LiveData<List<Transaccion>> transaccionesConUbicacion;

    /** DAO de lugares de compra, usado para insertar desde el diálogo del mapa. */
    private final LugarCompraDao lugarCompraDao;

    /** Lugares de compra con sus ítems, observados por el mapa (Feature 010). */
    private final LiveData<List<LugarCompraConItems>> lugaresCompra;

    /**
     * Construye el ViewModel cableado a los DAOs de transacciones y de lugares
     * de compra.
     *
     * @param transaccionDao DAO observado para obtener los movimientos con ubicación
     * @param lugarCompraDao DAO observado e invocado para los lugares de compra
     */
    public MapaViewModel(@NonNull TransaccionDao transaccionDao,
                         @NonNull LugarCompraDao lugarCompraDao) {
        this.transaccionesConUbicacion = Transformations.map(
                transaccionDao.getConUbicacion(),
                this::mapearADominio);
        this.lugarCompraDao = lugarCompraDao;
        this.lugaresCompra = lugarCompraDao.getAllLugaresConItemsLive();
    }

    /**
     * Observa los movimientos geolocalizados.
     *
     * @return LiveData con la lista de movimientos con ubicación (modelo de
     *         dominio), ordenada por fecha descendente según
     *         {@link TransaccionDao#getConUbicacion()}
     */
    public LiveData<List<Transaccion>> getTransaccionesConUbicacion() {
        return transaccionesConUbicacion;
    }

    /**
     * Observa los lugares de compra con sus ítems (Feature 010).
     * <p>
     * Room reejecuta la consulta tras cada inserción, así que el pin del lugar
     * recién creado aparece sin que la Activity tenga que recargar a mano.
     * </p>
     *
     * @return LiveData con los lugares de compra, del más reciente al más antiguo
     */
    public LiveData<List<LugarCompraConItems>> getLugaresCompra() {
        return lugaresCompra;
    }

    /**
     * Inserta un lugar de compra y su lista de ítems en un hilo secundario.
     * <p>
     * Usa {@link LugarCompraDao#insertLugarConItems(LugarCompra, List)}: la
     * cabecera y sus ítems se guardan en una sola transacción, asignando el id
     * generado a cada ítem. El resultado se publica en el hilo principal a
     * través del {@link MutableLiveData} para que la vista muestre el aviso.
     * </p>
     *
     * @param lugar Lugar de compra a insertar
     * @param items Ítems de la lista (puede ser vacía)
     * @return LiveData que emite {@code true} si se guardó y {@code false} si
     *         falló la persistencia
     */
    public LiveData<Boolean> insertarLugar(@NonNull LugarCompra lugar,
                                           @NonNull List<ItemCompra> items) {
        MutableLiveData<Boolean> resultado = new MutableLiveData<>();
        AppDatabase.getExecutor().execute(() -> {
            try {
                lugarCompraDao.insertLugarConItems(lugar, items);
                resultado.postValue(Boolean.TRUE);
            } catch (RuntimeException e) {
                Log.e(TAG, "No se pudo insertar el lugar de compra", e);
                resultado.postValue(Boolean.FALSE);
            }
        });
        return resultado;
    }

    /**
     * Transforma una lista de entidades Room a modelos de dominio.
     * <p>
     * Único punto de conversión entidad → dominio de este ViewModel: se apoya en
     * {@link Transaccion#fromEntity(com.localcash.data.entities.Transaccion)},
     * el par canónico definido en {@code domain/model/Transaccion}.
     * </p>
     *
     * @param entidades Movimientos geolocalizados tal como los entrega Room (puede ser vacía)
     * @return Lista equivalente de modelos de dominio
     */
    @NonNull
    private List<Transaccion> mapearADominio(
            @NonNull List<com.localcash.data.entities.Transaccion> entidades) {
        List<Transaccion> dominio = new ArrayList<>(entidades.size());
        for (com.localcash.data.entities.Transaccion entidad : entidades) {
            dominio.add(Transaccion.fromEntity(entidad));
        }
        return dominio;
    }
}
