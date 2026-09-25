# spec.md — Especificación de Funcionalidad

> **Identificación:** [NNN]-[nombre-de-la-feature]
> **Fecha de creación:** [FECHA] | **Responsable:** [RESPONSABLE]
> **Estado:** Borrador / En revisión / Aprobada
>
> **Propósito de este documento:** Definir QUÉ debe hacer la feature y CÓMO se demuestra que funciona,
> SIN especificar cómo se implementa (eso va en `plan.md`). Si aquí aparece un nombre de framework,
> el documento está fuera de su alcance.
>
> **Instrucciones:** Copia esta carpeta `_TEMPLATE_/` a `spec/features/[NNN]-[nombre-tarea]/`,
> renombra los archivos si es necesario y reemplaza todos los placeholders `[TEXTO_EN_MAYUSCULAS]`.

---

## 1. Descripción Detallada

[DESCRIPCION_DE_LA_FEATURE_EN_UN_PARAFO]

**Comportamiento esperado (narrativa):**
1. El usuario [ACCION_INICIAL_DEL_USUARIO].
2. El sistema [RESPUESTA_O_VALIDACION_DEL_SISTEMA].
3. El resultado observable es [RESULTADO_VISIBLE_PARA_EL_USUARIO].

> *Guía de redacción:* Escribe en tercera persona y tiempo presente ("el sistema valida...", no
> "validaremos..."). Describe comportamiento observable desde fuera, no mecanismos internos.

---

## 2. Contexto de Negocio

- **¿Por qué ahora?** [MOTIVO_DE_PRIORIZACION]
- **¿A qué objetivo de `mission.md` contribuye?** [OBJETIVO_RELACIONADO]
- **¿Qué valor entrega al usuario?** [VALOR_ESPERADO_PARA_EL_USUARIO]
- **Usuarios afectados:** [ROLES_O_PERFIL_DE_USUARIOS_INVOLUCRADOS]
- **Dependencias con otras features:** [FEATURES_RELACIONADAS_O_BLOQUEANTES]

---

## 3. Criterios de Aceptación

Cada criterio debe ser verificable por alguien distinto del implementador. Formato:
`Dado <contexto>, Cuando <acción>, Entonces <resultado observable>` (Gherkin en español).

- [ ] **[CA-1]** Dado [ESTADO_INICIAL_1], cuando [ACCION_1], entonces [RESULTADO_ESPERADO_1].
      *Ejemplo genérico de estilo:* Dado un usuario autenticado con sesión activa, cuando solicita
      exportar sus datos, entonces recibe un archivo con el formato declarado en [FORMATO_ACORDADO].
- [ ] **[CA-2]** Dado [ESTADO_INICIAL_2], cuando [ACCION_2], entonces [RESULTADO_ESPERADO_2].
      *Ejemplo genérico de estilo:* Dado un formulario con un campo obligatorio vacío, cuando el
      usuario intenta enviarlo, entonces el sistema muestra un mensaje de error junto al campo.
- [ ] **[CA-3]** Dado [ESTADO_INICIAL_3], cuando [ACCION_3], entonces [RESULTADO_ESPERADO_3].
      *Ejemplo genérico de estilo:* Dado un servicio externo no disponible, cuando se invoca la
      operación afectada, entonces el sistema degrada según [POLITICA_DE_DEGRADACION_DECLARADA].

**Requisitos no funcionales aplicables:**
- Rendimiento: [REQUISITO_DE_RENDIMIENTO] (p. ej. respuesta bajo [LIMITE_DE_TIEMPO]).
- Seguridad: [REQUISITO_DE_SEGURIDAD] (p. ej. datos cifrados en tránsito y reposo).
- Accesibilidad/compatibilidad: [REQUISITO_DE_ACCESIBILIDAD_O_COMPATIBILIDAD].

---

## 4. Casos Borde a Considerar

Lista exhaustiva de situaciones límite que la implementación DEBE manejar. Cada caso borde sin
solución acordada bloquea la aprobación de esta spec.

| # | Caso borde                                   | Comportamiento esperado                     | ¿Resuelto? |
|---|----------------------------------------------|---------------------------------------------|------------|
| 1 | Entrada vacía o nula donde se requiere dato  | [COMPORTAMIENTO_ANTE_ENTRADA_VACIA]         | [ ]        |
| 2 | Límite superior de volumen/tamaño aceptado   | [COMPORTAMIENTO_EN_LIMITE_SUPERIOR]         | [ ]        |
| 3 | Concurrencia: dos operaciones simultáneas sobre el mismo recurso | [ESTRATEGIA_DE_CONCURRENCIA] | [ ]        |
| 4 | Fallo del proveedor externo o dependencia    | [COMPORTAMIENTO_ANTE_FALLO_EXTERNO]         | [ ]        |
| 5 | Caducidad/expiración (sesión, token, plazo)  | [COMPORTAMIENTO_ANTE_EXPIRACION]            | [ ]        |
| 6 | Caracteres especiales / idiomas / formatos inesperados | [COMPORTAMIENTO_ANTE_DATOS_IMPREVISTOS] | [ ]      |

---

## 5. Fuera de alcance de esta feature

[LISTA_DE_LO_QUE_EXPLICITAMENTE_NO_SE_HACE_AQUI]
> *Guía:* Delimitar el "no" evita discusiones durante la implementación y scope creep. Ejemplo de
> estilo: "La validación de identidad ante proveedores externos queda para la feature [NNN+1]."

---

## 6. Trazabilidad

- Registro de memoria asociado: `doc/[NNN]-[nombre-tarea].md` (obligatorio al cerrar, ver `.agent.md` §7).
- Hito en roadmap: [REFERENCIA_EN_ROADMAP]
