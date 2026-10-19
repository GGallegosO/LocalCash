package com.localcash.ui.cuentas;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.localcash.domain.model.Transaccion;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel de la Vista "Movimientos" (historial de transacciones).
 * <p>
 * <b>Arquitectura (`.agent.md` §5.1):</b> pertenece a la capa {@code ui/}
 * (arquitectura por capas), que solo orquesta y traduce; no contiene reglas de
 * negocio ni lógica de dinero. Expone a la vista una lista <b>observable</b> de
 * movimientos del modelo de dominio, de modo que la UI reaccione a los cambios
 * sin consultar la base de datos directamente.
 * </p>
 * <p>
 * <b>Estado (T8a - estructura base):</b> la lista se expone por ahora vacía y
 * todavía <b>no</b> consulta la base de datos. La inyección del
 * {@code TransaccionDao}, la observación de Room y el mapeo de entidades a
 * dominio se completan en T8b.
 * </p>
 */
public class HistorialViewModel extends ViewModel {

    /**
     * Lista observable de movimientos del historial.
     * <p>
     * Hoy es una lista vacía de marcador (sin fuente de datos). En T8b pasará a
     * ser la consulta observable del DAO de transacciones.
     * </p>
     */
    private final MutableLiveData<List<Transaccion>> transacciones =
            new MutableLiveData<>(new ArrayList<>());

    /**
     * Constructor vacío de la estructura base.
     * <p>
     * En T8b se inyectará aquí el {@code TransaccionDao} para alimentar
     * {@link #transacciones}.
     * </p>
     */
    public HistorialViewModel() {
    }

    /**
     * Observa los movimientos del historial.
     *
     * @return LiveData con la lista de movimientos (por ahora, vacía)
     */
    public LiveData<List<Transaccion>> getTransacciones() {
        return transacciones;
    }
}
