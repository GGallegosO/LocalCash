# doc/006-register.md - Registro de Usuarios (Flujo 3 pasos)

## Fecha de ejecución: 05/10/2026 (actualización)

## Resumen de la implementación

Se implementó la **Feature 006: Registro de Usuarios** con un **flujo de 3 pasos** usando ViewFlipper dentro de una sola Activity. Esto mejora la experiencia de usuario al dividir un formulario largo en pantallas más pequeñas y manejables.

## Estructura del flujo

### Paso 1/3: Datos de ubicación
- **Campos:** Región, Ciudad
- **Barra de progreso:** LinearProgressIndicator (1/3)
- **Botones:** [Atrás] → Login | [Continuar] → Paso 2
- **Validaciones:** Región y ciudad no vacíos

### Paso 2/3: Credenciales
- **Campos:** Usuario, Contraseña (con toggle visibilidad integrado en TextInputLayout)
- **Barra de progreso:** LinearProgressIndicator (2/3)
- **Botones:** [Atrás] → Paso 1 | [Continuar] → Paso 3
- **Validaciones:** Campos no vacíos, usuario ≠ "admin", usuario no existente

### Paso 3/3: Confirmación
- **Contenido:** Texto "¿Está seguro que desea registrarse?" + resumen de datos
- **Barra de progreso:** LinearProgressIndicator (3/3)
- **Botones:** [Cancelar] → Login | [Finalizar] → Guarda + CuentasActivity

## Archivos creados/modificados

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `activity_register.xml` | **Reescrito** | Contenedor con ViewFlipper + toggle tema + footer |
| `view_register_step1.xml` | **Creado** | Vista 1: Región, Ciudad, progreso, botones |
| `view_register_step2.xml` | **Creado** | Vista 2: Usuario, Contraseña, progreso, botones |
| `view_register_step3.xml` | **Creado** | Vista 3: Confirmación, resumen, progreso, botones |
| `RegisterActivity.java` | **Reescrito** | ViewBinding + ViewFlipper + onSaveInstanceState + toggle tema |
| `SecureAuthManager.java` | Modificado | Eliminado parámetro `name` de `registerUser()` |
| `strings.xml` | Modificado | +20 strings nuevos para pasos, botones, validaciones |

## Preservación de estado (OBLIGATORIO)

`onSaveInstanceState()` preserva:
1. **Paso actual** del ViewFlipper (`KEY_CURRENT_STEP`)
2. **Datos de campos**: región, ciudad, usuario, contraseña
3. **Toggle de contraseña** (automático via TextInputLayout endIconMode)

Aplica tanto para **rotación de pantalla** como para **recreate() por cambio de tema**.

## Toggle de tema claro/oscuro

- Botón `btnThemeToggle` en top-right (igual que LoginActivity)
- Click → `ThemeManager.setDarkMode()` → `recreate()`
- `onSaveInstanceState` se ejecuta automáticamente antes de `recreate()`, preservando todo el estado

## Decisiones tomadas

1. **1 Activity + ViewFlipper** (no 3 Activities): Comparte datos en memoria sin Serializable/Parcelable, mantiene el estado del tema de forma consistente, reduce boilerplate de manifest e intents.
2. **Campo "Nombre" eliminado**: El nuevo spec lo remueve; el registro solo pide región, ciudad, usuario y contraseña.
3. **Barra de progreso LinearProgressIndicator**: Material 3 nativo, muestra "Paso X de 3" + barra visual.
4. **Botones horizontales con weight**: Atrás (~45%) y Continuar (~55%) para UX clara.
5. **Validación por paso**: Se valida al presionar "Continuar", no al final, para dar feedback temprano.
6. **Toggle contraseña integrado**: Se usa `app:endIconMode="password_toggle"` nativo de TextInputLayout (igual que Login), eliminando la necesidad de icono manual.

## Verificación técnica

- ✅ `./gradlew assembleDebug` - BUILD SUCCESSFUL
- ✅ `./gradlew lint` - BUILD SUCCESSFUL (0 errores)

## Criterios de aceptación (spec.md)

- [x] **CA-1**: Login → enlace "Regístrate" → RegisterActivity (paso 1)
- [x] **CA-2**: Validación campos vacíos en cada paso
- [x] **CA-3**: Bloqueo usuario "admin" (case-insensitive)
- [x] **CA-4**: Registro exitoso → Toast → CuentasActivity
- [ ] **CA-5**: Login con credenciales nuevas → CuentasActivity *(pendiente manual)*
- [x] **CA-6**: Seed "admin/123456" intacto

## Navegación entre vistas

| Vista | Botón Atrás | Botón Continuar/Cancelar/Finalizar |
|-------|-------------|-----------------------------------|
| 1 | finish() → Login | validateStep1() → Paso 2 |
| 2 | Paso 1 | validateStep2() → Paso 3 |
| 3 | Intent → Login | attemptRegister() → CuentasActivity |

## Lecciones aprendidas

- ViewFlipper con `<include>` permite reutilizar layouts sin duplicar estructura.
- ViewBinding genera clases anidadas para `<include>`: `binding.step1.tilRegisterRegion`.
- `LinearProgressIndicator` de Material 3 es preferido sobre `ProgressBar` nativo para el nuevo diseño.
- `onSaveInstanceState` antes de `recreate()` garantiza persistencia de datos en cambios de tema.
- Eliminar un parámetro de un método existente requiere actualizar todas las llamadas (incluye métodos legacy).

## Próximos pasos pendientes

1. **Verificación manual en dispositivo/emulador**:
   - Flujo completo Login → Paso1 → Paso2 → Paso3 → CuentasActivity
   - Rotación en cada paso (datos preservados)
   - Toggle tema en cada paso (datos preservados)
   - Seed admin/123456 funcional
   - Registro de usuario nuevo + login posterior

2. **Feature 007**: Migración a Room/SQLite, geolocalización con osmdroid.

---

**Estado final:** Flujo de 3 pasos implementado, compila sin errores, lint limpio, documentación actualizada.