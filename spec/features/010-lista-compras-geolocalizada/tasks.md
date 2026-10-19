# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** 010-lista-compras-geolocalizada | **Derivado de:** `spec.md` + `plan.md`
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

## Fase 1 — Fundaciones (Entidades Room, DAOs, Migración 3→4)

- [ ] **T1. Actualizar `data-model.md` con las tablas `lugar_compra` e `item_compra`.**
      Agregar el diccionario de ambas entidades, la relación uno-a-muchos y el índice/FK. Regla: se
      actualiza en el mismo cambio que el código de persistencia.
      *Criterio de salida:* `data-model.md` describe el esquema que Room implementará.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T2. Crear la entidad `LugarCompra` (Room).**
      `id` (PK), `latitud` (Double), `longitud` (Double), `tipoLugar` (String),
      `gastoPromedio` (Double), `fechaCreacion` (Long).
      *Criterio de salida:* entidad compila y Room la reconoce.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T3. Crear la entidad `ItemCompra` (Room).**
      `id` (PK), `lugarCompraId` (FK), `nombre` (String), `cantidad` (Int), `estaComprado` (Boolean).
      *Criterio de salida:* entidad compila y Room la reconoce.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T4. Crear los DAOs (`LugarCompraDao`, `ItemCompraDao`).**
      `LugarCompraDao`: insertar lugar, obtener lugar con sus items (`@Relation`), listar lugares,
      eliminar lugar (con cascada de items o borrado explícito de items). `ItemCompraDao`: insertar,
      eliminar, marcar como comprado.
      *Criterio de salida:* DAOs compilan y son invocables.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T5. Migración de base de datos 3 → 4 (no destructiva).**
      `CREATE TABLE lugar_compra` y `CREATE TABLE item_compra` (tablas nuevas, sin tocar existentes),
      `version` de Room a 4, exportar `app/schemas/.../4.json`. Prohibido `DROP TABLE`/
      `fallbackToDestructiveMigration()`.
      *Criterio de salida:* `./gradlew assembleDebug` resuelve la BD; `MigracionV3V4Test` pasa.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

---

## Fase 2 — UI de Creación (Diálogo de Long Press en el mapa)

- [ ] **T6. Detectar long press en `MapaLecturaController`.**
      Añadir `OnMapLongClickListener` al `MapView` de la pestaña "Mapa" que capture el `GeoPoint`
      pulsado y lance el diálogo de creación con coordenadas prellenadas.
      *Criterio de salida:* long press en el mapa abre el diálogo de creación.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T7. Crear el diálogo/formulario de creación (`dialog_nuevo_lugar_compra.xml`).**
      Campos: tipo de lugar (selección predefinida o personalizada), gasto promedio estimado y lista
      de items (nombre, cantidad, estado). ViewBinding obligatorio; sin textos/colores hardcodeados.
      *Criterio de salida:* el diálogo se infla y muestra todos los campos desde recursos.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T8. Conectar el diálogo con la persistencia.**
      Al guardar, validar (tipo obligatorio, nombre de item no vacío, cantidad ≥ 1) y persistir el
      `LugarCompra` con sus `ItemCompra` asociados.
      *Criterio de salida:* el lugar y sus items se guardan en Room y se recuperan correctamente.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

---

## Fase 3 — UI de Lectura (Pines de compra, popup de resumen)

- [ ] **T9. Extender `PinMapa` para el color `@color/mapa_pin_compra`.**
      Añadir la tinta/ícono de pin de compra, distinto al de movimiento (`@color/mapa_pin_movimiento`),
      definido en `colors.xml` (sin hardcodear).
      *Criterio de salida:* `PinMapa` puede pintar pines de compra con su color propio.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T10. Dibujar un pin de compra por lugar en el mapa de lectura.**
      `MapaLecturaController` consulta los lugares de compra y coloca un pin por cada uno con el
      color de compra, respetando el contrato de color de 009.
      *Criterio de salida:* en la pestaña "Mapa" aparecen los pines de compra con color distinto.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T11. Popup de resumen sobre el pin de compra.**
      Al tocar un pin de compra, mostrar popup con resumen (1-2 items) y botón "Ver detalle".
      *Criterio de salida:* el popup aparece y el botón "Ver detalle" es accionable.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

---

## Fase 4 — UI de Detalle (Activity de lista con RecyclerView y acciones)

- [ ] **T12. Crear `LugarCompraDetailActivity` y su layout.**
      Activity con `RecyclerView` para la lista completa de items del lugar. ViewBinding obligatorio.
      *Criterio de salida:* la actividad se abre desde el popup y muestra los items del lugar.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T13. Crear el adapter `LugarCompraItemAdapter`.**
      Fila del RecyclerView: nombre, cantidad, checkbox "comprado"; acciones agregar, eliminar y
      tachar items.
      *Criterio de salida:* se pueden agregar, eliminar y marcar items como comprados.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T14. Eliminar lista de compra con confirmación.**
      Diálogo de confirmación antes de borrar el lugar y sus items; solo se elimina si el usuario
      confirma.
      *Criterio de salida:* nunca se elimina sin confirmación explícita.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

---

## Fase 5 — Tests y Cierre

- [ ] **T15. Test de persistencia de lugar + items (`LugarCompraDaoTest`).**
      Room in-memory: guardar lugar con items, leer completo, verificar relación uno-a-muchos y
      borrado.
      *Criterio de salida:* tests en verde; cobertura ≥ 70% en la lógica nueva.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T16. Test de migración 3 → 4 (`MigracionV3V4Test`).**
      `MigrationTestHelper` + Robolectric: siembra datos en v3, aplica `MIGRATION_3_4` y valida contra
      `4.json`; datos v3 sobreviven y tablas nuevas existen.
      *Criterio de salida:* test en verde sin pérdida de datos.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T17. Verificación de lint, tests y build.**
      `./gradlew lint` (sin warnings nuevos sobre los 41 preexistentes), `./gradlew testDebugUnitTest`
      y `./gradlew assembleDebug`.
      *Criterio de salida:* todo en verde.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T18. Documentar la verificación manual en emulador.**
      Pasos del flujo completo (long press → crear → guardar → pin de compra → popup → detalle →
      eliminar con confirmación) en el registro de memoria `doc/010-lista-compras-geolocalizada.md`.
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

- [ ] **T19. Checklist de cierre de la feature.**
      Código, tests, lint, documentación y registro de memoria ✅. `connectedCheck` queda pendiente
      (requiere emulador/dispositivo activo).
      Responsable: [RESPONSABLE] | Estimación: [ESTIMACION]

---

## Verificación final (Definition of Done)

Los cuatro comandos van en este orden. `connectedCheck` va al final y **requiere dispositivo o
emulador activo**: si se ejecuta primero y falla, se pierde el ciclo de compilación.

- [ ] Lint sin advertencias nuevas: `./gradlew lint` (41 preexistentes, 0 nuevas)
- [ ] Tests unitarios en verde: `./gradlew testDebugUnitTest`
- [ ] Tests de integración y UI en verde: `./gradlew connectedCheck` — **pendiente** (requiere emulador)
- [ ] Build válido: `./gradlew assembleDebug`
- [ ] Registro de memoria creado: `doc/010-lista-compras-geolocalizada.md`
- [ ] `data-model.md` actualizado si la feature tocó el esquema (en el mismo cambio)
- [ ] Commits con formato de `.agent.md` §5.2, uno por unidad lógica

**Notas de ejecución:** [ESPACIO PARA APUNTES DURANTE LA IMPLEMENTACION]
