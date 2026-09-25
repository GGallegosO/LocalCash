# tech-stack.md — Constitución Tecnológica

> **Propósito de este documento:** Registrar las decisiones de stack con su justificación. Es el
> único lugar donde una tecnología se vuelve "oficial" para el proyecto (ver `.agent.md`, regla 6:
> ninguna dependencia sin entrada aquí).
>
> **Regla de oro de agnosticismo:** Mientras cualquier fila contenga `[TECNOLOGIA]` sin completar,
> está PROHIBIDO escribir código dependiente de lenguaje o framework. La selección de stack es una
> tarea con su propio registro en `doc/`.
>
> **Cómo completar cada tabla:** Para cada categoría, lista las candidatas evaluadas (mínimo 2),
> descarta con motivo y elige con justificación. Una elección sin alternativa evaluada no es una
> decisión, es un sesgo.

---

## 1. Lenguajes

| Uso                  | Candidatas evaluadas   | Selección final  | Versión | Justificación                          |
|----------------------|------------------------|------------------|---------|----------------------------------------|
| [LENGUAJE_USO_1]     | [CANDIDATAS_LENGUAJE_1]| [TECNOLOGIA]     | [VERSIÓN] | [JUSTIFICACION_DE_ELECCION]            |
| [LENGUAJE_USO_2]     | [CANDIDATAS_LENGUAJE_2]| [TECNOLOGIA]     | [VERSIÓN] | [JUSTIFICACION_DE_ELECCION]            |

> *Guía:* Los lenguajes se deciden por ecosistema disponible, talento del equipo y requisitos no
> funcionales (rendimiento, concurrencia, portabilidad), no por moda.

---

## 2. Frameworks

| Rol en el sistema        | Candidatas evaluadas      | Selección final | Versión | Justificación               |
|--------------------------|---------------------------|-----------------|---------|-----------------------------|
| [ROL_FRAMEWORK_1]        | [CANDIDATAS_FRAMEWORK_1]  | [TECNOLOGIA]    | [VERSIÓN] | [JUSTIFICACION_DE_ELECCION] |
| [ROL_FRAMEWORK_2]        | [CANDIDATAS_FRAMEWORK_2]  | [TECNOLOGIA]    | [VERSIÓN] | [JUSTIFICACION_DE_ELECCION] |

> *Guía:* Evalúa madurez, curva de aprendizaje, coste de salida (¿qué pasa si hay que migrar?) y
> compatibilidad con el patrón arquitectónico declarado en `.agent.md` sección 5.1.

---

## 3. Base de Datos y Persistencia

| Necesidad de datos     | Candidatas evaluadas     | Selección final | Modelo (relacional/documental/etc.) | Justificación               |
|------------------------|--------------------------|-----------------|-------------------------------------|-----------------------------|
| [NECESIDAD_DATOS_1]    | [CANDIDATAS_BD_1]        | [TECNOLOGIA]    | [MODELO_DE_DATOS]                   | [JUSTIFICACION_DE_ELECCION] |
| [NECESIDAD_DATOS_2]    | [CANDIDATAS_BD_2]        | [TECNOLOGIA]    | [MODELO_DE_DATOS]                   | [JUSTIFICACION_DE_ELECCION] |

**Decisiones de migración de esquema:** [ESTRATEGIA_DE_MIGRACION]
**Respaldo y recuperación:** [POLITICA_DE_BACKUP]
**Datos sensibles y cifrado:** [POLITICA_DE_DATOS_SENSIBLES]

> *Guía:* Empieza por el modelo de datos del dominio; la BD se elige después, no al revés.

---

## 4. Herramientas de Testing

| Nivel de prueba        | Herramienta              | Versión | Qué cubre exactamente                 |
|------------------------|--------------------------|---------|----------------------------------------|
| Unitarias              | [TECNOLOGIA_TEST_UNIT]   | [VERSIÓN] | [ALCANCE_TESTS_UNITARIOS]             |
| Integración            | [TECNOLOGIA_TEST_INT]    | [VERSIÓN] | [ALCANCE_TESTS_INTEGRACION]           |
| Extremo a extremo (E2E)| [TECNOLOGIA_TEST_E2E]    | [VERSIÓN] | [ALCANCE_TESTS_E2E]                   |
| Carga / rendimiento    | [TECNOLOGIA_TEST_CARGA]  | [VERSIÓN] | [ALCANCE_PRUEBAS_DE_CARGA]            |

**Cobertura mínima exigida:** [PORCENTAJE_COBERTURA_MINIMA]% (ver `.agent.md` sección 5.3)
**Comando canónico de tests:** [COMANDO_TEST_DEFINITIVO]

---

## 5. CI/CD

| Etapa                    | Herramienta / Servicio   | Configuración vive en        | Justificación               |
|--------------------------|--------------------------|------------------------------|-----------------------------|
| Integración continua     | [TECNOLOGIA_CI]          | [ARCHIVO_O_RUTA_DE_CONFIG_CI]| [JUSTIFICACION_DE_ELECCION] |
| Despliegue continuo      | [TECNOLOGIA_CD]          | [ARCHIVO_O_RUTA_DE_CONFIG_CD]| [JUSTIFICACION_DE_ELECCION] |
| Calidad de código (lint/seguridad) | [TECNOLOGIA_CALIDAD] | [ARCHIVO_DE_CONFIGURACION] | [JUSTIFICACION_DE_ELECCION] |

**Pipeline mínima obligatoria:** lint → unit tests → integración → build → despliegue a
[AMBIENTE_DE_STAGING] → pruebas E2E → [AMBIENTE_DE_PRODUCCION].

---

## 6. Deploy e Infraestructura

| Aspecto                | Decisión                 | Alternativa considerada | Justificación               |
|------------------------|--------------------------|-------------------------|-----------------------------|
| Plataforma de hosting  | [TECNOLOGIA_DEPLOY]      | [ALTERNATIVA_EVALUADA]   | [JUSTIFICACION_DE_ELECCION] |
| Empaquetado            | [ESTRATEGIA_DE_EMPAQUETADO] | [ALTERNATIVA_EVALUADA] | [JUSTIFICACION_DE_ELECCION] |
| Gestión de configuración/secreto | [MECANISMO_DE_SECRETOS] | [ALTERNATIVA_EVALUADA] | [JUSTIFICACION_DE_ELECCION] |
| Observabilidad (logs/métricas/trazas) | [PLATAFORMA_DE_OBSERVABILIDAD] | [ALTERNATIVA_EVALUADA] | [JUSTIFICACION_DE_ELECCION] |
| Estrategia de versiones | [ESTRATEGIA_DE_VERSIONADO] | N/A                   | [JUSTIFICACION_DE_ELECCION] |

> *Recordatorio de seguridad:* los secretos NUNCA van al repositorio ni a estos documentos; solo se
> referencia el mecanismo de obtención (ver `.agent.md` sección 6, prohibición 1).

---

## 7. Registro de cambios del stack

| Fecha | Cambio | Motivo | Registro en doc/ |
|-------|--------|--------|------------------|
| [FECHA] | [CAMBIO_DE_TECNOLOGIA] | [MOTIVO_DEL_CAMBIO] | [ARCHIVO_DOC_CORRESPONDIENTE] |
