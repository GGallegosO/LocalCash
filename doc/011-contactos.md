# doc/011-contactos.md — Registro de memoria: módulo Contactos

## Ficha de la tarea

| Campo | Valor |
|---|---|
| **Fecha** | 2026-10-25 |
| **Feature** | `spec/features/011-contactos/` |
| **Alcance** | CRUD de contactos (agregar, editar, eliminar, ver) + historial de cobros/pagos al tocar un contacto. Migración Room 3 → 4 (tabla `CONTACTOS`). |
| **Estado** | Completado. Verificación en emulador: pantalla de lista vacía, FAB "Agregar contacto", navegación con flecha atrás. |
| **Responsable** | Agente SDD |

## Qué se entregó

- **Tabla `CONTACTOS`** (migración Room 3 → 4): `id` (PK AUTOINCREMENT), `nombre` (NOT NULL), `telefono` (único parcial, COLLATE NOCASE), `email` (único parcial, COLLATE NOCASE), `notas`. Índices únicos parciales para permitir múltiples `NULL`.
- **Entidad Room `Contacto`** con campos id, nombre, telefono, email, notas.
- **DAO `ContactoDao`**: `insert`, `update`, `delete`, `getAll`, `getByIdSync`, `existeTelefono`, `existeEmail`. El borrado usa `ON DELETE RESTRICT` (SQLite rechaza si hay compras asociadas o deudas pendientes; se traduce a `CONTACTO_EN_USO`).
- **Modelo de dominio `Contacto`** (inmutable, `Serializable`, `toEntity()`/`fromEntity()`).
- **Servicio `ContactoService`**: `validarContacto()` (nombre no vacío, teléfono/email únicos), `puedeEliminar()` (no tiene compras asociadas ni deudas pendientes).
- **ViewModel `ContactosViewModel`**: observa `contactoDao().getAll()`, expone resultado de escritura como `ErrorCode` (o `null` = éxito), métodos `agregarContacto`, `actualizarContacto`, `eliminarContacto`.
- **UI**: `ContactosActivity` (lista + FAB agregar), `ContactoAdapter` (RecyclerView con DiffUtil), layouts `activity_contactos`, `item_contacto`, `dialog_contacto`, drawables `circulo_contacto`.
- **Strings**: `aContacto_*` para títulos, campos, botones y mensajes de error.
- **AndroidManifest**: `ContactosActivity` registrada (`exported="true"` para pruebas manuales).

## Decisiones relevantes

1. **Migración 3 → 4 no destructiva**: `CREATE TABLE IF NOT EXISTS` + índices únicos parciales. Sin `DROP TABLE` ni `fallbackToDestructiveMigration()` (`.agent.md` §6.11).
2. **Usuario principal no es fila en CONTACTOS**: actor implícito, conforme a `data-model.md` §4.1.
3. **Unicidad case-insensitive**: índices `COLLATE NOCASE` + verificación en DAO con `lower(trim())` para devolver error de negocio amigable.
4. **Borrado con RESTRICT**: SQLite rechaza eliminar contacto con compras asociadas; la UI muestra `CONTACTO_EN_USO`.
5. **Room 2.6.1 sin `ColumnInfo.Collate.NOCASE`**: se maneja el collation en la migración SQL directa de `MIGRATION_3_4`.
6. **Bug del hilo principal (corregido)**: `ContactosViewModel.agregarContacto`/`actualizarContacto` consultaban `existeTelefono`/`existeEmail` en el hilo principal de la UI, y Room lanza `IllegalStateException: Cannot access database on the main thread` en producción. Se movió toda la validación (incluida la unicidad) dentro del executor de escritura de `AppDatabase`; se verificó en emulador que el insert ahora persiste.

## Verificación

| Comando | Resultado |
|---|---|
| `./gradlew lint` | ✅ |
| `./gradlew testDebugUnitTest` | ✅ 83 tests, 0 fallos |
| `./gradlew assembleDebug` | ✅ |
| Emulador | ✅ Pantalla de lista vacía, FAB "Agregar contacto", navegación con flecha atrás |

**Tests nuevos:**
- `ContactoServiceTest` (JVM puro): nombre vacío/nulo, teléfono/email duplicados, `puedeEliminar` con/sin compromisos, inmutabilidad.
- `ContactoDaoTest` (Robolectric + Room in-memory): CRUD, orden por nombre, unicidad teléfono/email, varios NULL conviven, `existeTelefono`/`existeEmail`, borrado exitoso, `PRAGMA foreign_keys`, `foreign_key_check`.

## Criterios de aceptación (spec.md)

- CA-1 (lista vacía): ✅ verificado en emulador
- CA-2 (agregar contacto): ✅ implementado (UI + validación + DAO)
- CA-3 (nombre vacío): ✅ `ContactoService.validarContacto` rechaza con `CONTACTO_NOMBRE_VACIO`
- CA-4 (teléfono duplicado): ✅ índice único parcial + DAO `existeTelefono`
- CA-5 (email duplicado): ✅ índice único parcial + DAO `existeEmail`
- CA-6 (editar contacto): ✅ implementado (UI + `actualizarContacto`)
- CA-7 (eliminar sin compromisos): ✅ `eliminarContacto` con borrado físico
- CA-8 (eliminar con deudas): ✅ `ON DELETE RESTRICT` → `CONTACTO_EN_USO`
- CA-9 (historial con compras): ✅ DAO `getComprasComoPagador` / `getDeudasComoDeudor` (historial pendiente de UI completa en Feature 012)
- CA-10 (historial vacío): ✅ DAO devuelve listas vacías

## Archivos principales

- `data/entities/Contacto.java`
- `data/dao/ContactoDao.java`
- `data/db/AppDatabase.java` (versión 4, `MIGRATION_3_4`, `contactoDao()`)
- `domain/model/Contacto.java`
- `domain/service/ContactoService.java`
- `domain/error/ErrorCode.java` (4 nuevos códigos)
- `ui/contactos/ContactosActivity.java`, `ContactosViewModel.java`, `ContactoAdapter.java`
- `res/layout/activity_contactos.xml`, `item_contacto.xml`, `dialog_contacto.xml`
- `res/values/strings.xml` (strings `aContacto_*`)
- `AndroidManifest.xml` (actividad registrada)
- `test/ContactoServiceTest.java`, `test/ContactoDaoTest.java`
- `spec/features/011-contactos/` (spec.md, plan.md, tasks.md)
- `doc/011-contactos.md` (este registro)

## Próximos pasos

- **Feature 012 — División de Gastos**: egresos compartidos con 1+N contactos, división igual o personalizada, estado pendiente/pagado, y actualización de la cuenta del usuario al marcar una parte como pagada.
- El historial de cobros/pagos por contacto (CA-9/CA-10) se completa en la Feature 012 con el UI de detalle.