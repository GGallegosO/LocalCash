package com.localcash.domain.service;

import com.localcash.domain.error.BusinessRuleException;
import com.localcash.domain.error.ErrorCode;
import com.localcash.domain.model.Contacto;

/**
 * Reglas de negocio de los contactos de la agenda local.
 * <p>
 * Lógica pura, sin Android ni Room (`.agent.md` §5.1). La UI y los ViewModels
 * orquestan; este servicio decide si un contacto es válido para persistir y si
 * puede eliminarse.
 * </p>
 */
public final class ContactoService {

    private ContactoService() {
    }

    /**
     * Valida un contacto antes de persistirlo.
     *
     * @param contacto contacto a validar (no nulo)
     * @param contactoExistenteMismoTelefono {@code true} si ya existe otro
     *                                       contacto con el mismo teléfono
     * @param contactoExistenteMismoEmail    {@code true} si ya existe otro
     *                                       contacto con el mismo email
     * @throws BusinessRuleException con {@link ErrorCode} correspondiente si
     *                               el nombre está vacío o hay duplicado
     */
    public static void validarContacto(
            Contacto contacto,
            boolean contactoExistenteMismoTelefono,
            boolean contactoExistenteMismoEmail) {
        if (contacto == null) {
            throw new BusinessRuleException(
                    ErrorCode.CONTACTO_NOMBRE_VACIO, "Contacto nulo");
        }
        if (contacto.getNombre() == null || contacto.getNombre().trim().isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.CONTACTO_NOMBRE_VACIO,
                    "El nombre del contacto está vacío");
        }
        if (contactoExistenteMismoTelefono) {
            throw new BusinessRuleException(
                    ErrorCode.CONTACTO_TELEFONO_DUPLICADO,
                    "Ya existe un contacto con ese teléfono");
        }
        if (contactoExistenteMismoEmail) {
            throw new BusinessRuleException(
                    ErrorCode.CONTACTO_EMAIL_DUPLICADO,
                    "Ya existe un contacto con ese email");
        }
    }

    /**
     * Indica si un contacto puede eliminarse físicamente.
     *
     * @param tieneComprasAsociadas {@code true} si el contacto figura como
     *                              pagador en al menos una compra grupal
     * @param tieneDeudasPendientes {@code true} si el contacto tiene al menos
     *                              una deuda con estado pendiente o parcial
     * @return {@code true} si se puede eliminar; {@code false} si tiene
     *         compromisos pendientes
     */
    public static boolean puedeEliminar(
            boolean tieneComprasAsociadas, boolean tieneDeudasPendientes) {
        return !tieneComprasAsociadas && !tieneDeudasPendientes;
    }
}
