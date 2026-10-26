package com.localcash.ui.contactos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.localcash.data.db.AppDatabase;
import com.localcash.domain.error.BusinessRuleException;
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
    private final LiveData<List<com.localcash.data.entities.Contacto>> contactos;
    private final MutableLiveData<ErrorCode> resultado = new MutableLiveData<>();

    public ContactosViewModel(AppDatabase db) {
        this.db = db;
        this.contactos = db.contactoDao().getAll();
    }

    /** Lista de contactos observada desde Room. */
    public LiveData<List<com.localcash.data.entities.Contacto>> getContactos() {
        return contactos;
    }

    /** Resultado de la última operación de escritura ({@code null} = éxito). */
    public LiveData<ErrorCode> getResultado() {
        return resultado;
    }

    /**
     * Agrega un contacto. Valida antes de persistir.
     */
    public void agregarContacto(Contacto contactoDominio) {
        resultado.postValue(null);

        boolean telefonoDuplicado = db.contactoDao().existeTelefono(
                contactoDominio.getTelefono() != null ? contactoDominio.getTelefono() : "") > 0;
        boolean emailDuplicado = db.contactoDao().existeEmail(
                contactoDominio.getEmail() != null ? contactoDominio.getEmail() : "") > 0;

        try {
            ContactoService.validarContacto(contactoDominio, telefonoDuplicado, emailDuplicado);
        } catch (BusinessRuleException e) {
            resultado.postValue(e.getErrorCode());
            return;
        }

        db.getExecutor().execute(() -> {
            long id = db.contactoDao().insert(contactoDominio.toEntity());
            if (id > 0) {
                resultado.postValue(null);
            } else {
                resultado.postValue(ErrorCode.CONTACTO_NOMBRE_VACIO);
            }
        });
    }

    /**
     * Actualiza un contacto existente.
     */
    public void actualizarContacto(Contacto contactoDominio) {
        resultado.postValue(null);

        boolean telefonoDuplicado = false;
        boolean emailDuplicado = false;
        if (contactoDominio.getTelefono() != null) {
            telefonoDuplicado = db.contactoDao().existeTelefono(
                    contactoDominio.getTelefono()) > 0;
        }
        if (contactoDominio.getEmail() != null) {
            emailDuplicado = db.contactoDao().existeEmail(
                    contactoDominio.getEmail()) > 0;
        }

        try {
            ContactoService.validarContacto(contactoDominio, telefonoDuplicado, emailDuplicado);
        } catch (BusinessRuleException e) {
            resultado.postValue(e.getErrorCode());
            return;
        }

        db.getExecutor().execute(() -> {
            db.contactoDao().update(contactoDominio.toEntity());
            resultado.postValue(null);
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
