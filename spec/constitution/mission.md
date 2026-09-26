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

En el mediano plazo, los usuarios de dispositivos Android podrán gestionar sus cuentas personales, geolocalizar sus movimientos y dividir deudas grupales en un entorno totalmente autónomo, lo que resultará en un control financiero cotidiano eficiente y con privacidad absoluta sin depender de internet ni de infraestructura en la nube

> _Guía de redacción:_ Describe el estado futuro del mundo si el producto tiene éxito total.
> Estructura sugerida: "En [PLAZO_DE_TIEMPO], [ROL_DE_USUARIO] podrá [CAPACIDAD_NUEVA], lo que
> resultará en [IMPACTO_O_BENEFICIO_FINAL]."
>
> Ejemplo genérico (ficticio, no copiar): "En dos años, los pequeños comercios podrán gestionar su
> inventario desde el teléfono sin contratar software especializado, reduciendo pérdidas por stock
> descontrolado."

**Principio rector no negociable:** Privacidad y funcionamiento 100% local (_offline-first_ por defecto)

> _Ejemplos de principios:_ privacidad por defecto, simplicidad sobre funcionalidad, latencia
> mínima, accesibilidad. Elige UNO: es el criterio de desempate cuando haya conflicto de decisiones.

---

## 2. Problema a Resolver

**Situación actual (dolor):** Las herramientas financieras móviles convencionales exigen conexión constante a internet y almacenan registros financieros en servidores remotos de terceros, impidiendo la gestión en zonas sin cobertura y exponiendo información confidencial.

**Consecuencias del problema (cuánto cuesta):** Un usuario promedio pierde tiempo realizando cálculos manuales para repartir cuentas compartidas y corre el riesgo de fuga o uso indebido de sus datos personales y patrones de gasto al depender de servidores externos.

> _Guía de redacción:_ Cuantifica siempre que puedas: tiempo perdido, dinero, errores, abandono.
> Formato útil: "Hoy, [AFECTADOS] dedican [ESFUERZO] a [TAREA_MANUAL] con una tasa de error de [X]%."

**Por qué las soluciones existentes fallan:** Exigen la creación de cuentas en la nube, carecen de protección biométrica nativa para bases de datos locales y no integran en una misma aplicación _offline_ la geolocalización de movimientos junto con la división automática de deudas grupales.

**Evidencia que sustenta el problema:** Los requerimientos técnicos y pilares obligatorios del proyecto _LocalCash_, los cuales establecen la necesidad de desarrollo nativo en Java con SQLite local, biometría por huella dactilar y mapas interactivos.

> _Guía:_ Encuestas, entrevistas, datos de soporte, métricas internas. Cita la fuente; un problema
> sin evidencia es una suposición.

---

## 3. Objetivos Medibles

Define entre 3 y 5 objetivos. Cada uno debe tener métrica, línea base y meta con fecha.

| #   | Objetivo                                                | Métrica                                         | Línea base actual | Meta                                       | Fecha límite       |
| --- | ------------------------------------------------------- | ----------------------------------------------- | ----------------- | ------------------------------------------ | ------------------ |
| 1   | Garantizar la operatividad del sistema sin conectividad | % de funciones ejecutables de forma local       | 0%                | 100% de las funciones operativas _offline_ | 19 de Octubre 2026 |
| 2   | Proteger el acceso a la información financiera          | % de inicios de sesión validados por biometría  | 0%                | 100% mediante `BiometricPrompt`            | 19 de Octubre 2026 |
| 3   | Automatizar el desglose y asignación de gastos grupales | Tiempo promedio en calcular deudas por contacto | 5 minutos         | &lt; 30 segundos en el sistema             | 19 de Octubre 2026 |

> _Guía de redacción:_ Usa el patrón "de X a Y para Z fecha". Ejemplo genérico de estilo:
> "Reducir el tiempo de registro de [60] minutos a [10] minutos antes de [FECHA]".

**Fuera de alcance explícito (anti-objetivos):**

- **Sincronización en la nube o servidores remotos:** Se excluyen deliberadamente para garantizar la privacidad y la autonomía local.
- **Pasarelas de pago o transferencias bancarias reales:** La aplicación gestiona saldos, agendas y cálculos de deudas, pero no procesa transacciones monetarias electrónicas.
- **Soporte multiplataforma (iOS/Web):** El alcance se limita de forma exclusiva al desarrollo nativo en Java para Android

> _Importante:_ Declarar lo que NO se hará previene el crecimiento descontrolado de alcance.

---

## 4. Público Objetivo

**Perfil primario:** Usuario de smartphones Android que necesita administrar sus finanzas individuales y grupales de forma privada y sin conexión a internet.

- Contexto de uso: Dispositivo móvil personal en actividades cotidianas (compras, viajes, salidas en grupo) tanto en zonas urbanas como sin cobertura de red.
- Necesidad central: Llevar el control de cuentas, ubicar compras en un mapa y repartir gastos con contactos sin exponer datos personales en la nube.
- Nivel técnico esperado: Básico a intermedio (manejo estándar de aplicaciones móviles Android).

**Perfiles secundarios:** Contactos registrados en la agenda local (familiares, amigos o compañeros) con quienes se dividen productos o servicios comprados en conjunto

> Usuarios que se benefician o se ven afectados pero no son el foco: administradores, integradores,
> espectadores.

**Anti-persona (quién NO es nuestro usuario):** Usuarios que requieren sincronización bancaria automática en tiempo real, transacciones electrónicas directas o acceso multidispositivo desde la nube

> _Guía de redacción:_ Definir a quién servimos menos ayuda a priorizar: si una feature solo sirve
> a la anti-persona, se descarta o difiere.

---

## 5. Criterios de revisión

- Este documento se revisa **al finalizar cada hito del proyecto** o cuando cambie el enfoque del producto.
- Cualquier cambio requiere un registro en `doc/` explicando la motivación (ver `.agent.md`, sección 7).
