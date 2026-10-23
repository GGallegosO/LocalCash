# plan.md — Plan Técnico de Implementación

> **Identificación:** 009-geolocalizacion-map | **Fecha:** 23/10/2026 | **Autor:** GG
> **Estado:** Borrador (pendiente de aprobación)
>
> **Propósito de este documento:** Definir CÓMO se implementará la feature descrita en `spec.md`:
> componentes, decisiones técnicas, riesgos y dependencias. Es el puente entre el requisito y las
> tareas ejecutables (`tasks.md`).
>
> **Regla:** Ninguna decisión de este archivo puede contradecir `.agent.md` ni `tech-stack.md`.
> Si la contradicción es inevitable, primero se actualiza la constitución con su registro en `doc/`.

---

## 1. Componentes Involucrados

> Todos los archivos de esta tabla están pendientes de implementación. Si aparece uno fuera de la
> lista durante la ejecución, se actualiza este plan con justificación antes de continuar.

| Archivo / Módulo | Rol en la feature | Acción | Complejidad |
|---|---|---|---|
| `gradle/libs.versions.toml` + `app/build.gradle` | Declarar MapLibre Native 13.4.0 y el procesador de anotaciones ya existente | MODIFICAR | BAJA |
| `app/src/main/AndroidManifest.xml` | Permisos `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` y registro de la Activity de selección | MODIFICAR | BAJA |
| `data/entities/Lugar.java` | Entidad Room del pin: tipo, emoji, coordenadas, estado y vínculo opcional a movimiento | CREAR | MEDIA |
| `data/entities/ItemLugar.java` | Entidad Room de la lista de cosas del pin | CREAR | BAJA |
| `data/entities/Transaccion.java` | Añadir `latitud`, `longitud` y `direccionLugar` (nullable) | MODIFICAR | MEDIA |
| `data/dao/LugarDao.java` | CRUD observable de lugares, consulta geográfica y cambio de estado | CREAR | MEDIA |
| `data/dao/ItemLugarDao.java` | CRUD observable de los elementos de un lugar | CREAR | MEDIA |
| `data/dao/TransaccionDao.java` | `insert` devuelve el id generado; consulta de movimientos geolocalizados | MODIFICAR | BAJA |
| `data/db/AppDatabase.java` | Registrar entidades y migración no destructiva 2 → 3 | MODIFICAR | ALTA |
| `domain/model/Ubicacion.java` | Value object de coordenadas WGS84 con validación de rango | CREAR | BAJA |
| `domain/model/TipoLugar.java` | Dominio cerrado de tipos de lugar y emoji por defecto | CREAR | BAJA |
| `domain/model/EstadoLugar.java` | Estados `ABIERTO` / `CERRADO` | CREAR | BAJA |
| `domain/model/Lugar.java` | Modelo de dominio del pin y su lista de cosas | CREAR | MEDIA |
| `domain/model/ItemLugar.java` | Modelo de dominio de un elemento de la lista | CREAR | BAJA |
| `domain/service/LugarService.java` | Validación de nombre, tipo, emoji, coordenadas y transiciones de estado | CREAR | MEDIA |
| `domain/error/ErrorCode.java` | Códigos `LUGAR_INVALIDO`, `LUGAR_NO_ENCONTRADO`, `UBICACION_INVALIDA`, `PERMISO_UBICACION_DENEGADO` | MODIFICAR | BAJA |
| `ui/mapa/MapaController.java` | Encapsula `MapView`, cámara, pines, pulsación larga y ciclo de vida | CREAR | ALTA |
| `ui/mapa/MapaViewModel.java` | Expone lugares y elementos, y ejecuta las operaciones CRUD | CREAR | ALTA |
| `ui/mapa/LugaresAdapter.java` | Lista de lugares activos con emoji, nombre y tipo | CREAR | MEDIA |
| `ui/mapa/ItemsLugarAdapter.java` | Lista de cosas con casilla de completado y acciones | CREAR | MEDIA |
| `ui/mapa/SeleccionarUbicacionActivity.java` | Activity de mapa que devuelve un punto al registro de movimientos | CREAR | ALTA |
| `ui/mapa/LugarFormSheet.java` | Formulario de alta/edición de lugar y de su lista | CREAR | ALTA |
| `ui/mapa/LugarDetalleSheet.java` | Detalle del pin: datos, origen, lista de cosas y acciones de gestión | CREAR | ALTA |
| `ui/cuentas/CuentasActivity.java` | Sustituir el placeholder del mapa, cablear `MapaController` y el flujo "Geolocalizar" | MODIFICAR | ALTA |
| `ui/cuentas/TransaccionViewModel.java` | Persistir coordenadas y crear el pin vinculado en una sola transacción | MODIFICAR | ALTA |
| `res/layout/view_mapa.xml` | Mapa, botones "Mi ubicación"/"Mis lugares" y estado vacío | MODIFICAR | MEDIA |
| `res/layout/activity_seleccionar_ubicacion.xml` | Mapa de selección + confirmar/cancelar | CREAR | MEDIA |
| `res/layout/bottom_sheet_transaccion.xml` | Resumen de la coordenada elegida junto al interruptor "Geolocalizar" | MODIFICAR | BAJA |
| `res/layout/bottom_sheet_lugar.xml` | Formulario de lugar y lista de cosas | CREAR | MEDIA |
| `res/layout/bottom_sheet_detalle_lugar.xml` | Detalle del pin y sus acciones de gestión | CREAR | MEDIA |
| `res/layout/bottom_sheet_lugares.xml` | Lista de lugares registrados | CREAR | MEDIA |
| `res/layout/item_lugar.xml` | Fila de la lista de lugares | CREAR | BAJA |
| `res/layout/item_lista_lugar.xml` | Fila de la lista de cosas | CREAR | BAJA |
| `res/values/strings.xml` + `res/values/colors.xml` | Textos, descripciones de accesibilidad, colores de estado y atribución | MODIFICAR | BAJA |
| `res/drawable/ic_my_location.xml` + `res/drawable/ic_place_list.xml` | Iconos de los controles del mapa | CREAR | BAJA |
| `test/.../LugarServiceTest.java` | Validaciones de dominio y transiciones de estado | CREAR | MEDIA |
| `test/.../LugarDaoTest.java` | CRUD, cascada de elementos y consulta geográfica | CREAR | MEDIA |
| `test/.../ItemLugarDaoTest.java` | Alta, edición y completado de elementos | CREAR | MEDIA |
| `androidTest/.../MigracionV2V3Test.java` | Migración 2 → 3 con datos sembrados y `PRAGMA foreign_key_check` | CREAR | ALTA |

> **Ajuste durante la Fase 3 (justificado):** T19 exigía una hoja de detalle con acciones y lista de
> cosas, pero la tabla original solo nombraba el formulario. Se añadieron
> `ui/mapa/LugarDetalleSheet.java` y `res/layout/bottom_sheet_detalle_lugar.xml` para separar lectura
> y edición: el formulario no se reutiliza como detalle porque en edición oculta la lista inicial
> (su gestión vive en el detalle) y porque el detalle observa `ITEMS_LUGAR` en vivo. `item_lista_lugar.xml`
> ya estaba previsto y se reutiliza en ambas hojas sin duplicar la fila.

**Contrato de datos / interfaz expuesta:**

- **Pin (lugar):** `id`, `nombre`, `tipoLugar`, `emoji`, `descripcion`, `latitud`, `longitud`,
  `estado`, `transaccionId` (nullable) y `items`.
- **Alta desde el mapa:** pulsación larga entrega `{latitud, longitud}`; el formulario construye un
  lugar independiente (`transaccionId = null`) y solo lo persiste al confirmar.
- **Alta desde un movimiento:** al guardar un movimiento con coordenadas se persiste el movimiento y
  se crea un lugar con `transaccionId = <id del movimiento>`. Ambas escrituras ocurren en una sola
  transacción de Room; si falla una, no se confirma la otra.
- **Pin vinculado:** su posición se copia del movimiento y no se puede mover; sí se pueden editar
  nombre, tipo, emoji, descripción y lista.
- **Nombre del pin vinculado:** se toma del concepto del movimiento (o "Movimiento" si está vacío);
  nace de tipo `OTRO`, con emoji `📍`, abierto y sin lista de cosas. La lista se gestiona después
  desde el detalle del pin, igual que en un pin independiente.
- **Pin independiente:** permite editar también su posición.
- **Cerrar/reabrir:** cambia `estado`; no borra el pin ni sus elementos.
- **Errores de dominio:** `LUGAR_INVALIDO` (nombre, tipo, emoji o estado fuera de dominio),
  `LUGAR_NO_ENCONTRADO`, `UBICACION_INVALIDA` y `PERMISO_UBICACION_DENEGADO` (este último se traduce
  en la UI; la lógica de permisos no vive en el dominio).

---

## 2. Decisiones de Arquitectura

1. **Motor de mapas: MapLibre Native 13.4.0 con teselas vectoriales de OpenFreeMap.**
   - Opciones consideradas: MapLibre Native, osmdroid, Google Maps Android SDK, Mapbox SDK.
   - Decisión: **MapLibre Native** (`org.maplibre.gl:android-sdk:13.4.0`) con el estilo
     `https://tiles.openfreemap.org/styles/liberty` sobre datos de OpenStreetMap.
   - Justificación: renderizado vectorial moderno, API Java directa (`MapView`, `MapLibreMap`,
     `LatLng`, `Marker`), gestos nativos de pulsación larga y personalización de marcadores sin
     credenciales de Google ni Mapbox. Es la tecnología que se adopta para el módulo geográfico.
   - Consecuencia aceptada: el mapa base necesita red para teselas no cacheadas. La app conserva el
     funcionamiento local de datos, pines y listas, pero no promete un mapa base completo en modo
     avión. Esta consecuencia se refleja en `tech-stack.md` §2 y en la prueba CA-17.

2. **La pestaña "Mapa" se renderiza dentro del contenedor existente, sin una Activity extra.**
   - Opciones consideradas: (a) `Fragment` con su propio ciclo de vida; (b) `MapaController` que
     encapsula el `MapView` y recibe el ciclo de vida de `CuentasActivity`; (c) Activity dedicada.
   - Decisión: **(b) `MapaController`**.
   - Justificación: el proyecto usa `ViewFlipper` y Activities, no Fragments; un controlador mantiene
     el mapa encapsulado y permite reutilizar la misma lógica de pines en la Activity de selección.
   - Consecuencia aceptada: `CuentasActivity` debe reenviar `onStart`, `onResume`, `onPause`,
     `onStop`, `onDestroy` y `onLowMemory` al controlador.

3. **La selección de ubicación para un movimiento sigue en una Activity separada.**
   - Opciones consideradas: (a) formulario con mapa embebido; (b) Activity que devuelve coordenadas.
   - Decisión: **(b) `SeleccionarUbicacionActivity`** con `setResult`.
   - Justificación: aísla el ciclo de vida del mapa, evita un `MapView` dentro del BottomSheet y
     reutiliza un contrato claro (`latitud`/`longitud` o cancelación).
   - Consecuencia aceptada: el usuario sale momentáneamente del panel; al volver, conserva su estado.

4. **Dos tablas nuevas: `LUGARES` e `ITEMS_LUGAR`.**
   - Opciones consideradas: (a) una sola tabla con la lista serializada como texto/JSON; (b) una tabla
     de lugares y otra de elementos; (c) reutilizar `COMPRAS_PLANIFICADAS`/`ITEMS_COMPRA`.
   - Decisión: **(b) dos tablas relacionadas**.
   - Justificación: la lista de cosas se edita y se marca elemento por elemento; serializarla en un
     campo obligaría a reescribir el blob completo y no permitiría consultas ni integridad. Las tablas
     de compras existentes dependen de un contacto y de una compra grupal, así que no representan un
     pin independiente.
   - Consecuencia aceptada: el borrado de un lugar elimina sus elementos en cascada; la UI gestiona
     dos DAOs coordinados por el ViewModel.

5. **El pin puede ser independiente o estar vinculado a un movimiento.**
   - Decisión: `LUGARES.transaccion_id` es `NULL` para pines independientes y `UNIQUE` cuando tiene
     valor, con `ON DELETE SET NULL`.
   - Justificación: un movimiento genera a lo sumo un pin; si el movimiento se elimina, el lugar y su
     lista de cosas no deben desaparecer sin decisión del usuario.
   - Consecuencia aceptada: al borrar el movimiento, el pin pasa a comportarse como independiente y
     conserva su última ubicación.

6. **El estado del pin es `ABIERTO` o `CERRADO`, con transición reversible.**
   - Decisión: cerrar no elimina; oculta el pin de la lista activa y lo muestra atenuado en el mapa.
   - Justificación: el usuario quiere conservar el recuerdo y la lista, pero dejar de ver el lugar como
     pendiente.
   - Consecuencia aceptada: el repo no expone un borrado lógico genérico; `estado` es parte del
     dominio del lugar y el DAO solo ofrece `cerrar`/`reabrir`.

7. **Las coordenadas del pin son la fuente para el mapa; el movimiento conserva las suyas.**
   - Decisión: al crear un pin vinculado se copian las coordenadas del movimiento. La transacción es
     inmutable, así que no hay edición posterior que las desincronice.
   - Justificación: permite consultar todos los pines con una sola tabla y mantiene el historial
     financiero independiente.
   - Consecuencia aceptada: si una transacción geolocalizada no tiene pin (por un fallo previo), la
     app puede crear el pin desde el detalle del movimiento en una iteración futura; en esta feature
     el flujo normal garantiza ambos.

8. **Migración no destructiva 2 → 3.**
   - Decisión: la migración agrega las columnas de coordenadas a `TRANSACCIONES`, crea `LUGARES` e
     `ITEMS_LUGAR` con sus índices y sube `version` de Room a 3. Prohibido `DROP TABLE` o
     `fallbackToDestructiveMigration()`.
   - Justificación: `.agent.md` §6.11 y `data-model.md` §6 exigen conservar la información financiera.
   - Consecuencia aceptada: se debe probar con datos sembrados y `PRAGMA foreign_key_check` limpio.
   - **Nota de numeración:** el DDL canónico pasa a la revisión 2 (Apéndice A de `data-model.md`). La
     base Room ya está en `version = 2` por la migración 1 → 2 de `CUENTAS.activa` (Feature 007), que
     no forma parte del DDL canónico; por eso la implementación de esta feature se numera 2 → 3. La
     diferencia queda documentada en `data-model.md` §6.

9. **Ubicación con el framework de Android (`LocationManager`), sin Play Services.**
   - Decisión: obtener la posición actual con `LocationManager` y permisos en runtime.
   - Justificación: evita una dependencia no registrada y mantiene la app fuera de Google Play
     Services.
   - Consecuencia aceptada: la precisión y el tiempo de primera fijación dependen del proveedor del
     dispositivo; se trata como best-effort.

10. **Los emojis se renderizan a un bitmap para el marcador del mapa.**
    - Decisión: el tipo de lugar aporta un emoji por defecto y el usuario puede elegirlo de una paleta
      predefinida; el `MapaController` lo dibuja en un `Bitmap` y lo registra con `IconFactory`.
    - Justificación: MapLibre usa iconos de mapa, no vistas Android; dibujar el emoji mantiene el
      marcador nítido y evita depender de `MarkerView`.
    - Consecuencia aceptada: la apariencia exacta del emoji depende de la fuente del sistema; si no se
      puede dibujar, se usa un marcador de respaldo con el color del tipo.

11. **La lista de cosas es un checklist, no una transacción.**
    - Decisión: `ITEMS_LUGAR` guarda descripción y `completado`; no guarda precios ni cantidades en
      esta feature.
    - Justificación: el requisito es recordar qué comprar, no contabilizar el gasto; los precios y el
      reparto ya tienen su propio modelo.
    - Consecuencia aceptada: si más adelante se necesita cantidad o precio, se agrega una columna con
      migración y se actualiza la UI, sin romper el pin.

12. **Sin geocodificación ni rutas.**
    - Decisión: no se resuelven direcciones ni se calculan recorridos.
    - Justificación: mantiene el alcance y evita dependencias/servicios externos.
    - Consecuencia aceptada: el nombre y la descripción del lugar son responsabilidad del usuario.

---

## 3. Riesgos Identificados

| # | Riesgo | Probabilidad | Impacto | Mitigación | Dueño |
|---|--------|--------------|---------|------------|-------|
| 1 | El mapa base depende de teselas online y el modo avión deja un lienzo vacío | Alta | Medio | Cache local de MapLibre, mensaje de degradación y prueba CA-17; los pines siguen operativos | GG |
| 2 | El estilo o el servidor de teselas cambia de URL/disponibilidad | Media | Medio | Centralizar el estilo en una constante y documentar el proveedor; evaluar un estilo local si se vuelve bloqueante | GG |
| 3 | Ciclo de vida del `MapView` en `ViewFlipper` mal gestionado → fugas o crash | Media | Alto | `MapaController` reenvía todo el ciclo de vida y se prueba rotación/cambio de tema | GG |
| 4 | Migración 2 → 3 mal escrita → pérdida de datos | Media | Alto | `ALTER TABLE` + `CREATE TABLE` no destructivos; test con datos sembrados y `foreign_key_check` | GG |
| 5 | Escritura de movimiento y pin vinculado a medias | Media | Alto | Ambas dentro de `AppDatabase.runInTransaction`; probar el camino de error | GG |
| 6 | Muchos pines degradan el mapa | Media | Medio | Consulta por caja envolvente y actualización incremental; clustering solo si la medición lo exige | GG |
| 7 | Emoji no representable en algún dispositivo | Baja | Bajo | Marcador de respaldo y tipo de lugar legible en el detalle | GG |
| 8 | Permiso de ubicación denegado bloquea el flujo | Baja | Alto | Best-effort: aviso y mapa operativo (CA-14/E-1) | GG |
| 9 | Divergencia entre el DDL canónico y Room | Media | Medio | Actualizar `data-model.md` en el mismo cambio y probar la migración; nota de numeración en §2.8 | GG |
| 10 | La lista de lugares muestra pines cerrados o desactualizados | Media | Bajo | `LugarDao` observable por estado y `LiveData`; prueba de cierre/reabrir | GG |

**Criterio de abortar/replantear:** si MapLibre no permite renderizar el mapa con el estilo elegido
en el `minSdk` declarado, o si la integración exige credenciales/Play Services, se detiene la
implementación y se revisa la decisión de `tech-stack.md` §2 antes de continuar.

---

## 4. Dependencias Externas

| Dependencia | Tipo | Estado del acuerdo | Plan B si falla |
|---|---|---|---|
| MapLibre Native 13.4.0 | Librería | Registrada en `tech-stack.md` §2 | Volver a osmdroid con aprobación de constitución; no se adopta en silencio |
| OpenFreeMap (`tiles.openfreemap.org`) | Servicio de teselas y estilo | Disponible sin credenciales | Estilo local mínimo o proveedor alternativo de teselas vectoriales |
| OpenStreetMap | Datos geográficos | Disponible con atribución | Igual que el anterior; la atribución se mantiene visible |
| `android.location.LocationManager` | Framework Android | Disponible (sin dependencia) | Sin centrado automático; el usuario mueve el mapa |
| Room 2.6.1 y procesador existente | Librería | Ya en el proyecto | No aplica; se reutiliza |

**Requisitos previos del entorno:** permisos de ubicación en runtime; emulador con ubicación simulada;
conexión a red para la primera carga de teselas. Ninguna credencial real en documentos.

---

## 5. Estrategia de Verificación

| Criterio | Cómo se verifica |
|---|---|
| CA-1, CA-2, CA-4 | Prueba manual del flujo de registro + test del ViewModel/DAO con y sin coordenadas |
| CA-3 | Test de integración: movimiento geolocalizado crea exactamente un pin vinculado |
| CA-5, CA-6, CA-7 | Prueba manual en emulador: pulsación larga, tipo/emoji y nombre por defecto |
| CA-8 | Test de `ItemLugarDao` + prueba manual de completado |
| CA-9, CA-10 | Test de `LugarDao` + prueba manual de edición y posición bloqueada en pin vinculado |
| CA-11 | Test de borrado en cascada de `ITEMS_LUGAR` + prueba manual con confirmación |
| CA-12 | Test de transición `ABIERTO ↔ CERRADO` y consulta de la lista activa |
| CA-13, CA-14 | Prueba manual con permiso concedido, denegado y GPS simulado |
| CA-15 | Prueba manual de la lista de lugares y centrado de cámara |
| CA-16 | Prueba manual del detalle de un pin vinculado |
| CA-17 | Prueba en modo avión con caché poblada y sin caché |
| CA-18 | `MigracionV2V3Test` con datos sembrados, `foreign_key_check` limpio |
| Rendimiento | Prueba con cientos de pines sembrados y perfilado básico |

**Orden antes de cerrar (Definition of Done de `tasks.md`):** `./gradlew lint` →
`testDebugUnitTest` → `connectedCheck` → `assembleDebug` (`.agent.md` §3).

---

## 6. Dependencias entre features

- **Depende de:** 007 (`007-cuentas`) y 008 (`008-historial`).
- **Habilita a:** cualquier feature futura que necesite lugares, listas o pines; no depende de la
  feature de compras grupales.

---

## 7. Notas de cierre

- **Numeración de memoria:** existe `doc/009-actualizacion-diagrama.md` (registro de mantenimiento).
  Al cerrar esta feature se creará `doc/009-geolocalizacion-map.md`, por lo que antes del cierre se
  debe resolver la colisión de numeración: mover el registro de mantenimiento a `doc/010-*` o
  renumerar la feature según decida el responsable. La decisión se anota en el registro final.
- **Sincronización del ERD:** `spec/constitution/data-model.md` y `doc/009-actualizacion-diagrama.md`
  se actualizan en el mismo cambio que la migración; el Mermaid es la fuente de verdad y el SVG es
  complementario.
- **Alcance de la dependencia:** MapLibre aporta un mapa base online con caché. No se declara que el
  mapa base funcione íntegramente sin red; la funcionalidad financiera y los pines sí.
