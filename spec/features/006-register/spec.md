# spec.md — Especificación de Funcionalidad

> **Identificación:** 006-register
> **Fecha de creación:** 05/10/2026 | **Responsable:** GG
> **Estado:** Aprobada
>
> **Propósito de este documento:** Definir QUÉ debe hacer la feature y CÓMO se demuestra que funciona,
> SIN especificar cómo se implementa (eso va en `plan.md`). Si aquí aparece un nombre de framework,
> el documento está fuera de su alcance.
>
> **Instrucciones:** Copia esta carpeta `_TEMPLATE_/` a `spec/features/[NNN]-[nombre-tarea]/`,
> renombra los archivos si es necesario y reemplaza todos los placeholders `[TEXTO_EN_MAYUSCULAS]`.

---

## 1. Descripción Detallada

La feature de Registro de Usuarios permite a nuevos usuarios crear una cuenta personalizada en LocalCash, capturando sus datos personales (nombre, región, ciudad) y credenciales de acceso (usuario, contraseña). Esta información se almacena de forma cifrada para habilitar la futura geolocalización de transacciones y personalización de la experiencia. La cuenta demo "admin/123456" permanece disponible en todo momento para fines demostrativos.

**Comportamiento esperado (narrativa):**
1. El usuario accede a la pantalla de Login y selecciona la opción "¿No tienes cuenta? Regístrate".
2. El sistema presenta un formulario con campos para nombre, región, ciudad, usuario y contraseña.
3. El usuario completa el formulario y presiona "Crear Cuenta".
4. El sistema valida que todos los campos estén completos y que el usuario no sea "admin".
5. El sistema almacena los datos de forma cifrada y redirige al usuario al Login con un mensaje de éxito.
6. El usuario puede iniciar sesión con sus nuevas credenciales o continuar usando la cuenta demo "admin".

> *Guía de redacción:* Escribe en tercera persona y tiempo presente ("el sistema valida...", no
> "validaremos..."). Describe comportamiento observable desde fuera, no mecanismos internos.

---

## 2. Contexto de Negocio

- **¿Por qué ahora?** El proyecto necesita transicionar del seed data hardcodeado a un sistema de usuarios reales para habilitar la personalización y geolocalización en features posteriores.
- **¿A qué objetivo de `mission.md` contribuye?** Objetivo 2: "Permitir a usuarios de dispositivos Android gestionar sus cuentas financieras de forma personalizada y segura".
- **¿Qué valor entrega al usuario?** Permite crear una identidad única dentro de la app, captura datos geográficos para futura personalización de contenido (comercios locales), y mantiene la seguridad de las credenciales mediante cifrado.
- **Usuarios afectados:** Todos los usuarios finales de LocalCash que deseen crear una cuenta personalizada.
- **Dependencias con otras features:** Depende de Feature 005 (Login, Biometría, EncryptedSharedPreferences) completada. Precede a Feature 007 (Cuentas y Movimientos con Room y osmdroid).

---

## 3. Criterios de Aceptación

Cada criterio debe ser verificable por alguien distinto del implementador. Formato:
`Dado <contexto>, Cuando <acción>, Entonces <resultado observable>` (Gherkin en español).

- [ ] **[CA-1]** Dado que el usuario está en la pantalla de Login, cuando presiona el enlace "¿No tienes cuenta? Regístrate", entonces el sistema navega a la pantalla de Registro mostrando el formulario completo.
- [ ] **[CA-2]** Dado que el usuario está en la pantalla de Registro, cuando deja uno o más campos vacíos y presiona "Crear Cuenta", entonces el sistema muestra mensajes de error junto a los campos vacíos y no permite el registro.
- [ ] **[CA-3]** Dado que el usuario intenta registrarse con el nombre de usuario "admin" (en cualquier combinación de mayúsculas/minúsculas), cuando presiona "Crear Cuenta", entonces el sistema muestra un error indicando que este usuario está reservado y no permite el registro.
- [ ] **[CA-4]** Dado que el usuario completa todos los campos correctamente con un usuario válido (diferente de "admin"), cuando presiona "Crear Cuenta", entonces el sistema muestra un Toast de éxito, redirige al Login y los datos quedan almacenados de forma cifrada.
- [ ] **[CA-5]** Dado que el usuario se registró exitosamente, cuando intenta iniciar sesión con sus nuevas credenciales, entonces el sistema valida correctamente y permite el acceso a CuentasActivity.
- [ ] **[CA-6]** Dado que la cuenta demo "admin/123456" existe, cuando el usuario intenta iniciar sesión con estas credenciales después de registrar nuevos usuarios, entonces el sistema permite el acceso sin alteraciones.

**Requisitos no funcionales aplicables:**
- Rendimiento: El registro debe completarse en menos de 2 segundos desde que el usuario presiona "Crear Cuenta".
- Seguridad: Todas las credenciales y datos personales deben almacenarse cifrados usando EncryptedSharedPreferences.
- Accesibilidad/compatibilidad: El formulario debe ser usable en pantallas desde 4.7" hasta 6.7", con soporte para modo claro y oscuro.

---

## 4. Casos Borde a Considerar

Lista exhaustiva de situaciones límite que la implementación DEBE manejar. Cada caso borde sin
solución acordada bloquea la aprobación de esta spec.

| # | Caso borde                                   | Comportamiento esperado                     | ¿Resuelto? |
|---|----------------------------------------------|---------------------------------------------|------------|
| 1 | Usuario intenta registrarse con nombre "Admin", "ADMIN", "aDmIn" | Sistema rechaza el registro mostrando error "Este usuario está reservado" | [ ] |
| 2 | Usuario ingresa espacios en blanco al inicio/final de los campos | Sistema trim() de los valores antes de validar y guardar | [ ] |
| 3 | Usuario intenta registrarse con un nombre de usuario ya existente | Sistema rechaza el registro mostrando error "Este usuario ya existe" | [ ] |
| 4 | Usuario rota la pantalla durante el llenado del formulario | Sistema preserva los datos ingresados (usando savedInstanceState) | [ ] |
| 5 | Usuario presiona el botón "Atrás" del sistema durante el registro | Sistema vuelve al Login sin guardar datos parciales | [ ] |
| 6 | Campos de contraseña con caracteres especiales (@, #, $, %, etc.) | Sistema acepta y almacena correctamente estos caracteres | [ ] |

---

## 5. Fuera de alcance de esta feature

- Migración a base de datos Room/SQLite (se realizará en Feature 007).
- Registro biométrico durante el onboarding (el usuario deberá configurar biometría manualmente desde ajustes del dispositivo).
- Validación de fortaleza de contraseña (longitud mínima, caracteres especiales obligatorios).
- Recuperación de contraseña olvidada (la app es 100% offline, no hay backend para enviar emails).
- Edición de perfil de usuario después del registro (se agregará en features posteriores).
- Validación de formato de región/ciudad contra una lista predefinida (se aceptan valores libres de texto).

> *Guía:* Delimitar el "no" evita discusiones durante la implementación y scope creep. Ejemplo de
> estilo: "La validación de identidad ante proveedores externos queda para la feature [NNN+1]."

---

## 6. Trazabilidad

- Registro de memoria asociado: `doc/006-register.md` (obligatorio al cerrar, ver `.agent.md` §7).
- Hito en roadmap: Fase 2 - Autenticación y Onboarding (ver `spec/constitution/roadmap.md`).


