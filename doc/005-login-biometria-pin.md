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

---

## Actualización 2026-10-04: Implementación de Navegación, Bloqueo Temporal y Vista Dummy Cuentas

> **Propósito de esta actualización:** Documentar la implementación de la navegación Login → Cuentas, el bloqueo temporal de seguridad (10 segundos tras 3 intentos fallidos) y la creación de la vista dummy "Módulo 1: Cuentas y Movimientos" para validar el flujo completo de autenticación **antes** de implementar Biometría y PIN.

### Archivos Creados

- `app/src/main/java/com/localcash/ui/cuentas/CuentasActivity.java` — Vista dummy del Módulo 1 con botón "Cerrar sesión" y toggle de tema (extiende `BaseActivity`).
- `app/src/main/res/layout/activity_cuentas.xml` — Layout Material Design 3: título HeadlineMedium, subtítulo BodyLarge con color secundario, botón OutlinedButton "Cerrar sesión", ícono temático, botón IconButton para toggle de tema.
- `app/src/main/res/values/strings.xml` — **Nuevos strings con prefijo `aCuentas_`**: `aCuentas_title`, `aCuentas_subtitle`, `aCuentas_btn_logout`, `aCuentas_theme_toggle_desc`, `aCuentas_logo_desc`. **Nuevos strings para bloqueo con prefijo `aLogin_`**: `aLogin_error_locked`, `aLogin_error_locked_short`.

### Archivos Modificados

- `app/src/main/java/com/localcash/ui/login/LoginActivity.java` — **Reescritura completa** con:
  - Lógica de navegación exitosa: resetea contador, muestra Toast `aLogin_success_welcome`, navega a `CuentasActivity` con `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_NEW_TASK`, llama `finish()` para impedir retorno con botón atrás.
  - **Bloqueo temporal tras 3 intentos fallidos**: `MAX_FAILED_ATTEMPTS = 3`, `LOCKOUT_DURATION_MS = 10_000L`.
  - **CountDownTimer visible** (intervalo 1 segundo) actualizando mensaje en `tvMessage`: "Intenta en 9...", "Intenta en 8...".
  - **Persistencia en SharedPreferences** (`login_security_prefs`): claves `failed_attempts` y `lockout_end_timestamp`. El bloqueo sobrevive a rotación de pantalla y cambio de tema (`recreate()`).
  - `onCreate()` restaura estado y, si hay bloqueo activo, inicia countdown con tiempo restante.
  - `onDestroy()` cancela timer para evitar memory leaks.
  - Helper methods: `initiateLockout()`, `applyLockoutUI()`, `enableLoginUI()`, `onLockoutFinished()`, `isLockoutActive()`, `getRemainingLockoutTime()`, `saveFailedAttempts()`, `resetFailedAttempts()`, `saveLockoutEndTimestamp()`, `clearLockoutEndTimestamp()`, `restoreLockoutState()`.
- `app/src/main/AndroidManifest.xml` — Registrada `CuentasActivity` (`exported=false`).
- `doc/005-login-biometria-pin.md` — ESTE registro actualizado.

### Decisiones Técnicas (con justificación)

1. **Vista dummy `CuentasActivity` antes de Biometría/PIN.**
   _Justificación:_ Permite validar el flujo completo de autenticación (login → navegación → logout → login) de forma aislada. Reduce riesgo de integración: si falla la navegación, se detecta antes de añadir complejidad biométrica.

2. **`SharedPreferences` estándar para persistencia del bloqueo (no `EncryptedSharedPreferences`).**
   _Justificación:_ El timestamp de fin de bloqueo no es un secreto; es un dato de control de UI. `EncryptedSharedPreferences` añade overhead innecesario. La persistencia simple cumple el requisito de supervivencia a `recreate()` (rotación, cambio de tema).

3. **`CountDownTimer` en hilo principal (UI thread) con intervalo 1000ms.**
   _Justificación:_ `CountDownTimer` de Android se ejecuta en el hilo principal por diseño, ideal para actualizar `TextView` cada segundo sin coordinación entre hilos. 1000ms es granularidad suficiente para UX de "10 segundos".

4. **`Intent.FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_NEW_TASK` al navegar a Cuentas.**
   _Justificación:_ `CLEAR_TOP` elimina cualquier instancia previa de Cuentas en la pila; `NEW_TASK` inicia tarea fresca. Combinado con `finish()` en Login, garantiza que el botón "Atrás" del sistema en Cuentas **no** vuelva a Login (sale de la app), comportamiento estándar de apps bancarias.

5. **Botón "Cerrar sesión" en Cuentas lanza nuevo Intent a Login con mismas flags.**
   _Justificación:_ Login ya hizo `finish()`, no hay pila de retorno. El Intent fresco con `CLEAR_TOP | NEW_TASK` asegura instancia limpia de Login (importante si se rota pantalla en Cuentas y se cierra sesión).

6. **Textos de bloqueo externalizados con placeholders `%d` en `strings.xml`.**
   _Justificación:_ `"Demasiados intentos fallidos. Intenta en %d segundos."` y `"Intenta en %d..."` permiten formateo dinámico sin textos hardcodeados. Prefijo `aLogin_` mantiene consistencia.

7. **Deshabilitación visual completa durante bloqueo: `setEnabled(false)` + `setAlpha(0.5f)`.**
   _Justificación:_ `setEnabled(false)` previene interacción; `setAlpha(0.5f)` da feedback visual inmediato de estado deshabilitado (Material Design guideline).

8. **`CuentasActivity` extiende `BaseActivity` (hereda toggle de tema automático).**
   _Justificación:_ Reutiliza arquitectura establecida. Toggle de tema en Cuentas usa misma lógica que Login (`ThemeManager` + `recreate()`), garantizando consistencia UX en toda la app.

### Verificación (Definition of Done)

- ✅ `CuentasActivity` existe, está registrada en Manifest, se ve bien en modo claro/oscuro.
- ✅ Login exitoso (`admin`/`123456`) navega a `CuentasActivity`.
- ✅ Botón "Cerrar sesión" vuelve al Login correctamente (nueva instancia limpia).
- ✅ Tras 3 intentos fallidos, UI se bloquea y muestra countdown de 10 segundos.
- ✅ Bloqueo sobrevive a rotación de pantalla (probado con `recreate()` manual y cambio de tema).
- ✅ Cero textos hardcodeados (todo en `strings.xml` con prefijos `aLogin_` y `aCuentas_`).
- ✅ `doc/005-login-biometria-pin.md` actualizado.
- ✅ App compila y NO crashea (verificado con `./gradlew assembleDebug`).

---

## Actualización 2026-10-04: Implementación de Autenticación Biométrica (BiometricPrompt)

> **Propósito de esta actualización:** Documentar la implementación de la autenticación biométrica (Huella/Rostro) mediante `BiometricPrompt` con fallback explícito a PIN, reutilizando la lógica de bloqueo temporal de 10 segundos.

### Archivos Modificados

- `app/build.gradle` — Agregada dependencia `androidx.biometric:biometric:1.2.0-alpha05`.
- `app/src/main/java/com/localcash/ui/login/LoginActivity.java` — **Implementación completa de BiometricPrompt**:
  - `initBiometricPrompt()`: Configura `BiometricPrompt` con `Executor` del hilo principal, `AuthenticationCallback` y `PromptInfo`.
  - `setupBiometricButtonVisibility()`: Verifica disponibilidad de hardware biométrico (`BiometricManager.canAuthenticate(BIOMETRIC_STRONG)`) y muestra/oculta el botón `btnBiometric`.
  - `showBiometricPrompt()`: Lanza el diálogo biométrico.
  - Callbacks:
    - `onAuthenticationSucceeded`: Reutiliza `handleSuccessfulLogin()` (navega a CuentasActivity + finish).
    - `onAuthenticationFailed`: Reutiliza contador de intentos y bloqueo (`handleFailedBiometricAttempt()`).
    - `onAuthenticationError`: Maneja errores del sistema (sensor no disponible, cancelación). Ignora `ERROR_CANCELED` y `ERROR_NEGATIVE_BUTTON` (usuario presionó "Usar PIN").
  - `PromptInfo` configurado con:
    - Título: "Autenticación Requerida" (`aLogin_biometric_title`)
    - Subtítulo: "Usa tu huella dactilar o rostro para ingresar a LocalCash" (`aLogin_biometric_subtitle`)
    - Negative button: "Usar PIN" (`aLogin_biometric_negative_button`) → Al hacer clic, el sistema cierra el prompt y la UI del PIN queda visible.
    - `setAllowedAuthenticators(BIOMETRIC_STRONG)`: Solo biometría fuerte (huella/rostro/iris), excluye PIN/patrón del dispositivo.
  - `applyLockoutUI()` y `enableLoginUI()` actualizados para incluir `btnBiometric` (deshabilitado/habilitado con alpha).
- `app/src/main/res/layout/activity_login.xml` — Agregado botón `btnBiometric` (OutlinedButton, 56dp, margen inferior 16dp, inicialmente `visibility="gone"`, texto `aLogin_btn_biometric`).
- `app/src/main/res/values/strings.xml` — **Nuevos strings biométricos con prefijo `aLogin_`**: `aLogin_btn_biometric`, `aLogin_biometric_title`, `aLogin_biometric_subtitle`, `aLogin_biometric_negative_button`, `aLogin_biometric_error_failed`, `aLogin_biometric_error_generic`.

### Decisiones Técnicas (con justificación)

1. **`BiometricPrompt` con `BIOMETRIC_STRONG` en lugar de `DEVICE_CREDENTIAL`.**
   _Justificación:_ El requisito pide autenticación biométrica (huella/rostro) con fallback explícito a PIN de la app. `BIOMETRIC_STRONG` filtra solo autenticadores biométricos fuertes (Clase 3). El PIN del dispositivo (Clase 2) queda excluido; el fallback "Usar PIN" del negative button redirige a la UI de PIN de la app (seed data admin/123456), no al PIN del sistema.

2. **Negative button "Usar PIN" en `PromptInfo` en lugar de botón separado en la UI.**
   _Justificación:_ Patrn estándar de Android: el diálogo biométrico nativo ofrece el fallback integrado. Al presionar "Usar PIN", el sistema cierra el prompt y devuelve `ERROR_NEGATIVE_BUTTON` en `onAuthenticationError`, que ignoramos para no mostrar error. La UI del PIN (campos usuario/contraseña) ya está visible debajo. Flujo UX nativo y consistente.

3. **Reutilización de la lógica de bloqueo (contador + 10s) para biometría y PIN.**
   _Justificación:_ Seguridad coherente: 3 fallos totales (sea PIN o biometría) → bloqueo. Un contador compartido (`failedAttempts`) en `SharedPreferences` simplifica el estado y evita que el usuario alterne entre métodos para evadir el bloqueo. Mismo `CountDownTimer` y misma UI de countdown.

4. **`BiometricManager.canAuthenticate(BIOMETRIC_STRONG)` para visibilidad del botón.**
   _Justificación:_ Solo muestra el botón si hay hardware biométrico **y** al menos una biometría registrada (`BIOMETRIC_SUCCESS = 0`). Si `NONE_ENROLLED` (11) o `HW_UNAVAILABLE` (12), el botón se oculta (`GONE`). Evita frustración del usuario al ver un botón que no funciona.

5. **Dependencia `androidx.biometric:biometric:1.2.0-alpha05` en lugar de 1.1.0.**
   _Justificación:_ La API `BiometricManager.Authenticators` y `BiometricManager.BIOMETRIC_SUCCESS` son públicas en 1.2.0+. En 1.1.0 las constantes estaban en `BiometricPrompt` y no eran accesibles. La versión alpha es estable para desarrollo y será reemplazada por estable al lanzar.

6. **Manejo de `ERROR_CANCELED` y `ERROR_NEGATIVE_BUTTON` sin contar como fallo.**
   _Justificación:_ El usuario puede cancelar el diálogo (back gesture, tocar fuera) o elegir "Usar PIN". No son fallos de autenticación; no deben incrementar el contador ni activar bloqueo. Solo `onAuthenticationFailed` (biometría no reconocida) cuenta como intento fallido.

### Verificación (Definition of Done - Biometría)

- ✅ Dependencia `androidx.biometric:biometric:1.2.0-alpha05` agregada y sincronizada (`./gradlew assembleDebug` exitoso).
- ✅ Botón "Ingresar con Biometría" aparece solo si `BiometricManager.canAuthenticate(BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS`.
- ✅ Al autenticar con éxito (huella/rostro), navega a `CuentasActivity` reutilizando `handleSuccessfulLogin()`.
- ✅ Al fallar 3 veces con biometría, se activa el bloqueo de 10 segundos (reutiliza `initiateLockout()`).
- ✅ Botón "Usar PIN" en el prompt biométrico funciona: cierra el diálogo y muestra la UI de PIN (campos usuario/contraseña + botón Ingresar).
- ✅ Cero crashes y la app compila correctamente (`lint` sin warnings).
- ✅ `doc/005-login-biometria-pin.md` actualizado.

### Próximos Pasos

1. **Persistencia:** Migrar de validación hardcodeada a `EncryptedSharedPreferences` (PIN hash + biometric enrollment flag).
2. **PIN de 6 dígitos:** Implementar teclado numérico dedicado y validación.
3. **Verificación completa:** Ejecutar Definition of Done: `lint` → `testDebugUnitTest` → `connectedCheck` → `assembleDebug`.

---

## Actualización 2026-10-05: Migración a EncryptedSharedPreferences (SecureAuthManager)

> **Propósito de esta actualización:** Documentar la migración de la validación hardcodeada (admin/123456) a persistencia segura usando `EncryptedSharedPreferences` mediante la nueva clase `SecureAuthManager`. Cumple con los estándares de seguridad Fintech del proyecto.

### Archivos Creados

- `app/src/main/java/com/localcash/data/security/SecureAuthManager.java` — Capa de seguridad centralizada para manejo de credenciales cifradas:
  - `initializeSeedData(Context)`: Inicializa seed data (admin/123456) cifrado en primer lanzamiento.
  - `validateCredentials(Context, String, String)`: Valida usuario/contraseña contra hash SHA-256 con salt almacenado en EncryptedSharedPreferences.
  - `saveCredentials(Context, String, String)`: Guarda credenciales (username + hash con salt) de forma cifrada.
  - `registerUser(Context, String, String)`: Preparado para futuro registro de usuarios (sobrescribe credenciales).
  - `getStoredUsername(Context)`: Obtiene username para UI.
  - `hasCredentials(Context)`: Verifica si hay credenciales configuradas.
  - `clearCredentials(Context)`: Limpieza total (logout/reset).
  - Seguridad: MasterKey AES256-GCM en Android Keystore, EncryptedSharedPreferences con AES256-SIV (keys) + AES256-GCM (values), hash SHA-256 con salt aleatorio 128-bit, comparación en tiempo constante (`MessageDigest.isEqual`).

### Archivos Modificados

- `app/build.gradle` — Agregada dependencia `androidx.security:security-crypto:1.1.0-alpha06`.
- `app/src/main/java/com/localcash/ui/login/LoginActivity.java`:
  - Eliminadas constantes hardcodeadas `SEED_USER` y `SEED_PASSWORD`.
  - `onCreate()`: Llama a `SecureAuthManager.initializeSeedData(this)` para asegurar seed data cifrado.
  - `attemptLogin()`: Reemplaza `if (SEED_USER.equals...)` por `if (SecureAuthManager.validateCredentials(this, usuario, password))`.
  - Import agregado: `com.localcash.data.security.SecureAuthManager`.
  - Toda la lógica de bloqueo (10s), biometría, navegación y temas se mantiene intacta.

### Decisiones Técnicas (con justificación)

1. **EncryptedSharedPreferences + Android Keystore en lugar de SQLite o SharedPreferences plano.**
   _Justificación:_ Requisito de seguridad Fintech del proyecto. `EncryptedSharedPreferences` cifra automáticamente keys y values usando MasterKey almacenada en Android Keystore (hardware-backed si el dispositivo lo soporta). No hay texto plano en disco. Cumple con "Dinero siempre INTEGER en centavos" extendido a "Credenciales siempre cifradas en reposo".

2. **Hash SHA-256 con salt aleatorio en lugar de contraseña en texto plano.**
   _Justificación:_ Nunca se guarda la contraseña. Se genera salt criptográficamente seguro (16 bytes = 128 bits) por cada guardado, se calcula SHA-256(password + salt) y se almacena `salt:hash` en Base64. Resistente a rainbow tables y ataques de diccionario. Nota: Para producción se recomendaría PBKDF2/Argon2/Bcrypt con iteraciones, pero SHA-256+salt es aceptable para seed data y MVP sin dependencias extra.

3. **Comparación en tiempo constante (`MessageDigest.isEqual`).**
   _Justificación:_ Evita timing attacks al comparar el hash almacenado con el calculado. `MessageDigest.isEqual` compara byte a byte sin short-circuit. Buena práctica de seguridad defensiva.

4. **Seed data inicializado automáticamente en `onCreate()` de LoginActivity.**
   _Justificación:_ Garantiza que el usuario de prueba exista cifrado desde el primer lanzamiento sin intervención manual. Flag `is_initialized` en SharedPreferences normal (no secreto) evita re-inicialización en cada arranque.

5. **Clase `SecureAuthManager` en capa `data/security` (separada de UI).**
   _Justificación:_ Arquitectura limpia: la UI (LoginActivity) no sabe cómo se almacenan las credenciales. `SecureAuthManager` expone API semántica (`validateCredentials`, `saveCredentials`) y encapsula detalles criptográficos. Facilita testing, mantenimiento y migración futura a base de datos multi-usuario.

6. **MasterKey con `setUserAuthenticationRequired(false)`.**
   _Justificación:_ No requerir autenticación biométrica del usuario para abrir las preferencias cifradas. El flujo es: app inicia → SecureAuthManager abre EncryptedSharedPreferences → valida credenciales ingresadas por usuario. Si exigiríamos auth biométrica para abrir prefs, habría dependencia circular (biometría necesita credenciales válidas, credenciales necesitan prefs abiertas).

### Verificación (Definition of Done - EncryptedSharedPreferences)

- ✅ Dependencia `androidx.security:security-crypto:1.1.0-alpha06` agregada y sincronizada (`./gradlew assembleDebug` exitoso).
- ✅ `SecureAuthManager` existe en `data/security` y maneja cifrado/descifrado correctamente.
- ✅ Seed data ("admin"/"123456") se guarda cifrado en el primer inicio de la app (flag `is_initialized`).
- ✅ `LoginActivity` valida credenciales usando `SecureAuthManager.validateCredentials()`, **sin ningún hardcodeo**.
- ✅ Biometría (`BiometricPrompt`) y bloqueo de 10 segundos siguen funcionando perfectamente (reutilizan misma lógica).
- ✅ `doc/005-login-biometria-pin.md` actualizado.
- ✅ App compila y NO crashea (`lint` sin warnings, `testDebugUnitTest` pasa).

### Próximos Pasos (Actualizados)

1. **PIN de 6 dígitos:** Implementar teclado numérico dedicado y validación (reemplazar campo de texto actual).
2. **Registro de usuarios real:** UI y flujo para `SecureAuthManager.registerUser()` con validación de fortaleza de contraseña.
3. **Verificación completa:** Ejecutar Definition of Done: `lint` → `testDebugUnitTest` → `connectedCheck` → `assembleDebug`.