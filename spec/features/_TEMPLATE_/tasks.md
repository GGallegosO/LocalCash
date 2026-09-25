# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** [NNN]-[nombre-de-la-feature] | **Derivado de:** `spec.md` + `plan.md`
>
> **Propósito de este documento:** Descomponer la implementación en pasos ejecutables, ordenados y
> verificables. Cada casilla marcada `[x]` debe corresponder a un cambio real comprobable (tests
> verdes, archivo creado, comando ejecutado). Marcar sin verificar está prohibido.
>
> **Convenciones:**
> - Ejecuta las tareas EN ORDEN: cada una parte de la anterior completada.
> - Al terminar cada bloque relevante, ejecuta el lint/test con los comandos de `.agent.md` §3.
> - Si una tarea revela que el plan era incorrecto: DETENTE, actualiza `plan.md` con justificación
>   y continúa. Nunca implementes "al margen" del plan.

---

## Fase 1 — Definición y contratos

- [ ] **T1. Definir contrato de API / interfaz pública de la feature.**
      Dejar por escrito entrada, salida, códigos de error y ejemplos de uso en `plan.md` §1.
      *Criterio de salida:* el contrato permite escribir tests antes de implementar nada.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

## Fase 2 — Implementación núcleo

- [ ] **T2. Implementar lógica de negocio.**
      Codificar las reglas del dominio según `plan.md`, respetando el patrón arquitectónico de
      `.agent.md` §5.1. Sin tocar la capa de interfaz todavía.
      *Criterio de salida:* la lógica es invocable y verificable unitariamente; cero advertencias de linter.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T3. Crear tests unitarios.**
      Cubrir cada criterio de aceptación (CA-1..CA-N) y los casos borde marcados como críticos en
      `spec.md` §4, incluyendo caminos de error.
      *Criterio de salida:* tests en rojo → verde documentados; cobertura ≥ [PORCENTAJE_COBERTURA_MINIMA]% en la lógica nueva.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

## Fase 3 — Integración

- [ ] **T4. Integrar con frontend / capas consumidoras.**
      Conectar la feature con sus consumidores reales usando el contrato de T1; ajustar traducción
      de errores y estados de carga/ vacío según `spec.md`.
      *Criterio de salida:* flujo completo funciona en entorno local con el comando de desarrollo declarado.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

## Fase 4 — Cierre y trazabilidad

- [ ] **T5. Documentar cambios y registrar memoria.**
      Actualizar README/docs afectados, marcar criterios de `spec.md` verificados, actualizar el
      hito en `roadmap.md` y crear `doc/[NNN]-[nombre-tarea].md` con decisiones y lecciones
      (obligatorio según `.agent.md` §7).
      *Criterio de salida:* otra persona podría mantener la feature leyendo solo estos documentos.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

---

## Verificación final (Definition of Done)

- [ ] Lint sin advertencias: `[COMANDO_LINT]`
- [ ] Tests completos en verde: `[COMANDO_TEST]`
- [ ] Build válido: `[COMANDO_BUILD]`
- [ ] Registro de memoria creado: `doc/[NNN]-[nombre-tarea].md`
- [ ] Commits con formato de `.agent.md` §5.2, uno por unidad lógica

**Notas de ejecución:** [ESPACIO PARA APUNTES DURANTE LA IMPLEMENTACION]
