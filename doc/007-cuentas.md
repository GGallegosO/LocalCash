# doc/007-cuentas.md — Registro de Memoria: Feature 007 - Cuentas y Transacciones Básicas

> **Propósito de este archivo:** Registro de memoria de la Feature 007 según `.agent.md` §7.
> Aquí se registran las tareas ejecutadas (T1 → T19), decisiones, lecciones y próximos pasos.

---

## Ficha de la tarea

| Campo                         | Valor                               |
| ----------------------------- |-------------------------------------|
| **Fecha**                     | 2026-10-07                          |
| **Tarea ejecutada**           | T1 - Configurar Room en el proyecto |
| **Responsable**               | GG                                  |
| **Duración estimada vs real** | 15 min vs ~5 min                    |
| **Estado**                    | Completado                          |

---

## Tarea Ejecutada (resumen narrativo)

Se configuró **Room (SQLite)** como capa de persistencia para datos financieros en la Feature 007, introduciendo la base de datos relacional local por primera vez en el proyecto. Hasta este momento, la app solo usaba `EncryptedSharedPreferences` para credenciales de usuario (Feature 005/006).

Se agregaron las dependencias oficiales de Room 2.6.1 y se configuró la exportación automática del schema SQLite en `app/schemas/` para documentación técnica y validación académica.

---

## Archivos Creados / Modificados

**Creados:**

- (ninguno — los archivos de datos/entidades/DAOs se crearán en T2-T4)

**Modificados:**

- `app/build.gradle` — modificaciones:
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

---

## Lecciones Aprendidas

- La configuración de `room.schemaLocation` requiere el bloque `javaCompileOptions { annotationProcessorOptions { arguments = [...] } }` dentro de `defaultConfig` — no funciona en `kapt`, `buildFeatures` ni fuera del bloque `android{}`.
- El wrapper Gradle resolvió las dependencias de Room automáticamente sin conflictos de versiones con las dependencias existentes (biometric 1.2.0-alpha05, security-crypto 1.1.0-alpha06, material).
- Verificación inmediata con `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (20s, 36 tasks) — cero errores de compilación.
- Warning de deprecación de Gradle 10 detectado pero **no relacionado con Room** — es preexistente del proyecto (Java 11 + compileSdk 36).

---

## Próximos Pasos

1. **T2 — Crear entidades Room en `data/entities/`:**
   - `Cuenta.java`: `@Entity(tableName="cuentas")` con id (long autoGenerate), nombre (String), tipo (String: EFECTIVO/AHORRO/TARJETA), saldo (long pesos CLP), moneda (String DEFAULT "CLP")
   - `Transaccion.java`: `@Entity(tableName="transacciones", foreignKeys=@ForeignKey(entity=Cuenta.class, parentColumns="id", childColumns="cuentaId", onDelete=CASCADE))` con id, cuentaId, tipo (INGRESO/EGRESO), monto (long pesos CLP), concepto (String), fecha (long timestamp)

2. **T3 — Crear DAOs en `data/dao/`:**
   - `CuentaDao.java`: @Insert, @Query("SELECT * FROM cuentas") LiveData<List<Cuenta>> getAll(), @Query updateSaldo, @Delete
   - `TransaccionDao.java`: @Insert, @Query getByCuentaId LiveData, @Query getAll LiveData

3. **T4 — Crear `AppDatabase.java` en `data/db/`:**
   - Abstract class RoomDatabase, @Database(entities, version=1, exportSchema=true), singleton con Executors.newSingleThreadExecutor(), callback SeedDataInitializer (T10)

4. Ejecutar `./gradlew assembleDebug` tras cada fase para validar compilación.

5. Al finalizar todas las tareas: ejecutar `./gradlew lint` → cero warnings, `./gradlew testDebugUnitTest` → verde con cobertura ≥70%.

---

## Historial de Tareas de esta Feature

| Tarea | Descripción | Estado | Fecha |
|-------|-------------|--------|-------|
| T1 | Configurar Room en el proyecto | ✅ Completado | 2026-10-07 |
| T2 | Crear entidades Room en `data/entities/` | ⬜ Pendiente | — |
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
