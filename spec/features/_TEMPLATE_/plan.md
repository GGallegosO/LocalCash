# plan.md — Plan Técnico de Implementación

> **Identificación:** [NNN]-[nombre-de-la-feature] | **Fecha:** [FECHA] | **Autor:** [AUTOR]
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

| Archivo / Módulo                        | Rol en la feature                          | Acción (crear/modificar) | Complejidad estimada |
|-----------------------------------------|--------------------------------------------|--------------------------|----------------------|
| [RUTA_DEL_ARCHIVO_1]                    | [ROL_ARQUITECTONICO_1] (p. ej. validación de entrada) | [CREAR/MODIFICAR] | [ALTA/MEDIA/BAJA]    |
| [RUTA_DEL_ARCHIVO_2]                    | [ROL_ARQUITECTONICO_2] (p. ej. lógica de negocio)     | [CREAR/MODIFICAR] | [ALTA/MEDIA/BAJA]    |
| [RUTA_DEL_ARCHIVO_3]                    | [ROL_ARQUITECTONICO_3] (p. ej. persistencia)          | [CREAR/MODIFICAR] | [ALTA/MEDIA/BAJA]    |
| [RUTA_DE_PRUEBAS_1]                     | Cobertura de criterios CA-1..CA-N          | [CREAR/MODIFICAR]        | [ALTA/MEDIA/BAJA]    |

**Contrato de datos / interfaz expuesta:**
- Entrada esperada: [DESCRIPCION_DE_ENTRADA]
- Salida garantizada: [DESCRIPCION_DE_SALIDA]
- Errores posibles (códigos y significado): [LISTA_DE_ERRORES_DEFINIDOS]

---

## 2. Decisiones de Arquitectura

Formato recomendado por decisión: contexto → opciones → elección → justificación → consecuencia.

1. **[DECISION_1_TITULO]** (p. ej. "¿Dónde vive la lógica de validación?")
   - Opciones consideradas: [OPCION_A], [OPCION_B]
   - Decisión: [OPCION_ELEGIDA]
   - Justificación: [POR_QUE_SE_ELIGIO]
   - Consecuencia aceptada: [TRADE_OFF_ASUMIDO]
2. **[DECISION_2_TITULO]**
   - Opciones consideradas: [OPCION_A], [OPCION_B]
   - Decisión: [OPCION_ELEGIDA]
   - Justificación: [POR_QUE_SE_ELIGIO]
   - Consecuencia aceptada: [TRADE_OFF_ASUMIDO]

> *Guía:* Registra también lo que DECIDISTE NO HACER ("no se introduce caché todavía porque
> [MOTIVO]"). Las no-decisiones evitan re-discusiones futuras.

---

## 3. Riesgos Identificados

| # | Riesgo                                        | Probabilidad | Impacto | Mitigación                                       | Dueño       |
|---|-----------------------------------------------|--------------|---------|--------------------------------------------------|-------------|
| 1 | [RIESGO_1] (p. ej. dependencia externa inestable) | [ALTA/MEDIA/BAJA] | [ALTA/MEDIA/BAJA] | [ACCION_DE_MITIGACION_1]              | [RESPONSABLE_1] |
| 2 | [RIESGO_2] (p. ej. subestimación de complejidad)  | [ALTA/MEDIA/BAJA] | [ALTA/MEDIA/BAJA] | [ACCION_DE_MITIGACION_2]              | [RESPONSABLE_2] |
| 3 | [RIESGO_3] (p. ej. impacto en rendimiento existente) | [ALTA/MEDIA/BAJA] | [ALTA/MEDIA/BAJA] | [ACCION_DE_MITIGACION_3]            | [RESPONSABLE_3] |

**Criterio de abortar/replantear:** [CONDICION_DE_PARADA]
> Ejemplo de estilo: "Si la integración con [DEPENDENCIA] supera [LIMITE_DE_ESFUERZO], se escala a
> revisión de diseño antes de continuar."

---

## 4. Dependencias Externas

| Dependencia                        | Tipo (servicio/librería/dato/equipo) | Estado del acuerdo | Plan B si falla        |
|------------------------------------|---------------------------------------|--------------------|------------------------|
| [DEPENDENCIA_EXTERNA_1]            | [TIPO_DE_DEPENDENCIA_1]               | [DISPONIBLE/PENDIENTE/BLOQUEANTE] | [ALTERNATIVA_1] |
| [DEPENDENCIA_EXTERNA_2]            | [TIPO_DE_DEPENDENCIA_2]               | [DISPONIBLE/PENDIENTE/BLOQUEANTE] | [ALTERNATIVA_2] |

**Requisitos previos del entorno:** [CONFIGURACION_O_ACCESOS_NECESARIOS]
> Recuerda: ninguna credencial real en documentos; solo referencias al mecanismo de obtención
> declarado en `tech-stack.md`.

---

## 5. Estrategia de verificación

- Criterios de aceptación → tests: cada CA de `spec.md` mapea a [IDENTIFICADOR_DE_TEST_O_PRUEBA].
- Pruebas manuales necesarias (si aplica): [LISTA_DE_VERIFICACIONES_MANUALES].
- Datos de prueba requeridos: [JUEGO_DE_DATOS_DE_PRUEBA].
- Criterio de "terminado" (Definition of Done): código + tests verdes con los comandos de
  `.agent.md` §3 + documentación actualizada + registro en `doc/[NNN]-[nombre-tarea].md`.
