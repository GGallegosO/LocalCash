# doc/007-cuentas.md — Registro de Memoria: Feature 007 - Cuentas y Transacciones Básicas

> **Propósito de este archivo:** Registro de memoria de la Feature 007 según `.agent.md` §7.
> Aquí se registran las tareas ejecutadas (T1 → T19), decisiones, lecciones y próximos pasos.

---

## Estado de la sesión (retomada tras /compact)

- **Alcance actual:** Capa Datos/Dominio (T1→T12) completada + Fase 3 UI en progreso (T12 finalizada). Próxima tarea: **T13**.
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

**T12 — Crear layout `activity_cuentas.xml`:** Se creó `app/src/main/res/layout/activity_cuentas.xml` con root `ConstraintLayout`.

**Correcciones T12 aplicadas:**
- **ERROR 1 (posición toggle):** Eliminado `app:navigationIcon` y `app:navigationContentDescription` del Toolbar (iba a la izquierda). Creado `ImageButton` independiente con id `btnThemeToggle` en esquina superior derecha usando `app:layout_constraintEnd_toEndOf="parent"`. Ícono `@drawable/ic_brightness_medium` con `app:srcCompat`.
- **ERROR 2 (textos hardcodeados):** Reemplazado `app:title="Cuentas"` por `app:title="@string/aCuentas_toolbar_title"`. Eliminado `android:text="Cerrar sesión"` y `app:navigationContentDescription` (ya no aplican).
- **ERROR 3 (btnLogout no planificado):** Eliminado completamente el botón `btnLogout` y su listener de `CuentasActivity.java`. El layout y la actividad ahora solo incluyen `btnThemeToggle` para cambio de tema.
- **Strings agregados:** `aCuentas_toolbar_title` ("Cuentas") y `aCuentas_toggle_tema_desc` ("Cambiar tema claro/oscuro") en `strings.xml`.