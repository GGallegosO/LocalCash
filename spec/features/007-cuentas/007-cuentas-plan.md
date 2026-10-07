# plan.md — Plan Técnico de Implementación

> **Identificación:** 007-cuentas | **Fecha:** 05/10/2026 | **Autor:** GG
>
> **Propósito de este documento:** Definir CÓMO se implementará la feature descrita en `spec.md`:
> componentes, decisiones técnicas, riesgos y dependencias. Es el puente entre el requisito y las
> tareas ejecutables (`tasks.md`).
>
> **Regla:** Ninguna decisión de este archivo puede contradecir `.agent.md` ni `tech-stack.md`.
> Si la contradicción es inevitable, primero se actualiza la constitución con su registro en `doc/`.

---

## 1. Componentes Involucrados

> Lista TODO archivo/módulo que será creado o modificado. Si durante la implementación aparece un
> archivo fuera de esta lista, hay dos opciones: actualizar el plan (y justificar) o detenerse.

| Archivo / Módulo | Rol en la feature | Acción | Complejidad estimada |
|---|---|---|---|
| `app/build.gradle` | Agregar dependencias de Room + configurar export de schema | MODIFICAR | BAJA |
| `app/schemas/` | Carpeta para exportar schema SQLite generado por Room | CREAR | BAJA |
| `app/src/main/java/com/localcash/data/db/AppDatabase.java` | Configuración de Room Database | CREAR | MEDIA |
| `app/src/main/java/com/localcash/data/db/SeedDataInitializer.java` | Inserta cuenta demo "Efectivo Demo" con saldo $1.000.000 CLP al primer inicio | CREAR | MEDIA |
| `app/src/main/java/com/localcash/data/entities/Cuenta.java` | Entidad Room: id, nombre, tipo, saldo (long, en pesos CLP) | CREAR | BAJA |
| `app/src/main/java/com/localcash/data/entities/Transaccion.java` | Entidad Room: id, cuentaId, tipo, monto (long, en pesos CLP), concepto, fecha | CREAR | BAJA |
| `app/src/main/java/com/localcash/data/dao/CuentaDao.java` | DAO: insert, getAll, updateSaldo, delete | CREAR | MEDIA |
| `app/src/main/java/com/localcash/data/dao/TransaccionDao.java` | DAO: insert, getByCuentaId, getAll | CREAR | MEDIA |
| `app/src/main/java/com/localcash/domain/model/Cuenta.java` | Modelo de dominio con BigDecimal para cálculos | CREAR | BAJA |
| `app/src/main/java/com/localcash/domain/model/Transaccion.java` | Modelo de dominio con BigDecimal para cálculos | CREAR | BAJA |
| `app/src/main/java/com/localcash/domain/service/TransaccionService.java` | Lógica de negocio: validar saldo, calcular totales | CREAR | ALTA |
| `app/src/main/java/com/localcash/domain/error/ErrorCode.java` | Códigos de error: SALDO_INSUFICIENTE, MONTO_INVALIDO, CUENTA_VACIA | CREAR | BAJA |
| `app/src/main/java/com/localcash/domain/error/BusinessRuleException.java` | Excepción unchecked que transporta un ErrorCode a la UI (exigida por tasks.md T7) | CREAR | BAJA |
| `app/src/main/java/com/localcash/ui/cuentas/CuentasActivity.java` | Activity principal del Módulo 1 | CREAR | ALTA |
| `app/src/main/res/layout/activity_cuentas.xml` | Layout con BottomNav + ViewFlipper | CREAR | MEDIA |
| `app/src/main/res/layout/view_cuentas.xml` | Vista 1: Resumen de cuentas con RecyclerView | CREAR | MEDIA |
| `app/src/main/res/layout/view_transaccion.xml` | Vista 2: Formulario de transacción | CREAR | MEDIA |
| `app/src/main/res/layout/view_mapa.xml` | Vista 3: Placeholder (Próximamente) | CREAR | BAJA |
| `app/src/main/res/layout/view_contactos.xml` | Vista 4: Placeholder (Próximamente) | CREAR | BAJA |
| `app/src/main/res/menu/bottom_nav_menu.xml` | Menú de navegación inferior | CREAR | BAJA |
| `app/src/main/res/values/strings.xml` | Strings con prefijo `aCuentas_` | MODIFICAR | BAJA |
| `app/src/main/AndroidManifest.xml` | Registrar CuentasActivity | MODIFICAR | BAJA |
| `app/src/main/java/com/localcash/ui/login/LoginActivity.java` | Redirigir a CuentasActivity tras login | MODIFICAR | BAJA |
| `app/src/test/java/com/localcash/data/dao/CuentaDaoTest.java` | Tests unitarios para CuentaDao | CREAR | MEDIA |
| `app/src/test/java/com/localcash/data/dao/TransaccionDaoTest.java` | Tests unitarios para TransaccionDao | CREAR | MEDIA |
| `app/src/test/java/com/localcash/domain/service/TransaccionServiceTest.java` | Tests unitarios para lógica de negocio | CREAR | ALTA |
| `doc/007-cuentas.md` | Registro de memoria | CREAR | BAJA |

**Contrato de datos / interfaz expuesta:**
- **Entrada esperada:** 
  - Para Cuenta: nombre (String), tipo (Enum: EFECTIVO/AHORRO/TARJETA), saldo (long, en pesos CLP)
  - Para Transaccion: cuentaId (long), tipo (Enum: INGRESO/EGRESO), monto (long, en pesos CLP), concepto (String), fecha (long timestamp)
- **Salida garantizada:** 
  - Cuentas y transacciones persistidas en SQLite vía Room
  - Saldos calculados automáticamente (suma de ingresos - suma de egresos)
  - UI actualizada en tiempo real tras cada operación
  - Schema SQLite exportado en `app/schemas/` para documentación técnica
  - Tests unitarios con cobertura ≥70% en DAOs y lógica de dominio
  - Cuenta demo "Efectivo Demo" con saldo $1.000.000 CLP disponible tras primer login con "admin"
- **Errores posibles (códigos y significado):** 
  - `CUENTA_VACIA`: nombre no proporcionado o vacío
  - `MONTO_INVALIDO`: monto <= 0
  - `SALDO_INSUFICIENTE`: egreso supera el saldo disponible de la cuenta
  - `CUENTA_NO_ENCONTRADA`: referencia a cuenta inexistente

---

## 2. Decisiones de Arquitectura

Formato recomendado por decisión: contexto → opciones → elección → justificación → consecuencia.

1. **Persistencia de Datos Financieros**
   - Opciones consideradas: A) Mantener EncryptedSharedPreferences, B) Introducir Room (SQLite)
   - Decisión: **Opción B**
   - Justificación: Se requieren operaciones relacionales (sumar saldos, filtrar por cuenta, ordenar por fecha) que son ineficientes en SharedPreferences. Room es el estándar oficial de Android, está en el `tech-stack.md`, y permite consultas SQL tipadas.
   - Consecuencia aceptada: Requiere configurar dependencias de Room, crear entidades/DAOs, y manejar operaciones asíncronas (Room no permite queries en el hilo principal).

2. **Representación de Dinero (Pesos Chilenos)**
   - Opciones consideradas: A) Usar `double`/`float`, B) Usar `long` en centavos, C) Usar `long` en pesos CLP
   - Decisión: **Opción C**
   - Justificación: Los pesos chilenos (CLP) no tienen subdivisión en centavos en circulación (desde 1984). La unidad mínima es 1 peso. Usar `long` directamente en pesos evita conversiones innecesarias y mantiene la precisión. Se usa `long` (no `int`) porque los montos pueden ser grandes (ej. $10.000.000 CLP = 10 millones, dentro del rango de `long`).
   - Consecuencia aceptada: La regla del `.agent.md` §6.10 se interpreta como "usar enteros para moneda", adaptada al contexto local de CLP. Se documenta esta decisión en `doc/007-cuentas.md`.

3. **Navegación y UI del Módulo 1**
   - Opciones consideradas: A) Múltiples Activities, B) Single Activity + Fragments, C) Single Activity + ViewFlipper
   - Decisión: **Opción C**
   - Justificación: Mantiene el estado de los formularios fácilmente (sin perder datos al cambiar de vista), evita el boilerplate de Fragments y ViewModel, y es consistente con la arquitectura de la Feature 006 (Registro con ViewFlipper).
   - Consecuencia aceptada: Todas las vistas comparten el mismo ciclo de vida de la Activity, lo cual es aceptable para esta fase.

4. **Paleta de Colores y Temas**
   - Opciones consideradas: A) Colores hardcodeados (#FF0000), B) Atributos Material 3 (?attr/colorPrimary)
   - Decisión: **Opción B**
   - Justificación: Garantiza soporte nativo para modo claro/oscuro sin duplicar layouts, mantiene coherencia visual con Login y Register, y cumple con el pilar de Material Design 3 del proyecto.
   - Consecuencia aceptada: Requiere disciplina estricta para no usar colores hardcodeados; el linter debe verificarse constantemente.

5. **Geolocalización en Transacciones (Decisión Negativa)**
   - Opciones consideradas: A) Implementar osmdroid ahora, B) Solo UI del switch, C) Omitir completamente
   - Decisión: **Opción B**
   - Justificación: La integración de osmdroid requiere permisos de ubicación, configuración de mapas offline, y manejo de coordenadas. Es mejor separar esta complejidad en la Feature 008 para mantener esta feature atómica.
   - Consecuencia aceptada: El switch de geolocalización será visible pero no funcional; se documentará como "Próximamente".

6. **Demostración de SQLite para Evaluación Académica**
   - Opciones consideradas: A) Usar Room y no mencionar SQLite, B) Usar Room + documentar schema SQL, C) Usar SQLite directo sin Room
   - Decisión: **Opción B**
   - Justificación: Room es el estándar moderno de Android y produce código más mantenible, pero el requisito académico exige demostrar conocimiento de SQLite. Al exportar el schema y documentar las consultas equivalentes, cumplimos ambos requisitos.
   - Consecuencia aceptada: Requiere configurar `room.schemaLocation` en `build.gradle` y generar documentación adicional del schema SQL.

7. **Validación de Saldo Insuficiente (Regla de Negocio)**
   - Opciones consideradas: A) Validar en el DAO, B) Validar en la Activity, C) Validar en la capa `domain/`
   - Decisión: **Opción C** (cumple `.agent.md` §5.1)
   - Justificación: La regla "no permitir egresos que superen el saldo" es lógica de negocio pura. Debe vivir en `domain/` (ej. `TransaccionService.validarSaldoSuficiente()`) para ser testeable sin dispositivo y reutilizable.
   - Consecuencia aceptada: Requiere crear una capa `domain/service/` adicional, pero es la arquitectura correcta según el `.agent.md`.

8. **Datos Semilla para Cuenta Demo (admin/123456)**
   - Opciones consideradas: A) Base de datos vacía al primer login, B) Insertar datos semilla automáticamente tras crear la base de datos, C) Requerir que el usuario cree cuentas manualmente
   - Decisión: **Opción B**
   - Justificación: El proyecto es académico y requiere demostración inmediata. Al iniciar sesión con "admin/123456", el usuario debe ver una cuenta pre-cargada con saldo ficticio ($1.000.000 CLP) para probar transacciones sin configuración previa. Esto mejora la UX de demo y cumple el objetivo de mostrar la app funcionando "out of the box".
   - Consecuencia aceptada: Requiere implementar un `RoomDatabase.Callback` que inserte la cuenta demo tras la primera inicialización de la base de datos. Los datos semilla deben marcarse claramente como "Demo" en la UI para evitar confusión con datos reales.

---

## 3. Riesgos Identificados

| # | Riesgo | Probabilidad | Impacto | Mitigación | Dueño |
|---|---|---|---|---|---|
| 1 | Room requiere operaciones asíncronas (no en hilo principal) | ALTA | ALTA | Usar `Executors.newSingleThreadExecutor()` para operaciones de BD simples en esta fase; migrar a Coroutines/Flow en features posteriores si se adopta Kotlin. | GG |
| 2 | La IA hardcodee colores en los nuevos layouts | MEDIA | MEDIA | El prompt enfatizará la regla de "Cero colores hardcodeados"; revisar con lint tras cada generación. | GG |
| 3 | Conflictos de merge con `strings.xml` o `AndroidManifest.xml` | BAJA | MEDIA | Hacer commit frecuente y usar rebase antes de merge; resolver conflictos manualmente si aparecen. | GG |
| 4 | Errores de precisión en cálculos financieros | BAJA | ALTA | Usar `long` (pesos CLP) en `data/` y `BigDecimal` en `domain/`; tests unitarios exhaustivos para validar cálculos. | GG |
| 5 | Cobertura de tests insuficiente (<70%) | MEDIA | MEDIA | Escribir tests unitarios para cada DAO y método de dominio desde el inicio; usar Robolectric para tests de Room sin dispositivo. | GG |

**Criterio de abortar/replantear:** Si la configuración de Room supera 2 horas de esfuerzo o introduce errores de compilación recurrentes, se escala a revisión de diseño y se considera mantener SharedPreferences temporalmente (aunque no es la solución ideal).

---

## 4. Dependencias Externas

| Dependencia | Tipo | Estado del acuerdo | Plan B si falla |
|---|---|---|---|
| `androidx.room:room-runtime` | Librería de AndroidX | DISPONIBLE (estable, versión 2.6+) | N/A (es estándar de Android) |
| `androidx.room:room-compiler` | Annotation Processor | DISPONIBLE | N/A |
| `androidx.room:room-testing` | Librería para tests de Room | DISPONIBLE | Usar Robolectric si hay problemas con el test runner |
| Emulador Android / Dispositivo Físico | Entorno de ejecución | DISPONIBLE | Usar `BuildConfig.DEBUG` para bypass si hay problemas de permisos |

**Requisitos previos del entorno:** 
- Android Studio con soporte para Java 8+ (Room requiere anotaciones)
- Emulador o dispositivo con API 28+ (mínimo para Room según `tech-stack.md`)
- No se requieren credenciales de red ni servicios en la nube (100% offline)

---

## 5. Estrategia de verificación

- **Criterios de aceptación → tests:** Cada CA de `spec.md` mapea a tests unitarios en `app/src/test/java/com/localcash/`:
  - CA-1 → `CuentaDaoTest.testInsertAndGetAll()`
  - CA-2 → `TransaccionServiceTest.testRegistrarEgresoConSaldoSuficiente()`
  - CA-3 → Verificación manual (cambio de vistas no requiere lógica de dominio)
  - CA-4 → Verificación manual (toggle de tema es UI pura)
- **Pruebas manuales necesarias:**
  1. Iniciar sesión con "admin/123456" → debe navegar a CuentasActivity
  2. En Vista 1: Debe aparecer la cuenta "Efectivo Demo" con saldo $1.000.000 CLP (dato semilla)
  3. En Vista 1: Crear cuenta "Ahorro" con saldo inicial $0 → debe aparecer en la lista
  4. En Vista 2: Registrar ingreso de $50.000 en "Efectivo Demo" → saldo debe actualizarse a $1.050.000
  5. En Vista 2: Registrar egreso de $20.000 en "Efectivo Demo" → saldo debe actualizarse a $1.030.000
  6. En Vista 2: Intentar registrar egreso de $2.000.000 en "Efectivo Demo" (saldo $1.030.000) → debe mostrar error "Saldo insuficiente" y NO guardar la transacción
  7. Cambiar entre pestañas del BottomNav → ViewFlipper debe mostrar la vista correcta
  8. Presionar toggle de tema en cualquier vista → debe cambiar entre claro/oscuro
  9. Verificar que Login y Register sigan funcionando intactos
  10. Verificar que el archivo `app/schemas/` contenga el schema SQLite exportado
- **Datos de prueba requeridos:** 
  - Cuenta demo (seed): "Efectivo Demo", tipo EFECTIVO, saldo $1.000.000 CLP
  - Cuenta nueva: "Ahorro", tipo AHORRO, saldo inicial $0
  - Transacción 1: Ingreso $50.000, concepto "Sueldo Demo"
  - Transacción 2: Egreso $20.000, concepto "Almuerzo Demo"
  - Transacción 3 (caso borde): Egreso $2.000.000 → debe fallar con "Saldo insuficiente"
- **Criterio de "terminado" (Definition of Done):** 
  - Código compilado sin warnings (`./gradlew lint`)
  - Build exitoso (`./gradlew assembleDebug`)
  - Tests unitarios pasando (`./gradlew testDebugUnitTest`) con cobertura ≥70% en `data/` y `domain/`
  - Flujo manual verificado exitosamente (10 pruebas arriba)
  - Schema SQLite exportado en `app/schemas/`
  - Documentación actualizada (`doc/007-cuentas.md`) con sección de estructura de base de datos SQLite
  - Commits con formato Conventional Commits