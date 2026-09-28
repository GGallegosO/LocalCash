# AGENTS.md — Instrucciones para agentes de IA

> `.agent.md` es el **contrato maestro** del proyecto (identidad, stack, comandos, reglas,
> prohibiciones, memoria) y manda sobre este archivo, que solo resume lo que un agente **no puede
> deducir leyendo el código**: el estado real del repo y los errores que ya se cometieron.
> Ante cualquier conflicto, gana `.agent.md`.

## Estado real del repositorio (antes de tocar nada)

- Esto es un repositorio **de especificación, sin código todavía**: no existe `build.gradle`, ni
  wrapper `gradlew`, ni `app/`, ni `.github/`. No busques entrypoints: aún no hay.
- **Los comandos de la tabla siguiente no son ejecutables hoy.** Son la declaración canónica que se
  cumplirá en cuanto exista el proyecto Gradle (primer hito de Fase 1). No los "verifiques"
  ejecutándolos: no hay nada que compilar todavía.
- El único esquema de base de datos es el que está **escrito y validado** en
  `spec/constitution/data-model.md` (Apéndice A). Ese DDL es la fuente de verdad; el código de
  persistencia debe reproducirlo, no inventar un esquema paralelo.
- `spec/features/` solo contiene `_TEMPLATE_`. Ninguna feature real ha salido todavía.

## Flujo obligatorio de trabajo (SDD)

1. Lee `.agent.md` completo. Es el contrato, no una sugerencia.
2. Lee `doc/` (al menos los dos últimos registros) antes de decidir: ahí está por qué el esquema y
   el stack son como son.
3. Crea `spec/features/NNN-nombre/` desde `_TEMPLATE_` y rellena **en este orden**: `spec.md`
   (QUÉ) → `plan.md` (CÓMO) → `tasks.md` (ejecución). `spec.md` no puede nombrar librerías.
4. Ejecuta `tasks.md` casilla por casilla. Si una casilla revela que el plan estaba mal: detente,
   corrige `plan.md` con justificación, continúa. Nada "al margen" del plan.
5. **Cierra con `doc/NNN-nombre-tarea.md`.** Sin ese registro la tarea no está terminada.
6. Numeración de **tres dígitos compartida** entre `spec/features/NNN-*` y `doc/NNN-*`.

## Comandos canónicos

| Acción | Comando | Nota |
|---|---|---|
| Levantar entorno de desarrollo | `./gradlew installDebug` | Compila e instala en el dispositivo/emulador. |
| Tests unitarios | `./gradlew testDebugUnitTest` | JVM local, sin dispositivo. |
| Tests de integración y UI | `./gradlew connectedCheck` | **Requiere dispositivo o emulador activo.** |
| Lint / calidad | `./gradlew lint` | Cero advertencias aceptadas. |
| Compilar | `./gradlew assembleDebug` | Release: `assembleRelease` / `bundleRelease`. |
| Instalar dependencias | `N/A` | El wrapper Gradle las resuelve; tras cambiar el manifiesto basta recompilar. |

**Orden antes de cerrar una feature** (Definition of Done de `tasks.md`):
`lint` → `testDebugUnitTest` → `connectedCheck` → `assembleDebug`.

Ejecuta `connectedCheck` al final, no al principio: si falla, ya perdiste el ciclo de compilación.

## Reglas del esquema de datos (verificadas en `data-model.md`)

Estas son las que cuestan un bug de producción si se ignoran:

- **Dinero siempre `INTEGER` en centavos.** Nunca `REAL`, nunca `double` en la capa de datos: un
  saldo es una suma acumulada y en IEEE 754 la deriva aparece justo en el número que ve el usuario.
  `BigDecimal` solo en `domain/`, y la conversión a centavos vive en **un único** par de funciones.
- **`PRAGMA foreign_keys = ON` es un flag por conexión, no una propiedad del archivo.** Si la app
  abre la base por una vía que no lo activa, todo el `ON DELETE CASCADE` del DDL deja de ocurrir en
  silencio. Actívalo en `onConfigure()` (o verifica que Room lo haga) y ten un test que lo compruebe.
- **El `CHECK` de estado en deudas valida la fila, no el historial.** SQLite aceptaría escribir
  `estado = 'pendiente', monto_pagado = 0` sobre una fila que estaba `parcial`. La máquina de estados
  la hace cumplir el repositorio: el DAO no expone "rebajar abono" ni "reabrir deuda", y eso
  necesita test propio.
- **`CUENTAS.saldo_actual` es una proyección, no la fuente de verdad.** `TRANSACCIONES` lo es. Se
  actualiza solo con aritmética atómica (`SET saldo_actual = saldo_actual ± :monto`); nunca con
  leer-restar-escribir, que tiene condición de carrera.
- **Solo construcciones de SQLite 3.8+ (`minSdk` 23):** nada de `STRICT`, índices de expresión,
  `RETURNING`, columnas generadas ni `ON CONFLICT DO NOTHING` (ese es _upsert_, 3.24+). Para la
  semilla usa `INSERT OR IGNORE`.
- **Migraciones:** `PRAGMA user_version` es la versión del esquema. `fallbackToDestructiveMigration()`
  y cualquier `DROP TABLE` como estrategia de actualización están prohibidos: perderían información
  financiera sin que el usuario lo decida.
- **Nombres de tabla por constante del DAO**, nunca literales dispersos: un error tipográfico en un
  literal es un fallo en tiempo de ejecución, no de compilación.

## Trampa de proceso: el ERD y el DDL se desalinean en silencio

El diagrama Mermaid es la fuente de verdad del modelo, pero nadie lo valida solo por lectura.
Cuando toques el esquema:

- **Compara por `diff` el bloque Mermaid del documento contra el ERD aprobado.** Un nombre de tabla
  mal escrito en *una* línea de relación no da error visible: Mermaid crea una entidad fantasma.
  Comprueba que las relaciones referencian entidades declaradas.
- **Ejecuta el DDL antes de darlo por bueno.** `sqlite3` local vale como banco de pruebas del esquema
  mientras no exista el proyecto: crea el esquema, corre `PRAGMA foreign_key_check`, y prueba los
  caminos de rechazo (`CHECK`, `UNIQUE`, `RESTRICT`) y de borrado (`CASCADE`, `SET NULL`).
- Actualiza `data-model.md` **en el mismo cambio** que el código que lo implementa. Si divergen, hay
  un bug de documentación (regla del propio documento).

## Convenciones que difieren de los defaults

- **Documentación en español técnico; identificadores de código en inglés** (`montoAsignado`,
  `saldoActual`). Comentarios y textos de usuario, en español.
- **Commits:** Conventional Commits con los tipos de `.agent.md` §5.2. Ojo: el historial previo usa
  `dosc(sdd):`, que **no está en la lista** de la plantilla; no lo repliques, usa `docs(...)`.
- **Ramas:** `main` protegida; trabajo en `<tipo>/<descripcion-en-kebab>` (ejemplo real:
  `docs/setup-sdd`).
- **Cobertura mínima 70%** en lógica de negocio y repositorios/DAOs.
- **Toda dependencia nueva** se registra primero en `spec/constitution/tech-stack.md` con candidatas
  evaluadas. Es una prohibición dura, no una recomendación.

## Dónde está cada cosa

| Pregunta | Archivo |
|---|---|
| Qué construimos y por qué, objetivos medibles | `spec/constitution/mission.md` |
| Stack, alternativas descartadas, testing, CI/CD | `spec/constitution/tech-stack.md` |
| Qué se entrega y en qué orden | `spec/constitution/roadmap.md` |
| Esquema, ERD, DDL canónico, invariantes, índices | `spec/constitution/data-model.md` |
| Reglas, comandos, prohibiciones (contrato maestro) | `.agent.md` |
| Qué se decidió antes y por qué | `doc/NNN-*.md` |
| Qué haría una feature concreta | `spec/features/NNN-nombre/spec.md` → `plan.md` → `tasks.md` |
