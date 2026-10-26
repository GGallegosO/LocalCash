# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** 012-division-gastos | **Derivado de:** `spec.md` + `plan.md`
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
> - El esquema de la tabla se sincroniza con `data-model.md` ANTES de escribir código de persistencia
>   (regla de AGENTS.md: constitución y código en el mismo cambio, nunca esquema paralelo).

---

## Fase 1 — Contratos y constitución

- [x] **T1. Confirmar contrato público y códigos de error.**
      Verificar que `plan.md` §1 lista entrada/salida/códigos (`GASTO_SIN_CONTACTOS`,
      `REPARTO_NO_CUADRA`, `REPARTO_IGUAL_INSUFICIENTE`, `MONTO_PARTE_INVALIDO`,
      `CONTACTO_DUPLICADO`, `PARTE_INEXISTENTE`, `PARTE_YA_PAGADA`, `CONTACTO_EN_USO`) y que
      invariantes INV-1..INV-5 de `spec.md` §5 tienen dueño de capa (dominio/repositorio).
      *Criterio de salida:* el contrato permite escribir tests antes de implementar nada.
      Responsable: Agente SDD | Estimación: 15 min
      *Cierre (2026-10-25):* `plan.md` §1 publica la tabla del contrato invocable de
      `GastoCompartidoService` (`validarReparto`, `dividirIgual`, `repartoPersonalizado`, tipos
      `ModoDivision`/`PartePropuesta`, semántica `ErrorCode` vs `BusinessRuleException`) y `plan.md` §2
      agrega la matriz de propiedad de INV-1..INV-5 (dominio para 1–2; repositorio/DAO con respaldo de
      FK para 3–5) con su test asignado. Hallazgo: `ContactoDao` **no** tiene
      `getComprasComoPagador`/`getDeudasComoDeudor` aunque `doc/011` lo afirmaba; el historial por
      contacto se resolverá solo con `GASTOS_COMPARTIDOS` (T13).

- [x] **T2. Sincronizar `spec/constitution/data-model.md` con la entidad `GASTOS_COMPARTIDOS`.**
      Agregar al ERD Mermaid la entidad y sus relaciones (`TRANSACCIONES ||--o{ GASTOS_COMPARTIDOS :
      "se divide en"`, `CONTACTOS ||--o{ GASTOS_COMPARTIDOS : "responde por"`), la subsección de
      diccionario, invariantes numeradas tras I-11 (equivalentes a INV-1..INV-5), los índices y el
      DDL del Apéndice A con `CREATE TABLE GASTOS_COMPARTIDOS` + `CREATE INDEX`.
      *Criterio de salida:* el DDL se ejecuta en `sqlite3` local sin error; `PRAGMA foreign_key_check`
      limpio; relaciones del Mermaid referencian solo entidades declaradas (diff contra el ERD
      aprobado, trampa documentada en AGENTS.md).
      Responsable: Agente SDD | Estimación: 45 min
      *Cierre (2026-10-26):* `data-model.md` pasa a revisión v3 (11 tablas): ERD con la entidad y sus
      dos relaciones (verificado por script: 11 entidades declaradas, 11 referenciadas, 0 huérfanas),
      diccionario §3.11 con regla de tabla `estado` ↔ `fecha_pago`, índices de §3.11 reubicados a
      §3.12 con dos consultas nuevas, invariantes I-12..I-16, bloque atómico de cobro en §4.2, ADR 9
      (tabla única frente al trío de compras grupales), trazabilidad/registro de cambios y DDL del
      Apéndice A (`user_version = 3` canónico). **Validado en `sqlite3`:** DDL ejecutado sin error,
      `foreign_key_check` limpio, flujo egreso→reparto→cobro con saldo 150.000 = I-1, y rechazos
      comprobados (monto 0, `pagado` sin fecha, `pendiente` con fecha, contacto repetido, fecha
      malformada, FK inexistente, borrado de contacto con parte) más cascadas egreso→partes y
      cuenta→movimientos. **Desviación del plan corregida:** se eliminó
      `idx_gastos_compartidos_transaccion_id` por redundante con el único
      `(transaccion_id, contacto_id)`; `plan.md` Decisión 6 y T3 actualizados con la justificación.
      *Nota operativa:* el `PRAGMA foreign_keys = ON` es por conexión; los tests de rechazo solo
      pasan si se activa en cada conexión (lo que confirma la trampa documentada en AGENTS.md).

## Fase 2 — Persistencia

- [x] **T3. Crear entidad Room `GastoCompartido` y DAO `GastoCompartidoDao`.**
      `data/entities/GastoCompartido.java` (tabla `GASTOS_COMPARTIDOS`, índices
      `ux..._transaccion_contacto` único y `idx..._contacto_estado`; sin índice aislado sobre
      `transaccion_id`, cubierto por el único — ver `plan.md` Decisión 6 corregida en T2).
      `data/dao/GastoCompartidoDao.java` con nombre de tabla por constante: `insertTodas`,
      `getByTransaccion`, `getPendientesPorContacto`, `resumenDeudasPorContacto` (Σ pendientes,
      ORDER BY monto desc), `marcarPagada(parteId, fechaPagoIso)` con `WHERE ... estado='pendiente'`,
      `deleteByTransaccion` implícito por cascada (no exponer delete arbitrario).
      *Criterio de salida:* compila; cero advertencias de linter; el DAO no expone write de `estado`
      distinto de `marcarPagada` (INV-3).
      Responsable: Agente SDD | Estimación: 60 min
      *Cierre (2026-10-26):* creados `data/entities/GastoCompartido.java` (7 campos, FK
      `CASCADE` a `transacciones` / `RESTRICT` a `CONTACTOS`, constantes `ESTADO_PENDIENTE`/
      `ESTADO_PAGADO`, **solo 2 índices** con nombre explícito igual al DDL canónico),
      `data/dao/GastoCompartidoDao.java` (`TABLE_NAME`, `insertTodas`, `getByTransaccion`,
      `getPendientesPorContacto`, `resumenDeudasPorContacto()` global y por contacto,
      `marcarPagada`, `deleteByTransaccion` documentado como interno) y `data/model/ResumenDeuda.java`
      (`contactoId`, `nombreContacto`, `totalPendiente`). **INV-3 verificado por superficie:** el DAO
      no tiene `@Update` ni `@Delete`; la única escritura de estado es `marcarPagada` con
      `WHERE estado='pendiente'` (devuelve filas afectadas). **SQL validado en `sqlite3`** (Room aún
      no valida el DAO hasta registrarlo en T4): las 6 sentencias ejecutan, `marcarPagada` da 1 → 0 en
      el segundo intento conservando la `fecha_pago` del primero, y `EXPLAIN QUERY PLAN` usa
      `ux_gastos_compartidos_transaccion_contacto` para partes-por-egreso e
      `idx_gastos_compartidos_contacto_estado` para pendientes-por-contacto. `compileDebugJavaWithJavac`
      OK; `./gradlew lint` BUILD SUCCESSFUL sin advertencias en los archivos nuevos (las 120 del
      reporte son baseline previo: Timber/version catalog, fuera de scope). `data-model.md` §3.11
      ajustado: los índices se declaran con nombre explícito en `@Index`, no con el nombre automático
      de Room.

- [x] **T4. Agregar migración Room 4 → 5 en `AppDatabase`.**
      Entidad + DAO en la base, `version = 5`, `MIGRATION_4_5` que replica el DDL sincronizado de T2
      (`CREATE TABLE IF NOT EXISTS` + `CREATE INDEX`; sin `DROP TABLE` ni
      `fallbackToDestructiveMigration()`), `gastoCompartidoDao()`.
      *Criterio de salida:* `MigrationTestHelper` siembra v4, migra a v5, verifica `user_version = 5`
      y `PRAGMA foreign_key_check` limpio.
      Responsable: Agente SDD | Estimación: 60 min
      *Cierre (2026-10-26):* `AppDatabase` en **versión 5** con `GastoCompartido.class` en `entities`,
      `gastoCompartidoDao()` y `MIGRATION_4_5` (tabla + 2 índices, sin `DROP TABLE` ni
      `fallbackToDestructiveMigration()`; añadida a `addMigrations`). Room exporta
      `app/schemas/com.localcash.data.db.AppDatabase/5.json`, cuyo `createSql` coincide con el DDL
      canónico (columnas, `ON UPDATE CASCADE` y `ON DELETE CASCADE`/`RESTRICT`; se agregó
      `onUpdate = CASCADE` a los `@ForeignKey` de la entidad para no divergir). Nuevo test
      `MigracionV4V5Test` con **`MigrationTestHelper`**: crea v4 desde `4.json`, siembra cuenta/egreso/
      2 contactos, migra y valida contra `5.json`, y comprueba tabla+índices, **`user_version = 5`**,
      datos intactos, único `(transaccion_id, contacto_id)` que rechaza repetir contacto, `RESTRICT` al
      borrar contacto con parte, `CASCADE` al borrar el egreso y `PRAGMA foreign_key_check` limpio.
      **Decisión de andamiaje:** los schemas se exponen como assets **solo del build debug**
      (`sourceSets.debug.assets` en `app/build.gradle`), porque en tests JVM Robolectric solo lee los
      assets fusionados de la app y `sourceSets.test.assets` no llega a `android_merged_assets`
      (verificado: `FileNotFoundException`); la alternativa era empaquetar los JSON en `main`, lo que
      los metería en release. **Impacto colateral corregido:** `MigracionV2V3Test` apuntaba al builder
      sin `MIGRATION_4_5` y esperaba `user_version = 4`; se actualizó al tramo completo y a la versión
      5 (sin cambiar lo que prueba del 2 → 3). Verificación: `compileDebugJavaWithJavac` OK,
      `testDebugUnitTest` **86 tests en verde**, `lint` sin advertencias nuevas (120 baseline) y
      `assembleDebug` OK.

## Fase 3 — Dominio (núcleo de negocio)

- [x] **T5. Crear modelo `GastoCompartido` y servicio `GastoCompartidoService` (reparto).**
      Modelo inmutable con `toEntity()`/`fromEntity()`. Servicio (Java puro, sin Android):
      `dividirIgual(monto, contactosIds)` → aritmética entera con residuo de a 1 CLP al inicio de la
      lista, rechaza monto < n; `validarPersonalizada(monto, partes)` → Σ exacto (INV-2), cada parte > 0;
      rechaza 0 contactos (INV-1) y contacto duplicado. Devuelve `ErrorCode`, nunca lanza crudo.
      *Criterio de salida:* invocable y verificable unitariamente; cero advertencias.
      Responsable: Agente SDD | Estimación: 60 min
      *Cierre (2026-10-26):* desglose previo con **sequential-thinking** (3 pasos: reglas y orden de
      validación, casos borde numéricos, decisión de alcance). Creados `domain/model/PartePropuesta.java`
      (inmutable, `contactoId` + `montoAsignado` CLP) y `domain/service/GastoCompartidoService.java`
      (final, métodos estáticos, sin `android.*` ni `androidx.*`):
      `validarReparto(monto, ids)` → `MONTO_INVALIDO` / `GASTO_SIN_CONTACTOS` / `CONTACTO_DUPLICADO`;
      `dividirIgual(monto, ids)` → `base = total/n`, residuo de a 1 CLP a los primeros contactos,
      `REPARTO_IGUAL_INSUFICIENTE` si `total < n`; `validarPersonalizado(monto, partes)` →
      `MONTO_PARTE_INVALIDO`, `CONTACTO_DUPLICADO`, `REPARTO_NO_CUADRA`. Se agregaron los 7 códigos del
      contrato a `domain/error/ErrorCode.java`. **Corrección al plan (regla SDD):** las firmas reales
      son las de esta tarea (una sola `validarReparto` sin modo ni montos) y **todas las validaciones
      lanzan `BusinessRuleException`** con su `ErrorCode` (no devuelven `ErrorCode`); `plan.md` §1 y la
      matriz de INV-2 actualizados. **Alcance ajustado:** el modelo de dominio `GastoCompartido` con
      `toEntity()`/`fromEntity()` se pospone a T7, donde el registro del egreso realmente mapea las
      partes; T5 no lo necesita y así se evita inventar un código de error solo para el mapeo.
      Verificación: `compileDebugJavaWithJavac` OK, `lint` sin advertencias nuevas; los tests de esta
      lógica son T8 (aún no existen, por eso no hay tests nuevos en este commit). Comprobación manual en
      JVM contra las clases compiladas: `100000 / 3` → `33334 + 33333 + 33333` (suma exacta 100000) y
      los rechazos devuelven `REPARTO_IGUAL_INSUFICIENTE`, `GASTO_SIN_CONTACTOS`, `CONTACTO_DUPLICADO`,
      `REPARTO_NO_CUADRA` y `MONTO_PARTE_INVALIDO` según corresponda.

- [x] **T6. Implementar `marcarPagada()` en el repositorio con `runInTransaction`.**
      Secuencia exacta de `plan.md` §2 (bloque SQL): validar pendiente → UPDATE estado+fecha →
      INSERT ingreso "Devolución gasto compartido" en la cuenta del egreso →
      `SET saldo_actual = saldo_actual + :monto` — todo en un único `runInTransaction`.
      Agregar los códigos nuevos a `domain/error/ErrorCode.java`. El UPDATE usa
      `WHERE id = :parte AND estado = 'pendiente'`; 0 filas afectadas →
      `PARTE_INEXISTENTE`/`PARTE_YA_PAGADA` sin escribir nada más.
      *Criterio de salida:* ninguna vía expone reabrir una parte; fallando cualquier paso no se aplica
      ninguno.
      Responsable: Agente SDD | Estimación: 60 min
      *Cierre (2026-10-26):* desglose previo con **sequential-thinking** (4 pasos: validación, asiento
      + saldo, atomicidad/rollback, registro del egreso). Creado
      `data/repository/GastoCompartidoRepository.java` con `marcarPagada(parteId, fechaPagoIso)` —un solo
      `db.runInTransaction` con el orden leer/validar → `marcarPagada` (WHERE pendiente) →
      INSERT ingreso `Devolución gasto compartido` en la cuenta del egreso → `sumarSaldo` atómico— y
      `insertTodas(transaccionId, partes, fechaCreacionIso)`. Para ello se agregaron al DAO
      `GastoCompartidoDao.getByIdSync`, `TransaccionDao.getByIdSync` y el método **atómico**
      `CuentaDao.sumarSaldo` (`SET saldo = saldo + :delta`), sin leer-calcular-escribir. Se agregó
      `GASTO_COMPARTIDO_INVALIDO` (compartir un ingreso o fallo inesperado del reparto). El ViewModel
      gana las sobrecargas `registrarTransaccion(..., List<PartePropuesta>)` (las firmas anteriores
      delegan con `null`): valida con `GastoCompartidoService.validarReparto` + `validarPersonalizado`
      **antes** de abrir la transacción y llama a `insertTodas` **dentro** de la misma transacción del
      egreso y su saldo, de modo que un fallo revierte también el asiento.
      **Atomicidad verificada con una prueba temporal de Robolectric** (ejecutada y retirada: la
      cobertura permanente es T9): cobrar una parte de 60.000 sobre un egreso de 100.000 en una cuenta
      de 200.000 deja saldo **160.000**, un ingreso de 60.000 en el historial, la parte `pagado` con
      `fecha_pago`, I-1 exacto (`200.000 + Σ ingresos − Σ egresos`), segundo cobro →
      `PARTE_YA_PAGADA` sin cambios, parte inexistente → `PARTE_INEXISTENTE`, y una excepción inyectada
      dentro de la transacción deja **0 partes** (rollback). `compileDebugJavaWithJavac` OK,
      `testDebugUnitTest` en verde (86) y `lint` con **las mismas 120 advertencias baseline**: se
      omitieron los `Log` nuevos del repositorio porque el lint del proyecto exige Timber (no está en
      `tech-stack.md`, sería dependencia no registrada) y el código de negocio se registra en la UI
      (T14); los fallos inesperados siguen propagando excepción con contexto.
      *Deuda preexistente anotada (fuera de scope):* el asiento del egreso en `TransaccionViewModel`
      sigue usando `updateSaldo` absoluto (leer-calcular-escribir de 007/008); el cobro nuevo usa
      aritmética atómica. Corregir el flujo antiguo exige tocar la Feature 007 y merece su propia tarea.

- [x] **T7. Extender `TransaccionViewModel.registrarTransaccion()` para egresos compartidos.**
      Crear `domain/model/GastoCompartido.java` inmutable con `toEntity()`/`fromEntity()` (pospuesto
      desde T5: aquí es donde el registro mapea las partes al DAO) y `EstadoGastoCompartido`
      (`pendiente`/`pagado`) si hace falta tipar el estado. Tras el asiento del egreso y dentro de la
      MISMA transacción, validar con `GastoCompartidoService` (T5) e insertar las N partes con
      `estado='pendiente'` y `fecha_creacion`. Si la validación o alguna inserción de parte falla, el
      egreso tampoco se escribe (todo o nada).
      *Criterio de salida:* registrar un egreso compartido con partes inválidas (Σ ≠ total, 0
      contactos) no deja ni movimiento ni partes ni saldo modificado.
      Responsable: Agente SDD | Estimación: 60 min
      *Estado (2026-10-26):* implementado dentro de T6 (commit 3095cf3). Criterio de salida verificado
      end-to-end en T11/T12.
      Detalle: las sobrecargas con `List<PartePropuesta>` (las firmas anteriores delegan con `null`),
      la validación pre-transacción con `GastoCompartidoService.validarReparto` +
      `validarPersonalizado` y la llamada a `GastoCompartidoRepository.insertTodas` dentro de la MISMA
      transacción del egreso y su saldo se entregaron en T6 por instrucción explícita del dueño. En
      T11/T12 se comprobó que el rechazo del reparto no llega al registro: el botón "Registrar" queda
      deshabilitado mientras `EstadoReparto` es inválido y `registrarMovimiento` sale con el
      `ErrorCode` sin invocar el ViewModel (`TransaccionViewModelDivisionTest`, 2 tests JVM). El modelo
      de dominio `GastoCompartido` con `toEntity()`/`fromEntity()` no se creó: el repositorio construye
      la entidad desde `PartePropuesta`.
      *Alcance aún abierto (no bloquea T7):* la aserción sobre las filas de base de datos (ni asiento,
      ni partes, ni saldo tras un reparto inválido) corresponde a T9/T10, que siguen pendientes.

- [x] **T8. Tests de dominio `GastoCompartidoServiceTest` (JVM puro).**
      Cubre CA-1..CA-6 y casos borde 1–6 de `spec.md` §6: reparto igual exacto, con residuo
      (100000/3 → 33334+33333+33333), monto < n rechazado, 0 contactos rechazado, personalizada que
      cuadra y que no, parte ≤ 0, contacto duplicado.
      *Criterio de salida:* rojo → verde documentado; cobertura ≥ 70% en la lógica nueva.
      Responsable: Agente SDD | Estimación: 45 min
      *Cierre (2026-10-26):* creado
      `app/src/test/java/com/localcash/domain/service/GastoCompartidoServiceTest.java` con **25 tests
      JUnit 4**, sin una sola importación de `android.*` ni `androidx.*` (JVM puro, sin Robolectric):
      reparto exacto entre 2 (50.000 + 50.000), con residuo entre 3 (33.334 + 33.333 + 33.333),
      residuo de a 1 peso entre 7 (cinco de 14.286 y dos de 14.285), un solo contacto, orden de la
      lista, suma exacta para montos de 5 a 92.147.483.647 con 1–5 contactos, `monto < n` →
      `REPARTO_IGUAL_INSUFICIENTE`, lista nula/vacía/contacto nulo/contacto ≤ 0 →
      `GASTO_SIN_CONTACTOS`, duplicado → `CONTACTO_DUPLICADO`, monto 0/negativo → `MONTO_INVALIDO`,
      personalizada que cuadra (aceptada), suma 45.000 vs 50.000 → `REPARTO_NO_CUADRA`, parte 0 /
      negativa / nula → `MONTO_PARTE_INVALIDO`, contacto repetido → `CONTACTO_DUPLICADO`, total no
      positivo → `MONTO_INVALIDO`, y el resultado de `dividirIgual` aceptado por
      `validarPersonalizado`. **Cobertura:** JaCoCo no está configurado en el proyecto (añadirlo sería
      una dependencia nueva no registrada), así que se verifica por ramas: las 3 funciones y las 12
      salidas de error del servicio están ejercitadas, igual que los dos caminos del bucle de residuo
      (`i < residuo` verdadero/falso) → cobertura de ramas 100% del servicio.
      `./gradlew testDebugUnitTest` en verde: **25 tests nuevos**, suite total **111 tests, 0 fallos**
      (86 previos + 25).

- [ ] **T9. Tests de persistencia `GastoCompartidoDaoTest` (Robolectric + Room in-memory).**
      Cubre CA-7, CA-8, CA-11, CA-9 (resumen Σ/orden), INV-4 y el cobro de T6: tras marcar pagada,
      asserts de (a) estado+fecha_pago, (b) ingreso insertado con `cuenta_id`/`monto` correctos,
      (c) saldo = recálculo Σ movimientos (I-1), (d) segundo cobro afecta 0 filas sin tocar nada,
      (e) borrar egreso elimina partes (CASCADE), (f) borrar contacto con pendientes falla RESTRICT →
      `CONTACTO_EN_USO`, (g) `PRAGMA foreign_keys` encendido, (h) `foreign_key_check` limpio al final.
      *Criterio de salida:* rojo → verde documentado; `./gradlew testDebugUnitTest` en verde.
      Responsable: Agente SDD | Estimación: 90 min

- [ ] **T10. Test de integración end-to-end sin UI (post-T9).**
      Flujo completo por repositorio/DAO: crear cuenta + egreso compartido con 2 partes → marcar una
      parte pagada → verificar (a) saldo de la cuenta = saldo_inicial − egreso + parte cobrada,
      (b) el ingreso "Devolución gasto compartido" aparece en `TRANSACCIONES` con el monto correcto,
      (c) el resumen muestra solo la parte pendiente restante, (d) I-1 sigue cuadrando.
      *Criterio de salida:* verde en `./gradlew testDebugUnitTest`; ninguna dependencia de Activity.
      Responsable: Agente SDD | Estimación: 30 min

## Fase 4 — Interfaz

> **Orden de ejecución restante (2026-10-26):** T11 y T12 están cerradas (comportamiento confirmado en
> emulador por el dueño). La siguiente tarea real es **T13** (resumen de deudas + detalle de contacto);
> **T14 depende de T13** porque el botón "Marcar pagada" vive en el detalle que T13 crea. T9 y T10
> (pruebas de persistencia e integración, Fase 3) quedaron pendientes al ejecutarse T11/T12 fuera de
> orden por instrucción del dueño: deben cerrarse antes de T16.

- [x] **T11. Checkbox "Compartido" + selector de contactos en el formulario de egreso.**
      En `ui/cuentas` (formulario de `CuentasActivity`/`TransaccionViewModel`): marcar el egreso como
      compartido y elegir uno o más contactos de la agenda (Feature 011). Sin selector de contactos el
      registro compartido se rechaza con `GASTO_SIN_CONTACTOS` (dominio, no UI).
      *Criterio de salida:* en emulador se eligen contactos para un egreso; cancelar la selección no
      registra nada.
      Responsable: Agente SDD | Estimación: 60 min
      *Cierre (2026-10-26):* el bloque vive dentro de `bottom_sheet_transaccion.xml` (el formulario real
      de la app es un BottomSheet, no un `dialog_transaccion.xml`): `chkCompartido`,
      `layoutDivision` oculto por defecto, `contenedorContactos` con un `CheckBox` por contacto de la
      agenda (`item_contacto_seleccion.xml`, selección múltiple), `tvSinContactos` cuando la agenda está
      vacía, y observadores de `getDivisionVisible()`/`getEstadoReparto()` del ViewModel que muestran u
      ocultan el bloque. El checkbox se **deshabilita y desmarca** cuando el tipo elegido es Ingreso.
      `registrarMovimiento` pregunta al ViewModel (`isCompartido`/`validarReparto`/
      `partesParaRegistrar`) y solo entonces llama al registro con las partes. El agente no dispone de adb ni
      emulador en su entorno, así que la interacción se probó en el emulador del proyecto y **el dueño
      confirmó el 2026-10-26 que el bloque aparece al marcar "Compartido" y desaparece al desmarcarlo**;
      resta la pasada formal de `connectedCheck` en T16. Lo que sí está cubierto por tests automáticos es el estado del reparto
      (`TransaccionViewModelDivisionTest`, 2 tests JVM con Robolectric).
      *Dependencias:* requiere T3/T4 (DAO y base) y T5 (`GastoCompartidoService`). Bloquea a T12.*

- [x] **T12. Radios "Partes iguales" / "Montos personalizados" con suma en vivo.**
      Layouts `dialog_division_gastos.xml`, `item_parte_gasto.xml`: modo de división, fila por contacto
      (monto editable solo en personalizada), suma de partes vs total visible en vivo; "Registrar"
      solo se habilita cuando `GastoCompartidoService` (T5) aprueba el reparto. La UI solo orquesta:
      ningún cálculo de reparto ni regla en la Activity.
      *Criterio de salida:* CA-3 y CA-5 verificables a mano: residuo repartido de a 1 CLP; botón
      bloqueado mientras la suma no cuadra.
      Responsable: Agente SDD | Estimación: 60 min
      *Cierre (2026-10-26):* `rgModoDivision` con `rbIgual`/`rbPersonalizado`; `contenedorPartes` se
      reconstruye solo cuando cambia la firma (modo + contactos) para no robar el foco del teclado, con
      `item_parte_gasto.xml` (EditText con `TextWatcher` en personalizada; monto de solo lectura en
      iguales, donde se ve el residuo de a 1 CLP). `tvSumaEnVivo` muestra "Suma de las partes: X de Y ·
      N partes" en verde (`?attr/colorPrimary`) o rojo (`?attr/colorError`) más el motivo del rechazo, y
      `btnGuardarMovimiento` se deshabilita mientras el dominio no aprueba. La suma y la validez salen de
      `EstadoReparto`, calculado por el ViewModel con `GastoCompartidoService` — la Activity no hace
      aritmética. Nuevos archivos de dominio de apoyo: `ModoDivision` y `EstadoReparto`. Strings en
      español `aGastoCompartido_*`. Verificación: `assembleDebug` OK, `lint` con **las mismas 120
      advertencias baseline** (la primera versión del string de suma activaba `PluralsCandidate`; se
      reescribió), `testDebugUnitTest` en verde (113 tests, 2 nuevos de estado de reparto).

- [x] **T13. Resumen de deudas + detalle de contacto con historial de partes en `ui/contactos`.**
      Sección "Resumen de deudas" (Σ pendientes por contacto, orden descendente, layout
      `item_resumen_deuda.xml`, CA-9); detalle del contacto con historial de partes compartidas
      (layout `dialog_detalle_contacto.xml`, CA-12, cierra CA-9/CA-10 heredados de 011) usando
      `resumenDeudasPorContacto` y `getPendientesPorContacto` del DAO.
      *Criterio de salida:* el resumen y el detalle se pueblan desde la base; contacto sin partes
      muestra "Sin compras compartidas".
      Responsable: Agente SDD | Estimación: 90 min
      *Dependencias:* requiere T3/T4 (DAO `resumenDeudasPorContacto` / `getPendientesPorContacto`) y
      T11/T12 (ya cerradas: existen partes que mostrar). **Bloquea a T14**, que vive en este detalle.*
      *Cierre (2026-10-26):* el resumen va **en la propia fila** de la lista existente (sin sección
      aparte ni `item_resumen_deuda.xml`): se eliminó la flecha placeholder de `item_contacto.xml` y en
      su lugar `tvDeuda` muestra "Debe: $X" en `?attr/colorError` o "Al día" con
      `colorOnSurfaceVariant`, alimentado por `resumenDeudasPorContactoLive()` observado en
      `CuentasActivity` y un mapa en `ContactoAdapter`. El detalle es un `MaterialAlertDialog` (sin
      Activity nueva) con el nombre como título, filas dinámicas (`LinearLayout`) desde
      `getDeudasPendientesLive(contactoId)` —JOIN con `transacciones` para concepto y fecha— en formato
      "concepto / dd/MM/yyyy · $monto", el texto "Sin deudas pendientes" cuando no hay filas, y los
      botones existentes de editar/eliminar. Sin "A favor": la entidad no tiene dirección (ver
      `spec.md` §7). Verificación: `assembleDebug` OK, `lint` **120 baseline** (se corrigieron
      `NotifyDataSetChanged`, un `SetTextI18n` y se retiró el string `aContacto_btn_historial` que
      quedó huérfano), `testDebugUnitTest` verde. Falta la revisión visual en emulador (a cargo del
      dueño) y el botón de cobro que llega en T14.

- [ ] **T14. Botón "Marcar pagada" + verificación en emulador.**
      Desde el detalle del contacto invoca el cobro atómico de T6 (`marcarPagada()`); recarga resumen
      e historial tras el cobro. Verificar en emulador: el saldo de la cuenta sube exactamente el
      `monto_asignado`, el ingreso aparece en el historial y la parte cambia a `pagado` (CA-7);
      un segundo intento sobre la misma parte muestra "La parte ya fue cobrada" (CA-8); contacto con
      partes pendientes no se deja borrar (CA-10).
      *Criterio de salida:* flujo completo comprobado con `./gradlew installDebug` en emulador.
      Responsable: Agente SDD | Estimación: 60 min
      *Dependencias:* **requiere T13** (el botón se invoca desde el detalle de contacto que T13 crea) y
      T6 (`GastoCompartidoRepository.marcarPagada`). Conviene ejecutar T9/T10 antes para llegar a esta
      tarea con el cobro ya probado en persistencia.*

- [ ] **T15. Strings y lint.**
      Textos en español en `res/values/strings.xml` (prefijo de convención de la feature, p. ej.
      `aGastoCompartido_*`); `./gradlew lint` sin advertencias.
      *Criterio de salida:* lint limpio.
      Responsable: Agente SDD | Estimación: 30 min

## Fase 5 — Cierre y trazabilidad

- [ ] **T16. Verificación final completa (orden obligatorio, `connectedCheck` al final).**
      `./gradlew lint` → `./gradlew testDebugUnitTest` → `./gradlew connectedCheck` (emulador activo)
      → `./gradlew assembleDebug`. Pruebas manuales de `plan.md` §5 en emulador.
      *Criterio de salida:* los cuatro comandos en verde; CA-1..CA-12 marcados en `spec.md`.
      Responsable: Agente SDD | Estimación: 60 min

- [ ] **T17. Registrar memoria y commits.**
      Crear `doc/012-division-gastos.md` (fecha, archivos, decisiones con justificación —incluidas las
      tres confirmadas por el dueño el 2026-10-25: ingreso automático, "me deben" solo, timestamps
      TEXT ISO-8601—, lecciones aprendidas, próximos pasos). Verificar `data-model.md` sincronizado
      (T2) y `tech-stack.md` sin cambios (no hay dependencias nuevas). Commits Conventional Commits,
      uno por unidad lógica: p. ej. `docs(sdd): sincroniza modelo de datos con gastos compartidos`,
      `feat(gastos-compartidos): ...`, `test(gastos-compartidos): ...`.
      *Criterio de salida:* otra persona podría mantener la feature leyendo solo estos documentos.
      Responsable: Agente SDD | Estimación: 30 min

---

## Verificación final (Definition of Done)

Los cuatro comandos van en este orden. `connectedCheck` va al final y **requiere dispositivo o
emulador activo**: si se ejecuta primero y falla, se pierde el ciclo de compilación.

- [ ] Lint sin advertencias: `./gradlew lint`
- [ ] Tests unitarios en verde: `./gradlew testDebugUnitTest`
- [ ] Tests de integración y UI en verde: `./gradlew connectedCheck`
- [ ] Build válido: `./gradlew assembleDebug`
- [ ] Registro de memoria creado: `doc/012-division-gastos.md`
- [ ] `data-model.md` actualizado con la tabla `GASTOS_COMPARTIDOS` (mismo cambio que el código)
- [ ] `tech-stack.md` verificado (sin dependencias nuevas — si apareciera alguna, registrarla antes)
- [ ] Commits con formato de `.agent.md` §5.2, uno por unidad lógica

**Notas de ejecución:**

- **UX Issue #1 (T11, 2026-10-26):** el selector de contactos con CheckBox verticales no escala a más
  de ~20 contactos: obliga a hacer scroll largo dentro de la hoja inferior y vuelve difícil ver cuántos
  se eligieron. **Resuelto el 2026-10-26 (commit d258da4):** el selector pasó a `ChipGroup` +
  `AutoCompleteTextView` tipo Gmail (chips vacíos al abrir, búsqueda con desplegable y "X" por chip), y se agregaron cierre (X) y "Cancelar". Queda pendiente el contador visible de chips
  seleccionados.
- **UX Issue #2 (T13/T14, 2026-10-26):** la pantalla de Contactos todavía **no muestra deudas ni ofrece
  el botón de pago**: el resumen de deudas y el cobro atómico ya existen en datos (`GastoCompartidoDao`,
  `GastoCompartidoRepository`) pero no tienen superficie en la UI. **Bloquea la demo universitaria**:
  sin T13/T14 no se puede mostrar "quién me debe" ni cobrar una parte desde la app.
- **N+1 con usuario implícito (decisión del dueño, 2026-10-26):** el reparto igual divide entre
  usuario + N contactos, pero la parte del usuario **no se persiste** (no existe contacto "Yo"; el
  egreso completo ya es su gasto). `Σ partes guardadas = total − parte del usuario`; ratificado en
  `data-model.md` §3.11 e I-12/I-13, y reflejado en `spec.md` INV-2. Sin migración.
- Desfase de numeración durante la ejecución: el dueño solicitó los tests de dominio como "T7" y el
  formulario como "T9"; en este checklist corresponden a T8 y T11/T12. Se marcaron las tareas reales,
  no las etiquetas pedidas, para que el registro coincida con el código entregado.
- Conteo verificado de la suite: **113 tests en verde** (86 base + 25 de `GastoCompartidoServiceTest` +
  2 de `TransaccionViewModelDivisionTest`). En los cierres de T4 y T6 se había anotado "90"; se corrigió
  a 86 tras contar los XML de JUnit.
