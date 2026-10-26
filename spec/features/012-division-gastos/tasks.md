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

- [ ] **T1. Confirmar contrato público y códigos de error.**
      Verificar que `plan.md` §1 lista entrada/salida/códigos (`GASTO_SIN_CONTACTOS`,
      `REPARTO_NO_CUADRA`, `REPARTO_IGUAL_INSUFICIENTE`, `MONTO_PARTE_INVALIDO`,
      `CONTACTO_DUPLICADO`, `PARTE_INEXISTENTE`, `PARTE_YA_PAGADA`, `CONTACTO_EN_USO`) y que
      invariantes INV-1..INV-5 de `spec.md` §5 tienen dueño de capa (dominio/repositorio).
      *Criterio de salida:* el contrato permite escribir tests antes de implementar nada.
      Responsable: Agente SDD | Estimación: 15 min

- [ ] **T2. Sincronizar `spec/constitution/data-model.md` con la entidad `GASTOS_COMPARTIDOS`.**
      Agregar al ERD Mermaid la entidad y sus relaciones (`TRANSACCIONES ||--o{ GASTOS_COMPARTIDOS :
      "se divide en"`, `CONTACTOS ||--o{ GASTOS_COMPARTIDOS : "responde por"`), la subsección de
      diccionario, invariantes numeradas tras I-11 (equivalentes a INV-1..INV-5), los índices y el
      DDL del Apéndice A con `CREATE TABLE GASTOS_COMPARTIDOS` + `CREATE INDEX`.
      *Criterio de salida:* el DDL se ejecuta en `sqlite3` local sin error; `PRAGMA foreign_key_check`
      limpio; relaciones del Mermaid referencian solo entidades declaradas (diff contra el ERD
      aprobado, trampa documentada en AGENTS.md).
      Responsable: Agente SDD | Estimación: 45 min

## Fase 2 — Persistencia

- [ ] **T3. Crear entidad Room `GastoCompartido` y DAO `GastoCompartidoDao`.**
      `data/entities/GastoCompartido.java` (tabla `GASTOS_COMPARTIDOS`, índices
      `ux..._transaccion_contacto` único, `idx..._contacto_estado`, `idx..._transaccion_id`).
      `data/dao/GastoCompartidoDao.java` con nombre de tabla por constante: `insertTodas`,
      `getByTransaccion`, `getPendientesPorContacto`, `resumenDeudasPorContacto` (Σ pendientes,
      ORDER BY monto desc), `marcarPagada(parteId, fechaPagoIso)` con `WHERE ... estado='pendiente'`,
      `deleteByTransaccion` implícito por cascada (no exponer delete arbitrario).
      *Criterio de salida:* compila; cero advertencias de linter; el DAO no expone write de `estado`
      distinto de `marcarPagada` (INV-3).
      Responsable: Agente SDD | Estimación: 60 min

- [ ] **T4. Agregar migración Room 4 → 5 en `AppDatabase`.**
      Entidad + DAO en la base, `version = 5`, `MIGRATION_4_5` que replica el DDL sincronizado de T2
      (`CREATE TABLE IF NOT EXISTS` + `CREATE INDEX`; sin `DROP TABLE` ni
      `fallbackToDestructiveMigration()`), `gastoCompartidoDao()`.
      *Criterio de salida:* `MigrationTestHelper` siembra v4, migra a v5, verifica `user_version = 5`
      y `PRAGMA foreign_key_check` limpio.
      Responsable: Agente SDD | Estimación: 60 min

## Fase 3 — Dominio (núcleo de negocio)

- [ ] **T5. Crear modelo `GastoCompartido` y servicio `GastoCompartidoService` (reparto).**
      Modelo inmutable con `toEntity()`/`fromEntity()`. Servicio (Java puro, sin Android):
      `dividirIgual(monto, contactosIds)` → aritmética entera con residuo de a 1 CLP al inicio de la
      lista, rechaza monto < n; `validarPersonalizada(monto, partes)` → Σ exacto (INV-2), cada parte > 0;
      rechaza 0 contactos (INV-1) y contacto duplicado. Devuelve `ErrorCode`, nunca lanza crudo.
      *Criterio de salida:* invocable y verificable unitariamente; cero advertencias.
      Responsable: Agente SDD | Estimación: 60 min

- [ ] **T6. Implementar el cobro atómico `marcarPagada` en el repositorio/ViewModel.**
      Secuencia exacta de `plan.md` §2 (bloque SQL): validar pendiente → UPDATE estado+fecha →
      INSERT ingreso "Devolución gasto compartido" en la cuenta del egreso →
      `SET saldo_actual = saldo_actual + :monto` — todo en un único `runInTransaction`.
      Agregar los códigos nuevos a `domain/error/ErrorCode.java`. Extender
      `TransaccionViewModel.registrarTransaccion` para insertar las partes en la misma transacción
      del egreso tras validar con el servicio de T5.
      *Criterio de salida:* ninguna vía expone reabrir una parte; fallando cualquier paso no se aplica
      ninguno.
      Responsable: Agente SDD | Estimación: 90 min

- [ ] **T7. Tests de dominio `GastoCompartidoServiceTest` (JVM puro).**
      Cubre CA-1..CA-6 y casos borde 1–6 de `spec.md` §6: reparto igual exacto, con residuo
      (100000/3 → 33334+33333+33333), monto < n rechazado, 0 contactos rechazado, personalizada que
      cuadra y que no, parte ≤ 0, contacto duplicado.
      *Criterio de salida:* rojo → verde documentado; cobertura ≥ 70% en la lógica nueva.
      Responsable: Agente SDD | Estimación: 45 min

- [ ] **T8. Tests de persistencia `GastoCompartidoDaoTest` (Robolectric + Room in-memory).**
      Cubre CA-7, CA-8, CA-11, CA-9 (resumen Σ/orden), INV-4 y el cobro de T6: tras marcar pagada,
      asserts de (a) estado+fecha_pago, (b) ingreso insertado con `cuenta_id`/`monto` correctos,
      (c) saldo = recálculo Σ movimientos (I-1), (d) segundo cobro afecta 0 filas sin tocar nada,
      (e) borrar egreso elimina partes (CASCADE), (f) borrar contacto con pendientes falla RESTRICT →
      `CONTACTO_EN_USO`, (g) `PRAGMA foreign_keys` encendido, (h) `foreign_key_check` limpio al final.
      *Criterio de salida:* rojo → verde documentado; `./gradlew testDebugUnitTest` en verde.
      Responsable: Agente SDD | Estimación: 90 min

## Fase 4 — Interfaz

- [ ] **T9. Formulario de egreso compartido en `ui/cuentas`.**
      Checkbox "Compartido", selector de contactos (lista de la agenda 011), radios "Partes iguales" /
      "Montos personalizados" con filas editables por contacto y suma en vivo; solo se habilita
      "Registrar" cuando el dominio (T5) aprueba. Layouts `dialog_division_gastos.xml`,
      `item_parte_gasto.xml`. La UI solo orquesta: ningún cálculo de reparto ni regla en la Activity.
      *Criterio de salida:* flujo registra egreso + partes visibles en el detalle; errores del dominio
      traducidos a mensajes en español.
      Responsable: Agente SDD | Estimación: 120 min

- [ ] **T10. Resumen de deudas, detalle por contacto y acción "Marcar pagada" en `ui/contactos`.**
      Sección "Resumen de deudas" (Σ pendientes por contacto, CA-9) en la pantalla de contactos;
      detalle del contacto con historial de partes (CA-12, cierra CA-9/CA-10 heredados de 011) y
      botón "Marcar pagada" que invoca el cobro atómico de T6 (CA-7 visible en historial y saldo).
      *Criterio de salida:* en emulador, cobrar una parte sube el saldo de la cuenta y agrega el
      ingreso al historial; contacto con partes no se deja borrar (CA-10).
      Responsable: Agente SDD | Estimación: 120 min

- [ ] **T11. Strings y lint.**
      Textos en español en `res/values/strings.xml` (prefijo de convención de la feature, p. ej.
      `aGastoCompartido_*`); `./gradlew lint` sin advertencias.
      *Criterio de salida:* lint limpio.
      Responsable: Agente SDD | Estimación: 30 min

## Fase 5 — Cierre y trazabilidad

- [ ] **T12. Verificación final completa (orden obligatorio, `connectedCheck` al final).**
      `./gradlew lint` → `./gradlew testDebugUnitTest` → `./gradlew connectedCheck` (emulador activo)
      → `./gradlew assembleDebug`. Pruebas manuales de `plan.md` §5 en emulador.
      *Criterio de salida:* los cuatro comandos en verde; CA-1..CA-12 marcados en `spec.md`.
      Responsable: Agente SDD | Estimación: 60 min

- [ ] **T13. Registrar memoria y commits.**
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

**Notas de ejecución:** [ESPACIO PARA APUNTES DURANTE LA IMPLEMENTACION]
