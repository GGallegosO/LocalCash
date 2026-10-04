# doc/005-login-biometria-pin.md — Registro de Memoria: Setup de Feature 005 (Login Biométrico + PIN)

> **Propósito de este archivo:** Documentar la actualización del estado del proyecto y la creación
> de la estructura de la Feature 005 (`005-login-biometria-pin`), siguiendo el formato exigido
> por `.agent.md` §7.

---

## Ficha de la tarea

| Campo                         | Valor                                                                               |
| ----------------------------- | ----------------------------------------------------------------------------------- |
| **Fecha**                     | 2026-10-04                                                                          |
| **Tarea ejecutada**           | Actualización de estado del proyecto y creación de la estructura de la Feature 005  |
| **Responsable**               | Gustavo Gallegos                                                                     |
| **Duración estimada vs real** | [1 sesión] vs [1 sesión]                                                            |
| **Estado**                    | Completado                                                                          |

---

## Tarea Ejecutada (resumen narrativo)

Se actualizó la documentación del proyecto para reflejar la realidad actual: el proyecto Gradle ya existe y está inicializado con la estructura base de Android (Java). Se creó la carpeta `spec/features/005-login-biometria-pin/` desde la plantilla `_TEMPLATE_` y se redactaron los tres documentos del ciclo SDD: `spec.md` (QUÉ), `plan.md` (CÓMO) y `tasks.md` (CUÁNDO).

La Feature 005 define una pantalla de inicio de sesión con seguridad híbrida 100% offline: autenticación biométrica mediante `BiometricPrompt` (AndroidX Biometric) con fallback a PIN numérico de 6 dígitos. Incluye requisitos de accesibilidad (Light/Dark Mode, Alto Contraste, fuente OpenDyslexica) y bloqueo temporal tras intentos fallidos.

---

## Archivos Creados / Modificados

**Creados:**

- `spec/features/005-login-biometria-pin/spec.md` — Especificación de la feature con criterios de aceptación Gherkin, casos borde, impacto en data-model.md y verificación de invariantes.
- `spec/features/005-login-biometria-pin/plan.md` — Plan técnico con componentes, decisiones de arquitectura (BiometricPrompt, EncryptedSharedPreferences, PBKDF2, temas M3, fuente OpenDyslexica), riesgos y estrategia de persistencia.
- `spec/features/005-login-biometria-pin/tasks.md` — Checklist secuencial de 20 tareas organizadas en 7 fases (contratos, persistencia, lógica, UI/accesibilidad, pruebas, verificación de calidad, cierre).
- `doc/005-login-biometria-pin.md` — ESTE registro de memoria.

**Modificados:**

- `.agent.md` — Sección 3: actualizada la "Nota de estado" para reflejar que el proyecto Gradle ya existe y los comandos son ejecutables. Sección 2: actualizado `minSdk` de 23 a 28.
- `AGENTS.md` — Sección "Estado real del repositorio": actualizada para reflejar que la estructura base de Android está creada y lista para recibir la Feature 005.
- `spec/constitution/data-model.md` — Actualizadas las referencias a `minSdk` de 23 a 28 en las líneas 23, 721 y 735.

---

## Decisiones Tomadas (con justificación)

1. **Mantener `minSdk` 28 en lugar de 23.**
   _Justificación:_ El proyecto Gradle ya está inicializado con `minSdk 28` en `app/build.gradle`. Cambiarlo a 23 requeriría modificar la configuración existente y podría introducir incompatibilidades con las dependencias actuales. Se documenta esta decisión para mantener la consistencia entre la documentación y el código real.

2. **Almacenamiento de credenciales en EncryptedSharedPreferences en lugar de SQLite.**
   _Justificación:_ La Feature 005 es la primera feature real y no requiere multi-usuario. EncryptedSharedPreferences proporciona cifrado nativo mediante Android Keystore sin la complejidad de una migración de base de datos. Si en el futuro se necesita multi-usuario, se migrará a SQLite.

3. **Hash del PIN con PBKDF2 en lugar de SHA-256 simple.**
   _Justificación:_ PBKDF2 con salt es resistente a rainbow tables y es nativo en Android sin dependencias externas. SHA-256 simple sería vulnerable a ataques de diccionario.

4. **Tema Alto Contraste mediante `res/values-hc/` en lugar de overlays en código.**
   _Justificación:_ Es explícito, testeable y funciona en todas las versiones de Android. Los overlays en código son más difíciles de mantener y verificar.

5. **Fuente OpenDyslexica aplicada globalmente sobrescribiendo Typography en MaterialTheme.**
   _Justificación:_ Centraliza la aplicación de la fuente y respeta el tema activo. Si falta el archivo .ttf, se usa la fuente del sistema como fallback.

6. **Namespace actual `com.example.templocalcash` se migrará a `com.localcash` durante la implementación.**
   _Justificación:_ La estructura de paquetes declarada en `.agent.md` §4 es `com/localcash/{ui,domain,data}/`. La migración se realizará como parte de las tareas de implementación de la Feature 005.

---

## Lecciones Aprendidas

- **La documentación debe reflejar la realidad del código.** La "Nota de estado" en `.agent.md` y `AGENTS.md` estaba desactualizada desde que se inicializó el proyecto Gradle. Es importante actualizarla antes de comenzar una nueva feature para evitar confusiones.
- **Las decisiones de configuración (como `minSdk`) deben documentarse.** El cambio de `minSdk` 23 a 28 es una decisión de diseño que debe registrarse para futuras referencias.
- **El ciclo SDD (spec → plan → tasks) es esencial antes de codificar.** La redacción de los tres documentos permitió identificar casos borde, riesgos y dependencias que no serían evidentes al escribir código directamente.
- **La accesibilidad es un requisito, no un añadido.** Los requisitos de Light/Dark Mode, Alto Contraste y fuente OpenDyslexica deben estar presentes desde la especificación, no como mejoras posteriores.

---

## Próximos Pasos

1. **Implementación de la Feature 005:** Ejecutar las 20 tareas definidas en `tasks.md` en orden secuencial, comenzando por la creación de la estructura de paquetes `com/localcash/{ui,domain,data}/`.
2. **Migración de namespace:** Cambiar `com.example.templocalcash` a `com.localcash` en `app/build.gradle` y en todos los archivos Java.
3. **Dependencias:** Agregar `androidx.biometric:biometric` y `androidx.security:security-crypto` a `app/build.gradle`.
4. **Verificación:** Al finalizar la implementación, ejecutar el Definition of Done: `lint` → `testDebugUnitTest` → `connectedCheck` → `assembleDebug`.
5. **Registro de memoria de implementación:** Crear `doc/005-login-biometria-pin-implementacion.md` (o similar) con las lecciones aprendidas durante la codificación.
