# doc/007-cuentas.md — Registro de Memoria: Feature 007 - Cuentas y Transacciones Básicas

> **Propósito de este archivo:** Registro de memoria de la Feature 007 según `.agent.md` §7.
> Aquí se registran las tareas ejecutadas (T1 → T19), decisiones, lecciones y próximos pasos.

---

## Estado de la sesión (retomada tras /compact)

- **Alcance actual:** Capa Datos/Dominio (T1→T11) completada + Fase 3 UI iniciada (T11). Próxima tarea: **T12**.
- **Completadas:** T1 (Config Room), T2 (Entidades), T3 (DAOs), T4 (AppDatabase Singleton), T5 (Modelos de dominio con BigDecimal y Enums), T6 (ErrorCode), T7 (TransaccionService + BusinessRuleException), T8 (Tests de TransaccionService: 8/8), T9 (Tests de DAOs con Robolectric: 5/5), T10 (SeedDataInitializer + registro en AppDatabase), **T11 (Menú navegación inferior)**: `bottom_nav_menu.xml` con 4 ítems (`nav_cuentas`, `nav_nuevo`, `nav_mapa`, `nav_contactos`) + strings `aCuentas_` en `strings.xml`. `assembleDebug` BUILD SUCCESSFUL, 0 warnings. Total 13/13 tests en verde.
- **Reglas no negociables:**
  1. Dinero: `long` en entidades Room (pesos CLP directos, NO centavos, NO double). `BigDecimal` en modelos de dominio.
  2. Arquitectura: `domain/` NO importa `androidx.room` ni Android SDK (solo `java.*`).
  3. Testing: cobertura ≥70% en `data/` y `domain/` (`.agent.md` §5.3).
  4. Memoria: un `doc/007-cuentas.md` único va actualizado tras cada tarea (§7 del contrato).
- **Siguiente tarea inmediata:** **T12** — Crear layout `activity_cuentas.xml` (Fase 3 UI). Reglas de UI: cero colores hardcodeados (solo atributos Material 3), sin `ConstraintLayout` (solo `LinearLayout`/`RelativeLayout`), `MaterialButton` con `icon` y no `app:drawableStartCompat`.

---

## Fichas de tarea

| Campo | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 | T9 | T10 | T11 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **Fecha** | 2026-10-07 | 2026-10-07 | 2026-10-07 | 2026-10-07 | 2026-10-07 | 2026-10-07 | 2026-10-07 | 2026-10-07 | 2026-10-07 | 2026-10-14 | 2026-10-14 |
| **Tarea ejecutada** | Configurar Room en el proyecto | Crear entidades Room en `data/entities/` | Crear DAOs en `data/dao/` | Crear `AppDatabase.java` en `data/db/` | Crear modelos de dominio en `domain/model/` | Crear códigos de error en `domain/error/ErrorCode.java` | Crear `TransaccionService.java` en `domain/service/` | Crear tests unitarios de `TransaccionService` | Crear tests de DAOs (`CuentaDaoTest`, `TransaccionDaoTest`) | Crear `SeedDataInitializer.java` y registrar en `AppDatabase` | Crear menú `bottom_nav_menu.xml` + strings `aCuentas_` |
| **Responsable** | GG | GG | GG | GG | GG | GG | GG | GG | GG | GG | GG |
| **Duración estimada vs real** | 15 min vs ~5 min | 30 min vs ~10 min | 30 min vs ~10 min | 30 min vs ~15 min | 30 min vs ~15 min | 10 min vs ~5 min | 45 min vs ~15 min | 45 min vs ~15 min | 60 min vs ~40 min | 30 min vs ~10 min | 15 min vs ~5 min |
| **Estado** | Completado | Completado | Completado | Completado | Completado | Completado | Completado | Completado | Completado | Completado | Completado |

---

## Tarea Ejecutada (resumen narrativo)

**T1 — Configurar Room:** Se configuró **Room (SQLite)** como capa de persistencia para datos financieros en la Feature 007, introduciendo la base de datos relacional local por primera vez en el proyecto. Hasta este momento, la app solo usaba `EncryptedSharedPreferences` para credenciales de usuario (Feature 005/006). Se agregaron las dependencias oficiales de Room 2.6.1 y se configuró la exportación automática del schema SQLite en `app/schemas/` para documentación técnica y validación académica.

**T2 — Crear entidades Room:** Se crearon las dos entidades que definen el núcleo financiero del esquema: `Cuenta` (patrimonio del usuario) y `Transaccion` (movimiento ingreso/egreso). Ambas almacenan dinero como `long` en **pesos CLP directos** (sin centavos, `.agent.md` §6.10, `data-model.md` §3.2/§3.4). `Transaccion` declara clave foránea a `Cuenta` con `ON DELETE CASCADE` e índice sobre la columna hija `cuentaId`.

**T3 — Crear DAOs:** Se crearon las interfaces de acceso a datos `CuentaDao` y `TransaccionDao` con las operaciones de inserción, consulta observable (`LiveData`), actualización de saldo y borrado. Los DAOs solo persisten: no deciden reglas de negocio (la validación de saldo vive en `domain/service`, pendiente T7). Room valida los literales SQL de los `@Query` en compilación.

**T4 — Crear AppDatabase:** Se creó la clase abstracta `AppDatabase` (`RoomDatabase`) con `@Database(entities = {Cuenta, Transaccion}, version = 1, exportSchema = true)`, los métodos abstractos `cuentaDao()`/`transaccionDao()`, un **singleton thread-safe** (doble verificación con `volatile`) y un `ExecutorService` de un solo hilo para las operaciones síncronas de los DAOs. El registro de `SeedDataInitializer` queda pendiente a propósito (`// TODO: Registrar SeedDataInitializer en T10`). Bonus: la primera compilación con `@Database` reveló 4 warnings de constructores duplicados en las entidades; se corrigieron con `@Ignore` y se exportó el schema en `app/schemas/`.

**T5 — Crear modelos de dominio:** Se crearon los modelos de negocio inmutables `Cuenta` y `Transaccion` (con `BigDecimal` para dinero) más los enums `TipoCuenta` (EFECTIVO/AHORRO/TARJETA) y `TipoTransaccion` (INGRESO/EGRESO). Cada modelo incluye el único par de conversión `long` ↔ `BigDecimal` vía `toEntity()` / `fromEntity()` (factor 1: pesos CLP directos). La capa `domain/` no importa clases de Room ni de Android: solo `java.math.BigDecimal`.

**T6 — Crear códigos de error:** Se creó el enum `ErrorCode` en `domain/error/` con los cuatro códigos de negocio de la feature (`CUENTA_VACIA`, `MONTO_INVALIDO`, `SALDO_INSUFICIENTE`, `CUENTA_NO_ENCONTRADA`). El dominio propaga códigos, nunca texto: la traducción a mensajes accionables es responsabilidad de `ui/` (`.agent.md` §5.4).

**T7 — Crear TransaccionService:** Se creó el servicio de lógica de negocio pura en `domain/service/TransaccionService.java` con dos métodos `static` sin estado: `validarTransaccion(Transaccion, Cuenta)` (valida monto no nulo y > 0, cuenta no nula, y para EGRESO que `saldo >= monto`, lanzando `BusinessRuleException` con el `ErrorCode` correspondiente) y `calcularNuevoSaldo(Cuenta, Transaccion)` (suma para INGRESO, resta para EGRESO, todo con `BigDecimal`). Junto a él se creó `domain/error/BusinessRuleException.java`, excepción `unchecked` que transporta el `ErrorCode` para que `ui/` lo traduzca a mensaje (`.agent.md` §5.4). El inventario de archivos de `plan.md` se sincronizó con este archivo nuevo (exigido por `tasks.md` T7, ausente en el plan). Verificado: `domain/` solo importa `java.*` y paquetes `com.localcash.domain.*`.

**T8 — Crear tests de TransaccionService:** Se creó `TransaccionServiceTest.java` (JUnit 4.13.2, JVM pura, sin Robolectric ni Android) con los 4 tests mínimos de `tasks.md` (`testIngresoAumentaSaldo`, `testEgresoDisminuyeSaldo`, `testEgresoConSaldoInsuficienteLanzaExcepcion`, `testMontoInvalidoLanzaExcepcion`) más 4 complementarios de nulos, frontera y dominio para garantizar la cobertura ≥70% del criterio de salida. Resultado: **8 tests, 0 fallos, 0 errores** (`testDebugUnitTest` verde) y `assembleDebug` sin warnings. Todo el dinero en `BigDecimal` con literales String; aserciones por `compareTo` (mismo criterio que el servicio).

**T9 — Crear tests de DAOs:** La tarea empezó con un **hallazgo de plan**: se sondeó empíricamente (test temporal, después borrado) que `Room.inMemoryDatabaseBuilder()` **no puede** correr en JVM pura — falla con `Method getCacheDir ... not mocked` porque Room necesita `Context`/SQLite reales de Android. La premisa de `tasks.md` T9 ("sin dispositivo" en JVM local) era falsa sin Robolectric. Consultado el usuario (SDD: detente, corrige el plan con justificación), se eligió **Robolectric**, tecnología **ya preaprobada** en `tech-stack.md` §4 (fila "Integración": Robolectric 4.10+); se registró la adopción concreta en §7 (candidatas evaluadas) y se añadió `org.robolectric:robolectric:4.17` + `testOptions.unitTests.includeAndroidResources = true` (con `--add-exports java.base/jdk.internal.access` para el daemon JDK 21). Se crearon `CuentaDaoTest` (insert/getAll, updateSaldo, delete) y `TransaccionDaoTest` (insert/getByCuentaId, getAll con orden `fecha DESC`) con `Room.inMemoryDatabaseBuilder` en memoria + helper `LiveDataTestUtil` (patrón observe + `idle()` del looper + condición con timeout). **Resultado: 13/13 tests en verde** (8 de T8 + 5 de T9) en JVM local, sin emulador, y `assembleDebug` sin warnings.

**T10 — Crear `SeedDataInitializer.java`:** Se creó `data/db/SeedDataInitializer.java` (`final`, extiende `RoomDatabase.Callback`) con override de `onCreate(@NonNull SupportSQLiteDatabase)`: inserta la cuenta "Efectivo Demo" (tipo `EFECTIVO`, saldo `1_000_000L` = $1.000.000 CLP, moneda `CLP`) vía `execSQL` con argumentos enlazados `?` (no se usan DAOs: el callback corre durante la apertura, los DAOs aún no existen). En `AppDatabase.getInstance()` se reemplazó el `// TODO` por `.addCallback(new SeedDataInitializer())`. `onCreate` solo se dispara en la creación del archivo BD → la semilla no duplica cuentas en aperturas posteriores. **Cierre de la capa Datos/Dominio (T1→T10).** Verificación: `testDebugUnitTest` **13/13 verde** (los tests de DAO usan `inMemoryDatabaseBuilder`, que no invoca el callback — comportamiento esperado) y `assembleDebug` **BUILD SUCCESSFUL, 0 warnings**. El criterio de salida de la casilla (cuenta demo existe al primer arranque) se verifica en `connectedCheck` (T19/DoD), no en JVM.

**T11 — Crear menú navegación inferior:** Se creó `app/src/main/res/menu/bottom_nav_menu.xml` con 4 ítems de navegación inferior: `nav_cuentas` (`account_balance_wallet`), `nav_nuevo` (`add_circle`), `nav_mapa` (`map`), `nav_contactos` (`person`). Se agregaron 4 strings en `app/src/main/res/values/strings.xml` con prefijo `aCuentas_`: `aCuentas_nav_cuentas` ("Cuentas"), `aCuentas_nav_nuevo` ("Nuevo"), `aCuentas_nav_mapa` ("Mapa"), `aCuentas_nav_contactos` ("Contactos"). Los íconos son **vectoriales** (`@drawable/ic_...`) — Material Icons — configurados en `res/drawable/ic_menu_*.xml`. No se tocaron activities, layouts ni clases Java (solo XML). Verificación: `./gradlew assembleDebug` → **BUILD SUCCESSFUL, 0 warnings**. **Inicio de Fase 3 UI.**

---

## Archivos Creados / Modificados

**Creados:**

- `app/src/main/java/com/localcash/data/entities/Cuenta.java` (T2) — `@Entity(tableName = "cuentas")`: `id` (long, `autoGenerate`), `nombre` (String), `tipo` (String: EFECTIVO/AHORRO/TARJETA), `saldo` (long, pesos CLP), `moneda` (String, `@ColumnInfo(defaultValue = "CLP")`). Incluye constructor vacío (requerido por Room) y constructor completo con saldo de apertura.
- `app/src/main/java/com/localcash/data/entities/Transaccion.java` (T2) — `@Entity(tableName = "transacciones")` con `@ForeignKey(entity = Cuenta.class, parentColumns = "id", childColumns = "cuentaId", onDelete = ForeignKey.CASCADE)` e `@Index("cuentaId")`: `id` (long, `autoGenerate`), `cuentaId` (long), `tipo` (String: INGRESO/EGRESO), `monto` (long, pesos CLP, siempre positivo), `concepto` (String), `fecha` (long, timestamp epoch millis). Incluye constructor vacío y constructor completo.
- `app/src/main/java/com/localcash/data/dao/CuentaDao.java` (T3) — `@Dao` interface: `@Insert insert(Cuenta)`, `@Query("SELECT * FROM cuentas") LiveData<List<Cuenta>> getAll()`, `@Query("UPDATE cuentas SET saldo = :saldo WHERE id = :cuentaId") updateSaldo(long, long)`, `@Delete delete(Cuenta)`.
- `app/src/main/java/com/localcash/data/dao/TransaccionDao.java` (T3) — `@Dao` interface: `@Insert insert(Transaccion)`, `@Query("SELECT * FROM transacciones WHERE cuentaId = :cuentaId ORDER BY fecha DESC") LiveData<List<Transaccion>> getByCuentaId(long)`, `@Query("SELECT * FROM transacciones ORDER BY fecha DESC") LiveData<List<Transaccion>> getAll()`.
- `app/src/main/java/com/localcash/data/db/AppDatabase.java` (T4) — `@Database(entities = {Cuenta.class, Transaccion.class}, version = 1, exportSchema = true)`, abstract class `RoomDatabase`: singleton thread-safe (doble verificación + `volatile`), `Executors.newSingleThreadExecutor()` compartido (`getExecutor()`), `getInstance(Context)` con `applicationContext`, `// TODO: Registrar SeedDataInitializer en T10`, métodos abstractos `cuentaDao()` y `transaccionDao()`.
- `app/schemas/com.localcash.data.db.AppDatabase/1.json` (T4, generado) — schema SQLite exportado por Room (DoD T19).
- `app/src/main/java/com/localcash/domain/model/TipoCuenta.java` (T5) — enum EFECTIVO, AHORRO, TARJETA (dominio cerrado; `name()` = String persistido en la entidad).
- `app/src/main/java/com/localcash/domain/model/TipoTransaccion.java` (T5) — enum INGRESO, EGRESO (sin `transferencia`, fuera de alcance según `mission.md` §3).
- `app/src/main/java/com/localcash/domain/model/Cuenta.java` (T5) — modelo de dominio inmutable: `id` (long), `nombre` (String), `tipo` (TipoCuenta), `saldo` (BigDecimal); constante `MONEDA_CLP`; `toEntity()` / `fromEntity()` con `longValueExact()` / `BigDecimal.valueOf(long)`; `toString()` para logs.
- `app/src/main/java/com/localcash/domain/model/Transaccion.java` (T5) — modelo de dominio inmutable: `id`, `cuentaId` (long), `tipo` (TipoTransaccion), `monto` (BigDecimal), `concepto` (String), `fecha` (long); `toEntity()` / `fromEntity()` con el mismo par de conversión; `toString()` para logs.
- `app/src/main/java/com/localcash/domain/error/ErrorCode.java` (T6) — enum con `CUENTA_VACIA`, `MONTO_INVALIDO`, `SALDO_INSUFICIENTE`, `CUENTA_NO_ENCONTRADA`; Javadoc por constante; cero dependencias externas (solo `java.*` implícitas).
- `app/src/main/java/com/localcash/domain/error/BusinessRuleException.java` (T7) — excepción `extends RuntimeException` que transporta `ErrorCode` (`getErrorCode()`); mensaje técnico por defecto `"Regla de negocio violada: <CODIGO>"`; sin dependencias Android.
- `app/src/main/java/com/localcash/domain/service/TransaccionService.java` (T7) — servicio `final` con constructor privado y métodos `static`: `validarTransaccion(Transaccion, Cuenta)` lanza `BusinessRuleException` con `MONTO_INVALIDO` (monto nulo o ≤ 0), `CUENTA_NO_ENCONTRADA` (cuenta nula) o `SALDO_INSUFICIENTE` (EGRESO con `saldo < monto`, vía `compareTo`); `calcularNuevoSaldo(Cuenta, Transaccion)` retorna `BigDecimal` (add para INGRESO, subtract para EGRESO) y lanza `IllegalStateException` si el tipo está fuera del dominio cerrado.
- `app/src/test/java/com/localcash/domain/service/TransaccionServiceTest.java` (T8) — JUnit 4 puro (JVM, sin Android): 8 tests — los 4 mínimos de `tasks.md` (ingreso suma 1_000_000 + 50_000 = 1_050_000; egreso resta → 950_000; egreso > saldo → `SALDO_INSUFICIENTE`; monto 0 y negativo → `MONTO_INVALIDO`) + `testCuentaNulaLanzaExcepcion` (`CUENTA_NO_ENCONTRADA` en validar y calcular), `testTransaccionNulaLanzaExcepcion` (`MONTO_INVALIDO` en validar y calcular), `testEgresoIgualAlSaldoPermitido` (frontera `>=` → saldo 0) y `testTipoFueraDeDominioLanzaIllegalState` (dominio cerrado). Helpers con `BigDecimal` literales String; aserciones por `compareTo`.
- `app/src/test/java/com/localcash/testing/LiveDataTestUtil.java` (T9) — helper de test: `awaitValue(LiveData)` / `awaitValue(LiveData, Predicate)` con `observeForever` + `idle()` del looper principal de Robolectric en bucle con timeout de 5s; sin dependencias adicionales.
- `app/src/test/java/com/localcash/data/dao/CuentaDaoTest.java` (T9) — `@RunWith(RobolectricTestRunner.class)`, Room in-memory: `testInsertAndGetAll()` (1 cuenta, nombre/tipo/saldo/moneda), `testUpdateSaldo()` (500_000 → 750_000 vía `updateSaldo`), `testDelete()` (getAll queda vacío).
- `app/src/test/java/com/localcash/data/dao/TransaccionDaoTest.java` (T9) — `@RunWith(RobolectricTestRunner.class)`, Room in-memory: `testInsertAndGetByCuentaId()` (1 movimiento con FK, tipo, monto, concepto, fecha) y `testGetAll()` (3 movimientos insertados fuera de orden con fechas 1000/3000/2000 → retorno `fecha DESC` = 3000, 2000, 1000).
- `app/src/main/java/com/localcash/data/db/SeedDataInitializer.java` (T10) — `public final class SeedDataInitializer extends RoomDatabase.Callback`: override `onCreate(@NonNull SupportSQLiteDatabase db)` que inserta la cuenta demo (`"Efectivo Demo"`, `"EFECTIVO"`, `1_000_000L`, `"CLP"`) con `execSQL` + bindArgs `?`. Constantes privadas del SQL (tabla, nombre, tipo, saldo, moneda); sin DAOs (el callback corre durante la apertura de la BD).
- `app/src/main/res/menu/bottom_nav_menu.xml` (T11) — menú de navegación inferior con 4 ítems: `nav_cuentas` (`ic_menu_account_balance_wallet`), `nav_nuevo` (`ic_menu_add_circle`), `nav_mapa` (`ic_menu_map`), `nav_contactos` (`ic_menu_person`). Íconos vectoriales en `res/drawable/ic_menu_*.xml` (Material Icons, sin `@android:drawable/`).
- `app/src/main/res/values/strings.xml` (T11 modificado) — agregados 4 strings con prefijo `aCuentas_`: `aCuentas_nav_cuentas` ("Cuentas"), `aCuentas_nav_nuevo` ("Nuevo"), `aCuentas_nav_mapa` ("Mapa"), `aCuentas_nav_contactos` ("Contactos").

**Modificados:**

- `app/build.gradle` (T1) — modificaciones:
  - Agregado `implementation "androidx.room:room-runtime:2.6.1"` (runtime de Room)
  - Agregado `annotationProcessor "androidx.room:room-compiler:2.6.1"` (procesador de anotaciones)
  - Agregado `testImplementation "androidx.room:room-testing:2.6.1"` (testing de Room)
  - Configurado `room.schemaLocation` en `defaultConfig.javaCompileOptions.annotationProcessorOptions.arguments` → `"$projectDir/schemas"`
- `app/src/main/java/com/localcash/data/entities/Cuenta.java` (corregido en T4) — constructor con parámetros anotado con `@Ignore` (elimina warning "multiple good constructors").
- `app/src/main/java/com/localcash/data/entities/Transaccion.java` (corregido en T4) — constructor con parámetros anotado con `@Ignore` (misma corrección).
- `spec/features/007-cuentas/007-cuentas-plan.md` (T7) — inventario de archivos: agregada la fila de `domain/error/BusinessRuleException.java` (CREAR, BAJA). Sincronización exigida por `.agent.md`/SDD: `tasks.md` T7 pide "excepciones personalizadas con ErrorCode" y el inventario del plan no la listaba; se corrige el plan, no se ejecuta algo al margen.
- `app/build.gradle` (T9) — modificaciones:
  - Agregado `testImplementation "org.robolectric:robolectric:4.17"` (tests de DAO en JVM)
  - Agregado `testOptions { unitTests { includeAndroidResources = true; all { jvmArgs '--add-exports=java.base/jdk.internal.access=ALL-UNNAMED' } } }` (Robolectric necesita recursos fusionados y el export del paquete interno de `java.base` en el daemon JDK 21)
- `spec/constitution/tech-stack.md` (T9) — §7 "Registro de cambios del stack": fila de adopción concreta de Robolectric 4.17 para los tests de DAO de la Feature 007, con motivo (sondeo empírico) y candidatas evaluadas. La fila §4 "Integración" ya preaprobaba Robolectric 4.10+.
- `app/src/main/java/com/localcash/data/db/AppDatabase.java` (T10) — en `getInstance()`: el `// TODO: Registrar SeedDataInitializer en T10` fue reemplazado por `.addCallback(new SeedDataInitializer())` en el builder (antes de `.build()`).

---

## Decisiones Tomadas (con justificación)

1. **Versión de Room: 2.6.1 (estable)**
   _Justificación:_ Es la versión estable actual compatible con `compileSdk 36` y `minSdk 28`. Evita versiones alpha/beta que podrían introducir inestabilidad en el esquema.

2. **`room.schemaLocation` apuntando a `$projectDir/schemas`**
   _Justificación:_ Cumple con el requisito académico del `plan.md` (Decisión #6) y `tasks.md` (T19) de exportar el schema SQLite para documentación técnica. La carpeta `app/schemas/` se generará automáticamente al compilar con una entidad `@Database`.

3. **`annotationProcessor` para `room-compiler` (no `kapt` ni `ksp`)**
   _Justificación:_ El proyecto es **100% Java** (`.agent.md` §2, §5.3). `kapt`/`ksp` son para Kotlin. `annotationProcessor` es el estándar para Java y Room lo soporta completamente.

4. **`room-testing` en `testImplementation` (no `androidTestImplementation`)**
   _Justificación:_ Permite tests unitarios JVM locales con `Room.inMemoryDatabaseBuilder()` (T9) sin requerir dispositivo/emulador. `connectedCheck` se reserva para tests de integración UI.

5. **Ubicación del bloque `javaCompileOptions` dentro de `defaultConfig`**
   _Justificación:_ Room requiere que `room.schemaLocation` se declare en `javaCompileOptions.annotationProcessorOptions.arguments` dentro de `defaultConfig` (no en `buildTypes` ni fuera de `android{}`). Es la ubicación canónica según documentación de Room.

6. **Dinero como `long` en pesos CLP directos en las entidades (no centavos, no `double`)**
   _Justificación:_ `.agent.md` §6.10 y `data-model.md` §1.4/§3: los pesos chilenos no tienen subdivisión en circulación desde 1984, así que `$1.000.000 CLP = 1_000_000L`. `BigDecimal` se reserva para `domain/`; la conversión vive en un único par de funciones (T5).

7. **`@Index("cuentaId")` en `Transaccion` (agregado respecto a lo pedido en `tasks.md` T3/T2)**
   _Justificación:_ Room emite un warning de compilación si la columna hija de una `@ForeignKey` no está indexada (`LR_ROOM_FOREIGN_KEY_INDEX`). Como el DoD exige cero warnings y el índice además acelera el `WHERE cuentaId = :id` del historial, se declara desde la entidad. Es una adición al plan detectada en la ejecución (plan.md §1 lista solo los campos; se registra aquí).

8. **`fecha` como `long` (epoch millis) en la entidad, no `TEXT` ISO-8601**
   _Justificación:_ El contrato de datos de `plan.md` §1 y `tasks.md` T2 especifican explícitamente `fecha (long, timestamp)`, y es lo que consume el spinner/formulario de la UI. Divergencia consciente con `data-model.md` §3.4 (que define `fecha_hora TEXT ISO-8601`): debe resolverse en T19 actualizando `data-model.md` o migrando la columna; queda registrada como deuda documental.

9. **Esquema de la feature es un subconjunto del esquema canónico**
   _Justificación:_ `Cuenta` no incluye `saldo_inicial` ni `tipo_cuenta`/`CHECK`; `Transaccion` no incluye `categoria_id` ni geolocalización. `plan.md` §1 define el alcance reducido de T2-T19 (mapas y categorías son features posteriores: 008/009). La divergencia con `data-model.md` debe reconciliarse en T19 (tarea DoD: "`data-model.md` actualizado si la feature tocó el esquema").

10. **`updateSaldo(cuentaId, saldo)` persiste un valor absoluto calculado en dominio**
    _Justificación:_ El cálculo del nuevo saldo (`saldo actual ± monto`) ocurre en `domain/service/TransaccionService.calcularNuevoSaldo()` con `BigDecimal`; el DAO solo escribe el resultado. Cumple `.agent.md` §5.1: `data/` implementa lo que `domain/` define y no decide reglas propias. La escritura por transacción (insert de movimiento + update de saldo) se orquestará en T16.

11. **Consultas devuelven `LiveData` (no `List` directo)**
    _Justificación:_ Room exige que las consultas que devuelven colecciones se ejecuten fuera del hilo principal; `LiveData` resuelve eso y notifica automáticamente a la UI ante cada cambio (DoD: actualización visible en <500ms). Consistente con el patrón de `tasks.md` T3.

12. **Singleton con doble verificación + `volatile` (no synchronized en todo el método)**
    _Justificación:_ Thread-safe bajo concurrencia sin el coste de bloquear `getInstance()` en cada llamada; `volatile` garantiza la publicación segura de la instancia entre hilos. El `applicationContext` evita leaks si una Activity consultara la BD.

13. **Un solo `ExecutorService` estático (`newSingleThreadExecutor`) compartido por la app**
    _Justificación:_ Serializa todas las escrituras de la BD: imposibilita la condición de carrera "leer-restar-escribir" del saldo porque nunca hay dos escrituras simultáneas (refuerzo en capa de código de la regla `data-model.md` §4.2 de aritmética atómica). Exponerlo vía `getExecutor()` centraliza el punto de ejecución en background para `ui/`.

14. **`SeedDataInitializer` NO registrado en T4 (solo `// TODO` en T10)**
    _Justificación:_ Instrucción explícita de ejecución: mantener commits pequeños y revisables, una tarea a la vez. El callback se añadirá con `.addCallback(new SeedDataInitializer())` en T10.

15. **`@Ignore` en los constructores con parámetros de ambas entidades (corrección detectada en T4)**
    _Justificación:_ La primera compilación con `@Database` activo emitió 4 warnings ("multiple good constructors"); el DoD exige cero warnings. `@Ignore` le dice a Room que use solo el constructor vacío. Corrección registrada aquí por ser un hallazgo de T4 sobre archivos de T2.

16. **Modelos de dominio inmutables (campos `final`, sin setters)**
    _Justificación:_ Un saldo ya calculado no puede ser mutado por referencia compartida; cada estado nuevo es una instancia nueva. Simplifica los tests de `TransaccionService` (T8) y elimina estados intermedios inválidos en memoria.

17. **Referencia a las entidades Room por nombre completo (FQN) en `toEntity()`/`fromEntity()`, sin `import`**
    _Justificación:_ El simple name de `domain.model.Cuenta` y `data.entities.Cuenta` es idéntico: Java prohíbe importar un tipo con el mismo nombre que la clase declarada en la unidad de compilación (JLS §7.5.1). Con FQN se cumple el requisito de `toEntity()`/`fromEntity()` sin importar anotaciones de Room — `domain/` no importa `androidx.*`, solo `java.math.BigDecimal`.

18. **Conversión exacta: `longValueExact()` en `BigDecimal → long` y `BigDecimal.valueOf(long)` en `long → BigDecimal`**
    _Justificación:_ Factor 1 (pesos CLP directos, sin centavos). `longValueExact()` lanza `ArithmeticException` si el valor tuviera fracción: una fracción en CLP es error de programación y debe fallar ruidosamente (`.agent.md` §5.4: nunca silenciar un error) en lugar de truncar en silencio.

19. **Enums en Java como representación de dominio cerrado, persistidos por `name()`**
    _Justificación:_ `TipoCuenta`/`TipoTransaccion` viven en `domain/` sin dependencias; `name()` produce exactamente el String que espera la entidad ("EFECTIVO", "INGRESO"). `valueOf()` en `fromEntity()` valida el dominio al leer: un valor desconocido en BD lanza `IllegalArgumentException` en lugar de propagarse.

20. **`ErrorCode` como enum (no constantes `String`)**
    _Justificación:_ Tipado fuerte y estático exigido por `.agent.md` §5.3: un `switch` sobre enum se valida en compilación al añadir un código nuevo; los `String` fallan en tiempo de ejecución. El `Javadoc` por constante documenta qué caso de negocio representa cada uno, y la traducción a texto queda delegada a `ui/` (§5.4: el dominio no produce mensajes).

21. **Alcance de `ErrorCode`: solo los 4 códigos de esta feature**
    _Justificación:_ `tasks.md` T6 define exactamente `CUENTA_VACIA`, `MONTO_INVALIDO`, `SALDO_INSUFICIENTE`, `CUENTA_NO_ENCONTRADA`. `CONTACTO_EN_USO` (ya canónico en `data-model.md` §4.2) se agrega con la Feature 009 de contactos/deudas, no aquí (prohibición de modificar fuera de scope, `.agent.md` §6.3).

22. **`BusinessRuleException extends RuntimeException` (unchecked), ubicada en `domain/error/`**
    _Justificación:_ `tasks.md` T7 exige "excepciones personalizadas con ErrorCode" y `plan.md` no la listaba en el inventario → se sincronizó el plan (SDD: corregir el plan, no actuar al margen). Es `unchecked` porque una regla de negocio violada es una violación de invariantes que el flujo normal no debe incumplir; forzar `throws` en cada llamada añadiría ruido sin valor (la UI decide dónde hacer `catch` por `ErrorCode`). El mensaje es técnico (para log) y el `ErrorCode` es el contrato con `ui/` (`.agent.md` §5.4: el dominio no produce texto de usuario).

23. **Métodos `static` sin estado en `TransaccionService` (constructor privado)**
    _Justificación:_ La lógica es pura (mismas entradas → mismas salidas, sin estado compartido): `static` la hace trivialmente testeable en T8 sin instanciar nada y coincide con cómo `plan.md` la invoca (`TransaccionService.validar...`). El constructor privado impide instanciar una utilidad sin estado.

24. **Mapeo de fallos a códigos según las definiciones de `plan.md` §1**
    _Justificación:_ Plan: `MONTO_INVALIDO` = "monto <= 0" → se usa también para monto/transacción nulos (no hay importe válido); `CUENTA_NO_ENCONTRADA` = "referencia a cuenta inexistente" → cuenta nula en `validarTransaccion`/`calcularNuevoSaldo`; `SALDO_INSUFICIENTE` = "egreso supera el saldo". Un tipo de movimiento fuera del dominio cerrado en `calcularNuevoSaldo` lanza `IllegalStateException` (no `BusinessRuleException`): es error de programación, no un caso de negocio que la UI deba mostrar.

25. **Comparaciones monetarias con `compareTo`, nunca `equals`, en `BigDecimal`**
    _Justificación:_ `BigDecimal.equals` compara también la escala (`1000` ≠ `1000.00`), lo que produciría falsos negativos en `SALDO_INSUFICIENTE`. `compareTo` compara por valor: `saldo.compareTo(monto) >= 0` significa exactamente "el saldo alcanza". Igual criterio para `monto > 0`.

26. **Tests: los 4 mínimos de `tasks.md` T8 + 4 complementarios (8 en total)**
    _Justificación:_ Los 4 pedidos dejan ramas sin cubrir (nulos, frontera `saldo == monto`, tipo fuera de dominio): los `throw` no ejecutados dejaban la cobertura de `TransaccionService` en el borde del 70%, y el criterio de salida exige ≥70% **garantizado**. Con `testCuentaNulaLanzaExcepcion`, `testTransaccionNulaLanzaExcepcion`, `testEgresoIgualAlSaldoPermitido` y `testTipoFueraDeDominioLanzaIllegalState` se ejecutan todos los caminos del servicio. Los tests complementarios están en la misma clase y se marcan con un comentario de sección (no cambian el alcance de la tarea: siguen siendo tests de T8).

27. **Aserciones monetarias por `compareTo` y montos con literales String en `BigDecimal`**
    _Justificación:_ `assertEquals(BigDecimal, BigDecimal)` usa `equals` (sensible a escala) y puede fallar en verde por una diferencia de formato, no de valor: `assertEquals(..., 0, esperado.compareTo(obtenido))` compara valor, coherente con la decisión #25 del servicio. `new BigDecimal("50000")` (String) en vez de `new BigDecimal(50000.0)`/`double`: evita el error de representación binaria prohibido por `.agent.md` §6.10.

28. **T9 corregido en caliente: Robolectric 4.17 en lugar de "JVM pura sin dependencias"**
    _Contexto:_ El sondeo empírico demostró que `Room.inMemoryDatabaseBuilder()` lanza `Method getCacheDir ... not mocked` en JVM pura: Room exige `Context`/SQLite reales. Opciones presentadas al usuario (SDD: detener y corregir el plan): instrumentados `androidTest/` (requiere emulador), Robolectric (dependencia nueva registrable) o posponer T9. _Elección del usuario:_ **Robolectric** — cumple literalmente el criterio de `tasks.md` T9 ("sin dispositivo", `testDebugUnitTest`). _Justificación de no requerir registro nuevo de candidatas:_ `tech-stack.md` §4 fila "Integración" ya aprobaba Robolectric 4.10+ para exactamente este caso ("CRUD en DAOs sin dispositivo"); se registró la adopción concreta en §7 antes de tocar el build.

29. **`--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` en los workers de test**
    _Justificación:_ El daemon Gradle corre en JDK 21 (`gradle-daemon-jvm.properties`), donde `java.base` no exporta `jdk.internal.access`; Robolectric lo necesita para interceptar `FileDescriptor` (`ApplicationSharedMemory.create` en el arranque del entorno SDK 36). Sin el flag, los 5 tests fallan con `IllegalAccessException ... module java.base does not export jdk.internal.access`. Es configuración de build, no una dependencia nueva.

30. **Helper propio `LiveDataTestUtil` en lugar de `androidx.arch.core:core-testing`**
    _Justificación:_ Las lecturas de los DAO devuelven `LiveData`; para consumirlas en JVM se usa el patrón estándar `observeForever` + `idle()` del looper principal de Robolectric + condición con timeout de 5s. `core-testing` (`InstantTaskExecutorRule`) no está registrado en `tech-stack.md`, y el helper (~70 líneas, solo test) resuelve lo mismo sin añadir dependencia. Si en el futuro se adopta `core-testing`, registrarla primero (regla dura §6).

31. **`RuntimeEnvironment.getApplication()` como `Context` de los tests de DAO**
    _Justificación:_ API oficial de Robolectric ya incluida en la dependencia elegida: evita añadir `androidx.test:core` solo por `ApplicationProvider`. La fila §4 de `tech-stack.md` preaprueba `androidx.test 1.5+` por si se necesita, pero aquí no hace falta.

32. **Seed por SQL directo (`execSQL` + bindArgs), no vía DAO, con constantes locales de tabla**
    _Justificación:_ `RoomDatabase.Callback.onCreate` se ejecuta **durante** la apertura de la base de datos, momento en que los DAOs generados aún no están disponibles; la única vía válida es SQL crudo. Se usan argumentos enlazados (`?` + `Object[]`) en lugar de concatenar valores (forma recomendada por Room). A diferencia de los `@Query` de los DAOs —que Room valida en compilación—, este SQL solo falla en runtime, así que el nombre de tabla vive en una constante privada `TABLA_CUENTAS` sincronizada con `@Entity(tableName = "cuentas")`, mitigando el riesgo de typo de la regla de `AGENTS.md` ("nombres de tabla por constante, nunca literales dispersos"). La semilla no duplica datos porque `onCreate` solo se invoca al crearse el archivo de BD, no en cada apertura.

33. **Íconos vectoriales locales en `res/drawable/` (no `@android:drawable/`)**
    _Justificación:_ Los drawables del framework (`@android:drawable/ic_menu_...`) fueron deprecados/eliminados en SDK 36 y solo incluyen un subconjunto mínimo de íconos (`ic_menu_add`, `ic_menu_compass`), insuficiente para los 4 íconos de Material Design (`account_balance_wallet`, `add_circle`, `map`, `person`) que exige `tasks.md` T11. La solución correcta es **crear los archivos vectoriales** en `res/drawable/ic_menu_*.xml` (Material Icons, exportados desde Android Studio → New → Vector Asset). Alternativas descartadas: usar `@android:drawable/` (no disponible en SDK 36 para los 4 íconos pedidos) o crear una dependencia de Material Components (solo por íconos, sobredimensionado).

34. **Strings con prefijo `aCuentas_` en `strings.xml` (organización por feature)**
    _Justificación:_ Convención establecida en el proyecto desde la Feature 005/006: cada feature tiene su prefijo único (`aLogin_`, `aRegister_`, `aCuentas_`) para evitar colisiones de nombres y facilitar la localización. Mantener esto desde el inicio simplifica auditorías y previene bugs de strings cruzados entre features.

---

## Lecciones Aprendidas

- La configuración de `room.schemaLocation` requiere el bloque `javaCompileOptions { annotationProcessorOptions { arguments = [...] } }` dentro de `defaultConfig` — no funciona en `kapt`, `buildFeatures` ni fuera del bloque `android{}`.
- El wrapper Gradle resolvió las dependencias de Room automáticamente sin conflictos de versiones con las dependencias existentes (biometric 1.2.0-alpha05, security-crypto 1.1.0-alpha06, material).
- Verificación inmediata con `./gradlew assembleDebug` → **BUILD SUCCESSFUL** en T1 (20s) y en T2 (3s) — cero errores de compilación.
- Warning de deprecación de Gradle 10 detectado pero **no relacionado con Room** — es preexistente del proyecto (Java 11 + compileSdk 36).
- **T2:** Room exige que la columna hija de toda `@ForeignKey` esté indexada (`LR_ROOM_FOREIGN_KEY_INDEX`); se declaró `@Index("cuentaId")` preventivamente en la entidad para evitar el warning y acelerar el `WHERE cuentaId = ?` del historial.
- **T2:** Las entidades llevan constructor vacío público + constructor completo con parámetros; Room usa el vacío y asigna por setters (compilado y verificado con `assembleDebug` exitoso).
- **T2:** El esquema aprobado de la feature es un subconjunto del esquema canónico de `data-model.md` (falta `saldo_inicial`, `categoria_id`, geolocalización, `fecha_hora TEXT`); la reconciliación documental queda pendiente para T19 — si no se hace, hay "bug de documentación" según la regla de sincronización del propio `data-model.md`.
- **T3:** Los literales de tabla dentro de `@Query` son seguros: Room los valida **en compilación** (si la tabla no existe, no compila). La advertencia de AGENTS.md sobre "literales dispersos" aplica a SQLiteOpenHelper puro, no a Room.
- **T3:** `LiveData<List<T>>` es la forma de que Room ejecute en background y notifique a la UI; los métodos síncronos (`insert`, `updateSaldo`, `delete`) deben invocarse desde el `Executor` del `AppDatabase` (T4) para no bloquear el hilo principal.
- **T4:** Los warnings de Room sobre constructores **solo aparecen cuando hay una clase `@Database`** que procesa las entidades; en T2 el build fue limpio porque el annotation processor no había activado la validación completa. Lección: verificar warnings en la tarea que activa el `@Database`, no antes.
- **T4:** `exportSchema = true` solo genera el JSON si `room.schemaLocation` está bien configurado; verificado: `app/schemas/com.localcash.data.db.AppDatabase/1.json` creado tras el build.
- **T4:** Corregido con `@Ignore` en constructores con parámetros: 4 warnings → 0 (DoD de cero warnings cumplido).
- **T5:** Dos clases con el mismo simple name en paquetes distintos (`domain.model.Cuenta` vs `data.entities.Cuenta`) no pueden resolverse con `import` en Java — hay que usar FQN en las firmas de conversión. Alternativa descartada: renombrar los modelos de dominio (rompería la terminología del dominio y los nombres pedidos en `tasks.md` T5).
- **T5:** Verificar la independencia de `domain/`: `grep -r "androidx" app/src/main/java/com/localcash/domain/` debe dar cero resultados (regla de arquitectura `.agent.md` §5.1).
- **T6:** Un enum sin dependencias es la forma más barata de código de error tipado: cero costo en runtime, validación en compilación y Javadoc por constante como documentación viva de los casos de negocio.
- **T7:** `BigDecimal.equals` es una trampa en validaciones monetarias (compara escala, no valor): siempre `compareTo`. Un test futuro en T8 debe cubrir explícitamente `monto = 0` y egreso exactamente igual al saldo (borde `>=`, que pasa).
- **T7:** Verificación de arquitectura con `grep "^import" domain/` tras el build: todos los imports son `java.*` o `com.localcash.domain.*` — regla §5.1 cumplida y comprobable en un comando.
- **T8:** El resultado verificable de los tests está en `app/build/test-results/testDebugUnitTest/TEST-<class>.xml` (`tests=... failures=... errors=...`): leer ese atributo es la prueba dura de "en verde", más fiable que confiar en el resumen de la consola. Aquí: `tests="8" skipped="0" failures="0" errors="0"`.
- **T8:** `assertThrows` existe desde JUnit 4.13 (el proyecto usa 4.13.2 vía `libs.junit` del catálogo de versiones): permite asertar el `ErrorCode` del fallo sin try/catch + `fail()`, escribiendo tests más legibles.
- **T8:** Los tests de JVM pura no requieren `connectedCheck` ni emulador: `testDebugUnitTest` compila y ejecuta todo en ~4s, ideal como puerta de calidad rápida en cada iteración.
- **T9 (hallazgo de proceso):** `tasks.md` T9 prometía "tests de DAO sin dispositivo en JVM pura" — **premisa falsa verificada empíricamente**: un sondeo temporal (borrado después) mostró `Method getCacheDir ... not mocked`. Método para futuras tareas: antes de ejecutar una casilla que depende de un supuesto técnico, sondearlo con un test mínimo; si el plan está mal, detenerse y corregirlo (SDD), no improvisar en silencio.
- **T9:** `tech-stack.md` §4 ya preaprobaba Robolectric ("Integración: CRUD en DAOs sin dispositivo") — **leer la Constitución antes de proponer dependencias nuevas** evitó trámite de candidatas; solo hizo falta registrar la adopción concreta en §7.
- **T9:** Mensajes engañosos de Robolectric: `"Failed to interact with raw FileDescriptor internals; perhaps JRE has changed?"` **no** es una incompatibilidad de versión de JDK — la cadena `Caused by` real era `module java.base does not export jdk.internal.access` (falta `--add-exports` en los workers de test). Lección: leer siempre el `Caused by` completo del XML de resultados antes de cambiar de versión de JDK.
- **T9:** Robolectric descarga `android-all-instrumented` (~SDK 36) en la primera ejecución desde Maven Central; ejecuciones siguientes son rápidas (~6s). Requiere red en el primer `testDebugUnitTest`.
- **T11:** Los `@android:drawable/` del framework fueron deprecados/eliminados en SDK 36; para Material Design icons se deben crear archivos vectoriales locales en `res/drawable/ic_menu_*.xml` (New → Vector Asset en Android Studio). Usar `@drawable/` local, no `@android:drawable/`.
- **T11:** La organización de strings con prefijo por feature (`aCuentas_`) es una convención del proyecto desde Fase 1 — facilita auditorías y previene colisiones entre features. Mantenerla desde el inicio es más barato que refactorizar después.

---

## Próximos Pasos

1. **Fase 3 UI — T11 completada.** Siguiente tarea: **T12** — crear layout `activity_cuentas.xml`.

2. **T12–T18 — Fase 3 UI:** crear layouts de vistas (T12, T13), `CuentasActivity.java` (T14), conectar Vista 1 con Room (T15), conectar Vista 2 con Room y dominio (T16), modificar `LoginActivity.java` (T17), actualizar `AndroidManifest.xml` y `strings.xml` (T18). Reglas de UI: cero colores hardcodeados (solo atributos Material 3), sin `ConstraintLayout` (solo `LinearLayout`/`RelativeLayout`), `MaterialButton` con `icon` y no `app:drawableStartCompat`.

3. **T19 (DoD, al cierre de la feature):** orden `lint` → `testDebugUnitTest` → `connectedCheck` → `assembleDebug`. En `connectedCheck` se verifica el criterio de salida de T10 (cuenta "Efectivo Demo" con $1.000.000 CLP al primer arranque real). Reconciliar el esquema de la feature con `data-model.md` (subconjunto de entidades, `fecha long` vs `fecha_hora TEXT`). Medir cobertura real (Jacoco) de `data/` y `domain/` para confirmar ≥70%.

---

## Historial de Tareas de esta Feature

| Tarea | Descripción | Estado | Fecha |
|-------|-------------|--------|-------|
| T1 | Configurar Room en el proyecto | ✅ Completado | 2026-10-07 |
| T2 | Crear entidades Room en `data/entities/` | ✅ Completado | 2026-10-07 |
| T3 | Crear DAOs en `data/dao/` | ✅ Completado | 2026-10-07 |
| T4 | Crear `AppDatabase.java` en `data/db/` | ✅ Completado | 2026-10-07 |
| T5 | Crear modelos de dominio en `domain/model/` | ✅ Completado | 2026-10-07 |
| T6 | Crear códigos de error en `domain/error/ErrorCode.java` | ✅ Completado | 2026-10-07 |
| T7 | Crear `TransaccionService.java` en `domain/service/` | ✅ Completado | 2026-10-07 |
| T8 | Tests unitarios para `TransaccionService` | ✅ Completado | 2026-10-07 |
| T9 | Tests unitarios para DAOs | ✅ Completado | 2026-10-07 |
| T10 | Crear `SeedDataInitializer.java` en `data/db/` | ✅ Completado | 2026-10-14 |
| T11 | Crear menú `bottom_nav_menu.xml` + strings `aCuentas_` | ✅ Completado | 2026-10-14 |
| T12 | Crear layout `activity_cuentas.xml` | ✅ Completado | 2026-10-14 |
| T13 | Crear layouts de vistas | ⬜ Pendiente | — |
| T14 | Crear `CuentasActivity.java` en `ui/cuentas/` | ⬜ Pendiente | — |
| T15 | Conectar Vista 1 (Cuentas) con Room | ⬜ Pendiente | — |
| T16 | Conectar Vista 2 (Transacción) con Room y dominio | ⬜ Pendiente | — |
| T17 | Modificar `LoginActivity.java` | ⬜ Pendiente | — |
| T18 | Actualizar `AndroidManifest.xml` y `strings.xml` | ⬜ Pendiente | — |
| T19 | Verificación final y documentación | ⬜ Pendiente | — |

---

## Referencias

- **spec/features/007-cuentas/spec.md** — Especificación (QUÉ)
- **spec/features/007-cuentas/plan.md** — Plan técnico (CÓMO) — Decisiones #1, #2, #6, #10
- **spec/features/007-cuentas/tasks.md** — Checklist T1-T19
- **.agent.md §6.10** — Regla de dinero: INTEGER (long) en pesos CLP directos (no centavos)
- **data-model.md §3.2, §3.4** — Diccionario entidades CUENTAS y TRANSACCIONES
