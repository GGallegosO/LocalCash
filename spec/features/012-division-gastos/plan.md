# plan.md — Plan Técnico de Implementación

> **Identificación:** 012-division-gastos | **Fecha:** 2026-10-25 | **Autor:** Agente SDD
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
| `spec/constitution/data-model.md` | Sincronizar esquema: ERD Mermaid, diccionario `GASTOS_COMPARTIDOS`, invariantes I-12.., DDL Apéndice A | MODIFICAR (antes del código, regla AGENTS.md) | MEDIA |
| `data/entities/GastoCompartido.java` | Entidad Room: tabla `GASTOS_COMPARTIDOS` con índices | CREAR | BAJA |
| `data/dao/GastoCompartidoDao.java` | DAO: inserción por lotes, consulta por transacción, partes pendientes por contacto, resumen de deudas, operaciones de cobro. Nombre de tabla por constante | CREAR | ALTA |
| `data/db/AppDatabase.java` | Entidad + DAO + versión 5 + `MIGRATION_4_5` + `gastoCompartidoDao()` | MODIFICAR | ALTA |
| `domain/model/GastoCompartido.java` | Modelo de dominio inmutable con `toEntity()`/`fromEntity()` (se crea en T7, cuando el registro del egreso necesita mapear las partes) | CREAR | BAJA |
| `domain/service/GastoCompartidoService.java` | Reglas de negocio: INV-1, INV-2 (reparto igual y personalizada), validaciones de monto | CREAR | ALTA |
| `domain/error/ErrorCode.java` | Nuevos códigos de error de negocio | MODIFICAR | BAJA |
| `ui/cuentas/TransaccionViewModel.java` | Extender `registrarTransaccion` con partes compartidas y cobro atómico | MODIFICAR | ALTA |
| `ui/cuentas/CuentasActivity.java` | Formulario de egreso: checkbox "Compartido", selector de contactos, modo de división, suma en vivo | MODIFICAR | ALTA |
| `res/layout/dialog_division_gastos.xml` | Layout del bloque de división (modo + filas de partes) | CREAR | BAJA |
| `res/layout/item_parte_gasto.xml` | Fila: contacto + monto asignado editable/etiqueta de estado | CREAR | BAJA |
| `ui/contactos/ContactosViewModel.java` | Resumen de deudas (Σ pendientes por contacto) + marcar parte pagada | MODIFICAR | MEDIA |
| `ui/contactos/ContactosActivity.java` | Sección "Resumen de deudas" en la lista; detalle/historial por contacto con acción "Marcar pagada" (cierra CA-9/CA-10 heredados de 011) | MODIFICAR | ALTA |
| `res/layout/item_resumen_deuda.xml` | Fila del resumen: contacto + total que debe | CREAR | BAJA |
| `res/layout/dialog_detalle_contacto.xml` | Detalle de contacto con historial de partes compartidas | CREAR | BAJA |
| `res/values/strings.xml` | Textos en español: etiquetas, botones, mensajes de error | MODIFICAR | BAJA |
| `test/GastoCompartidoServiceTest.java` | Tests de dominio puro (INV-1, INV-2, casos borde 1–6) | CREAR | MEDIA |
| `test/GastoCompartidoDaoTest.java` | Robolectric + Room in-memory: sumas, unicidad, RESTRICT, CASCADE, cobro atómico, `PRAGMA foreign_keys`, `foreign_key_check` | CREAR | ALTA |

**Contrato de datos / interfaz expuesta:**

- Entrada esperada: registro — `cuentaId`, `monto` del egreso (> 0), `contactosIds` (≥ 1), modo
  (`IGUAL` | `PERSONALIZADA`), opcionalmente `montosPorContacto` (cada uno > 0). Cobro — `parteId`.
- Salida garantizada: egreso registrado + N partes `pendiente` cuya suma iguala el monto (o `ErrorCode`);
  al cobrar: parte `pagado` con `fecha_pago` + ingreso automático en historial + saldo actualizado
  (o `ErrorCode`). Resumen: lista `(contacto, Σ adeudado)` por contacto con pendientes.

**Contrato mutable invocable de `GastoCompartidoService`** (T5 — firmas implementadas; es lo que los
tests de T8 pueden escribir desde ya, sin conocer Room ni Android):

| Método | Entrada | Devuelve | Regla |
|---|---|---|---|
| `validarReparto(long montoTotal, List<Long> contactosIds)` | total > 0; ≥ 1 id identificable (no nulo ni ≤ 0); sin repetir | `void`; lanza `BusinessRuleException` con `MONTO_INVALIDO`, `GASTO_SIN_CONTACTOS` o `CONTACTO_DUPLICADO` | INV-1 + validación común de INV-2 |
| `dividirIgual(long montoTotal, List<Long> contactosIds)` | total > 0; ≥ 1 id; `total >= n` | `List<PartePropuesta>` `(contactoId, montoAsignado)`; lanza `REPARTO_IGUAL_INSUFICIENTE` si `total < n` | Reparto entero: `base = total / n`, residuo `total % n` a los primeros ids de a 1 CLP; Σ = total exacto |
| `validarPersonalizado(long montoTotal, List<PartePropuesta> partes)` | total > 0; ≥ 1 parte; montos > 0; contactos sin repetir | `void`; lanza `MONTO_INVALIDO`, `GASTO_SIN_CONTACTOS`, `MONTO_PARTE_INVALIDO`, `CONTACTO_DUPLICADO` o `REPARTO_NO_CUADRA` | INV-2: Σ exacta del total |

- `PartePropuesta` (`contactoId`, `montoAsignado`) vive en `domain/model/`. `GastoCompartidoService`
  sigue el patrón de `ContactoService`/`LugarService`: clase final con métodos estáticos, y **todas las
  validaciones lanzan `BusinessRuleException` con su `ErrorCode`** (decisión del dueño en T5, en lugar
  de devolver `ErrorCode`), de modo que el llamador no puede ignorar un reparto inválido por accidente.
  Los códigos permiten a la UI traducir a texto; el dominio nunca produce mensajes de usuario.
- **Repositorio (T6), fuera del alcance de los tests puros:** `marcarPagada(long parteId)` devuelve
  `ErrorCode` (`null` = cobrada) y es el **único** punto de escritura del estado `pagado`. No existe
  método público que reabra una parte (`PARTE_YA_PAGADA` / `PARTE_INEXISTENTE` cubren el rechazo).
- Errores posibles (códigos y significado):
  - `GASTO_SIN_CONTACTOS`: egreso marcado compartido sin ningún contacto elegido (INV-1).
  - `REPARTO_NO_CUADRA`: la suma de las partes personalizadas no iguala el total del egreso (INV-2).
  - `REPARTO_IGUAL_INSUFICIENTE`: división igual con monto menor que el número de contactos (INV-2).
  - `MONTO_PARTE_INVALIDO`: una parte ≤ 0 o nula en división personalizada.
  - `CONTACTO_DUPLICADO`: el mismo contacto elegido dos veces para el mismo egreso (unicidad).
  - `PARTE_INEXISTENTE` / `PARTE_YA_PAGADA`: el cobro apunta a una fila que no existe o que ya no está
    pendiente (INV-3/INV-5).
  - `CONTACTO_EN_USO` (ya definido en la constitución §5.4): bloqueo de borrado por RESTRICT (INV-4).

---

## 2. Decisiones de Arquitectura

1. **¿`GASTOS_COMPARTIDOS` simple o el trío `COMPRAS_GRUPALES` + `ITEMS_COMPRA` + `DEUDAS_PARTICIPANTES` de la constitución?**
   - Opciones consideradas: implementar el trío completo de `data-model.md` §3.6–3.8; una sola tabla
     transacción↔contacto más simple (`GASTOS_COMPARTIDOS`).
   - Decisión: tabla única `GASTOS_COMPARTIDOS`, y `data-model.md` se actualiza **en el mismo cambio**
     registrándola como estado implementado de la Fase 3 (precedente: las notas "Estado implementado"
     de §3.9 para la Feature 009).
   - Justificación: 012 necesita reparto por egreso con cobro binario (pendiente/pagado); el desglose en
     ítems y la liquidación parcial corresponden al hito futuro de COMPRAS_GRUPALES. La regla de AGENTS.md
     ("no inventar esquema paralelo") se cumple porque la constitución se sincroniza, no se la ignora.
   - Consecuencia aceptada: la tabla del ERD v2 convive documentada como futura; 012 no usa
     `DEUDAS_PARTICIPANTES` todavía.

2. **¿El cobro de una parte mueve el saldo sin asiento contable? (Decisión del dueño, opción A)**
   - Opciones consideradas: `UPDATE CUENTAS SET saldo = saldo + monto` a secas; insertar un ingreso
     automático en `TRANSACCIONES` y luego actualizar el saldo, todo en la misma transacción.
   - Decisión: **con ingreso automático** (`tipo='ingreso'`, `descripcion='Devolución gasto compartido'`,
     `cuenta_id` = cuenta del egreso original, `monto` = `monto_asignado` de la parte).
   - Justificación: `data-model.md` I-1 exige `saldo = saldo_inicial + Σ ingresos − Σ egresos`; mover el
     saldo sin asiento lo rompería y rompería el test de recálculo existente. Además el cobro queda visible
     en el historial del usuario.
   - Consecuencia aceptada: el historial muestra ingresos generados por la app; la UI debe distinguirlos
     (descripción fija "Devolución gasto compartido").

3. **¿"Cuánto les debo" en el resumen? (Decisión del dueño, opción A)**
   - Decisión: 012 solo cubre **"me deben"** (partes pendientes de egresos pagados por el usuario); el
     caso "les debo" queda para la futura COMPRAS_GRUPALES donde `pagado_por_contacto_id ≠ NULL`.
   - Justificación: la entidad propuesta no tiene columna de dirección — todo egreso se asienta en una
     cuenta del usuario, así que toda parte pendiente es deuda **hacia** él.
   - Consecuencia aceptada: el resumen y el detalle de contacto muestran una sola dirección; se documenta
     como fuera de alcance en `spec.md` §7.

4. **¿Timestamps `INTEGER` epoch o `TEXT` ISO-8601? (Decisión del dueño: TEXT)**
   - Decisión: `fecha_creacion TEXT NOT NULL` y `fecha_pago TEXT` (nullable), formato ISO-8601 UTC,
     consistente con la convención de fechas de `data-model.md` §1 (tabla de precisión: orden
     lexicográfico = cronológico, `.db` exportable legible).
   - Justificación: mantener la constitución sin excepciones; ninguna consulta de 012 ordena por estas
     columnas, pero la consistencia tipográfica vale más que el ahorro de un conversor.
   - Consecuencia aceptada: la app genera la marca en ISO-8601 al crear y al cobrar.

5. **¿Dónde vive la máquina de estados pendiente→pagado?**
   - Opciones: exponer un `update(GastoCompartido)` genérico en el DAO; exponer solo
    `marcarPagada(parteId)` con la secuencia atómica.
   - Decisión: solo `marcarPagada(parteId)`; el DAO **no expone** ninguna operación que reabrisca una
     parte ni que escriba estado arbitrario.
   - Justificación: patrón establecido en `data-model.md` §4.2 ("rebajar abono" prohibido): el `CHECK`
     valida la fila, no el historial; el repositorio es quien hace cumplir la transición. La actualización
     usa `WHERE id = :parte AND estado = 'pendiente'` para que un cobro concurrente pierda el efecto
     sin corromper nada.
   - Consecuencia aceptada: corregir una parte cobrada por error exige borrar y reinsertar el egreso
     (comportamiento heredado de la inmutabilidad de `TRANSACCIONES`).

6. **Migración Room 4 → 5 no destructiva.**
   - `MIGRATION_4_5` ejecuta el DDL del Apéndice A sincronizado (`CREATE TABLE GASTOS_COMPARTIDOS` +
     índices, `IF NOT EXISTS`), sin `DROP TABLE` ni `fallbackToDestructiveMigration()` (`.agent.md` §6.11).
     Room valida el esquema: los índices se declaran también en `@Entity(indices=...)`
     (`ux_gastos_compartidos_transaccion_contacto` único sobre `(transaccion_id, contacto_id)`;
     `idx_gastos_compartidos_contacto_estado`). Ninguna columna involucrada es nullable, así que los
     índices pueden declararse completos sin `WHERE`.
   - **Corrección de T2 (2026-10-26):** se descarta el tercer índice
     `idx_gastos_compartidos_transaccion_id` que figuraba en el borrador. El único compuesto
     `(transaccion_id, contacto_id)` ya tiene `transaccion_id` como columna izquierda y sirve
     `WHERE transaccion_id = ?`; un índice adicional duplicaría escrituras sin añadir plan de consulta.
     `data-model.md` §3.12 lo prohíbe expresamente ("no se crean índices por si acaso").

7. **Reglas que el motor impone y reglas que solo valida el dominio** (Room no permite `CHECK` en
   `@Entity` — precedente documentado §3.9):
   - Motor: PK/FK con `ON DELETE CASCADE` (transacción) y `ON DELETE RESTRICT` (contacto), `NOT NULL`,
     unicidad `(transaccion_id, contacto_id)`.
   - Dominio con test propio: INV-1 (≥1 contacto), INV-2 (Σ partes = total; rechazo de monto < n y de
     partes ≤ 0), INV-3 (no reabrir), INV-5 (secuencia atómica de cobro).
   - `PRAGMA foreign_keys = ON`: Room lo emite por defecto por conexión, pero se mantiene el test
     explícito (regla AGENTS.md: verificar, no confiar en la librería) y `PRAGMA foreign_key_check` al
     terminar los tests de integración.

8. **Dinero: `INTEGER` (`long` en Java), pesos CLP directos.** Nunca `REAL`/`double` en capa de datos
   (`.agent.md` §6.10); el reparto igual es aritmética entera (`monto / n` y residuo `monto % n`
   distribuido de a 1 CLP); `BigDecimal` no aparece en esta feature porque no hay porcentajes ni
   subdivisiones.

9. **`CUENTAS.saldo_actual` es proyección.** Toda escritura del cobro usa aritmética atómica
   `SET saldo_actual = saldo_actual + :monto` dentro de `runInTransaction` — nunca leer-sumar-escribir
   (condición de carrera, regla AGENTS.md). El asiento de `TRANSACCIONES` sigue siendo la fuente de verdad.

**Matriz de propiedad de las invariantes** (`spec.md` §5 → capa que la hace cumplir → test que la prueba).
Ninguna invariante queda sin dueño ni se duplica el dueño entre capas: la UI nunca valida, el dominio
valida y calcula, el repositorio/DAO hace cumplir el ciclo de vida y el motor impone la integridad
referencial.

| Invariante | Dueño (capa) | Respaldo del motor | Test | Método del contrato |
|---|---|---|---|---|
| INV-1 · compartido = 0 contactos es inválido | `domain/` — `GastoCompartidoService.validarReparto` | Ninguno (Room no permite `CHECK` ni contar filas hijas) | `GastoCompartidoServiceTest` (T8) | `validarReparto` → `GASTO_SIN_CONTACTOS` |
| INV-2 · Σ partes = monto del egreso, al peso | `domain/` — `dividirIgual` / `validarPersonalizado` | Ninguno (regla agregada entre filas) | `GastoCompartidoServiceTest` (T8) + integración (T10) | `validarReparto` → `MONTO_INVALIDO`, `GASTO_SIN_CONTACTOS`, `CONTACTO_DUPLICADO`; `dividirIgual` → `REPARTO_IGUAL_INSUFICIENTE`; `validarPersonalizado` → `MONTO_PARTE_INVALIDO`, `REPARTO_NO_CUADRA` |
| INV-3 · una parte pagada no vuelve a pendiente | `data/` — repositorio/DAO: único write es `marcarPagada` con `WHERE estado='pendiente'` | `NOT NULL` + dominio cerrado de `estado` (sin `CHECK` en Room) | `GastoCompartidoDaoTest` (T9, asserts d) | `marcarPagada` → `PARTE_YA_PAGADA` |
| INV-4 · no se elimina contacto con partes | `data/` — motor + traducción a `CONTACTO_EN_USO` en el repositorio (`ContactoService.puedeEliminar`) | `FK ON DELETE RESTRICT` | `GastoCompartidoDaoTest` (T9, assert f) | `ContactoService.puedeEliminar` |
| INV-5 · cobro atómico (estado → ingreso → saldo) | `data/` — repositorio con un solo `runInTransaction` | Aritmética atómica de `saldo_actual`; rollback de Room | `GastoCompartidoDaoTest` (T9, asserts a–c) + integración I-1 (T10) | `marcarPagada` → `PARTE_INEXISTENTE`/`PARTE_YA_PAGADA` |

> **Consecuencia para `test/GastoCompartidoServiceTest.java` (T8):** solo depende de clases de
> `domain/` (`GastoCompartidoService`, `ModoDivision`, `PartePropuesta`, `ErrorCode`,
> `BusinessRuleException`). No importa `android.*` ni `androidx.room.*`, así que se ejecuta en JVM puro
> sin Robolectric. Los tests de INV-3/INV-4/INV-5 se escriben contra el DAO en T9 y el flujo completo en
> T10; no deben pretender probarse con el servicio.

**Operación atómica de referencia: cobrar una parte** (Room `runInTransaction`, orden obligatorio):

```sql
-- paso 1: validar (SELECT) que la parte existe y está 'pendiente'; obtener monto_asignado y cuenta_id
--         (cuenta del egreso original, unida por transaccion_id)

-- paso 2: cambiar estado y registrar fecha de pago
UPDATE GASTOS_COMPARTIDOS
   SET estado = 'pagado', fecha_pago = :ahora_iso
 WHERE id = :parte_id AND estado = 'pendiente';
-- 0 filas afectadas → PARTE_YA_PAGADA / PARTE_INEXISTENTE → rollback

-- paso 3: asiento del cobro (Decisión 2)
INSERT INTO TRANSACCIONES (cuenta_id, categoria_id, monto, tipo, fecha_hora, descripcion,
                           latitud, longitud, direccion_lugar)
VALUES (:cuenta_id, NULL, :monto_asignado, 'ingreso', :ahora_iso,
        'Devolución gasto compartido', NULL, NULL, NULL);

-- paso 4: saldo con aritmética atómica (nunca leer-sumar-escribir)
UPDATE CUENTAS SET saldo_actual = saldo_actual + :monto_asignado WHERE id = :cuenta_id;
```

**Operación de registro del egreso compartido:** extiende el flujo ya existente de
`TransaccionViewModel.registrarTransaccion` (que inserta el egreso y descuenta el saldo): dentro de la
misma transacción, tras el asiento, valida INV-1/INV-2 en `GastoCompartidoService` e inserta las N partes
con `estado='pendiente'` y `fecha_creacion`. Si alguna parte fallara, el egreso tampoco se escribe.

**No-decisiones registradas:** no se añade columna de dirección al registro (ver Decisión 3); no se
implementa liquidación parcial (`monto_pagado` acumulable) — es binario; no se crea vista SQL para el
resumen (se resuelve con una `GROUP BY` en el DAO, siguiendo el criterio de 011 de evitar vistas fuera
de la validación de Room).

---

## 3. Riesgos Identificados

| # | Riesgo | Probabilidad | Impacto | Mitigación | Dueño |
|---|---|---|---|---|---|
| 1 | Romper I-1: saldo desplazado si el ingreso y el UPDATE no caen en la misma transacción | MEDIA | ALTA | Un solo `runInTransaction` (bloque SQL arriba) + test DAO que recalcula Σ movimientos vs saldo | Agente SDD |
| 2 | Desaline silencioso ERD/DDL al sincronizar `data-model.md` | MEDIA | MEDIA | `diff` del bloque Mermaid contra el ERD aprobado; ejecutar el DDL en sqlite3 local y `PRAGMA foreign_key_check` antes de darlo por bueno (regla AGENTS.md) | Agente SDD |
| 3 | Deriva de reparto igual: partes que no cuadran al peso (entero vs residuo) | BAJA | ALTA | Aritmética entera pura + test parametrizado de INV-2 para todos los casos `monto % n ≠ 0` | Agente SDD |
| 4 | Cobro doble concurrente en la misma parte | BAJA | ALTA | `WHERE ... AND estado='pendiente'`: el segundo `UPDATE` afecta 0 filas → error sin escribir nada | Agente SDD |
| 5 | Complejidad del formulario de egreso (checkbox + selector + dos modos) sobre `CuentasActivity` ya denso | MEDIA | MEDIA | El bloque de división vive en layouts propios (`dialog_division_gastos`, `item_parte_gasto`); la UI solo orquesta, las reglas están en el dominio | Agente SDD |
| 6 | Migración 4→5 no aplicada en dispositivos existentes | BAJA | ALTA | `MigrationTestHelper` sembrando v4 y verificando `user_version = 5` y `foreign_key_check` limpio | Agente SDD |

**Criterio de abortar/replantear:** Si la extensión del formulario de `CuentasActivity` exigiera
refactorizar el flujo de registro de transacciones más allá de agregar parámetros, se detiene la
ejecución, se actualiza este `plan.md` con la justificación y se replanea la UI antes de continuar.

---

## 4. Dependencias Externas

| Dependencia | Tipo | Estado del acuerdo | Plan B si falla |
|---|---|---|---|
| Room 2.6.1 (ya en `tech-stack.md`) | Librería | DISPONIBLE | N/A — ninguna dependencia nueva; si hiciera falta alguna, se registra antes en `tech-stack.md` (prohibición §6.4/§6.6) |
| Emulador Android API 36 | Equipo | DISPONIBLE | Pruebas de DAO en JVM con Robolectric; `connectedCheck` se corre al final |

**Requisitos previos del entorno:** Emulador activo para `connectedCheck` y `installDebug`. Ninguna
credencial real en documentos.

---

## 5. Estrategia de Verificación

- CA-1..CA-6 → `GastoCompartidoServiceTest` (JVM puro: reparto igual con y sin residuo, personalizada
  que cuadra y que no, 0 contactos, monto < n, partes ≤ 0, contacto duplicado).
- CA-7..CA-8, CA-11, INV-4, INV-5 → `GastoCompartidoDaoTest` (Robolectric + Room in-memory): el cobro
  deja parte `pagado` + ingreso en `TRANSACCIONES` + saldo incrementado exactamente en `monto_asignado`
  (assert de I-1); cobrar dos veces falla sin efectos; borrar el egreso borra las partes en cascada;
  borrar contacto pendiente lanza RESTRICT → `CONTACTO_EN_USO`; `PRAGMA foreign_keys` encendido;
  `foreign_key_check` limpio al final.
- CA-9, CA-10, CA-12 → test del DAO de resumen (Σ por contacto, orden descendente) + verificación manual
  en emulador.
- Migración → `MigrationTestHelper` v4 → v5.
- Pruebas manuales (emulador): registrar egreso compartido con reparto igual y personalizada; marcar
  pagada una parte y ver el ingreso en el historial y el saldo subir; abrir resumen de deudas; borrar
  contacto con partes pendientes.
- Criterio de "terminado": Definition of Done de `spec.md` §8 (orden: lint → testDebugUnitTest →
  connectedCheck → assembleDebug → `doc/012-division-gastos.md` → sincronizar constitución → commits).
