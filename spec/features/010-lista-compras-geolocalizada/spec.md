# spec.md — Especificación de Funcionalidad

> **Identificación:** 010-lista-compras-geolocalizada
> **Fecha de creación:** 19/10/2026 | **Responsable:** GG
> **Estado:** Borrador (pendiente de aprobación)
>
> **Propósito de este documento:** Definir QUÉ debe hacer la feature y CÓMO se demuestra que funciona,
> SIN especificar cómo se implementa (eso va en `plan.md`). Si aquí aparece un nombre de framework,
> el documento está fuera de su alcance.

---

## 1. Descripción Detallada

La Feature 010 agrega al mapa de LocalCash la posibilidad de crear **Lugares de Compra**: el usuario
mantiene presionado un punto del mapa y el sistema abre un formulario para registrar un nuevo lugar de
compra (tipo de lugar predefinido o personalizado, gasto promedio estimado **opcional** y una lista de
items asociada). Cada lugar de compra se muestra en el mapa con un pin de color distinto al de los
movimientos (Feature 009). Al tocar un pin de compra se abre un popup con el tipo, el gasto estimado
(o "Sin gasto estimado"), la cantidad de items y un botón "Ver detalle"; mantener presionado el pin
también abre el detalle. Esa actividad gestiona la lista completa (agregar, eliminar, tachar items como
comprados) y permite eliminar el lugar de compra tras una confirmación de seguridad.

**Comportamiento esperado (narrativa):**

1. El usuario abre la pestaña "Mapa" y mantiene presionado un punto del mapa.
2. El sistema abre un diálogo/formulario para crear un nuevo Lugar de Compra, prellenado con las
   coordenadas del punto seleccionado.
3. El usuario elige el tipo de lugar de un Spinner (Supermercado, Farmacia, Ferretería, Panadería,
   Verdulería u Otro), puede agregar o eliminar tipos personalizados, ingresa opcionalmente un gasto
   promedio estimado y agrega una lista de items (nombre y cantidad) desde el mismo diálogo.
4. Al guardar, el sistema persiste el lugar de compra con sus coordenadas, el gasto (o `NULL` si no
   se ingresó) y sus items asociados.
5. En el mapa, el lugar de compra aparece con un pin de color distinto al de los movimientos.
6. Al tocar el pin de compra, aparece un popup con un resumen (1-2 items) y un botón "Ver detalle".
7. Al tocar "Ver detalle", el sistema abre una actividad con un RecyclerView que permite gestionar la
   lista completa de items (agregar, eliminar, marcar como comprado).
8. Al eliminar una lista de compra, el sistema muestra un diálogo de confirmación antes de borrar.

> *Guía de redacción:* Escribe en tercera persona y tiempo presente ("el sistema valida...", no
> "validaremos..."). Describe comportamiento observable desde fuera, no mecanismos internos.

---

## 2. Contexto de Negocio

- **¿Por qué ahora?** Es el siguiente paso tras la Feature 009 (geolocalización de movimientos): pasar
  de marcar puntos en el mapa a gestionar listas de compras asociadas a lugares geográficos.
- **Objetivo de la misión que atiende:** organizar las compras del usuario con contexto geográfico,
  de forma **100% offline** y con privacidad absoluta (`mission.md` §1-§2).
- **Valor / problema que resuelve:** el usuario puede planificar compras por lugar (ej. "lo que necesito
  del supermercado cerca de casa"), ver en el mapa dónde está cada lista y controlar qué items ya
  compró y cuáles faltan.
- **Usuarios afectados:** todo usuario autenticado que registre movimientos y quiera organizar compras
  por lugar geográfico.
- **Dependencias:**
  - Depende de la Feature 007 (`007-cuentas`): esquema de base de datos base y persistencia local.
  - Depende de la Feature 008 (`008-historial`): panel de registro y navegación principal.
  - Depende de la Feature 009 (`009-geolocalizacion-map`): reutiliza el contrato de pines de mapa,
    el `MapView` de osmdroid y las teselas preempaquetadas; define un segundo color de pin para los
    lugares de compra.

---

## 3. Criterios de Aceptación

> *Formato:* Dado [contexto] / Cuando [acción] / Entonces [resultado observable].

| ID   | Criterio de Aceptación | Estado |
|------|------------------------|--------|
| CA-1 | **Dado** la pestaña "Mapa" abierta, **cuando** el usuario mantiene presionado un punto del mapa, **entonces** el sistema abre un diálogo/formulario para crear un nuevo Lugar de Compra con las coordenadas del punto seleccionado. | ⏳ Pendiente |
| CA-2 | **Dado** el diálogo de creación abierto, **cuando** el usuario elige un tipo del Spinner (predefinido o personalizado), **entonces** el sistema acepta el tipo; además puede agregar tipos personalizados persistentes y eliminar solo los personalizados. El gasto promedio es opcional: si el campo queda vacío, se guarda `NULL`; si se ingresa, se guarda como entero CLP ≥ 0. | ⏳ Pendiente |
| CA-3 | **Dado** el diálogo de creación abierto, **cuando** el usuario agrega items (nombre y cantidad ≥ 1) y guarda el lugar, **entonces** el lugar de compra se persiste con sus coordenadas y sus items asociados en una sola transacción. | ⏳ Pendiente |
| CA-4 | **Dado** hay lugares de compra registrados, **cuando** el usuario abre la pestaña "Mapa", **entonces** ve un pin por cada lugar de compra con un color/ícono distinto al de los movimientos (`@color/mapa_pin_compra`). | ⏳ Pendiente |
| CA-5 | **Dado** un pin de compra en el mapa, **cuando** el usuario lo toca, **entonces** aparece un InfoWindow con "Tipo: [tipo]", "Gasto estimado: $[monto]" (o "Sin gasto estimado"), "[N] items en la lista" y un botón "Ver detalle". | ⏳ Pendiente |
| CA-6 | **Dado** el popup de un pin de compra, **cuando** el usuario toca "Ver detalle", **entonces** se abre una actividad con un RecyclerView para gestionar la lista completa de items (agregar, eliminar, tachar como comprado). | ⏳ Pendiente |
| CA-7 | **Dado** una lista de compra en la actividad de detalle, **cuando** el usuario elige eliminarla, **entonces** aparece un diálogo de confirmación para evitar eliminar por error, y solo si confirma se borra el lugar y sus items. | ⏳ Pendiente |
| CA-8 | **Dado** un pin de Lugar de Compra en el mapa, **cuando** el usuario lo mantiene presionado, **entonces** se abre la actividad de detalle con el id del lugar. El long press en mapa vacío sigue abriendo el diálogo de creación. | ⏳ Pendiente |

**Requisitos no funcionales aplicables:**
- **Offline:** 100% local (Room en SQLite), sin permiso `INTERNET`, sin llamadas de red.
- **UI:** cero textos o colores hardcodeados (solo `strings.xml` y `colors.xml`); ViewBinding obligatorio en todas las Activities.
- **Persistencia:** migraciones no destructivas del esquema (3 → 4 y 4 → 5), sin pérdida de datos
  existentes. La 4 → 5 hace nullable `gastoPromedio`.

---

## 4. Casos Borde a Considerar

| # | Caso borde | Comportamiento esperado | Resuelto |
|---|------------|-------------------------|----------|
| E-1 | Long press sobre un punto sin coordenadas válidas (fuera del área de teselas). | El sistema permite crear el lugar con las coordenadas del punto; el mapa muestra un lienzo navegable vacío fuera del área empaquetada, sin bloquear. | [ ] |
| E-2 | Diálogo de creación sin tipo de lugar seleccionado. | El sistema impide guardar y muestra un aviso; el Spinner siempre ofrece al menos los predefinidos. | [ ] |
| E-3 | Lista de items vacía al guardar el lugar. | El lugar se guarda sin items (lista vacía permitida); el InfoWindow mostrará "0 items en la lista". | [ ] |
| E-4 | Item con nombre vacío o cantidad inválida (0 o negativa). | El sistema rechaza el guardado y muestra un aviso; los ítems válidos no se persisten a medias. | [ ] |
| E-5 | Eliminar lugar de compra sin confirmación. | Nunca se elimina sin que el usuario confirme en el diálogo de seguridad. | [ ] |
| E-6 | Rotación de pantalla con el diálogo de creación abierto. | El sistema conserva los datos ya ingresados en el diálogo (estado de instancia). | [ ] |
| E-7 | Muchos lugares de compra (decenas). | El mapa renderiza todos los pines de compra sin bloqueos perceptibles. | [ ] |
| E-8 | Tocar un pin de movimiento y un pin de compra superpuestos. | El sistema distingue y abre el popup correspondiente al origen de cada pin (color distinto). | [ ] |
| E-9 | Long press sobre un pin de compra o de movimiento existente. | No abre el diálogo de creación: el pin de compra abre el detalle y el de movimiento consume el gesto. El diálogo solo aparece en mapa vacío. | [ ] |
| E-10 | Gasto estimado vacío al guardar. | Se persiste `NULL`; el InfoWindow muestra "Sin gasto estimado" en lugar de `$0`. | [ ] |

---

## 5. Fuera de Alcance de esta Feature

- **Sincronización o respaldo en la nube** de las listas de compra.
- **Geocodificación inversa** (resolver coordenadas a dirección legible): las coordenadas se guardan, la dirección queda como extensión futura.
- **Compartir listas de compra** con otros usuarios o contactos.
- **Edición del tipo de lugar o gasto promedio** tras crear el lugar (solo se permite editar/eliminar items en la actividad de detalle y eliminar el lugar completo).
- **Compra grupal o desglose de deudas** (Feature 003/004): este lugar de compra es una lista personal, no una compra grupal.

---

## 6. Trazabilidad

- **Registro de memoria asociado:** `doc/010-lista-compras-geolocalizada.md` (obligatorio al cerrar,
  ver `.agent.md` §7).
- **Hito en roadmap:** Fase 2 — "Lista de Compras Geolocalizada" (En Progreso, Media).
- **Esquema de datos:** `spec/constitution/data-model.md` — nuevas tablas `lugar_compra` e `item_compra`
  (uno-a-muchos); se agrega en el mismo cambio que el código (no en este documento).
- **Deriva a:** `plan.md` (CÓMO) y `tasks.md` (ejecución).
- **Proviene de:** Feature 009 (`009-geolocalizacion-map`), que define el contrato de pines y el mapa de
  lectura que esta feature reutiliza con un color distinto.
