# doc/007-cuentas.md — Registro de Memoria: Feature 007 - Cuentas y Transacciones Básicas

> **Propósito de este archivo:** Registro de memoria de la Feature 007 según `.agent.md` §7.
> Aquí se registran las tareas ejecutadas (T1 → T19), decisiones, lecciones y próximos pasos.

---

## Fichas de tarea

| Campo | T1 | T2 | T3 | T4 | T5 |
| --- | --- | --- | --- | --- | --- |
| **Fecha** | 2026-10-07 | 2026-10-07 | 2026-10-07 | 2026-10-07 | 2026-10-07 |
| **Tarea ejecutada** | Configurar Room en el proyecto | Crear entidades Room en `data/entities/` | Crear DAOs en `data/dao/` | Crear `AppDatabase.java` en `data/db/` | Crear modelos de dominio en `domain/model/` |
| **Responsable** | GG | GG | GG | GG | GG |
| **Duración estimada vs real** | 15 min vs ~5 min | 30 min vs ~10 min | 30 min vs ~10 min | 30 min vs ~15 min | 30 min vs ~15 min |
| **Estado** | Completado | Completado | Completado | Completado | Completado |

---

## Tarea Ejecutada (resumen narrativo)

**T1 — Configurar Room:** Se configuró **Room (SQLite)** como capa de persistencia para datos financieros en la Feature 007, introduciendo la base de datos relacional local por primera vez en el proyecto. Hasta este momento, la app solo usaba `EncryptedSharedPreferences` para credenciales de usuario (Feature 005/006). Se agregaron las dependencias oficiales de Room 2.6.1 y se configuró la exportación automática del schema SQLite en `app/schemas/` para documentación técnica y validación académica.

**T2 — Crear entidades Room:** Se crearon las dos entidades que definen el núcleo financiero del esquema: `Cuenta` (patrimonio del usuario) y `Transaccion` (movimiento ingreso/egreso). Ambas almacenan dinero como `long` en **pesos CLP directos** (sin centavos, `.agent.md` §6.10, `data-model.md` §3.2/§3.4). `Transaccion` declara clave foránea a `Cuenta` con `ON DELETE CASCADE` e índice sobre la columna hija `cuentaId`.

**T3 — Crear DAOs:** Se crearon las interfaces de acceso a datos `CuentaDao` y `TransaccionDao` con las operaciones de inserción, consulta observable (`LiveData`), actualización de saldo y borrado. Los DAOs solo persisten: no deciden reglas de negocio (la validación de saldo vive en `domain/service`, pendiente T7). Room valida los literales SQL de los `@Query` en compilación.

**T4 — Crear AppDatabase:** Se creó la clase abstracta `AppDatabase` (`RoomDatabase`) con `@Database(entities = {Cuenta, Transaccion}, version = 1, exportSchema = true)`, los métodos abstractos `cuentaDao()`/`transaccionDao()`, un **singleton thread-safe** (doble verificación con `volatile`) y un `ExecutorService` de un solo hilo para las operaciones síncronas de los DAOs. El registro de `SeedDataInitializer` queda pendiente a propósito (`// TODO: Registrar SeedDataInitializer en T10`). Bonus: la primera compilación con `@Database` reveló 4 warnings de constructores duplicados en las entidades; se corrigieron con `@Ignore` y se exportó el schema en `app/schemas/`.

**T5 — Crear modelos de dominio:** Se crearon los modelos de negocio inmutables `Cuenta` y `Transaccion` (con `BigDecimal` para dinero) más los enums `TipoCuenta` (EFECTIVO/AHORRO/TARJETA) y `TipoTransaccion` (INGRESO/EGRESO). Cada modelo incluye el único par de conversión `long` ↔ `BigDecimal` vía `toEntity()` / `fromEntity()` (factor 1: pesos CLP directos). La capa `domain/` no importa clases de Room ni de Android: solo `java.math.BigDecimal`.

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

**Modificados:**

- `app/build.gradle` (T1) — modificaciones:
  - Agregado `implementation "androidx.room:room-runtime:2.6.1"` (runtime de Room)
  - Agregado `annotationProcessor "androidx.room:room-compiler:2.6.1"` (procesador de anotaciones)
  - Agregado `testImplementation "androidx.room:room-testing:2.6.1"` (testing de Room)
  - Configurado `room.schemaLocation` en `defaultConfig.javaCompileOptions.annotationProcessorOptions.arguments` → `"$projectDir/schemas"`
- `app/src/main/java/com/localcash/data/entities/Cuenta.java` (corregido en T4) — constructor con parámetros anotado con `@Ignore` (elimina warning "multiple good constructors").
- `app/src/main/java/com/localcash/data/entities/Transaccion.java` (corregido en T4) — constructor con parámetros anotado con `@Ignore` (misma corrección).

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

---

## Próximos Pasos

1. **T6 — Crear `domain/error/ErrorCode.java`:** enum o constantes CUENTA_VACIA, MONTO_INVALIDO, SALDO_INSUFICIENTE, CUENTA_NO_ENCONTRADA (el código `CONTACTO_EN_USO` ya está definido en `data-model.md` §4.2 para features posteriores).

2. **T7 — Crear `domain/service/TransaccionService.java`:**
   - `validarTransaccion(Transaccion, Cuenta)`: monto > 0, cuenta no nula, y si EGRESO entonces `cuenta.saldo >= transaccion.monto`
   - `calcularNuevoSaldo(Cuenta, Transaccion)`: BigDecimal con saldo actualizado
   - Excepciones personalizadas con `ErrorCode` (cero dependencias Android)

3. **T8 — Tests unitarios de `TransaccionService`** (ingreso suma, egreso resta, saldo insuficiente lanza excepción, monto inválido lanza excepción)

4. Ejecutar `./gradlew assembleDebug` tras cada fase para validar compilación.

5. **Pendiente documental (T19):** reconciliar el esquema de la feature con `data-model.md` (subconjunto de entidades, `fecha long` vs `fecha_hora TEXT`) — regla de sincronización del propio documento.

---

## Historial de Tareas de esta Feature

| Tarea | Descripción | Estado | Fecha |
|-------|-------------|--------|-------|
| T1 | Configurar Room en el proyecto | ✅ Completado | 2026-10-07 |
| T2 | Crear entidades Room en `data/entities/` | ✅ Completado | 2026-10-07 |
| T3 | Crear DAOs en `data/dao/` | ✅ Completado | 2026-10-07 |
| T4 | Crear `AppDatabase.java` en `data/db/` | ✅ Completado | 2026-10-07 |
| T5 | Crear modelos de dominio en `domain/model/` | ✅ Completado | 2026-10-07 |
| T4 | Crear `AppDatabase.java` en `data/db/` | ⬜ Pendiente | — |
| T5 | Crear modelos de dominio en `domain/model/` | ⬜ Pendiente | — |
| T6 | Crear códigos de error en `domain/error/ErrorCode.java` | ⬜ Pendiente | — |
| T7 | Crear `TransaccionService.java` en `domain/service/` | ⬜ Pendiente | — |
| T8 | Tests unitarios para `TransaccionService` | ⬜ Pendiente | — |
| T9 | Tests unitarios para DAOs | ⬜ Pendiente | — |
| T10 | Crear `SeedDataInitializer.java` en `data/db/` | ⬜ Pendiente | — |
| T11 | Crear menú `bottom_nav_menu.xml` | ⬜ Pendiente | — |
| T12 | Crear layout `activity_cuentas.xml` | ⬜ Pendiente | — |
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
