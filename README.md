# LocalCash

> Aplicación móvil **Android nativa en Java** para la gestión financiera personal: cuentas,
> movimientos geolocalizados en mapas y división de gastos compartidos. Funciona **100% offline** y
> con **privacidad absoluta**: no hay servidor, ni backend, ni cuenta de usuario. Todos los datos
> viven en un único archivo SQLite dentro del dispositivo.

- **Contrato maestro del proyecto:** [`.agent.md`](.agent.md) (reglas, comandos, prohibiciones).
- **Instrucciones resumidas para agentes de IA:** [`AGENTS.md`](AGENTS.md).
- **Repositorio:** https://github.com/GGallegosO/LocalCash

---

## Cómo se trabaja aquí: Spec-Driven Development

Este repositorio separa tres niveles que no deben mezclarse, y cada decisión queda registrada con su
justificación. La pregunta que decide en qué carpeta escribir es siempre la misma:

| Nivel | Pregunta                                  | Artefacto                          |
|-------|-------------------------------------------|------------------------------------|
| QUÉ   | ¿Qué se construye y cómo se valida?      | `spec/features/NNN-nombre/spec.md` |
| CÓMO  | ¿Cómo se implementa y con qué decisiones? | `spec/features/NNN-nombre/plan.md` |
| CUÁNDO| ¿En qué orden se ejecuta?                 | `spec/features/NNN-nombre/tasks.md`|

El ciclo de una feature:

1. **Lee `.agent.md` y los últimos registros de `doc/`** antes de decidir nada.
2. Copia la plantilla y numera con tres dígitos:
   ```bash
   cp -r spec/features/_TEMPLATE_ spec/features/004-[nombre-de-la-feature]
   ```
3. Rellena `spec.md` → `plan.md` → ejecuta `tasks.md` casilla por casilla.
4. **Cierra con `doc/NNN-[nombre-de-la-feature].md`.** Sin ese registro la tarea no está terminada.

La numeración de tres dígitos es compartida entre `spec/features/NNN-*` y `doc/NNN-*`, lo que da
trazabilidad bidireccional: del requisito a lo ejecutado y de lo ejecutado a su justificación.

---

## Estructura

```
.
├── .agent.md                      # CONTRATO MAESTRO: reglas, comandos, prohibiciones, memoria
├── AGENTS.md                      # Resumen operativo para agentes de IA
├── README.md                      # Este archivo
├── .gitignore                     # Exclusión universal (SO, IDEs, dependencias, builds, secretos)
├── spec/                          # CONTRATOS: qué y por qué (antes de cualquier código)
│   ├── constitution/              # Fundamentos: cambian rara vez, se revisan formalmente
│   │   ├── mission.md             # Visión, problema, objetivos medibles, público objetivo
│   │   ├── tech-stack.md          # Decisiones tecnológicas con candidatas evaluadas
│   │   ├── roadmap.md             # Hitos por fase, estado y prioridad
│   │   ├── data-model.md          # Esquema oficial: ERD, diccionario, invariantes y DDL canónico
│   │   └── diagrams/              # Imágenes renderizadas del ERD (complemento del código)
│   └── features/                  # Una carpeta numerada por funcionalidad
│       └── _TEMPLATE_/            # Plantilla maestra: cópiala para cada feature nueva
│           ├── spec.md            # Descripción, contexto, criterios de aceptación, casos borde
│           ├── plan.md            # Componentes, decisiones, riesgos, dependencias
│           └── tasks.md           # Checklist secuencial + Definition of Done
├── doc/                           # MEMORIA del proyecto (un registro por tarea ejecutada)
│   ├── 000-initial-setup.md       # Ejemplo completo del formato de registro
│   ├── 001-seleccion-stack.md
│   ├── 002-seleccion-roadmap.md
│   ├── 003-modelo-datos.md
│   └── 004-actualizacion-agente.md
└── app/                           # Proyecto Gradle Android (se crea en el primer hito de Fase 1)
    └── src/
        ├── main/java/com/localcash/{ui,domain,data}/
        ├── main/res/{layout,values,drawable}/
        ├── test/java/             # JUnit + Mockito + Robolectric (JVM local)
        └── androidTest/java/      # Espresso (requiere dispositivo)
```

> **Estado actual:** el repositorio está en fase de especificación. `app/` todavía no existe; la
> estructura Java nativa es la convención declarada en `.agent.md` §4, no un árbol de archivos real.

---

## Comandos canónicos

Definidos en `.agent.md` §3 y usados exclusivamente en el Definition of Done de cada feature.

| Acción                | Comando                                                    |
| --------------------- | ---------------------------------------------------------- |
| Levantar entorno dev  | `./gradlew installDebug`                                   |
| Tests unitarios       | `./gradlew testDebugUnitTest`                              |
| Tests de integración y UI | `./gradlew connectedCheck` (requiere dispositivo)       |
| Lint / calidad        | `./gradlew lint`                                           |
| Compilar              | `./gradlew assembleDebug`                                  |

Orden obligatorio antes de cerrar una feature:
`lint` → `testDebugUnitTest` → `connectedCheck` → `assembleDebug`.

Dependencias: **N/A**. El wrapper Gradle las resuelve; tras cambiar el manifiesto basta recompilar.

---

## Stack

Java 8+ / JDK 11 · Android Views + Material Components · SQLite local (`SQLiteOpenHelper` en
Fases 1-2, `Room` desde la Fase 3) · `osmdroid` 6.x para mapas offline · AndroidX `BiometricPrompt`
· JUnit, Mockito, Robolectric, Espresso y Android Lint.

El detalle de cada elección, las alternativas descartadas y su justificación están en
[`spec/constitution/tech-stack.md`](spec/constitution/tech-stack.md). **Ninguna dependencia nueva se
agrega sin registrarse ahí primero.**

---

## Reglas del proyecto

1. **100% offline y privado:** ninguna funcionalidad puede exigir backend, conexión permanente ni API
   en la nube.
2. **El dinero es `INTEGER` en centavos**, nunca `REAL` ni `double` en la capa de datos; `BigDecimal`
   solo en `domain/`.
3. **El esquema está cerrado y documentado:** su fuente de verdad es
   [`spec/constitution/data-model.md`](spec/constitution/data-model.md) (Apéndice A contiene el DDL
   ejecutable). Cambiarlo exige actualizar ese documento en el mismo cambio.
4. **Nada de migraciones destructivas:** la versión del esquema vive en `PRAGMA user_version`.
5. **Cada tarea cierra con su registro en `doc/`.** Un registro incompleto equivale a una tarea sin
   terminar.

---

## Contribuciones

1. Lee [`.agent.md`](.agent.md) completo antes de proponer cambios: es el contrato, no una sugerencia.
2. **Todo cambio relevante** (estructura, reglas, proceso, esquema) abre una entrada en `doc/` con su
   motivación, igual que una feature.
3. **Coherencia de formato:** documentación en español técnico; identificadores de código en inglés
   técnico; placeholders de plantilla con formato `[TEXTO_EN_MAYUSCULAS]`; numeración de tres
   dígitos compartida entre `spec/features/` y `doc/`.
4. **Cobertura mínima del 70%** en lógica de negocio y repositorios/DAOs, y cero advertencias de
   lint.
5. `main` está protegida: el trabajo va en una rama `<tipo>/<descripcion-en-kebab>` y se integra con
   *pull request*.

> El valor de esta estructura no está en sus archivos, sino en el hábito que impone: especificar
> antes de construir, y recordar después de decidir.
