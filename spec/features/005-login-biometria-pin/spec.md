# spec.md — Especificación de Funcionalidad

> **Identificación:** 005-login-biometria-pin
> **Fecha de creación:** 2026-10-04 | **Responsable:** Gustavo Gallegos
> **Estado:** Borrador
>
> **Propósito de este documento:** Definir QUÉ debe hacer la feature y CÓMO se demuestra que funciona,
> SIN especificar cómo se implementa (eso va en `plan.md`).

---

## 1. Descripción Detallada

Pantalla de inicio de sesión con seguridad híbrida 100% offline para LocalCash. Permite al usuario autenticarse mediante biometría (huella dactilar/reconocimiento facial) o, como fallback, mediante un PIN numérico de 6 dígitos.

**Comportamiento esperado (narrativa):**
1. El usuario abre la aplicación.
2. El sistema verifica si el dispositivo tiene biometría configurada.
3. Si hay biometría disponible, se muestra el prompt de BiometricPrompt.
4. Si la biometría falla o no está disponible, se muestra el teclado de PIN.
5. El resultado observable es el acceso a la app o el bloqueo temporal tras N intentos fallidos.

---

## 2. Contexto de Negocio

- **¿Por qué ahora?** La misión exige proteger el acceso a la información financiera (Objetivo 2: 100% de inicios de sesión validados por biometría).
- **¿A qué objetivo de `mission.md` contribuye?** Objetivo 2: Proteger el acceso a la información financiera mediante BiometricPrompt.
- **¿Qué valor entrega al usuario?** Acceso rápido y seguro sin exponer datos en la nube.
- **Usuarios afectados:** Usuario principal (dueño del dispositivo).
- **Dependencias con otras features:** Ninguna (es la puerta de entrada a la app).

---

## 3. Criterios de Aceptación

- [ ] **CA-1:** Dado un dispositivo con biometría configurada, cuando el usuario se autentica exitosamente, entonces accede a la aplicación.
- [ ] **CA-2:** Dado un fallo biométrico, cuando el usuario lo solicita o falla 3 veces, entonces se muestra el teclado de PIN de 6 dígitos.
- [ ] **CA-3:** Dado 5 intentos fallidos de PIN, entonces la cuenta se bloquea temporalmente (30 segundos).
- [ ] **CA-4:** Dado cualquier escenario de autenticación, entonces la operación es 100% offline.
- [ ] **CA-5:** Dado un usuario con tema Oscuro activado, cuando abre la pantalla de login, entonces la interfaz respeta el tema oscuro.
- [ ] **CA-6:** Dado un usuario con modo Alto Contraste activado, cuando abre la pantalla de login, entonces la interfaz usa el tema de alto contraste.
- [ ] **CA-7:** Dado un usuario con fuente Dislexía seleccionada, cuando abre la pantalla de login, entonces la interfaz usa la tipografía OpenDyslexic.

**Requisitos no funcionales:**
- Rendimiento: Respuesta inmediata (< 200ms) en la verificación de PIN.
- Seguridad: Credenciales cifradas con EncryptedSharedPreferences; nunca en texto plano.
- Accesibilidad: Soporte nativo para Light/Dark Mode, High Contraste y OpenDyslexic.

---

## 4. Casos Borde a Considerar

| # | Caso borde | Comportamiento esperado |
|---|------------|------------------------|
| 1 | Dispositivo sin sensor biométrico | Mostrar directamente el teclado de PIN |
| 2 | Biometría no configurada en el SO | Mostrar mensaje y ofrecer fallback a PIN |
| 3 | 5 intentos fallidos de PIN | Bloqueo temporal de 30 segundos con mensaje visible |
| 4 | El usuario cancela el prompt biométrico | Mostrar teclado de PIN como fallback |
| 5 | Tema cambiado mientras la app está abierta | La pantalla de login se recrea con el nuevo tema |
| 6 | Fuente OpenDyslexic no disponible | Usar fuente del sistema como fallback |
| 7 | Rotación de pantalla durante autenticación | Mantener estado de autenticación |

---

## 5. Fuera de alcance de esta feature

- Recuperación de contraseña olvidada (requiere feature posterior).
- Registro de usuarios adicionales (solo usuario "admin").
- Sincronización de credenciales con la nube (prohibido por misión).
- Integración con cuentas de Google/Apple.

---

## 6. Impacto en data-model.md

**No aplica.** Esta feature no modifica el esquema de datos. Las credenciales de autenticación se almacenan en EncryptedSharedPreferences, no en la base de datos SQLite.

---

## 7. Verificación de Invariantes de Negocio y Persistencia

- [ ] **Montos enteros CLP:** No aplica directamente, pero cualquier futura referencia a montos debe ser INTEGER.
- [ ] **PRAGMA foreign_keys = ON:** No aplica (no usa SQLite directamente).
- [ ] **Integridad de saldos:** No aplica en esta feature.
- [ ] **Operación 100% offline:** Verificar que ninguna operación de autenticación requiere red.

---

## 8. Trazabilidad

- Registro de memoria asociado: `doc/005-login-biometria-pin.md` (obligatorio al cerrar).
- Hito en roadmap: Fase 1 - Autenticación Biométrica.
