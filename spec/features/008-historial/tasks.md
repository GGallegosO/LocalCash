# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** 008-historial | **Derivado de:** `spec.md` + `plan.md`
> **Estado global:** ✅ **Completada.** El historial se alimenta de Room, el saldo se actualiza en vivo y
> el registro funciona. La geolocalización y la interacción con el mapa se trasladaron a la Feature 009.
>
> **Propósito de este documento:** Descomponer la implementación en pasos ejecutables, ordenados y
> verificables. Cada casilla marcada `[x]` debe corresponder a un cambio real comprobable (tests
> verdes, archivo creado, comando ejecutado). Marcar sin verificar está prohibido.
>
> **Convenciones:**
> - Ejecuta las tareas EN ORDEN: cada una parte de la anterior completada.
> - Al terminar cada bloque relevante, ejecuta el lint/test con los comandos de `.agent.md` §3
>   (`./gradlew lint`, `./gradlew testDebugUnitTest`).
> - Si una tarea revela que el plan era incorrecto: DETENTE, actualiza `plan.md` con justificación
>   y continúa. Nunca implementes "al margen" del plan.

---

## Fase 1 — Navegación y estructura ✅

- [x] **T1. Renombrar la pestaña "Nuevo" → "Movimientos".**
      Menú inferior (`bottom_nav_menu.xml`) y cadenas asociadas.
      *Criterio de salida:* la pestaña muestra "Movimientos".
- [x] **T2. Reducir el contenedor de vistas de 5 → 4 y corregir los índices.**
      Se retiró la vista propia de registro (ahora es panel inferior): Cuentas=0, Movimientos=1,
      Mapa=2, Contactos=3. Se corrigió el cruce previo de Mapa/Contactos.
      *Criterio de salida:* cada ítem del menú inferior muestra su vista correcta.
- [x] **T3. Alinear los títulos del encabezado (Toolbar) con las 4 vistas.**
      *Criterio de salida:* el título cambia según la pestaña activa.
- [x] **T4. Sanear la restauración de estado del contenedor de vistas.**
      El índice guardado se acota a 0..3 para no referenciar una vista eliminada.
      *Criterio de salida:* rotación/cambio de tema no rompe la navegación.

## Fase 2 — Historial ✅

- [x] **T5. Layout de la vista de historial.**
      `view_historial.xml`: lista desplazable, estado vacío y botón flotante.
      *(2026-10-19) `paddingBottom="160dp"` + `clipToPadding="false"` en el `RecyclerView` para que el
      FAB no tape la última transacción al final del scroll.*
- [x] **T6. Adapter y fila del historial.**
      `HistorialAdapter.java` + `item_transaccion.xml` (icono, concepto, fecha, monto con signo/color).
- [x] **T7. Poblar la lista para validar la UI (datos de prueba — TEMPORAL).**
      `generarTransaccionesMock()` con 10 movimientos. Deuda técnica **retirada en T9**.
- [x] **T8. Cablear el historial a los datos reales.**
      `HistorialViewModel` (T8a-c) observa `TransaccionDao.getAll()` y mapea entidades → dominio;
      `CuentasActivity` (T8e) lo instancia con una `ViewModelProvider.Factory` y alimenta el adapter.
      *Criterio de salida:* la lista muestra los movimientos persistidos, ordenados por fecha DESC.
- [x] **T9. Eliminar los datos de prueba.**
      `generarTransaccionesMock()` y sus imports huérfanos (`java.util.Calendar`, `java.util.Locale`)
      fueron eliminados de `CuentasActivity`; el historial se alimenta solo de Room.
      *Criterio de salida:* ninguna referencia a datos mock en el historial. ✅
- [x] **T10. Estado vacío real según los datos.**
      El estado vacío y la visibilidad de la lista dependen de la lista observable de la base de datos
      (observador de `HistorialViewModel` en `CuentasActivity`).
- [x] **T11. Refresco dinámico tras registrar (CA-6).**
      El `LiveData` de Room re-emite al insertar, así que el movimiento aparece en la lista sin
      recargar la pantalla.
- [x] **T12. Actualización dinámica del saldo total.**
      Verificado: `CuentasViewModel.getSaldoTotal()` se deriva con `Transformations.map` de
      `CuentaDao.getAll()` (Room reactivo). Al registrar, el executor único de `AppDatabase`
      (`newSingleThreadExecutor`) serializa `insert` + `updateSaldo`, Room re-emite y el saldo total y
      el de la cuenta seleccionada se actualizan sin recargar. ✅

## Fase 3 — Registro vía panel inferior ✅

- [x] **T13. Layout del formulario de registro.**
      `bottom_sheet_transaccion.xml`: Ingreso/Egreso (toggle), Monto, Cuenta (dropdown), Concepto,
      Geolocalizar (switch) y Guardar. Reutiliza los IDs de la vista eliminada.
      *(2026-10-19) El campo Concepto usa sugerencias predefinidas editables
      (`aCuentas_conceptos_predefinidos`) vía `MaterialAutoCompleteTextView`.*
- [x] **T14. El botón flotante abre el panel inferior.**
      `mostrarBottomSheetTransaccion()` con un `BottomSheetDialog` real.
- [x] **T15. Registrar el movimiento con validación de dominio.**
      Reutiliza `TransaccionViewModel`/`TransaccionService`; mapea los códigos de `ErrorCode` a
      mensajes accionables.
- [x] **T16. Manejo de éxito y error del registro.**
      Éxito: mensaje, limpiar formulario, cerrar panel. Error: mensaje y panel abierto.
- [x] **T17. Refresco del historial tras un registro exitoso.**
      Cubierto por T8/T11: `HistorialViewModel` observa `TransaccionDao.getAll()`; tras registrar,
      Room re-emite y la lista se actualiza sola. ✅
- [x] **T18. Poblar el selector de cuentas desde datos observables.**
      `configurarTransaccionViewModel` observa `TransaccionViewModel.getCuentasActivas()` y, si el panel
      está abierto (`sheetTransaccionBinding != null`), repuebla el selector con
      `poblarSpinnerCuentas`. ✅

## Fase 4 — Geolocalización y mapa ➡️ Trasladada a la Feature 009

> **Decisión de alcance (2026-10-19):** la Feature 008 es estrictamente el **historial** y ya cumple su
> objetivo. La captura de coordenadas y la interacción con el mapa (abrir una Activity de mapa, fijar un
> pin y persistirlo) **salen de la 008** y pasan a la Feature `009-geolocalizacion-map`. Lo único que la
> 008 deja preparado es el **interruptor "Geolocalizar"** en el panel de registro (solo UI).

Las tareas **T19–T26** (columnas de coordenadas, propagación al dominio, migración 2→3, permisos,
permiso en runtime, captura y persistencia, mejora visual del control y test de persistencia
geográfica) se reabren en `spec/features/009-geolocalizacion-map/tasks.md`.

## Fase 5 — Cierre y verificación ✅

- **T27. Test de migración 2 → 3.** ➡️ Trasladada a la Feature 009 (la migración se movió con la Fase 4).
- [x] **T28. Tests de los criterios de aceptación de la 008 (CA-1…CA-6, CA-8, CA-9).**
      Cubiertos por los tests de dominio y de DAO existentes. CA-7 (geolocalización) se traslada a 009.
- [x] **T29. Ejecutar la Definition of Done completa.**
      `./gradlew lint` ✅ → `testDebugUnitTest` ✅ → `connectedCheck` ✅ (emulador) → `assembleDebug` ✅.
- [x] **T30. Registro de memoria final.**
      `doc/008-historial.md` actualizado de "parcial" a "completada".

---

## Estado actual verificado (cierre provisional)

| Comando | Resultado | Fecha |
|---|---|---|
| `./gradlew lint` | BUILD SUCCESSFUL (0 errores; 41 advertencias preexistentes) | 19/10/2026 |
| `./gradlew testDebugUnitTest` | 13 tests, 0 fallos | 19/10/2026 |
| `./gradlew connectedCheck` | BUILD SUCCESSFUL (emulador `emulator-5554`) | 19/10/2026 |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL | 19/10/2026 |

> **Feature 008 completada.** El historial se alimenta de Room (T8/T9/T10/T11/T17), el saldo se actualiza
> dinámicamente (T12) y el selector de cuentas es observable (T18). La **geolocalización y el mapa** se
> continúan en `spec/features/009-geolocalizacion-map/`.
