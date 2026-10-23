# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** 009-geolocalizacion-map | **Derivado de:** `spec.md` + `plan.md`
> **Estado global:** Fases 0 a 5 completadas; Fase 6 (cierre y verificación final) en curso.
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

## Fase 0 — Contratos y documentación

- [x] **T1. Re-escribir `spec.md` con el alcance completo.**
      Mapa interactivo, pines vinculados/independientes, tipos de lugar, lista de cosas, cierre y
      reapertura, "Mi ubicación" y lista de lugares. *Salida:* CA-1..CA-18 verificables.
- [x] **T2. Re-escribir `plan.md` con las decisiones técnicas.**
      MapLibre Native 13.4.0, OpenFreeMap, esquema `LUGARES`/`ITEMS_LUGAR`, migración 2 → 3 y
      estrategia de pruebas. *Salida:* cada decisión tiene justificación y consecuencia.
- [x] **T3. Re-escribir `tasks.md` como checklist secuencial.**
      *Salida:* este archivo, alineado con `spec.md` y `plan.md`.
- [x] **T4. Actualizar la constitución.**
      `tech-stack.md` §2/§7 (MapLibre reemplaza a osmdroid), `.agent.md` §2 (stack vigente),
      `roadmap.md` (hito y deuda técnica) y `data-model.md` (nuevas tablas, ERD, DDL, versión).
- [x] **T5. Sincronizar la documentación del modelo.**
      `doc/009-actualizacion-diagrama.md` y `README.md`: 10 tablas, MapLibre, pines y listas.

## Fase 1 — Fundaciones técnicas

- [x] **T6. Declarar MapLibre Native 13.4.0.**
      Añadido `org.maplibre.gl:android-sdk:13.4.0` en `gradle/libs.versions.toml` y
      `app/build.gradle`, más el helper `MapaConfig.inicializar(context)`. `CuentasActivity` ya
      inicializa MapLibre antes de inflar las vistas internas.
      *Salida:* compila con la dependencia y sin errores nuevos.
- [x] **T7. Declarar permisos en `AndroidManifest.xml`.**
      `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`.
      El registro de `SeleccionarUbicacionActivity` se hará en T23, cuando exista la clase.
      *Salida:* el mapa puede descargar teselas y la ubicación se solicita en runtime.
- [x] **T8. Crear las entidades y DAOs de lugares.**
      `Lugar`, `ItemLugar`, `LugarDao` e `ItemLugarDao` según `data-model.md`; registradas en
      `AppDatabase`. *Salida:* compila y las consultas observables están definidas.
- [x] **T9. Implementar la migración 2 → 3.**
      Agregadas `latitud`, `longitud` y `direccion_lugar` a `TRANSACCIONES`; creadas `LUGARES` e
      `ITEMS_LUGAR` con sus índices; `version` subida a 3. Sin `DROP TABLE` ni
      `fallbackToDestructiveMigration()`.
      *Salida:* migración compilada y cubierta por T29.
- [x] **T10. Crear el dominio del lugar.**
      `Ubicacion`, `TipoLugar`, `EstadoLugar`, `Lugar`, `ItemLugar` y `LugarService`; códigos de
      error nuevos. *Salida:* validaciones invocables sin Android y cubiertas por T27.
- [x] **T11. Centralizar el estilo y la atribución del mapa.**
      `MapaConfig` con el estilo `https://tiles.openfreemap.org/styles/liberty` y la atribución.
      *Salida:* no hay URLs de teselas dispersas en el código.

## Fase 2 — Mapa de lectura y ubicación actual

- [x] **T12. Reemplazar el placeholder de `view_mapa.xml`.**
      `MapView`, botón "Mi ubicación", botón "Mis lugares", estado vacío y control de atribución.
      *Salida:* la pestaña "Mapa" muestra el mapa y los controles.
- [x] **T13. Implementar `MapaController`.**
      Encapsular `MapView`/`MapLibreMap`, cámara, pines, pulsación larga y reenvío de ciclo de vida
      desde `CuentasActivity`. *Salida:* rotación y cambio de tema no filtran ni rompen el mapa.
- [x] **T14. Implementar `MapaViewModel` y la consulta de lugares.**
      Observar `LUGARES` e `ITEMS_LUGAR`, exponer lugares activos y mapear entidades a dominio.
      *Salida:* un pin guardado aparece sin recargar la pantalla.
- [x] **T15. Implementar "Mi ubicación".**
      Permiso en runtime y `LocationManager`; centrar cámara o mostrar aviso si falla (CA-13/CA-14).
      *Salida:* no bloquea la app con permiso denegado o sin GPS.
- [x] **T16. Implementar la lista de lugares.**
      BottomSheet con `LugaresAdapter`; al tocar un lugar, centrar el mapa y abrir su detalle
      (CA-15). *Salida:* lista observable y navegación al pin.

## Fase 3 — CRUD de pines y lista de cosas

- [x] **T17. Crear el formulario de lugar.**
      `bottom_sheet_lugar.xml` + `LugarFormSheet`: tipo, emoji, nombre, descripción y lista inicial.
      Nombre por defecto = nombre del tipo (CA-6/CA-7).
- [x] **T18. Alta de pin por pulsación larga.**
      `MapaController` captura la coordenada y abre el formulario; el pin solo se inserta al
      confirmar (CA-5). *Salida:* cancelar no deja filas huérfanas.
- [x] **T19. Detalle del pin.**
      BottomSheet con datos, acciones editar/cerrar/reabrir/eliminar y lista de cosas. Eliminar pide
      confirmación (CA-9/CA-11/CA-12).
- [x] **T20. Edición de pin.**
      Actualizar nombre, tipo, emoji y descripción; para pines independientes, también la posición.
      Un pin vinculado no permite mover la coordenada (CA-10).
- [x] **T21. Gestión de la lista de cosas.**
      `ItemsLugarAdapter`: agregar, editar, marcar completado y eliminar elementos; persistencia
      observable (CA-8).
- [x] **T22. Cierre y reapertura.**
      Cambiar `estado` sin borrar; el pin cerrado sale de la lista activa y se dibuja atenuado en el
      mapa; reabrir lo devuelve a la lista (CA-12).

## Fase 4 — Integración con la geolocalización de movimientos

- [x] **T23. Crear `SeleccionarUbicacionActivity` y registrarla en el manifiesto.**
      Mapa MapLibre con pin seleccionable, confirmar/cancelar y devolución de `latitud`/`longitud`
      por `setResult` (CA-1/CA-2). Declararla `android:exported="false"` en `AndroidManifest.xml`.
- [x] **T24. Conectar el interruptor "Geolocalizar".**
      `CuentasActivity` lanza la Activity con `ActivityResultLauncher` y conserva el estado del panel;
      sin confirmación, el movimiento queda sin coordenadas (CA-4).
- [x] **T25. Persistir movimiento y pin vinculado de forma atómica.**
      `TransaccionDao.insert` devuelve el id; `TransaccionViewModel` guarda coordenadas y crea el
      lugar dentro de `AppDatabase.runInTransaction` (CA-3). *Salida:* un fallo no deja medio pin.
- [x] **T26. Mostrar pines vinculados e indicar su origen.**
      El mapa incluye los lugares con `transaccionId`; el detalle indica "vinculado a un movimiento"
      sin duplicar el marcador (CA-16).

## Fase 5 — Pruebas

- [x] **T27. Tests de dominio (`LugarServiceTest`).**
      Nombre vacío, tipo inválido, emoji vacío, coordenadas fuera de rango, transiciones de estado y
      regla de posición inmutable en pin vinculado.
- [x] **T28. Tests de persistencia (`LugarDaoTest`, `ItemLugarDaoTest`).**
      CRUD, consulta geográfica, cascada de elementos, unicidad del vínculo con movimiento y
      `PRAGMA foreign_keys`.
- [x] **T29. Test de migración (`MigracionV2V3Test`).**
      Sembrar datos en la versión 2, migrar, comprobar cuentas/movimientos intactos y
      `PRAGMA foreign_key_check` limpio.
- [x] **T30. Pruebas manuales de casos borde.**
      Permiso denegado, sin GPS, modo avión, rotación/cambio de tema, cientos de pines, emoji no
      representable y eliminación del movimiento vinculado.

## Fase 6 — Cierre y verificación

- [ ] **T31. Ejecutar la Definition of Done completa.**
      `./gradlew lint` → `testDebugUnitTest` → `connectedCheck` → `assembleDebug`.
- [ ] **T32. Verificación manual guiada por `spec.md`.**
      Recorrer CA-1..CA-18 y registrar evidencia en el futuro `doc/009-geolocalizacion-map.md`.
- [ ] **T33. Sincronizar el esquema si la implementación diverge.**
      Actualizar `data-model.md` y `doc/009-actualizacion-diagrama.md` en el mismo cambio; validar el
      DDL en `sqlite3` con `PRAGMA foreign_key_check`.
- [ ] **T34. Actualizar roadmap y README.**
      Estado del hito, stack de mapas y conteo de tablas.
- [ ] **T35. Crear el registro de memoria y resolver la numeración.**
      `doc/009-geolocalizacion-map.md` con decisiones, archivos, lecciones y próximos pasos. Antes,
      mover `doc/009-actualizacion-diagrama.md` a un número libre o renumerar según decisión del
      responsable (`plan.md` §7).

---

## Verificación final (Definition of Done)

Los cuatro comandos van en este orden. `connectedCheck` va al final y **requiere dispositivo o
emulador activo**: si se ejecuta primero y falla, se pierde el ciclo de compilación.

- [ ] Lint sin advertencias: `./gradlew lint`
- [ ] Tests unitarios en verde: `./gradlew testDebugUnitTest`
- [ ] Tests de integración y UI en verde: `./gradlew connectedCheck`
- [ ] Build válido: `./gradlew assembleDebug`
- [ ] Registro de memoria creado: `doc/009-geolocalizacion-map.md`
- [ ] `data-model.md` y `doc/009-actualizacion-diagrama.md` actualizados si la feature tocó el esquema
- [ ] Roadmap y README actualizados
- [ ] Commits con formato de `.agent.md` §5.2, uno por unidad lógica

**Notas de ejecución:** (pendiente de iniciar)
