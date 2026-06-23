# ANÁLISIS DE BRECHAS - VISCO ORINOCO ERP

> **Fecha**: 19/05/2026
> **Alcance**: Backend completo (Spring Boot 4.0.6, Java 21, PostgreSQL)
> **Archivos analizados**: 105 archivos Java (18 entidades, 40 DTOs, 15 servicios, 13 controladores, 12 repositorios, 4 tests)

---

## 1. BRECHAS CRÍTICAS (Bloquean funcionalidad)

### 1.1 Invoice 3-way matching incompleto
- **NOTA**: El módulo de facturas (Invoice) fue eliminado del backend. Esta brecha ya no aplica.

### 1.2 GoodReceipt no registra quién recibió (GoodReceipt.java)
- **Problema**: `GoodReceipt` no tiene campo `receivedBy` (usuario que recibió la mercancía). El `createdBy` se obtiene de `order.getCreatedBy()`, que es quien CREÓ la orden, no quien recibió.
- **Impacto**: No hay trazabilidad de quién recibió físicamente la mercancía.
- **Solución**: Agregar `@ManyToOne User receivedBy` a `GoodReceipt` y recibirlo como parámetro en `receiveGoods()`.

### 1.3 Cancelación de PO no revierte pending stock completamente (ProcurementService.java:223-254)
- **Problema**: `cancelOrderById()` resta pending stock, pero solo si `order.getDestinationWarehouse() != null` (siempre lo es porque es `nullable=false`). Sin embargo, NO revierte el pending stock si la orden ya estaba parcialmente recibida (status `PARTIALLY_DELIVERED`).
- **Impacto**: Si una orden parcialmente recibida se cancela, el pending stock no se limpia correctamente.
- **Solución**: La lógica debería restar `pendingStock` solo por la cantidad NO recibida: `cantidadPendiente = item.quantity - totalRecibido`.

### 1.4 No hay entidad de Auditoría / Audit Log
- **Problema**: No existe una tabla `audit_log` para registrar cambios de estado, creación/eliminación de entidades. Solo `Supplier` usa `@CreatedDate`/`@LastModifiedDate`. El resto usa `@PrePersist` manual.
- **Impacto**: Sin trazabilidad de quién cambió qué y cuándo. Auditoría forense imposible.
- **Solución**: Implementar `AuditLog` entity + Spring Data Envers o listener personalizado.

### 1.5 backup.sh vacío (scripts/backup.sh)
- **Problema**: El script de backup existe en dos ubicaciones pero está vacío (directorio sin contenido real).
- **Impacto**: No hay backup automatizado funcional en producción.
- **Solución**: Implementar script de backup de PostgreSQL con rotación.

---

## 2. BRECHAS DE SEGURIDAD (Alto riesgo)

### 2.1 `/api/cost-centers/**` es público (SecurityConfig.java:69)
- **Problema**: La ruta `/api/cost-centers/**` está en la lista `permitAll()` (pública). Expone la estructura financiera completa de la organización (centros de costo, divisiones, gerencias) sin autenticación.
- **Impacto**: Cualquier persona sin autenticar puede ver la estructura organizacional completa.
- **Solución**: Mover a solo ADMIN/MANAGER o al menos requerir autenticación.

### 2.2 Sin rate limiting en `/api/cost-centers/**` (RateLimitFilter.java)
- **Problema**: `RateLimitFilter` no tiene entrada para `/api/cost-centers/`, pero como la ruta es pública, podría ser abusada.
- **Solución**: Agregar `COST_CENTERS("/api/cost-centers", 30, 1)` al enum.

### 2.3 AuthController.register devuelve token en body (AuthController.java:36-40)
- **Problema**: El endpoint `/api/auth/register` devuelve el JWT en el body de la respuesta (como `register` no crea cookie), mientras que `/api/auth/login` lo borra del body y lo pone en cookie HttpOnly.
- **Impacto**: Inconsistencia de seguridad. El registro expone el token en texto plano en la respuesta.
- **Solución**: Unificar comportamiento: ambos endpoints deben devolver token solo en cookie HttpOnly.

### 2.4 Sin refresh token mechanism
- **Problema**: `AuthService.refreshToken()` existe (genera nuevo token) pero no hay endpoint HTTP expuesto para refrescar. Tampoco hay refresh token persistido ni rotación.
- **Impacto**: Cuando el token expire (24h por defecto), el usuario debe volver a hacer login.
- **Solución**: Implementar `/api/auth/refresh` con un refresh token persistido en DB.

### 2.5 CORS hardcoded (SecurityConfig.java:38-44)
- **Problema**: Las origins permitidas están hardcodeadas: `localhost:3000`, `localhost:5173`, `192.168.88.38:3000`.
- **Impacto**: Para cada nuevo entorno (staging, producción) hay que recompilar. No se puede configurar vía env var.
- **Solución**: Mover a `app.cors.allowed-origins` en application.properties.

### 2.6 Sin bloqueo por intentos fallidos de login
- **Problema**: No hay mecanismo que bloquee cuentas después de N intentos fallidos. El rate limiter global (8 req/min) no distingue usuario específico.
- **Impacto**: Ataque de fuerza bruta contra contraseñas de usuarios conocidos.
- **Solución**: Implementar contador de intentos fallidos por usuario en DB/tabla de login_attempts.

---

## 3. BRECHAS DE RENDIMIENTO (Afectan escala)

### 3.1 StockLevelRepository N+1 en getGlobalStockSummary (WarehouseService.java:428-446)
- **Problema**: Aunque `getGlobalStockByWarehouse()` usa una proyección eficiente, otras consultas como `findByProductId()` en `getTotalPendingStock` (ProductService.java:97-105) cargan entidades completas sin paginación.
- **Impacto**: Con 10k+ productos, carga innecesaria de todas las entidades StockLevel en memoria.
- **Solución**: Usar `@Query` con SUM directo como ya se hace en `getTotalStockByProductId`.

### 3.2 getMovements sin paginación (WarehouseService.java:582-631)
- **Problema**: `getMovements()` retorna `List<InventoryMovement>` sin paginación. Usa `findMovementsWithFilters` que no tiene `Pageable`.
- **Impacto**: Con millones de movimientos (kardex a 5 años), esta consulta puede saturar memoria y red.
- **Solución**: Agregar `Pageable` como parámetro y retornar `Page<InventoryMovementResponse>`.

### 3.3 StockLevelRepository no tiene @Lock ni versioning optimista
- **Problema**: `addPendingStock`, `substractCurrentStock`, etc. no usan `@Version` ni `@Lock(PESSIMISTIC_WRITE)`. En concurrencia, dos requests simultáneos pueden leer el mismo stock y sobrescribirse.
- **Impacto**: Race conditions en ajustes de stock concurrentes (pérdida de actualizaciones).
- **Solución**: Agregar `@Version` a StockLevel y manejar `OptimisticLockException`.

### 3.4 Dashboard cache sin invalidation explícita (StatsService.java:38, 94, 168)
- **Problema**: `@Cacheable(value = "dashboard", key = "...")` usa caché simple sin TTL ni invalidación programática. Los datos quedan stale hasta reinicio.
- **Impacto**: Dashboard muestra datos desactualizados hasta que el caché se limpie manualmente.
- **Solución**: Configurar TTL global en application.properties (`spring.cache.cache-name.specs`) o agregar `@CacheEvict` en servicios que modifican datos (crear PO, recibir mercancía, etc.).

### 3.5 Invoice e InvoiceItem sin índices
- **NOTA**: Módulo de facturas eliminado. Esta brecha ya no aplica.

---

## 4. BRECHAS DE DATOS (Inconsistencias)

### 4.1 PurchaseOrderResponse no expone campos de negocio faltantes
- **Problema**: `CreatePurchaseOrderRequest` y `PurchaseOrderResponse` no incluyen:
  - `expectedDeliveryDate` (fecha de entrega esperada) → Crítica para logística
  - `paymentTerms` (términos de pago: Net30, Net60, etc.)
  - `incoterms` (FOB, CIF, EXW, DDP)
  - `insurance` (costo de seguro)
  - `specialConditions` (condiciones especiales)
- **Entidad**: `PurchaseOrder.java` tampoco tiene estos campos.
- **Impacto**: El sistema no puede modelar contratos de compra reales del sector oil & gas.
- **Solución**: Agregar campos a `PurchaseOrder` entity y exponer en DTOs.

### 4.2 GoodReceiptResponse no incluye quién recibió ni firma
- **Problema**: `GoodReceiptResponse` no expone `receivedBy` (quién recibió), ni `signature`, ni `photos`.
- **Impacto**: Sin evidencia de recepción, disputas con proveedores no se pueden resolver.
- **Solución**: Agregar campos a `GoodReceipt` entity y DTO.

### 4.3 Inconsistencia tipos: PurchaseOrderItem.quantity es int, StockLevel usa BigDecimal
- **Problema**: `PurchaseOrderItem.quantity` es `Integer` mientras que `StockLevel.currentStock` y `StockLevel.pendingStock` son `BigDecimal`. Las operaciones de stock convierten `int` a `BigDecimal` (p.ej. `BigDecimal.valueOf(itemReq.quantity())`).
- **Impacto**: Productos fraccionables (por peso, volumen) pierden precisión. No se pueden ordenar 1.5 kg.
- **Solución**: Cambiar `PurchaseOrderItem.quantity` a `BigDecimal`.

### 4.4 DeleteSupplier contradictorio (SupplierService.java:115-127)
- **Problema**: `deleteSupplier()` solo permite eliminar si el supplier está INACTIVO (`Boolean.TRUE.equals(supplier.getActive())` lanza excepción → solo deja eliminar si está false). Pero `deactivateSupplier()` ya lo desactiva. La función `deleteSupplier` es prácticamente inutilizable porque primero hay que desactivar (soft delete via @SQLDelete) y luego llamar delete que falla porque el supplier ya no está activo.
- **Impacto**: Método `deleteSupplier` no funcionará nunca porque @SQLDelete hace soft delete (UPDATE) y marca is_active=false, y luego el método verifica `if active==true` y lanza error.
- **Solución**: Eliminar método `deleteSupplier` o cambiar lógica a `if (hasActiveOrders) throw...`.

### 4.5 PurchaseOrder.deletedAt sin @Column explícita (PurchaseOrder.java)
- **Problema**: El campo `deletedAt` no tiene anotación `@Column`, por lo que JPA usará el nombre del campo (`deletedAt` → `deleted_at`). Pero no está en el esquema esperado ni hay @SQLDelete para PO.
- **Impacto**: El campo puede no crearse en la tabla si DDL es `update` y no hay columna `deleted_at`.
- **Solución**: Agregar `@Column(name = "deleted_at")`.

### 4.6 Invoice no tiene @Index en purchase_order_id
- **NOTA**: Módulo de facturas eliminado. Esta brecha ya no aplica.

---

## 5. BRECHAS DE FUNCIONALIDAD (Completitud)

### 5.1 No existe endpoint para refrescar token
- **Endpoint faltante**: No hay `POST /api/auth/refresh`.
- **Servicio**: `AuthService.refreshToken()` existe pero no se usa desde ningún controller.
- **Solución**: Agregar endpoint que reciba el cookie actual y devuelva nuevo token.

### 5.2 No hay workflow de aprobación con escalamiento
- **Problema**: La aprobación de POs y requisiciones es de un solo nivel (cualquier ADMIN/MANAGER puede aprobar). No hay escalamiento por monto, ni aprobación múltiple.
- **Impacto**: Una orden de $1M puede ser aprobada por un MANAGER sin revisión adicional.
- **Solución**: Implementar reglas de aprobación por monto (ej: >$50k requiere aprobación de MANAGER, >$200k requiere ADMIN).

### 5.3 No hay devolución de mercancía (Returns)
- **Problema**: No existe entidad `Return` o `DebitNote`. Si la mercancía recibida está dañada o no cumple especificaciones, no hay proceso de devolución.
- **Impacto**: Proceso de negocio incompleto. Las devoluciones se manejarían fuera del sistema.
- **Solución**: Agregar entidad `ReturnOrder` + servicio + endpoints.

### 5.4 No hay edición de Purchase Orders
- **Problema**: Una vez creada la PO, solo se pueden hacer transiciones de estado (submit, approve, reject, cancel, send). No se puede modificar items, cantidades, precios.
- **Impacto**: Para cambiar 1 item hay que cancelar y recrear toda la orden.
- **Solución**: Permitir edición en estado PENDING (antes de submit).

### 5.5 No hay alertas de stock bajo (solo dashboard)
- **Problema**: `StatsService.getCriticalInventory()` lista productos bajo reorder point, pero no hay un mecanismo de notificación programada (cron) que envíe alertas por email de stock bajo.
- **Impacto**: Nadie revisa el dashboard constantemente; productos pueden llegar a stock cero sin aviso.
- **Solución**: Agregar `@Scheduled` en WeeklyReportService o servicio dedicado que verifique stock crítico diariamente y envíe alertas.

### 5.6 No hay valoración de inventario (FIFO/LIFO/Promedio)
- **Problema**: `InventoryMovement` tiene `entryUnitPrice` y `exitUnitPrice`, pero no hay lógica de costeo (FIFO, LIFO, weighted average).
- **Solución**: Implementar servicio de costeo de inventario que calcule el costo de salida según método configurable.

### 5.7 No hay límite de capacidad por almacén
- **Problema**: `Warehouse` no tiene `maxCapacity` o `currentUtilization`. Se puede recibir mercancía sin verificar si el almacén tiene espacio.
- **Solución**: Agregar campo `capacity` a Warehouse y validar al recibir goods.

---

## 6. BRECHAS DE TESTING (Cobertura)

### 6.1 Sólo 4 controllers tienen tests (de 13 totales)
| Controller | Tiene Test? |
|---|---|
| AuthController | ✅ |
| DashboardController | ✅ |
| SuppliersController | ✅ |
| WarehouseController | ✅ |
| **InvoiceController** | ❌ (Eliminado) |
| **ProcurementController** | ❌ |
| **ProductController** | ❌ |
| **RequisitionController** | ❌ |
| **AdminController** | ❌ |
| **CostCenterController** | ❌ |
| **CategoryController** | ❌ |
| **UserController** | ❌ |
| **ProductMigrationController** | ❌ |

### 6.2 Tests saltan seguridad completamente
- **Problema**: Todos los tests usan `@AutoConfigureMockMvc(addFilters = false)`, lo que significa que ningún test verifica autorización, autenticación, o rate limiting.
- **Impacto**: Los tests no detectan regresiones de seguridad.
- **Solución**: Agregar tests con `addFilters = true` para probar rutas protegidas/no protegidas.

### 6.3 No hay tests de servicio ni repositorio
- **Problema**: Solo hay tests de controlador (`@WebMvcTest`). No hay tests unitarios de servicios ni tests de integración de repositorios (`@DataJpaTest`).
- **Impacto**: La lógica de negocio (3-way matching, transiciones de estado, cálculos de stock) no está testeada.
- **Solución**: Agregar tests unitarios de servicios críticos (ProcurementService, WarehouseService) y tests de integración de repositorios.

### 6.4 Cobertura de casos de error insuficiente
- **Problema**: Los tests existentes prueban principalmente el "happy path". Hay pocos tests de casos de error (404, 409, 400, 401, 403).
- **Solución**: Agregar tests para: validaciones fallidas, estados inválidos, duplicados, soft-delete.

---

## 7. BRECHAS DE DOCUMENTACIÓN

### 7.1 API Documentation incompleta
- **Problema**: Swagger/OpenAPI está configurado pero solo `AuthController` y `DashboardController` tienen anotaciones `@Operation` y `@Tag`. El resto de controladores no tiene descripciones de endpoints.
- **Solución**: Agregar `@Operation` y `@ApiResponse` a todos los endpoints.

### 7.2 Diagrama ER no existe
- **Problema**: No hay archivo de diagrama entidad-relación en el repositorio.
- **Solución**: Generar diagrama ER (usando JPA Buddy o similar) y mantenerlo en `docs/`.

### 7.3 backup.sh vacío
- **Problema**: Dos ubicaciones (`src/main/java/com/visco/backend/scripts/backup.sh` y `scripts/backup.sh`), ambas vacías o directorios sin contenido.
- **Solución**: Implementar script funcional de backup PostgreSQL con `pg_dump`.

### 7.4 README.md no verificado
- **Problema**: README.md existe pero no se ha analizado su contenido.

---

## 8. BRECHAS ADICIONALES (Mantenibilidad y Calidad)

### 8.1 WarehouseService.getMovements cálculo de running balance incorrecto
- **Problema**: El running balance en `getMovements()` (WarehouseService.java:599-627) simplemente suma `m.getQuantity()` a `runningBalance[0]`. Pero para OUTPUT y ADJUSTMENT, la cantidad debería restarse (no sumarse), pues `quantity` siempre es positiva en el movimiento.
- **Solución**: `runningBalance[0] = runningBalance[0].add(m.getType() == MovementType.OUTPUT ? m.getQuantity().negate() : m.getQuantity())`.

### 8.2 GoodReceipt.notes tiene @Column(length = 1000) (brecha de consistencia ya no aplica, Invoice eliminado)
- No es crítico pero sugiere falta de estandarización.

### 8.3 RateLimitFilter no diferencia métodos HTTP
- **Problema**: La misma ruta (ej: `/api/procurement/orders`) tiene el mismo límite para GET (lectura, debería ser alto) que para POST (creación, debería ser bajo).
- **Solución**: Incluir método HTTP en la clave del bucket o separar rutas por método.

### 8.4 JwtAuthFilter no establece SecurityContext para rutas públicas
- **Problema**: Cuando el token es inválido/expirado, el filtro registra warning pero continúa la cadena. Para rutas públicas está bien, pero si alguien envía un token inválido a una ruta autenticada, el error se captura y se continúa como no autenticado en lugar de retornar 401.
- **Solución**: En `ExpiredJwtException` y `JwtException`, retornar 401 directamente.

### 8.5 EmailService.sendWelcomeEmail traga excepciones silenciosamente
- **Problema**: Si el mail server no está configurado, el error se loggea pero el registro del usuario continúa exitosamente. El usuario queda sin notificación pero no lo sabe.
- **Solución**: Lanzar excepción (o al menos devolver indicación al frontend).

---

## 9. RECOMENDACIONES PRIORITARIAS

### SPRINT 1 (Quick Wins - 1-2 días)
1. ✅ (Eliminado) Agregar `@Index` a `Invoice` y `InvoiceItem`
2. ✅ Agregar `@Column(name = "deleted_at")` a `PurchaseOrder.deletedAt`
3. ✅ Corregir running balance en `WarehouseService.getMovements()` (restar en OUTPUT)
4. ✅ Mover CORS origins a application.properties
5. ✅ Refactorizar `deleteSupplier()` (eliminar método redundante)

### SPRINT 2 (Seguridad - 3-5 días)
1. 🔴 Proteger `/api/cost-centers/**` (quitar de permitAll)
2. 🔴 Unificar manejo de tokens (siempre cookie HttpOnly, nunca en body)
3. 🔴 Implementar refresh token endpoint
4. 🔴 Agregar rate limiting a `/api/cost-centers/`
5. 🔴 Implementar bloqueo de cuentas por intentos fallidos

### SPRINT 3 (Funcionalidad Crítica - 1 semana)
1. 🔴 (Eliminado) Corregir 3-way matching en InvoiceService
2. 🔴 Agregar `receivedBy` a GoodReceipt
3. 🔴 Corregir cancelación de PO (revertir pending stock correctamente)
4. 🔴 Agregar `expectedDeliveryDate`, `paymentTerms`, `incoterms` a PO

### SPRINT 4 (Rendimiento y Concurrencia - 1 semana)
1. ⚠️ Implementar `@Version` en `StockLevel` para optimistic locking
2. ⚠️ Agregar paginación a `getMovements()`
3. ⚠️ Configurar TTL de caché dashboard
4. ⚠️ Crear índices faltantes en tablas User, Warehouse (Invoice eliminado)

### SPRINT 5 (Testing - 1-2 semanas)
1. ✅ Tests para controllers faltantes (Procurement, Product, Requisition) — Invoice eliminado
2. ✅ Tests de servicio (ProcurementService, WarehouseService, StatsService) — InvoiceService eliminado
3. ✅ Tests de repositorio con @DataJpaTest
4. ✅ Tests de seguridad (probar rutas protegidas con addFilters=true)

### SPRINT 6 (Infraestructura - Depués del MVP)
1. Implementar AuditLog
2. Crear entidad de devoluciones (Return)
3. Workflow de aprobación con escalamiento
4. Valoración de inventario (FIFO)
5. Reportes PDF
6. Alertas de stock bajo automáticas

---

## 10. RIESGOS PRIORIZADOS

| # | Riesgo | Severidad | Probabilidad | Impacto | Mitigación |
|---|---|---|---|---|---|
| 1 | **Inconsistencia de stock por race conditions** | CRÍTICA | Alta | $ y datos incorrectos | `@Version` en StockLevel ASAP |
| 2 | **Factura pagada sin coincidir con recepción** | CRÍTICA | Media | $ pérdida financiera | (Eliminado) Módulo de facturas removido |
| 3 | **Cost-centers expuestos públicamente** | ALTA | Baja | Información sensible expuesta | Mover a autenticado |
| 4 | **Backup no funcional** | ALTA | Cierta | Pérdida total de datos | Implementar backup.sh |
| 5 | **Dashboard stale por cache sin TTL** | MEDIA | Alta | Decisiones con datos desactualizados | Configurar TTL |
| 6 | **Movimientos masivos sin paginación** | MEDIA | Media (escala) | OOM del servidor | Agregar Pageable |
| 7 | **POs grandes aprobadas sin escalamiento** | MEDIA | Alta | Riesgo financiero | Workflow de aprobación |

---

## 11. QUICK WINS (Implementación inmediata)

```java
// 1. (Eliminado) Invoice.java - Ya no aplica, módulo eliminado
// @Table(name = "invoices", indexes = {
//     @Index(name = "idx_invoice_po", columnList = "purchase_order_id"),
//     @Index(name = "idx_invoice_supplier", columnList = "supplier_id")
})

// 2. PurchaseOrder.java - Agregar @Column faltante
@Column(name = "deleted_at")
private LocalDateTime deletedAt;

// 3. WarehouseService.java:605 - Corregir running balance
runningBalance[0] = runningBalance[0].add(
    m.getType() == MovementType.OUTPUT || m.getType() == MovementType.ADJUSTMENT 
        ? m.getQuantity().negate() 
        : m.getQuantity()
);

// 4. SecurityConfig.java - Mover origins a properties
@Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
private String[] allowedOrigins;

// 5. (Eliminado) InvoiceService.java - Ya no aplica, módulo eliminado
boolean qtyMatch = invQty.compareTo(poQty) == 0 && invQty.compareTo(receivedQty) == 0;
```

---

## 12. ESTADÍSTICAS DEL ANÁLISIS

| Métrica | Valor |
|---|---|
| Entidades analizadas | 18 |
| DTOs analizados | 40 |
| Servicios analizados | 15 |
| Controladores analizados | 13 |
| Repositorios analizados | 12 |
| Tests existentes | 4 (de 13 controladores) |
| Brechas críticas encontradas | 5 |
| Brechas de seguridad | 6 |
| Brechas de rendimiento | 5 |
| Brechas de datos | 6 |
| Brechas funcionales | 7 |
| Brechas de testing | 4 |
| Brechas de documentación | 4 |
| Quick Wins identificados | 5 |
| Sprints recomendados | 6 |
