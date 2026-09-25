# mission.md — Misión del Producto

> **Propósito de este documento:** Definir el POR QUÉ existencial del proyecto. Este archivo es la
> constitución de producto: toda decisión de alcance, prioridad o diseño debe poder rastrearse hasta
> una sección de aquí. Si una feature no contribuye a la misión, no se construye.
>
> **Instrucciones:** Responde cada sección con placeholders `[TEXTO_EN_MAYUSCULAS]`. Sé concreto y
> medible; evita adjetivos de marketing ("revolucionario", "líder"). Una misión que no puede
> falsarse no guía decisiones.

---

## 1. Visión del Producto

[VISION_DEL_PRODUCTO_EN_UN_PARAFO]

> *Guía de redacción:* Describe el estado futuro del mundo si el producto tiene éxito total.
> Estructura sugerida: "En [PLAZO_DE_TIEMPO], [ROL_DE_USUARIO] podrá [CAPACIDAD_NUEVA], lo que
> resultará en [IMPACTO_O_BENEFICIO_FINAL]."
>
> Ejemplo genérico (ficticio, no copiar): "En dos años, los pequeños comercios podrán gestionar su
> inventario desde el teléfono sin contratar software especializado, reduciendo pérdidas por stock
> descontrolado."

**Principio rector no negociable:** [PRINCIPIO_RECTOR]
> *Ejemplos de principios:* privacidad por defecto, simplicidad sobre funcionalidad, latencia
> mínima, accesibilidad. Elige UNO: es el criterio de desempate cuando haya conflicto de decisiones.

---

## 2. Problema a Resolver

**Situación actual (dolor):** [DESCRIPCION_DEL_PROBLEMA_ACTUAL]

**Consecuencias del problema (cuánto cuesta):** [COSTO_O_IMPACTO_DEL_PROBLEMA]
> *Guía de redacción:* Cuantifica siempre que puedas: tiempo perdido, dinero, errores, abandono.
> Formato útil: "Hoy, [AFECTADOS] dedican [ESFUERZO] a [TAREA_MANUAL] con una tasa de error de [X]%."

**Por qué las soluciones existentes fallan:** [LIMITACION_DE_ALTERNATIVAS_ACTUALES]

**Evidencia que sustenta el problema:** [FUENTE_DE_EVIDENCIA]
> *Guía:* Encuestas, entrevistas, datos de soporte, métricas internas. Cita la fuente; un problema
> sin evidencia es una suposición.

---

## 3. Objetivos Medibles

Define entre 3 y 5 objetivos. Cada uno debe tener métrica, línea base y meta con fecha.

| # | Objetivo                       | Métrica                     | Línea base actual | Meta          | Fecha límite     |
|---|--------------------------------|-----------------------------|-------------------|---------------|------------------|
| 1 | [OBJETIVO_NEGOCIO_1]           | [METRICA_1]                 | [VALOR_INICIAL_1] | [META_1]      | [FECHA_META_1]   |
| 2 | [OBJETIVO_NEGOCIO_2]           | [METRICA_2]                 | [VALOR_INICIAL_2] | [META_2]      | [FECHA_META_2]   |
| 3 | [OBJETIVO_NEGOCIO_3]           | [METRICA_3]                 | [VALOR_INICIAL_3] | [META_3]      | [FECHA_META_3]   |

> *Guía de redacción:* Usa el patrón "de X a Y para Z fecha". Ejemplo genérico de estilo:
> "Reducir el tiempo de registro de [60] minutos a [10] minutos antes de [FECHA]".

**Fuera de alcance explícito (anti-objetivos):** [FUNCIONALIDADES_QUE_NO_SE_CONSTRUIRAN_Y_POR_QUE]
> *Importante:* Declarar lo que NO se hará previene el crecimiento descontrolado de alcance.

---

## 4. Público Objetivo

**Perfil primario:** [ROL_DEL_USUARIO_PRINCIPAL]
- Contexto de uso: [CONTEXTO_OPERATIVO] (¿dónde, cuándo y cómo usará el sistema?)
- Necesidad central: [NECESIDAD_CENTRAL]
- Nivel técnico esperado: [NIVEL_TECNICO_ESPERADO]

**Perfiles secundarios:** [ROLES_SECUNDARIOS]
> Usuarios que se benefician o se ven afectados pero no son el foco: administradores, integradores,
> espectadores.

**Anti-persona (quién NO es nuestro usuario):** [ANTI_PERSONA]
> *Guía de redacción:* Definir a quién servimos menos ayuda a priorizar: si una feature solo sirve
> a la anti-persona, se descarta o difiere.

---

## 5. Criterios de revisión

- Este documento se revisa [FRECUENCIA_DE_REVISION] o cuando cambie el enfoque del producto.
- Cualquier cambio requiere un registro en `doc/` explicando la motivación (ver `.agent.md`, sección 7).
