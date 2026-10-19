# tech-stack.md — Constitución Tecnológica

> **Propósito de este documento:** Registrar las decisiones de stack con su justificación. Es el
> único lugar donde una tecnología se vuelve "oficial" para el proyecto (ver `.agent.md`, regla 6:
> ninguna dependencia sin entrada aquí).
>
> **Regla de oro de agnosticismo:** Mientras cualquier fila contenga `[TECNOLOGIA]` sin completar,
> está PROHIBIDO escribir código dependiente de lenguaje o framework. La selección de stack es una
> tarea con su propio registro en `doc/`.
>
> **Cómo completar cada tabla:** Para cada categoría, lista las candidatas evaluadas (mínimo 2),
> descarta con motivo y elige con justificación. Una elección sin alternativa evaluada no es una
> decisión, es un sesgo.

---

## 1. Lenguajes

| Uso                                            | Candidatas evaluadas          | Selección final | Versión          | Justificación                                                                                                                                                        |
| ---------------------------------------------- | ----------------------------- | --------------- | ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Desarrollo Nativo Móvil (Lógica de Aplicación) | Java, Kotlin, Dart (Flutter)  | Java            | Java 8+ / JDK 11 | Requisito obligatorio del proyecto para desarrollo nativo en Android Studio, asegurando compatibilidad directa con las APIs locales de Android sin capas adicionales |
| Persistencia de Datos y Consultas              | SQL (SQLite), Room SQL, Realm | SQL (SQLite)    | SQLite 3.x       | Motor relacional ligero integrado nativamente en Android que garantiza la persistencia, autonomía y funcionamiento 100% offline sin servidores                       |

> _Guía:_ Los lenguajes se deciden por ecosistema disponible, talento del equipo y requisitos no
> funcionales (rendimiento, concurrencia, portabilidad), no por moda.

---

## 2. Frameworks

| Rol en el sistema                                 | Candidatas evaluadas                                                                 | Selección final                                              | Versión                       | Justificación                                                                                                                                                                                                    |
| ------------------------------------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------ | ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Framework de Interfaz de Usuario y Vistas         | Android SDK (Android View System / Material Components), Jetpack Compose, Flutter    | Android SDK (Android Views &amp; Material Design Components) | AndroidX / Material 1.x / 2.x | Integración nativa directa con **Java** en Android Studio. Se descarta Jetpack Compose por requerir Kotlin y Flutter por romper el enfoque nativo requerido por el proyecto                                      |
| Framework de Renderizado Geográfico y Mapas       | osmdroid (OpenStreetMap), MapLibre Native, Google Maps Android SDK, Mapbox SDK      | osmdroid (OpenStreetMap Android)                             | 6.x                           | Permite almacenar en caché y renderizar mapas de forma **100% offline** sin depender de credenciales ni APIs propietarias en la nube (como Google o Mapbox). Re-evaluado frente a **MapLibre Native** el 2026-10-19 (ver §7): MapLibre es vectorial y más moderno, pero añade peso y complejidad de configuración (estilos, `GLSurfaceView`) innecesarios para el alcance actual (pines + tiles raster offline), mientras que la API Java de `MapView`/`Marker` de osmdroid es directa y ya estaba pre-aprobada. **Offline-first (2026-10-19):** configuración con teselas preempaquetadas para Santiago de Chile (zoom 10-14) en `assets/`; no requiere permiso `INTERNET` y el mapa es explorable desde el primer arranque sin conexión. |
| Framework de Seguridad y Autenticación Biométrica | AndroidX Biometric (`BiometricPrompt`), FingerprintManager (legacy), KeyguardManager | **AndroidX Biometric Library (** **BiometricPrompt** **)**   | 11.0+                         | API estándar oficial de Android Jetpack para validar huellas dactilares con retrocompatibilidad (API 23+) y gestión segura de credenciales por el SO                                                             |
| RFramework de Abstracción de Persistencia Local   | Room Persistence Library, SQLiteOpenHelper (Nativo API), Realm DB                    | Room Persistence Library                                     | 2.5.+ (o SQLiteOpenHelper)    | Proporciona una capa de abstracción sobre **SQLite** que valida consultas SQL en tiempo de compilación y se integra fluidamente con la arquitectura de la aplicación evitando código repetitivo (_boilerplate_). |

> _Guía:_ Evalúa madurez, curva de aprendizaje, coste de salida (¿qué pasa si hay que migrar?) y
> compatibilidad con el patrón arquitectónico declarado en `.agent.md` sección 5.1.

---

## 3. Base de Datos y Persistencia

| Necesidad de datos                                                                             | Candidatas evaluadas                                      | Selección final                                         | Modelo (relacional/documental/etc.) | Justificación                                                                                                                                                                                                                                   |
| ---------------------------------------------------------------------------------------------- | --------------------------------------------------------- | ------------------------------------------------------- | ----------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Persistencia Principal del Dominio** _(Cuentas, Transacciones, Compras, Deudas y Contactos)_ | SQLite (Nativo / Room), Realm, Firebase Realtime Database | **SQLite** _(a través de Room o_ _SQLiteOpenHelper_ _)_ | Relacional                          | Cumple con el requisito obligatorio de operar **100% offline y de forma local** sin servidores externos. Su estructura relacional garantiza la integridad referencial entre cuentas, movimientos geolocalizados, contactos y deudas compartidas |

**Decisiones de migración de esquema:** Gestión centralizada del versionado del esquema mediante los controladores nativos de `SQLiteOpenHelper` (_métodos_ _onCreate_ _y_ _onUpgrade_) o la API de **Room Migrations** (`Migration(startVersion, endVersion)`). Toda modificación en las tablas de cuentas, transacciones o deudas se ejecuta localmente en el dispositivo sin requerir sincronización remota ni comprometer los datos existentes.

**Capa de acceso y momento de adopción de cada API:** la adopción es progresiva y queda fijada por
fases para que la Fase 1 no dependa de una decisión todavía no tomada (ver `roadmap.md` §1):

- **Fases 1 y 2 — `SQLiteOpenHelper`:** el esquema se materializa en `onCreate()`/`onUpgrade()` y se
  expone mediante DAOs que encapsulan el SQL. _Justificación:_ permite validar el modelo de datos
  (8 tablas, claves foráneas, índices y saldos) sin la dependencia de anotaciones ni el coste de
  compilación de consultas, y mantiene el control explícito de `PRAGMA foreign_keys = ON`, que es
  un flag **por conexión** y no se puede activar dentro de una transacción.
- **Fase 3 en adelante — Room:** los DAOs migran a anotaciones `@Entity`/`@Dao` y se adoptan
  `Migration(startVersion, endVersion)`. _Justificación:_ la verificación de consultas en tiempo de
  compilación aporta más valor justo cuando la lógica de división de deudas se vuelve compleja.
- **Invariante común:** ambas capas consumen el **único DDL canónico** del Apéndice A de
  `spec/constitution/data-model.md`, que es la fuente de verdad del esquema. La versión vive en un
  solo lugar por vía: `PRAGMA user_version` con `SQLiteOpenHelper`, el parámetro `version` del
  `RoomDatabase` con Room. Ninguna de las dos puede divergir de aquel documento, y
  `fallbackToDestructiveMigration()` queda prohibido por perder información financiera.

**Modelo de datos:** la estructura relacional, los tipos de columna (dinero en centavos `INTEGER`,
coordenadas en `REAL`), la política de integridad referencial, los índices y el versionado del
esquema están definidos en **`spec/constitution/data-model.md`**. Ese documento es de lectura
obligatoria antes de escribir cualquier DAO o migración.

**Respaldo y recuperación:** Al tratarse de un sistema con arquitectura 100% autónoma y sin servidores centrales, la política de resguardo se fundamenta en la exportación e importación manual del archivo de base de datos SQLite (`.db`) hacia el almacenamiento interno o externo del dispositivo móvil, previa validación del usuario.

**Datos sensibles y cifrado:** La barrera primaria de seguridad y resguardo de la información financiera se gestiona mediante autenticación biométrica obligatoria por huella dactilar a través de la API `BiometricPrompt`. Las claves y estados de configuración sensibles se protegen a nivel de dispositivo mediante el **Android Keystore System** o almacenamiento local cifrado (`EncryptedSharedPreferences`)

> _Guía:_ Empieza por el modelo de datos del dominio; la BD se elige después, no al revés.

---

## 4. Herramientas de Testing

| Nivel de prueba         | Herramienta                                                | Versión                  | Qué cubre exactamente                                                                                                                                                                     |
| ----------------------- | ---------------------------------------------------------- | ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Unitarias               | **JUnit 4** / **Mockito**                                  | 4.13.2 / 5.x             | Pruebas de la lógica de negocio pura: algoritmos de división de gastos grupales, cálculo de saldos, validadores de entrada y lógica de agendamiento sin requerir el framework de Android. |
| Integración             | **Robolectric** / **androidx.test (Room In-Memory)**       | 4.10+ / 1.5+             | Verificación de la persistencia de datos en la base de datos **SQLite** (operaciones CRUD en DAOs, consultas SQL de movimientos y cuentas) sin necesidad de un dispositivo físico.        |
| Extremo a extremo (E2E) | Espresso (AndroidX Test)                                   | 3.5.1+                   | Automatización de flujos completos de pantalla: flujo de acceso biométrico, navegación por el panel principal, creación de cuentas y registro de transacciones con geolocalización.       |
| Carga / rendimiento     | **Android Profiler** (Android Studio) / **Macrobenchmark** | Nativo de Android Studio | Medición del consumo de memoria, CPU y tiempos de renderizado al cargar marcas de geolocalización en el mapa[4] y ejecutar consultas con gran cantidad de registros financieros locales   |

**Cobertura mínima exigida:** **70%** en la capa de lógica de negocio (Dominio/Modelos) y repositorios/DAOs de base de datos SQLite.
**Comando canónico de tests:** `./gradlew testDebugUnitTest connectedCheck`

---

## 5. CI/CD

| Etapa                              | Herramienta / Servicio    | Configuración vive en                            | Justificación                                                                                                                                                                      |
| ---------------------------------- | ------------------------- | ------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Integración continua               | GitHub Actions            | .github/workflows/ci.yml                         | Integración nativa con el repositorio Git que permite ejecutar compilaciones automáticas, análisis estático y pruebas en cada _Pull Request_ sin costo de infraestructura externa. |
| Despliegue continuo                | GitHub Actions + Fastlane | `.github/workflows/cd.yml` / `fastlane/Fastfile` | Automatiza la generación de artefactos firmados (`.apk` y `.aab`) y su publicación en canales de prueba interna (Google Play Internal Testing o GitHub Releases).                  |
| Calidad de código (lint/seguridad) | Android Lint + Checkstyle | `app/build.gradle` / `config/lint/lint.xml`      | Herramientas estándar e integradas en el pipeline de compilación de Android para detectar errores de código Java, vulnerabilidades de seguridad y desviaciones de estilo.          |

**Pipeline mínima obligatoria:** `lint` → `unit tests` → `integración (Robolectric)` → `build (assembleRelease / bundleRelease)` → despliegue a **Google Play Internal Testing / GitHub Releases (Staging)** → pruebas E2E (Espresso) → **Google Play Production / Release APK (Producción)**.

---

## 6. Deploy e Infraestructura

| Aspecto                               | Decisión                                                                                      | Alternativa considerada                                      | Justificación                                                                                                                                                                |
| ------------------------------------- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Plataforma de hosting                 | **Aplicación Cliente Móvil Local** _(Distribución vía Google Play Console / GitHub Releases)_ | Servidor Cloud Backend (AWS, Firebase, Heroku)               | Cumplimiento estricto del pilar **100% offline**: el código corre en el dispositivo del usuario y no existe infraestructura o servidor remoto central.                       |
| Empaquetado                           | **Android App Bundle (** **.aab** **) y APK (** **.apk** **)**                                | APK único monolítico sin optimizar                           | El formato `.aab` optimiza el tamaño de instalación según el dispositivo desde Google Play, mientras que el `.apk` permite la instalación directa y distribución offline.    |
| Gestión de configuración/secreto      | **Android Keystore System + Gradle** **local.properties** **\+ GitHub Secrets**               | Variables hardcodeadas en código Java / Configuración remota | Mantiene las claves de firma y credenciales fuera del repositorio. Las claves se inyectan en tiempo de compilación vía CI/CD o archivo `local.properties` (ignorado en Git). |
| Observabilidad (logs/métricas/trazas) | Android Logcat (Local) + StrictMode                                                           | Datadog, Sentry, Firebase Crashlytics                        | Se evitan herramientas de monitoreo en la nube para respetar el principio rector de **privacidad absoluta por defecto** y funcionamiento sin conexión a internet.            |
| Estrategia de versiones               | **Semantic Versioning (** **MAJOR.MINOR.PATCH** **)**                                         | N/A                                                          | Sincronizado con `versionCode` y `versionName` en `build.gradle` para coordinar de forma transparente las migraciones del esquema de base de datos SQLite.                   |

> _Recordatorio de seguridad:_ los secretos NUNCA van al repositorio ni a estos documentos; solo se
> referencia el mecanismo de obtención (ver `.agent.md` sección 6, prohibición 1).

---

## 7. Registro de cambios del stack

| Fecha      | Cambio                                                                                                 | Motivo                                                                                                   | Registro en doc/  |
| ---------- | ------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------- | ----------------- |
| 2026-09-25 | Definición inicial de stack: Java 8+, Android SDK Views, SQLite / Room, AndroidX Biometric y osmdroid. | Definición de la arquitectura base alineada a los requerimientos de la aplicación nativa y 100% offline. | doc/tech-stack.md |
| 2026-09-28 | Secuencia de la capa de acceso: `SQLiteOpenHelper` en Fases 1-2 y Room desde Fase 3, con un único DDL canónico en `spec/constitution/data-model.md`. | `tech-stack.md` §3 y `roadmap.md` §4 se contradecían sobre cuándo adoptar Room; la ambigüedad podía hacer depender la Fase 1 de una decisión no tomada. | doc/003-modelo-datos.md |
| 2026-10-07 | Adopción concreta de **Robolectric 4.17** (`testImplementation` en `app/build.gradle`) para los tests de DAO en JVM de la Feature 007 (T9), conforme a la fila "Integración" de §4 (Robolectric 4.10+). Incluye `testOptions.unitTests.includeAndroidResources = true`. | `tasks.md` T9 exige tests de DAO "sin dispositivo" en JVM local; un sondeo empírico demostró que `Room.inMemoryDatabaseBuilder()` falla en JVM pura (`Method getCacheDir ... not mocked`) porque Room necesita `Context`/SQLite reales de Android. Candidatas evaluadas (§4): Robolectric (elegida: ya preaprobada, sin dispositivo) vs tests instrumentados `androidTest/` (descartada: requiere emulador) vs adaptador JDBC propio sobre `sqlite-jdbc` (descartada: dependencia sin registrar + ~300 líneas de adaptador). | doc/007-cuentas.md |
| 2026-10-19 | Re-confirmación de **osmdroid 6.x** como framework de mapas (§2) para la Feature 009, con evaluación explícita de **MapLibre Native** (descartada) como candidata adicional. | La fila de §2 estaba pre-aprobada desde el 2026-09-25 sin contraste documentado. Al abrir la Feature `009-geolocalizacion-map` se re-evalúa frente a MapLibre Native para dejar constancia de la comparación de candidatas que exige este documento; se mantiene osmdroid por simplicidad de API y encaje con el alcance (pines y tiles raster offline). | doc/008-historial.md |
| 2026-10-19 | Configuración **offline-first** de osmdroid (§2): teselas OSM **preempaquetadas** para Santiago de Chile (zoom 10-14) en `assets/`, **sin** permiso `INTERNET`. | Decisión de alcance de la Feature 009: se mantiene el pilar "100% offline" estricto (Opción B) para que el mapa funcione desde el primer arranque sin conexión, en lugar de descargar/cachear teselas con red. Peso estimado ~30-50 MB en el APK. | doc/009-geolocalizacion-map.md (al cerrar la 009) |
