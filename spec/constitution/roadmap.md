# roadmap.md — Hoja de Ruta del Proyecto

> **Propósito de este documento:** Traducir la misión (`mission.md`) en una secuencia temporal de
> hitos verificables. Responde a: ¿en qué orden construimos y qué se entrega en cada fase?
>
> **Convenciones de uso:**
>
> - Cada feature del roadmap debe tener (o prometer) una carpeta en `spec/features/NNN-nombre/`.
> - Estados permitidos: `Pendiente` | `En Progreso` | `Completado` | `Bloqueado` | `Descartado`.
> - Prioridades: `Alta` (bloquea la misión), `Media` (importante, aplazable), `Baja` (deseable).
> - Actualiza esta tabla en cada registro de `doc/` que cierre o mueva un hito. Un roadmap que no
>   se actualiza es decoración, no gestión.

---

## 1. Resumen de fases

| Fase                                                           | Periodo objetivo         | Objetivo de la fase                                                                                                                                            |
| -------------------------------------------------------------- | ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Fase 1: Fundamentos y Seguridad (Entregable 1)                 | 2026-09-25 al 2026-09-28 | Establecer la arquitectura Android en Java, base de datos SQLite local, autenticación biométrica (`BiometricPrompt`) y el CRUD básico de cuentas y contactos.  |
| Fase 2: Mapas e Historial de Compras (Entregable 2)            | 2026-09-29 al 2026-10-05 | Integrar el componente de mapas _offline_ (`osmdroid`) para geolocalizar transacciones, junto con el historial y agendamiento de compras futuras.              |
| Fase 3: Gastos Compartidos y Deudas (Entregable 3)             | 2026-10-06 al 2026-10-12 | Implementar la lógica de desglose de ítems, asignación de deudas grupales entre contactos y balance automático de saldos pendientes[6].                        |
| Fase 4: Estabilización, Documentación y Cierre (Entrega Final) | 2026-10-13 al 2026-10-19 | Ejecución de pruebas unitarias y E2E, optimización de rendimiento, generación del APK/AAB firmado, documentación técnica completa y preparación de la defensa. |

---

## 2. Tabla de hitos

> Las tres primeras filas son un EJEMPLO FICTICIO de cómo rellenar la tabla. Bórralas cuando
> existan hitos reales del proyecto; conserva el formato.

| Feature                                                  | Estado    | Prioridad | Notas                                                                                                                               |
| -------------------------------------------------------- | --------- | --------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| **Autenticación Biométrica (** **BiometricPrompt** **)** | Pendiente | Alta      | Pauta obligatoria de seguridad. Valida la huella dactilar para acceder al sistema y resguardar la base de datos local.              |
| Directorio Local de Contactos                            | Pendiente | Alta      | Módulo 0. Agenda interna para almacenar contactos locales a enlazar en compras y deudas.                                            |
| Gestión de Cuentas y Transacciones (SQLite)              | Pendiente | alta      | Módulo 1. Permite la creación de múltiples cuentas (efectivo, ahorros, tarjetas) y registro de ingresos/egresos en SQLite.usuarios. |
| Geolocalización de Movimientos en Mapa                   | Pendiente | Alta      | Módulo 1. Integración de `osmdroid` para situar marcas geográficas en el mapa al registrar un gasto.                                |
| Historial y Agendamiento de Compras                      | Pendiente | Media     | Módulo 2. Planificación de compras futuras y registro histórico de adquirencia.                                                     |
| Gastos Compartidos y Asignación de Deudas                | Pendiente | Alta      | Módulo 3. Repartición equitativa o personalizada del costo de compras grupales entre contactos de la agenda.                        |
| Pruebas y QA (JUnit / Espresso)                          | Pendiente | Media     | Cobertura de tests unitarios de la lógica de balance de deudas y pruebas de interfaz de usuario.                                    |
| Documentación Técnica y Presentación                     | Pendiente | Alta      | Entregable obligatorio. Guía de compilación, diagrama de base de datos SQLite y pauta para la defensa del proyecto                  |

---

## 3. Reglas de priorización

1. Lo que desbloquea aprendizaje validado va primero (prototipos finos sobre perfección temprana).
2. Ningún hito `En Progreso` sin su `spec.md` y `plan.md` completos en `spec/features/`.
3. Máximo [NUMERO_MAXIMO_DE_FEATURES_SIMULTANEAS] features simultáneas en `En Progreso`.
4. Todo cambio de estado relevante genera entrada en `doc/` (obligatorio según `.agent.md` sección 7).

---

## 4. Deuda técnica y mejoras continuas

| Elemento                                                       | Impacto si se ignora                                                                                                       | Cuándo se atenderá                                                                                   |
| -------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| **Optimización y caché local de mapas (** **osmdroid** **)**   | Retrasos en la carga o falta de fluidez al renderizar marcas de transacciones en zonas completamente sin cobertura de red. | **Fase 2 (Entregable 2 - 5 de Octubre)** durante la integración del módulo geográfico.               |
| Abstracción de SQLite mediante Room ORM                        | Aumento de código repetitivo (_boilerplate_) y riesgo de errores sintácticos en consultas complejas de saldos y deudas.    | **Fase 3 (Entregable 3 - 12 de Octubre)** previo a la implementación del cálculo de deudas grupales. |
| **Cifrado del archivo de respaldo exportable (** **.db** **)** | Vulnerabilidad de privacidad si el usuario exporta su copia de seguridad local y el archivo es extraído del dispositivo.   | **Fase 4 (Entrega Final - 19 de Octubre)** durante la etapa de consolidación de seguridad.           |
| Paginación en la lista de transacciones e historial            | Degradación de memoria y lag en el _scroll_ de la interfaz gráfica al acumular cientos de registros financieros en SQLite. | **Fase 4 (Entrega Final - 19 de Octubre)** en la fase de pruebas de carga y rendimiento.             |

---

## 5. Criterios de cierre de roadmap

Un hito pasa a `Completado` solo cuando:

- [ ] Todos los criterios de aceptación de su `spec.md` están verificados.
- [ ] Los tests asociados pasan en la pipeline oficial (comandos de `.agent.md` sección 3).
- [ ] Existe el registro correspondiente en `doc/NNN-nombre-tarea.md`.
- [ ] La documentación afectada fue actualizada.
