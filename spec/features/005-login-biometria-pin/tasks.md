# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** 005-login-biometria-pin | **Derivado de:** `spec.md` + `plan.md`
>
> **Propósito de este documento:** Descomponer la implementación en pasos ejecutables, ordenados y
> verificables. Cada casilla marcada `[x]` debe corresponder a un cambio real comprobable (tests
> verdes, archivo creado, comando ejecutado). Marcar sin verificar está prohibido.

---

## Fase 1 — Contratos e Interfaces

- [ ] **T1. Definir contrato de autenticación en `domain/`.**
      Crear interfaces `AuthUseCase` y `PasswordHasher` con métodos:
      - `authenticateWithBiometric(Callback callback)`
      - `authenticateWithPin(String pin): AuthResult`
      - `isBiometryAvailable(): boolean`
      - `isAccountLocked(): boolean`
      *Criterio de salida:* Interfaces compilables; documentación de errores.
      Responsable: GG | Estimación: 2h

## Fase 2 — Persistencia de Credenciales

- [ ] **T2. Implementar `CredentialsStore` con EncryptedSharedPreferences.**
      Crear `data/auth/CredentialsStore.java` con:
      - Inicialización de EncryptedSharedPreferences
      - Métodos de guardar/verificar PIN
      - Conteo de intentos fallidos y bloqueo temporal
      *Criterio de salida:* Clase compilable; tests unitarios en rojo.
      Responsable: GG | Estimación: 3h

- [ ] **T3. Crear seed data (usuario admin, PIN 123456).**
      Insertar credenciales hasheadas en EncryptedSharedPreferences al primer arranque.
      *Criterio de salida:* Al ejecutar la app por primera vez, el PIN "123456" funciona.
      Responsable: GG | Estimación: 1h

## Fase 3 — Lógica de Autenticación

- [ ] **T4. Implementar `PasswordHasher` con PBKDF2.**
      Crear `domain/auth/PasswordHasher.java` con:
      - `hash(String pin, byte[]): String`
      - `verify(String pin, String hash, byte[]): boolean`
      *Criterio de salida:* Tests unitarios en verde; verificación de hash.
      Responsable: GG | Estimación: 2h

- [ ] **T5. Implementar `AuthUseCase`.**
      Crear `domain/auth/AuthUseCase.java` con:
      - Orquestación de BiometricPrompt y fallback a PIN
      - Manejo de bloqueo temporal
      - Comunicación con CredentialsStore
      *Criterio de salida:* Tests unitarios en verde con mocks.
      Responsable: GG | Estimación: 3h

## Fase 4 — UI y Accesibilidad

- [ ] **T6. Configurar Material Design 3 (Material You).**
      Modificar `app/build.gradle` para incluir Material Components.
      Update `res/values/themes.xml` con `Theme.Material3.*`.
      *Criterio de salida:* App compila con M3.
      Responsable: GG | Estimación: 1h

- [ ] **T7. Configurar temas Claro/Oscuro.**
      Crear `res/values-night/themes.xml` con variante oscura.
      Verificar que `DayNight` está activado.
      *Criterio de salida:* Al cambiar el tema del sistema, la app responde.
      Responsable: GG | Estimación: 1h

- [ ] **T8. Crear tema Alto Contraste.**
      Crear `res/values-hc/themes.xml` con colores de alto contraste (WCAG AAA).
      Verificar ratios de contraste.
      *Criterio de salida:* Tema aplica correctamente en modo alto contraste.
      Responsable: GG | Estimación: 2h

- [ ] **T9. Importar y configurar fuente OpenDyslexic.**
      Descargar `OpenDyslexic-Regular.ttf` y colocar en `res/font/`.
      Crear `res/font/font_family.xml` que referencie la fuente.
      Aplicar globalmente sobrescribiendo Typography en el tema base.
      *Criterio de salida:* La fuente se aplica en toda la app.
      Responsable: GG | Estimación: 2h

- [ ] **T10. Crear `LoginActivity` y `activity_login.xml`.**
      Implementar UI con ViewBinding:
      - MaterialCardView como contenedor
      - TextInputLayout para PIN
      - MaterialButton para "Usar biometría"
      - TextView para mensajes de error
      *Criterio de salida:* Layout renderiza correctamente; ViewBinding funciona.
      Responsable: GG | Estimación: 3h

- [ ] **T11. Implementar flujo de autenticación en `LoginActivity`.**
      Conectar UI con AuthUseCase:
      - Mostrar BiometricPrompt si está disponible
      - Fallback a PIN si falla o no está disponible
      - Manejo de bloqueo temporal con mensaje
      *Criterio de salida:* Flujo completo funciona manualmente.
      Responsable: GG | Estimación: 3h

## Fase 5 — Pruebas

- [ ] **T12. Tests unitarios de `AuthUseCase`.**
      Cubrir: PIN correcto, PIN incorrecto, bloqueo temporal, biometría no disponible.
      *Criterio de salida:* Cobertura ≥ 70%; tests en verde.
      Responsable: GG | Estimación: 3h

- [ ] **T13. Tests unitarios de `PasswordHasher`.**
      Verificar: hash único por salt, verificación correcta, rechazo de PIN incorrecto.
      *Criterio de salida:* Tests en verde.
      Responsable: GG | Estimación: 2h

- [ ] **T14. Tests de UI con Robolectric.**
      Verificar: aplicación de temas (claro/oscuro/alto contraste), 
      fuente OpenDyslexic, fallback a PIN.
      *Criterio de salida:* Tests en verde con `./gradlew testDebugUnitTest`.
      Responsable: GG | Estimación: 4h

- [ ] **T15. Tests instrumentados con Espresso.**
      Verificar flujo completo en dispositivo/emulador.
      *Criterio de salida:* Tests en verde con `./gradlew connectedCheck`.
      Responsable: GG | Estimación: 3h

## Fase 6 — Verificación de Cobertura y Calidad

- [ ] **T16. Verificar cobertura ≥ 70%.**
      Ejecutar `./gradlew testDebugUnitTest` con reporte de cobertura.
      Verificar que dominio y data están cubiertos.
      *Criterio de salida:* Reporte muestra ≥ 70% en clases de autenticación.
      Responsable: GG | Estimación: 1h

- [ ] **T17. Lint sin advertencias.**
      Ejecutar `./gradlew lint`.
      Corregir cualquier advertencia.
      *Criterio de salida:* Build exitoso sin advertencias.
      Responsable: GG | Estimación: 1h

- [ ] **T18. Build válido.**
      Ejecutar `./gradlew assembleDebug`.
      Verificar que el APK se genera.
      *Criterio de salida:* APK en `app/build/outputs/apk/debug/`.
      Responsable: GG | Estimación: 1h

## Fase 7 — Cierre y Trazabilidad

- [ ] **T19. Actualizar `AndroidManifest.xml`.**
      Cambiar tema base a M3, agregar permisos de biometría.
      *Criterio de salida:* Manifest correcto.
      Responsable: GG | Estimación: 30min

- [ ] **T20. Crear registro de memoria `doc/005-login-biometria-pin.md`.**
      Crear registro de memoria con:
      - Archivos creados/modificados
      - Decisiones tomadas
      - Lecciones aprendidas
      - Próximos pasos
      *Criterio de salida:* Registro completo.
      Responsable: GG | Estimación: 1h

---

## Verificación final (Definition of Done)

- [ ] Lint sin advertencias: `./gradlew lint`
- [ ] Tests unitarios en verde: `./gradlew testDebugUnitTest`
- [ ] Tests de integración y UI en verde: `./gradlew connectedCheck`
- [ ] Build válido: `./gradlew assembleDebug`
- [ ] Registro de memoria creado: `doc/005-login-biometria-pin.md`
- [ ] Cobertura ≥ 70% verificada
- [ ] Commits con formato de `.agent.md` §5.2, uno por unidad lógica

**Notas de ejecución:**
- La feature 005 es la primera feature real del proyecto.
- Se debe crear la estructura de paquetes `com/localcash/{ui,domain,data}/`.
- El namespace actual es `com.example.templocalcash`; se debe cambiar a `com.localcash`.
