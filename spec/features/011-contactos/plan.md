# plan.md — Plan Técnico de Implementación

> **Identificación:** 011-contactos | **Fecha:** 2026-10-25 | **Autor:** Agente SDD
>
> **Propósito de este documento:** Definir CÓMO se implementará la feature descrita en `spec.md`:
> componentes, decisiones técnicas, riesgos y dependencias. Es el puente entre el requisito y las
> tareas ejecutables (`tasks.md`).
>
> **Regla:** Ninguna decisión de este archivo puede contradecir `.agent.md` ni `tech-stack.md`.
> Si la contradicción es inevitable, primero se actualiza la constitución con su registro en `doc/`.

---

## 1. Componentes Involucrados

| Archivo / Módulo                        | Rol en la feature                          | Acción (crear/modificar) | Complejidad estimada |
|-----------------------------------------|--------------------------------------------|--------------------------|----------------------|
| `data/entities/Contacto.java`           | Entidad Room: tabla CONTACTOS              | CREAR                    | BAJA                 |
| `data/dao/ContactoDao.java`             | DAO: CRUD de contactos, consulta de contacto por id, verificar existencia por teléfono/email | CREAR | MEDIA |
| `domain/model/Contacto.java`            | Modelo de dominio: inmutable, sin setters  | CREAR                    | BAJA                 |
| `domain/service/ContactoService.java`   | Validación de negocio: nombre no vacío, teléfono/email únicos, restricción de eliminación | CREAR | MEDIA |
| `data/db/AppDatabase.java`              | Agregar entidad Contacto, DAO ContactoDao, migrar versión 3 → 4 | MODIFICAR | ALTA |
| `ui/contactos/ContactosActivity.java`   | Activity: lista de contactos, agregar, editar, eliminar, ver detalle | CREAR | ALTA |
| `ui/contactos/ContactosViewModel.java`  | ViewModel: orquesta DAO y servicio, expone LiveData | CREAR | MEDIA |
| `res/layout/activity_contactos.xml`     | Layout de la lista de contactos            | CREAR                    | BAJA |
| `res/layout/item_contacto.xml`          | Layout de una fila de contacto             | CREAR                    | BAJA |
| `res/layout/dialog_contacto.xml`        | Layout del dialogo de agregar/editar contacto | CREAR | BAJA |
| `res/layout/detalle_contacto.xml`       | Layout del detalle de contacto con historial | CREAR | BAJA |
| `res/values/strings.xml`                | Textos de contacto: títulos, botones, mensajes de error | MODIFICAR | BAJA |
| `test/ContactoServiceTest.java`         | Tests de dominio: validación de nombre, teléfono, email | CREAR | MEDIA |
| `test/ContactoDaoTest.java`             | Tests de persistencia: CRUD, unicidad, RESTRICT en eliminación | CREAR | MEDIA |

**Contrato de datos / interfaz expuesta:**

- Entrada esperada: nombre (texto no vacío), teléfono (texto opcional), email (texto opcional), notas (texto opcional).
- Salida garantizada: Contacto con id asignado, o error de negocio (`CONTACTO_NOMBRE_VACIO`, `CONTACTO_TELEFONO_DUPLICADO`, `CONTACTO_EMAIL_DUPLICADO`, `CONTACTO_EN_USO`).
- Errores posibles (códigos y significado):
  - `CONTACTO_NOMBRE_VACIO`: el nombre está vacío o solo tiene espacios.
  - `CONTACTO_TELEFONO_DUPLICADO`: ya existe otro contacto con ese teléfono (case-insensitive, ignorando NULLs).
  - `CONTACTO_EMAIL_DUPLICADO`: ya existe otro contacto con ese email (case-insensitive, ignorando NULLs).
  - `CONTACTO_EN_USO`: el contacto tiene compras asociadas o deudas pendientes/parciales; no se puede eliminar.

---

## 2. Decisiones de Arquitectura

1. **¿Dónde vive la validación de unicidad de teléfono/email?**
   - Opciones consideradas: solo en DAO (índice único parcial de SQLite), solo en dominio (ContactoService), en ambos.
   - Decisión: en ambos. SQLite impone la restricción con `UNIQUE` parcial (garantía de motor); ContactoService valida antes de persistir para devolver un mensaje de negocio amigable.
   - Justificación: el índice parcial de SQLite es la fuente de verdad; el dominio evita que el usuario vea un error de BD crudo.
   - Consecuencia aceptada: dos lugares que conocen la regla de unicidad; si cambia, hay que actualizar ambos.

2. **¿Cómo se representa el usuario principal en la agenda?**
   - Opciones consideradas: fila propia en CONTACTOS con nombre "Yo", no existe en CONTACTOS (actor implícito).
   - Decisión: no existe en CONTACTOS (actor implícito).
   - Justificación: `data-model.md` §4.1: "La app no tiene usuario propio en la base de datos. CONTACTOS es la agenda de terceros; el propietario de la app es el actor implícito y nunca es una fila."
   - Consecuencia aceptada: los contactos son solo terceros; el saldo del usuario principal se gestiona en CUENTAS/TRANSACCIONES.

3. **¿Migración Room 3 → 4 o nueva base desde cero?**
   - Opciones consideradas: migración 3 → 4 (agregar tabla CONTACTOS), destruir y recrear.
   - Decisión: migración 3 → 4.
   - Justificación: `.agent.md` §6.11 prohíbe `fallbackToDestructiveMigration()` y `DROP TABLE` como estrategia de actualización.
   - Consecuencia aceptada: hay que escribir una migración Room `MIGRATION_3_4` que cree la tabla CONTACTOS y sus índices.

4. **¿Cómo se consulta el historial de un contacto?**
   - Opciones consideradas: consultar COMPRAS_GRUPALES + DEUDAS_PARTICIPANTES por contacto_id, crear una vista SQL.
   - Decisión: consultar COMPRAS_GRUPALES y DEUDAS_PARTICIPANTES por contacto_id desde el DAO.
   - Justificación: las vistas SQL quedan fuera de la verificación en compilación de Room (`data-model.md` §8).
   - Consecuencia aceptada: el DAO expone dos consultas (compras donde es pagador, deudas donde es deudor) y el ViewModel las combina.

---

## 3. Riesgos Identificados

| # | Riesgo                                        | Probabilidad | Impacto | Mitigación                                       | Dueño       |
|---|-----------------------------------------------|--------------|---------|--------------------------------------------------|-------------|
| 1 | La migración Room 3 → 4 no se aplica correctamente en dispositivos existentes | MEDIA | ALTA | Probar con MigrationTestHelper siembrando datos v3 y verificando v4 | Agente SDD |
| 2 | Eliminar contacto con deudas pendientes falla en silencio por RESTRICT de SQLite | BAJA | MEDIA | Capturar SQLException de SQLite y traducir a `CONTACTO_EN_USO` en el repositorio | Agente SDD |
| 3 | Teléfono/email duplicado con distinta capitalización (case-insensitive) | MEDIA | MEDIA | COLLATE NOCASE en columnas e índices únicos parciales | Agente SDD |
| 4 | Subestimación de complejidad del UI (Activity, RecyclerView, dialogs) | MEDIA | MEDIA | Seguir el patrón de `CuentasActivity`/`LugaresAdapter` ya existentes | Agente SDD |

**Criterio de abortar/replantear:** Si la migración Room 3 → 4 no puede probarse con `MigrationTestHelper` en JVM, se escala a revisión de diseño antes de continuar.

---

## 4. Dependencias Externas

| Dependencia                        | Tipo (servicio/librería/dato/equipo) | Estado del acuerdo | Plan B si falla        |
|------------------------------------|---------------------------------------|--------------------|------------------------|
| Room (ya en proyecto)              | Librería                              | DISPONIBLE         | N/A                    |
| Emulador Android API 36            | Equipo                                | DISPONIBLE         | Pruebas en JVM con Robolectric |

**Requisitos previos del entorno:** Emulador Android activo para `connectedCheck`. Ninguna credencial real en documentos.

---

## 5. Estrategia de verificación

- Criterios de aceptación → tests: cada CA de `spec.md` mapea a un test en `ContactoServiceTest` o `ContactoDaoTest`.
- Pruebas manuales necesarias: agregar/editar/eliminar contacto en emulador, verificar mensaje `CONTACTO_EN_USO`, verificar historial de contacto.
- Datos de prueba requeridos: contacto "Juan Pérez" con teléfono "555-0100", contacto "María García" sin compras, contacto con deuda pendiente.
- Criterio de "terminado" (Definition of Done): código + tests verdes con los comandos de `.agent.md` §3 + documentación actualizada + registro en `doc/011-contactos.md`.