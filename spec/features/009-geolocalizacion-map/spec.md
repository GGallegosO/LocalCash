# spec.md — Especificación de Funcionalidad

> **Identificación:** 009-geolocalizacion-map
> **Fecha de creación:** 19/10/2026 | **Última reescritura:** 23/10/2026 | **Responsable:** GG
> **Estado:** Borrador (pendiente de aprobación)
>
> **Propósito de este documento:** Definir QUÉ debe hacer la feature y CÓMO se demuestra que funciona,
> SIN especificar cómo se implementa (eso va en `plan.md`). Si aquí aparece un nombre de framework,
> el documento está fuera de su alcance.

---

## 1. Descripción Detallada

La Feature 009 entrega el **mapa interactivo de LocalCash** y completa la geolocalización de
movimientos que la Feature 008 dejó preparada. El mapa deja de ser una vista de relleno y pasa a ser
un espacio de trabajo donde el usuario puede:

- fijar la ubicación de un movimiento al registrarlo;
- crear **pines referenciales** en cualquier punto del mapa con una pulsación larga;
- clasificar cada pin por **tipo de lugar** (supermercado, farmacia, restaurante, etc.) y asociarle
  un **emoji**;
- añadir al pin una **lista de cosas** (por ejemplo, la lista de compras del supermercado) y marcar
  sus elementos como completados;
- **editar, cerrar/reabrir o eliminar** el pin;
- centrar el mapa en su **ubicación actual**;
- consultar la **lista de lugares** y saltar al pin seleccionado.

Un pin puede estar **vinculado a un movimiento** (cuando nace del flujo "Geolocalizar" del registro)
o ser **independiente** (cuando se crea desde la pestaña "Mapa" con una pulsación larga). Ambos tipos
se gestionan con las mismas acciones, pero el detalle indica su origen.

**Comportamiento esperado (narrativa):**

1. En el panel de registro de un movimiento, el usuario activa el interruptor "Geolocalizar".
2. El sistema abre una vista de mapa y permite elegir un punto.
3. Al confirmar, el sistema vuelve al panel con las coordenadas elegidas; al guardar el movimiento,
   persiste **ambas** coordenadas y crea un pin vinculado a ese movimiento.
4. Si el usuario no confirma un punto, el movimiento se guarda sin coordenadas y sin pin.
5. En la pestaña "Mapa", el sistema muestra el mapa con los pines existentes (vinculados e
   independientes) y los controles "Mi ubicación" y "Mis lugares".
6. Una pulsación larga sobre el mapa abre el formulario de nuevo pin en ese punto.
7. El usuario elige el tipo de lugar, revisa o cambia el emoji, escribe un nombre y, si lo desea,
   agrega elementos a la lista de cosas. El pin solo se crea al confirmar.
8. Al tocar un pin, el sistema abre su detalle con las acciones editar, cerrar/reabrir y eliminar,
   además de la lista de cosas.
9. La lista de lugares muestra los pines creados; al tocar uno, el mapa se centra en él y abre su
   detalle.
10. Los pines cerrados dejan de aparecer en la lista activa, pero siguen visibles en el mapa con un
    tratamiento atenuado y pueden reabrirse.

> *Guía de redacción:* Tercera persona, tiempo presente, comportamiento observable. La tecnología
> concreta del mapa vive en `plan.md` y en `tech-stack.md`.

---

## 2. Contexto de Negocio

- **¿Por qué ahora?** Es el hito "Geolocalización de Movimientos en Mapa" del roadmap (Fase 2,
  prioridad Alta). La Feature 008 dejó el interruptor "Geolocalizar" en la interfaz y trasladó a esta
  feature toda la interacción geográfica.
- **Objetivo de la misión que atiende:** dar contexto espacial a las finanzas personales y permitir
  recordatorios de compra por lugar, manteniendo la información financiera en el dispositivo
  (`mission.md` §1-§2).
- **Valor / problema que resuelve:** el usuario no solo recuerda **cuánto** gastó, sino **dónde**;
  además puede guardar lugares útiles y listas de cosas para una visita futura, sin depender de una
  app de notas o de un servicio en la nube.
- **Usuarios afectados:** todo usuario autenticado que registre movimientos geolocalizados o que use
  la pestaña "Mapa" para organizar sus lugares.
- **Dependencias:**
  - Depende de la Feature 007 (`007-cuentas`): cuentas, movimientos y actualización de saldo.
  - Depende de la Feature 008 (`008-historial`): panel de registro, pestaña "Mapa" y el interruptor
    "Geolocalizar".
  - No depende de la Feature 010 ni de funcionalidades de compras grupales.

---

## 3. Criterios de Aceptación

> *Formato:* Dado [contexto] / Cuando [acción] / Entonces [resultado observable].

| ID    | Criterio de Aceptación | Estado |
|-------|------------------------|--------|
| CA-1  | **Dado** el panel de registro con el interruptor "Geolocalizar", **cuando** el usuario lo activa, **entonces** el sistema abre una vista de mapa para elegir un punto. | ✅ Verificado |
| CA-2  | **Dado** la vista de mapa abierta desde el registro, **cuando** el usuario elige un punto y confirma, **entonces** vuelve al panel con ambas coordenadas y el movimiento queda listo para guardarse con ubicación. | ✅ Verificado |
| CA-3  | **Dado** un movimiento guardado con coordenadas, **cuando** el sistema persiste, **entonces** crea un pin vinculado a ese movimiento y lo muestra en la pestaña "Mapa". | ✅ Verificado |
| CA-4  | **Dado** un movimiento sin punto confirmado, **cuando** el usuario lo guarda, **entonces** el movimiento se guarda sin coordenadas, sin pin y sin error. | ✅ Verificado |
| CA-5  | **Dado** el usuario en la pestaña "Mapa", **cuando** mantiene pulsado un punto del mapa, **entonces** el sistema abre el formulario de nuevo pin con esa coordenada. | ✅ Verificado |
| CA-6  | **Dado** el formulario de nuevo pin, **cuando** el usuario elige un tipo de lugar, **entonces** el sistema muestra el emoji asociado y permite confirmar el pin. | ✅ Verificado |
| CA-7  | **Dado** el formulario de nuevo pin, **cuando** el usuario no escribe nombre, **entonces** el sistema usa el nombre del tipo de lugar como nombre por defecto y no bloquea el guardado. | ✅ Verificado |
| CA-8  | **Dado** el detalle de un pin, **cuando** el usuario agrega, edita o marca como completado un elemento de la lista, **entonces** el cambio se persiste y se refleja al volver a abrir el pin. | ✅ Verificado |
| CA-9  | **Dado** un pin existente, **cuando** el usuario edita su nombre, tipo, emoji o descripción, **entonces** el cambio se guarda y se refleja en el mapa y en la lista de lugares. | ✅ Verificado |
| CA-10 | **Dado** un pin independiente, **cuando** el usuario edita su posición, **entonces** el mapa actualiza el pin; un pin vinculado a un movimiento conserva la posición del movimiento y no permite moverla. | ✅ Verificado |
| CA-11 | **Dado** un pin existente, **cuando** el usuario confirma su eliminación, **entonces** el pin y su lista de cosas desaparecen del mapa y de la lista de lugares. | ✅ Verificado |
| CA-12 | **Dado** un pin abierto, **cuando** el usuario lo cierra, **entonces** deja de listarse entre los lugares activos, permanece visible con estilo atenuado y puede reabrirse. | ✅ Verificado |
| CA-13 | **Dado** el botón "Mi ubicación", **cuando** el usuario lo pulsa y concede el permiso, **entonces** el mapa se centra en su posición actual. | ✅ Verificado |
| CA-14 | **Dado** el permiso de ubicación denegado o sin señal, **cuando** el usuario pulsa "Mi ubicación", **entonces** el sistema informa la situación y el mapa sigue operativo sin bloquearse. | ✅ Verificado |
| CA-15 | **Dado** la lista de lugares, **cuando** el usuario toca un lugar, **entonces** el mapa se centra en su pin y abre su detalle. | ✅ Verificado |
| CA-16 | **Dado** un pin vinculado a un movimiento, **cuando** el usuario abre su detalle, **entonces** el sistema indica que proviene de un movimiento y muestra la información del lugar sin duplicar el pin. | ✅ Verificado |
| CA-17 | **Dado** el mapa sin conexión, **cuando** el usuario lo abre, **entonces** los pines y las listas siguen disponibles y el sistema no se bloquea si las teselas base no pueden descargarse. | ✅ Verificado |
| CA-18 | **Dado** una base con datos de la versión anterior, **cuando** la app se actualiza, **entonces** la migración conserva cuentas y movimientos y crea la estructura de lugares sin pérdida de información. | ✅ Verificado |

---

## 4. Requisitos No Funcionales

| Categoría | Requisito | Verificación |
|-----------|-----------|--------------|
| Privacidad de datos | La información financiera y las listas permanecen en el dispositivo. El mapa base puede solicitar teselas a un proveedor de mapas, pero no se envían datos financieros ni credenciales. | Revisión de dependencias + inspección de tráfico |
| Funcionamiento sin red | Registrar movimientos, consultar pines y editar listas funciona sin conexión. La visualización del mapa base degrada a caché local o lienzo navegable si no hay teselas. | Prueba en modo avión (CA-17) |
| Compatibilidad | Funciona en API 28+ (Android 9 en adelante). | `minSdk` del `build.gradle` |
| Exactitud geográfica | Las coordenadas se guardan como grados WGS84 con rango válido (lat -90..90, lon -180..180). | Tests de dominio + `CHECK` de la tabla |
| Permisos | La ubicación se pide solo al usar "Mi ubicación" o el flujo de geolocalización; su denegación no bloquea el resto de la app. | Prueba manual |
| Persistencia | La migración crea las tablas nuevas y conserva los datos existentes, con `PRAGMA foreign_key_check` limpio. | Test de migración con datos sembrados |
| Rendimiento | El mapa con cientos de pines y la lista de lugares se mantienen fluidos en el hilo principal. | Prueba manual / perfilado |
| Accesibilidad | Cada pin y cada control tiene descripción textual; el emoji no es el único portador de significado. | Revisión de `contentDescription` |
| Atribución | La vista de mapa muestra la atribución exigida por el proveedor de teselas. | Prueba manual |

---

## 5. Casos Borde

| # | Caso | Comportamiento esperado | Estado |
|---|------|-------------------------|--------|
| E-1 | Permiso de ubicación denegado. | "Mi ubicación" muestra un aviso y no mueve la cámara; el mapa sigue usable. | ⏳ |
| E-2 | Ubicación no disponible (interior, sin señal GPS). | Se mantiene la última vista; se informa que no se pudo obtener la posición. | ⏳ |
| E-3 | El usuario abre el mapa del registro pero no confirma ningún punto. | El movimiento se guarda sin coordenadas y sin pin. | ⏳ |
| E-4 | Se obtiene una sola coordenada. | No se persiste media ubicación (regla "ambas o ninguna"). | ⏳ |
| E-5 | Sin conexión al abrir la pestaña "Mapa". | Los pines y listas se leen de la base local; si no hay teselas en caché, se muestra el lienzo navegable sin bloquear. | ⏳ |
| E-6 | Pulsación larga cerca del borde de la pantalla. | El formulario se abre igualmente y conserva la coordenada elegida. | ⏳ |
| E-7 | El usuario crea dos pines en la misma coordenada. | Se permiten; la lista los distingue por nombre y tipo. | ⏳ |
| E-8 | Nombre de pin vacío. | Se usa el nombre del tipo de lugar como valor por defecto. | ⏳ |
| E-9 | Lista de cosas vacía o con muchos elementos. | El pin es válido sin lista; la lista se desplaza sin romper el detalle. | ⏳ |
| E-10 | Se elimina el movimiento al que estaba vinculado un pin. | El pin sobrevive como lugar independiente y conserva su última ubicación. | ⏳ |
| E-11 | El usuario cierra un pin y luego lo reabre. | El pin vuelve a la lista activa sin perder su lista de cosas. | ⏳ |
| E-12 | Rotación o cambio de tema con el mapa abierto. | La cámara y el pin en edición se conservan, o se restauran a un estado equivalente. | ⏳ |
| E-13 | Cientos de pines en pantalla. | El mapa sigue respondiendo; el detalle solo se abre del pin tocado. | ⏳ |
| E-14 | Emoji no representable por la fuente del dispositivo. | Se muestra un marcador de respaldo y el tipo de lugar sigue siendo legible. | ⏳ |

---

## 6. Fuera de Alcance

- **Rutas, navegación giro a giro o cálculo de distancias.**
- **Geocodificación inversa o búsqueda de direcciones por texto:** el nombre del lugar lo escribe el
  usuario.
- **Sincronización, respaldo en la nube o compartición de lugares entre dispositivos.**
- **Precios, cantidades o conversión de la lista de cosas en un gasto:** la lista es un recordatorio,
  no una transacción financiera.
- **Edición de movimientos ya registrados desde el mapa.**
- **Clustering avanzado de pines:** se evaluará solo si el rendimiento lo exige; el alcance actual
  prioriza la claridad de cada pin.
- **Otros proveedores de mapas o estilos alternativos seleccionables por el usuario.**

---

## 7. Trazabilidad

- **Registro de memoria:** `doc/009-geolocalizacion-map.md` (se crea al cerrar la feature).
- **Hito del roadmap:** Fase 2 — "Geolocalización de Movimientos en Mapa" (En Progreso, Alta).
- **Contrato de datos:** `spec/constitution/data-model.md` — `TRANSACCIONES` (coordenadas),
  `LUGARES` (pin, tipo, emoji, estado y vínculo opcional con un movimiento) e `ITEMS_LUGAR` (lista
  de cosas del pin).
- **Tecnología:** `spec/constitution/tech-stack.md` §2 — "Framework de Renderizado Geográfico y
  Mapas".
- **Deriva a:** `plan.md` (CÓMO) y `tasks.md` (ejecución).
- **Proviene de:** Feature 008 (`008-historial`), que trasladó aquí la geolocalización, los permisos,
  la migración y la interacción con el mapa.
