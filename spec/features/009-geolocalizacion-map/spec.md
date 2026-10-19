# spec.md — Especificación de Funcionalidad

> **Identificación:** 009-geolocalizacion-map
> **Fecha de creación:** 19/10/2026 | **Responsable:** GG
> **Estado:** Borrador (pendiente de aprobación)
>
> **Propósito de este documento:** Definir QUÉ debe hacer la feature y CÓMO se demuestra que funciona,
> SIN especificar cómo se implementa (eso va en `plan.md`). Si aquí aparece un nombre de framework,
> el documento está fuera de su alcance.

---

## 1. Descripción Detallada

La Feature 009 completa la **geolocalización de movimientos** que la Feature 008 dejó preparada: el
interruptor "Geolocalizar" del panel de registro deja de ser solo visual y pasa a abrir una **vista de
mapa** donde el usuario fija un punto. Al confirmar, las coordenadas vuelven al formulario y se guardan
junto con el movimiento. Además, la pestaña "Mapa" deja de ser un texto de relleno y muestra, en **modo
lectura** por ahora, los movimientos que tienen coordenadas.

**Comportamiento esperado (narrativa):**
1. En el panel de registro de un movimiento, el usuario activa el interruptor "Geolocalizar".
2. El sistema abre la vista de mapa centrada, si hay permiso de ubicación, en la posición actual del
   dispositivo; si no hay permiso o no hay señal, la abre en una vista por defecto.
3. El usuario coloca o ajusta un pin sobre el mapa y confirma la selección.
4. El sistema cierra la vista de mapa y deja el movimiento asociado al punto elegido.
5. Al guardar el movimiento, el sistema persiste **ambas** coordenadas; si el usuario no confirmó ningún
   punto, guarda el movimiento sin coordenadas (nunca a medias).
6. Al abrir la pestaña "Mapa", el sistema muestra un pin por cada movimiento geolocalizado.

> *Guía de redacción:* Escribe en tercera persona y tiempo presente ("el sistema valida...", no
> "validaremos..."). Describe comportamiento observable desde fuera, no mecanismos internos.

---

## 2. Contexto de Negocio

- **¿Por qué ahora?** Es el hito "Geolocalización de Movimientos en Mapa" del roadmap (Fase 2,
  prioridad Alta). La Feature 008 dejó el interruptor y el contrato de datos preparados; falta la
  interacción real con el mapa.
- **Objetivo de la misión que atiende:** dar contexto espacial a los gastos, de forma **100% offline** y
  con privacidad absoluta (`mission.md` §1-§2): el mapa y las coordenadas se resuelven en el
  dispositivo, sin servicios de mapas en la nube.
- **Valor / problema que resuelve:** el usuario recuerda **dónde** hizo un gasto, no solo cuándo y
  cuánto; el mapa convierte el historial en un registro con ubicación.
- **Usuarios afectados:** todo usuario autenticado que registre movimientos con geolocalización.
- **Dependencias:**
  - Depende de la Feature 007 (`007-cuentas`): cuentas, entidad de movimientos, actualización de saldo.
  - Depende de la Feature 008 (`008-historial`): panel de registro y el interruptor "Geolocalizar" ya
    presentes en la UI.
  - Habilita a la Feature 010 (`010-lista-compras`): define el contrato visual de pines y de consulta
    geográfica que la lista de compras reutilizará con un **color distinto**.

---

## 3. Criterios de Aceptación

> *Formato:* Dado [contexto] / Cuando [acción] / Entonces [resultado observable].

| ID   | Criterio de Aceptación | Estado |
|------|------------------------|--------|
| CA-1 | **Dado** el panel de registro con el interruptor "Geolocalizar", **cuando** el usuario lo activa, **entonces** el sistema abre la vista de mapa para elegir un punto. | ⏳ Pendiente |
| CA-2 | **Dado** la vista de mapa abierta, **cuando** el usuario coloca o ajusta un pin y confirma, **entonces** vuelve al panel y el movimiento queda asociado a las coordenadas elegidas. | ⏳ Pendiente |
| CA-3 | **Dado** un movimiento con coordenadas confirmadas, **cuando** el usuario guarda, **entonces** el sistema persiste **ambas** coordenadas y el movimiento aparece geolocalizado. | ⏳ Pendiente |
| CA-4 | **Dado** un movimiento sin punto confirmado, **cuando** el usuario guarda, **entonces** el movimiento se guarda sin coordenadas y sin error. | ⏳ Pendiente |
| CA-5 | **Dado** el usuario en la pestaña "Mapa", **cuando** hay movimientos geolocalizados, **entonces** ve un pin por cada uno y puede distinguirlos entre sí. | ⏳ Pendiente |
| CA-6 | **Dado** el permiso de ubicación denegado, **cuando** el usuario activa "Geolocalizar", **entonces** la vista de mapa se abre igualmente (sin centrar en la posición actual) y nunca bloquea el registro. | ⏳ Pendiente |
| CA-7 | **Dado** un movimiento con una sola coordenada disponible, **cuando** el sistema intenta persistir, **entonces** no guarda media ubicación (regla "ambas o ninguna"). | ⏳ Pendiente |
| CA-8 | **Dado** el mapa con movimientos de distinto origen (movimientos y, a futuro, lista de compras), **cuando** se muestran, **entonces** cada origen usa un color de pin distinto. | ⏳ Pendiente |

---

## 4. Requisitos No Funcionales

| Categoría | Requisito | Verificación |
|-----------|-----------|--------------|
| Privacidad | 0 llamadas de red: ni siquiera se declara el permiso `INTERNET`. El mapa usa teselas **preempaquetadas** (Santiago de Chile, zoom 10-14) y funciona desde el primer arranque sin conexión. | Revisión del manifiesto fusionado + prueba en modo avión |
| Compatibilidad | Funciona en API 28+ (Android 9 en adelante). | `minSdk` del `build.gradle` |
| Exactitud geográfica | Las coordenadas se guardan como grados WGS84 con rango válido (lat -90..90, lon -180..180) y nunca a medias. | `PRAGMA`/tests de DAO + tests de dominio |
| Permisos | La ubicación se pide solo cuando el usuario activa "Geolocalizar"; su denegación no bloquea el registro. | Prueba manual |
| Persistencia de datos | La migración del esquema agrega las columnas de coordenadas **sin pérdida** de movimientos existentes. | Test de migración con datos sembrados |
| Rendimiento | La pestaña "Mapa" con decenas de pines se muestra sin bloqueos perceptibles en el hilo principal. | Prueba manual / Macrobenchmark |

---

## 5. Casos Borde

| # | Caso | Comportamiento esperado | Estado |
|---|------|-------------------------|--------|
| E-1 | Permiso de ubicación denegado. | La vista de mapa abre sin centrar en la posición actual; el registro sigue disponible (best-effort). | ⏳ |
| E-2 | Ubicación no disponible (interior, sin señal GPS). | Abrir en vista por defecto; el usuario puede fijar el pin manualmente. | ⏳ |
| E-3 | El usuario abre el mapa pero no confirma ningún punto. | El movimiento se guarda sin coordenadas. | ⏳ |
| E-4 | Solo se obtiene una de las dos coordenadas. | No se persiste media ubicación (regla "ambas o ninguna"). | ⏳ |
| E-5 | Sin conexión a red al abrir el mapa. | El mapa se renderiza con las teselas en caché; si no hay ninguna, muestra un lienzo vacío navegable, sin bloquear. | ⏳ |
| E-6 | Rotación/cambio de tema con la vista de mapa abierta. | La posición del mapa y el pin en edición se conservan. | ⏳ |
| E-7 | Movimiento sin coordenadas en la pestaña "Mapa". | No genera pin; no altera la vista. | ⏳ |
| E-8 | Muchos movimientos geolocalizados (cientos). | El mapa agrupa o limita la consulta por la zona visible para no degradar el _scroll_/zoom. | ⏳ |

---

## 6. Fuera de Alcance

- **Pines de la lista de compras** (agregar/eliminar con confirmación) y su color propio: Feature
  `010-lista-compras`. Aquí solo se fija el **contrato visual** (un color por origen) para que 010 lo
  reutilice.
- **Edición o borrado** de movimientos ya registrados.
- **Rutas, direcciones o geocodificación inversa** (resolver una coordenada a un texto de dirección): el
  campo descriptivo de dirección, si existe, se rellena solo si el dispositivo lo aporta; no se llaman
  servicios externos de geocodificación.
- **Sincronización o respaldo en la nube** de las ubicaciones.

---

## 7. Trazabilidad

- **Registro de memoria:** `doc/009-geolocalizacion-map.md` (se crea al cerrar; ver nota de numeración en
  `plan.md` §7).
- **Hito del roadmap:** Fase 2 — "Geolocalización de Movimientos en Mapa" (Pendiente, Alta).
- **Contrato de datos:** `spec/constitution/data-model.md` — tabla `TRANSACCIONES`, columnas `latitud`,
  `longitud`, `direccion_lugar`, CHECK de rango y regla `(latitud IS NULL) = (longitud IS NULL)`, más el
  índice parcial `idx_transacciones_geo` (§3 y Apéndice A).
- **Tecnología:** `spec/constitution/tech-stack.md` §2 — "Framework de Renderizado Geográfico y Mapas".
- **Deriva a:** `plan.md` (CÓMO) y `tasks.md` (ejecución).
- **Proviene de:** Feature 008 (`008-historial`), que trasladó aquí CA-7 y los casos E-5/E-6/E-7.
