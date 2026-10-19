# doc/008-historial.md — Registro de Memoria: Historial de Movimientos (estado parcial)

> **Propósito de este archivo:** Dejar constancia de qué se decidió y qué se hizo en la Feature 008
> (`spec/features/008-historial/`) y, sobre todo, **del estado real a la fecha**: una entrega
> **parcial** con deuda técnica explícita. No describe el estado final, que aún no existe.

---

## Ficha de la tarea

| Campo | Valor |
|---|---|
| **Fecha** | 2026-10-18 |
| **Tarea ejecutada** | Feature 008 — Historial de movimientos: lista, botón flotante y panel de registro |
| **Responsable** | GG |
| **Estado** | **Parcial** (En Progreso). No cumple todavía la Definition of Done completa. |
| **Rama** | `feat/008-historial` |

---

## Tarea ejecutada (resumen)

Se entregó el **historial de movimientos** del Módulo 1 y se habilitó el registro de nuevos
movimientos desde la propia vista del historial. El cambio de mayor impacto fue **reemplazar la
antigua pestaña "Nuevo"** (una vista completa dedicada a registrar) por un **panel inferior** que se
abre con un botón flotante, lo que redujo el contenedor de vistas de 5 a 4 y obligó a corregir el
mapeo de la navegación inferior.

**Lo que quedó funcionando:**
- Pestaña "Movimientos" con lista desplazable (concepto, fecha, monto con signo/color, icono).
- Botón flotante "+" que abre un panel inferior con el formulario completo.
- Registro de movimientos con validación de dominio (monto, cuenta, saldo insuficiente) y manejo de
  éxito/error.
- Navegación inferior con índices correctos (Mapa/Contactos ya no estaban cruzados) y títulos del
  encabezado alineados a la pestaña activa.

**Deuda técnica explícita (ver `tasks.md`):**
- La lista todavía se alimenta de **datos de prueba** (`generarTransaccionesMock()`, 10 movimientos),
  no de la base de datos. Un movimiento recién guardado se persiste pero **no aparece aún** en la lista.
- La **geolocalización** está diseñada y especificada, pero **no implementada**: no hay columnas de
  coordenadas en la entidad, ni permisos en el manifiesto, ni captura real, ni migración de base de datos.
- El **saldo total** no se refresca dinámicamente en la vista de historial tras registrar.

---

## Archivos creados / modificados

**Creados:**
- `spec/features/008-historial/spec.md`, `plan.md`, `tasks.md`
- `doc/008-historial.md` (este archivo)
- `app/src/main/res/layout/bottom_sheet_transaccion.xml`
- (de la entrega de lista) `app/src/main/res/layout/view_historial.xml`, `item_transaccion.xml`,
  `ui/cuentas/HistorialAdapter.java`

**Modificados:**
- `app/src/main/java/com/localcash/ui/cuentas/CuentasActivity.java` — navegación, botón flotante,
  panel de registro, índices del contenedor de vistas y títulos.
- `app/src/main/res/values/strings.xml` — cadenas del título del panel; retiro de cadenas huérfanas.
- `app/src/main/res/menu/bottom_nav_menu.xml` — pestaña "Nuevo" → "Movimientos".

**Eliminados:**
- `app/src/main/res/layout/view_transaccion.xml` (la vista propia de registro, sustituida por el panel).

**Renuméricación asociada:**
- `doc/008-actualizacion-diagrama.md` → `doc/009-actualizacion-diagrama.md`, para respetar la
  numeración compartida `spec/features/NNN` ↔ `doc/NNN` (ese registro se había creado como `008`
  cuando `007` era la feature activa).

---

## Decisiones tomadas

1. **El formulario de registro es un panel inferior (BottomSheet), no una pestaña.**
   *Justificación:* una acción corta no debería costar una navegación completa. Se sacrificó la
   quinta vista y se corrigió el mapeo del menú inferior.
2. **"Nueva cuenta" se mantiene como diálogo; su unificación con el panel se difiere a la Feature 010.**
   *Justificación:* acotar el alcance y no mezclar dos formularios con reglas distintas en un mismo cambio.
3. **Geolocalización como "best-effort" y con regla "ambas o ninguna".**
   *Justificación:* el contrato de `data-model.md` exige `CHECK ((latitud IS NULL) = (longitud IS NULL))`
   y un rango válido; denegar el permiso nunca debe impedir registrar el movimiento.
4. **Captura de ubicación con el framework de Android, sin dependencia nueva.**
   *Justificación:* `.agent.md` §6.6 obliga a registrar cualquier dependencia en `tech-stack.md`;
   `play-services-location` no está en el stack y rompería el pilar 100% offline.
5. **Los datos mock son temporales y verificables.**
   *Justificación:* permiten validar la UI del historial antes del cableado a la base de datos; se
   marcan como deuda técnica para no confundirlos con datos reales.

---

## Validación

- `./gradlew lint`: BUILD SUCCESSFUL (0 errores; 41 advertencias preexistentes, ninguna introducida
  por los archivos cambiados).
- `./gradlew testDebugUnitTest`: 13 tests, 0 fallos.
- `./gradlew assembleDebug`: BUILD SUCCESSFUL (`app/build/outputs/apk/debug/app-debug.apk`).
- `./gradlew connectedCheck`: **no ejecutado** (requiere dispositivo/emulador activo).

> Estas validaciones cubren el estado **parcial**. No habilitan el cierre de la feature: faltan los
> criterios CA-1, CA-6 y CA-7 y la verificación de migración.

---

## Lecciones aprendidas

- **Causa raíz del botón flotante "muerto":** el manejador hacía `setDisplayedChild(2)` y acto
  seguido `setSelectedItemId(nav_movimientos)`, lo que **re-disparaba el listener de la navegación
  inferior** y volvía a fijar la vista en 1. La vista quedaba "atrás" del panel. Lección: no mezclar
  el control programático del contenedor con el evento del menú que lo gobierna.
- **Eliminar una vista obliga a reindexar todo lo que la referencia.** Al pasar de 5 a 4 vistas se
  descubrió que Mapa y Contactos estaban mapeados al revés; el error era invisible hasta que se
  probaba la navegación. Lección: al cambiar el número de vistas, revisar navegación, restauración de
  estado y títulos en un mismo cambio.
- **Documentar "parcial" es parte del contrato.** Marcar como hecho lo que no está en el código
  contradice `.agent.md` (la memoria es fuente de verdad). Este registro refleja el estado real.

---

## Próximos pasos pendientes

1. **Cablear el historial a la base de datos** y eliminar los datos de prueba (T8, T9, T10).
2. **Refrescar la lista y el saldo** tras registrar un movimiento (T11, T12, T17).
3. **Implementar la geolocalización**: columnas, migración 2→3, permisos, captura y persistencia
   (T19-T26).
4. **Verificar la migración** y cubrir los criterios de aceptación con tests (T27, T28).
5. **Ejecutar la Definition of Done completa** (incluido `connectedCheck`) y cerrar el registro (T29, T30).

Estos pasos están detallados y estado-lizados en `spec/features/008-historial/tasks.md`.
