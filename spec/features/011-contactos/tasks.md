# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** 011-contactos | **Derivado de:** `spec.md` + `plan.md`
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

## Fase 1 — Definición y contratos

- [ ] **T1. Definir contrato de API / interfaz pública de la feature.**
      Dejar por escrito entrada, salida, códigos de error y ejemplos de uso en `plan.md` §1.
      *Criterio de salida:* el contrato permite escribir tests antes de implementar nada.
      Responsable: Agente SDD | Estimación: 15 min

## Fase 2 — Implementación núcleo

- [ ] **T2. Crear entidad Room `Contacto` y DAO `ContactoDao`.**
      `data/entities/Contacto.java` con campos id, nombre, telefono, email, notas.
      `data/dao/ContactoDao.java` con insert, update, delete, getById, getAll, existsByTelefono, existsByEmail, getComprasComoPagador, getDeudasComoDeudor.
      *Criterio de salida:* la entidad y el DAO compilan; cero advertencias de linter.
      Responsable: Agente SDD | Estimación: 45 min

- [ ] **T3. Crear modelo de dominio `Contacto` y servicio `ContactoService`.**
      `domain/model/Contacto.java` inmutable, con `toEntity()` / `fromEntity()`.
      `domain/service/ContactoService.java` con validarContacto() (nombre no vacío, teléfono/email únicos) y puedeEliminar() (no tiene deudas pendientes/parciales).
      *Criterio de salida:* la lógica es invocable y verificable unitariamente; cero advertencias de linter.
      Responsable: Agente SDD | Estimación: 45 min

- [ ] **T4. Agregar migración Room 3 → 4 en `AppDatabase`.**
      Agregar entidad Contacto, DAO ContactoDao, versión 4, MIGRATION_3_4 que crea tabla CONTACTOS e índices.
      *Criterio de salida:* `MigrationTestHelper` siembra datos v3, migra a v4, verifica `user_version = 4` y `PRAGMA foreign_key_check` limpio.
      Responsable: Agente SDD | Estimación: 60 min

- [ ] **T5. Crear tests unitarios de dominio y DAO.**
      `ContactoServiceTest`: validación de nombre vacío, teléfono duplicado, email duplicado.
      `ContactoDaoTest`: CRUD, unicidad de teléfono/email, RESTRICT en eliminación con deudas pendientes.
      *Criterio de salida:* tests en rojo → verde documentados; cobertura ≥ 70% en la lógica nueva.
      Responsable: Agente SDD | Estimación: 60 min

## Fase 3 — Integración

- [x] **T6. Crear UI: Activity, ViewModel, layouts, adaptador.**
      `ContactosActivity`, `ContactosViewModel`, `activity_contactos.xml`, `item_contacto.xml`,
      `dialog_contacto.xml`, `detalle_contacto.xml`, `ContactoAdapter`.
      Conectar con ViewModel y DAO; manejar estados de carga/vacío/error.
      *Criterio de salida:* flujo completo funciona en emulador con `./gradlew installDebug`.
      Responsable: Agente SDD | Estimación: 120 min

- [x] **T7. Agregar strings y verificar lint.**
      `res/values/strings.xml` con textos de contacto.
      `./gradlew lint` sin advertencias.
      *Criterio de salida:* lint limpio.
      Responsable: Agente SDD | Estimación: 30 min

## Fase 4 — Cierre y trazabilidad

- [ ] **T8. Documentar cambios y registrar memoria.**
      Crear `doc/011-contactos.md` con decisiones, lecciones, verificación de CA.
      Marcar criterios de `spec.md` verificados. Actualizar `roadmap.md` si corresponde.
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
- [ ] Registro de memoria creado: `doc/011-contactos.md`
- [ ] `data-model.md` actualizado si la feature tocó el esquema (en el mismo cambio)
- [ ] Commits con formato de `.agent.md` §5.2, uno por unidad lógica

**Notas de ejecución:** [ESPACIO PARA APUNTES DURANTE LA IMPLEMENTACION]