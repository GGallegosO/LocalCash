# doc/002-seleccion-roadmap.md — Registro de Memoria: Inicialización del Proyecto

> **Propósito de este archivo:** Es el EJEMPLO DE REFERENCIA del formato de memoria que exige
> `.agent.md` §7. Todas las tareas futuras deben crear un registro análogo (`001-...`, `002-...`).
> El contenido siguiente es ficticio pero realista: muestra el nivel de detalle esperado.
> Al iniciar tu proyecto real, conserva este archivo como modelo y numera tus tareas desde 001.

---

## Ficha de la tarea

| Campo                         | Valor                                                                        |
| ----------------------------- | ---------------------------------------------------------------------------- |
| **Fecha**                     | 2026-09-24                                                                   |
| **Tarea ejecutada**           | Definición de la Hoja de Ruta (Roadmap), Fases Temporales e Hitos de Entrega |
| **Responsable**               | GG                                                                           |
| **Duración estimada vs real** | [1 sesión] vs [1 sesión]                                                     |
| **Estado**                    | Completado                                                                   |

---

## Tarea Ejecutada (resumen narrativo)

Se definió y estructuró la hoja de ruta del proyecto (**roadmap.md**) para la aplicación móvil **LocalCash** en Android Studio (Java). La planificación se dividió en **4 fases estratégicas** ajustadas a las fechas límite del proyecto (28 de septiembre, 5 de octubre, 12 de octubre y entrega final el 19 de octubre de 2026). Cada fase agrupa funcionalidades verificables que integran los 4 pilares obligatorios del sistema: desarrollo nativo en Java, persistencia local en SQLite, autenticación biométrica y mapa interactivo con `osmdroid`.

---

## Archivos Creados / Modificados

**Creados:**

- `spec/constitution/roadmap.md` — Documento principal del roadmap con el resumen de fases, tabla de hitos, reglas de priorización, gestión de deuda técnica y criterios de cierre.
- `doc/002-selection-roadmap.md` — Registro formal de memoria (este archivo) para el control de cambios según la norma de gestión del proyecto.

**Modificados:**

- N/A

---

## Decisiones Tomadas (con justificación)

- **Estructura en 4 fases incrementales semanales (28 Sep, 5 Oct, 12 Oct, 19 Oct):** _Justificación:_ Asegura entregables funcionales cada semana y evita acumulaciones de trabajo crítico antes de la entrega final del 19 de octubre.
- **Priorización de la Seguridad y Persistencia Local en la Fase 1 (Entregable 28 de Sep):** _Justificación:_ La autenticación biométrica (`BiometricPrompt`) y la base de datos local SQLite son el núcleo _offline_ de la aplicación[1]. Validarlos primero mitiga riesgos estructurales de la arquitectura.
- **Mapeo offline (** **osmdroid** **) asignado a la Fase 2 (Entregable 5 de Oct):** _Justificación:_ Requiere que la creación de cuentas y el registro de movimientos en SQLite estén estabilizados antes de asociarles coordenadas geográficas e integrarlos en el mapa[3][5].
- **Módulo de Gastos Compartidos y Deudas asignado a la Fase 3 (Entregable 12 de Oct):** _Justificación:_ Es la lógica de negocio con mayor complejidad matemática (desglose de productos y balances pendientes entre contactos)[6][7]. Necesita que la agenda de contactos esté previamente consolidada[3].
- **Criterios de Cierre Estrictos (** **Definition of Done** **):** _Justificación:_ Ningún hito pasa a `Completado` sin verificación de `spec.md`, ejecución exitosa del comando de tests `./gradlew testDebugUnitTest connectedCheck`, actualización documental y registro en `doc/`.

---

## Lecciones Aprendidas

- Definir fechas límite intermedias ayuda a acotar el alcance de cada módulo e impide la expansión no controlada de características (_scope creep_).
- Identificar y calendarizar la deuda técnica desde el principio (como el almacenamiento en caché de mapas y la optimización de consultas en SQLite) previene caídas de rendimiento en la fase final de pruebas.

---

## Próximos Pasos

1. **Iniciar la Fase 1 (Objetivo: 28 de Sep):** Crear la especificación en `spec/features/001-biometria/spec.md` y el esquema base de base de datos SQLite para cuentas y contactos.
2. Implementar los primeros tests unitarios en JUnit para la capa de persistencia y validación de seguridad.
3. Documentar los avances de la primera entrega en `doc/003-fase1-fundamentos.md`.
