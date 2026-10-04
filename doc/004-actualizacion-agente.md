# doc/004-actualizacion-agente.md — Registro de Memoria: Configuración del Contrato Maestro del Agente

> **Propósito de este archivo:** Documentar la actualización de `.agent.md` (contrato maestro) y la
> creación de `AGENTS.md` exigidas por `.agent.md` §7 tras una tarea de configuración del repo.

---

## Ficha de la tarea

| Campo                         | Valor                                                         |
| ----------------------------- | ------------------------------------------------------------- |
| **Fecha**                     | 2026-09-28                                                    |
| **Tarea ejecutada**           | Configuración del contrato maestro: `.agent.md` + `AGENTS.md` |
| **Responsable**               | GG                                                            |
| **Duración estimada vs real** | [1 sesión] vs [1 sesión]                                      |
| **Estado**                    | Completado                                                    |

---

## Tarea Ejecutada (resumen narrativo)

Se eliminaron los 25 placeholders que quedaban en `.agent.md` sustituyéndolos por la especificación
oficial del proyecto: identidad, comandos canónicos, estructura Android, stack fijo, precisión
financiera y la obligación de memoria. En paralelo se creó `AGENTS.md`, un resumen operativo
destinado a agentes de IA que recoge lo que **no** se puede deducir leyendo el repositorio: el
estado real (no hay código todavía), el orden obligatorio de verificación y las trampas del esquema
de datos que la tarea 003 ya verificó una a una.

El diagnóstico previo detectó tres trampas que un agente habría pasado por alto:

1. **`./gradlew` no existe todavía**: no hay `build.gradle`, ni wrapper, ni `app/`, ni `.github/`. Los
   comandos se declararon como canónicos pero explícitamente **no ejecutables hoy**, para que nadie
   los "verifique" por ejecución ni interprete su ausencia como un fallo.
2. **La prohibición 4 de `.agent.md` §6 estaba obsoleta**: decía "prohibido generar código
   dependiente de un lenguaje mientras `tech-stack.md` tenga placeholders", pero `tech-stack.md`
   quedó cerrado en la tarea 001. Se reescribió en el sentido correcto: lo prohibido es usar tecnología
   _no registrada_.
3. **`spec/features/_TEMPLATE_/tasks.md` conservaba los placeholders de comandos**, y toda feature
   futura los copia literalmente en su Definition of Done. Dejaban comandos irresolubles justo donde
   se verifica la feature.

También se reescribió `README.md`, que hasta ahora se describía como "plantilla SDD agnóstica" con
rutas genéricas (`src/`) y comandos sin resolver, e inducía a error sobre qué es este repositorio. Y
se eliminó `src/.gitkeep`: la estructura de código pasa a `app/`, y un directorio vacío heredado de
la plantilla invitaba a escribir código en el lugar equivocado.

---

## Archivos Creados / Modificados

**Creados:**

- `AGENTS.md` — instrucciones operativas para agentes de IA. Declara que `.agent.md` manda sobre él,
  y resume estado real del repo, flujo SDD, comandos canónicos, trampas del esquema, convenciones
  que difieren de los defaults y una tabla de "dónde está cada cosa".
- `doc/004-actualizacion-agente.md` — ESTE registro.

**Modificados:**

- `.agent.md` — 25 placeholders reemplazados. §2 (stack, 6 filas desde `tech-stack.md`), §3 (5
  comandos + orden obligatorio + nota de estado), §4 (mapa Android con `app/src/main/java/com/localcash/`
  y `spec/constitution/diagrams/`), §5.1 a §5.4 (arquitectura por capas, commits, tipado, errores)
  y §6 (prohibición 4 reescrita + 3 nuevas: pilar offline, dinero `INTEGER`, no migraciones
  destructivas).
- `spec/features/_TEMPLATE_/tasks.md` — 4 placeholders de comandos resueltos con los comandos
  reales, cobertura 70%, y el Definition of Done pasó de 5 a 7 casillas (se separaron tests
  unitarios de integración/UI y se añadió la casilla de `data-model.md` cuando la feature toca el
  esquema).
- `README.md` — reescrito como documentación real del proyecto LocalCash: identidad, ciclo SDD,
  estructura, comandos, stack, 5 reglas del proyecto y guía de contribuciones.

**Eliminados:**

- `src/.gitkeep` — el código pasa a `app/` (declarado en `.agent.md` §4).

---

## Decisiones Tomadas (con justificación)

1. **`AGENTS.md` como resumen, no como copia de `.agent.md`.**
   _Justificación:_ duplicar el contrato maestro en dos archivos garantiza que se desincronicen. Se
   eligió que `AGENTS.md` contenga solo lo no deducible del código (estado real, trampas verificadas,
   orden de verificación) y que remita explícitamente a `.agent.md` con la regla de que, ante
   conflicto, gana `.agent.md`.
2. **`./gradlew installDebug` en la fila "Levantar entorno dev".**
   _Justificación:_ Android no tiene servidor de desarrollo, así que la fila quedaba sin equivalente
   directo. `installDebug` (compila e instala en el dispositivo) es su análogo funcional y es
   coherente con el resto de comandos, que también exigen un dispositivo conectado. La fila
   "Instalar dependencias" se completó como `N/A` con justificación, porque el wrapper Gradle resuelve
   las dependencias y no existe un comando de instalación aparte.
3. **Lista de tipos de commit intacta, con la divergencia anotada.**
   _Justificación:_ el historial real usa `dosc(sdd):` en 4 commits de documentación, tipo que no está
   en la lista de la plantilla. Se respetó la decisión de mantener la lista original y se documentó
   la divergencia como caso conocido, indicando que no debe replicarse: para documentación se usa
   `docs(agente)`. Así un agente futuro no "corrige" commits ya escritos ni inventa un cuarto formato.
4. **Prohibición 4 reescrita en lugar de eliminada.**
   _Justificación:_ el número de la lista se conserva para no romper referencias, pero su contenido
   cambió de sentido: ya no se protege el agnosticismo (el stack está cerrado) sino que se prohíbe
   usar tecnología no registrada. Mantener el texto viejo habría dejado la prohibición inútil, que es
   peor que no tenerla.
5. **Añadidas tres prohibiciones derivadas de la constitución, no del enunciado original.**
   _Justificación:_ el pilar offline, la precisión financiera en centavos y la prohibición de
   migraciones destructivas son restricciones de producción que viven en `data-model.md` y
   `tech-stack.md` pero no estaban en la lista de prohibiciones de `.agent.md`, que es el documento
   que un agente lee primero. Declararlas donde ya se cumplen evita que se descubran tarde.
6. **El Definition of Done pasó de 5 a 7 casillas.**
   _Justificación:_ "Tests completos en verde" escondía que `connectedCheck` requiere dispositivo y
   falla de forma distinta a los unitarios. Separarlos hace visible esa dependencia, y la casilla de
   `data-model.md` ennegrece la regla de sincronización que el propio documento del esquema exige.
7. **`src/` eliminado y no conservado como reserva.**
   _Justificación:_ la plantilla agnóstica usaba `src/` porque el stack era desconocido. Con el stack
   cerrado y la estructura Android declarada, un directorio vacío de 0 bytes es una invitación a
   escribir código fuera de la convención, que es exactamente el tipo de desviación silenciosa que
   después cuesta deshacer.
8. **Ningún ejemplo de commit inventado como si fuera real.**
   _Justificación:_ el ejemplo `feat(autenticacion): agrega validación biométrica al acceso` está
   marcado como "ejemplo real del proyecto" cuando todavía no existe tal commit. Se cambió a un
   formato ilustrativo sin afirmar que ocurrió, para no contaminar la memoria con hechos inventados.

---

## Lecciones Aprendidas

- **Un contrato maestro con placeholders sin resolver es peor que uno ausente**, porque se lee como
  si estuviera completo. Los 25 placeholders de `.agent.md` convivían con documentos de constitución
  ya cerrados: el stack se elegía en `tech-stack.md` mientras el contrato seguía diciendo "agnóstico".
  Merece la pena un chequeo periódico de placeholders en los documentos de gobierno.
- **El orden de las ediciones importa para la verificación.** Actualizar `.agent.md` antes que
  `tasks.md` permitió usar los comandos definitivos como origen único y evitar que los dos documentos
  discreparan. Si se hubiera ido al revés, habría que corregir las referencias.
- **Los directorios vacíos heredados de una plantilla son deuda silenciosa.** `src/.gitkeep` no
  producía ningún error, pero describía una estructura de proyecto que ya no era la válida.
- **Borrar la tabla de "Personalización por Stack" del README también fue una decisión.** Contenía
  los mismos placeholders de comandos que `.agent.md`; mantenerla habría dejado el README como una
  segunda fuente de verdad del stack, justo lo que se quería evitar en `.agent.md`.
- **Un registro de memoria en sí mismo es requisito de su propia creación.** Esta tarea modificó el
  contrato que obliga a cerrar con `doc/NNN-nombre-tarea.md`; cumplir la regla en el mismo cambio es la
  única forma de que la tarea sea coherente consigo misma.

---

## Próximos Pasos

1. **Primer hito de Fase 1 — proyecto Gradle:** crear `app/build.gradle`, el wrapper `gradlew` y
   `local.properties` ignorado por Git, para que los comandos canónicos de `.agent.md` §3 pasen a ser
   ejecutables y `AGENTS.md` pueda dejar de declarar que no lo son.
2. **Primera feature real:** copiar `spec/features/_TEMPLATE_/` a
   `spec/features/004-[nombre]/` y ejecutar el ciclo spec → plan → tasks siguiendo el orden de
   `AGENTS.md`.
3. **Materializar el esquema:** implementar `SQLiteOpenHelper.onCreate()` con el DDL del Apéndice A de
   `data-model.md`, con `PRAGMA foreign_keys = ON` en `onConfigure()` y `PRAGMA user_version = 1`.
4. **Convertir la validación en tests:** los 23 casos de rechazo y los comportamientos de borrado
   validados con `sqlite3` en la tarea 003 deben convertirse en tests de integración permanentes
   (Robolectric o `MigrationTestHelper`), como está registrado en `doc/003-modelo-datos.md`.
5. **Pendientes detectados en esta tarea, fuera del alcance aprobado:**
   - `spec/constitution/roadmap.md` §3 tiene un placeholder sin resolver:
     `[NUMERO_MAXIMO_DE_FEATURES_SIMULTANEAS]`. Es una decisión pendiente, no un ejemplo.
   - `spec/constitution/tech-stack.md` §1 conserva una "regla de oro de agnosticismo" superada por
     esta misma tarea; conviene actualizarla o marcarla como histórica.
   - Completar los placeholders por-feature de `_TEMPLATE_/spec.md` y `_TEMPLATE_/plan.md` no se hizo
     porque son deliberados: se rellenan en cada feature.
6. **Configurar la CI declarada en `tech-stack.md` §5** cuando exista el proyecto Gradle, replicando
   el orden `lint` → tests → build.
