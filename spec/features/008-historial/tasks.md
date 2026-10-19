# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** 008-historial | **Derivado de:** `spec.md` + `plan.md`
> **Estado global:** Parcialmente completado (Fase 2 avanzada) — el historial se alimenta de Room, se
> refresca por LiveData y ya no contiene datos de prueba. Pendiente inmediato: T12 (saldo); además,
> las Fases 4-5.
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

## Fase 1 — Navegación y estructura ✅

- [x] **T1. Renombrar la pestaña "Nuevo" → "Movimientos".**
      Menú inferior (`bottom_nav_menu.xml`) y cadenas asociadas.
      *Criterio de salida:* la pestaña muestra "Movimientos".
- [x] **T2. Reducir el contenedor de vistas de 5 → 4 y corregir los índices.**
      Se retiró la vista propia de registro (ahora es panel inferior): Cuentas=0, Movimientos=1,
      Mapa=2, Contactos=3. Se corrigió el cruce previo de Mapa/Contactos.
      *Criterio de salida:* cada ítem del menú inferior muestra su vista correcta.
- [x] **T3. Alinear los títulos del encabezado (Toolbar) con las 4 vistas.**
      *Criterio de salida:* el título cambia según la pestaña activa.
- [x] **T4. Sanear la restauración de estado del contenedor de vistas.**
      El índice guardado se acota a 0..3 para no referenciar una vista eliminada.
      *Criterio de salida:* rotación/cambio de tema no rompe la navegación.

## Fase 2 — Historial (avanzada; queda T12) ⏳

- [x] **T5. Layout de la vista de historial.**
      `view_historial.xml`: lista desplazable, estado vacío y botón flotante.
- [x] **T6. Adapter y fila del historial.**
      `HistorialAdapter.java` + `item_transaccion.xml` (icono, concepto, fecha, monto con signo/color).
- [x] **T7. Poblar la lista para validar la UI (datos de prueba — TEMPORAL).**
      `generarTransaccionesMock()` con 10 movimientos. Deuda técnica **retirada en T9**.
- [x] **T8. Cablear el historial a los datos reales.**
      `HistorialViewModel` (T8a-c) observa `TransaccionDao.getAll()` y mapea entidades → dominio;
      `CuentasActivity` (T8e) lo instancia con una `ViewModelProvider.Factory` y alimenta el adapter.
      *Criterio de salida:* la lista muestra los movimientos persistidos, ordenados por fecha DESC.
- [x] **T9. Eliminar los datos de prueba.**
      `generarTransaccionesMock()` y sus imports huérfanos (`java.util.Calendar`, `java.util.Locale`)
      fueron eliminados de `CuentasActivity`; el historial se alimenta solo de Room.
      *Criterio de salida:* ninguna referencia a datos mock en el historial. ✅
- [x] **T10. Estado vacío real según los datos.**
      El estado vacío y la visibilidad de la lista dependen de la lista observable de la base de datos
      (observador de `HistorialViewModel` en `CuentasActivity`).
- [x] **T11. Refresco dinámico tras registrar (CA-6).**
      El `LiveData` de Room re-emite al insertar, así que el movimiento aparece en la lista sin
      recargar la pantalla.
- [ ] **T12. Actualización dinámica del saldo total.**
      Verificar/garantizar que el saldo consolidado de las cuentas refleja el movimiento recién
      registrado en la vista de historial.

## Fase 3 — Registro vía panel inferior (parcial) ⏳

- [x] **T13. Layout del formulario de registro.**
      `bottom_sheet_transaccion.xml`: Ingreso/Egreso (toggle), Monto, Cuenta (dropdown), Concepto,
      Geolocalizar (switch) y Guardar. Reutiliza los IDs de la vista eliminada.
- [x] **T14. El botón flotante abre el panel inferior.**
      `mostrarBottomSheetTransaccion()` con un `BottomSheetDialog` real.
- [x] **T15. Registrar el movimiento con validación de dominio.**
      Reutiliza `TransaccionViewModel`/`TransaccionService`; mapea los códigos de `ErrorCode` a
      mensajes accionables.
- [x] **T16. Manejo de éxito y error del registro.**
      Éxito: mensaje, limpiar formulario, cerrar panel. Error: mensaje y panel abierto.
- [ ] **T17. Refresco del historial tras un registro exitoso.**
      Depende de T8/T11.
- [ ] **T18. Poblar el selector de cuentas desde datos observables.**
      Hoy se puebla puntualmente; verificar que se actualiza si cambian las cuentas mientras el panel
      está abierto.

## Fase 4 — Geolocalización (PENDIENTE) ⏳

- [ ] **T19. Añadir las columnas de coordenadas a la entidad Room de movimientos.**
      `latitud` (`REAL`), `longitud` (`REAL`), `direccionLugar` (`TEXT`), según `data-model.md`.
- [ ] **T20. Propagar las coordenadas en el modelo de dominio.**
      Ajustar `domain/model/Transaccion` y el único par `toEntity()`/`fromEntity()`; opcionalmente
      crear el value object `Ubicacion`.
- [ ] **T21. Migración de base de datos 2 → 3 (no destructiva).**
      `ALTER TABLE ... ADD COLUMN` nullable, subir `user_version`, crear el índice parcial
      `idx_transacciones_geo`. Prohibido `DROP TABLE`/migración destructiva.
- [ ] **T22. Declarar los permisos de ubicación en `AndroidManifest.xml`.**
      `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`.
- [ ] **T23. Solicitar el permiso en runtime al activar "Geolocalizar".**
      Denegación → registrar sin coordenadas (best-effort, no bloqueante).
- [ ] **T24. Capturar las coordenadas reales y persistirlas.**
      Con el framework de Android (`LocationManager`), sin dependencias nuevas. Aplicar la regla
      "ambas o ninguna" (nunca media ubicación).
- [ ] **T25. Mejora visual del control "Geolocalizar".**
      Refinar la presentación del interruptor en el panel (hoy es un `MaterialSwitch` estándar).
- [ ] **T26. Test de persistencia geográfica.**
      `TransaccionDaoGeoTest`: coordenada válida se guarda; coordenada incompleta se rechaza.

## Fase 5 — Cierre y verificación ⏳

- [ ] **T27. Test de migración 2 → 3.**
      `MigracionV2V3Test`: sembrar datos en v2, migrar y comprobar que no se perdió información.
- [ ] **T28. Tests de los criterios de aceptación CA-1…CA-9.**
      Cubrir lista real, estado vacío, registro válido/ inválido y geolocalización.
- [ ] **T29. Ejecutar la Definition of Done completa.**
      `./gradlew lint` → `testDebugUnitTest` → `connectedCheck` → `assembleDebug`.
- [ ] **T30. Registro de memoria final.**
      Actualizar `doc/008-historial.md` de "parcial" a "completado" y ajustar el roadmap.

---

## Estado actual verificado (cierre provisional)

| Comando | Resultado | Fecha |
|---|---|---|
| `./gradlew lint` | BUILD SUCCESSFUL (0 errores; 41 advertencias preexistentes) | 18/10/2026 |
| `./gradlew testDebugUnitTest` | 13 tests, 0 fallos | 18/10/2026 |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL | 18/10/2026 |
| `./gradlew connectedCheck` | No ejecutado (requiere dispositivo) | — |

> El historial se alimenta de Room y ya no contiene datos de prueba (T8/T9/T10/T11). Queda sin
> implementar la **geolocalización** (Fase 4) y el saldo dinámico (T12). Estas verificaciones deben
> repetirse al cerrar las Fases 4 y 5.
