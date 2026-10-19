# plan.md — Plan Técnico de Implementación

> **Identificación:** 010-lista-compras-geolocalizada | **Fecha:** 19/10/2026 | **Autor:** GG
>
> **Propósito de este documento:** Definir CÓMO se implementará la feature descrita en `spec.md`:
> componentes, decisiones técnicas, riesgos y dependencias. Es el puente entre el requisito y las
> tareas ejecutables (`tasks.md`).
>
> **Regla:** Ninguna decisión de este archivo puede contradecir `.agent.md` ni `tech-stack.md`.
> Si la contradicción es inevitable, primero se actualiza la constitución con su registro en `doc/`.

---

## 1. Componentes Involucrados

| Archivo / Módulo | Rol en la feature | Acción | Complejidad estimada |
|---|---|---|---|
| `spec/constitution/data-model.md` | Tablas `lugar_compra` e `item_compra` (uno-a-muchos) | MODIFICAR | BAJA |
| `data/entities/LugarCompra.java` | Entidad Room: id, latitud, longitud, tipoLugar, gastoPromedio, fechaCreacion | CREAR | MEDIA |
| `data/entities/ItemCompra.java` | Entidad Room: id, lugarCompraId (FK), nombre, cantidad, estaComprado | CREAR | MEDIA |
| `data/dao/LugarCompraDao.java` | DAO de lugares de compra (CRUD + consulta con @Relation a items) | CREAR | MEDIA |
| `data/dao/ItemCompraDao.java` | DAO de items (insertar, eliminar, marcar comprado) | CREAR | BAJA |
| `data/relations/LugarCompraConItems.java` | POJO `@Embedded` + `@Relation` (lugar con su lista de items) | CREAR | BAJA |
| `data/db/AppDatabase.java` | `version = 5` + `MIGRATION_3_4` y `MIGRATION_4_5` (no destructivas) + esquemas exportados `4.json` y `5.json` | MODIFICAR | ALTA |
| `data/preferences/TiposLugarPreferencias.java` | Catálogo local de tipos personalizados (SharedPreferences + JSON) | CREAR | BAJA |
| `ui/mapa/MapaLecturaController.java` | Long press en el mapa y en pines; InfoWindow de compra; navegación al detalle | MODIFICAR | ALTA |
| `ui/mapa/PinMapa.java` | Soporte para el color `@color/mapa_pin_compra` (distinto al de movimientos) | MODIFICAR | BAJA |
| `ui/mapa/LugarCompraInfoWindow.java` | InfoWindow con tipo, gasto, cantidad de ítems y "Ver detalle" | CREAR | MEDIA |
| `ui/mapa/MarcadorLugarCompra.java` | Marcador con long press propio (no confunde con el mapa vacío) | CREAR | MEDIA |
| `ui/compras/ItemCompraCreacionAdapter.java` | Adapter editable de ítems dentro del diálogo de creación | CREAR | MEDIA |
| `ui/compras/LugarCompraDetailActivity.java` | Placeholder del detalle (Fase 4: RecyclerView editable) | CREAR | BAJA |
| `ui/compras/LugarCompraItemAdapter.java` | Adapter del RecyclerView del detalle (Fase 4) | CREAR | MEDIA |
| `res/layout/dialog_crear_lugar_compra.xml` | Diálogo de creación: Spinner de tipo, gasto opcional y lista de ítems | CREAR | MEDIA |
| `res/layout/item_crear_item_compra.xml` | Fila editable (nombre, cantidad, eliminar) del diálogo | CREAR | BAJA |
| `res/layout/dialog_agregar_tipo_lugar.xml` | Mini-diálogo para tipos personalizados | CREAR | BAJA |
| `res/layout/mapa_info_window_lugar_compra.xml` | InfoWindow del pin de compra | CREAR | BAJA |
| `res/layout/activity_lugar_compra_detalle.xml` | Placeholder del detalle (Fase 4: RecyclerView y acciones) | CREAR | BAJA |
| `res/layout/item_lista_compra.xml` | Fila del RecyclerView del detalle (Fase 4) | CREAR | BAJA |
| `res/values/strings.xml` | Cadenas de creación, tipos, ítems, InfoWindow y detalle | MODIFICAR | BAJA |
| `res/values/colors.xml` | `@color/mapa_pin_compra` | MODIFICAR | BAJA |
| `res/drawable/ic_delete.xml` | Ícono de eliminar ítem/tipo (tinte desde el tema) | CREAR | BAJA |
| `app/schemas/com.localcash.data.db.AppDatabase/5.json` | Esquema exportado de la versión 5 | CREAR | BAJA |
| `test/.../LugarCompraDaoTest.java` | Persistencia de lugar + items (Room in-memory) | CREAR | MEDIA |
| `test/.../MigracionV3V4Test.java` | Migración 3→4 con `MigrationTestHelper` (Robolectric) | CREAR | MEDIA |
| `test/.../MigracionV4V5Test.java` | Migración 4→5 (gasto nullable, datos y FK preservados) | CREAR | MEDIA |

**Contrato de datos / interfaz expuesta:**
- **Entrada (desde el mapa):** long press en `(latitud, longitud)` → abre diálogo de creación con
  coordenadas prellenadas.
- **Salida (guardar lugar):** `LugarCompra` persistido con `latitud`/`longitud`, `tipoLugar`,
  `gastoPromedio`, `fechaCreacion` y su lista de `ItemCompra` asociados.
- **Salida (detalle):** `ItemCompra` agregado/eliminado/marcado como comprado; eliminación del
  `LugarCompra` tras confirmación.
- **Errores posibles:** tipo de lugar vacío → error de validación; item sin nombre o cantidad inválida
  → error de validación; eliminación sin confirmación → no se borra.

### 1.1 Esquema de Base de Datos (Room)

**Tabla `lugar_compra`** (entidad `LugarCompra`):

| Columna | Tipo Java | Tipo SQLite | Restricción |
|---|---|---|---|
| `id` | `long` | `INTEGER` | `PK` · `AUTOINCREMENT` |
| `latitud` | `double` | `REAL` | `NOT NULL` |
| `longitud` | `double` | `REAL` | `NOT NULL` |
| `tipoLugar` | `String` | `TEXT` | `NOT NULL` |
| `gastoPromedio` | `Long` (boxed) | `INTEGER` | `NULL` permitido · pesos CLP; `null` = sin gasto estimado |
| `fechaCreacion` | `long` | `INTEGER` | `NOT NULL` · epoch millis |

**Tabla `item_compra`** (entidad `ItemCompra`):

| Columna | Tipo Java | Tipo SQLite | Restricción |
|---|---|---|---|
| `id` | `long` | `INTEGER` | `PK` · `AUTOINCREMENT` |
| `lugarCompraId` | `long` | `INTEGER` | `FK → lugar_compra(id)` · `ON DELETE CASCADE` · índice |
| `nombre` | `String` | `TEXT` | `NOT NULL` |
| `cantidad` | `int` | `INTEGER` | `NOT NULL` (unidades; `≥ 1` en dominio) |
| `estaComprado` | `boolean` | `INTEGER` | `NOT NULL` (`1`/`0`) |

- **Relación:** uno-a-muchos `LugarCompra → ItemCompra`, resuelta con `@Relation` en
  `LugarCompraConItems` y `@Transaction` en el DAO.
- **Migración 3→4:** `MIGRATION_3_4` crea solo las dos tablas nuevas y el índice
  `idx_item_compra_lugar`; no altera las tablas existentes.
- **Migración 4→5:** `MIGRATION_4_5` cambia `gastoPromedio` a nullable. SQLite no permite
  retirar `NOT NULL` con `ALTER TABLE`, así que se copian los datos a tablas nuevas, se retiran
  las viejas y se recrean las definitivas con la FK explícita. El `DROP TABLE` solo ocurre
  después de copiar y dentro de la transacción de Room; `MigracionV4V5Test` verifica que no se
  pierdan lugares ni ítems (`data-model.md` §6).

---

## 2. Decisiones de Arquitectura

1. **ORM: Room (migración no destructiva 3 → 4).**
   - Opciones consideradas: (a) seguir con `SQLiteOpenHelper` nativo; (b) migrar a Room para las nuevas
     tablas.
   - Decisión: **(b) Room** para `lugar_compra` e `item_compra`, aprovechando `@Relation` para la
     consulta uno-a-muchos.
   - Justificación: `tech-stack.md` §3 prevé Room desde la Fase 3; la relación uno-a-muchos se expresa
     limpiamente con `@Relation`; y `.agent.md` §6.11 prohíbe migraciones destructivas (`DROP TABLE`).
   - Consecuencia aceptada: hay que escribir y testear `MIGRATION_3_4` y exportar `4.json`.

2. **Long press en el mapa → diálogo de creación.**
   - Opciones consideradas: (a) `OnMapLongClickListener` de osmdroid; (b) detector de gestos propio
     sobre el `MapView`.
   - Decisión: **(a) `OnMapLongClickListener` de osmdroid**.
   - Justificación: es la API nativa del motor de mapas, ya integrado en la Feature 009, y devuelve
     directamente el `GeoPoint` pulsado.
   - Consecuencia aceptada: el long press solo se habilita en la pestaña "Mapa" (no en el selector
     `MapaPickerActivity` de 009).

3. **Color de pin distinto por origen (contrato 009 → 010).**
   - Decisión: los lugares de compra usan `@color/mapa_pin_compra`; los movimientos siguen con
     `@color/mapa_pin_movimiento` (definido en 009).
   - Justificación: requirement explícito del usuario (CA-4) y contrato ya reservado en 009 §2.4.
   - Consecuencia aceptada: `PinMapa` debe soportar dos tintas; se añade una nueva constante de color
     y opcionalmente un drawable propio.

4. **Validación en dominio, no solo en UI.**
   - Decisión: tipo de lugar obligatorio, nombre de item no vacío, cantidad ≥ 1 se validan antes de
     persistir (análogo a `Ubicacion.crear` en 009).
   - Justificación: evita filas inválidas en la BD y mantiene una fuente única de verdad.
   - Consecuencia aceptada: más código de dominio, pero tests más baratos (JVM sin dispositivo).

5. **Migración 3 → 4 no destructiva.**
   - Decisión: `CREATE TABLE lugar_compra` y `CREATE TABLE item_compra` (nuevas, sin tocar tablas
     existentes), `version` de Room a 4, sin `DROP TABLE`.
   - Justificación: `.agent.md` §6.11 prohíbe `fallbackToDestructiveMigration()` y el `DROP TABLE`
     como estrategia destructiva; nunca se pierde información financiera.
   - Consecuencia aceptada: test obligatorio `MigracionV3V4Test`. La migración 4→5 usa el patrón
     de copia previa documentado en `data-model.md` (ver decisión #8), no un borrado destructivo.

6. **`gastoPromedio` como entero en pesos CLP, no `Double`.**
   - Opciones consideradas: (a) `Double` (promedio fraccionario); (b) entero en pesos CLP.
   - Decisión: **(b) entero**, porque es dinero y `.agent.md` §6.10 prohíbe `REAL`/`double` en la
     capa de datos; los pesos chilenos no tienen centavos en circulación. En la versión inicial se
     usó `long`; desde la Fase 2.5 es `Long` nullable para representar "sin gasto estimado"
     (decisión #8).
   - Justificación: mantiene la aritmética exacta y la coherencia con `Cuenta.saldo` y
     `Transaccion.monto`. La conversión a `BigDecimal` (si se necesita) vive en `domain/`.
   - Consecuencia aceptada: el promedio se redondea al peso al persistirse; el formulario puede
     capturarlo como entero o dejarlo vacío.

---

7. **Tipo de lugar: Spinner + catálogo local de personalizados (Fase 2.5).**
   - Opciones consideradas: (a) `EditText` libre; (b) `Spinner` con predefinidos y tipos
     personalizados persistidos.
   - Decisión: **(b)**. Los predefinidos viven en `strings.xml`
     (`tipos_lugar_predefinidos`); los personalizados se guardan en `SharedPreferences` como un
     `JSONArray` ordenado (`TiposLugarPreferencias`), con alta y baja desde el propio diálogo.
   - Justificación: el usuario pidió opciones consistentes y reutilizables sin agregar una tabla
     al esquema financiero; `SharedPreferences` es local, offline y no requiere migración.
   - Consecuencia aceptada: eliminar un tipo personalizado no altera los lugares ya creados con
     ese texto; el catálogo solo afecta a futuras selecciones.

8. **`gastoPromedio` nullable (`Long`) + migración 4 → 5 (Fase 2.5).**
   - Decisión: el gasto estimado es opcional; se persiste `NULL` cuando el usuario deja el campo
     vacío. Se mantiene la regla de dinero entero (pesos CLP), ahora con `Long` boxed.
   - Justificación: requisito explícito del usuario y semántica correcta para "no estimado"
     (distinto de `0`, que significaría un gasto estimado de cero pesos).
   - Consecuencia aceptada: `MIGRATION_4_5` recrea las tablas con copia previa y
     `MigracionV4V5Test` valida que datos, FK e índice sobrevivan.

9. **Lista de ítems editable al crear el lugar (Fase 2.5).**
   - Decisión: un `RecyclerView` acotado (5-6 filas visibles) con filas de nombre, cantidad y
     eliminar; al guardar se persiste todo con `insertLugarConItems` en una sola transacción.
   - Justificación: el usuario puede planificar la compra completa desde el pin, sin esperar a la
     Activity de detalle. La lista vacía sigue siendo válida (E-3).
   - Consecuencia aceptada: la validación de ítems ocurre en la UI antes de persistir; el adapter
     mantiene un modelo borrador en memoria.

10. **Interacción de pines: clic normal vs. long press (Fase 3).**
    - Decisión: clic normal en un pin de compra muestra un `InfoWindow` propio con resumen y botón
      "Ver detalle"; long press sobre el pin abre el detalle. El long press en mapa vacío sigue
      abriendo el diálogo de creación.
    - Implementación: `MarcadorLugarCompra` extiende `Marker` y consume el long press solo si
      `hitTest` confirma que el evento cayó sobre el icono. Los marcadores de movimiento también
      consumen el long press sobre su icono para no abrir el diálogo de creación por error.
    - Consecuencia aceptada: el detalle es un placeholder hasta la Fase 4; el contrato de
      navegación (`EXTRA_LUGAR_COMPRA_ID`) queda fijado desde ahora.

## 3. Riesgos Identificados

| # | Riesgo | Probabilidad | Impacto | Mitigación | Dueño |
|---|--------|--------------|---------|------------|-------|
| 1 | Teclado cubre campos del diálogo de creación (tipo, gasto, items) | Media | Alto | `windowSoftInputMode="adjustResize"` en la actividad/diálogo y scroll en el formulario | GG |
| 2 | RecyclerView con muchas listas/items lento en el scroll | Media | Medio | `RecyclerView` con `LinearLayoutManager`, `setHasFixedSize(true)` y view binding eficiente | GG |
| 3 | Migración 3→4 mal escrita → pérdida de esquemas existentes | Baja | Alto | `CREATE TABLE` solo para tablas nuevas + `MigracionV3V4Test` con datos sembrados | GG |
| 4 | Long press confunde con el pan/zoom del `MapView` | Media | Medio | Ajustar el umbral de detección de long press de osmdroid; documentar el comportamiento | GG |
| 5 | Divergencia entre `data-model.md` y las migraciones reales | Media | Medio | Actualizar `data-model.md` en el mismo cambio que el esquema (regla del documento) | GG |
| 6 | Migración 4→5 pierde lugares o ítems al recrear tablas | Baja | Alto | Copiar a tablas de paso antes de retirar las viejas; `MigracionV4V5Test` verifica datos, FK, índice y cascada | GG |
| 7 | Long press sobre un pin dispara también el diálogo de creación | Media | Medio | `MarcadorLugarCompra` y `MarcadorMovimiento` consumen el gesto solo si `hitTest` cae sobre el icono | GG |

**Criterio de abortar/replantear:** si la migración a Room no puede coexistir con las tablas
creadas por `SQLiteOpenHelper` (Fases 1-2) sin riesgo de pérdida de datos, se detiene y se revisa
la estrategia de persistencia antes de continuar.

---

## 4. Dependencias Externas

| Dependencia | Tipo | Estado del acuerdo | Plan B si falla |
|---|---|---|---|
| Room (AndroidX) | Librería | Registrada en `tech-stack.md` §3 | Seguir con `SQLiteOpenHelper` nativo para las dos tablas |
| osmdroid 6.1.x (Feature 009) | Librería | Ya integrada, sin cambios | N/A (no se añade dependencia de mapa nueva) |
| `androidx.recyclerview` | Librería | Registrada en `tech-stack.md` §3 | `ListView` (menos flexible, último recurso) |

**Requisitos previos del entorno:** emulador o dispositivo para `connectedCheck`; ninguna credencial
real en documentos.

---

## 5. Estrategia de Verificación

| Criterio | Cómo se verifica |
|---|---|
| CA-1 | Prueba manual en emulador: long press en el mapa abre el diálogo con coordenadas |
| CA-2 | Prueba manual + `TiposLugarPreferenciasTest`: Spinner con tipos personalizados; gasto vacío persiste `NULL` |
| CA-3 | `LugarCompraDaoTest`: lugar con items se guarda y se lee completo |
| CA-4 | Prueba manual: pin de compra con color distinto al de movimiento |
| CA-5 | Prueba manual: InfoWindow con tipo, gasto (o "Sin gasto estimado"), cantidad de ítems y "Ver detalle" |
| CA-6 | `LugarCompraItemAdapter` + prueba manual: agregar/eliminar/tachar items |
| CA-7 | Prueba manual: diálogo de confirmación antes de eliminar |
| CA-8 | Prueba manual: long press en pin abre detalle; long press en mapa vacío abre creación |
| Migración 3→4 | `MigracionV3V4Test` (Robolectric + `MigrationTestHelper`) |
| Migración 4→5 | `MigracionV4V5Test` (Robolectric + `MigrationTestHelper`) |
| Offline | Prueba en modo avión: creación y lectura de lugares de compra sin red |

**Orden antes de cerrar (Definition of Done de `tasks.md`):** `./gradlew lint` →
`testDebugUnitTest` → `connectedCheck` → `assembleDebug` (`.agent.md` §3).

---

## 6. Dependencias entre features

- **Depende de:** 007 (`007-cuentas`), 008 (`008-historial`) y 009 (`009-geolocalizacion-map`).
- **Habilita a:** ninguna feature pendiente definida aún.

---

## 7. Nota de numeración

La convención del proyecto comparte la numeración de tres dígitos entre `spec/features/NNN-*` y
`doc/NNN-*`. Esta feature usa `010`, reservada en el plan de 009 (§7). El registro de memoria de esta
feature será, por tanto, `doc/010-lista-compras-geolocalizada.md`.
