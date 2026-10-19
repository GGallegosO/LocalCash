# doc/009-geolocalizacion-map.md — Registro de Memoria: Geolocalización en Mapa

> **Propósito de este archivo:** Dejar constancia de qué se decidió y qué se hizo en la Feature 009
> (`spec/features/009-geolocalizacion-map/`) y de su estado real: **implementada en JVM, con cierre
> parcial**. Falta empaquetar las teselas offline (T0) y ejecutar la verificación manual/`connectedCheck`
> en emulador. **No se abre Pull Request** hasta completar ambos.

---

## Ficha de la tarea

| Campo | Valor |
|---|---|
| **Fecha** | 2026-10-19 |
| **Tarea ejecutada** | Feature 009 — Geolocalización de movimientos en mapa (osmdroid offline) |
| **Responsable** | GG |
| **Estado** | **Implementada (Fases 1-4).** Cierre parcial: `T0` (teselas) y `connectedCheck` pendientes. Sin PR. |
| **Rama** | `feat/009-geolocalizacion-map` |

---

## Tarea ejecutada (resumen)

Se habilitó el flujo completo de geolocalización de un movimiento y su lectura sobre un mapa
**100 % offline**, sin permiso `INTERNET`:

- **Captura:** el interruptor "Geolocalizar transacción" del panel lanza `MapaPickerActivity`, un mapa
  donde el usuario fija un pin tocando la pantalla. "Confirmar ubicación" devuelve `latitud`/`longitud`
  al panel; "Cancelar" no devuelve nada.
- **Centrado best-effort:** al abrir el selector se intenta centrar en la última ubicación conocida
  (`LocationManager.getLastKnownLocation`, sin listeners ni diálogo). Sin permiso o sin fix, cae a
  Santiago de Chile.
- **Persistencia:** las coordenadas viajan en el value object `Ubicacion`, se validan ("ambas o ninguna"
  y rango WGS84) y se guardan en las columnas nuevas `latitud`/`longitud`/`direccionLugar` (nullable).
  Sin ubicación → las tres columnas quedan `NULL`.
- **Lectura:** la pestaña "Mapa" monta un `MapView` con teselas locales, zoom, atribución OSM y un pin
  por movimiento geolocalizado. Cada pin abre un popup con concepto, monto y fecha; si no hay
  movimientos, se muestra "Sin movimientos geolocalizados".

**Estado del render:** hasta que T0 empaquete las teselas de Santiago, el mapa se verá como un lienzo
vacío navegable. La lógica (selector, pines, popup, estado vacío y persistencia) sí está completa.

---

## Decisiones tomadas

1. **Motor de mapas: osmdroid 6.1.20.**
   *Justificación:* 100 % offline con teselas preempaquetadas, sin Play Services ni credenciales; API Java
   directa (`MapView`/`Marker`) suficiente para pines y selector. Registrada en `tech-stack.md` §2.

2. **Teselas OSM empaquetadas en `assets/`, sin permiso `INTERNET`.**
   *Justificación:* mantiene el pilar 100 % offline sin excepciones. Cobertura acotada a la Región
   Metropolitana, zoom 10-14; fuera del área se ve un lienzo vacío, no un error. **T0 pendiente.**

3. **El selector de pin vive en una Activity separada (`MapaPickerActivity`).**
   *Justificación:* el `MapView` de osmdroid tiene ciclo de vida propio y no encaja dentro del
   `BottomSheet`; una Activity aísla el ciclo y el retorno se resuelve con `ActivityResultLauncher`.

4. **La pestaña "Mapa" es de solo lectura y vive en el `ViewFlipper` de `CuentasActivity`, sin Fragments.**
   *Justificación:* el proyecto es MVC con `ViewBinding`; `MapaLecturaController` encapsula el `MapView`,
   las teselas, el zoom, los pines y el popup, y `CuentasActivity` solo orquesta y delega el ciclo de vida.

5. **Regla "ambas o ninguna" y rango WGS84 en `Ubicacion`, no con `CHECK` de Room.**
   *Justificación:* Room no expone `CHECK` en `@Entity`. `Ubicacion.crear` es la **única** validación de
   escritura; la consulta `getConUbicacion()` exige ambas columnas no nulas y `Transaccion.fromEntity()`
   lanza `IllegalArgumentException` ante una fila con una sola coordenada. Se cubre con
   `UbicacionTest` y `TransaccionDaoGeoTest`.

6. **Migración no destructiva 2 → 3 e índice geo parcial.**
   *Justificación:* `.agent.md` prohíbe `fallbackToDestructiveMigration()` y `DROP TABLE`. La migración
   agrega columnas con `ALTER TABLE` y crea `idx_transacciones_geo (latitud, longitud)` con
   `WHERE latitud IS NOT NULL AND longitud IS NOT NULL`. La entidad declara el índice sin predicado
   (limitación de Room) y la validación de Room ignora la cláusula `WHERE` al comparar.

7. **`direccionLugar` se persiste como `null`.**
   *Justificación:* la geocodificación inversa offline exigiría empaquetar un dataset de direcciones
   (peso adicional) y no es crítica para el objetivo. La columna queda como punto de extensión; la UI
   muestra la coordenada (`Ubicación fijada: -33.44890, -70.66930`).

8. **Contrato de color de pines centralizado en `PinMapa` + `@color/mapa_pin_movimiento`.**
   *Justificación:* los pines de movimientos y los de la lista de compras (Feature 010) deben distinguirse;
   010 reutilizará el helper cambiando solo el recurso de color.

9. **La consulta por caja envolvente se difiere a la Feature 010.**
   *Justificación:* el mapa de lectura muestra todos los pines; el filtro por viewport no es requisito de
   009. Se dejan el índice y el contrato listos para 010.

---

## Archivos creados / modificados

**Creados:**
- `spec/features/009-geolocalizacion-map/spec.md`, `plan.md`, `tasks.md`
- `doc/009-geolocalizacion-map.md` (este archivo)
- `app/src/main/java/com/localcash/domain/model/Ubicacion.java`
- `app/src/main/java/com/localcash/ui/mapa/TeselasLocales.java`
- `app/src/main/java/com/localcash/ui/mapa/MapaPickerActivity.java`
- `app/src/main/java/com/localcash/ui/mapa/MapaViewModel.java`
- `app/src/main/java/com/localcash/ui/mapa/MapaLecturaController.java`
- `app/src/main/java/com/localcash/ui/mapa/PinMapa.java`
- `app/src/main/res/layout/activity_mapa.xml`
- `app/src/main/res/layout/mapa_info_window.xml`
- `app/src/main/res/drawable/ic_mapa_pin.xml`, `ic_zoom_in.xml`, `ic_zoom_out.xml`
- `app/schemas/com.localcash.data.db.AppDatabase/3.json`
- `app/src/test/java/com/localcash/domain/model/UbicacionTest.java`
- `app/src/test/java/com/localcash/data/dao/TransaccionDaoGeoTest.java`
- `app/src/test/java/com/localcash/data/db/MigracionV2V3Test.java`

**Modificados:**
- `gradle/libs.versions.toml` y `app/build.gradle` — osmdroid 6.1.20 y esquemas en los assets del variant
  `debug` para `MigrationTestHelper`.
- `app/src/main/AndroidManifest.xml` — permisos de ubicación y registro de `MapaPickerActivity` (sin
  `INTERNET`).
- `app/src/main/java/com/localcash/data/entities/Transaccion.java` — columnas geográficas e índice.
- `app/src/main/java/com/localcash/domain/model/Transaccion.java` — propagación de `Ubicacion`.
- `app/src/main/java/com/localcash/domain/error/ErrorCode.java` — `UBICACION_INVALIDA`.
- `app/src/main/java/com/localcash/data/db/AppDatabase.java` — versión 3 y `MIGRATION_2_3`.
- `app/src/main/java/com/localcash/data/dao/TransaccionDao.java` — `getConUbicacion()`.
- `app/src/main/java/com/localcash/ui/cuentas/CuentasActivity.java` — selector, mapa de lectura y ciclo de vida.
- `app/src/main/java/com/localcash/ui/cuentas/TransaccionViewModel.java` — persistencia de coordenadas.
- `app/src/main/res/layout/view_mapa.xml` — `MapView`, zoom, atribución y estado vacío.
- `app/src/main/res/layout/bottom_sheet_transaccion.xml` — confirmación "Ubicación fijada".
- `app/src/main/res/values/strings.xml` — cadenas `mapa_*` y de confirmación.
- `app/src/main/res/values/colors.xml` — `mapa_pin_movimiento`.
- `spec/constitution/data-model.md`, `tech-stack.md`, `roadmap.md` — esquema geo, motor de mapas e hito.
- `spec/features/008-historial/*` — traspaso de alcance de geolocalización a 009.

**Renombrado:**
- `doc/009-actualizacion-diagrama.md` → `doc/011-actualizacion-diagrama.md`, para liberar el `009` de la
  numeración compartida `spec/features/NNN` ↔ `doc/NNN`.

---

## Estado actual de implementación

| Área | Estado |
|---|---|
| Dependencia osmdroid y permisos (T1, T2) | ✅ |
| Columnas, `Ubicacion` y migración 2→3 (T3-T5) | ✅ |
| Selector de pin, `TileSource` y centrado best-effort (T6, T7, T7b) | ✅ (render pendiente de T0) |
| Integración con el panel y persistencia (T8, T9) | ✅ |
| Pestaña "Mapa": lectura, pines y popup (T10-T12) | ✅ (render pendiente de T0) |
| Tests de dominio, persistencia y migración (T13-T15) | ✅ 28/28 en JVM |
| Teselas offline de Santiago (T0) | ⏳ **pendiente** (tarea manual con MOBAC) |
| Verificación manual y `connectedCheck` en emulador | ⏳ **pendiente** de T0 |
| Pull Request | ⏳ **no abierto** hasta cerrar lo anterior |

---

## Validación ejecutada

- `./gradlew testDebugUnitTest`: **28 tests, 0 fallos** (24 previos + 3 de persistencia geo + 1 de migración).
- `./gradlew lint`: BUILD SUCCESSFUL, **41 advertencias preexistentes, 0 nuevas**.
- `./gradlew assembleDebug`: BUILD SUCCESSFUL.
- `./gradlew connectedCheck`: **pendiente** (requiere T0 y emulador activo).

> La Definition of Done de `.agent.md` §3 queda incompleta por diseño: `connectedCheck` va al final y
> no puede validar el render sin las teselas. No se marca como cerrada.

---

## Verificación manual en emulador (T18)

Pasos para ejecutar cuando T0 esté completo:

1. Abrir la app e iniciar sesión.
2. Ir a la pestaña **"Cuentas"**.
3. Presionar el FAB **"+"** para abrir el panel de registro.
4. Activar el interruptor **"Geolocalizar transacción"**.
5. Verificar que se abra **`MapaPickerActivity`** (mapa con controles de zoom y atribución OSM).
6. Tocar el mapa para **fijar el pin**.
7. Presionar **"Confirmar ubicación"**.
8. Verificar que el panel muestre **"Ubicación fijada: &lt;lat&gt;, &lt;lon&gt;"**.
9. Completar y **guardar la transacción**.
10. Ir a la pestaña **"Mapa"** y verificar que aparezca el **pin** del movimiento; tocarlo para ver el
    popup con concepto, monto y fecha.

**Casos adicionales:**
- **Permiso denegado:** el selector debe abrir centrado en Santiago y permitir fijar el pin; el registro
  nunca se bloquea (CA-6/E-1).
- **Modo avión desde el primer arranque:** el mapa y los pines deben verse sin conexión (E-5).
- **Sin movimientos geolocalizados:** la pestaña "Mapa" debe mostrar "Sin movimientos geolocalizados".

> **Nota:** sin T0, los pasos 5-7 y 10 funcionan a nivel de lógica y controles, pero el mapa se verá en
> blanco (sin teselas).

---

## Checklist de cierre

- [x] Código compilando sin errores (`assembleDebug`)
- [x] Tests unitarios pasando (28/28)
- [x] Lint sin warnings nuevos (41 preexistentes, 0 nuevas)
- [x] Documentación actualizada (`plan.md`, `tasks.md`, este registro)
- [x] Migración 2→3 cubierta con `MigrationTestHelper` y datos sembrados
- [x] Regla "ambas o ninguna" y rango cubiertos en dominio, consulta y mapeo
- [ ] **T0:** teselas de Santiago empaquetadas en `assets/tiles/` (pendiente, tarea manual con MOBAC)
- [ ] **`connectedCheck`** en emulador (pendiente, requiere T0)
- [ ] **Pull Request** (pendiente, requiere todo lo anterior)

---

## Lecciones aprendidas

- **`MigrationTestHelper` bajo Robolectric no lee `src/test/assets`.** Robolectric resuelve los assets del
  variant `debug` (`mergeDebugAssets`), así que los esquemas exportados se exponen con
  `sourceSets.debug.assets` en `app/build.gradle` (solo debug, nunca release).
- **La validación de índices de Room ignora el `WHERE`.** Un índice parcial creado por la migración
  valida contra el índice no parcial declarado en la entidad/`3.json`; la divergencia queda documentada
  en `data-model.md` en vez de convertirse en un fallo silencioso.
- **El color del pin se centraliza antes de que exista el segundo consumidor.** `PinMapa` y
  `@color/mapa_pin_movimiento` dejan listo el contrato para la Feature 010 sin redefinirlo.

---

## Próximos pasos

1. **T0 (manual):** generar con MOBAC las teselas de la Región Metropolitana (lat -33.45..-33.30,
   lon -70.70..-70.55, zoom 10-14) y dejarlas en `app/src/main/assets/tiles/{z}/{x}/{y}.png`.
2. **Verificación manual** de los pasos de la sección anterior en emulador.
3. **`./gradlew connectedCheck`** con el emulador activo.
4. **Pull Request** de `feat/009-geolocalizacion-map` → `main` (recién entonces).
