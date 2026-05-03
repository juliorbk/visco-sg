# 📦 Visco Orinoco - Procurement Management System

Un sistema empresarial robusto diseñado para digitalizar y optimizar el flujo de trabajo del departamento de procura. Esta plataforma centraliza la gestión de inventario, el ciclo de vida de las órdenes de compra (PO) y el rendimiento de los proveedores en una única interfaz integral.

## 🚀 Tecnologías Principales (Stack)

*   **Backend:** Java 21 / Spring Boot 3
*   **Base de Datos:** PostgreSQL
*   **Frontend:** React / TypeScript
*   **Arquitectura:** Patrón MVC (Model-View-Controller) / API RESTful

## ✨ Características Principales

*   **📊 Dashboard Analítico:** Visualización en tiempo real de métricas clave (gastos, tasas de cumplimiento, inventario activo).
*   **🏭 Gestión de Inventario:** Control de stock con alertas automáticas de punto de reorden y categorización de materiales industriales.
*   **🛒 Ciclo de Procura (P2P):** Motor de estados para Órdenes de Compra (Borrador -> Pendiente -> Aprobado -> Enviado -> Recibido).
*   **🤝 Módulo de Proveedores:** Base de datos de empresas asociadas, seguimiento de certificaciones (ej. ISO 9001) y calificación de rendimiento.
*   **📑 Reportes:** Generación de informes financieros y operativos exportables (PDF/Excel).

## 📂 Estructura del Proyecto

El sistema está estructurado separando claramente las responsabilidades técnicas:

- `/src/main/java/com/.../controllers`: Puntos de entrada de la API REST.
- `/src/main/java/com/.../services`: Reglas de negocio y transacciones.
- `/src/main/java/com/.../models`: Entidades de base de datos (JPA) y DTOs.
- `/src/main/java/com/.../repositories`: Acceso a datos de PostgreSQL.

## 👨‍💻 Autor
**Julio Suarez** - *Ingeniero Informático*