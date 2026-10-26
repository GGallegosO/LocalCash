# spec.md — Especificación de Funcionalidad

> **Identificación:** 012-division-gastos
> **Fecha de creación:** 2026-10-25 | **Responsable:** Agente SDD
> **Estado:** Borrador
>
> **Propósito de este documento:** Definir QUÉ debe hacer la feature y CÓMO se demuestra que funciona,
> SIN especificar cómo se implementa (eso va en `plan.md`). Si aquí aparece un nombre de framework,
> el documento está fuera de su alcance.

---

## 1. Descripción Detallada

División de gastos compartidos: al registrar un egreso, el usuario puede marcarlo como **compartido** y
elegir uno o más contactos de su agenda local. El monto del egreso se divide entre los elegidos en partes
**iguales** o **personalizadas** (un monto por contacto que suma exactamente el total). Cada parte nace con
estado `pendiente` y pasa a `pagado` cuando el contacto devuelve su dinero; en ese momento la cuenta del
usuario se actualiza con el monto cobrado. La feature además muestra el resumen de deudas por contacto:
cuánto le debe cada uno al usuario.

**Comportamiento esperado (narrativa):**

1. El usuario abre el formulario de registro de un egreso, marca la opción "Compartido" y elige uno o más
   contactos de su agenda.
2. El sistema exige elegir el modo de división: **partes iguales** o **montos personalizados** por contacto.
3. En partes iguales, el sistema calcula porciones enteras que suman exactamente el total del egreso
   (el residuo de la división se reparte de a un peso, empezando por el primer contacto elegido).
4. En montos personalizados, el sistema muestra la suma de las partes en vivo y solo permite registrar
   cuando la suma iguala el total del egreso.
5. El resultado observable es: el egreso aparece en el historial, y cada contacto elegido figura con una
   parte `pendiente` por su monto asignado.
6. Cuando un contacto devuelve su dinero, el usuario marca esa parte como `pagada`; el sistema registra
   un ingreso de tipo "Devolución gasto compartido" en la cuenta del egreso original y el saldo de esa
   cuenta aumenta exactamente el monto asignado de la parte.
7. El usuario abre el resumen de deudas y ve, por contacto con partes pendientes, el total que le debe.
8. El usuario intenta eliminar un contacto con partes pendientes: el sistema lo bloquea con el mensaje de
   contacto en uso y no borra nada.

---

## 2. Contexto de Negocio

- **¿Por qué ahora?** Es la Feature 012 del roadmap (Fase 3 — Gastos Compartidos y Deudas). La Feature 011
  (Contactos) quedó completada expresamente como su prerequisito, y `doc/011-contactos.md` le transfiere el
  cierre del historial por contacto (CA-9/CA-10 de 011).
- **¿A qué objetivo de `mission.md` contribuye?** Gestión financiera personal y grupal 100% offline: saber
  quién debe cuánto sin hojas de cálculo externas.
- **¿Qué valor entrega al usuario?** Dejar de llevar la cuenta "a mano" de lo que le deben; el cobro de una
  devolución queda registrado y reflejado en el saldo sin pasos extra.
- **Usuarios afectados:** Usuario principal de LocalCash (actor implícito, dueño de las cuentas); terceros
  solo como contactos de su agenda.
- **Dependencias con otras features:** Feature 011 (Contactos) — completada; Features 007/008 (Cuentas e
  Historial) — el formulario de egreso y el asiento del movimiento ya existen y se extienden.

---

## 3. Criterios de Aceptación

Cada criterio debe ser verificable por alguien distinto del implementador. Formato:
`Dado <contexto>, Cuando <acción>, Entonces <resultado observable>` (Gherkin en español).

- [ ] **[CA-1]** Dado un egreso válido de $90.000, cuando el usuario lo marca compartido con 3 contactos en
      división igual y confirma, entonces el egreso queda registrado y existen 3 partes pendientes de $30.000
      cada una, una por contacto.
- [ ] **[CA-2]** Dado un egreso marcado como compartido, cuando el usuario confirma sin elegir ningún
      contacto, entonces el sistema rechaza el registro y muestra "Debes elegir al menos un contacto".
- [ ] **[CA-3]** Dado un egreso de $100.000 compartido en partes iguales entre 3 contactos, cuando el
      sistema calcula el reparto, entonces las partes son $33.334, $33.333 y $33.333 y su suma es
      exactamente $100.000.
- [ ] **[CA-4]** Dado un egreso de $2.000 compartido en partes iguales entre 3 contactos, cuando el usuario
      confirma, entonces el sistema rechaza el reparto y muestra que el monto no alcanza para una parte por
      contacto.
- [ ] **[CA-5]** Dado un egreso de $50.000 en división personalizada, cuando el usuario ingresa partes que
      suman $45.000 y confirma, entonces el sistema rechaza el registro y muestra que la suma de las partes
      debe igualar el total del egreso.
- [ ] **[CA-6]** Dado un egreso de $50.000 en división personalizada, cuando el usuario ingresa $30.000 y
      $20.000 (suma exacta) y confirma, entonces quedan dos partes pendientes con esos montos.
- [ ] **[CA-7]** Dada una parte pendiente de $30.000, cuando el usuario la marca como pagada, entonces la
      parte pasa a estado pagado con su fecha de pago, aparece en el historial un ingreso de $30.000 con
      descripción "Devolución gasto compartido" en la cuenta del egreso original, y el saldo de esa cuenta
      aumenta en $30.000.
- [ ] **[CA-8]** Dada una parte ya pagada, cuando el usuario intenta volver a marcarla o reabrirla, entonces
      el sistema no ofrece la acción y rechaza cualquier intento con "La parte ya fue cobrada".
- [ ] **[CA-9]** Dado que existen contactos con partes pendientes, cuando el usuario abre el resumen de
      deudas, entonces ve una fila por contacto con su nombre y la suma total que le debe, ordenada de mayor
      a menor deuda.
- [ ] **[CA-10]** Dado un contacto con al menos una parte pendiente, cuando el usuario intenta eliminarlo,
      entonces el sistema muestra "No se puede eliminar: el contacto tiene compromisos pendientes" y el
      contacto permanece en la agenda.
- [ ] **[CA-11]** Dado un egreso compartido con partes, cuando el usuario elimina el egreso, entonces
      desaparecen también sus partes (el asiento contable y su reparto son una sola realidad) y el resumen
      deja de mostrar esas deudas.
- [ ] **[CA-12]** Dado un contacto con egresos compartidos, cuando el usuario abre su detalle, entonces ve
      su historial de partes compartidas (egreso, monto asignado, estado pendiente/pagado), cerrando los
      criterios CA-9/CA-10 heredados de la Feature 011.

**Requisitos no funcionales aplicables:**

- Rendimiento: el resumen de deudas y el historial por contacto se cargan en menos de 1 segundo con hasta
  500 partes registradas.
- Consistencia: el cobro de una parte nunca deja el saldo desfaseado del libro mayor; la suma de movimientos
  de una cuenta sigue reflejando exactamente su saldo (invariante I-1 de la constitución).
- Seguridad: ningún dato de gastos compartidos sale del dispositivo (100% offline).
- Accesibilidad: todos los campos del formulario de división tienen etiquetas de acceso.

---

## 4. Entidad propuesta: `gastos_compartidos`

Registro de la parte que un contacto debe sobre un egreso compartido. Es la tabla intermedia entre
movimientos y contactos: cada fila es "el contacto X debe Y pesos por el egreso Z".

| Campo | Tipo | Clave / Restricción | Nullable | Descripción |
|---|---|---|---|---|
| `id` | `INTEGER` | PK · autoincremental | No | Identificador local. |
| `transaccion_id` | `INTEGER` | FK → transacciones(id) · obligatorio | No | Egreso compartido del que nace la parte. Una parte sin gasto no significa nada. |
| `contacto_id` | `INTEGER` | FK → contactos(id) · obligatorio · borrado restringido | No | Contacto deudor. Es siempre un tercero, nunca el usuario principal. |
| `monto_asignado` | `INTEGER` | obligatorio · positivo · pesos CLP | No | Parte del egreso que debe este contacto. |
| `estado` | `TEXT` (dominio cerrado) | obligatorio · `pendiente` / `pagado` | No | Ciclo de vida de la parte. `pagado` es terminal. |
| `fecha_creacion` | `TEXT` ISO-8601 | obligatorio | No | Momento en que se registró el reparto. |
| `fecha_pago` | `TEXT` ISO-8601 | — | Sí | Momento en que el usuario marcó la parte como cobrada. `NULL` mientras esté pendiente. |

Reglas adicionales del registro:

- **Unicidad:** un mismo contacto no puede tener dos partes sobre el mismo egreso (`transaccion_id` +
  `contacto_id` únicos).
- **Borrado del egreso:** sus partes se eliminan en cascada junto con él.
- **Borrado del contacto:** restringido; la agenda local bloquea eliminar a alguien con partes registradas.

---

## 5. Invariantes críticas (van a `domain/` con tests propios)

- **[INV-1]** Un egreso marcado como compartido sin al menos un contacto **no es válido**. La división sin
  deudores es un reparto de nada.
- **[INV-2]** La suma de los `monto_asignado` de todas las partes **iguala exactamente** el monto del egreso,
  al peso. En la división igual, el residuo de la división entera se asigna de a un peso por contacto
  empezando por el primero de la lista; si el monto es menor que el número de contactos, el reparto se
  rechaza (no existen partes de cero pesos).
- **[INV-3]** Una parte ya `pagada` **no puede volver a `pendiente`**: el sistema no ofrece ninguna
  operación para reabrir un cobro (la máquina de estados la hace cumplir el repositorio, no la interfaz).
- **[INV-4]** No se puede eliminar un contacto con partes registradas: la restricción de clave foránea lo
  impide en el motor y el dominio lo traduce a un mensaje de negocio comprensible.
- **[INV-5]** Marcar una parte como pagada es una operación **atómica**, dentro de una sola transacción de
  base de datos, con este orden obligatorio: (1) validar que la parte existe y está pendiente; (2) cambiar
  su estado a pagado y registrar su fecha de pago; (3) registrar un ingreso automático en la cuenta del
  egreso original con descripción "Devolución gasto compartido"; (4) aumentar el saldo de esa cuenta con
  aritmética atómica (`saldo = saldo + monto_asignado`, nunca leer-calcular-escribir). Si cualquier paso
  falla, ninguno se aplica.

---

## 6. Casos Borde a Considerar

| # | Caso borde | Comportamiento esperado | ¿Resuelto? |
|---|---|---|---|
| 1 | Egreso compartido con 0 contactos | Rechazar con `GASTO_SIN_CONTACTOS` (INV-1) | [ ] |
| 2 | El mismo contacto elegido dos veces | Rechazar: un contacto tiene a lo sumo una parte por egreso (unicidad) | [ ] |
| 3 | División igual con monto no divisible por n | Reparto con residuo de a 1 peso; suma exacta garantizada (INV-2) | [ ] |
| 4 | División igual con monto < n contactos | Rechazar: no hay partes de 0 pesos | [ ] |
| 5 | División personalizada con suma ≠ total | Rechazar con `REPARTO_NO_CUADRA`, mostrando suma actual vs total | [ ] |
| 6 | Parte de monto 0 o negativo en división personalizada | Rechazar: toda parte es un monto positivo | [ ] |
| 7 | Marcar como pagada una parte ya pagada (o inexistente) | Rechazar sin tocar saldo ni historial (INV-3/INV-5) | [ ] |
| 8 | Dos intentos simultáneos de cobrar la misma parte | Solo uno tiene efecto: la transacción de base serializa; el segundo no encuentra la parte pendiente | [ ] |
| 9 | Eliminar el egreso original | Sus partes desaparecen en cascada; el resumen las deja de mostrar (CA-11) | [ ] |
| 10 | Eliminar contacto con partes pendientes | Bloqueado (INV-4) | [ ] |
| 11 | Eliminar la cuenta del egreso | Sus movimientos caen en cascada y con ellos las partes; comportamiento heredado de la Feature 007 | [ ] |
| 12 | Egreso compartido de un solo contacto | Aceptado: es válido que alguien me deba el 100% | [ ] |

---

## 7. Fuera de alcance de esta feature

- **"Cuánto les debo"** (deudas del usuario con contactos): queda para la futura feature de COMPRAS_GRUPALES
  de la constitución, donde el pagador real es un tercero. En 012 el resumen solo cubre "me deben"
  (Decisión del dueño del producto, 2026-10-25).
- Desglose de la compra en ítems con precios unitarios (`ITEMS_COMPRA`) y liquidación parcial de deudas
  (`monto_pagado` acumulable): esta feature usa partes de un solo cobro, `pendiente → pagado`.
- Dividir un egreso en "partes iguales con montos que no cuadran al peso" con céntimos: CLP no tiene
  subdivisión en circulación.
- Notificaciones o recordatorios de cobro.
- Sincronización con servidores o entre dispositivos.

---

## 8. Definition of Done

1. `spec.md` → `plan.md` → `tasks.md` creados y coherentes entre sí.
2. Ejecutar `tasks.md` casilla por casilla (marcar solo con verificación real).
3. `./gradlew lint` — cero advertencias.
4. `./gradlew testDebugUnitTest` — verde (JVM, sin dispositivo).
5. `./gradlew connectedCheck` — verde (requiere emulador activo; se ejecuta al final).
6. `./gradlew assembleDebug` — build válido.
7. Crear `doc/012-division-gastos.md` (registro de memoria obligatorio, `.agent.md` §7).
8. Sincronizar `spec/constitution/data-model.md` (ERD + diccionario + invariantes + DDL del Apéndice A) y
   `tech-stack.md` si hubo cambios, en el mismo cambio que el código.
9. Commit Conventional Commits, uno por unidad lógica (p. ej. `feat(gastos-compartidos): ...`).

---

## 9. Trazabilidad

- Registro de memoria asociado: `doc/012-division-gastos.md` (obligatorio al cerrar, ver `.agent.md` §7).
- Hito en roadmap: "Gastos Compartidos y Asignación de Deudas" (`roadmap.md` §2, Fase 3 — Entregable 3).
- Predecesor: `spec/features/011-contactos/` (completada; le transfiere CA-9/CA-10 del historial por contacto).
