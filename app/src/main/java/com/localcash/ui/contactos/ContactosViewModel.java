package com.localcash.ui.contactos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.localcash.data.db.AppDatabase;
import com.localcash.domain.error.BusinessRuleException;
import com.localcash.data.model.DeudaContacto;
import com.localcash.data.model.ResumenDeuda;
import com.localcash.data.repository.GastoCompartidoRepository;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.Contacto;
import com.localcash.domain.service.ContactoService;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel de la pantalla de Contactos (Feature 011).
 * <p>
 * Orquesta el DAO y el servicio de dominio. Expone la lista de contactos como
 * {@link LiveData} y el resultado de operaciones de escritura como
 * {@link MutableLiveData} de {@link ErrorCode} (o {@code null} si éxito).
 * </p>
 */
public class ContactosViewModel extends ViewModel {

    private final AppDatabase db;
    private final GastoCompartidoRepository gastoCompartidoRepository;
    private final MutableLiveData<ErrorCode> resultadoPago = new MutableLiveData<>();
    private final LiveData<List<com.localcash.data.entities.Contacto>> contactos;
    private final MutableLiveData<ErrorCode> resultado = new MutableLiveData<>();

    public ContactosViewModel(AppDatabase db) {
        this.db = db;
        this.gastoCompartidoRepository = new GastoCompartidoRepository(db);
        this.contactos = db.contactoDao().getAll();
    }

    /** Lista de contactos observada desde Room. */
    public LiveData<List<com.localcash.data.entities.Contacto>> getContactos() {
        return contactos;
    }

    /** Deuda pendiente por contacto para pintar la lista (Feature 012, T13). */
    public LiveData<java.util.List<ResumenDeuda>> getResumenDeudas() {
        return db.gastoCompartidoDao().resumenDeudasPorContactoLive();
    }

    /** Deudas pendientes de un contacto con concepto y fecha (T13). */
    public LiveData<java.util.List<DeudaContacto>> getDeudasPendientes(long contactoId) {
        return db.gastoCompartidoDao().getDeudasPendientesLive(contactoId);
    }

    /** Resultado de la última operación de escritura ({@code null} = éxito). */
    public LiveData<ErrorCode> getResultado() {
        return resultado;
    }

    /** Partes ya cobradas de un contacto: historial del diálogo de detalle. */
    public LiveData<java.util.List<com.localcash.data.model.PagoContacto>> getPagosPorContacto(
            long contactoId) {
        return db.gastoCompartidoDao().getPagosPorContactoLive(contactoId);
    }

    /** Resultado del último cobro ({@code null} = éxito). */
    public LiveData<ErrorCode> getResultadoPago() {
        return resultadoPago;
    }

    /**
     * Cobra una parte de gasto compartido (botón "Pagar" del detalle).
     *
     * @param parteId id de la parte
     */
    public void pagarParte(long parteId) {
        db.getExecutor().execute(() -> resultadoPago.postValue(
                gastoCompartidoRepository.marcarPagada(
                        parteId, java.time.Instant.now().toString())));
    }

    /**
     * Cobra varias partes en una sola transacción (botón "Pagar todo").
     *
     * @param partesIds ids de las partes pendientes
     */
    public void pagarTodas(java.util.List<Long> partesIds) {
        db.getExecutor().execute(() -> resultadoPago.postValue(
                gastoCompartidoRepository.marcarTodasPagadas(
                        partesIds, java.time.Instant.now().toString())));
    }

    /**
     * Agrega un contacto. Valida nombre y duplicados en el hilo de escritura
     * (Room no permite consultas síncronas en el hilo principal).
     */
    public void agregarContacto(Contacto contactoDominio) {
        resultado.postValue(null);
        db.getExecutor().execute(() -> {
            try {
                boolean telefonoDuplicado = db.contactoDao().existeTelefono(
                        contactoDominio.getTelefono() != null
                                ? contactoDominio.getTelefono() : "") > 0;
                boolean emailDuplicado = db.contactoDao().existeEmail(
                        contactoDominio.getEmail() != null
                                ? contactoDominio.getEmail() : "") > 0;
                ContactoService.validarContacto(
                        contactoDominio, telefonoDuplicado, emailDuplicado);

                long id = db.contactoDao().insert(contactoDominio.toEntity());
                if (id > 0) {
                    resultado.postValue(null);
                } else {
                    resultado.postValue(ErrorCode.CONTACTO_NOMBRE_VACIO);
                }
            } catch (BusinessRuleException e) {
                resultado.postValue(e.getErrorCode());
            }
        });
    }

    /**
     * Actualiza un contacto existente. Valida en el hilo de escritura.
     */
    public void actualizarContacto(Contacto contactoDominio) {
        resultado.postValue(null);
        db.getExecutor().execute(() -> {
            try {
                boolean telefonoDuplicado = db.contactoDao().existeTelefono(
                        contactoDominio.getTelefono() != null
                                ? contactoDominio.getTelefono() : "") > 0;
                boolean emailDuplicado = db.contactoDao().existeEmail(
                        contactoDominio.getEmail() != null
                                ? contactoDominio.getEmail() : "") > 0;
                ContactoService.validarContacto(
                        contactoDominio, telefonoDuplicado, emailDuplicado);

                db.contactoDao().update(contactoDominio.toEntity());
                resultado.postValue(null);
            } catch (BusinessRuleException e) {
                resultado.postValue(e.getErrorCode());
            }
        });
    }

    /**
     * Elimina un contacto. Si tiene compras asociadas o deudas pendientes,
     * SQLite rechaza el borrado (RESTRICT) y se traduce a
     * {@link ErrorCode#CONTACTO_EN_USO}.
     */
    public void eliminarContacto(long contactoId) {
        resultado.postValue(null);
        db.getExecutor().execute(() -> {
            try {
                com.localcash.data.entities.Contacto entidad = db.contactoDao().getByIdSync(contactoId);
                if (entidad != null) {
                    db.contactoDao().delete(entidad);
                }
                resultado.postValue(null);
            } catch (android.database.sqlite.SQLiteConstraintException e) {
                resultado.postValue(ErrorCode.CONTACTO_EN_USO);
            }
        });
    }

    /**
     * Convierte una lista de entidades Contacto a una lista de modelos de dominio.
     *
     * @param entidades lista de entidades Room
     * @return lista de modelos de dominio (nunca {@code null})
     */
    public static List<Contacto> aDominio(List<com.localcash.data.entities.Contacto> entidades) {
        List<Contacto> resultado = new ArrayList<>();
        if (entidades != null) {
            for (com.localcash.data.entities.Contacto e : entidades) {
                resultado.add(Contacto.fromEntity(e));
            }
        }
        return resultado;
    }
}
