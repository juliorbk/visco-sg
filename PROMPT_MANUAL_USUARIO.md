# Prompt para Generar Manual de Usuario con Gamma AI

Copia todo el contenido de abajo y pégalo en Gamma AI para generar el manual de usuario del sistema **OriFlow by Visco Orinoco**.

---

## Instrucciones para Gamma AI

Genera un **manual de usuario profesional** para el sistema **OriFlow by Visco Orinoco**, una plataforma empresarial de gestión de almacenes y compras. El manual debe estar en **español**, ser visualmente atractivo, e incluir las siguientes especificaciones:

### Estilo y Formato
- Tono profesional pero amigable
- Usar los colores corporativos: vino tinto (#73152D) y vino oscuro (#400B18)
- Incluir placeholders para capturas de pantalla con el texto `[INSERTAR CAPTURA: descripción]`
- Cada sección debe comenzar en una nueva página
- Incluir tabla de contenido al inicio
- Formato de documento A4, orientación vertical

### Estructura del Manual

---

## 1. PORTADA
- Título: "Manual de Usuario — OriFlow by Visco Orinoco"
- Subtítulo: "Sistema de Gestión de Almacenes y Compras"
- Versión actual y fecha

---

## 2. INTRODUCCIÓN
### 2.1 ¿Qué es OriFlow?
Plataforma empresarial para la gestión integral de almacenes, compras y proveedores. Cubre el ciclo completo desde la requisición de materiales hasta la recepción y despacho de mercancía, incluyendo órdenes de compra, reportes y administración de usuarios.

### 2.2 Módulos del Sistema
1. Dashboard — Resumen Ejecutivo
2. Inventario — Gestión de Productos
3. Almacenes — Gestión de Almacenes y Movimientos
4. Recepciones — Entrada de Mercancía
5. Despachos — Salida de Mercancía
6. Requisiciones — Solicitudes de Compra
7. Compras — Órdenes de Compra
8. Proveedores — Catálogo de Proveedores
9. Reportes — Reportes y Analíticas
10. Administración — Usuarios, Invitaciones y Empleados

---

## 3. ACCESO AL SISTEMA
### 3.1 Inicio de Sesión
- URL del sistema
- Pantalla de login con dos paneles: formulario (izquierda) y marca con estadísticas (derecha)
- Campos: correo electrónico y contraseña
- Opción "Olvidé mi contraseña" para recuperación

### 3.2 Registro de Usuario
- Solo mediante invitación (token enviado por administrador)
- El token define el correo y rol asignado
- Formulario de registro con datos personales

### 3.3 Cierre de Sesión
- Menú de usuario en barra superior → Cerrar sesión

### 3.4 Roles y Permisos
Explicar los 5 niveles de acceso en orden jerárquico:

| Rol | Nivel | Acceso Principal |
|---|---|---|
| ALMACENISTA (WAREHOUSEMAN) | 1 | Dashboard, Inventario, Almacenes, Recepciones, Despachos |
| COMPRAS (PROCUREMENT) | 2 | Lo anterior + Requisiciones, Compras, Proveedores |
| GERENTE (MANAGER) | 3 | Lo anterior + Reportes |
| ADMINISTRADOR (ADMIN) | 4 | Lo anterior + Administración de usuarios |
| SUPERADMIN | 5 | Todo + Eliminación permanente de usuarios |

---

## 4. NAVEGACIÓN GENERAL
### 4.1 Barra Lateral
- Logo de OriFlow (acceso directo al Dashboard)
- Menú de navegación con 10 secciones filtradas por rol
- Atajo "Nueva Orden de Compra" (visible para roles Compras+)

### 4.2 Barra Superior
- Botón de menú (colapsar/expandir sidebar en móvil)
- Búsqueda global (redirige al inventario)
- Iconos: notificaciones, configuración, ayuda
- Avatar de usuario con menú desplegable (perfil, preferencias, cerrar sesión)

### 4.3 Búsqueda Global
- Campo de búsqueda en barra superior
- Busca productos en el inventario por nombre o código SKU

---

## 5. DASHBOARD — RESUMEN EJECUTIVO
### 5.1 Descripción
Vista general del estado del negocio con indicadores clave.

### 5.2 Tarjetas KPI
- Total de órdenes de compra
- Total de unidades en inventario
- Gasto mensual
- Tasa de cumplimiento de órdenes

### 5.3 Gráficos
- Tendencia de gastos mensuales (barras: real vs proyectado)
- Desglose de gastos por categoría (gráfico de dona)

### 5.4 Tablas
- Órdenes recientes (últimas 6) con estado y proveedor
- Alertas de inventario crítico (productos bajo punto de reorden)
- Alertas de sobrestock (productos excediendo stock máximo)

### 5.5 Acciones Disponibles
- Exportar datos
- Filtrar por período (últimos 30 días)

---

## 6. INVENTARIO — GESTIÓN DE PRODUCTOS
### 6.1 Descripción
Catálogo completo de productos con control de stock.

### 6.2 Tabla de Productos
Columnas: Código Interno, Código SAP, Nombre, Categoría, SKU, Unidad de Medida, Stock, Estado
- Estados visuales: Sin Stock, Bajo Stock, En Stock, Stock Excedido

### 6.3 Filtros y Búsqueda
- Búsqueda por nombre o código SKU (con debounce)
- Filtro por categoría (categorías jerárquicas padre/hijo)
- Ordenar por nivel de stock
- Toggle "Solo productos con stock"

### 6.4 Agregar/Editar Producto
- Modal con formulario: código interno, SAP, nombre, descripción, SKU, categoría, unidad de medida, punto de reorden, stock máximo, proveedor

### 6.5 Panel de Detalle del Producto
- Información completa del producto
- Historial de movimientos
- Acciones de stock (agregar/quitar en almacén específico)
- Botón de edición

### 6.6 Gestor de Categorías
- Crear/editar/eliminar categorías de productos
- Soporte para jerarquía (categorías padre/hijo)

---

## 7. ALMACENES — GESTIÓN DE ALMACENES
### 7.1 Descripción
Administración de almacenes físicos, inventario por almacén y control de movimientos.

### 7.2 Pestañas
#### Pestaña "Almacenes"
- Tarjetas de almacenes mostrando: nombre, código SAP, cantidad de productos, stock pendiente
- Panel de detalle con: dirección, responsable, total de stock/productos
- Modal "Nuevo Almacén": nombre, dirección, código SAP, responsable

#### Pestaña "Inventario"
- Tabla de productos en el almacén seleccionado con stock actual y pendiente

#### Pestaña "Movimientos"
- Historial completo de movimientos (Kardex) con filtros por tipo, producto, almacén y rango de fechas
- Tipos de movimiento: ENTRADA, SALIDA, AJUSTE, TRANSFERENCIA, DESPACHO
- Cada movimiento muestra saldo corriente (running balance)
- Modal de detalle con cantidades antes/después

### 7.3 Transferencia de Stock
- Modal: seleccionar producto, almacén origen, almacén destino, cantidad, costo unitario, motivo

### 7.4 Ajuste de Stock
- Modal: seleccionar producto, almacén, cantidad a ajustar (positiva/negativa), motivo

### 7.5 Gestión de Ubicaciones
- Crear/editar ubicaciones dentro de un almacén
- Búsqueda de ubicaciones activas

---

## 8. RECEPCIONES — ENTRADA DE MERCANCÍA
### 8.1 Descripción
Registro de mercancía recibida contra órdenes de compra.

### 8.2 Tarjetas KPI
- Recepciones del día
- Órdenes pendientes
- Entregas parciales
- Entregas completadas

### 8.3 Tabla de Recepciones
- Búsqueda, paginación
- Estados: PENDIENTE → EN TRÁNSITO → PARCIALMENTE ENTREGADO → ENTREGADO/COMPLETADO

### 8.4 Panel de Detalle de Recepción
- Número de recepción, orden de compra relacionada, proveedor, almacén
- Ítems recibidos vs esperados con diferencias
- Usuario que recibió

### 8.5 Nueva Recepción
- Modal: seleccionar orden de compra, ingresar cantidades recibidas por ítem, notas, almacén, ubicación

---

## 9. DESPACHOS — SALIDA DE MERCANCÍA
### 9.1 Descripción
Registro de salidas de mercancía del almacén.

### 9.2 Tabla de Despachos
- Búsqueda, paginación

### 9.3 Panel de Detalle de Despacho
- Número de despacho, almacén, empleado, centro de costo
- Ítems con cantidades y precios unitarios de salida

### 9.4 Nuevo Despacho
- Modal: seleccionar productos (con stock disponible), cantidades, precios de salida, almacén, empleado (búsqueda por documento), notas

---

## 10. REQUISICIONES — SOLICITUDES DE COMPRA
### 10.1 Descripción
Solicitudes internas de materiales que inician el flujo de compras.

### 10.2 Tabla de Requisiciones
- Filtro por estado (Todas, Pendiente, Esperando Aprobación, Aprobadas, Rechazadas, Convertidas, Canceladas)

### 10.3 Flujo de Estados
BORRADOR → PENDIENTE → ESPERANDO APROBACIÓN → APROBADO → CONVERTIDO
                                      ↘ RECHAZADO
                                      ↘ CANCELADO

### 10.4 Panel de Detalle
- Número de requisición, solicitante, centro de costo, ítems, línea de tiempo
- Acciones: aprobar, rechazar, cancelar, eliminar (solo en borrador)

### 10.5 Nueva Requisición
- Modal: seleccionar productos, cantidades, notas, centro de costo

### 10.6 Convertir a Orden de Compra
- Transforma una requisición aprobada en una orden de compra

---

## 11. COMPRAS — ÓRDENES DE COMPRA
### 11.1 Descripción
Gestión del ciclo completo de órdenes de compra a proveedores.

### 11.2 Tabla de Órdenes
- Ordenadas por fecha (más recientes primero)
- Columnas: N° Orden, Fecha, Proveedor, Estado, Solicitante

### 11.3 Flujo de Estados
PENDIENTE → ESPERANDO APROBACIÓN → APROBADO → EN TRÁNSITO → PARCIALMENTE ENTREGADO → ENTREGADO/COMPLETADO
                                            ↘ RECHAZADO
                                            ↘ CANCELADO
                                            ↘ ESPERANDO PAGO
                                            ↘ RETENIDO EN ADUANA

### 11.4 Panel de Detalle de Orden
- Información completa: proveedor, almacén destino, método de pago, tipo de orden, lead time
- Ítems con cantidades, precios unitarios y subtotales
- Impuestos y envío
- Acciones según estado actual

### 11.5 Crear Orden de Compra
- Modal: seleccionar proveedor (o crear nuevo), almacén destino, método de pago, tipo de orden (SERVICIOS / MATERIALES / MRO / EQUIPO DE CAPITAL)
- Agregar ítems con búsqueda de productos, cantidades y precios unitarios
- Opción de pre-llenar desde requisición aprobada

### 11.6 Acciones sobre Órdenes
- Enviar para aprobación
- Aprobar (con notas opcionales)
- Rechazar (con motivo obligatorio)
- Cancelar
- Recibir mercancía (abre modal de recepción)
- Exportar PDF de la orden

---

## 12. PROVEEDORES — CATÁLOGO DE PROVEEDORES
### 12.1 Descripción
Administración del catálogo de proveedores con seguimiento de desempeño.

### 12.2 Gráfico de Desempeño
- Cumplimiento mensual de proveedores

### 12.3 Tarjetas de Proveedores
- Muestran: nombre, descripción, calificación, categoría, total de órdenes
- Filtro por categoría y búsqueda por nombre

### 12.4 Panel de Detalle
- Información completa: nombre, descripción, dirección, moneda, email, teléfonos, representantes legales, calificación, total de órdenes, categoría

### 12.5 Agregar/Editar Proveedor
- Modal con formulario completo: nombre, email, dirección, teléfonos, moneda, representantes, categoría

### 12.6 Acciones
- Desactivar/Reactivar proveedor (rol Gerente+)
- Eliminar proveedor (solo Superadmin)

### 12.7 Gestor de Categorías de Proveedores
- Crear/editar/activar/desactivar categorías de proveedores

---

## 13. REPORTES Y ANALÍTICAS
### 13.1 Descripción
Generación de reportes bajo demanda y programados.

### 13.2 Pestañas

#### Pestaña "Dashboard de Reportes"
- KPIs: total de reportes, programados, completados, fallidos, pendientes, registros exportados
- Gráficos: tendencia mensual, reportes por tipo, distribución por estado

#### Pestaña "Generar Reporte"
- Formulario: nombre, tipo de reporte (Stock, Movimientos, Alertas Críticas, Análisis de Almacén), formato (PDF, Excel, JSON), rango de fechas, filtros opcionales (categoría, almacén, búsqueda)

#### Pestaña "Historial"
- Tabla de reportes generados con estado (PENDIENTE / PROCESANDO / COMPLETADO / FALLIDO), cantidad de registros, tamaño, descarga

#### Pestaña "Programados"
- Crear/editar reportes programados: frecuencia (DIARIO / SEMANAL / MENSUAL), hora, día de semana/mes, destinatarios de correo, formato, filtros

---

## 14. ADMINISTRACIÓN
### 14.1 Descripción
Gestión de usuarios, invitaciones y empleados del sistema.

### 14.2 Pestañas

#### Pestaña "Usuarios"
- Tabla de usuarios: nombre, email, rol (badge de color), estado activo/inactivo, centro de costo
- Acciones: cambiar rol, activar/desactivar, eliminar permanentemente (Superadmin)
- Eliminación con verificación de referencias

#### Pestaña "Invitaciones"
- Crear invitaciones: email, rol, centro de costo, expiración
- Lista de invitaciones: activas, expiradas, usadas, revocadas
- Acciones: copiar enlace, revocar invitación

#### Pestaña "Empleados"
- Tabla de empleados: nombre completo, documento, teléfono, centro de costo, estado activo
- Acciones: crear, editar, activar/desactivar

### 14.3 Gestión de Áreas y Centros de Costo
- Modal con jerarquía organizacional: Gerencia General → Gerencia → Centro de Costo
- Crear/editar elementos de la jerarquía

---

## 15. FUNCIONALIDADES TRANSVERSALES
### 15.1 Búsqueda Global
- Accesible desde la barra superior en cualquier pantalla
- Busca productos por nombre o código

### 15.2 Exportación de Datos
- Disponible en Dashboard, Inventario, Recepciones y otras pantallas
- Formatos: PDF, Excel

### 15.3 Temas Visuales
- Modo claro y modo oscuro
- Cambio desde el menú de usuario

### 15.4 Notificaciones
- Toast de confirmación para acciones exitosas
- Toast de error para operaciones fallidas

### 15.5 Vencimiento de Sesión
- Redirección automática al login cuando la sesión expira

---

## 16. GLOSARIO DE TÉRMINOS
- **SKU**: Código único de identificación de producto
- **Kardex**: Registro histórico de movimientos de inventario
- **Lead Time**: Tiempo de entrega estimado del proveedor
- **(Eliminado) 3-Way Matching**: Módulo de facturas eliminado del backend
- **Punto de Reorden**: Nivel mínimo de stock que dispara una alerta de reabastecimiento
- **Stock Máximo**: Nivel máximo de stock recomendado para un producto
- **SAP**: Código de referencia del sistema SAP corporativo
- **UoM**: Unidad de Medida (Unidad, Caja, Kilogramo, Litro, Metro, etc.)
- **MRO**: Maintenance, Repair and Operations (Materiales de mantenimiento)
- **Centro de Costo**: Unidad organizacional para imputación de gastos

---

## 17. PREGUNTAS FRECUENTES (FAQ)
1. ¿Cómo recupero mi contraseña? → Usar "Olvidé mi contraseña" en la pantalla de login
2. ¿Cómo solicito acceso al sistema? → Un administrador debe enviarte una invitación por correo
3. ¿Puedo crear una orden de compra sin requisición? → Sí, desde el módulo de Compras
4. ¿Cómo convierto una requisición en orden de compra? → Desde el detalle de la requisición aprobada, usar "Convertir a OC"
5. ¿Qué significan los colores de estado en el inventario? → Rojo: Sin Stock, Amarillo: Bajo Stock, Verde: En Stock, Naranja: Stock Excedido
6. ¿Cómo transfiero stock entre almacenes? → Módulo Almacenes → Pestaña Inventario o Movimientos → Transferir Stock
7. ¿Quién puede aprobar requisiciones y órdenes? → Usuarios con rol Gerente (MANAGER) o superior
8. ¿Cómo exporto un reporte? → Módulo Reportes → pestaña Generar Reporte o Historial → descargar
9. ¿Cómo programo reportes automáticos? → Módulo Reportes → pestaña Programados → Nuevo reporte programado
10. ¿Cómo agrego un nuevo usuario? → Módulo Admin → pestaña Invitaciones → Nueva invitación

---

## Instrucciones Finales para Gamma AI

Genera el manual completo con un diseño profesional usando los colores corporativos (#73152D y #400B18), incluyendo:

1. Tabla de contenido navegable al inicio
2. Cada sección comenzando en nueva página
3. Íconos representativos para cada módulo
4. Diagramas de flujo para los procesos principales (requisiciones, órdenes de compra, recepción)
5. Placeholders `[INSERTAR CAPTURA: descripción de lo que debe mostrar]` en ubicaciones estratégicas
6. Callouts o cajas de "Importante" y "Nota" para información crítica
7. Formato A4, tipografía profesional, espaciado adecuado
8. Pie de página con número de página y nombre del sistema
