# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** 009-geolocalizacion-map | **Derivado de:** `spec.md` + `plan.md`
> **Estado global:** Pendiente (sin implementar). La preparación previa (interruptor en el panel) proviene
> de la Feature 008.
>
> **Propósito de este documento:** Descomponer la implementación en pasos ejecutables, ordenados y
> verificables. Cada casilla marcada `[x]` debe corresponder a un cambio real comprobable (tests
> verdes, archivo creado, comando ejecutado). Marcar sin verificar está prohibido.
>
> **Convenciones:**
> - Ejecuta las tareas EN ORDEN: cada una parte de la anterior completada.
> - Al terminar cada bloque relevante, ejecuta el lint/test con los comandos de `.agent.md` §3
>   (`./gradlew lint`, `./gradlew testDebugUnitTest`).
> - Si una tarea revela que el plan era incorrecto: DETENTE, actualiza `plan.md` con justificación
>   y continúa. Nunca implementes "al margen" del plan.

---

## Fase 1 — Fundaciones (dependencia, permisos, esquema y dominio)

- [ ] **T0. Empaquetar las teselas offline de Santiago de Chile.**
      Generar con **MOBAC** (Mobile Atlas Creator) o un script propio contra la API de OSM las teselas
      raster de la Región Metropolitana (lat **-33.45..-33.30**, lon **-70.70..-70.55**, **zoom 10-14**)
      y empaquetarlas en `app/src/main/assets/tiles/{z}/{x}/{y}.png` (esquema XYZ). Peso ~30-50 MB.
      **Sin permiso `INTERNET`.** *Bloquea* la renderización del mapa (Fases 2-3), no las tareas de
      esquema (T1-T5). *Criterio de salida:* teselas en `assets/` dentro del peso estimado.
- [x] **T1. Declarar la dependencia de osmdroid (6.1.x).**
      `osmdroid-android:6.1.20` en `gradle/libs.versions.toml` e `app/build.gradle` (registrada en
      `tech-stack.md` §2). *Criterio de salida:* `./gradlew assembleDebug` resuelve la dependencia. ✅
- [x] **T2. Declarar los permisos de ubicación en `AndroidManifest.xml`.**
      `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` (denegación → best-effort). Manifiesto fusionado
      sin duplicados. ✅
- [x] **T3. Añadir las columnas de coordenadas a la entidad Room.**
      `latitud` (`Double`), `longitud` (`Double`) y `direccionLugar` (`String`) nullable en
      `data/entities/Transaccion.java`, según `data-model.md`. (La versión de la BD y el índice se
      abordan en T5.) ✅
- [x] **T4. Crear el value object `Ubicacion` y la validación de dominio.**
      `domain/model/Ubicacion.java` (inmutable) encapsula la regla "ambas o ninguna" y el rango
      (lat -90..90, lon -180..180); `crear(...)`/`vacia()` devuelven `null` cuando no hay ubicación.
      La validación vive en el value object (fuente única); `TransaccionService` la consumirá al
      propagar las coordenadas (T9). Tests JVM en `UbicacionTest`. ✅
- [x] **T5. Migración de base de datos 2 → 3 (no destructiva).**
      `ALTER TABLE transacciones ADD COLUMN` para las tres columnas, `version` de Room a 3 y crear
      `idx_transacciones_geo`. El `@Entity` declara el índice (Room no admite índices parciales) y la
      migración crea la forma parcial canónica. `app/schemas/.../3.json` exportado. Prohibido
      `DROP TABLE`/migración destructiva. ✅

## Fase 2 — Captura de pin e integración con el panel

- [x] **T6. Crear el layout del mapa (`activity_mapa.xml`) y el `TileSource` local.**
      `res/layout/activity_mapa.xml`: `MapView` de osmdroid + controles de zoom (+/−) + atribución
      "© OpenStreetMap contributors" (desde recursos). `ui/mapa/TeselasLocales.java`: `TileSource` propio
      que lee las teselas de `assets/tiles` con `MapTileAssetsProvider` (sin descargador de red). ✅
- [x] **T7. Crear `MapaPickerActivity` (selector de ubicación).**
      `ui/mapa/MapaPickerActivity.java`: abre centrada en Santiago de Chile (lat -33.4489, lon -70.6693),
      coloca un pin al tocar el mapa; "Confirmar" devuelve `setResult(RESULT_OK)` con extras
      `latitud`/`longitud`/`direccion`, "Cancelar" cierra sin resultado. Ciclo de vida del `MapView`
      gestionado y Activity registrada en el manifiesto. ✅
- [x] **T7b. Centrar el mapa en la posición actual (best-effort), con fallback a Santiago.**
      `MapaPickerActivity`: al abrir, lee la última ubicación conocida con `LocationManager`
      (comprueba permiso en runtime sin solicitar diálogo, sin listeners y sin bloquear) y centra ahí;
      si no hay permiso o ningún fix previo, cae a Santiago (-33.4489, -70.6693). El mapa funciona con
      o sin ubicación (CA-6/E-1/E-2). ✅
- [x] **T8. Integrar el selector con el panel.**
      `CuentasActivity`: al activar "Geolocalizar" lanza `MapaPickerActivity` con un `ActivityResultLauncher`,
      recibe las coordenadas en el callback, muestra la confirmación y conserva el estado del panel. ✅
- [x] **T9. Persistir las coordenadas al registrar el movimiento.**
      `domain/model/Transaccion` propaga `Ubicacion` (`toEntity`/`fromEntity`); `TransaccionViewModel`
      la construye/valida con `Ubicacion.crear` (incompleta o fuera de rango → `UBICACION_INVALIDA`) y la
      persiste; sin ubicación → columnas `NULL`. Ambas coordenadas o ninguna (CA-3/CA-4/CA-7). ✅

## Fase 3 — Pestaña "Mapa" (modo lectura)

- [x] **T10. Crear el layout de la pestaña "Mapa" y el mapa de lectura.**
      `res/layout/view_mapa.xml` aloja un `MapView` (con el `TileSource` local, la atribución OSM,
      los controles de zoom y el mensaje de estado vacío); `ui/mapa/MapaLecturaController` monta las
      teselas locales offline y el encuadre inicial. ✅
- [x] **T11. Exponer los movimientos geolocalizados (`TransaccionDao` + `MapaViewModel`).**
      `TransaccionDao.getConUbicacion()` filtra `latitud IS NOT NULL AND longitud IS NOT NULL`
      (apoyado en `idx_transacciones_geo`); `ui/mapa/MapaViewModel` transforma las entidades a dominio. ✅
- [x] **T12. Dibujar un pin por movimiento, con popup y estado vacío.**
      `MapaLecturaController.mostrarMovimientos` coloca un pin por fila geolocalizada con el color
      `@color/mapa_pin_movimiento` (helper `PinMapa`, contrato que reutilizará 010), un popup con
      concepto, monto y fecha, y alterna el mensaje de estado vacío (CA-5/CA-8). ✅
      *(La consulta por caja envolvente sobre `idx_transacciones_geo` queda como punto de extensión para 010.)*

## Fase 4 — Tests

- [x] **T13. Test de dominio de `Ubicacion`.**
      `UbicacionTest` (11 casos): rango válido/inválido (incluye límites exactos y `NaN`), regla
      "ambas o ninguna", factory `crear()` y `vacia()`, más coherencia `equals`/`hashCode`. ✅
      *(Nombre definitivo `UbicacionTest`; el checklist citaba `UbicacionValidatorTest`, hoy inexistente. Se
      reconcilia la referencia, no se renombra la clase.)*
- [x] **T14. Test de persistencia geográfica.**
      `TransaccionDaoGeoTest` (Room in-memory): ida y vuelta `toEntity`/`fromEntity` con coordenadas
      válidas; sin ubicación → columnas `NULL` y fuera de `getConUbicacion()`; coordenada incompleta →
      excluida de la consulta y rechazada al mapear a dominio. ✅
- [x] **T15. Test de migración 2 → 3.**
      `MigracionV2V3Test` (Room `MigrationTestHelper` + Robolectric): siembra datos en v2, aplica
      `MIGRATION_2_3` y valida contra `3.json`; comprueba las columnas `latitud`/`longitud`/`direccionLugar`,
      el índice `idx_transacciones_geo` (lat, lon) y que los datos v2 sobreviven con la geo en `NULL`. ✅
      *(Los esquemas se exponen a Robolectric solo en el variant `debug`: el source set `test` no llega a
      `mergeDebugAssets`, que es de donde Robolectric lee los assets.)*

## Fase 5 — Cierre y verificación

- [ ] **T16. Ejecutar la Definition of Done completa.**
      `./gradlew lint` → `testDebugUnitTest` → `connectedCheck` → `assembleDebug`.
- [ ] **T17. Verificación manual en emulador.**
      Activar el interruptor → fijar pin → guardar → ver el pin en la pestaña "Mapa"; probar permiso
      denegado y modo avión (CA-6/E-5).
- [ ] **T18. Actualizar `data-model.md` si la implementación diverge del DDL (en el mismo cambio).**
- [ ] **T19. Registro de memoria final.**
      Crear `doc/009-geolocalizacion-map.md` y actualizar el hito del roadmap. (La numeración de `doc/`
      ya quedó resuelta: el registro de mantenimiento se renombró a `011`.)

---

## Verificación final (Definition of Done)

Los cuatro comandos van en este orden. `connectedCheck` va al final y **requiere dispositivo o
emulador activo**: si se ejecuta primero y falla, se pierde el ciclo de compilación.

- [ ] Lint sin advertencias: `./gradlew lint`
- [ ] Tests unitarios en verde: `./gradlew testDebugUnitTest`
- [ ] Tests de integración y UI en verde: `./gradlew connectedCheck`
- [ ] Build válido: `./gradlew assembleDebug`
- [ ] Registro de memoria creado: `doc/009-geolocalizacion-map.md`
- [ ] `data-model.md` actualizado si la feature tocó el esquema (en el mismo cambio)
- [ ] Commits con formato de `.agent.md` §5.2, uno por unidad lógica

**Notas de ejecución:** (pendiente de iniciar)
