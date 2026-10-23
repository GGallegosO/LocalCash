package com.localcash.ui.mapa;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.localcash.data.db.AppDatabase;
import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.EstadoLugar;
import com.localcash.domain.model.ItemLugar;
import com.localcash.domain.model.Lugar;
import com.localcash.domain.model.Ubicacion;
import com.localcash.domain.service.LugarService;

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
 * Las escrituras se ejecutan en el executor de un solo hilo de
 * {@link AppDatabase}, igual que {@code TransaccionViewModel}. La validación de
 * negocio vive en {@link LugarService}; este ViewModel solo orquesta y
 * traduce los errores a {@link ErrorCode} (`.agent.md` §5.4).
 * </p>
 */
public class MapaViewModel extends ViewModel {

    /** Etiqueta de logs del ViewModel. */
    private static final String TAG = "MapaViewModel";

    private final AppDatabase db;

    /** Todos los lugares (abiertos y cerrados) para dibujar el mapa. */
    private final LiveData<List<Lugar>> lugares;

    /** Solo lugares abiertos para la lista "Mis lugares". */
    private final LiveData<List<Lugar>> lugaresAbiertos;

    /**
     * Construye el ViewModel cableado a la base de datos.
     *
     * @param db base Room de la aplicación
     */
    public MapaViewModel(@NonNull AppDatabase db) {
        this.db = db;
        this.lugares = Transformations.map(db.lugarDao().getAll(), this::mapearLugares);
        this.lugaresAbiertos =
                Transformations.map(db.lugarDao().getAbiertos(), this::mapearLugares);
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
                db.itemLugarDao().getByLugarId(lugarId), this::mapearItems);
    }

    /**
     * Observa el movimiento vinculado a un pin, para mostrar su fecha y hora
     * en el detalle (Feature 009).
     *
     * @param transaccionId id del movimiento vinculado
     * @return LiveData con el movimiento de dominio, o {@code null} si ya no existe
     */
    public LiveData<com.localcash.domain.model.Transaccion> getTransaccion(long transaccionId) {
        return Transformations.map(db.transaccionDao().getById(transaccionId),
                entidad -> entidad != null
                        ? com.localcash.domain.model.Transaccion.fromEntity(entidad)
                        : null);
    }

    /**
     * Crea un lugar y, en la misma transacción, sus cosas iniciales.
     * <p>
     * Cancelar el formulario no llama a este método: no se inserta nada hasta
     * que el usuario confirma (T18).
     * </p>
     *
     * @param lugar pin a insertar (id 0)
     * @param items cosas iniciales (puede ser {@code null} o vacía)
     * @return LiveData con {@code null} si se guardó, o el código del fallo
     */
    public LiveData<ErrorCode> crearLugar(@NonNull Lugar lugar, List<ItemLugar> items) {
        MutableLiveData<ErrorCode> resultado = new MutableLiveData<>();
        List<ItemLugar> iniciales = items != null ? items : new ArrayList<>();

        db.getExecutor().execute(() -> {
            try {
                LugarService.validarLugar(lugar);
                for (ItemLugar item : iniciales) {
                    if (item.getDescripcion() == null || item.getDescripcion().trim().isEmpty()) {
                        throw new BusinessRuleException(
                                ErrorCode.LUGAR_INVALIDO, "Descripción de cosa vacía");
                    }
                }
            } catch (BusinessRuleException e) {
                Log.w(TAG, "Lugar rechazado por regla de negocio", e);
                resultado.postValue(e.getErrorCode());
                return;
            }

            try {
                db.runInTransaction(() -> {
                    long lugarId = db.lugarDao().insert(lugar.toEntity());
                    for (ItemLugar item : iniciales) {
                        db.itemLugarDao().insert(new com.localcash.data.entities.ItemLugar(
                                lugarId, item.getDescripcion(), item.isCompletado()));
                    }
                });
                resultado.postValue(null);
            } catch (RuntimeException e) {
                Log.e(TAG, "No se pudo crear el lugar", e);
                resultado.postValue(ErrorCode.LUGAR_INVALIDO);
            }
        });
        return resultado;
    }

    /**
     * Actualiza nombre, tipo, emoji, descripción y estado de un lugar.
     *
     * @param lugar pin con los valores nuevos (debe tener id)
     */
    public void actualizarLugar(@NonNull Lugar lugar) {
        db.getExecutor().execute(() -> {
            try {
                LugarService.validarLugar(lugar);
                db.lugarDao().update(lugar.toEntity());
            } catch (BusinessRuleException e) {
                Log.w(TAG, "Actualización de lugar rechazada", e);
            }
        });
    }

    /**
     * Mueve un pin independiente. La regla de inmutabilidad de los pines
     * vinculados la aplica {@link LugarService#validarMovimiento}.
     *
     * @param lugar         pin a mover
     * @param nuevaUbicacion nueva coordenada
     * @param resultado     recibe {@code null} si se movió o el código del fallo
     */
    public void moverLugar(@NonNull Lugar lugar, @NonNull Ubicacion nuevaUbicacion,
                           @NonNull MutableLiveData<ErrorCode> resultado) {
        db.getExecutor().execute(() -> {
            try {
                LugarService.validarMovimiento(lugar, nuevaUbicacion);
                Lugar movido = new Lugar(
                        lugar.getId(),
                        lugar.getTransaccionId(),
                        lugar.getNombre(),
                        lugar.getTipo(),
                        lugar.getEmoji(),
                        lugar.getDescripcion(),
                        nuevaUbicacion,
                        lugar.getEstado());
                db.lugarDao().update(movido.toEntity());
                resultado.postValue(null);
            } catch (BusinessRuleException e) {
                Log.w(TAG, "Movimiento de pin rechazado", e);
                resultado.postValue(e.getErrorCode());
            }
        });
    }

    /**
     * Elimina un lugar; sus cosas se borran en cascada por la FK.
     *
     * @param lugarId id del lugar
     */
    public void eliminarLugar(long lugarId) {
        db.getExecutor().execute(() -> {
            com.localcash.data.entities.Lugar entidad = db.lugarDao().getByIdSync(lugarId);
            if (entidad != null) {
                db.lugarDao().delete(entidad);
            }
        });
    }

    /**
     * Cierra o reabre un pin sin borrarlo.
     *
     * @param lugar    pin afectado
     * @param resultado recibe {@code null} si se actualizó o el código del fallo
     */
    public void alternarEstado(@NonNull Lugar lugar,
                               @NonNull MutableLiveData<ErrorCode> resultado) {
        db.getExecutor().execute(() -> {
            try {
                EstadoLugar nuevo = LugarService.alternarEstado(lugar.getEstado());
                db.lugarDao().updateEstado(lugar.getId(), nuevo.getValor());
                resultado.postValue(null);
            } catch (BusinessRuleException e) {
                Log.w(TAG, "Cambio de estado rechazado", e);
                resultado.postValue(e.getErrorCode());
            }
        });
    }

    /**
     * Agrega una cosa a la lista de un lugar.
     *
     * @param lugarId     id del lugar
     * @param descripcion texto de la cosa (no vacío)
     */
    public void agregarItem(long lugarId, @NonNull String descripcion) {
        db.getExecutor().execute(() -> {
            ItemLugar item = new ItemLugar(0L, lugarId, descripcion.trim(), false);
            try {
                LugarService.validarItem(item);
                db.itemLugarDao().insert(item.toEntity());
            } catch (BusinessRuleException e) {
                Log.w(TAG, "Cosa rechazada por regla de negocio", e);
            }
        });
    }

    /**
     * Marca o desmarca una cosa.
     *
     * @param item      elemento afectado
     * @param completado nuevo estado
     */
    public void marcarItem(@NonNull ItemLugar item, boolean completado) {
        db.getExecutor().execute(() ->
                db.itemLugarDao().updateCompletado(item.getId(), completado));
    }

    /**
     * Elimina una cosa de la lista.
     *
     * @param item elemento a eliminar
     */
    public void eliminarItem(@NonNull ItemLugar item) {
        db.getExecutor().execute(() -> {
            com.localcash.data.entities.ItemLugar entidad =
                    new com.localcash.data.entities.ItemLugar(
                            item.getLugarId(), item.getDescripcion(), item.isCompletado());
            entidad.setId(item.getId());
            db.itemLugarDao().delete(entidad);
        });
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
