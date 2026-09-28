# 📱 LocalCash — Mobile Fintech 100% Offline & Private

[![Android](https://img.shields.io/badge/Platform-Android_Native-green.svg?style=flat-square&logo=android)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Language-Java_8%2B-orange.svg?style=flat-square&logo=java)](https://www.oracle.com/java/)
[![SQLite](https://img.shields.io/badge/Database-SQLite-blue.svg?style=flat-square&logo=sqlite)](https://www.sqlite.org/)
[![Methodology](https://img.shields.io/badge/Architecture-Spec--Driven_Development_(SDD)-purple.svg?style=flat-square)](https://github.com/GGallegosO/Template-SSD)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg?style=flat-square)](LICENSE)

**LocalCash** es una aplicación móvil Android nativa desarrollada en **Java** concebida para operar **100% de forma local y offline**. Combina la administración de cuentas financieras personales, el registro georreferenciado de movimientos en mapa, la planificación de compras futuras y un motor avanzado para la división de gastos compartidos y deudas entre contactos.

El proyecto pone un énfasis crítico en la **privacidad absoluta del usuario** (cero comunicación con servidores externos) y en el rigor de ingeniería mediante la metodología **Spec-Driven Development (SDD)**.

---

## 🌟 Características Principales

- 🔐 **Seguridad Biométrica Local:** Acceso protegido mediante la API nativa `BiometricPrompt` (huella dactilar).
- 💳 **Gestión Multicuenta y Transacciones:** Administración de fuentes de dinero (Efectivo, Tarjetas, Ahorros) e historial de ingresos/egresos.
- 📍 **Mapa Espacial Interactivo:** Visualización y filtrado offline de movimientos mediante coordenadas GPS (`latitud`, `longitud`) integradas con `osmdroid`.
- 🗓️ **Planificación de Compras:** Programación de compras e historial con presupuestos estimados.
- 👥 **Gastos Compartidos y División de Deudas:** Motor de cuentas claras para compras grupales con dos modalidades (División equitativa global o desglose itemizado por producto).

---

## 🏗️ Arquitectura y Pilar de Ingeniería (SDD)

El desarrollo del proyecto no improvisa líneas de código: sigue la metodología **Spec-Driven Development (SDD)** guiada por la plantilla [Template-SSD](https://github.com/GGallegosO/Template-SSD).

```text
.
├── .agent.md                      # Contrato maestro de reglas y estándares de código
├── AGENTS.md                      # Manual operativo y gotchas de SQLite para IA/Devs
├── spec/                          # CONTRATOS TÉCNICOS (Qué y Por qué)
│   ├── constitution/              # Misión, Tech Stack, Modelo de Datos y Roadmap
│   │   ├── mission.md
│   │   ├── tech-stack.md
│   │   ├── data-model.md          # ERD y DDL oficial (Single Source of Truth)
│   │   └── roadmap.md
│   └── features/                  # Especificaciones por funcionalidad (spec/plan/tasks)
├── doc/                           # MEMORIA INMUTABLE (Registros de tareas ejecutadas)
└── app/                           # Código Fuente Android (Clean Architecture: UI, Domain, Data)
    └── src/main/java/com/localcash/
```

---

### Decisión de Persistencia y Precisión Financiera

Para garantizar la integridad matemática en los cálculos financieros offline (evitando errores binarios de coma flotante IEEE 754) [1, 13]:
- 💰 **Montos y Saldos:** Se almacenan estrictamente como números enteros (**`INTEGER`**) en la unidad mínima de moneda (centavos) [1].
- 🗺️ **Coordenadas GPS:** Se almacenan como **`REAL`** para filtrado por radio geográfico [1, 7].

---

## 📊 Modelo de Datos (ERD)

El esquema relacional SQLite está compuesto por 8 tablas interconectadas [14, 15]:

```mermaid
erDiagram
    CONTACTOS ||--o{ COMPRAS_PLANIFICADAS : "asociado a"
    CONTACTOS ||--o{ COMPRAS_GRUPALES : "paga o participa"
    CONTACTOS ||--o{ DEUDAS_PARTICIPANTES : "mantiene deuda con"

    CUENTAS ||--o{ TRANSACCIONES : "registra"
    CATEGORIAS ||--o{ TRANSACCIONES : "clasifica"
    TRANSACCIONES ||--o| COMPRAS_GRUPALES : "vincula pago de"

    COMPRAS_GRUPALES ||--|{ ITEMS_COMPRA : "desglosa en"
    COMPRAS_GRUPALES |o--o{ DEUDAS_PARTICIPANTES : "genera"
    ITEMS_COMPRA |o--o{ DEUDAS_PARTICIPANTES : "asigna elemento a"

    CONTACTOS {
        INTEGER id PK
        TEXT nombre
        TEXT telefono
        TEXT email
        TEXT notas
    }

    CUENTAS {
        INTEGER id PK
        TEXT nombre
        TEXT tipo_cuenta
        INTEGER saldo_inicial
        INTEGER saldo_actual
        TEXT moneda
    }

    CATEGORIAS {
        INTEGER id PK
        TEXT nombre
        TEXT icono
        TEXT tipo_transaccion
    }

    TRANSACCIONES {
        INTEGER id PK
        INTEGER cuenta_id FK
        INTEGER categoria_id FK
        INTEGER monto
        TEXT tipo
        TEXT fecha_hora
        TEXT descripcion
        REAL latitud
        REAL longitud
        TEXT direccion_lugar
    }

    COMPRAS_PLANIFICADAS {
        INTEGER id PK
        INTEGER contacto_id FK
        TEXT titulo
        INTEGER monto_estimado
        TEXT fecha_agendada
        TEXT estado
        TEXT notas
    }

    COMPRAS_GRUPALES {
        INTEGER id PK
        INTEGER transaccion_id FK
        INTEGER pagado_por_contacto_id FK
        TEXT titulo
        INTEGER monto_total
        TEXT fecha
        TEXT estado
    }

    ITEMS_COMPRA {
        INTEGER id PK
        INTEGER compra_grupal_id FK
        TEXT descripcion
        INTEGER precio_unitario
        INTEGER cantidad
        INTEGER monto_subtotal
    }

    DEUDAS_PARTICIPANTES {
        INTEGER id PK
        INTEGER compra_grupal_id FK
        INTEGER item_id FK
        INTEGER contacto_id FK
        INTEGER monto_asignado
        INTEGER monto_pagado
        TEXT estado
    }
```

## 🛠️ Stack Tecnológico

| Capa / Componente | Tecnología Seleccionada | Justificación Técnica |
| :--- | :--- | :--- |
| **Lenguaje** | Java 8+ / JDK 11 | Compatibilidad nativa en Android Studio. |
| **Persistencia** | SQLite (`SQLiteOpenHelper` → Room) | Persistencia relacional local de alto rendimiento y cero dependencia de servidores. |
| **Mapas & Geometría** | `osmdroid` 6.x | Renderizado de mapa interactivo 100% offline sin llaves API pagadas. |
| **Seguridad** | `BiometricPrompt` API | Estándar de la plataforma Android para autenticación biométrica local. |
| **UI Components** | Material Design Components | Interfaz limpia, responsiva y adaptable. |
| **Testing** | JUnit, Mockito, Robolectric, Espresso | Cobertura objetivo ≥ 70% en capas de Dominio y DAOs. |

---

## 🚀 Compilación y Ejecución

### Requisitos Previos
- **Android Studio:** Ladybug | 2024.2+ o superior.
- **Android SDK:** v34 (API Level 34).
- **JDK:** Version 11 instalada y configurada.

### Comandos de Desarrollo (Gradle Wrapper)

```bash
# Compilar el proyecto en modo Debug
./gradlew assembleDebug

# Ejecutar Pruebas Unitarias (Robolectric / JUnit)
./gradlew testDebugUnitTest

# Ejecutar Pruebas de Integración y UI (Dispositivo/Emulador conectado)
./gradlew connectedCheck

# Instalar APK de desarrollo en emulador/dispositivo
./gradlew installDebug

# Análisis Estático de Código
./gradlew lint
```

---

## 👤 Autor

**Gustavo Gallegos**  
- **GitHub:** [@GGallegosO](https://github.com/GGallegosO)  
- **Proyecto:** [LocalCash Repository](https://github.com/GGallegosO/LocalCash)
---


