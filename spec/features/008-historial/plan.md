# plan.md — Plan Técnico de Implementación

> **Identificación:** 008-historial | **Fecha:** 18/10/2026 | **Autor:** GG
> **Estado:** En Progreso (parcial) — los componentes marcados ⏳ no están implementados todavía.
>
> **Propósito de este documento:** Definir CÓMO se implementará la feature descrita en `spec.md`:
> componentes, decisiones técnicas, riesgos y dependencias. Es el puente entre el requisito y las
> tareas ejecutables (`tasks.md`).
>
> **Regla:** Ninguna decisión de este archivo puede contradecir `.agent.md` ni `tech-stack.md`.
> Si la contradicción es inevitable, primero se actualiza la constitución con su registro en `doc/`.

---

## 1. Componentes Involucrados

> Estado: ✅ implementado · ⏳ pendiente. Todo archivo nuevo fuera de esta lista obliga a actualizar
> el plan (y justificar) o a detenerse.

| Archivo / Módulo | Rol en la feature | Acción | Complejidad | Estado |
|---|---|---|---|---|
| `res/menu/bottom_nav_menu.xml` | Renombrar la pestaña "Nuevo" → "Movimientos" | MODIFICAR | BAJA | ✅ |
| `res/layout/view_historial.xml` | Lista desplazable + estado vacío + botón flotante | CREAR | BAJA | ✅ |
| `res/layout/item_transaccion.xml` | Fila del historial (icono, concepto, fecha, monto) | CREAR | BAJA | ✅ |
| `ui/cuentas/HistorialAdapter.java` | Presenta la lista de movimientos | CREAR | MEDIA | ✅ |
| `ui/cuentas/CuentasActivity.java` | Orquesta navegación, botón flotante y panel de registro | MODIFICAR | ALTA | ✅ |
| `res/layout/bottom_sheet_transaccion.xml` | Formulario de registro en el panel inferior | CREAR | MEDIA | ✅ |
| `ui/cuentas/TransaccionViewModel.java` | Registro con validación de dominio | CREAR | MEDIA | ✅ |
| `res/values/strings.xml` | Cadenas del título y textos nuevos | MODIFICAR | BAJA | ✅ |
| `data/dao/TransaccionDao.java` | Consulta observable de **todos** los movimientos | MODIFICAR | BAJA | ✅ (método `getAll()` ya existe) |
| `ui/cuentas/HistorialViewModel.java` | Expone el historial observable y lo mapea a dominio | CREAR | MEDIA | ⏳ |
| `data/entities/Transaccion.java` (entidad Room) | Añadir `latitud`, `longitud`, `direccionLugar` | MODIFICAR | MEDIA | ⏳ |
| `domain/model/Transaccion.java` | Propagar las coordenadas en el modelo de dominio | MODIFICAR | MEDIA | ⏳ |
| `domain/model/Ubicacion.java` | Value object de coordenadas (lat, lon, dirección) | CREAR | BAJA | ⏳ |
| `data/db/AppDatabase.java` | Migración `user_version` 2 → 3 (ALTER TABLE, no destructiva) | MODIFICAR | ALTA | ⏳ |
| `AndroidManifest.xml` | Permisos `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | MODIFICAR | BAJA | ⏳ |
| `ui/util/LocationCapture.java` | Captura de coordenadas con el framework de Android | CREAR | MEDIA | ⏳ |
| `ui/cuentas/TransaccionViewModel.java` | Persistir coordenadas al registrar | MODIFICAR | MEDIA | ⏳ |
| `res/layout/bottom_sheet_transaccion.xml` | Mejora visual del control "Geolocalizar" | MODIFICAR | BAJA | ⏳ |
| `test/.../TransaccionDaoGeoTest.java` | Verifica persistencia y regla "ambas o ninguna" | CREAR | MEDIA | ⏳ |
| `androidTest/.../MigracionV2V3Test.java` | Verifica la migración sin pérdida de datos | CREAR | MEDIA | ⏳ |

### Contrato de datos del registro

- **Entrada del formulario:** `tipo` (INGRESO/EGRESO), `monto` (entero, pesos CLP > 0), `cuentaId`,
  `concepto` (sugerencias predefinidas editables), `geolocalizar` (booleano) y, si aplica,
  `latitud`/`longitud`/`direccionLugar`.
- **Salida:** movimiento persistido + saldo de la cuenta actualizado (aritmética atómica).
- **Errores de dominio (códigos ya existentes en `ErrorCode`):** `MONTO_INVALIDO`,
  `CUENTA_NO_ENCONTRADA`, `SALDO_INSUFICIENTE`. La capa de UI los traduce a texto accionable.

---

## 2. Decisiones Técnicas

1. **El formulario vive en un panel inferior (BottomSheet), no en una pestaña.**
   *Justificación:* elimina una pestaña que solo servía para registrar y evita el antipatrón de
   navegar para completar una acción corta. Consecuencia: el contenedor de vistas pasó de 5 a 4
   vistas (Cuentas=0, Movimientos=1, Mapa=2, Contactos=3) y se corrigió el mapeo de la navegación
   inferior (antes Mapa y Contactos estaban cruzados).

2. **"Nueva cuenta" permanece como diálogo (MaterialAlertDialog); no se unifica aún.**
   *Justificación:* mantiene el cambio acotado. La unificación de "Nueva cuenta" y "Nuevo movimiento"
   en un mismo patrón de panel se difiere a una feature posterior (**010**) para no mezclar alcances.

3. **El historial se ordena por fecha descendente.**
   *Justificación:* el movimiento más reciente es el de mayor interés. Se reaprovecha la consulta
   `TransaccionDao.getAll()` (`ORDER BY fecha DESC`), que ya existe.

4. **Geolocalización opcional con dos columnas + una descriptiva.**
   `latitud`/`longitud` se guardan como `REAL` (grados WGS84) y `direccion_lugar` como texto.
   `REAL` **sí** es válido aquí porque no es dinero (`.agent.md` solo prohíbe punto flotante para
   montos). Se respeta el contrato de `data-model.md`: `CHECK` de rango y la regla de tabla
   `(latitud IS NULL) = (longitud IS NULL)` — nunca media ubicación.

5. **Migración no destructiva 2 → 3.**
   Las columnas nuevas se añaden con `ALTER TABLE ... ADD COLUMN` (nullable). Prohibido
   `fallbackToDestructiveMigration()` o `DROP TABLE` (`.agent.md` §6, pérdida de información
   financiera). Se sube `user_version` y se añade el índice parcial `idx_transacciones_geo`.

6. **Captura de ubicación con el framework de Android (`LocationManager`), sin dependencia nueva.**
   *Justificación:* el pilar del proyecto es operar 100% offline y sin dependencias no registradas
   (`.agent.md` §6.6; `tech-stack.md`). `play-services-location` (FusedLocationProvider) no está en
   el stack y ataría la app a Google Play Services. Si en el futuro se requiere, primero se registra
   la candidata en `tech-stack.md`.

7. **El dinero sigue siendo entero (`long`) en pesos CLP.** Sin cambios respecto de la Feature 007;
   `BigDecimal` solo en `domain/`, con la conversión en el único par `toEntity()`/`fromEntity()`.

8. **Los datos de prueba actuales son temporales.**
   `generarTransaccionesMock()` existe solo para validar la UI del historial mientras el cableado a la
   base de datos no está hecho. Debe eliminarse al completar T8-T9 (`tasks.md`).

---

## 3. Riesgos y Mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|
| Migración 2→3 mal escrita → pérdida de datos | Media | Alto | `ALTER TABLE` nullable + test de migración (`MigracionV2V3Test`) con datos sembrados |
| Confusión entre datos de prueba y reales en demo | Alta | Medio | Marcar como deuda técnica verificable; eliminar el mock al cerrar T9 |
| Permiso de ubicación denegado bloquea el registro | Media | Alto | Tratar la ubicación como best-effort: guardar sin coordenadas |
| `LocationManager` devuelve una sola coordenada | Baja | Medio | Aplicar la regla "ambas o ninguna" antes de persistir |
| El saldo no se refresca en la UI tras registrar | Media | Medio | Observar el saldo por `LiveData`; prueba E2E (CA-6) |
| `osmdroid` no registrado / no usado aún | Baja | Bajo | La visualización de mapas es otra feature; aquí solo se persisten columnas |

---

## 4. Dependencias

- **Ya en el stack:** Room (`tech-stack.md`), AndroidX, Material Components, `osmdroid` (para la
  feature de mapas, no para esta).
- **Del framework Android (sin librería externa):** `android.location.LocationManager`, permisos de
  ubicación en runtime.
- **Depende de:** Feature 007 (`007-cuentas`) — cuentas, entidad de movimientos, actualización de saldo.
- **Habilita a:** feature de mapas `NNN-mapa-osmdroid` (Fase 2).
- **No requiere** nuevas dependencias de `build.gradle` distintas de las ya declaradas por 007.

---

## 5. Estrategia de Verificación

| Criterio | Cómo se verifica |
|---|---|
| CA-1, CA-6 | Prueba de integración/UI con datos reales (tras T8) |
| CA-2, CA-3 | Prueba manual en emulador + `connectedCheck` |
| CA-4, CA-5 | Test unitario de `TransaccionService`/`TransaccionViewModel` (códigos de error) |
| CA-7 | Prueba manual con GPS simulado; test del DAO con coordenadas |
| CA-9 | Prueba manual de navegación en tema claro/oscuro |
| Migración | `MigracionV2V3Test` (instrumentado) |

**Orden antes de cerrar (Definition of Done de `tasks.md`):** `./gradlew lint` → `testDebugUnitTest`
→ `connectedCheck` → `assembleDebug` (`.agent.md` §3).

> **Nota de estado:** el estado **parcial actual** (sin geolocalización ni cableado a la base de
> datos) ya pasa `lint`, `testDebugUnitTest` y `assembleDebug`; `connectedCheck` y el test de
> migración quedan para cuando se completen T8-T21.
