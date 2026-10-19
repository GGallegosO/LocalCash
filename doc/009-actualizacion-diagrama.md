# doc/009-actualizacion-diagrama.md — Sincronización visual del ERD tras cambio semántico CLP

## Ficha de la tarea

| Campo | Valor |
|---|---|
| **Fecha** | 2026-10-05 |
| **Tarea ejecutada** | Re-renderizado/verificación del diagrama ERD + actualización de referencias en `.agent.md` y `AGENTS.md` |
| **Responsable** | Agente de mantenimiento documental |
| **Estado** | Completado |

## Tarea ejecutada (resumen)

Tarea de mantenimiento documental, no una feature nueva (sin `spec/features/NNN-*/` porque no
cambia comportamiento funcional). El esquema sigue en `user_version` 1, 8 tablas, sin cambios
estructurales. El único cambio previo era semántico: unidad monetaria de centavos a pesos CLP
(Decisión #2 en `spec/features/007-cuentas/plan.md`, `CUENTAS.moneda DEFAULT 'CLP'`).

## Archivos creados / modificados

- **Modificado:** `spec/constitution/data-model.md`
  - §2: placeholder `[INGRESAR_ENLACE]` → `diagrams/erd-localcash.svg` (ruta relativa,
    se mantiene "(opcional, complementaria)" y "el Mermaid manda").
  - §3.8 e I-4: "resto de centavos" → "resto de pesos" (coherencia CLP: no hay centavos
    en circulación desde 1984).
- **Verificado sin cambios:** `spec/constitution/diagrams/erd-localcash.svg` (184 KB, contiene
  las 8 tablas del Mermaid; `mmdc` no disponible en el entorno, y al no haber diff estructural
  se reutiliza el SVG existente según lo autorizado).
- **Verificado sin cambios:** `.agent.md` §6.10 (ya dice pesos CLP con ejemplo `1000000L`) y
  `AGENTS.md` "Reglas del esquema de datos" (ya dice pesos CLP + excepción documentada en
  `doc/007-cuentas.md` y `spec/features/007-cuentas/plan.md`). No se tocó la regla
  "el Mermaid es la fuente de verdad".
- **Creado:** este archivo `doc/009-actualizacion-diagrama.md`.

Nota de numeración: este registro se creó originalmente como `008` porque `007` era la feature
activa por entonces. Al abrirse la Feature 008 (`spec/features/008-historial/`) se **reasignó a
`009`** para respetar la numeración compartida `spec/features/NNN` ↔ `doc/NNN` (`.agent.md`).

## Decisiones tomadas

1. **Reutilizar el SVG existente** en vez de regenerarlo: `mmdc` no está instalado, no hay
   diff estructural que renderizar, y el SVG actual contiene las 8 entidades con sus campos.
   Justificación: el Mermaid es la fuente de verdad; el SVG es solo visualización.
2. **No tocar conteo de tablas ni `user_version`**: no cambiaron.
3. **Corrección "centavos" → "pesos" en I-4/§3.8**: las menciones restantes a "centavos" en
   `data-model.md` (§1, §5.1, §7, Apéndice A) son históricas o explicativas y se conservan;
   solo el reparto equitativo describía una operación actual con la unidad vieja.

## Validación (SDD, trampa de proceso ERD↔DDL)

- Mermaid §2 vs DDL Apéndice A: 8 entidades ↔ 8 `CREATE TABLE`, 9 relaciones ↔ FK con
  `CASCADE`/`RESTRICT`/`SET NULL` + `UNIQUE` en `COMPRAS_GRUPALES.transaccion_id`. Sin fantasmas.
- DDL del Apéndice A ejecutado en `sqlite3` local (3.54.0): esquema creado, 8 tablas,
  `PRAGMA foreign_key_check` limpio.
- Enlace verificado: `spec/constitution/diagrams/erd-localcash.svg` existe y es referenciado
  con ruta relativa desde `data-model.md`.

## Lecciones aprendidas

- La extracción del DDL por regex ingenua capturó bloques SQL de otras secciones
  (`BEGIN IMMEDIATE`, notas de rendimiento) y falló; hay que anclar al marcador
  `-- LocalCash Database Schema v1` → `PRAGMA user_version = 1;`.
- `mmdc` no está en el entorno base; para futuras regeneraciones reales conviene fijar
  la herramienta (p. ej. `npx @mermaid-js/mermaid-cli`) antes de prometer re-render.

## Próximos pasos pendientes

- Nada pendiente de esta tarea. La Feature 007 (`007-cuentas`) consume este esquema como contrato.
