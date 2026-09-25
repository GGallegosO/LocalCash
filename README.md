# Plantilla SDD Agnóstica para Proyectos de Software

> Repositorio semilla que inicializa CUALQUIER proyecto de software con una arquitectura base
> basada en **Spec-Driven Development (SDD)** y un **sistema de memoria para agentes**.
> No asume lenguaje, framework ni herramienta: todo es plantilla con placeholders
> `[TEXTO_EN_MAYUSCULAS]` listos para buscar y reemplazar.

---

## ¿Qué es esta plantilla?

Es el punto de partida documentado y gobernable de un proyecto. Responde por adelantado a las
tres preguntas que suelen improvisarse (y luego doler):

1. **¿Qué estamos construyendo y por qué?** → `spec/constitution/mission.md`
2. **¿Con qué reglas se trabaja?** → `.agent.md` (contrato maestro para humanos y agentes de IA)
3. **¿Qué pasó antes?** → `doc/` (memoria secuencial de tareas ejecutadas)

La metodología SDD separa tres niveles que no deben mezclarse:

| Nivel | Pregunta         | Artefacto                          |
|-------|------------------|------------------------------------|
| QUÉ   | ¿Qué se construye y cómo se valida? | `spec/features/NNN-nombre/spec.md` |
| CÓMO  | ¿Cómo se implementa y con qué decisiones? | `spec/features/NNN-nombre/plan.md` |
| CUÁNDO| ¿En qué orden se ejecuta?           | `spec/features/NNN-nombre/tasks.md`|

Y la **memoria de agentes** garantiza que ninguna decisión quede solo en una conversación o en la
memoria de un participante: cada tarea cierra creando `doc/NNN-nombre-tarea.md`.

**Ideales para:** proyectos nuevos con agentes de IA colaborativos, equipos pequeños sin proceso
formal, prototipos que deben escalar a producto, o cualquier repositorio donde "¿por qué se decidió
esto?" debería tener respuesta seis meses después.

---

## Estructura de Carpetas

```
.
├── .agent.md                      # Reglas globales: stack, comandos, convenciones, prohibiciones, memoria
├── README.md                      # Este archivo: porta de entrada humana
├── .gitignore                     # Exclusión universal (SO, IDEs, dependencias, builds, secretos)
├── spec/                          # CONTRATOS: qué y por qué (antes de cualquier código)
│   ├── constitution/              # Fundamentos del proyecto (cambian rara vez, se revisan formalmente)
│   │   ├── mission.md             # Visión, problema, objetivos medibles, público objetivo
│   │   ├── tech-stack.md          # Decisiones tecnológicas con justificación (tablas)
│   │   └── roadmap.md             # Hitos: Feature | Estado | Prioridad | Notas
│   └── features/                  # Una carpeta numerada por funcionalidad
│       └── _TEMPLATE_/            # Plantilla maestra: cópiala para cada feature nueva
│           ├── spec.md            # Descripción, contexto, criterios de aceptación, casos borde
│           ├── plan.md            # Componentes, decisiones de arquitectura, riesgos, dependencias
│           └── tasks.md           # Checklist secuencial T1..T5 + Definition of Done
├── doc/                           # MEMORIA del proyecto (registro inmutable de tareas)
│   └── 000-initial-setup.md       # Ejemplo completo del formato de registro
└── src/                           # Código fuente (vacío a propósito hasta elegir stack)
    └── .gitkeep
```

---

## Cómo Usarla (paso a paso)

### Paso 1 — Clonar e iniciar desde cero

```bash
git clone [URL_DE ESTA PLANTILLA] [NOMBRE_DEL_PROYECTO]
cd [NOMBRE_DEL_PROYECTO]
rm -rf .git && git init        # historia limpia: el proyecto nace aquí
git add . && git commit -m "chore(inicializacion): adopta plantilla SDD agnostica"
```

### Paso 2 — Renombrar la identidad

Abre `.agent.md` y reemplaza `[NOMBRE_DEL_PROYECTO]` y `[DESCRIPCION_1_A_2_FRASES]` por los valores
reales. Este archivo será leído por cada agente/desarrollador antes de tocar nada, así que vale la
pena hacerlo bien primero.

Búsqueda rápida de pendientes en todo el repositorio:

```bash
grep -rn "\[A-Z_]*\]" --include="*.md" .
```

### Paso 3 — Completar la constitución (`spec/constitution/`)

Orden recomendado:

1. **`mission.md`** primero: sin visión, problema y objetivos medibles, el resto no tiene criterio
   de priorización.
2. **`tech-stack.md`** segundo: evalúa al menos dos candidatas por categoría y escribe la
   justificación. Esta tarea merece su propio registro `doc/001-seleccion-stack.md`.
3. **`roadmap.md`** tercero: convierte la misión en hitos con estado y prioridad; borra las filas de
   ejemplo cuando tengas las reales.

> Mientras queden placeholders `[TECNOLOGIA]` en `tech-stack.md`, está prohibido escribir código
> dependiente de un lenguaje concreto (regla de agnosticismo en `.agent.md` §6).

### Paso 4 — Crear la primera feature real

```bash
cp -r spec/features/_TEMPLATE_ spec/features/002-[nombre-de-la-feature]
```

Luego, en ese orden estricto:

1. Rellena `spec.md`: descripción, contexto de negocio y criterios de aceptación verificables
   (formato Dado/Cuando/Entonces), más los casos borde.
2. Rellena `plan.md`: componentes involucrados, decisiones con justificación, riesgos y dependencias.
3. Ejecuta `tasks.md` casilla por casilla: contrato → lógica → tests → integración → documentación.
4. Cierra SIEMPRE con el registro de memoria:

```bash
# crea doc/002-[nombre-de-la-feature].md siguiendo el modelo de doc/000-initial-setup.md
```

### Paso 5 — Repetir el ciclo

Cada nueva feature = una carpeta numerada + un registro en `doc/`. La numeración compartida entre
`spec/features/NNN-*` y `doc/NNN-*` da trazabilidad bidireccional: del requisito a lo ejecutado y de
lo ejecutado a su justificación original.

---

## Personalización por Stack

La plantilla es agnóstica; tu stack debe hacerla concreta **sin romper el proceso**:

| Área              | Qué adaptar                                                                 |
|-------------------|------------------------------------------------------------------------------|
| Comandos          | Define `[COMANDO_DEV]`, `[COMANDO_TEST]`, `[COMANDO_BUILD]`, `[COMANDO_LINT]` en `.agent.md` §3 y úsalos exclusivamente. |
| Estructura `src/` | Ajusta el mapa ASCII de `.agent.md` §4 a la convención de carpetas de tu lenguaje/ecosistema. |
| Tipado y calidad  | Fija `[NIVEL_DE_TIPADO]` y `[PORCENTAJE_COBERTURA_MINIMA]` según tus herramientas (`.agent.md` §5.3). |
| CI/CD             | Declara pipeline mínima en `tech-stack.md` §5 y refleja sus etapas en el Definition of Done de `tasks.md`. |
| Herramientas      | Toda adición pasa por `tech-stack.md` con candidatas evaluadas y justificación (prohibición 6 de `.agent.md`). |

Regla de personalización: puedes añadir archivos propios de tu ecosistema (configuraciones,
manifiestos, pipelines), pero nunca elimines secciones de las plantillas; si algo no aplica, márcalo
como `N/A` con una línea de justificación.

---

## Contribuciones

1. **Lee `.agent.md` completo** antes de proponer cambios: es el contrato, no una sugerencia.
2. **Todo cambio relevante** (estructura, reglas, proceso) abre una entrada en `doc/` con su
   motivación, como cualquier feature.
3. **Propuestas de mejora de la plantilla:** ábrelas como issue/PR describiendo el problema de
   proceso observado, no solo la solución; una plantilla que nadie entiende tampoco gobierna.
4. **Mantén el agnosticismo:** si una aportación menciona una tecnología específica fuera de las
   tablas de `tech-stack.md` o de ejemplos claramente marcados como ficticios, se rechaza.
5. **Coherencia de formato:** placeholders siempre `[TEXTO_EN_MAYUSCULAS]`; documentación en español
   técnico; numeración de tres dígitos compartida entre `spec/features/` y `doc/`.

> El valor de esta plantilla no está en sus archivos sino en el hábito que impone: especificar antes
> de construir y recordar después de decidir.
