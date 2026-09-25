# doc/000-initial-setup.md — Registro de Memoria: Inicialización del Proyecto

> **Propósito de este archivo:** Es el EJEMPLO DE REFERENCIA del formato de memoria que exige
> `.agent.md` §7. Todas las tareas futuras deben crear un registro análogo (`001-...`, `002-...`).
> El contenido siguiente es ficticio pero realista: muestra el nivel de detalle esperado.
> Al iniciar tu proyecto real, conserva este archivo como modelo y numera tus tareas desde 001.

---

## Ficha de la tarea

| Campo              | Valor                                                        |
|--------------------|--------------------------------------------------------------|
| **Fecha**          | 2026-09-24                                                   |
| **Tarea ejecutada**| Inicialización del repositorio con arquitectura SDD y sistema de memoria de agentes |
| **Responsable**    | [ROL_O_NOMBRE_DEL_EJECUTOR] (en este ejemplo: "Agente de inicialización") |
| **Duración estimada vs real** | [1 sesión] vs [1 sesión]                          |
| **Estado**         | Completado                                                   |

---

## Tarea Ejecutada (resumen narrativo)

Se creó desde cero la estructura base agnóstica al lenguaje del proyecto `[NOMBRE_DEL_PROYECTO]`:
un archivo de reglas global para agentes (`.agent.md`), la constitución de producto en `spec/constitution/`
(misión, stack tecnológico provisional y roadmap), una plantilla reutilizable de feature en
`spec/features/_TEMPLATE_/` (spec + plan + tasks), el directorio de memoria `doc/` con este primer
registro, el directorio de código `src/` reservado con `.gitkeep`, un `.gitignore` universal y el
README de uso de la plantilla. No se seleccionó ninguna tecnología a propósito: todas las decisiones
de stack quedan como placeholders pendientes de la tarea 001.

---

## Archivos Creados / Modificados

**Creados:**
- `.agent.md` — reglas globales, comandos, convenciones, prohibiciones y política de memoria.
- `spec/constitution/mission.md` — plantilla de misión con guías de redacción.
- `spec/constitution/tech-stack.md` — plantillas tabulares de decisión tecnológica.
- `spec/constitution/roadmap.md` — hoja de ruta con tabla de hitos y ejemplo ficticio.
- `spec/features/_TEMPLATE_/spec.md` — plantilla de especificación con criterios Gherkin y casos borde.
- `spec/features/_TEMPLATE_/plan.md` — plantilla de plan técnico (componentes, decisiones, riesgos).
- `spec/features/_TEMPLATE_/tasks.md` — checklist secuencial de 5 fases tipo.
- `doc/000-initial-setup.md` — ESTE registro (modelo de memoria).
- `src/.gitkeep` — reserva del directorio de código.
- `.gitignore` — exclusiones universales por SO, IDE, dependencias, builds y metadatos.
- `README.md` — guía de uso de la plantilla.

**Modificados:**
- `README.md` (existía con un título mínimo) → reemplazado por la documentación completa de la plantilla.

---

## Decisiones Tomadas (con justificación)

1. **Plantilla de feature nombrada `_TEMPLATE_` en lugar de `001-...`.**
   *Justificación:* evita confundir la carpeta de ejemplo con una feature real ya numerada; el
   prefijo de guion bajo además la ordena primero y señala visualmente que no es contenido operativo.
2. **Mantener `src/` vacío con `.gitkeep` en vez de generar código de ejemplo.**
   *Justificación:* el agnosticismo es una regla dura; cualquier archivo de ejemplo sesgaría la
   elección futura de lenguaje. Git no rastrea carpetas vacías, de ahí el marcador.
3. **Numeración de tres dígitos (`000`, `001`, ...) compartida entre `doc/` y `spec/features/`.**
   *Justificación:* permite trazabilidad directa spec ↔ memoria sin ambigüedad de orden lexicográfico.
4. **Los placeholders usan exclusivamente el formato `[TEXTO_EN_MAYUSCULAS]`.**
   *Justificación:* facilita búsqueda y reemplazo automatizado (grep/regex) al adaptar la plantilla.
5. **La selección de stack se pospone a una tarea explícita (001) en lugar de preseleccionar opciones.**
   *Justificación:* obliga a registrar candidatas y justificaciones antes de escribir código, que es
   el espíritu de SDD.

---

## Lecciones Aprendidas

- Redactar las guías de cada sección ("qué va aquí y por qué") toma más tiempo que los títulos, pero
  es lo que convierte una carpeta de archivos vacíos en un proceso utilizable por un tercero.
- Conviene declarar las prohibiciones del agente en imperativos negativos numerados: son fáciles de
  citar en revisiones ("esto viola la prohibición 3 de .agent.md").
- Riesgo detectado: si nadie rellena `doc/` tras cada tarea, la memoria muere en la semana dos.
  Mitigación aplicada: el Definition of Done de `tasks.md` incluye el registro como última fase.
- La distinción spec (QUÉ) vs plan (CÓMO) es frágil en la práctica: hay que revisar que no se cuelen
  nombres de herramientas dentro de los archivos `spec.md`.

---

## Próximos Pasos

1. **Tarea 001 — Selección de stack:** completar `spec/constitution/tech-stack.md` evaluando al menos
   dos candidatas por categoría y registrando la decisión en `doc/001-seleccion-stack.md`.
2. **Tarea 002 — Primera feature real:** copiar `spec/features/_TEMPLATE_/` a
   `spec/features/002-[nombre]/`, rellenar `mission.md` si aún está pendiente y ejecutar el ciclo
   spec → plan → tasks.
3. Rellenar los comandos de `.agent.md` §3 tan pronto exista el primer manifiesto de dependencias.
4. Configurar la pipeline mínima declarada en `tech-stack.md` §5 (lint → tests → build).
5. Definir [RESPONSABLE_O_EQUIPO] y [URL_DEL_REPOSITORIO] reales en `.agent.md` §1.
