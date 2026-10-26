# doc/012-division-gastos.md — Registro de memoria: División de gastos compartidos

## Ficha de la tarea

| Campo | Valor |
|---|---|
| **Fecha** | 2026-10-25 → 2026-10-26 |
| **Feature** | `spec/features/012-division-gastos/` |
| **Alcance** | Egresos compartidos entre contactos: reparto igual (usuario implícito N+1) o personalizado, estado `pendiente`/`pagado` por parte, cobro atómico con ingreso automático al saldo, resumen de deudas en la lista de contactos, detalle con botón "Pagar" y "Pagar todo". Migración Room 4 → 5 (tabla `GASTOS_COMPARTIDOS`). |
| **Estado** | Completada. Cerrada con T9/T10 (127 tests en verde), T15 (lint baseline sin avisos nuevos) y T16 (lint + unit + assemble; `connectedCheck` no se ejecutó: la UI se validó manualmente en emulador). |
| **Responsable** | Agente SDD |

## Qué se entregó

**Constitución y spec**
- `spec/constitution/data-model.md`: revisión **v3** (11 tablas) — ERD con `GASTOS_COMPARTIDOS` y sus
  relaciones, diccionario §3.11, invariantes **I-12..I-16**, operación atómica de cobro en §4.2,
  ADR 9 (tabla única frente al trío de compras grupales), índices §3.12 y DDL del Apéndice A
  (`user_version = 3` canónico). DDL validado en `sqlite3` (rechazos, cascadas, I-1).
- `spec/features/012-division-gastos/{spec,plan,tasks}.md`: CA-1..CA-12 verificados, contrato de
  `GastoCompartidoService`, matriz de propiedad de INV-1..INV-5, 17 tareas cerradas.

**Datos (capa `data/`)**
- `entities/GastoCompartido.java`: 7 campos, FK `CASCADE` a `transacciones` / `RESTRICT` a `CONTACTOS`,
  índices **solo dos** (`ux_gastos_compartidos_transaccion_contacto`, `idx_gastos_compartidos_contacto_estado`)
  con nombre explícito igual al DDL canónico.
- `dao/GastoCompartidoDao.java`: `insertTodas`, `getByTransaccion`, `getByIdSync`,
  `getPendientesPorContacto` (monto desc), `resumenDeudasPorContacto()` global y por contacto
  (+ variantes `LiveData`), `getDeudasPendientesLive` (JOIN con el egreso: concepto y fecha),
  `marcarPagada` y `deleteByTransaccion`. **No expone `@Update` ni `@Delete`** (INV-3).
- `model/ResumenDeuda.java`, `model/DeudaContacto.java` (proyecciones de solo lectura).
- `db/AppDatabase.java`: versión 5 + `MIGRATION_4_5` no destructiva + `gastoCompartidoDao()`.
- `repository/GastoCompartidoRepository.java`: `marcarPagada` y `marcarTodasPagadas` en una sola
  `runInTransaction` (validar pendiente → estado+fecha → INSERT ingreso → `sumarSaldo` atómico).
- `dao/CuentaDao.java`: **`sumarSaldo`** (`SET saldo = saldo + :delta`), aritmética atómica.
- `dao/TransaccionDao.java`: `getByIdSync` para leer la cuenta del egreso original.

**Dominio (capa `domain/`, Java puro)**
- `model/PartePropuesta.java` (con `delUsuario`), `model/EstadoReparto.java`, `model/ModoDivision.java`.
- `service/GastoCompartidoService.java`: `validarReparto`, `dividirIgual`,
  **`dividirConUsuario`** (N+1, residuo de a 1 CLP), `validarPersonalizado`.
- `error/ErrorCode.java`: +9 códigos (`GASTO_SIN_CONTACTOS`, `REPARTO_NO_CUADRA`,
  `REPARTO_IGUAL_INSUFICIENTE`, `MONTO_PARTE_INVALIDO`, `CONTACTO_DUPLICADO`, `PARTE_INEXISTENTE`,
  `PARTE_YA_PAGADA`, `GASTO_COMPARTIDO_INVALIDO`; `CONTACTO_EN_USO` ya existía).

**UI**
- `ui/cuentas/TransaccionViewModel.java`: sobrecargas `registrarTransaccion(..., List<PartePropuesta>)`
  (validación pre-transacción, partes dentro de la misma transacción del egreso) y estado de reparto
  observable (`getDivisionVisible`, `getEstadoReparto`, `validarReparto`, `partesParaRegistrar`).
- `ui/cuentas/CuentasActivity.java`: bloque "Compartido" en el BottomSheet (checkbox inhabilitado para
  ingresos), selector tipo Gmail/WhatsApp (`AutoCompleteTextView` + `ChipGroup` con "X"), radios de
  modo, suma en vivo verde/roja con "Tu parte: $X", botón deshabilitado hasta que el dominio aprueba,
  cierre (X) y "Cancelar"; formato de moneda unificado (`instalarWatcherMoneda`); lista de contactos con
  "Debe: $X" / "Al día"; detalle de deudas con "Pagar" por fila y "Pagar todo ($X)" en el footer.
- `data/repository` invocado desde `ui/contactos/ContactosViewModel.java` (`pagarParte`, `pagarTodas`,
  `getResultadoPago`).
- Layouts: `bottom_sheet_transaccion.xml` (modificado), `item_parte_gasto.xml`,
  `item_deuda_contacto.xml`, `ic_close.xml`, `ic_check.xml`; strings `aGastoCompartido_*`.

**Tests**
- `GastoCompartidoServiceTest` (25, JVM puro): reparto igual exacto y con residuo, N+1, montos
  insuficientes, 0 contactos, duplicados, personalizada (acepta suma menor, rechaza suma mayor).
- `TransaccionViewModelDivisionTest` (2, Robolectric): suma en vivo y estados del formulario.
- `MigracionV4V5Test` (5): `MigrationTestHelper` crea v4, migra, valida esquema, `user_version = 5`,
  único `(transaccion_id, contacto_id)`, `RESTRICT`, `CASCADE`, `foreign_key_check` limpio.
- `GastoCompartidoDaoTest` (10, T9): `insertTodas`, unicidad, pendientes ordenadas por monto, resumen
  Σ y orden, `marcarPagada` 1 → 0 filas conservando `fecha_pago`, cascada, RESTRICT, PRAGMA.
- `GastoCompartidoIntegrationTest` (2, T10): flujo cuenta → egreso → reparto N+1 → cobro →
  `saldo = inicial − egreso + parte` (I-1), ingreso "Devolución gasto compartido", resumen y detalle.
- Total: **127 tests en verde**.

## Decisiones relevantes y justificación

1. **Parte del usuario implícita (N+1), sin fila "Yo" y sin persistirla.** El reparto igual divide entre
   usuario + N contactos, pero solo las partes de los contactos se guardan: `CONTACTOS` es agenda de
   terceros y crear un contacto "Yo" inventaría un registro que no corresponde a nadie
   (`data-model.md` §4.1, decisión ratificada por el dueño el 2026-10-26). Consecuencia documentada en
   I-12/I-13 y `spec.md` INV-2: `Σ partes guardadas = total − parte del usuario`. Evitó una migración
   (`contacto_id` nullable) y replica lo que hacen Splitwise/Tricount. En modo personalizado el resto
   (`total − suma`) también es del usuario: se acepta suma menor y solo se rechaza la suma mayor.
2. **Cobrar una parte inserta un ingreso automático** (`"Devolución gasto compartido"`, cuenta del egreso
   original) antes de subir el saldo, en la misma `runInTransaction`. Mover el saldo sin asiento habría
   roto I-1 (`saldo = saldo_inicial + Σ ingresos − Σ egresos`) y su test de recálculo; además el cobro
   queda visible en el historial (decisión del dueño, opción A).
3. **Saldo con aritmética atómica** (`CuentaDao.sumarSaldo`, `SET saldo = saldo + :delta`) y no
   leer-calcular-escribir, que tiene condición de carrera. El flujo antiguo de 007/008 sigue usando
   `updateSaldo` absoluto: deuda registrada, fuera del scope de esta feature.
4. **Máquina de estados en el repositorio, no en el motor.** Room no expresa `CHECK`, así que el DAO no
   expone `update`/`delete` ni escritura libre de `estado`: la única vía es `marcarPagada` con
   `WHERE id = :parte AND estado = 'pendiente'`. Un segundo cobro afecta 0 filas y no sobrescribe la
   `fecha_pago` original (INV-3/INV-4 probados en T9).
5. **Un índice redundante eliminado.** `idx_gastos_compartidos_transaccion_id` se cayó del DDL: el único
   `(transaccion_id, contacto_id)` ya sirve las consultas por egreso por su columna izquierda, y la
   constitución prohíbe índices "por si acaso".
6. **Timestamps `TEXT` ISO-8601** en `GASTOS_COMPARTIDOS` (decisión del dueño) para no abrir una
   excepción a la convención de fechas de la constitución. Ojo: `transacciones.fecha` es epoch millis
   (esquema histórico de 007/008), por eso `DeudaContacto.fechaMillis` no es ISO.
7. **Formato de moneda unificado con un único `TextWatcher`** (`instalarWatcherMoneda`, `$5.000`, sin
   sufijo " CLP"): el campo se formatea visualmente y el ViewModel recibe siempre el valor limpio.
8. **Selector tipo Gmail/WhatsApp.** Los chips de todos los contactos no escalan (UX Issue #1); ahora el
   `ChipGroup` arranca vacío, se busca en el `AutoCompleteTextView` y cada chip tiene su "X".
9. **"Pagar todo" en una sola transacción** (`marcarTodasPagadas`): si una parte falla, no se cobra
   ninguna. Evita saldos parcialmente acreditados.
10. **Schemas de Room como assets solo del build debug** (`sourceSets.debug.assets`): `MigrationTestHelper`
    los lee vía `AssetManager` y en tests JVM Robolectric solo ve los assets fusionados de la app; la
    alternativa (`main/assets`) los habría empaquetado también en release.

## Verificación

| Comando | Resultado |
|---|---|
| `./gradlew lint` | ✅ BUILD SUCCESSFUL, 120 advertencias = baseline preexistente (0 nuevas) |
| `./gradlew testDebugUnitTest` | ✅ **127 tests, 0 fallos** |
| `./gradlew connectedCheck` | ⚠️ No ejecutado por el agente: sin adb/emulador en su entorno. La UI se validó manualmente en emulador del proyecto (formulario, chips, suma en vivo, pagar, pagar todo) |
| `./gradlew assembleDebug` | ✅ |
| DDL canónico en `sqlite3` | ✅ esquema v3 creado, `foreign_key_check` limpio, rechazos de `CHECK`/`UNIQUE`/`RESTRICT` y cascadas comprobados |
| Emulador (debido al dueño) | ✅ bloque "Compartido" aparece/desaparece; cobro refleja en saldo e historial |

## Lecciones aprendidas

1. **Un `TextWatcher` de formato puede romper la validación si el validador lee el texto mostrado.**
   El campo pasó a verse "$20.000" y `Long.parseLong` devolvía 0 → error falso de "monto vacío". Fix:
   limpiar los no dígitos antes de validar y conservar el valor limpio aparte. Regla practice: **la UI
   formatea, el dominio consume números**.
2. **Chips con todos los contactos visibles es mala idea con agendas grandes.** Mejor buscador +
   selección progresiva (el patrón de Gmail/WhatsApp).
3. **Las transacciones atómicas protegen la integridad en operaciones por lotes**: en "Pagar todo",
   iterar llamadas sueltas dejaba medio cobro si una parte fallaba; un solo `runInTransaction` elimina
   ese estado intermedio.
4. **Validar el DDL ejecutándolo, no leyéndolo**: el `diff` del Mermaid y `sqlite3` con
   `foreign_key_check` detectaron el índice redundante y confirmaron que `PRAGMA foreign_keys` es
   **por conexión** (sin él, los rechazos de FK pasan en silencio).
5. **La numeración del checklist y la del diálogo con el dueño divergieron** (pidió "T7" y "T9" para
   tareas que eran T8 y T11/T12). Marcar la tarea real, no la etiqueta mencionada, evita un registro
   falso. También: **contar los tests, no estimarlos** — el "90 tests" anotado a ojo era 86.
6. **El lint del proyecto pide Timber sin tener Timber registrado**: cada `android.util.Log` nuevo suma
   una advertencia. Se evitaron logs donde el `ErrorCode` ya viaja a la UI, en lugar de añadir una
   dependencia no registrada.
7. **Sincronizar la constitución en el mismo cambio** (T2 antes que T3/T4) evitó el esquema paralelo,
   pero obliga a corregirla cuando una decisión del dueño cambia la semántica (I-12/I-13 con N+1).

## Próximos pasos

- `./gradlew connectedCheck` y `installDebug` en emulador propio (el pipeline de CI de la feature queda
  con esa casilla sin ejecutar por el agente).
- Cobertura de UI de la hoja de movimiento (chips, buscador, suma en vivo, pagar/pagar todo).
- Feature futura **COMPRAS_GRUPALES + ITEMS_COMPRA + DEUDAS_PARTICIPANTES** (ya definidas en
  `data-model.md` §3.6-3.8) para habilitar el **"les debo"/"A favor"** (deuda con dirección) y la
  liquidación parcial (`monto_pagado`), que esta feature dejó explícitamente fuera de alcance.
- Deuda registrada: el asiento del egreso sigue usando `updateSaldo` absoluto (leer-calcular-escribir,
  herencia de 007/008); unificarlo con `sumarSaldo` requiere tocar la Feature 007 y merece su propia tarea.
- Contador visible de chips seleccionados (resto de UX Issue #1).
- Considerar una tarea propia para "Corregir un cobro" (hoy obliga a borrar y reinsertar el egreso):
  falta una vía explícita en la UI.
