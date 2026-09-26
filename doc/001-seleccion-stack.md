# doc/001-seleccion-stack.md — Registro de Memoria: Constitución y Selección del Stack Tecnológico

> **Propósito de este archivo:** Es el EJEMPLO DE REFERENCIA del formato de memoria que exige
> `.agent.md` §7. Todas las tareas futuras deben crear un registro análogo (`001-...`, `002-...`).
> El contenido siguiente es ficticio pero realista: muestra el nivel de detalle esperado.
> Al iniciar tu proyecto real, conserva este archivo como modelo y numera tus tareas desde 001.

---

## Ficha de la tarea

| Campo                         | Valor                                                                         |
| ----------------------------- | ----------------------------------------------------------------------------- |
| **Fecha**                     | 2026-09-25                                                                    |
| **Tarea ejecutada**           | IDefinición y Selección Oficial de la Constitución Tecnológica (_Tech Stack_) |
| **Responsable**               | GG                                                                            |
| **Duración estimada vs real** | [1 sesión] vs [1 sesión]                                                      |
| **Estado**                    | Completado                                                                    |

---

## Tarea Ejecutada (resumen narrativo)

Se evaluó y oficializó la constitución tecnológica del proyecto en el archivo **tech-stack.md**. En cumplimiento con la regla de oro de agnosticismo y el pilar rector de funcionamiento **100% offline y de privacidad absoluta**, se seleccionaron las herramientas necesarias para cubrir el desarrollo nativo en Android (Java), la persistencia relacional en SQLite, el renderizado de mapas sin servidor (`osmdroid`), la seguridad biométrica (`BiometricPrompt`) y el pipeline de pruebas y calidad.

---

## Archivos Creados / Modificados

**Creados:**

- `spec/constitution/tech-stack.md` — Registro oficial del stack tecnológico con la evaluación de alternativas, justificaciones, herramientas de testing, CI/CD y despliegue.
- `doc/001-seleccion-stack.md` — Registro formal de memoria (este archivo) para respaldar la toma de decisiones del stack.

---

## Decisiones Tomadas (con justificación)

1. **Lenguaje Principal: Java 8+ / JDK 11** _Justificación:_ Requisito obligatorio del proyecto para desarrollo nativo en Android Studio, asegurando integración directa con las APIs del sistema sin capas de abstracción adicionales.
2. **Motor de Persistencia: SQLite (a través de Room /** **SQLiteOpenHelper** **)** _Justificación:_ Garantiza el almacenamiento de datos relacionales (cuentas, transacciones, deudas, contactos) de forma local, liviana y totalmente autónoma sin depender de servidores en la nube.
3. **Renderizado de Mapas:** **osmdroid** **(OpenStreetMap Android)** _Justificación:_ Permite descargar y almacenar en caché teselas de mapas para su visualización **100% offline**, evitando la dependencia de claves de API o servicios de pago como Google Maps o Mapbox.
4. **Seguridad y Autenticación: AndroidX Biometric (** **BiometricPrompt** **)** _Justificación:_ API estándar de Android para validación de huellas dactilares con retrocompatibilidad y gestión de credenciales protegida por el hardware del dispositivo (Android Keystore).
5. **Herramientas de Testing y Calidad: JUnit 4, Mockito, Robolectric, Espresso y Android Lint** _Justificación:_ Cobertura completa de pruebas unitarias para la lógica de negocio (cálculo de saldos y deudas), pruebas de integración para la base de datos SQLite y pruebas E2E para los flujos de interfaz de usuario.
6. **Despliegue e Infraestructura: Distribución Local / Google Play Console (AAB/APK) + GitHub Actions** _Justificación:_ Mantiene el principio de "cero backend remoto" y automatiza el análisis estático y generación de artefactos firmados sin exponer datos del usuario.

---

## Lecciones Aprendidas

- Evaluar al menos dos alternativas por categoría (como comparar `osmdroid` frente a Google Maps SDK o Room frente a Realm) evita sesgos y fundamenta las elecciones en restricciones reales de arquitectura (_offline-first_ y privacidad).
- Formalizar el stack antes de escribir código evita la introducción de librerías no autorizadas que puedan comprometer la seguridad o la autonomía del sistema.

---

## Próximos Pasos

1. Vincular el stack oficializado en `tech-stack.md` con las especificaciones de características de la Fase 1.
2. Proceder con la creación del archivo de **Arquitectura del Sistema (** **architecture.md** **)** o iniciar el diseño de la especificación `spec/features/001-biometria/spec.md`.
