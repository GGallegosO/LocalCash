# spec.md — Especificación de Funcionalidad

> **Identificación:** 008-historial
> **Fecha de creación:** 18/10/2026 | **Responsable:** GG
> **Estado:** Completada — el historial se alimenta de Room, el saldo se actualiza en vivo y el
> registro funciona. La **captura de geolocalización y su visualización en el mapa** se trasladaron a
> la Feature `009-geolocalizacion-map` (ver §6).
>
> **Propósito de este documento:** Definir QUÉ debe hacer la feature y CÓMO se demuestra que funciona,
> SIN especificar cómo se implementa (eso va en `plan.md`). Si aquí aparece un nombre de framework,
> el documento está fuera de su alcance.

---

## 1. Descripción Detallada

La Feature 008 entrega el **historial de movimientos** del Módulo 1: una lista cronológica de los
ingresos y egresos del usuario, y el registro de nuevos movimientos mediante un panel inferior que se
despliega al pulsar un botón flotante. El formulario permite elegir el tipo (ingreso/egreso), el monto,
la cuenta, un concepto y un interruptor "Geolocalizar" (preparado en la UI; su captura real se difiere
a la Feature 009).

**Comportamiento esperado (narrativa):**
1. El usuario entra a la pestaña "Movimientos".
2. El sistema muestra la lista de movimientos ordenada por fecha descendente; si no hay ninguno,
   muestra un estado vacío.
3. El usuario pulsa el botón flotante "+" y el sistema despliega un panel inferior con el formulario
   de registro.
4. El usuario completa el formulario y confirma; el sistema valida las reglas (monto > 0, cuenta
   seleccionada, saldo suficiente si es egreso), guarda el movimiento y actualiza el saldo de la cuenta.
5. El formulario incluye un interruptor "Geolocalizar"; su comportamiento funcional (abrir el mapa,
   fijar un pin y persistir las coordenadas) se implementa en la Feature 009.

**Estado de implementación (resumen):** los pasos 1-4 están operativos con datos reales de Room; la
lista refleja en vivo el movimiento recién guardado (CA-6) y el saldo se actualiza. El paso 5 se
difiere a la Feature 009.

---

## 2. Contexto de Negocio

- **¿Por qué ahora?** Es la segunda capacidad del Módulo 1, después de Cuentas (Feature 007). El
  historial da trazabilidad a cada peso que entra o sale; sin él, el saldo sería un número sin respaldo
  visible.
- **Objetivo de la misión que atiende:** control financiero cotidiano **100% offline** y con privacidad
  absoluta (`mission.md` §1-§2). El historial se lee y escribe íntegramente en el dispositivo.
- **Valor / problema que resuelve:** el usuario ve, sin conexión ni cuentas en la nube, el detalle de
  sus movimientos y registra nuevos gastos o ingresos en el momento en que ocurren.
- **Usuarios afectados:** todo usuario autenticado que ya tenga al menos una cuenta.
- **Dependencias:**
  - Depende de la Feature 007 (`007-cuentas`): cuentas, entidad de movimientos y actualización de saldo.
  - Habilita a la Feature 009 (`009-geolocalizacion-map`): deja preparados el interruptor
    "Geolocalizar" en el panel de registro y el punto de extensión del esquema de coordenadas
    (`data-model.md` §7).

---

## 3. Criterios de Aceptación

> *Formato:* Dado [contexto] / Cuando [acción] / Entonces [resultado observable].

| ID   | Criterio de Aceptación | Estado |
|------|------------------------|--------|
| CA-1 | **Dado** un usuario con movimientos guardados, **cuando** abre "Movimientos", **entonces** ve la lista ordenada por fecha descendente con concepto, fecha, monto y tipo. | ✅ Implementado |
| CA-2 | **Dado** un usuario sin movimientos, **cuando** abre "Movimientos", **entonces** ve un estado vacío (lista oculta, mensaje visible). | ✅ Implementado |
| CA-3 | **Dado** el usuario en "Movimientos", **cuando** pulsa el botón flotante "+", **entonces** se despliega el panel inferior con el formulario (Ingreso/Egreso, Monto, Cuenta, Concepto, Geolocalizar, Guardar). | ✅ Implementado |
| CA-4 | **Dado** un formulario con un egreso cuyo monto supera el saldo, **cuando** confirma, **entonces** el sistema no guarda nada, muestra un mensaje de saldo insuficiente y mantiene el panel abierto. | ✅ Implementado |
| CA-5 | **Dado** un movimiento válido, **cuando** confirma, **entonces** el sistema lo guarda, muestra un mensaje de éxito, limpia y cierra el panel, y el saldo de la cuenta se actualiza. | ✅ Implementado |
| CA-6 | **Dado** el historial visible, **cuando** se registra un movimiento nuevo desde el panel, **entonces** el movimiento aparece en la lista sin recargar la pantalla. | ✅ Implementado |
| CA-7 | **Dado** el control "Geolocalizar" activo, **cuando** el usuario lo activa, **entonces** se abre el mapa para fijar un pin y las coordenadas se asocian al movimiento. | ➡️ Trasladado a la Feature 009 |
| CA-8 | **Dado** el usuario en cualquier pestaña, **cuando** selecciona "Mapa" o "Contactos", **entonces** el sistema muestra la vista correspondiente (índices de navegación correctos). | ✅ Implementado |
| CA-9 | **Dado** el usuario con el tema claro u oscuro, **cuando** navega entre pestañas, **entonces** el estado de la vista se conserva y el título del encabezado corresponde a la pestaña activa. | ✅ Implementado |

---

## 4. Requisitos No Funcionales

| Categoría | Requisito | Verificación |
|-----------|-----------|--------------|
| Rendimiento | Registrar un movimiento y ver reflejado el saldo en menos de 500 ms. | Prueba manual cronometrada / Macrobenchmark |
| Privacidad | 0 llamadas de red: historial y registro operan solo con datos locales. | Revisión de manifiesto y dependencias |
| Compatibilidad | Funciona en API 28+ (Android 9 en adelante). | `minSdk` del `build.gradle` |
| Exactitud monetaria | Los montos se guardan como enteros en pesos CLP; nunca en punto flotante. | Tests de dominio + revisión |
| Permisos | La ubicación se pide solo cuando el usuario activa "Geolocalizar"; su denegación no bloquea el registro. | Prueba manual (Feature 009) |

---

## 5. Casos Borde

| # | Caso | Comportamiento esperado | Estado |
|---|------|-------------------------|--------|
| E-1 | Lista de movimientos vacía. | Mostrar mensaje de estado vacío y ocultar la lista. | ✅ |
| E-2 | Monto igual o menor a cero. | Rechazar con mensaje; no guardar. | ✅ |
| E-3 | Egreso mayor al saldo de la cuenta. | Rechazar con "saldo insuficiente"; no guardar. | ✅ |
| E-4 | Ninguna cuenta seleccionada al guardar. | Rechazar con mensaje; no guardar. | ✅ |
| E-5 | "Geolocalizar" activo pero permiso denegado. | Registrar el movimiento sin coordenadas (o pedir permiso); nunca bloquear. | ➡️ Feature 009 |
| E-6 | Ubicación no disponible (interior, sin señal GPS). | Registrar sin coordenadas tras timeout razonable. | ➡️ Feature 009 |
| E-7 | Solo se captura una de las dos coordenadas. | El sistema no debe guardar media ubicación (ambas o ninguna). | ➡️ Feature 009 |
| E-8 | Rotación/cambio de tema con el panel abierto. | El estado del formulario no se pierde de forma inconsistente. | ⏳ Por verificar |

---

## 6. Fuera de Alcance

- **Captura de geolocalización y visualización en el mapa** (`osmdroid`): se traslada a la Feature
  `009-geolocalizacion-map`. Aquí solo se deja preparado el interruptor "Geolocalizar" en el panel de
  registro. Los criterios CA-7 y los casos E-5/E-6/E-7 pasan a esa feature.
- **Lista de compras y sus pines en el mapa** (color distinto al de los movimientos): Feature
  `010-lista-compras`.
- **Agenda de contactos y división de deudas grupales** (Módulos posteriores).
- **Paginación, filtros o búsqueda** del historial: deuda técnica registrada para una fase futura.
- **Edición o borrado** de movimientos ya registrados.

---

## 7. Trazabilidad

- **Registro de memoria:** `doc/008-historial.md`.
- **Hito del roadmap:** Fase 2 — "Mapas e Historial de Compras" (`roadmap.md` §1). Habilita el hito
  "Geolocalización de Movimientos en Mapa", ahora propiedad de la Feature 009.
- **Contrato de datos:** `spec/constitution/data-model.md` — tabla `TRANSACCIONES`, columnas
  `latitud`, `longitud`, `direccion_lugar` (§3 y Apéndice A); su materialización corresponde a 009.
- **Deriva a:** `plan.md` (CÓMO) y `tasks.md` (ejecución). La geolocalización se continúa en
  `spec/features/009-geolocalizacion-map/`.
