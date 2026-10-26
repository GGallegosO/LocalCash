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

- [ ] **T3. Crear entidad Room `GastoCompartido` y DAO `GastoCompartidoDao`.**
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

- [ ] **T6. Implementar `marcarPagada()` en el repositorio con `runInTransaction`.**
      Secuencia exacta de `plan.md` §2 (bloque SQL): validar pendiente → UPDATE estado+fecha →
      INSERT ingreso "Devolución gasto compartido" en la cuenta del egreso →
      `SET saldo_actual = saldo_actual + :monto` — todo en un único `runInTransaction`.
      Agregar los códigos nuevos a `domain/error/ErrorCode.java`. El UPDATE usa
      `WHERE id = :parte AND estado = 'pendiente'`; 0 filas afectadas →
      `PARTE_INEXISTENTE`/`PARTE_YA_PAGADA` sin escribir nada más.
      *Criterio de salida:* ninguna vía expone reabrir una parte; fallando cualquier paso no se aplica
      ninguno.
      Responsable: Agente SDD | Estimación: 60 min

- [ ] **T7. Extender `TransaccionViewModel.registrarTransaccion()` para egresos compartidos.**
      Tras el asiento del egreso y dentro de la MISMA transacción, validar con
      `GastoCompartidoService` (T5) e insertar las N partes con `estado='pendiente'` y
      `fecha_creacion`. Si la validación o alguna inserción de parte falla, el egreso tampoco se
      escribe (todo o nada).
      *Criterio de salida:* registrar un egreso compartido con partes inválidas (Σ ≠ total, 0
      contactos) no deja ni movimiento ni partes ni saldo modificado.
      Responsable: Agente SDD | Estimación: 60 min

- [ ] **T8. Tests de dominio `GastoCompartidoServiceTest` (JVM puro).**
      Cubre CA-1..CA-6 y casos borde 1–6 de `spec.md` §6: reparto igual exacto, con residuo
      (100000/3 → 33334+33333+33333), monto < n rechazado, 0 contactos rechazado, personalizada que
      cuadra y que no, parte ≤ 0, contacto duplicado.
      *Criterio de salida:* rojo → verde documentado; cobertura ≥ 70% en la lógica nueva.
      Responsable: Agente SDD | Estimación: 45 min

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

- [ ] **T11. Checkbox "Compartido" + selector de contactos en el formulario de egreso.**
      En `ui/cuentas` (formulario de `CuentasActivity`/`TransaccionViewModel`): marcar el egreso como
      compartido y elegir uno o más contactos de la agenda (Feature 011). Sin selector de contactos el
      registro compartido se rechaza con `GASTO_SIN_CONTACTOS` (dominio, no UI).
      *Criterio de salida:* en emulador se eligen contactos para un egreso; cancelar la selección no
      registra nada.
      Responsable: Agente SDD | Estimación: 60 min

- [ ] **T12. Radios "Partes iguales" / "Montos personalizados" con suma en vivo.**
      Layouts `dialog_division_gastos.xml`, `item_parte_gasto.xml`: modo de división, fila por contacto
      (monto editable solo en personalizada), suma de partes vs total visible en vivo; "Registrar"
      solo se habilita cuando `GastoCompartidoService` (T5) aprueba el reparto. La UI solo orquesta:
      ningún cálculo de reparto ni regla en la Activity.
      *Criterio de salida:* CA-3 y CA-5 verificables a mano: residuo repartido de a 1 CLP; botón
      bloqueado mientras la suma no cuadre.
      Responsable: Agente SDD | Estimación: 60 min

- [ ] **T13. Resumen de deudas + detalle de contacto con historial de partes en `ui/contactos`.**
      Sección "Resumen de deudas" (Σ pendientes por contacto, orden descendente, layout
      `item_resumen_deuda.xml`, CA-9); detalle del contacto con historial de partes compartidas
      (layout `dialog_detalle_contacto.xml`, CA-12, cierra CA-9/CA-10 heredados de 011) usando
      `resumenDeudasPorContacto` y `getPendientesPorContacto` del DAO.
      *Criterio de salida:* el resumen y el detalle se pueblan desde la base; contacto sin partes
      muestra "Sin compras compartidas".
      Responsable: Agente SDD | Estimación: 60 min

- [ ] **T14. Botón "Marcar pagada" + verificación en emulador.**
      Desde el detalle del contacto invoca el cobro atómico de T6 (`marcarPagada()`); recarga resumen
      e historial tras el cobro. Verificar en emulador: el saldo de la cuenta sube exactamente el
      `monto_asignado`, el ingreso aparece en el historial y la parte cambia a `pagado` (CA-7);
      un segundo intento sobre la misma parte muestra "La parte ya fue cobrada" (CA-8); contacto con
      partes pendientes no se deja borrar (CA-10).
      *Criterio de salida:* flujo completo comprobado con `./gradlew installDebug` en emulador.
      Responsable: Agente SDD | Estimación: 60 min

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

**Notas de ejecución:** [ESPACIO PARA APUNTES DURANTE LA IMPLEMENTACION]
