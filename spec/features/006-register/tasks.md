# tasks.md — Checklist Secuencial de Ejecución

> **Identificación:** 006-register | **Derivado de:** `spec.md` + `plan.md`
>
> **Propósito de este documento:** Descomponer la implementación en pasos ejecutables, ordenados y
> verificables. Cada casilla marcada `[x]` debe corresponder a un cambio real comprobable (tests
> verdes, archivo creado, comando ejecutado). Marcar sin verificar está prohibido.
>
> **Convenciones:**
> - Ejecuta las tareas EN ORDEN: cada una parte de la anterior completada.
> - Al terminar cada bloque relevante, ejecuta el lint/test con los comandos de `.agent.md` §3
    >   (`./gradlew lint`, `./gradlew testDebugUnitTest`).
> - Si una tarea revela que el plan era incorrecto: DETENTE, actualiza `plan.md` con justificación
    >   y continúa. Nunca implementes "al margen" del plan.
> - **Excepción académica:** Para esta iteración específica, se prioriza la verificación manual exhaustiva sobre tests unitarios automatizados (documentado en `doc/006-register.md`).

---

## Fase 1 — Definición y contratos

- [ ] **T1. Revisar y validar el contrato de datos definido en `plan.md` §1.**
  Verificar que el contrato de entrada (Nombre, Región, Ciudad, Usuario, Contraseña) y salida (datos cifrados en `EncryptedSharedPreferences`) esté completo y sea coherente con `spec.md`.
  *Criterio de salida:* El contrato permite implementar la UI y la capa de datos sin ambigüedades.
  Responsable: GG | Estimación: 15 min

## Fase 2 — Implementación núcleo

- [ ] **T2. Modificar `SecureAuthManager.java` para soportar múltiples usuarios.**
  Agregar métodos para:
  - `registerUser(Context, String username, String password, String name, String region, String city)`: Guarda los datos cifrados con claves dinámicas (ej. `user_<username>_password`).
  - `validateUser(Context, String username, String password)`: Valida credenciales de usuarios registrados.
  - Mantener el método existente para validar el seed "admin/123456" como fallback.
  - Agregar validación para bloquear el registro del usuario "admin" (case-insensitive).
  *Criterio de salida:* La clase compila sin warnings, los métodos son invocables y la lógica de validación funciona para ambos casos (usuario registrado y seed demo).
  Responsable: GG | Estimación: 45 min

- [ ] **T3. Crear `RegisterActivity.java` y `activity_register.xml`.**
  - Crear el layout Material 3 con 5 campos (Nombre, Región, Ciudad, Usuario, Contraseña con toggle de visibilidad) y 2 botones (Crear Cuenta, Volver al Login).
  - Implementar la Activity extendiendo `BaseActivity` para heredar el tema.
  - Agregar validación de campos vacíos antes de invocar `SecureAuthManager.registerUser()`.
  - Manejar el error `USUARIO_RESERVADO` mostrando un mensaje claro al usuario.
  - Al guardar exitosamente, mostrar Toast y navegar de vuelta al Login.
  *Criterio de salida:* La pantalla se renderiza correctamente en modo claro y oscuro, los campos validan entradas, y el flujo completo funciona manualmente.
  Responsable: GG | Estimación: 1 hora

- [ ] **T4. Modificar `LoginActivity.java` para agregar enlace de navegación al Registro.**
  - Agregar un TextView o botón con estilo de enlace debajo del botón "Ingresar con Biometría".
  - Texto: "¿No tienes cuenta? Regístrate aquí"
  - Al hacer clic, navegar a `RegisterActivity`.
  *Criterio de salida:* El enlace es visible, clickeable y navega correctamente sin romper el flujo de login existente.
  Responsable: GG | Estimación: 15 min

- [ ] **T5. Actualizar `AndroidManifest.xml` y `strings.xml`.**
  - Registrar `RegisterActivity` en el manifest con `android:exported="false"`.
  - Agregar todos los strings nuevos con prefijo `aRegister_` (ej. `aRegister_title`, `aRegister_hint_name`, `aRegister_btn_create`, etc.).
  *Criterio de salida:* La app compila sin errores de recursos faltantes, el manifest está correcto.
  Responsable: GG | Estimación: 15 min

## Fase 3 — Integración

- [ ] **T6. Pruebas manuales exhaustivas del flujo completo.**
  Ejecutar las siguientes verificaciones manuales (documentadas en `spec.md` §5):
  1. Navegar desde Login → Registro → Login → CuentasActivity.
  2. Registrar un usuario válido (ej. "gustavo_demo") y verificar que puede iniciar sesión.
  3. Intentar registrar con usuario "admin" y verificar que se rechaza.
  4. Verificar en *Device File Explorer* que los datos están cifrados (ciphertext).
  5. Verificar que la cuenta demo "admin/123456" sigue funcionando.
  6. Probar rotación de pantalla durante el registro (los datos deben preservarse).
  7. Probar el botón "Atrás" del sistema durante el registro (debe volver al Login sin guardar).
  *Criterio de salida:* Todos los escenarios pasan exitosamente, cero crashes.
  Responsable: GG | Estimación: 30 min

- [ ] **T7. Ejecutar lint y build de verificación.**
  Ejecutar en orden:
  1. `./gradlew lint` (cero advertencias en código nuevo)
  2. `./gradlew assembleDebug` (build exitoso)
  *Criterio de salida:* Ambos comandos terminan sin errores.
  Responsable: GG | Estimación: 10 min

## Fase 4 — Cierre y trazabilidad

- [ ] **T8. Crear registro de memoria en `doc/006-register.md`.**
  Documentar:
  - Fecha y resumen de la tarea ejecutada.
  - Archivos creados y modificados.
  - Decisiones tomadas (ej. "Se mantuvo EncryptedSharedPreferences en lugar de migrar a Room").
  - Lecciones aprendidas (qué salió bien, qué se corregiría).
  - Próximos pasos pendientes (Feature 007: Cuentas y Movimientos con Room y osmdroid).
  *Criterio de salida:* Otra persona podría entender el estado del proyecto leyendo solo este documento.
  Responsable: GG | Estimación: 20 min

- [ ] **T9. Commit y push a la rama `feat/006-register`.**
  - Agregar todos los archivos modificados/creados.
  - Crear commit con mensaje Conventional Commits: `feat(register): agrega flujo de registro de usuarios con EncryptedSharedPreferences`
  - Push a la rama remota.
  *Criterio de salida:* Los cambios están respaldados en GitHub.
  Responsable: GG | Estimación: 5 min

---

## Verificación final (Definition of Done)

Los cuatro comandos van en este orden. `connectedCheck` va al final y **requiere dispositivo o
emulador activo**: si se ejecuta primero y falla, se pierde el ciclo de compilación.

- [x] Lint sin advertencias: `./gradlew lint` *(pendiente de ejecutar)*
- [ ] Tests unitarios en verde: `./gradlew testDebugUnitTest` *(excepción académica: se prioriza verificación manual)*
- [ ] Tests de integración y UI en verde: `./gradlew connectedCheck` *(pendiente de ejecutar)*
- [x] Build válido: `./gradlew assembleDebug` *(pendiente de ejecutar)*
- [ ] Registro de memoria creado: `doc/006-register.md` *(pendiente de crear)*
- [ ] `data-model.md` actualizado si la feature tocó el esquema *(NO APLICA: no se modificó el esquema de datos)*
- [ ] Commits con formato de `.agent.md` §5.2, uno por unidad lógica *(pendiente de ejecutar)*

**Notas de ejecución:**
- Fecha de inicio: 05/10/2026
- Excepción documentada: Se prescinde de tests unitarios automatizados para esta iteración académica, priorizando la verificación manual exhaustiva del flujo completo.
- Dependencia crítica: La Feature 005 (Login, Biometría, EncryptedSharedPreferences) debe estar estable y mergeada antes de iniciar esta feature.