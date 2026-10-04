# plan.md — Plan Técnico de Implementación

> **Identificación:** 005-login-biometria-pin | **Fecha:** 2026-10-04 | **Autor:** Gustavo Gallegos
>
> **Propósito de este documento:** Definir CÓMO se implementará la feature descrita en `spec.md`:
> componentes, decisiones técnicas, riesgos y dependencias. Es el puente entre el requisito y las
> tareas ejecutables (`tasks.md`).

---

## 1. Componentes Involucrados

| Archivo / Módulo | Rol en la feature | Acción | Complejidad |
|------------------|-------------------|--------|-------------|
| `app/src/main/java/com/localcash/ui/login/LoginActivity.java` | UI de autenticación | Crear | Media |
| `app/src/main/res/layout/activity_login.xml` | Layout de login | Crear | Media |
| `app/src/main/res/values/themes.xml` | Tema base M3 | Modificar | Baja |
| `app/src/main/res/values-night/themes.xml` | Tema oscuro M3 | Crear | Baja |
| `app/src/main/res/values-hc/themes.xml` | Tema alto contraste | Crear | Baja |
| `app/src/main/res/font/opendyslexic.ttf` | Fuente para dislexía | Crear | Baja |
| `app/src/main/java/com/localcash/domain/auth/AuthUseCase.java` | Lógica de autenticación | Crear | Alta |
| `app/src/main/java/com/localcash/domain/auth/PasswordHasher.java` | Hash de PIN | Crear | Media |
| `app/src/main/java/com/localcash/data/auth/CredentialsStore.java` | Persistencia cifrada | Crear | Media |
| `app/src/main/java/com/localcash/data/auth/AuthSeed.java` | Datos semilla (admin/123456) | Crear | Baja |
| `app/src/test/java/com/localcash/domain/auth/AuthUseCaseTest.java` | Tests unitarios | Crear | Media |
| `app/src/test/java/com/localcash/data/auth/CredentialsStoreTest.java` | Tests de integración | Crear | Media |

**Contrato de datos / interfaz expuesta:**
- Entrada: PIN de 6 dígitos (String) o resultado de BiometricPrompt
- Salida: boolean (autenticación exitosa) + intent (navegar a MainActivity)
- Errores posibles: `AUTH_FAILED`, `BIOMETRY_UNAVAILABLE`, `ACCOUNT_LOCKED`, `INVALID_PIN_FORMAT`

---

## 2. Decisiones de Arquitectura

### 2.1 BiometricPrompt en Domain vs UI

**Contexto:** La UI Layer invoca BiometricPrompt, pero la lógica de "qué hacer después del fallo" debe residir en Domain.

**Opciones consideradas:**
- A) Toda la lógica en Activity (fácil pero viola separación de capas)
- B) AuthUseCase en domain/ con callbacks, UI solo muestra el prompt

**Decisión:** B

**Justificación:** Cumple arquitectura por capas; UI orquesta, Domain decide.

**Consecuencia aceptada:** Se definen interfaces de callback en domain/auth/.

### 2.2 Almacenamiento de credenciales: EncryptedSharedPreferences

**Contexto:** El PIN hasheado debe almacenarse de forma segura.

**Opciones consideradas:**
- A) SQLite (tabla USUARIOS) - más complejo, requiere migración
- B) EncryptedSharedPreferences - nativo, cifrado, simple
- C) Archivo cifrado manual - más control, más código

**Decisión:** B

**Justificación:** Cumple requisito de seguridad sin complejidad adicional; usa Android Keystore.

**Consecuencia aceptada:** Si en el futuro se necesita multi-usuario, migrar a SQLite.

### 2.3 Hash del PIN

**Contexto:** El PIN no debe almacenarse en texto plano.

**Opciones consideradas:**
- A) SHA-256 simple
- B) PBKDF2 con salt
- C) BCrypt

**Decisión:** B (PBKDF2)

**Justificación:** Resistente a rainbow tables; nativo en Android sin dependencias externas.

**Consecuencia aceptada:** El hash es de un solo sentido; no se puede recuperar el PIN original.

### 2.4 Tema Alto Contraste

**Contexto:** Debe garantizar ratios WCAG AAA.

**Opciones consideradas:**
- A) `res/values-hc/` con colores definidos
- B) Theme overlay en código
- C) Accesibilidad del sistema + colores dinámicos

**Decisión:** A

**Justificación:** Explícito, testeable, y funciona en todas las versiones.

**Consecuencia aceptada:** Se deben definir colores de alto contraste para todos los componentes.

### 2.5 Fuente OpenDyslexic

**Contexto:** Debe aplicarse globalmente sin afectar rendimiento.

**Opciones consideradas:**
- A) Sobrescribir Typography en MaterialTheme
- B) Fuente del sistema con fallback
- C) Custom View que aplique la fuente

**Decisión:** A

**Justificación:** Centraliza la aplicación; respeta el tema activo.

**Consecuencia aceptada:** La fuente se aplica a toda la app; si falta el .ttf, se usa la del sistema.

---

## 3. Riesgos Identificados

| # | Riesgo | Probabilidad | Impacto | Mitigación |
|---|--------|-------------|---------|------------|
| 1 | BiometricPrompt no disponible en emulador | Alta | Media | Usar Robolectric con shadow; fallback a PIN |
| 2 | PBKDF2 costoso en dispositivos antiguos | Baja | Baja | Usar 10,000 iteraciones (balance seguridad/rendimiento) |
| 3 | Usuario olvida el PIN | Alta | Baja | Mensaje de "contactar soporte" (feature posterior) |
| 4 | Fuente OpenDyslexic no incluida | Baja | Baja | Verificar inclusión en APK; fallback a fuente del sistema |

**Criterio de abortar/replantear:** Si BiometricPrompt falla consistentemente en pruebas, escalar a revisión de diseño antes de continuar.

---

## 4. Dependencias Externas

| Dependencia | Tipo | Estado | Plan B si falla |
|-------------|------|--------|-----------------|
| `androidx.biometric:biometric:1.1.0` | Librería | Disponible | Usar FingerprintManager (legacy) |
| `androidx.security:security-crypto:1.1.0-alpha06` | Librería | Disponible | Usar SharedPreferences estándar (menos seguro) |
| OpenDyslexic.ttf | Recurso | Pendiente | Usar fuente del sistema |

**Requisitos previos del entorno:** Ninguno adicional.

---

## 5. Estrategia de Persistencia y DAOs

**No aplica SQLite.** Las credenciales se almacenan en EncryptedSharedPreferences:

```java
// CredentialsStore.java (data layer)
public class CredentialsStore {
    private static final String PREFS_NAME = "secure_credentials";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_BIOMETRY_ENABLED = "biometry_enabled";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_TIME = "lockout_time";
    
    // Métodos: savePin(), verifyPin(), isBiometryEnabled(), etc.
}
```

**Seed Data:** Usuario "admin" con PIN "123456" hasheado con PBKDF2.

---

## 6. Estrategia de verificación

- Criterios de aceptación → tests unitarios (JUnit + Mockito) y UI (Robolectric)
- Pruebas manuales: Flujo completo en dispositivo real con biometría
- Datos de prueba: PIN "123456" (correcto), "000000" (incorrecto)
- Criterio de "terminado" (Definition of Done): Código + tests verdes + documentación actualizada + registro en `doc/005-login-biometria-pin.md`
