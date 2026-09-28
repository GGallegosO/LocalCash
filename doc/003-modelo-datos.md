# doc/003-modelo-datos.md — Registro de Memoria: Constitución del Modelo de Datos

> **Propósito de este archivo:** Documentar las decisiones de modelado de datos de **LocalCash**
> (estructura relacional, tipos monetarios, política de integridad referencial, índices y
> versionado) exigidas por `.agent.md` §7 tras la creación de `spec/constitution/data-model.md`.

---

## Ficha de la tarea

| Campo                         | Valor                                                                                |
| ----------------------------- | ------------------------------------------------------------------------------------ |
| **Fecha**                     | 2026-09-28                                                                           |
| **Tarea ejecutada**           | Redacción, validación y estructuración del modelo de datos oficial (8 tablas SQLite)    |
| **Responsable**               | GG                                                                                   |
| **Duración estimada vs real** | [1 sesión] vs [1 sesión]                                                             |
| **Estado**                    | Completado                                                                           |

---

## Tarea Ejecutada (resumen narrativo)

Se redactó `spec/constitution/data-model.md`, cuarto documento de la constitución del proyecto
(`mission.md`, `tech-stack.md`, `roadmap.md` y ahora el modelo de datos), que fija el esquema
relacional local sobre el que se construirá la Fase 1. El documento cubre los cinco dominios de la
aplicación: cuentas, movimientos con geolocalización, categorías, agenda de contactos y el ciclo de
gasto compartido (compra grupal → ítems → deudas por participante).

El contenido incluye el ERD en Mermaid, el diccionario de datos columna por columna de las 8
tablas, el mapa completo de claves foráneas con su política de borrado, 8 invariantes verificables
y tres máquinas de estado, los índices SQL con el mapa consulta→índice, ocho decisiones técnicas
justificadas con sus alternativas descartadas, la estrategia de versionado y migraciones, y un
Apéndice A con el DDL ejecutable.

**Segunda revisión: alineación con la plantilla oficial.** El documento se reescribió para seguir
literalmente la estructura de `spec/constitution/data-model.md` de
[Template-SSD](https://github.com/GGallegosO/Template-SSD), que es la plantilla de la que este
repositorio procede. Se adoptaron sus siete encabezados (`1. Resumen de la Decisión`, `2. Diagrama
Entidad-Relación`, `3. Diccionario de Entidades`, `4. Relaciones y Cardinalidades`, `5. Decisiones
de Diseño del Esquema (ADR-lite)`, `6. Estrategia de Evolución del Esquema`, `7. Trazabilidad hacia
Features`), su bloque de propósito introductorio, sus guías de redacción y el bloque de "Imagen
renderizada (opcional, complementaria)". La tabla de campos por entidad pasa de las 5 columnas
genéricas de la plantilla a 5 columnas equivalentes pero materializadas: `Campo`, `Tipo SQLite`,
`Clave / Restricción`, `Nullable` y `Descripción y regla de negocio`; la columna *Tipo lógico* se
materializa como *Tipo SQLite* porque el stack ya está congelado en `tech-stack.md` y dejar el tipo
agnóstico al motor ya no aporta nada.

**Dos cambios de esquema acompañaron esa revisión, ambos exigidos por el ERD aprobado:**

1. La tabla de deudas pasa de `DEUDAS_PARTICIPALES` (errata heredada del primer borrador) a
   **`DEUDAS_PARTICIPANTES`**, corrigiendo la abierta que el documento anterior señalaba.
2. Las relaciones `COMPRAS_GRUPALES → DEUDAS_PARTICIPANTES` e `ITEMS_COMPRA → DEUDAS_PARTICIPANTES`
   pasan a cardinalidad `|o--o{` (cero o muchas), lo que formaliza dos casos que el modelo ya
   soportaba: una compra grupal puede no tener deudas (compra propia, sin reparto) y una deuda puede
   no imputarse a ningún ítem. Como consecuencia, `DEUDAS_PARTICIPANTES.item_id` deja de ser una
   desviación documentada del ERD para ser **exactamente** lo que el diagrama declara.

**El DDL y el ERD se validaron con herramientas, no solo por lectura:**

- El Apéndice A se ejecutó en `sqlite3` (3.54): crea las 8 tablas, 16 índices y
  `user_version = 1` sin errores; `PRAGMA foreign_key_check` queda limpio.
- La semilla de categorías se ejecutó dos veces seguidas: `INSERT OR IGNORE` sobre el índice único
  `(nombre, tipo_transaccion)` la hace idempotente, que es el requisito del primer arranque.
- 23 casos de escritura inválida se probaron uno a uno contra el esquema renombrado y todos fueron
  rechazados por el motor (`CHECK` de monto positivo, tipo de movimiento, formato ISO-8601, rango y
  atomicidad de coordenadas, subtotal = precio × cantidad, coherencia estado↔pago, unicidad de
  compra-por-movimiento, unicidad de deuda por ítem, unicidad de planificación, `RESTRICT` al
  borrar un contacto con deuda).
- Los comportamientos de borrado se comprobaron en ejecución: `CASCADE` de compra a ítems y deudas,
  `CASCADE` de cuenta a movimientos, `SET NULL` de categoría y de ítem, y `RESTRICT` de contacto
  deudor y de contacto pagador. Se verificó también que una compra grupal puede crearse sin deudas
  (cardinalidad `0..N`) y que, al borrar su ítem, la deuda sobrevive con `item_id IS NULL`.
- Los invariantes I-1, I-3, I-4 y I-7 se comprobaron con datos reales: el saldo se reconstruye
  desde los movimientos, y el total de la compra cuadra con la suma de ítems y de deudas.
- `EXPLAIN QUERY PLAN` confirmó que las consultas principales de la interfaz usan los
  índices declarados (`SEARCH ... USING INDEX`) y ninguna hace `SCAN` de tabla.
- El ERD se renderizó con `@mermaid-js/mermaid-cli`, y el bloque del documento se comparó por `diff`
  contra el ERD aprobado: **idéntico byte a byte**. Una comprobación adicional confirma que las 9
  relaciones referencian las 8 entidades declaradas, sin entidades fantasma.

---

## Archivos Creados / Modificados

**Creados:**

- `spec/constitution/data-model.md` — modelo de datos oficial con la estructura de la plantilla
  Template-SSD: §1 Resumen de la Decisión, §2 Diagrama Entidad-Relación, §3 Diccionario de
  Entidades (8 tablas + §3.9 índices y plan de consultas), §4 Relaciones y Cardinalidades (§4.1
  roles financieros, §4.2 reglas del motor frente a reglas de la DAO), §5 Decisiones de Diseño
  (ADR-lite), §6 Estrategia de Evolución, §7 Trazabilidad hacia Features y Apéndice A con el DDL
  canónico.
- `spec/constitution/diagrams/erd-localcash.svg` — imagen renderizada del ERD desde el propio
  bloque Mermaid, enlazada en §2 como complemento (la plantilla la declara opcional y no
  sustitutiva del código).
- `doc/003-modelo-datos.md` — ESTE registro de memoria.

**Modificados:**

- `spec/constitution/roadmap.md` — se añadió el hito "Modelo de Datos y Esquema SQLite" a la tabla
  de hitos (§2), en estado `Completado` y de prioridad `Alta`, como bloqueante de los hitos de
  cuentas, contactos, mapa y deudas.
- `spec/constitution/tech-stack.md` — se añadió en §3 el bloque "Capa de acceso al esquema y momento
  de adopción" que reparte `SQLiteOpenHelper` (Fases 1-2) y `Room` (Fase 3 en adelante), cerrando la
  contradicción que existía entre §3 y la deuda técnica de `roadmap.md` §4; y se añadió la fila
  correspondiente en el registro de cambios de §7.

---

## Decisiones Tomadas (con justificación)

1. **Dinero en `INTEGER` (centavos), no en `REAL`.**
   _Justificación:_ los saldos se acumulan por aritmética repetida y los repartos de deudas exigen
   que la suma de las partes cuadre exactamente con el total. En IEEE 754 `0.1 + 0.2 =
   0.30000000000000004`, es decir, el error aparecería precisamente en el saldo que el usuario ve.
   Con enteros, el `CHECK` de subtotal (`monto_subtotal = precio_unitario * cantidad`) es una
   verificación exacta y no una tolerancia. Se descartó `TEXT` decimal y `NUMERIC` porque SQLite no
   implementa precisión decimal fija. Consecuencia aceptada: la conversión `BigDecimal` ↔ centavos
   vive en un único punto de la capa de dominio y ningún DAO acepta `double`.

2. **`REAL` conservado solo para `latitud` y `longitud`.**
   _Justificación:_ `osmdroid` consume un `GeoPoint(double, double)`; guardar el mismo `double` que
   ve la librería evita una conversión por lectura. A 7 decimales la resolución es de ~1 cm, muy por
   debajo del error del GPS. Se descartaron `INTEGER` escalado (redondeo innecesario) y
   `R-Tree`/`SpatiaLite` (dependencia nativa no registrada en `tech-stack.md`, y el volumen local no
   lo justifica).

3. **Política única de borrado en tres intenciones: `CASCADE`, `RESTRICT` y `SET NULL`.**
   _Justificación:_ decidir caso por caso invita a la inconsistencia, así que se fijó una regla
   legible: `CASCADE` cuando el hijo no existe sin el padre; `RESTRICT` cuando el hijo guarda un
   dato no recuperable (la identidad de una persona: pagador, deudor, planificador); `SET NULL`
   cuando el padre es etiqueta o detalle. El caso de `pagado_por_contacto_id` motivó la regla: con
   `SET NULL`, borrar al contacto que pagó convertiría silenciosamente su pago en un pago del
   usuario principal, reescribiendo el pasado. `RESTRICT` además se traduce al error de negocio
   `CONTACTO_EN_USO`, que la UI explica y resuelve reasignando o eliminando con doble confirmación.

4. **`DEUDAS_PARTICIPANTES.item_id` nullable, alineado con el ERD tras la revisión.**
   _Justificación:_ en la primera versión del ERD (`ITEMS_COMPRA ||--o{ DEUDAS_PARTICIPALES`) la
   columna debía ser `NOT NULL`, lo que prohibía el caso real "la cena se dividió a partes iguales
   sin que nadie apunte qué pidió" y hacía que borrar un ítem destruyera la deuda. La revisión del
   ERD adoptó `ITEMS_COMPRA |o--o{ DEUDAS_PARTICIPANTES`, con lo que la columna nullable es
   exactamente lo que el diagrama declara. Su coste se mantiene: la unicidad de
   `(compra, ítem, contacto)` con `ítem = NULL` la vigila la DAO, no el motor, y por eso tiene test
   propio.
   `compra_grupal_id` sigue siendo `NOT NULL` aunque el `|o` de la izquierda se podría leer en ambos
   sentidos: una deuda sin compra no tiene significado de dominio y rompería el invariante I-4. Esa
   lectura del diagrama está declarada en §2.1 del documento.

5. **Sin columna de estado `parcial` en `COMPRAS_GRUPALES`.**
   _Justificación:_ almacenarla sería una segunda fuente de verdad capaz de divergir de las deudas.
   Se deriva con `EXISTS (... WHERE estado <> 'liquidado')`, que además evita una clase entera de
   bugs de actualización. En `DEUDAS_PARTICIPANTES` el estado sí se almacena porque es el objeto que
   la UI filtra y la aritmética de abonos necesita.

6. **`saldo_actual` desnormalizado, con `TRANSACCIONES` como única fuente de verdad.**
   _Justificación:_ el panel principal muestra el saldo de cada cuenta y recalcularlo con `SUM` en
   cada lectura es unAggregate sobre la tabla de movimientos. Se actualiza con una sola sentencia
   atómica (`saldo_actual = saldo_actual ± :monto`) dentro de la transacción de la escritura, lo
   que elimina la condición de carrera del patrón leer-restar-escribir. La coherencia se verifica con
   el invariante I-1, no se supone.

7. **Fechas en `TEXT` ISO-8601 UTC.**
   _Justificación:_ el orden lexicográfico de ISO-8601 UTC coincide con el cronológico, así que
   `ORDER BY fecha_hora` usa el índice sin conversión, y el `.db` exportado —único mecanismo de
   respaldo del proyecto— queda legible. Se descartó epoch en enteros por opaco y porque el modelo
   no necesita aritmética de fechas.

8. **`AUTOINCREMENT` en las 8 tablas.**
   _Justificación:_ sin ella SQLite reutiliza el id de la última fila eliminada, y un respaldo
   importado sobre una base con ids distintos colisionaría con referencias guardadas. UUID en `TEXT`
   duplicaría el tamaño de la clave sin ganar nada en un dispositivo de un solo usuario.

9. **Índices parciales y compuestos declarados caso por caso.**
   _Justificación:_ los índices de `estado <> 'liquidado'`, `categoria_id IS NOT NULL` y
   `latitud IS NOT NULL` no incluyen las filas que ninguna consulta pide, y los compuestos
   `(cuenta_id, fecha_hora DESC, id DESC)` y `(contacto_id, estado)` resuelven el filtro y el orden
   desde el índice. Se verificó con `EXPLAIN QUERY PLAN` que las consultas de la interfaz los usan.

10. **DDD limitado a SQLite 3.8+ (`minSdk` 23).**
    _Justificación:_ `tech-stack.md` fija AndroidX Biometric con retrocompatibilidad API 23, y el
    SQLite embebido en Android 6.0 no soporta `STRICT` (3.37+), índices de expresión (3.9+),
    `RETURNING` (3.35+) ni columnas generadas (3.31+). El Apéndice A se validó contra las
    construcciones realmente soportadas, y esa restricción queda declarada en §1.2 para que una
    migración futura no introduzca sintaxis que rompa en dispositivos antiguos.

11. **Aclaración de la capa de acceso en `tech-stack.md` §3: `SQLiteOpenHelper` en Fases 1-2,
    `Room` desde la Fase 3.**
    _Justificación:_ `tech-stack.md` §3 declaraba SQLite "a través de Room o SQLiteOpenHelper"
    mientras `roadmap.md` §4 programaba Room como deuda técnica a atender en Fase 3; era una
    contradicción que podía hacer que la Fase 1 dependiera de una decisión no tomada. Se fijó la
    secuencia por fases y un único DDL canónico (Apéndice A) del que ambas capas consumen.

---

## Lecciones Aprendidas

- **Validar el DDL con el motor evita discutir afirmaciones en la revisión.** Siete decisiones del
  modelo se comprobaron por ejecución y dos se ajustaron al resultado: el `CHECK` de subtotal
  `precio_unitario * cantidad` exige un techo en ambos factores (en desbordamiento, SQLite
  convierte el producto a `REAL` y el `CHECK` dejaría de ser exacto), y el estado "liquidada"
  necesita `transaccion_id NOT NULL` para que una compra sin asiento no pueda darse por saldada.
- **Un `CHECK` valida la fila, no el historial.** El motor aceptó escribir
  `estado='pendiente', monto_pagado=0` sobre una deuda que estaba `parcial`, porque la combinación
  es válida en sí misma. La máquina de estados la hace cumplir la DAO, no el esquema. Se documentó
  explícitamente en §4.5 qué garantiza el motor y qué garantiza el repositorio, porque presentar
  ambos como el mismo nivel de garantía es el error que induciría a un implementador a saltarse el
  test.
- **El diff del ERD contra la versión anterior fue más útil que la relectura.** Al comparar se
  detectó que una línea de relación decía `DEUDAS_PARTICIPANTES` y el bloque de entidad
  `DEUDAS_PARTICIPALES`: en Mermaid eso no es un error visible, crea una segunda entidad fantasma en
  el gráfico. La validación de nombres declarados frente a nombres referenciados en relaciones
  pasó a formar parte del chequeo.
- **Escribir el ERD con `REAL` y el DDL con enteros era una inconsistencia innecesaria.** Se alineó
  el propio diagrama a la decisión de centavos, para que el documento no exija recordar una
  excepción al leerlo.
- **`sqlite3` como banco de pruebas del esquema es barato y suficiente** mientras no exista el
  proyecto Gradle: los 23 casos de rechazo y los comportamientos de borrado se validaron en un
  directorio temporal, sin tocar el repositorio. Cuando exista el proyecto, esto se traslada a
  `MigrationTestHelper` y tests Robolectric de `SQLiteDatabase`.
- **La sintaxis moderna de SQLite rompe la compatibilidad declarada.** La semilla de categorías se
  escribió primero con `ON CONFLICT ... DO NOTHING`, que es _upsert_ y exige SQLite 3.24; el
  proyecto declara 3.8+ por `minSdk` 23. Se cambió a `INSERT OR IGNORE`, que funciona en todas las
  versiones. Merece la pena tener la versión mínima de SQLite del proyecto escrita a la vista
  antes de escribir la primera sentencia, no después.
- **La errata del nombre de tabla se resolvió, y el mecanismo que la perdió explica por qué
  ocurrió.** El primer borrador conservaba `DEUDAS_PARTICIPALES` (con `A` final) en todo el
  documento por no divergir del diagrama aprobado, y lo dejó anotado como pendiente. La revisión
  del ERD corrigió el nombre a `DEUDAS_PARTICIPANTES`, y con él desapareció el pendiente. Lección
  transferible: un nombre de tabla con errata sobrevive más tiempo que el error que la produce,
  porque "no divergir del aprobado" y "corregir el aprobado" son decisiones distintas que hay que
  tomar explícitamente. Mientras el nombre no se estabilice, el código debe referenciar la
  constante del DAO y nunca el literal.

---

## Próximos Pasos

1. **Fase 1 — materializar el esquema:** implementar `SQLiteOpenHelper.onCreate()` con el DDL del
   Apéndice A y `onConfigure()` con `PRAGMA foreign_keys = ON`, más `PRAGMA user_version = 1`.
2. **Fase 1 — capa de datos de contactos y cuentas:** DAOs con las consultas del mapa §5.2 y los
   tests de integración que ejecutan `PRAGMA foreign_key_check`.
3. **Fase 1 — tests del modelo:** convertir los 23 casos de rechazo de esta validación en tests de
   integración permanentes (Robolectric o `MigrationTestHelper`), incluidos los 4 comportamientos
   de borrado y la verificación de que `registrarAbono()` no puede reducir `monto_pagado`.
4. **Fase 3 — adoptar Room:** migrar los DAOs a anotaciones y volcar el DDL canónico en la primera
   `Migration`, sin divergir del Apéndice A.
5. **Fase 2 — geolocalización:** implementar el filtro de caja envolvente sobre
   `idx_transacciones_geo` y el agrupado de marcadores en `osmdroid`.
6. **Convertir el SVG en un PNG versionado** solo si la defensa o el informe lo necesitan; el
   Mermaid sigue siendo la fuente de verdad y la plantilla lo declara complementario.
7. Completar los placeholders pendientes de `.agent.md` §2 y §3 (stack y comandos canónicos) y de
   `tech-stack.md`, ahora que el esquema ya no tiene decisiones abiertas.
