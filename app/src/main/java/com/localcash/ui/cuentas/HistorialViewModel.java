package com.localcash.ui.cuentas;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.localcash.data.dao.TransaccionDao;
import com.localcash.domain.model.Transaccion;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel de la Vista "Movimientos" (historial de transacciones).
 * <p>
 * <b>Arquitectura (`.agent.md` §5.1):</b> pertenece a la capa {@code ui/}
 * (arquitectura por capas), que solo orquesta y traduce; no contiene reglas de
 * negocio ni lógica de dinero. Observa la base de datos a través del
 * {@link TransaccionDao} y expone a la vista una lista <b>observable</b> del
 * modelo de dominio, de modo que la UI reaccione a los cambios sin consultar
 * Room directamente.
 * </p>
 * <p>
 * <b>Estado (T8c - cableado al DAO):</b> el DAO se inyecta por constructor
 * (DI simple, sin Hilt/Dagger). El {@code LiveData} de entidades que emite
 * {@link TransaccionDao#getAll()} se transforma a dominio en un único punto de
 * mapeo ({@link #mapearADominio(List)}), porque la entidad Room y el modelo de
 * dominio son tipos distintos (ver {@code domain/model/Transaccion}).
 * </p>
 */
public class HistorialViewModel extends ViewModel {

    /** Lista observable de movimientos del historial, en modelo de dominio. */
    private final LiveData<List<Transaccion>> transacciones;

    /**
     * Construye el ViewModel cableado al DAO de transacciones.
     * <p>
     * <b>Instanciación desde la Activity (pendiente para T8e):</b> al recibir el
     * DAO por constructor no sirve la fábrica por defecto
     * ({@code new ViewModelProvider(this).get(HistorialViewModel.class)} tardaría
     * en crear la instancia); se necesita una {@code ViewModelProvider.Factory}:
     * </p>
     * <pre>
     * HistorialViewModel vm = new ViewModelProvider(this,
     *         new ViewModelProvider.Factory() {
     *             {@literal @}NonNull
     *             {@literal @}Override
     *             public &lt;T extends ViewModel&gt; T create({@literal @}NonNull Class&lt;T&gt; modelClass) {
     *                 return (T) new HistorialViewModel(
     *                         AppDatabase.getInstance(getApplicationContext()).transaccionDao());
     *             }
     *         }).get(HistorialViewModel.class);
     * </pre>
     *
     * @param transaccionDao DAO observado para obtener todos los movimientos
     */
    public HistorialViewModel(@NonNull TransaccionDao transaccionDao) {
        this.transacciones = Transformations.map(
                transaccionDao.getAll(),
                this::mapearADominio);
    }

    /**
     * Observa el historial completo de movimientos.
     *
     * @return LiveData con la lista de movimientos (modelo de dominio),
     *         ordenada por fecha descendente según {@link TransaccionDao#getAll()}
     */
    public LiveData<List<Transaccion>> getTransacciones() {
        return transacciones;
    }

    /**
     * Transforma una lista de entidades Room a modelos de dominio.
     * <p>
     * Único punto de conversión entidad → dominio de este ViewModel: se apoya en
     * {@link Transaccion#fromEntity(com.localcash.data.entities.Transaccion)},
     * el par canónico definido en {@code domain/model/Transaccion}.
     * </p>
     *
     * @param entidades Movimientos tal como los entrega Room (puede ser vacía)
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
