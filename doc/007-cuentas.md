# doc/007-cuentas.md — Registro de Memoria: Feature 007 - Cuentas y Transacciones Básicas

> **Propósito de este archivo:** Registro de memoria de la Feature 007 según `.agent.md` §7.
> Aquí se registran las tareas ejecutadas (T1 → T19), decisiones, lecciones y próximos pasos.

---

## Fichas de tarea

| Campo | T1 | T2 |
| --- | --- | --- |
| **Fecha** | 2026-10-07 | 2026-10-07 |
| **Tarea ejecutada** | Configurar Room en el proyecto | Crear entidades Room en `data/entities/` |
| **Responsable** | GG | GG |
| **Duración estimada vs real** | 15 min vs ~5 min | 30 min vs ~10 min |
| **Estado** | Completado | Completado |

---

## Tarea Ejecutada (resumen narrativo)

**T1 — Configurar Room:** Se configuró **Room (SQLite)** como capa de persistencia para datos financieros en la Feature 007, introduciendo la base de datos relacional local por primera vez en el proyecto. Hasta este momento, la app solo usaba `EncryptedSharedPreferences` para credenciales de usuario (Feature 005/006). Se agregaron las dependencias oficiales de Room 2.6.1 y se configuró la exportación automática del schema SQLite en `app/schemas/` para documentación técnica y validación académica.

**T2 — Crear entidades Room:** Se crearon las dos entidades que definen el núcleo financiero del esquema: `Cuenta` (patrimonio del usuario) y `Transaccion` (movimiento ingreso/egreso). Ambas almacenan dinero como `long` en **pesos CLP directos** (sin centavos, `.agent.md` §6.10, `data-model.md` §3.2/§3.4). `Transaccion` declara clave foránea a `Cuenta` con `ON DELETE CASCADE` e índice sobre la columna hija `cuentaId`.

---

## Archivos Creados / Modificados

**Creados:**

- `app/src/main/java/com/localcash/data/entities/Cuenta.java` — `@Entity(tableName = "cuentas")`: `id` (long, `autoGenerate`), `nombre` (String), `tipo` (String: EFECTIVO/AHORRO/TARJETA), `saldo` (long, pesos CLP), `moneda` (String, `@ColumnInfo(defaultValue = "CLP")`). Incluye constructor vacío (requerido por Room) y constructor completo con saldo de apertura.
- `app/src/main/java/com/localcash/data/entities/Transaccion.java` — `@Entity(tableName = "transacciones")` con `@ForeignKey(entity = Cuenta.class, parentColumns = "id", childColumns = "cuentaId", onDelete = ForeignKey.CASCADE)` e `@Index("cuentaId")`: `id` (long, `autoGenerate`), `cuentaId` (long), `tipo` (String: INGRESO/EGRESO), `monto` (long, pesos CLP, siempre positivo), `concepto` (String), `fecha` (long, timestamp epoch millis). Incluye constructor vacío y constructor completo.

**Modificados:**

- `app/build.gradle` (T1) — modificaciones:
  - Agregado `implementation "androidx.room:room-runtime:2.6.1"` (runtime de Room)
  - Agregado `annotationProcessor "androidx.room:room-compiler:2.6.1"` (procesador de anotaciones)
  - Agregado `testImplementation "androidx.room:room-testing:2.6.1"` (testing de Room)
  - Configurado `room.schemaLocation` en `defaultConfig.javaCompileOptions.annotationProcessorOptions.arguments` → `"$projectDir/schemas"`

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

---

## Lecciones Aprendidas

- La configuración de `room.schemaLocation` requiere el bloque `javaCompileOptions { annotationProcessorOptions { arguments = [...] } }` dentro de `defaultConfig` — no funciona en `kapt`, `buildFeatures` ni fuera del bloque `android{}`.
- El wrapper Gradle resolvió las dependencias de Room automáticamente sin conflictos de versiones con las dependencias existentes (biometric 1.2.0-alpha05, security-crypto 1.1.0-alpha06, material).
- Verificación inmediata con `./gradlew assembleDebug` → **BUILD SUCCESSFUL** en T1 (20s) y en T2 (3s) — cero errores de compilación.
- Warning de deprecación de Gradle 10 detectado pero **no relacionado con Room** — es preexistente del proyecto (Java 11 + compileSdk 36).
- **T2:** Room exige que la columna hija de toda `@ForeignKey` esté indexada (`LR_ROOM_FOREIGN_KEY_INDEX`); se declaró `@Index("cuentaId")` preventivamente en la entidad para evitar el warning y acelerar el `WHERE cuentaId = ?` del historial.
- **T2:** Las entidades llevan constructor vacío público + constructor completo con parámetros; Room usa el vacío y asigna por setters (compilado y verificado con `assembleDebug` exitoso).
- **T2:** El esquema aprobado de la feature es un subconjunto del esquema canónico de `data-model.md` (falta `saldo_inicial`, `categoria_id`, geolocalización, `fecha_hora TEXT`); la reconciliación documental queda pendiente para T19 — si no se hace, hay "bug de documentación" según la regla de sincronización del propio `data-model.md`.

---

## Próximos Pasos

1. **T3 — Crear DAOs en `data/dao/`:**
   - `CuentaDao.java`: @Insert, @Query("SELECT * FROM cuentas") LiveData<List<Cuenta>> getAll(), @Query("UPDATE cuentas SET saldo = :saldo WHERE id = :cuentaId") updateSaldo, @Delete
   - `TransaccionDao.java`: @Insert, @Query getByCuentaId (LiveData, ORDER BY fecha DESC), @Query getAll (LiveData)
   - Recordar: nombres de tabla por constante, no literales dispersos (AGENTS.md)

2. **T4 — Crear `AppDatabase.java` en `data/db/`:**
   - Abstract class RoomDatabase, @Database(entities = {Cuenta.class, Transaccion.class}, version = 1, exportSchema = true), singleton con Executors.newSingleThreadExecutor(), callback SeedDataInitializer (T10)

3. Ejecutar `./gradlew assembleDebug` tras cada fase para validar compilación.

4. **Pendiente documental (T19):** reconciliar el esquema de la feature con `data-model.md` (subconjunto de entidades, `fecha long` vs `fecha_hora TEXT`) — regla de sincronización del propio documento.

---

## Historial de Tareas de esta Feature

| Tarea | Descripción | Estado | Fecha |
|-------|-------------|--------|-------|
| T1 | Configurar Room en el proyecto | ✅ Completado | 2026-10-07 |
| T2 | Crear entidades Room en `data/entities/` | ✅ Completado | 2026-10-07 |
| T3 | Crear DAOs en `data/dao/` | ⬜ Pendiente | — |
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
