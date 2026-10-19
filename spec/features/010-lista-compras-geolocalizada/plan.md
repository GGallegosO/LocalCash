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
| `data/db/AppDatabase.java` | `version = 4` + `MIGRATION_3_4` (no destructiva) + esquemas exportados `4.json` | MODIFICAR | ALTA |
| `ui/mapa/MapaLecturaController.java` | Detectar long press en el `MapView` y lanzar el diálogo de creación | MODIFICAR | ALTA |
| `ui/mapa/PinMapa.java` | Soporte para el color `@color/mapa_pin_compra` (distinto al de movimientos) | MODIFICAR | BAJA |
| `ui/compras/LugarCompraDetailActivity.java` | Activity con RecyclerView para gestionar la lista de items | CREAR | ALTA |
| `ui/compras/LugarCompraItemAdapter.java` | Adapter del RecyclerView (agregar, eliminar, tachar) | CREAR | MEDIA |
| `res/layout/dialog_nuevo_lugar_compra.xml` | Diálogo/formulario de creación (tipo, gasto promedio, items) | CREAR | MEDIA |
| `res/layout/activity_lugar_compra_detalle.xml` | Activity de detalle con RecyclerView y acciones | CREAR | MEDIA |
| `res/layout/item_lista_compra.xml` | Fila del RecyclerView (nombre, cantidad, checkbox comprado) | CREAR | BAJA |
| `res/values/strings.xml` | Cadenas de creación, detalle, confirmación de eliminación | MODIFICAR | BAJA |
| `res/values/colors.xml` | `@color/mapa_pin_compra` | MODIFICAR | BAJA |
| `res/drawable/ic_mapa_pin_compra.xml` | Icono de pin de compra (opcional, si difiere del de movimiento) | CREAR | BAJA |
| `app/schemas/com.localcash.data.db.AppDatabase/4.json` | Esquema exportado de la versión 4 | CREAR | BAJA |
| `test/.../LugarCompraDaoTest.java` | Persistencia de lugar + items (Room in-memory) | CREAR | MEDIA |
| `test/.../MigracionV3V4Test.java` | Migración 3→4 con `MigrationTestHelper` (Robolectric) | CREAR | MEDIA |

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
| `gastoPromedio` | `long` | `INTEGER` | `NOT NULL` · pesos CLP |
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
- **Migración:** `MIGRATION_3_4` crea solo las dos tablas nuevas y el índice
  `idx_item_compra_lugar`; no altera las tablas existentes.

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
   - Justificación: `.agent.md` §6.11 prohíbe `fallbackToDestructiveMigration()` y `DROP TABLE`;
     nunca se pierde información financiera.
   - Consecuencia aceptada: test obligatorio `MigracionV3V4Test`.

6. **`gastoPromedio` como `long` (pesos CLP), no `Double`.**
   - Opciones consideradas: (a) `Double` (promedio fraccionario); (b) `long` en pesos CLP.
   - Decisión: **(b) `long`**, porque es dinero y `.agent.md` §6.10 prohíbe `REAL`/`double` en la
     capa de datos; los pesos chilenos no tienen centavos en circulación.
   - Justificación: mantiene la aritmética exacta y la coherencia con `Cuenta.saldo` y
     `Transaccion.monto`. La conversión a `BigDecimal` (si se necesita) vive en `domain/`.
   - Consecuencia aceptada: el promedio se redondea al peso al persistirse; el formulario puede
     capturarlo como entero.

---

## 3. Riesgos Identificados

| # | Riesgo | Probabilidad | Impacto | Mitigación | Dueño |
|---|--------|--------------|---------|------------|-------|
| 1 | Teclado cubre campos del diálogo de creación (tipo, gasto, items) | Media | Alto | `windowSoftInputMode="adjustResize"` en la actividad/diálogo y scroll en el formulario | GG |
| 2 | RecyclerView con muchas listas/items lento en el scroll | Media | Medio | `RecyclerView` con `LinearLayoutManager`, `setHasFixedSize(true)` y view binding eficiente | GG |
| 3 | Migración 3→4 mal escrita → pérdida de esquemas existentes | Baja | Alto | `CREATE TABLE` solo para tablas nuevas + `MigracionV3V4Test` con datos sembrados | GG |
| 4 | Long press confunde con el pan/zoom del `MapView` | Media | Medio | Ajustar el umbral de detección de long press de osmdroid; documentar el comportamiento | GG |
| 5 | Divergencia entre `data-model.md` y las migraciones reales | Media | Medio | Actualizar `data-model.md` en el mismo cambio que el esquema (regla del documento) | GG |

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
| CA-2 | Prueba manual + `LugarCompraDaoTest`: tipo y gasto promedio se persisten |
| CA-3 | `LugarCompraDaoTest`: lugar con items se guarda y se lee completo |
| CA-4 | Prueba manual: pin de compra con color distinto al de movimiento |
| CA-5 | Prueba manual: popup con resumen (1-2 items) y botón "Ver detalle" |
| CA-6 | `LugarCompraItemAdapter` + prueba manual: agregar/eliminar/tachar items |
| CA-7 | Prueba manual: diálogo de confirmación antes de eliminar |
| Migración | `MigracionV3V4Test` (Robolectric + `MigrationTestHelper`) |
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
