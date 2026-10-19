package com.localcash.ui.mapa;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.localcash.data.dao.TransaccionDao;
import com.localcash.domain.model.Transaccion;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel de la pestaña "Mapa" en modo lectura (Feature 009, T11).
 * <p>
 * <b>Arquitectura (`.agent.md` §5.1):</b> pertenece a la capa {@code ui/}, que
 * solo orquesta y traduce; no contiene reglas de negocio. Observa la base de
 * datos a través del {@link TransaccionDao} y expone a la vista una lista
 * <b>observable</b> del modelo de dominio con los movimientos geolocalizados,
 * de modo que los pines del mapa se actualicen solos sin consultar Room desde
 * la vista.
 * </p>
 * <p>
 * <b>Estado (cableado al DAO):</b> el DAO se inyecta por constructor (DI simple,
 * sin Hilt/Dagger): la Activity necesita una {@code ViewModelProvider.Factory},
 * igual que {@code HistorialViewModel}. El {@code LiveData} de entidades que
 * emite {@link TransaccionDao#getConUbicacion()} se transforma a dominio en un
 * único punto de mapeo ({@link #mapearADominio(List)}).
 * </p>
 */
public class MapaViewModel extends ViewModel {

    /** Lista observable de movimientos geolocalizados, en modelo de dominio. */
    private final LiveData<List<Transaccion>> transaccionesConUbicacion;

    /**
     * Construye el ViewModel cableado al DAO de transacciones.
     *
     * @param transaccionDao DAO observado para obtener los movimientos con ubicación
     */
    public MapaViewModel(@NonNull TransaccionDao transaccionDao) {
        this.transaccionesConUbicacion = Transformations.map(
                transaccionDao.getConUbicacion(),
                this::mapearADominio);
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
