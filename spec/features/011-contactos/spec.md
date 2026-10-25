# spec.md — Especificación de Funcionalidad

> **Identificación:** 011-contactos
> **Fecha de creación:** 2026-10-25 | **Responsable:** Agente SDD
> **Estado:** Borrador
>
> **Propósito de este documento:** Definir QUÉ debe hacer la feature y CÓMO se demuestra que funciona,
> SIN especificar cómo se implementa (eso va en `plan.md`). Si aquí aparece un nombre de framework,
> el documento está fuera de su alcance.

---

## 1. Descripción Detallada

Directorio local de contactos: agenda de terceros con los que el usuario comparte gastos. Cada
contacto tiene nombre obligatorio, teléfono y email opcionales, y notas libres. El usuario puede
agregar, editar, eliminar y ver contactos. Al tocar un contacto se abre su historial de cobros y
pagos (movimientos compartidos donde ese contacto figura como pagador o deudor).

**Comportamiento esperado (narrativa):**

1. El usuario abre el módulo "Contactos" y ve la lista de contactos registrados.
2. El usuario toca "Agregar contacto", llena nombre (obligatorio) y opcionalmente teléfono/email/notas, y confirma.
3. El sistema valida que el nombre no esté vacío y que no exista otro contacto con el mismo teléfono o email (si se proporcionó alguno).
4. El resultado observable es que el nuevo contacto aparece en la lista.
5. El usuario toca un contacto de la lista y el sistema abre su detalle: datos del contacto e historial de movimientos compartidos (compra grupal, monto asignado, monto pagado, estado pendiente/parcial/liquidado).
6. El usuario toca "Eliminar" en un contacto sin compromisos: el sistema borra el contacto y vuelve a la lista.
7. El usuario toca "Eliminar" en un contacto con deudas pendientes: el sistema muestra error `CONTACTO_EN_USO` y no borra nada.

---

## 2. Contexto de Negocio

- **¿Por qué ahora?** Es el requisito previo para compartir gastos entre contactos (Feature 012). Sin contactos no hay con quién dividir una compra.
- **¿A qué objetivo de `mission.md` contribuye?** Gestión financiera local y offline, registro de quién debe qué a quién.
- **¿Qué valor entrega al usuario?** Saber a quién le debe dinero y quién le debe a él, sin depender de memoria ni de hojas externas.
- **Usuarios afectados:** Usuario principal de LocalCash (dueño de la app).
- **Dependencias con otras features:** Feature 009 (mapa) ya está en `main`; Feature 012 (división de gastos) depende de esta para existir.

---

## 3. Criterios de Aceptación

Cada criterio debe ser verificable por alguien distinto del implementador. Formato:
`Dado <contexto>, Cuando <acción>, Entonces <resultado observable>` (Gherkin en español).

- [ ] **[CA-1]** Dado que no hay contactos, cuando el usuario abre el módulo Contactos, entonces ve una lista vacía con mensaje "Sin contactos".
- [ ] **[CA-2]** Dado que el usuario está en la lista de contactos, cuando toca "Agregar contacto", llena un nombre válido y confirma, entonces el contacto aparece en la lista con el nombre, teléfono y email indicados.
- [ ] **[CA-3]** Dado que el usuario intenta agregar un contacto con nombre vacío, cuando confirma, entonces el sistema rechaza el ingreso y muestra "Ingresa un nombre para el contacto".
- [ ] **[CA-4]** Dado que existe un contacto con teléfono "555-0100", cuando el usuario intenta agregar otro contacto con el mismo teléfono, entonces el sistema rechaza el ingreso y muestra "Ya existe un contacto con ese teléfono".
- [ ] **[CA-5]** Dado que existe un contacto con email "juan@correo.cl", cuando el usuario intenta agregar otro contacto con el mismo email, entonces el sistema rechaza el ingreso y muestra "Ya existe un contacto con ese email".
- [ ] **[CA-6]** Dado que existe un contacto "Juan Pérez", cuando el usuario toca "Editar", cambia el nombre a "Juan Pérez (trabajo)" y confirma, entonces la lista muestra el nombre actualizado.
- [ ] **[CA-7]** Dado que existe un contacto sin compras asociadas, cuando el usuario toca "Eliminar" y confirma, entonces el contacto desaparece de la lista.
- [ ] **[CA-8]** Dado que existe un contacto con al menos una deuda `pendiente` o `parcial`, cuando el usuario toca "Eliminar", entonces el sistema muestra "No se puede eliminar: el contacto tiene compromisos pendientes" y el contacto permanece en la lista.
- [ ] **[CA-9]** Dado que existe un contacto con historial de compras compartidas, cuando el usuario toca el contacto en la lista, entonces el sistema abre el detalle del contacto mostrando sus datos y el historial de movimientos compartidos (compra, monto asignado, monto pagado, estado).
- [ ] **[CA-10]** Dado que existe un contacto sin compras compartidas, cuando el usuario toca el contacto en la lista, entonces el sistema abre el detalle mostrando sus datos y el mensaje "Sin compras compartidas".

**Requisitos no funcionales aplicables:**

- Rendimiento: la lista de contactos se carga en menos de 1 segundo con hasta 200 contactos.
- Seguridad: los datos de contacto nunca salen del dispositivo (100% offline).
- Accesibilidad: todos los campos tienen etiquetas de acceso para lectores de pantalla.

---

## 4. Casos Borde a Considerar

Lista exhaustiva de situaciones límite que la implementación DEBE manejar. Cada caso borde sin
solución acordada bloquea la aprobación de esta spec.

| # | Caso borde                                   | Comportamiento esperado                     | ¿Resuelto? |
|---|----------------------------------------------|---------------------------------------------|------------|
| 1 | Entrada vacía o nula donde se requiere dato (nombre) | Rechazar con mensaje "Ingresa un nombre para el contacto" | [ ]        |
| 2 | Teléfono o email duplicado (case-insensitive) | Rechazar con mensaje "Ya existe un contacto con ese teléfono/email" | [ ]        |
| 3 | Teléfono o email vacío (opcional)           | Aceptar (es opcional)                       | [ ]        |
| 4 | Eliminar contacto con deudas pendientes/parciales | Bloquear con `CONTACTO_EN_USO`             | [ ]        |
| 5 | Eliminar contacto con deudas liquidadas     | Permitir eliminación física                 | [ ]        |
| 6 | Caracteres especiales en nombre (acentos, ñ, emoji) | Aceptar y mostrar correctamente          | [ ]        |
| 7 | Lista con 200 contactos                     | Carga en menos de 1 segundo                 | [ ]        |
| 8 | Edición de contacto: cambiar a teléfono/email ya usado por otro contacto | Rechazar con mensaje de duplicado | [ ]        |

---

## 5. Fuera de alcance de esta feature

- Importar contactos de la agenda del sistema Android.
- Sincronización con servidor o copia de seguridad en la nube.
- Validación de formato de teléfono o email más allá de no-vacío y unicidad.
- Foto de perfil o avatar del contacto.
- Llamar o enviar mensaje al contacto desde la app.

---

## 6. Trazabilidad

- Registro de memoria asociado: `doc/011-contactos.md` (obligatorio al cerrar, ver `.agent.md` §7).
- Hito en roadmap: "Directorio Local de Contactos" (`roadmap.md` §2).