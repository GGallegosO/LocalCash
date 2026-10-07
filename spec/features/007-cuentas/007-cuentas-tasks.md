# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** 007-cuentas | **Derivado de:** `spec.md` + `plan.md`
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

## Fase 1 — Configuración de Room y Contratos

- [ ] **T1. Agregar dependencias de Room en `app/build.gradle`**
  - Agregar `androidx.room:room-runtime:2.6.1`
  - Agregar `androidx.room:room-compiler:2.6.1` (annotationProcessor)
  - Agregar `androidx.room:room-testing:2.6.1` (testImplementation)
  - Configurar `room.schemaLocation` en `defaultConfig` para exportar schema SQLite
  - *Criterio de salida:* `./gradlew assembleDebug` compila sin errores
  - Responsable: GG | Estimación: 15 min

- [ ] **T2. Crear entidades Room en `data/entities/`**
  - Crear `Cuenta.java`: `@Entity(tableName = "cuentas")` con campos: `id` (long, autoGenerate), `nombre` (String), `tipo` (String: EFECTIVO/AHORRO/TARJETA), `saldo` (long, en pesos CLP)
  - Crear `Transaccion.java`: `@Entity(tableName = "transacciones", foreignKeys = @ForeignKey(entity = Cuenta.class, parentColumns = "id", childColumns = "cuentaId", onDelete = CASCADE))` con campos: `id` (long, autoGenerate), `cuentaId` (long), `tipo` (String: INGRESO/EGRESO), `monto` (long, en pesos CLP), `concepto` (String), `fecha` (long, timestamp)
  - *Criterio de salida:* Entidades compiladas, sin warnings de lint
  - Responsable: GG | Estimación: 30 min

- [ ] **T3. Crear DAOs en `data/dao/`**
  - Crear `CuentaDao.java` con métodos: `@Insert void insert(Cuenta cuenta)`, `@Query("SELECT * FROM cuentas") LiveData<List<Cuenta>> getAll()`, `@Query("UPDATE cuentas SET saldo = :saldo WHERE id = :cuentaId") void updateSaldo(long cuentaId, long saldo)`, `@Delete void delete(Cuenta cuenta)`
  - Crear `TransaccionDao.java` con métodos: `@Insert void insert(Transaccion transaccion)`, `@Query("SELECT * FROM transacciones WHERE cuentaId = :cuentaId ORDER BY fecha DESC") LiveData<List<Transaccion>> getByCuentaId(long cuentaId)`, `@Query("SELECT * FROM transacciones ORDER BY fecha DESC") LiveData<List<Transaccion>> getAll()`
  - *Criterio de salida:* DAOs compilados, sin warnings de lint
  - Responsable: GG | Estimación: 30 min

- [ ] **T4. Crear `AppDatabase.java` en `data/db/`**
  - Clase abstracta que extienda `RoomDatabase`
  - Anotación `@Database(entities = {Cuenta.class, Transaccion.class}, version = 1, exportSchema = true)`
  - Métodos abstractos: `CuentaDao cuentaDao()`, `TransaccionDao transaccionDao()`
  - Implementar patrón Singleton con `Executors.newSingleThreadExecutor()` para operaciones asíncronas
  - *Criterio de salida:* Base de datos inicializable, sin errores de compilación
  - Responsable: GG | Estimación: 30 min

---

## Fase 2 — Lógica de Dominio y Tests

- [ ] **T5. Crear modelos de dominio en `domain/model/`**
  - Crear `Cuenta.java` (modelo de dominio, no entidad Room) con campos: `id`, `nombre`, `tipo` (Enum), `saldo` (BigDecimal)
  - Crear `Transaccion.java` (modelo de dominio) con campos: `id`, `cuentaId`, `tipo` (Enum: INGRESO/EGRESO), `monto` (BigDecimal), `concepto`, `fecha`
  - Incluir métodos de conversión desde/hacia entidades Room (mapeo `long` ↔ `BigDecimal`)
  - *Criterio de salida:* Modelos compilados, sin dependencias de Android
  - Responsable: GG | Estimación: 30 min

- [ ] **T6. Crear códigos de error en `domain/error/ErrorCode.java`**
  - Definir enum o constantes: `CUENTA_VACIA`, `MONTO_INVALIDO`, `SALDO_INSUFICIENTE`, `CUENTA_NO_ENCONTRADA`
  - *Criterio de salida:* Códigos de error disponibles para uso en `TransaccionService`
  - Responsable: GG | Estimación: 10 min

- [ ] **T7. Crear `TransaccionService.java` en `domain/service/`**
  - Método `validarTransaccion(Transaccion transaccion, Cuenta cuenta)`: valida que monto > 0, cuenta no nula, y si es EGRESO que `cuenta.saldo >= transaccion.monto`
  - Método `calcularNuevoSaldo(Cuenta cuenta, Transaccion transaccion)`: retorna `BigDecimal` con saldo actualizado (suma para INGRESO, resta para EGRESO)
  - Lanza excepciones personalizadas con `ErrorCode` cuando falla la validación
  - *Criterio de salida:* Lógica de negocio testeable sin dispositivo, cero dependencias de Android
  - Responsable: GG | Estimación: 45 min

- [ ] **T8. Crear tests unitarios para `TransaccionService`**
  - Crear `TransaccionServiceTest.java` en `app/src/test/java/com/localcash/domain/service/`
  - Test: `testIngresoAumentaSaldo()` → verifica que saldo aumente correctamente
  - Test: `testEgresoDisminuyeSaldo()` → verifica que saldo disminuya correctamente
  - Test: `testEgresoConSaldoInsuficienteLanzaExcepcion()` → verifica que bloquee egreso > saldo
  - Test: `testMontoInvalidoLanzaExcepcion()` → verifica que bloquee monto <= 0
  - *Criterio de salida:* Tests en verde, cobertura ≥70% en `TransaccionService`
  - Responsable: GG | Estimación: 45 min

- [ ] **T9. Crear tests unitarios para DAOs**
  - Crear `CuentaDaoTest.java` y `TransaccionDaoTest.java` en `app/src/test/java/com/localcash/data/dao/`
  - Usar `Room.inMemoryDatabaseBuilder()` para tests sin dispositivo
  - Test `CuentaDaoTest`: insertar cuenta, verificar que `getAll()` la retorne, actualizar saldo, verificar actualización
  - Test `TransaccionDaoTest`: insertar transacción, verificar que `getByCuentaId()` la retorne
  - *Criterio de salida:* Tests en verde, cobertura ≥70% en DAOs
  - Responsable: GG | Estimación: 60 min

---

## Fase 3 — Implementación de UI

- [ ] **T10. Crear `SeedDataInitializer.java` en `data/db/`**
  - Extender `RoomDatabase.Callback`
  - Override `onCreate()`: insertar cuenta demo "Efectivo Demo" con tipo EFECTIVO y saldo 1_000_000L (=$1.000.000 CLP)
  - Registrar en `AppDatabase` con `.addCallback(new SeedDataInitializer())`
  - *Criterio de salida:* Al iniciar la app por primera vez, la cuenta demo existe en la BD
  - Responsable: GG | Estimación: 30 min

- [ ] **T11. Crear menú de navegación inferior `bottom_nav_menu.xml`**
  - Crear en `app/src/main/res/menu/bottom_nav_menu.xml`
  - 4 ítems: `nav_cuentas` (ícono: account_balance_wallet), `nav_nuevo` (ícono: add_circle), `nav_mapa` (ícono: map), `nav_contactos` (ícono: person)
  - Strings en `strings.xml` con prefijo `aCuentas_`
  - *Criterio de salida:* Menú compilado, sin errores de recursos
  - Responsable: GG | Estimación: 20 min

- [ ] **T12. Crear layout `activity_cuentas.xml`**
  - Root: `ConstraintLayout`
  - AppBar superior con título dinámico y botón de toggle de tema (reutilizar estilo de Login/Register)
  - `ViewFlipper` en el centro (ocupa espacio restante)
  - `BottomNavigationView` en la parte inferior
  - Usar atributos Material 3 (`?attr/colorPrimary`, `?attr/colorSurface`, etc.), CERO colores hardcodeados
  - *Criterio de salida:* Layout compilado, sin warnings de lint
  - Responsable: GG | Estimación: 45 min

- [ ] **T13. Crear layouts de vistas `view_cuentas.xml`, `view_transaccion.xml`, `view_mapa.xml`, `view_contactos.xml`**
  - `view_cuentas.xml`: Card de saldo total consolidado, `RecyclerView` para lista de cuentas, FAB "+" para agregar cuenta
  - `view_transaccion.xml`: Toggle segmentado Ingreso/Egreso (`MaterialButtonToggleGroup`), Input monto, Spinner/Dropdown de cuenta, Input concepto, Switch de geolocalización (UI only), Botón "Guardar Movimiento"
  - `view_mapa.xml` y `view_contactos.xml`: Placeholder con TextView centrado "Próximamente"
  - Todos los layouts deben usar atributos Material 3, CERO colores hardcodeados
  - *Criterio de salida:* 4 layouts compilados, sin warnings de lint
  - Responsable: GG | Estimación: 90 min

- [ ] **T14. Crear `CuentasActivity.java` en `ui/cuentas/`**
  - Extender `BaseActivity` (hereda tema)
  - Usar ViewBinding (`ActivityCuentasBinding`)
  - Conectar `BottomNavigationView` con `ViewFlipper` (cambiar vista al seleccionar ítem)
  - Implementar toggle de tema (reutilizar lógica de Login/Register)
  - Manejar estado del ViewFlipper en `onSaveInstanceState()` para preservar vista activa en rotación
  - *Criterio de salida:* Activity compila, navegación entre vistas funciona, toggle de tema funciona
  - Responsable: GG | Estimación: 60 min

---

## Fase 4 — Integración y Cierre

- [ ] **T15. Conectar Vista 1 (Cuentas) con Room**
  - En `CuentasActivity`, observar `LiveData<List<Cuenta>>` desde `CuentaDao.getAll()`
  - Crear adapter para `RecyclerView` que muestre nombre, tipo y saldo de cada cuenta
  - Calcular saldo total consolidado (suma de todos los saldos) y mostrarlo en la Card superior
  - Implementar FAB para agregar cuenta nueva (diálogo o navegación a formulario simple)
  - *Criterio de salida:* Al iniciar sesión con "admin", se ve la cuenta "Efectivo Demo" con saldo $1.000.000 CLP
  - Responsable: GG | Estimación: 60 min

- [ ] **T16. Conectar Vista 2 (Transacción) con Room y dominio**
  - Al presionar "Guardar Movimiento":
    1. Validar campos (monto > 0, cuenta seleccionada, concepto no vacío)
    2. Crear modelo de dominio `Transaccion` y `Cuenta`
    3. Llamar a `TransaccionService.validarTransaccion()` (lanza excepción si saldo insuficiente)
    4. Si válido: insertar en `TransaccionDao`, calcular nuevo saldo con `TransaccionService.calcularNuevoSaldo()`, actualizar en `CuentaDao`
    5. Mostrar Toast de éxito o error según resultado
    6. Limpiar formulario
  - Manejar errores mostrando mensajes accionables al usuario
  - *Criterio de salida:* Registrar ingreso/egreso actualiza saldo correctamente; egreso > saldo muestra error "Saldo insuficiente"
  - Responsable: GG | Estimación: 90 min

- [ ] **T17. Modificar `LoginActivity.java` para redirigir a `CuentasActivity`**
  - Tras login exitoso (biometría o credenciales), navegar a `CuentasActivity` en lugar de `CuentasActivity` placeholder actual
  - *Criterio de salida:* Login con "admin/123456" lleva a `CuentasActivity` funcional
  - Responsable: GG | Estimación: 15 min

- [ ] **T18. Actualizar `AndroidManifest.xml` y `strings.xml`**
  - Registrar `CuentasActivity` en `AndroidManifest.xml`
  - Agregar todos los strings necesarios con prefijo `aCuentas_` en `strings.xml`
  - *Criterio de salida:* Manifest y strings compilados, sin duplicados
  - Responsable: GG | Estimación: 20 min

- [ ] **T19. Verificación final y documentación**
  - Ejecutar `./gradlew lint` → cero warnings en código nuevo
  - Ejecutar `./gradlew testDebugUnitTest` → todos los tests en verde, cobertura ≥70%
  - Ejecutar `./gradlew assembleDebug` → build exitoso
  - Pruebas manuales (10 casos del `plan.md` §5)
  - Verificar que `app/schemas/` contenga el schema SQLite exportado
  - Crear `doc/007-cuentas.md` con registro de memoria (decisiones, lecciones, próximos pasos)
  - *Criterio de salida:* Definition of Done completo, feature lista para merge
  - Responsable: GG | Estimación: 60 min

---

## Verificación final (Definition of Done)

Los cuatro comandos van en este orden. `connectedCheck` va al final y **requiere dispositivo o
emulador activo**: si se ejecuta primero y falla, se pierde el ciclo de compilación.

- [ ] Lint sin advertencias: `./gradlew lint`
- [ ] Tests unitarios en verde: `./gradlew testDebugUnitTest`
- [ ] Tests de integración y UI en verde: `./gradlew connectedCheck` (si aplica)
- [ ] Build válido: `./gradlew assembleDebug`
- [ ] Registro de memoria creado: `doc/007-cuentas.md`
- [ ] `data-model.md` actualizado si la feature tocó el esquema (en el mismo cambio)
- [ ] Commits con formato de `.agent.md` §5.2, uno por unidad lógica

**Notas de ejecución:** 
- Prioridad: mantener la app 100% offline, sin colores hardcodeados, y con tests ≥70% en dominio/DAOs.
- Si Room introduce complejidad inesperada (>2h), escalar a revisión de diseño antes de continuar.
- La cuenta demo "Efectivo Demo" con $1.000.000 CLP es crítica para la demostración académica.