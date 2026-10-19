# plan.md — Plan Técnico de Implementación

> **Identificación:** 009-geolocalizacion-map | **Fecha:** 19/10/2026 | **Autor:** GG
> **Estado:** Implementado (Fases 1-4). Cierre parcial: falta **T0** (teselas offline, tarea manual con
> MOBAC) y la verificación manual/`connectedCheck` en emulador. Sin Pull Request.
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

| Archivo / Módulo | Rol en la feature | Acción | Complejidad estimada |
|---|---|---|---|
| `gradle/libs.versions.toml` + `app/build.gradle` | osmdroid 6.1.20; `app/schemas` expuesto en los assets del variant `debug` para `MigrationTestHelper` | MODIFICAR | BAJA |
| `app/src/main/AndroidManifest.xml` | Permisos de ubicación y registro de `MapaPickerActivity` (sin `INTERNET`) | MODIFICAR | BAJA |
| `app/src/main/assets/tiles/` | Teselas OSM preempaquetadas de Santiago (zoom 10-14), offline — **pendiente (T0)** | CREAR | MEDIA |
| `ui/mapa/TeselasLocales.java` | `TileSource` propio que lee las teselas desde `assets/` (sin red) | CREAR | MEDIA |
| `data/entities/Transaccion.java` | `latitud`, `longitud`, `direccionLugar` (nullable) e índice declarado | MODIFICAR | MEDIA |
| `domain/model/Transaccion.java` | Propaga `Ubicacion` en `toEntity`/`fromEntity` | MODIFICAR | MEDIA |
| `domain/model/Ubicacion.java` | Value object de coordenadas (lat, lon, dirección) | CREAR | BAJA |
| `domain/error/ErrorCode.java` | Nuevo código `UBICACION_INVALIDA` | MODIFICAR | BAJA |
| `domain/service/TransaccionService.java` | La validación geo vive en `Ubicacion` (value object); aquí no se requieren cambios | SIN CAMBIOS | BAJA |
| `data/db/AppDatabase.java` | `version = 3` y `MIGRATION_2_3` (`ALTER TABLE`, no destructiva) e índice parcial `idx_transacciones_geo` | MODIFICAR | ALTA |
| `data/dao/TransaccionDao.java` | `getConUbicacion()` (ambas coordenadas, `ORDER BY fecha DESC`); la caja envolvente queda para 010 | MODIFICAR | MEDIA |
| `ui/mapa/MapaPickerActivity.java` | Activity de mapa que deja fijar un pin y devuelve coordenadas | CREAR | ALTA |
| `ui/mapa/MapaViewModel.java` | Expone los movimientos geolocalizados al mapa (modo lectura) | CREAR | MEDIA |
| `ui/mapa/MapaLecturaController.java` | `MapView` de la pestaña Mapa: teselas, zoom, pines, popup y estado vacío | CREAR | MEDIA |
| `ui/mapa/PinMapa.java` | Tinte del pin según origen (contrato de color reutilizable por 010) | CREAR | BAJA |
| `ui/cuentas/CuentasActivity.java` | Abre la Activity de selección desde el interruptor y puebla la pestaña "Mapa" | MODIFICAR | MEDIA |
| `ui/cuentas/TransaccionViewModel.java` | Persistir las coordenadas al registrar el movimiento | MODIFICAR | MEDIA |
| `res/layout/activity_mapa.xml` | Vista de mapa + confirmación/cancelación | CREAR | MEDIA |
| `res/layout/view_mapa.xml` | `MapView`, zoom, atribución y mensaje de estado vacío | MODIFICAR | MEDIA |
| `res/layout/mapa_info_window.xml` | Popup de los pines (concepto, monto y fecha) | CREAR | BAJA |
| `res/layout/bottom_sheet_transaccion.xml` | Confirmación "Ubicación fijada" en el panel | MODIFICAR | BAJA |
| `res/values/strings.xml` | Cadenas del selector, del mapa y de la confirmación | MODIFICAR | BAJA |
| `res/values/colors.xml` | `mapa_pin_movimiento` (contrato de color de pines) | MODIFICAR | BAJA |
| `res/drawable/ic_mapa_pin.xml`, `ic_zoom_in.xml`, `ic_zoom_out.xml` | Iconografía del mapa | CREAR | BAJA |
| `app/schemas/com.localcash.data.db.AppDatabase/3.json` | Esquema exportado de la versión 3 | CREAR | BAJA |
| `test/.../UbicacionTest.java` | Rango, "ambas o ninguna", `crear()`, `vacia()` (dominio puro) | CREAR | BAJA |
| `test/.../TransaccionDaoGeoTest.java` | Persistencia geográfica (Room in-memory) | CREAR | MEDIA |
| `test/.../MigracionV2V3Test.java` | Migración 2→3 con `MigrationTestHelper` (Robolectric) | CREAR | MEDIA |

**Contrato de datos / interfaz expuesta:**
- **Entrada (desde el panel):** al activar "Geolocalizar", `CuentasActivity` lanza
  `MapaPickerActivity`; el resultado es `{latitud, longitud}` o "sin selección".
- **Salida de la Activity:** `RESULT_OK` con extras `latitud`/`longitud` (ambos o ninguno) y
  `direccion` (hoy siempre `null`), o `RESULT_CANCELED`.
- **Persistencia:** `Transaccion` con `latitud`/`longitud` (ambos `NULL` o ambos con valor) y
  `direccionLugar` opcional.
- **Errores posibles:** coordenada fuera de rango → no se persiste; media coordenada → rechazada
  (nunca se guarda a medias).

---

## 2. Decisiones de Arquitectura

1. **Motor de mapas: osmdroid 6.1.x (OpenStreetMap).**
   - Opciones consideradas: osmdroid, MapLibre Native, Google Maps Android SDK, Mapbox SDK.
   - Decisión: **osmdroid** (`tech-stack.md` §2, re-confirmado el 2026-10-19).
   - Justificación: 100% offline con teselas **preempaquetadas**, sin Play Services ni credenciales; API
     Java directa (`MapView`/`Marker`) suficiente para pines y un pin selector.
   - Consecuencia aceptada: el APK crece (~30-50 MB) por las teselas de Santiago; el mapa se limita a la
     Región Metropolitana (zoom 10-14) y **no se declara `INTERNET`**.

2. **La selección de pin vive en una Activity separada, no dentro del panel.**
   - Opciones consideradas: (a) Activity de mapa que devuelve coordenadas; (b) `MapView` embebido en el
     `BottomSheet`.
   - Decisión: **(a) Activity separada** que retorna por `setResult`.
   - Justificación: el `MapView` de osmdroid tiene ciclo de vida propio y no encaja bien dentro de un
     `BottomSheetDialog`; una Activity aísla el ciclo de vida y simplifica el retorno del resultado vía
     `ActivityResultLauncher`.
   - Consecuencia aceptada: el usuario sale del panel momentáneamente; al volver, el panel permanece
     abierto con el estado previo.

3. **La pestaña "Mapa" es de solo lectura.**
   - Decisión: muestra un `MapView` con un pin por movimiento geolocalizado; sin alta/baja de pines.
   - Justificación: acota el alcance; la gestión de pines de la lista de compras es la Feature 010.
   - Consecuencia aceptada: para "geolocalizar" hay que pasar por el panel de registro.

4. **Contrato de color de pines por origen.**
   - Decisión: los pines de **movimientos** usan un color; los de **lista de compras** usarán otro
     (reservado para 010). El color se centraliza en un recurso/constante único.
   - Justificación: requirement explícito del usuario; 010 lo reutilizará sin redefinirlo.

5. **"Ambas o ninguna" y rango de coordenadas se validan en el dominio; la consulta y el mapeo cierran
   las fugas, no un `CHECK` de Room.**
   - Opciones consideradas: (a) `CHECK` en la tabla (como en el DDL canónico); (b) validación en
     dominio/repositorio.
   - Decisión: **(b)**, con tests propios. `Ubicacion.crear` es la **única** validación de escritura;
     `TransaccionDao.getConUbicacion()` exige ambas columnas no nulas y `Transaccion.fromEntity()`
     lanza `IllegalArgumentException` ante una fila con una sola coordenada. Room **no expone**
     `CHECK` en `@Entity`, y ya hay precedente (la máquina de estados de deudas se hace cumplir en el
     repositorio, `AGENTS.md`).
   - Justificación: mantiene la fuente de verdad única del DDL en `data-model.md` y evita una validación
     silenciosamente ausente por usar Room.
   - Consecuencia aceptada: si en el futuro se escribe por fuera del repositorio, la regla no la protege
     el motor; la fila corrupta **no se muestra** en el mapa y **falla ruidosamente** al mapear a dominio
     (cubierto por `TransaccionDaoGeoTest`).

6. **Migración no destructiva 2 → 3.**
   - Decisión: `ALTER TABLE transacciones ADD COLUMN` para `latitud`, `longitud` y `direccion_lugar`
     (nullable), subir `version` de Room a 3 y crear el índice parcial `idx_transacciones_geo`.
   - Justificación: `.agent.md` §6.11 prohíbe `fallbackToDestructiveMigration()` y `DROP TABLE`; nunca
     se pierde información financiera.
   - Consecuencia aceptada: se debe escribir y testear `MigracionV2V3Test`.

7. **Sin dependencias nuevas más allá de osmdroid.**
   - Decisión: no se añade `play-services-location`; la posición actual (para centrar el mapa) se obtiene
     con el framework de Android (`LocationManager`), ya sin dependencias.
   - Justificación: pilar 100% offline y regla de registrar toda dependencia en `tech-stack.md`.

8. **Se implementa solo la consulta de lectura; la caja envolvente queda como punto de extensión.**
   - Decisión: `getConUbicacion()` devuelve todos los movimientos con ambas coordenadas, apoyada en el
     índice parcial `idx_transacciones_geo`. La consulta por caja envolvente
     (`WHERE latitud BETWEEN ? AND ? AND longitud BETWEEN ? AND ?`) **no** se implementa en 009: el mapa
     de lectura muestra todos los pines y el filtro por viewport no es un requisito de esta feature.
   - Consecuencia aceptada: el índice y el contrato quedan listos para que la Feature 010 los consuma sin
     redefinirlos.

9. **Teselas preempaquetadas offline (Santiago de Chile), sin permiso `INTERNET`.**
   - Opciones consideradas: (a) descargar y cachear teselas en runtime (requiere `INTERNET`);
     (b) **empaquetar teselas** de la Región Metropolitana en `assets/` (offline-first estricto).
   - Decisión: **(b)**.
   - Justificación: mantiene el pilar "100% offline" sin excepciones; el mapa se explora desde el primer
     arranque sin conexión y no depende de la política de uso de un servidor de teselas en runtime.
   - Consecuencia aceptada: cobertura limitada a Santiago (zoom 10-14) y ~30-50 MB más de APK; fuera del
     área empaquetada se verá un lienzo vacío navegable (no un error).

10. **`direccionLugar` se persiste como `null` hasta que exista geocodificación inversa offline.**
    - Decisión: la columna `direccion_lugar` viaja siempre como `null` en esta feature. La
      geocodificación inversa **offline** (coordenada → dirección legible) queda **fuera del alcance**.
    - Justificación: resolverla offline exige empaquetar un dataset de direcciones (peso adicional del
      APK) y no es crítica para el objetivo (fijar el punto del movimiento); la columna queda como punto
      de extensión para una feature futura.
    - Consecuencia aceptada: la UI muestra la coordenada, no una dirección del lugar.

### Estrategia de teselas offline

| Aspecto | Definición |
|---|---|
| Origen | OpenStreetMap (© OpenStreetMap contributors) |
| Región | Santiago de Chile / Región Metropolitana |
| Bounding box (aprox.) | lat **-33.45 .. -33.30**, lon **-70.70 .. -70.55** (a refinar al generar) |
| Zoom | **10-14** |
| Empaquetado | `app/src/main/assets/tiles/{z}/{x}/{y}.png` (esquema XYZ, compatible con osmdroid) |
| TileSource | Propio (`ui/mapa/TeselasLocales.java`), apuntando a `file:///android_asset/tiles/` |
| Herramienta (build-time) | **MOBAC** (Mobile Atlas Creator) o script propio contra la API de teselas de OSM |
| Peso estimado | ~30-50 MB en el APK |
| Atribución | Obligatoria en la UI: "© OpenStreetMap contributors" (cadena en `strings.xml`, sin hardcodear) |
| Reglas | Respetar la política de uso de OSM al **generar** las teselas; **nunca** descargar en runtime |

---

## 3. Riesgos Identificados

| # | Riesgo | Probabilidad | Impacto | Mitigación | Dueño |
|---|--------|--------------|---------|------------|-------|
| 1 | Migración 2→3 mal escrita → pérdida de datos | Media | Alto | `ALTER TABLE` nullable + `MigracionV2V3Test` con datos sembrados | GG |
| 2 | Ciclo de vida del `MapView` de osmdroid (onResume/onPause/onDestroy) mal gestionado → fugas o crash | Media | Alto | Encapsular el ciclo en la Activity; `onDetach`/`detach()` según la guía de osmdroid | GG |
| 3 | Fuera del área empaquetada el mapa aparece vacío | Media | Medio | Abrir centrado en Santiago; documentar la cobertura; mostrar un lienzo navegable sin error | GG |
| 4 | Permiso de ubicación denegado bloquea el flujo | Baja | Alto | Best-effort: abrir el mapa sin centrar; nunca impedir el registro (CA-6/E-1) | GG |
| 5 | `CHECK` de `data-model.md` no reproducible en Room → divergencia silenciosa | Media | Medio | Enforcar en dominio/repositorio + tests; documentar la divergencia decidida | GG |
| 6 | Peso del APK por teselas y atribución OSM incompleta | Media | Medio | Acotar bbox/zoom (RM, 10-14) y mostrar la atribución "© OpenStreetMap contributors" (cadena en recursos) | GG |

**Criterio de abortar/replantear:** si la integración de osmdroid no permite operar de forma razonable
sin red (caché inutilizable) o si exige credenciales/Play Services, se detiene la implementación y se
revisa la decisión de `tech-stack.md` §2 antes de continuar.

---

## 4. Dependencias Externas

| Dependencia | Tipo | Estado del acuerdo | Plan B si falla |
|---|---|---|---|
| osmdroid 6.1.x | Librería | Registrada en `tech-stack.md` §2 | MapLibre Native (registrada como candidata descartada) |
| Teselas OSM **preempaquetadas** (Santiago, zoom 10-14) | Dato (assets del APK) | A generar en **T0** (MOBAC o script) | Reducir bbox/zoom; proveedor alternativo de tiles raster |
| Herramienta de empaquetado (MOBAC o script con API de OSM) | Herramienta de build (no runtime) | A definir en T0 | Script propio de descarga |
| `android.location.LocationManager` | Framework Android | Disponible (sin dependencia) | Sin centrado inicial del mapa |

**Requisitos previos del entorno:** permisos de ubicación en runtime; emulador con "ubicación simulada"
para las pruebas. Ninguna credencial real en documentos.

---

## 5. Estrategia de Verificación

| Criterio | Cómo se verifica |
|---|---|
| CA-1, CA-2 | Prueba manual en emulador: activar el interruptor y fijar un pin |
| CA-3, CA-4, CA-7 | Tests de dominio (`Ubicacion`) + `TransaccionDaoGeoTest` |
| CA-5 | Prueba manual en la pestaña "Mapa" con movimientos sembrados |
| CA-6 | Prueba manual denegando el permiso de ubicación |
| CA-8 | Revisión del contrato de color + prueba manual |
| Migración | `MigracionV2V3Test` (Robolectric + `MigrationTestHelper`) |
| Offline | Prueba en modo avión **desde el primer arranque**: el mapa y los pines se ven sin conexión |

**Nota de infraestructura de test:** `MigrationTestHelper` lee los esquemas exportados desde los assets.
Robolectric resuelve los assets del variant `debug` (no del source set `test`), por eso `app/build.gradle`
añade `app/schemas` a `sourceSets.debug.assets`; el release no los incluye. `runMigrationsAndValidate`
ignora la cláusula `WHERE` del índice parcial al comparar, por lo que el índice creado por la migración
no rompe la validación contra `3.json`.

**Orden antes de cerrar (Definition of Done de `tasks.md`):** `./gradlew lint` → `testDebugUnitTest`
→ `connectedCheck` → `assembleDebug` (`.agent.md` §3).

---

## 6. Dependencias entre features

- **Depende de:** 007 (`007-cuentas`) y 008 (`008-historial`).
- **Habilita a:** 010 (`010-lista-compras`) — reutiliza el contrato de pines y la consulta geográfica.

---

## 7. Nota de numeración (resuelta)

La convención del proyecto comparte la numeración de tres dígitos entre `spec/features/NNN-*` y
`doc/NNN-*`. Existía `doc/009-actualizacion-diagrama.md` (registro de mantenimiento completado), que
colisionaba con el registro de memoria de esta feature.

**Resuelto (2026-10-19):** se renombró a `doc/011-actualizacion-diagrama.md` (dejando `009` para esta
feature y `010` reservado para la lista de compras) y se actualizaron sus referencias internas. El
registro de memoria de esta feature es, por tanto, `doc/009-geolocalizacion-map.md`.

## 8. Nota de nombres (resuelta)

Al iniciar la Fase 2 se fijó el nombre de los artefactos del selector de ubicación, que difiere del
borrador inicial del plan:

| Borrador | Definitivo |
|---|---|
| `ui/mapa/SeleccionarUbicacionActivity.java` | `ui/mapa/MapaPickerActivity.java` |
| `res/layout/activity_seleccionar_ubicacion.xml` | `res/layout/activity_mapa.xml` |

**Motivo:** nombre más corto y explícito de la función (elegir un punto en el mapa) indicado por el
usuario al ejecutar T6; se actualizan el plan y `tasks.md` para no dejar referencias colgando.

