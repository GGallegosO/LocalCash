# spec.md — Especificación de Funcionalidad

> **Identificación:** 007-cuentas
> **Fecha de creación:** 05/10/2026 | **Responsable:** GG
> **Estado:** Aprobada
>
> **Propósito de este documento:** Definir QUÉ debe hacer la feature y CÓMO se demuestra que funciona,
> SIN especificar cómo se implementa (eso va en `plan.md`). Si aquí aparece un nombre de framework,
> el documento está fuera de su alcance.

---

## 1. Descripción Detallada

Implementación del núcleo del Módulo 1: Gestión de Cuentas y Transacciones financieras utilizando una base de datos local SQLite. Se introduce una arquitectura de "Single Activity" con navegación inferior (Bottom Navigation) y un contenedor de vistas (ViewFlipper) para manejar 4 secciones, desarrollando completamente las vistas de "Cuentas" y "Nuevo Movimiento".

**Comportamiento esperado (narrativa):**
1. El usuario ingresa a la aplicación y se autentica exitosamente.
2. El sistema redirige a la pantalla principal de "Cuentas", mostrando el saldo total consolidado y la lista de cuentas existentes.
3. El usuario crea nuevas cuentas o registra ingresos/egresos, y el sistema actualiza los saldos de forma inmediata y persistente en el dispositivo.

---

## 2. Contexto de Negocio

- **¿Por qué ahora?** Es el núcleo funcional de la aplicación. Sin cuentas y movimientos, las features de mapas y contactos carecen de datos que mostrar.
- **¿A qué objetivo de `mission.md` contribuye?** Permite la gestión financiera 100% offline, garantizando la privacidad de los datos sin depender de servidores externos.
- **¿Qué valor entrega al usuario?** Control total y visualización clara del estado financiero personal, con una interfaz limpia y sin fricción.
- **Usuarios afectados:** Todos los usuarios registrados (incluyendo la cuenta demo "admin").
- **Dependencias con otras features:** Depende de la Feature 006 (Login/Registro) para la autenticación previa. Bloquea las Features 008 (Mapas) y 009 (Contactos/Deudas).

---

## 3. Criterios de Aceptación

Cada criterio debe ser verificable por alguien distinto del implementador. Formato: `Dado <contexto>, Cuando <acción>, Entonces <resultado observable>` (Gherkin en español).

- [ ] **[CA-1]** Dado que el usuario está en la vista "Cuentas", cuando crea una nueva cuenta (ej. "Efectivo") con un saldo inicial, entonces la cuenta aparece en la lista y el saldo total consolidado se actualiza inmediatamente.
- [ ] **[CA-2]** Dado que el usuario está en la vista "Nuevo", cuando registra un "Egreso" de un monto específico en una cuenta seleccionada, entonces el saldo de dicha cuenta disminuye exactamente en ese monto y la transacción queda registrada en la base de datos local.
- [ ] **[CA-3]** Dado que el usuario está en cualquier vista del Módulo 1, cuando selecciona un ítem en la barra de navegación inferior, entonces el contenido central cambia a la vista correspondiente sin perder el estado de la Activity.
- [ ] **[CA-4]** Dado que el usuario está en la pantalla de Cuentas, cuando presiona el botón de alternar tema, entonces la interfaz cambia inmediatamente entre modo claro y oscuro utilizando los atributos de Material 3, sin colores hardcodeados.

**Requisitos no funcionales aplicables:**
- **Rendimiento:** La actualización del saldo tras una transacción debe ser visible en la interfaz en menos de 500ms.
- **Seguridad:** Los datos financieros se almacenan localmente en SQLite; no se transmite información a servidores externos. Los montos se almacenan como enteros (centavos) para evitar errores de precisión de punto flotante (cumpliendo `.agent.md` §6.10).
- **Compatibilidad:** Funciona en dispositivos Android con API 28+ (según `tech-stack.md`).

---

## 4. Casos Borde a Considerar

Lista exhaustiva de situaciones límite que la implementación DEBE manejar. Cada caso borde sin solución acordada bloquea la aprobación de esta spec.

| # | Caso borde                                   | Comportamiento esperado                     | ¿Resuelto? |
|---|----------------------------------------------|---------------------------------------------|------------|
| 1 | Entrada de monto <= 0 en una transacción     | El sistema rechaza el guardado y muestra un error: "El monto debe ser mayor a cero". | [x]        |
| 2 | Intentar crear una cuenta con nombre vacío   | El sistema deshabilita el botón de guardar o muestra un error de validación en el campo. | [x]        |
| 3 | Intentar registrar un egreso que supere el saldo disponible de la cuenta       | El sistema bloquea la transacción, no la guarda en la base de datos y muestra un mensaje de error visible: "Saldo insuficiente en la cuenta seleccionada". | [x]        |
| 4 | Rotación de pantalla durante el llenado del formulario | La Activity se recrea, pero la vista activa del ViewFlipper y los datos ingresados se preservan. | [x]        |
| 5 | Base de datos no inicializada al primer acceso | El sistema inicializa la base de datos Room en segundo plano de forma segura antes de cualquier operación. | [x]        |
| 6 | Cambio rápido y repetido entre pestañas      | El ViewFlipper maneja las transiciones sin crashear ni duplicar operaciones de base de datos. | [x]        |

---

## 5. Fuera de alcance de esta feature

- Funcionalidad real del mapa con osmdroid y geolocalización (la UI del switch existirá, pero no guardará coordenadas ni mostrará el mapa interactivo).
- Gestión de contactos y lógica de división de gastos (la vista mostrará un placeholder "Próximamente").
- Tests de interfaz de usuario (Espresso) automatizados complejos (se prioriza la verificación manual exhaustiva, pero se requieren tests unitarios básicos para la lógica de dominio/DAO según `.agent.md`).
- Migración de datos desde features anteriores (se asume una base de datos limpia para esta feature).

---

## 6. Trazabilidad

- Registro de memoria asociado: `doc/007-cuentas.md` (obligatorio al cerrar, ver `.agent.md` §7).
- Hito en roadmap: Fase 3 - Núcleo Financiero (Introducción de Room/SQLite y transacciones básicas).




