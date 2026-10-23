# doc/009-geolocalizacion-map.md — Registro de memoria: pines del mapa y geolocalización de movimientos

## Ficha de la tarea

| Campo | Valor |
|---|---|
| **Fecha** | 2026-10-23 |
| **Feature** | `spec/features/009-geolocalizacion-map/` |
| **Alcance** | Mapa MapLibre (alta/edición/borrado/cierre de pines, lista de cosas), selección de ubicación de un movimiento y persistencia atómica movimiento + pin vinculado. Migración Room 2 → 3 (`LUGARES`, `ITEMS_LUGAR`, coordenadas en `transacciones`). |
| **Estado** | Completado. `docs(sdd)` que preceden: Fases 0–5 entregadas por commit. |
| **Responsable** | Agente SDD + verificación manual en emulador |

## Qué se entregó

- **Mapa interactivo** (pestaña "Mapa"): MapLibre Native 13.4.0 con teselas vectoriales de
  OpenFreeMap/OpenStreetMap (`styles/liberty`). Atribución textual en pantalla
  (`© OpenStreetMap contributors · © OpenFreeMap`); logo y atribución nativos desactivados
  **en dos momentos** (arranque y después de cargar el estilo) para evitar el artefacto de la marca
  de agua reapareciendo tras el estilo.
- **CRUD de pines**: pulsación larga abre el formulario (tipo → emoji y nombre por defecto), detalle
  con origen ("Pin independiente" / "Vinculado a un movimiento"), edición, "Mover en el mapa",
  cerrar/reabrir y eliminar con confirmación. Lista de cosas por pin (agregar, marcar, editar,
  eliminar, tachado de completados).
- **"Mis lugares"**: lista de pines activos (los cerrados salen de la lista, quedan atenuados en el
  mapa y se reabren desde el detalle).
- **"Mi ubicación"**: `LocationManagerCompat.getCurrentLocation` (15 s de timeout), sin Play
  Services; manejo de permiso denegado con aviso y mapa operativo.
- **Geolocalización de movimientos** (pestaña "Movimientos"): interruptor "Geolocalizar" en el
  BottomSheet, `SeleccionarUbicacionActivity` a pantalla completa (toque → marcador → Confirmar),
  resultados por `setResult` y UI con `android:fitsSystemWindows="true"`.
- **Persistencia atómica**: `TransaccionViewModel.registrarTransaccion(..., lat, lon, dir)` inserta
  movimiento, pin vinculado y actualización de saldo dentro de `AppDatabase.runInTransaction`. Sin
  coordenadas (o con selección cancelada) el movimiento se guarda sin pin y sin error.
- **Migración no destructiva 2 → 3**: `ALTER TABLE transacciones ADD COLUMN latitud/longitud/
  direccion_lugar` + `CREATE TABLE LUGARES/ITEMS_LUGAR` + índices Room. Sin `DROP TABLE` ni
  `fallbackToDestructiveMigration()`.

## Decisiones relevantes y su justificación

1. **MapLibre Native 13.4.0 + OpenFreeMap** — ya registrada en `tech-stack.md` por la Fase 0/1 de
   la feature. Sin llaves API; la caché de teselas degrada aceptablemente sin red (CA-17).
2. **El pin vinculado nace completo y mínimo**: nombre = concepto del movimiento ("Movimiento" si
   está vacío), tipo `OTRO`, emoji `📍`, estado `ABIERTO`, sin items. La lista se gestiona después
   desde el detalle, igual que en un pin independiente. Así el `runInTransaction` no duplica lógica
   de formulario.
3. **La selección de ubicación no persiste nada**: solo devuelve coordenadas por
   `setResult(EXTRA_LATITUD, EXTRA_LONGITUD)`. Toda escritura vive en el ViewModel; cancelar deja el
   panel intacto y apaga el interruptor (CA-4).
4. **Índices con convención de Room y sin `CHECK` en el motor**: las anotaciones `@Entity` no
   expresan `CHECK`. Las reglas canon (nombre/emoji no vacíos, dominio de tipos y estado, rango
   WGS84, coordenada atómica) las valida `LugarService`/`Ubicacion` con tests; la unicidad del
   vínculo es `UNIQUE index_LUGARES_transaccion_id` (no parcial: los `NULL` no colisionan, semántica
   equivalente al `ux_lugares_transaccion` canon). Documentado en `data-model.md` §3.9 y §6.
5. **Migración test en JVM (Robolectric)** en lugar de `androidTest/`: la constitución prefiere
   persistencia sin dispositivo; `MigracionV2V3Test` siembra el DDL real de la versión 2 (de
   `app/schemas/2.json`), aplica `MIGRATION_2_3` y verifica datos, tablas, `user_version` y
   `PRAGMA foreign_key_check`. Evitó añadir dependencias de instrumentación no registradas.
6. **Numeración de `doc/`**: el registro de mantenimiento que ocupaba `009`
   (era `009-actualizacion-diagrama.md`) se movió a `doc/010-actualizacion-diagrama.md` para liberar
   el número y respetar `doc/NNN` ↔ `spec/features/NNN` (`.agent.md`).

## Verificación (criterios de aceptación)

Fuente: `spec/features/009-geolocalizacion-map/spec.md` §3, todos con estado **Verificado**.

| CA | Verificación |
|---|---|
| CA-1, CA-2 | Manual: interruptor → Activity de selección → toque → "Punto elegido" → Confirmar → resumen `-33.44069, -70.66736` en el panel. |
| CA-3, CA-16 | Manual + DB: "Compra con ubicacion" $25.000 creó `transacciones` id 1 con coordenadas, `LUGARES` id 4 (`transaccion_id=1`), saldo 750000 → 775000. El detalle del pin muestra "Vinculado a un movimiento" y omite "Mover en el mapa". |
| CA-4 | Manual + DB: movimiento "Sin ubicacion" $1.000 con selección cancelada: switch apagado, sin resumen, fila con `latitud/longitud NULL`, sin pin nuevo, saldo 775000 → 776000. |
| CA-5..CA-7 | Manual: pulsación larga abre formulario con coordenada; tipo define emoji; nombre por defecto = nombre del tipo. |
| CA-8 | Manual + DB agregando/marcando/eliminando ítems ("Manzanas", "Leche", "Peras") desde formulario y detalle. |
| CA-9..CA-12 | Manual: nombre editado ("Feria editada" visible en lista y mapa), posición movida (long-press subsecuente; `latitud/longitud` cambiaron en DB), eliminación con confirmación (0 filas en `LUGARES`/`ITEMS_LUGAR`), cierre/reapertura (desaparece y reaparece en "Mis lugares"). |
| CA-13 | Manual (fase 2): mapa centrado en la posición actual tras conceder permiso. |
| CA-14 | Manual (fase 6): con permiso revocado, toast "Permiso de ubicación denegado. Actívalo en Ajustes…"; el mapa sigue operativo. |
| CA-15 | Manual: tap en fila de "Mis lugares" centra el mapa y abre el detalle. |
| CA-17 | Manual: modo avión; el mapa queda navegable con teselas cacheadas y un pin se creó offline (fila id 5 en `LUGARES`). |
| CA-18 | Automático: `MigracionV2V3Test` (migración 2 → 3 con datos) + migración real verificada en emulador durante las fases 1–2. |

**Casos borde T30 verificados** — permiso denegado (CA-14), rotación paisaje sin crash y tema
cambiado, modo avión con persistencia local (CA-17), **204 pines sembrados y renderizados sin
crash/out-of-memory** (la cámara construida para pocos pines los dibuja superpuestos; el
*clustering* queda como mejora futura, no como defecto), emoji multicódigo (`🏳️‍🌈` conservado
íntegro en TEXT, test `testEmojiMulticodigoSeConserva`) y eliminación del movimiento vinculado
(`SET NULL` automático en `testEliminarMovimientoDejaElPinIndependiente`). El escenario "sin señal
GPS" del emulador queda cubierto por el timeout de 15 s de `LocationManagerCompat` y el manejo de
error del listener.

## Tests

| Suite | Casos | Resultado |
|---|---|---|
| `LugarServiceTest` (JVM pura) | 19 | ✅ |
| `LugarDaoTest` (Robolectric) | 11 (CRUD, bbox, vínculo único, cascada, `foreign_keys`, `foreign_key_check`, emoji multicódigo) | ✅ |
| `ItemLugarDaoTest` (Robolectric) | 6 | ✅ |
| `MigracionV2V3Test` (Robolectric, DDL v2 real) | 4 | ✅ |
| Suite preexistente (Cuenta/Transaccion) | 8 | ✅ |
| `connectedCheck` | instrumentados en emulador | ✅ 0 fallos |

**Validación de esquema con `sqlite3` comandos (`data-model.md` Apéndice A + trampa de proceso):**
DDL Room v3 generado desde `app/schemas/3.json`, ejecutado en `sqlite3`: `PRAGMA foreign_key_check`
limpio; camino de rechazo `UNIQUE constraint failed: LUGARES.transaccion_id`; `ON DELETE SET NULL`
verificado; `ON DELETE CASCADE` verificado; `user_version = 3`.

## Definition of Done

| Comando | Resultado |
|---|---|
| `./gradlew lint` | ✅ (`LogNotTimber` es solo de nivel *default* del proyecto, no error ni warning de bloqueo; el resto de la suite limpia) |
| `./gradlew testDebugUnitTest` | 53 tests, 0 fallos |
| `./gradlew connectedCheck` | 0 fallos en emulador (API 36) |
| `./gradlew assembleDebug` | ✅ |

Commits de la feature (rama `feat/009-mapa`, Conventional Commits de `.agent.md` §5.2):
Fases 0–3 previas; `00eac14` (Fase 4: vinculación movimiento↔pin), `8832fc2` (Fase 5: tests),
y el commit final de cierre (Fase 6: documentación y numeración).

## Archivos principales

- `ui/mapa/`: `MapaController`, `MapaViewModel`, `MapaConfig`, `LugarFormSheet`,
  `LugarDetalleSheet`, `LugaresAdapter`, `ItemsLugarAdapter`, `SeleccionarUbicacionActivity`.
- `ui/cuentas/`: `CuentasActivity`, `TransaccionViewModel`.
- `data/entities|dao`: `Lugar`, `ItemLugar`, `LugarDao`, `ItemLugarDao`, `Transaccion` (coords).
- `data/db/AppDatabase`: entidades + `MIGRATION_2_3`.
- `domain/`: `Lugar`, `ItemLugar`, `TipoLugar`, `EstadoLugar`, `Ubicacion`, `LugarService`.
- `res/layout/`: `view_mapa`, `bottom_sheet_lugar`, `bottom_sheet_detalle_lugar`,
  `bottom_sheet_lugares`, `bottom_sheet_transaccion` (switch + resumen), `item_lugar`,
  `item_lista_lugar`, `activity_seleccionar_ubicacion`.
- `test/`: `LugarServiceTest`, `LugarDaoTest`, `ItemLugarDaoTest`, `MigracionV2V3Test`.
- Docs: `data-model.md` (§3.9, §6, historial), `roadmap.md`, `doc/010-actualizacion-diagrama.md`,
  este registro.

## Lecciones aprendidas

- **`fitsSystemWindows` desde el primer día**: la Activity de selección a pantalla completa se
  solapaba con la status bar; añadirla al layout raíz lo resolvió en un commit. Toda Activity
  fullscreen futura debe aplicarlo desde su creación.
- **`Marker.showInfoWindow(map, mapView)`**: MapLibre
  13.4.0 necesita la referencia al mapa; el listener es `setOnMarkerClickListener`, no
  `addOnMarkerClickListener`.
- **Deshabilitar logo/atribución nativos en dos momentos** (arranque + post-estilo) evita la marca
  de agua fantasma que reaparece con el estilo cargado.
- **Los `NULL` no colisionan en un índice UNIQUE**: permitió cumplir la semántica del índice parcial
  canon sin soporte parcial en Room.
- **Robolectric cubre la migración**: sembrar el DDL real de la versión previa y abrir Room aplicando
  la migración es un test de archivo genuine sin dispositivo, coherente con la constitución.
- **Volver a tocar la hoja abierta por error** (scrim) cierra el BottomSheet perdiendo el estado del
  formulario durante una prueba manual: cuidado con toques superpuestos al automatizar.

## Próximos pasos

- *Clustering* de marcadores cuando el mapa acumule muchos pines en la misma zona (verificado con
  200+ pines: funcional pero visualmente denso).
- Dirección legible (`direccion_lugar`) requiere geocodificación inversa: queda como mejora futura
  (no se encoló a ninguna feature activa).
- Clustering aparte, el *roadmap* continúa con la Feature 010 (compras planificadas/agendamiento),
  que materializa `TRANSACCIONES` y puede reutilizar el flujo de pin vinculado.
