# doc/008-historial.md — Registro de Memoria: Historial de Movimientos (Completada)

> **Propósito de este archivo:** Dejar constancia de qué se decidió y qué se hizo en la Feature 008
> (`spec/features/008-historial/`) y de su estado real: **completada**. La geolocalización y el mapa se
> trasladaron a la Feature 009; aquí se registra ese traspaso de alcance.

---

## Ficha de la tarea

| Campo | Valor |
|---|---|
| **Fecha** | 2026-10-18 |
| **Tarea ejecutada** | Feature 008 — Historial de movimientos: lista, botón flotante y panel de registro |
| **Responsable** | GG |
| **Estado** | **Completada.** Historial cableado a Room, sin datos de prueba, con saldo dinámico y Definition of Done ejecutada (incluido `connectedCheck`). La geolocalización/mapa se trasladó a la Feature 009. |
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
- Historial cableado a Room mediante `HistorialViewModel`: la lista y el estado vacío se actualizan en
  vivo y un movimiento recién guardado aparece sin recargar (T8/T10/T11/T17).
- **Saldo dinámico (T12):** al registrar un movimiento, el saldo de la cuenta y el **saldo total** se
  actualizan solos. `CuentasViewModel.getSaldoTotal()` se deriva de `CuentaDao.getAll()` (Room reactivo)
  y las escrituras se serializan en el executor único de `AppDatabase`.
- El selector de cuentas del panel se rellena de forma **observable** (T18): se refresca si las cuentas
  cambian mientras el panel está abierto.
- El campo **Concepto** del panel de registro ofrece **sugerencias predefinidas**
  (Transporte, Supermercado, Alimentación, Casa, Entretenimiento, Salud, Servicios, Educación, Otros)
  sin dejar de ser editable: el usuario puede elegir una sugerencia o escribir un concepto propio.
- El **FAB** ya no tapa las últimas filas: el `RecyclerView` del historial tiene
  `paddingBottom="160dp"` con `clipToPadding="false"`, de modo que el último movimiento queda visible
  al hacer scroll hasta el fondo.

**Alcance trasladado (no deuda técnica):**
- La **geolocalización y el mapa** salen de la Feature 008 por decisión de alcance (2026-10-19). Lo único
  que la 008 deja preparado es el **interruptor "Geolocalizar"** en el panel (solo UI). La captura de
  coordenadas, la migración 2→3, los permisos y la visualización en el mapa pasan a la Feature
  `009-geolocalizacion-map` (sus antiguas T19–T26).

---

## Archivos creados / modificados

**Creados:**
- `spec/features/008-historial/spec.md`, `plan.md`, `tasks.md`
- `doc/008-historial.md` (este archivo)
- `app/src/main/res/layout/bottom_sheet_transaccion.xml`
- `app/src/main/java/com/localcash/ui/cuentas/HistorialViewModel.java` (T8a-c: observa el DAO de
  transacciones y mapea entidades a dominio)
- (de la entrega de lista) `app/src/main/res/layout/view_historial.xml`, `item_transaccion.xml`,
  `ui/cuentas/HistorialAdapter.java`

**Modificados:**
- `app/src/main/java/com/localcash/ui/cuentas/CuentasActivity.java` — navegación, botón flotante,
  panel de registro, índices del contenedor de vistas y títulos; y (T8e) observación del `LiveData` del
  `HistorialViewModel` mediante una `ViewModelProvider.Factory` que inyecta el `TransaccionDao`,
  reemplazando los datos mock por los datos reales de Room; y `configurarConceptosPredefinidos(...)`,
  que carga las sugerencias del campo Concepto.
- `app/src/main/res/values/strings.xml` — cadenas del título del panel; retiro de cadenas huérfanas; y
  el array `aCuentas_conceptos_predefinidos` con las sugerencias del campo Concepto.
- `app/src/main/res/menu/bottom_nav_menu.xml` — pestaña "Nuevo" → "Movimientos".
- `app/src/main/res/layout/bottom_sheet_transaccion.xml` — el campo Concepto pasa de texto libre a
  `MaterialAutoCompleteTextView` (estilo `ExposedDropdownMenu`) con sugerencias predefinidas, editable.
- `app/src/main/res/layout/view_historial.xml` — `paddingBottom="160dp"` + `clipToPadding="false"` en
  el `RecyclerView` para que el FAB no tape la última fila al final del scroll.

**Eliminados:**
- `app/src/main/res/layout/view_transaccion.xml` (la vista propia de registro, sustituida por el panel).

**Renuméricación asociada:**
- `doc/008-actualizacion-diagrama.md` → `doc/009-actualizacion-diagrama.md`, para respetar la
  numeración compartida `spec/features/NNN` ↔ `doc/NNN` (ese registro se había creado como `008`
  cuando `007` era la feature activa).
- *(2026-10-19)* Ese registro se renombró de nuevo a **`doc/011-actualizacion-diagrama.md`** al abrir
  la Feature 009, para no colisionar con el número `009`.

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
6. **El `HistorialViewModel` expone el modelo de dominio, no la entidad de Room.**
   *Justificación:* el `HistorialAdapter` consume dominio; la conversión se concentra en un único punto
   (`mapearADominio`). El DAO se inyecta por constructor con una `ViewModelProvider.Factory` (DI simple
   y testeable, sin Hilt/Dagger).
7. **El campo Concepto usa sugerencias predefinidas editables, no un desplegable cerrado.**
   *Justificación:* las categorías frecuentes (Transporte, Supermercado, Alimentación, etc.) agilizan el
   registro sin quitar la libertad de escribir un concepto nuevo, manteniendo el comportamiento del
   texto libre anterior. Se implementa con `MaterialAutoCompleteTextView` (estilo `ExposedDropdownMenu`),
   que además resuelve la altura correcta del campo frente al `OutlinedBox` plano.

---

## Validación (Definition of Done ejecutada)

- `./gradlew lint`: BUILD SUCCESSFUL (0 errores; 41 advertencias preexistentes, ninguna introducida
  por los archivos cambiados).
- `./gradlew testDebugUnitTest`: 13 tests, 0 fallos.
- `./gradlew connectedCheck`: **BUILD SUCCESSFUL** (emulador `emulator-5554`).
- `./gradlew assembleDebug`: BUILD SUCCESSFUL (`app/build/outputs/apk/debug/app-debug.apk`).

> La Definition of Done de la Feature 008 queda **completa**: los cuatro comandos pasan en el orden de
> `.agent.md` §3. Los criterios CA-1…CA-6, CA-8 y CA-9 están verificados; CA-7 (geolocalización) pasó a
> la Feature 009.

- Re-verificado tras T8e y T9 (2026-10-19): `./gradlew assembleDebug` y `./gradlew lint`
  → BUILD SUCCESSFUL (0 errores, 41 advertencias preexistentes, 0 nuevas).
- Verificado en emulador (2026-10-19): `connectedCheck` en verde; el fix del FAB y las sugerencias de
  Concepto funcionan (última transacción visible al final del scroll; nueve sugerencias seleccionables).

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

## Traspaso a la Feature 009

La Feature 008 queda **cerrada**. Todo lo relativo a la geolocalización se continúa en
`spec/features/009-geolocalizacion-map/`:

1. **Fundaciones:** dependencia de osmdroid 6.1.x, permisos de ubicación, columnas de coordenadas y
   migración 2→3 no destructiva.
2. **Captura:** el interruptor "Geolocalizar" abre una Activity de mapa para fijar un pin; al confirmar,
   las coordenadas vuelven al panel y se persisten (ambas o ninguna).
3. **Lectura:** la pestaña "Mapa" muestra un pin por movimiento geolocalizado.
4. **Contrato de color:** pines de movimientos y de lista de compras con colores distintos (el segundo,
   para la Feature 010).

**Tecnología:** osmdroid 6.1.x ya está registrada en `tech-stack.md` §2; se re-confirmó el 2026-10-19
frente a MapLibre Native (registro en §7 de ese documento).

**Nota de numeración:** resuelta el 2026-10-19; `doc/009-actualizacion-diagrama.md` se renombró a
`doc/011-actualizacion-diagrama.md`, dejando `009` para la geolocalización y `010` para la lista de compras.

**Resueltas en la 008:** T1–T18 (navegación, historial cableado a Room, estado vacío real, refresco
dinámico, saldo dinámico, panel de registro, conceptos predefinidos y selector de cuentas observable).
