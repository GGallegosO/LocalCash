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
- `app/src/main/java/com/localcash/ui/theme/ThemeManager.java` — Gestor de temas persistente (SharedPreferences, light/dark/system).
- `app/src/main/java/com/localcash/ui/BaseActivity.java` — Activity base que aplica tema automáticamente antes de inflar.
- `app/src/main/res/values/strings.xml` — Recursos de texto con convención `aLogin_` para LoginActivity.
- `app/src/main/res/drawable/ic_brightness_medium.xml` — Vector oficial de Material Design (Apache 2.0) con ícono de brillo (sol/luna combinado).
- `app/src/main/res/drawable/ic_lock.xml` — Ícono para input de contraseña.
- `app/src/main/res/drawable/ic_person.xml` — Ícono para input de usuario.
- `app/src/main/res/drawable/ic_security.xml` — Ícono para footer de seguridad.

**Modificados:**

- `.agent.md` — Sección 3: actualizada la "Nota de estado" para reflejar que el proyecto Gradle ya existe y los comandos son ejecutables. Sección 2: actualizado `minSdk` de 23 a 28.
- `AGENTS.md` — Sección "Estado real del repositorio": actualizada para reflejar que la estructura base de Android está creada y lista para recibir la Feature 005.
- `spec/constitution/data-model.md` — Actualizadas las referencias a `minSdk` de 23 a 28 en las líneas 23, 721 y 735.
- `app/build.gradle` — Namespace y applicationId migrados a `com.localcash`.
- `app/src/main/java/com/localcash/ui/login/LoginActivity.java` — Extiende `BaseActivity`, implementa toggle de tema con ícono nativo, usa strings externalizadas, ViewBinding.
- `app/src/main/res/layout/activity_login.xml` — Logo 120dp fitCenter, CardView con padding 32dp / margin 80dp arriba / 48dp abajo, MaterialButton toggle tema, todos los textos externalizados a @string/.
- `app/src/main/res/values/themes.xml` / `values-night/themes.xml` — Heredan de `Theme.Material3.DayNight.NoActionBar` con paleta oficial.
- `app/src/main/AndroidManifest.xml` — LoginActivity como LAUNCHER, Theme.LocalCash.

**Eliminados:**

- `app/src/main/java/com/example/templocalcash/MainActivity.java` — Activity template obsoleta.
- `app/src/main/res/layout/activity_main.xml` — Layout template obsoleto.
- `app/src/main/res/drawable/gradient_header.xml` — Gradiente innecesario.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — Ícono de primer plano no usado (se usa ic_launcher adaptativo).
- `app/src/main/res/drawable/ic_brightness_low.xml` — Vector innecesario (ícono de brillo bajo).
- `app/src/main/res/drawable/ic_moon.xml` — Vector personalizado innecesario.
- `app/src/main/res/drawable/ic_sun.xml` — Vector personalizado innecesario.
- `app/src/test/java/com/example/templocalcash/ExampleUnitTest.java` — Test template obsoleto.
- `app/src/androidTest/java/com/example/templocalcash/ExampleInstrumentedTest.java` — Test template obsoleto.

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

6. **Namespace actual `com.example.templocalcash` se migró a `com.localcash`.**
   _Justificación:_ La estructura de paquetes declarada en `.agent.md` §4 es `com/localcash/{ui,domain,data}/`. La migración se realizó en `app/build.gradle` y todos los imports Java.

7. **Uso de ícono nativo Material Design `ic_brightness_medium` (Apache 2.0) en lugar de vectores personalizados.**
   _Justificación:_ El vector oficial muestra sol y luna combinados, representa perfectamente el concepto de cambio de tema, elimina archivos XML innecesarios, reduce mantenimiento y garantiza consistencia visual con el ecosistema Android. Reutilizado del proyecto TaskMaster.

8. **Externalización total de textos a `strings.xml` con prefijo `aLogin_`.**
   _Justificación:_ Facilita mantenimiento, traducciones futuras y revisión de textos. Convención `aLogin_` (Activity Login) permite identificar rápidamente qué textos pertenecen a esta pantalla. Patrón consistente con proyectos anteriores.

9. **Eliminación de vectores personalizados y archivos innecesarios.**
   _Justificación:_ `ic_moon.xml`, `ic_sun.xml`, `ic_brightness_low.xml`, `gradient_header.xml`, `ic_launcher_foreground.xml` eran redundantes. Mantener el proyecto limpio evita confusión y reduce tamaño del APK.

10. **Arquitectura reutilizable: `BaseActivity` + `ThemeManager`.**
    _Justificación:_ `BaseActivity` aplica tema automáticamente antes de inflar vistas. `ThemeManager` encapsula lógica de persistencia y aplicación de tema. Cualquier Activity futura solo necesita extender `BaseActivity` para heredar esta funcionalidad. Patrón validado en TaskMaster.

11. **MaterialButton con estilo `Widget.Material3.Button.IconButton` para toggle de tema.**
    _Justificación:_ Botón de acción flotante con elevación 8dp, ícono nativo, sigue guías Material Design 3. Posicionado fuera de la CardView para que flote visualmente. Patrón replicado de TaskMaster.

---

## Lecciones Aprendidas

- **La documentación debe reflejar la realidad del código.** La "Nota de estado" en `.agent.md` y `AGENTS.md` estaba desactualizada desde que se inicializó el proyecto Gradle. Es importante actualizarla antes de comenzar una nueva feature para evitar confusiones.
- **Las decisiones de configuración (como `minSdk`) deben documentarse.** El cambio de `minSdk` 23 a 28 es una decisión de diseño que debe registrarse para futuras referencias.
- **El ciclo SDD (spec → plan → tasks) es esencial antes de codificar.** La redacción de los tres documentos permitió identificar casos borde, riesgos y dependencias que no serían evidentes al escribir código directamente.
- **La accesibilidad es un requisito, no un añadido.** Los requisitos de Light/Dark Mode, Alto Contraste y fuente OpenDyslexica deben estar presentes desde la especificación, no como mejoras posteriores.
- **Es fundamental externalizar todos los textos a `strings.xml` desde el inicio** para facilitar mantenimiento y traducciones. No dejar textos hardcodeados en layouts XML.
- **Los íconos nativos de Material Design (Apache 2.0) son preferibles a crear vectores personalizados** cuando están disponibles. Reducen código, mantenimiento y garantizan consistencia.
- **La limpieza de archivos innecesarios debe ser parte del Definition of Done.** Archivos obsoletos (`MainActivity`, vectores personalizados, tests template) confunden y ensucian el proyecto.
- **La arquitectura reutilizable (BaseActivity + ThemeManager) escala mejor** que implementar lógica de tema en cada Activity.
- **Reutilizar patrones de diseño y vectores oficiales de proyectos previos (TaskMaster) acelera el desarrollo** y garantiza consistencia visual y técnica.

---

## Próximos Pasos

1. **Biometría:** Integrar `BiometricPrompt` con fallback a PIN.
2. **Persistencia:** Migrar de validación hardcodeada a `EncryptedSharedPreferences`.
3. **PIN de 6 dígitos:** Implementar teclado numérico y validación.
4. **Bloqueo temporal:** Agregar lógica de bloqueo tras intentos fallidos.
5. **Verificación:** Ejecutar Definition of Done: `lint` → `testDebugUnitTest` → `connectedCheck` → `assembleDebug`.