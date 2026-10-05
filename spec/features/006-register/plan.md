# plan.md — Plan Técnico de Implementación

> **Identificación:** 006-register | **Fecha:** 05/10/2026 | **Autor:** GG
>
> **Propósito de este documento:** Definir CÓMO se implementará la feature descrita en `spec.md`:
> componentes, decisiones técnicas, riesgos y dependencias. Es el puente entre el requisito y las
> tareas ejecutables (`tasks.md`).
>
> **Regla:** Ninguna decisión de este archivo puede contradecir `.agent.md` ni `tech-stack.md`.
> Si la contradicción es inevitable, primero se actualiza la constitución con su registro en `doc/`.

---

## 1. Componentes Involucrados

> Lista TODO archivo/módulo que será creado o modificado. Si durante la implementación aparece un
> archivo fuera de esta lista, hay dos opciones: actualizar el plan (y justificar) o detenerse.

| Archivo / Módulo                        | Rol en la feature                          | Acción (crear/modificar) | Complejidad estimada |
|-----------------------------------------|--------------------------------------------|--------------------------|----------------------|
| `app/src/main/java/com/localcash/ui/register/RegisterActivity.java` | Controlador UI del formulario de registro | CREAR | MEDIA |
| `app/src/main/res/layout/activity_register.xml` | Layout Material 3 para el formulario de registro | CREAR | BAJA |
| `app/src/main/java/com/localcash/data/security/SecureAuthManager.java` | Lógica de persistencia segura de credenciales | MODIFICAR | ALTA |
| `app/src/main/java/com/localcash/ui/login/LoginActivity.java` | Agregar enlace de navegación al registro | MODIFICAR | BAJA |
| `app/src/main/res/values/strings.xml` | Recursos de texto (prefijo `aRegister_`) | MODIFICAR | BAJA |
| `app/src/main/AndroidManifest.xml` | Declaración de la nueva Activity | MODIFICAR | BAJA |
| `doc/006-register.md` | Registro de memoria de la tarea ejecutada | CREAR | BAJA |

**Contrato de datos / interfaz expuesta:**
- **Entrada esperada:** Nombre (String), Región (String), Ciudad (String), Usuario (String), Contraseña (String).
- **Salida garantizada:** Datos almacenados de forma cifrada en `EncryptedSharedPreferences` y redirección a `LoginActivity`.
- **Errores posibles:** `CAMPO_VACIO` (validación UI), `USUARIO_RESERVADO` (si intenta usar "admin").

---

## 2. Decisiones de Arquitectura

Formato recomendado por decisión: contexto → opciones → elección → justificación → consecuencia.

1. **Estrategia de Persistencia de Múltiples Usuarios**
   - Opciones consideradas: A) Migrar a Room/SQLite ahora, B) Usar `EncryptedSharedPreferences` con claves dinámicas (ej. `user_<username>_password`).
   - Decisión: **Opción B**.
   - Justificación: Reutiliza la infraestructura de seguridad ya validada y blindada en la Feature 005. Mantiene la app 100% offline y ligera. La migración a Room se reserva para la Feature 007 (Cuentas y Movimientos) donde se requiere relacionar usuarios con transacciones.
   - Consecuencia aceptada: Escalabilidad limitada a unos pocos usuarios en el mismo dispositivo, pero más que suficiente para el alcance académico, demostrativo y de prototipado actual.

2. **Manejo de la Cuenta Demo ("admin")**
   - Opciones consideradas: A) Forzar el registro y eliminar el seed, B) Guardar el seed en SharedPreferences al inicio, C) Mantener el seed como fallback hardcodeado en la lógica de validación.
   - Decisión: **Opción C**.
   - Justificación: Garantiza que la demo siempre funcione al instante, incluso si se limpian los datos de la app o si el registro falla. Cumple con el requisito explícito del dueño del producto para fines demostrativos.
   - Consecuencia aceptada: Se debe agregar una validación estricta en el registro para bloquear la creación de un usuario llamado "admin" (case-insensitive).

---

## 3. Riesgos Identificados

### Riesgo 1: Sobrescritura o corrupción de las credenciales de "admin"
- **Probabilidad:** BAJA
- **Impacto:** ALTO (crítico para la demo)
- **Mitigación:** Validación estricta `if (usuario.equalsIgnoreCase("admin"))` antes de cualquier operación de guardado.
- **Dueño:** Gustavo Gallegos

### Riesgo 2: La IA (OpenCode) intente migrar a Room prematuramente o usar Kotlin
- **Probabilidad:** MEDIA
- **Impacto:** MEDIO (desvío del plan, pero recuperable)
- **Mitigación:** El prompt inicial enfatiza explícitamente leer `.agent.md` y usar Java + EncryptedSharedPreferences.
- **Dueño:** Gustavo Gallegos

### Riesgo 3: Pérdida de contexto en la sesión de IA, rompiendo la Feature 005
- **Probabilidad:** MEDIA
- **Impacto:** ALTO (requiere rehacer trabajo)
- **Mitigación:** Uso de una nueva sesión de OpenCode con el contexto del `.agent.md` y `spec/` inyectado desde el inicio.
- **Dueño:** Gustavo Gallegos

**Criterio de abortar/replantear:** Si la refactorización de `SecureAuthManager` para soportar múltiples usuarios requiere más de 2 horas de esfuerzo o introduce complejidad ciclomática alta, se detiene la tarea y se escala a revisión de diseño (se mantendrá el enfoque simple de SharedPreferences).

**Criterio de abortar/replantear:** Si la refactorización de `SecureAuthManager` para soportar múltiples usuarios requiere más de 2 horas de esfuerzo o introduce complejidad ciclomática alta, se detiene la tarea y se escala a revisión de diseño (se mantendrá el enfoque simple de SharedPreferences).

---

## 4. Dependencias Externas

| Dependencia                        | Tipo (servicio/librería/dato/equipo) | Estado del acuerdo | Plan B si falla        |
|------------------------------------|---------------------------------------|--------------------|------------------------|
| `androidx.security:security-crypto`| Librería de AndroidX                  | DISPONIBLE (Feature 005) | N/A (Ya integrada y probada) |
| Emulador Android / Dispositivo Físico | Entorno de ejecución y pruebas UI    | DISPONIBLE         | Usar `BuildConfig.DEBUG` para bypass de flujos si el emulador presenta bugs de biometría. |

**Requisitos previos del entorno:** Emulador o dispositivo con API 28+ (para soporte robusto de biometría y crypto). No se requieren credenciales de red ni servicios en la nube (Pilar 100% Offline).

---

## 5. Estrategia de verificación

- **Criterios de aceptación → tests:** Dado el acuerdo académico actual para agilizar el prototipado, se prioriza la **verificación manual exhaustiva** sobre tests unitarios automatizados para esta iteración específica (se documenta como excepción controlada en `doc/006-register.md`).
- **Pruebas manuales necesarias:**
   1. Ir a Login → Click en "¿No tienes cuenta? Regístrate".
   2. Llenar formulario con datos válidos (Usuario: "test_user", NO "admin").
   3. Verificar Toast de éxito y redirección automática al Login.
   4. Iniciar sesión con el nuevo usuario "test_user".
   5. Verificar en *Device File Explorer* que los datos están cifrados (ciphertext).
   6. Verificar que la cuenta demo "admin" / "123456" sigue funcionando sin alteraciones.
- **Datos de prueba requeridos:** Nombre: "Gustavo", Región: "Metropolitana", Ciudad: "Santiago", Usuario: "gustavo_demo", Pass: "demo123".
- **Criterio de "terminado" (Definition of Done):** Código compilado sin warnings (`lint`), flujo manual verificado exitosamente, documentación actualizada y registro de memoria generado en `doc/006-register.md`.