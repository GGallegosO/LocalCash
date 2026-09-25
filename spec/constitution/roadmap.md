# roadmap.md — Hoja de Ruta del Proyecto

> **Propósito de este documento:** Traducir la misión (`mission.md`) en una secuencia temporal de
> hitos verificables. Responde a: ¿en qué orden construimos y qué se entrega en cada fase?
>
> **Convenciones de uso:**
> - Cada feature del roadmap debe tener (o prometer) una carpeta en `spec/features/NNN-nombre/`.
> - Estados permitidos: `Pendiente` | `En Progreso` | `Completado` | `Bloqueado` | `Descartado`.
> - Prioridades: `Alta` (bloquea la misión), `Media` (importante, aplazable), `Baja` (deseable).
> - Actualiza esta tabla en cada registro de `doc/` que cierre o mueva un hito. Un roadmap que no
>   se actualiza es decoración, no gestión.

---

## 1. Resumen de fases

| Fase                 | Periodo objetivo | Objetivo de la fase                    |
|----------------------|------------------|----------------------------------------|
| [FASE_1_NOMBRE]      | [RANGO_DE_FECHAS_1] | [RESULTADO_ESPERADO_FASE_1]         |
| [FASE_2_NOMBRE]      | [RANGO_DE_FECHAS_2] | [RESULTADO_ESPERADO_FASE_2]         |
| [FASE_3_NOMBRE]      | [RANGO_DE_FECHAS_3] | [RESULTADO_ESPERADO_FASE_3]         |

---

## 2. Tabla de hitos

> Las tres primeras filas son un EJEMPLO FICTICIO de cómo rellenar la tabla. Bórralas cuando
> existan hitos reales del proyecto; conserva el formato.

| Feature                                    | Estado      | Prioridad | Notas                                                        |
|--------------------------------------------|-------------|-----------|--------------------------------------------------------------|
| [EJEMPLO] Autenticación de usuarios        | Completado  | Alta      | Ejemplo ficticio: entregada en la fase inicial; incluye registro, acceso y recuperación por correo. |
| [EJEMPLO] Panel de administración básico   | En Progreso | Media     | Ejemplo ficticio: bloqueado parcialmente por definición de roles pendiente (ver spec 002). |
| [EJEMPLO] Reportes exportables             | Pendiente   | Baja      | Ejemplo ficticio: decidido después de validar demanda con primeros usuarios. |
| [FEATURE_REAL_1]                           | [ESTADO]    | [PRIORIDAD] | [NOTAS]          |
| [FEATURE_REAL_2]                           | [ESTADO]    | [PRIORIDAD] | [NOTAS]                                                    |
| [FEATURE_REAL_3]                           | [ESTADO]    | [PRIORIDAD] | [NOTAS]                                                    |

---

## 3. Reglas de priorización

1. Lo que desbloquea aprendizaje validado va primero (prototipos finos sobre perfección temprana).
2. Ningún hito `En Progreso` sin su `spec.md` y `plan.md` completos en `spec/features/`.
3. Máximo [NUMERO_MAXIMO_DE_FEATURES_SIMULTANEAS] features simultáneas en `En Progreso`.
4. Todo cambio de estado relevante genera entrada en `doc/` (obligatorio según `.agent.md` sección 7).

---

## 4. Deuda técnica y mejoras continuas

| Elemento                          | Impacto si se ignora           | Cuándo se atenderá       |
|-----------------------------------|--------------------------------|--------------------------|
| [ELEMENTO_DE_DEUDA_TECNICA_1]     | [IMPACTO_1]                    | [MOMENTO_PLANIFICADO_1]  |
| [ELEMENTO_DE_DEUDA_TECNICA_2]     | [IMPACTO_2]                    | [MOMENTO_PLANIFICADO_2]  |

---

## 5. Criterios de cierre de roadmap

Un hito pasa a `Completado` solo cuando:
- [ ] Todos los criterios de aceptación de su `spec.md` están verificados.
- [ ] Los tests asociados pasan en la pipeline oficial (comandos de `.agent.md` sección 3).
- [ ] Existe el registro correspondiente en `doc/NNN-nombre-tarea.md`.
- [ ] La documentación afectada fue actualizada.
