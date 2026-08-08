# Visco Orinoco — Backend API

Sistema de gestión empresarial para control de inventario, órdenes de compra, proveedores y facturación. Desarrollado con Spring Boot 3 y desplegado en Railway.

---

## Tabla de contenidos

- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Módulos del sistema](#módulos-del-sistema)
- [Requisitos previos](#requisitos-previos)
- [Configuración local](#configuración-local)
- [Variables de entorno](#variables-de-entorno)
- [Ejecutar el proyecto](#ejecutar-el-proyecto)
- [API Reference](#api-reference)
- [Seguridad](#seguridad)
- [Despliegue en Railway](#despliegue-en-railway)
- [Estructura del proyecto](#estructura-del-proyecto)

---

## Stack tecnológico

| Tecnología | Versión | Uso |
|------------|---------|-----|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.x | Framework base |
| Spring Security | 3.x | Autenticación y autorización |
| Spring Data JPA | 3.x | Persistencia |
| PostgreSQL | 15+ | Base de datos |
| Hibernate | 6.x | ORM |
| JWT (jjwt) | 0.12.x | Tokens de sesión |
| Apache POI | 5.x | Generación de reportes Excel |
| Apache Commons CSV | 1.x | Importación masiva de productos |
| Bucket4j | 8.x | Rate limiting |
| Lombok | 1.18.x | Reducción de boilerplate |
| SpringDoc OpenAPI | 2.x | Documentación Swagger |
| Maven | 3.9+ | Build tool |

---

## Arquitectura

```
┌─────────────────────────────────────────┐
│           Frontend (Next.js)            │
│        viscoorinocosia.vercel.app       │
└────────────────┬────────────────────────┘
                 │ HTTPS + HttpOnly Cookie (JWT)
┌────────────────▼────────────────────────┐
│         Spring Boot API (Railway)       │
│                                         │
│  Controllers → Services → Repositories  │
│                                         │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │  Security   │  │   Rate Limiter   │  │
│  │  JWT Filter │  │   (Bucket4j)     │  │
│  └─────────────┘  └──────────────────┘  │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         PostgreSQL (Railway)            │
└─────────────────────────────────────────┘
```

El sistema implementa autenticación **stateless** mediante JWT almacenado en cookies HttpOnly. Cada request es validado por `JwtAuthFilter` antes de llegar a los controllers.

---

## Módulos del sistema

### 🔐 Autenticación (`/api/auth`)
Registro, login y logout de usuarios. El JWT se entrega exclusivamente en cookie HttpOnly para prevenir ataques XSS.

### 👥 Usuarios (`/api/users`)
Gestión de cuentas: activar, desactivar, cambiar roles y asignar centros de costo. Solo accesible para `ADMIN`.

### 🏭 Proveedores (`/api/suppliers`)
CRUD completo de proveedores con soft-delete, representantes legales, múltiples teléfonos y métricas de desempeño mensual por proveedor.

### 📦 Productos e Inventario (`/api/inventory`)
Catálogo de productos con código interno, SKU, código SAP, unidad de medida y punto de reorden. Soporta importación masiva desde CSV.

### 🏪 Almacenes (`/api/warehouse`)
Gestión de almacenes, ubicaciones físicas, recepción de mercancía contra órdenes de compra, transferencias entre almacenes, ajustes de stock y kardex de movimientos.

### 📋 Requisiciones (`/api/requisitions`)
Flujo de solicitudes de compra con workflow de aprobación: `DRAFT → PENDING → AWAITING_APPROVAL → APPROVED → CONVERTED`.

### 🛒 Órdenes de Compra (`/api/procurement`)
Gestión completa del ciclo de compra con workflow de aprobación: `PENDING → AWAITING_APPROVAL → APPROVED → IN_TRANSIT → DELIVERED`.

### 🧾 Facturas (`/api/invoices`)
Conciliación de facturas contra órdenes de compra (3-way matching: cantidad ordenada, cantidad recibida y precio unitario).

### 📊 Dashboard (`/api/dashboard`)
KPIs en tiempo real: total de órdenes, unidades en inventario, gasto mensual, tasa de cumplimiento, inventario crítico y órdenes recientes.

### 🏢 Centros de Costo (`/api/cost-centers`)
Catálogo de centros de costo para clasificación de requisiciones y usuarios.

---

## Requisitos previos

- **Java 21** — [Descargar](https://adoptium.net/)
- **Maven 3.9+** — incluido con `./mvnw`
- **PostgreSQL 15+** — local o en la nube
- **Git**

---

## Configuración local

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/viscosg-backend.git
cd viscosg-backend
```

### 2. Crear la base de datos

```sql
CREATE DATABASE visco_db;
CREATE USER visco_admin WITH PASSWORD 'tu_password';
GRANT ALL PRIVILEGES ON DATABASE visco_db TO visco_admin;
```

### 3. Configurar variables de entorno

Crea el archivo `src/main/resources/application-local.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/visco_db
spring.datasource.username=visco_admin
spring.datasource.password=tu_password

app.jwt.secret=tu_secreto_base64_de_al_menos_32_caracteres
app.jwt.expiration-ms=86400000

spring.mail.username=tu_correo@gmail.com
spring.mail.password=tu_app_password_de_gmail

jwt.cookie.secure=false
```

> ⚠️ Nunca subas `application-local.properties` a Git. Ya está en `.gitignore`.

---

## Variables de entorno

Variables requeridas en producción (Railway):

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `DB_URL` | URL de conexión PostgreSQL en **formato JDBC** | `jdbc:postgresql://host:5432/railway` |
| `DB_USERNAME` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de la base de datos | `***` |
| `JWT_SECRET` | Secreto para firmar JWT (Base64, min 32 chars) | `***` |
| `JWT_EXPIRATION_MS` | Expiración del token en ms | `86400000` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos (sin espacios) | `https://viscoorinocosia.vercel.app` |
| `JPA_DDL_AUTO` | Estrategia DDL de Hibernate | `validate` |
| `JWT_COOKIE_SECURE` | Cookie segura (true en producción) | `true` |
| `RESEND_API_KEY` | API key del transporte de email (Resend) | `***` |
| `APP_EMAIL_ENABLED` | Activa el envío de emails | `true` |
| `REPORT_RECIPIENTS` | Emails para reporte semanal (separados por coma) | `admin@empresa.com` |
| `CLOUDINARY_URL` | URL de Cloudinary para uploads | `cloudinary://key:secret@cloud` |
| `OTLP_ENABLED` | Activa export de métricas OTLP a Grafana | `false` |
| `REPORTS_MAX_RECORDS` | Tope de filas por reporte/export | `2000` |
| `PORT` | Puerto(HTTP) que Railway inyectaautomáticamente | `8080` |

> **Nota sobre `DB_URL`:** Railway Postgres expone `DATABASE_URL` en formato libpq (`postgres://...`). Spring Boot JDBC necesita `jdbc:postgresql://...`. En Railway, define `DB_URL` manualmente con el formato JDBC, o referencia `${Postgres.DATABASE_PRIVATE_URL}` y reemplaza el scheme `postgres://` → `jdbc:postgresql://` en el valor. Las migraciones las maneja Flyway, por eso `JPA_DDL_AUTO=validate`.

---

## Ejecutar el proyecto

### Desarrollo local

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Build para producción

```bash
./mvnw clean package -DskipTests
java -jar target/*.jar
```

### Ejecutar tests

```bash
./mvnw test
```

La API estará disponible en `http://localhost:8080`.

---

## API Reference

La documentación interactiva está disponible en:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

### Endpoints principales

```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/logout
GET    /api/auth/me

GET    /api/suppliers
POST   /api/suppliers
GET    /api/suppliers/{id}
PUT    /api/suppliers/{id}
DELETE /api/suppliers/{id}
GET    /api/suppliers/performance

GET    /api/procurement/orders
POST   /api/procurement/orders
PATCH  /api/procurement/orders/{id}/submit-for-approval
PATCH  /api/procurement/orders/{id}/approve
PATCH  /api/procurement/orders/{id}/reject
PATCH  /api/procurement/orders/{id}/send-to-supplier
PATCH  /api/procurement/orders/{id}/cancel

GET    /api/requisitions
POST   /api/requisitions
PATCH  /api/requisitions/{id}/submit
PATCH  /api/requisitions/{id}/approve
PATCH  /api/requisitions/{id}/reject
PATCH  /api/requisitions/{id}/convert

POST   /api/warehouse/orders/{id}/receive
POST   /api/warehouse/stock/transfer
POST   /api/warehouse/stock/adjust
GET    /api/warehouse/movements
GET    /api/warehouse/products/{productId}/stock-breakdown

GET    /api/dashboard/kpis
GET    /api/dashboard/recent-orders
GET    /api/dashboard/spending
GET    /api/dashboard/critical-inventory
```

---

## Seguridad

### Autenticación
- JWT firmado con HMAC-SHA256 almacenado en cookie **HttpOnly + Secure**
- Expiración configurable (default: 24 horas)
- El token nunca se expone en el body de las respuestas

### Autorización por rol

| Rol | Acceso |
|-----|--------|
| `ADMIN` | Todo el sistema |
| `MANAGER` | Proveedores, compras, requisiciones, dashboard |
| `PROCUREMENT` | Proveedores, compras, requisiciones, facturas |
| `WAREHOUSEMAN` | Almacenes, inventario, dashboard |
| `USER` | Solo endpoints autenticados básicos |

### Rate Limiting (por IP)

| Endpoint | Límite |
|----------|--------|
| `/api/auth/login` | 8 requests / 1 minuto |
| `/api/auth/register` | 3 requests / 5 minutos |
| `/api/inventory` | 60 requests / 1 minuto |
| `/api/procurement` | 60 requests / 1 minuto |
| `/api/dashboard` | 30 requests / 1 minuto |

---

## Despliegue en Railway

### Configuración del servicio

El deploy usa el `Dockerfile` multi-stage (Maven build → JRE alpine) y `railway.json` de la raíz del repo. No requiere Build/Start Command manuales: Railway detecta el Dockerfile automáticamente.

| Campo | Valor |
|-------|-------|
| Builder | Dockerfile (`railway.json` ya configurado) |
| Start Command | `/app/start.sh` (definido en `railway.json`) |
| Healthcheck | `GET /actuator/health` (Actuator, expuesto) |
| Restart Policy | `ON_FAILURE` (máx. 10 reintentos) |
| Branch | `main` |
| Plan | Developer (512 MB RAM mínimo) |

### Pasos para desplegar

1. Crea un nuevo proyecto en [Railway](https://railway.app).
2. **New → Database → PostgreSQL** (Railway aprovisiona Postgres y expone variables `DATABASE_PUBLIC_URL` / `DATABASE_PRIVATE_URL`).
3. **New → GitHub Repo** y selecciona este repositorio. Railway detectará `Dockerfile` + `railway.json`.
4. En la pestaña **Variables** del servicio, agrega:
   - `DB_URL` = `jdbc:postgresql://<host>:<port>/<db>` (convierte el `postgres://` de Railway al scheme JDBC).
   - `DB_USERNAME`, `DB_PASSWORD` (del plugin Postgres).
   - `JWT_SECRET`, `JWT_COOKIE_SECURE=true`, `JPA_DDL_AUTO=validate`.
   - `CORS_ALLOWED_ORIGINS`, `RESEND_API_KEY`, `APP_EMAIL_ENABLED`, `REPORT_RECIPIENTS`, etc. (ver tabla de variables).
5. Railway inyecta `PORT` automáticamente; la app escucha en él (`server.port=${PORT:8080}`).
6. Opcional: agrega un **Custom Domain** en Settings → Networking.
7. Flyway ejecutará las migraciones automáticamente en el primer arranque.

### Deploy automático

Cada push a `main` dispara un deploy automático en Railway:

```bash
git add .
git commit -m "tu mensaje"
git push origin main
```

### Reporte semanal automático

El sistema genera y envía un reporte Excel cada **lunes a las 8:00 AM** a los emails configurados en `REPORT_RECIPIENTS`. Para dispararlo manualmente (solo `ADMIN`):

```bash
POST /api/admin/reports/send
```

---

## Estructura del proyecto

```
src/
├── main/
│   ├── java/com/visco/backend/
│   │   ├── config/          # SecurityConfig, JwtAuthFilter, RateLimitFilter, OpenAPI
│   │   ├── controllers/     # REST controllers
│   │   ├── exception/       # GlobalExceptionHandler
│   │   ├── models/
│   │   │   ├── dtos/        # Request/Response records y clases
│   │   │   └── entities/    # Entidades JPA y enums
│   │   ├── repositories/    # Spring Data JPA repositories
│   │   ├── scripts/         # backup.sh para PostgreSQL
│   │   └── services/        # Lógica de negocio
│   └── resources/
│       ├── application.properties        # Config base (usa variables de entorno)
│       └── application-local.properties  # Config local (no subir a Git)
└── test/
    └── java/com/visco/backend/           # Unit tests
```

---

## Backup de base de datos

El script `backup.sh` genera backups automáticos comprimidos de PostgreSQL con rotación configurable:

```bash
# Ejecutar manualmente
./src/main/java/com/visco/backend/scripts/backup.sh

# Variables del script
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
BACKUP_DIR=/backups
RETENTION_DAYS=7
```

---

## Licencia

Proyecto privado — Visco Orinoco © 2025. Todos los derechos reservados.