<div align="center">

# 📦 Inventory Backend

[![CI](https://github.com/no-felix/inventory-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/no-felix/inventory-backend/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**A modern, reactive inventory management system built with Spring Boot 4 and Hexagonal Architecture**

[Features](#-features) •
[Architecture](#-architecture) •
[Getting Started](#-getting-started) •
[API](#-api) •
[Testing](#-testing) •
[Contributing](#-contributing)

</div>

---

## ✨ Features

- 🚀 **Reactive Stack** — Built with Spring WebFlux and R2DBC for non-blocking I/O
- 🏗️ **Hexagonal Architecture** — Clean separation of concerns with ports and adapters
- 📋 **Contract-First API** — OpenAPI 3.0 specification with code generation
- � **JWT Authentication** — "Secure" API with register, login, and token refresh
- 🗃️ **PostgreSQL** — Production-ready database with Flyway migrations
- 📊 **Inventory Metrics** — Low stock alerts, slow-moving items, valuation reports
- 🧪 **Comprehensive Testing** — 237 tests with Testcontainers
- 🐳 **Docker Ready** — Docker Compose for local development

## 🏛️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Adapters (Infrastructure)                │
│  ┌──────────────────┐                    ┌──────────────────┐   │
│  │   REST API       │                    │   PostgreSQL     │   │
│  │   (WebFlux)      │                    │   (R2DBC)        │   │
│  └────────┬─────────┘                    └────────┬─────────┘   │
│           │                                       │             │
│           ▼                                       ▼             │
│  ┌──────────────────┐                    ┌──────────────────┐   │
│  │   Input Ports    │                    │  Output Ports    │   │
│  │   (Use Cases)    │◄──────────────────►│  (Repositories)  │   │
│  └────────┬─────────┘                    └──────────────────┘   │
│           │                                                     │
├───────────┼─────────────────────────────────────────────────────┤
│           ▼                Domain Layer                         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  • Product          • PurchaseOrder    • StockMovement  │    │
│  │  • Business Rules   • Domain Events    • Validations    │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Project Structure

```
src/main/java/de/nofelix/inventorybackend/
├── domain/                    # Core business logic
│   ├── model/                 # Domain entities
│   ├── port/in/              # Input ports (use cases)
│   ├── port/out/             # Output ports (repositories)
│   └── exception/            # Domain exceptions
├── application/              # Application services
│   └── usecase/              # Use case implementations
├── adapter/                  # Infrastructure adapters
│   ├── in/web/              # REST controllers
│   └── out/persistence/     # Database repositories
└── infrastructure/          # Cross-cutting concerns
    ├── config/              # Spring configuration
    └── exception/           # Global exception handling
```

## 🚀 Getting Started

### Prerequisites

- **Java 21** or later
- **Docker** & Docker Compose (for PostgreSQL)
- **Maven 3.9+** (or use the included wrapper `./mvnw`)

### Quick Start (Development)

```bash
# 1. Clone the repository
git clone https://github.com/no-felix/inventory-backend.git
cd inventory-backend

# 2. Start PostgreSQL with Docker Compose
docker compose up -d

# 3. Run the application
./mvnw spring-boot:run

# 4. The API is now available at http://localhost:8080
```

That's it! The default configuration connects to the Docker PostgreSQL instance automatically.

---

## ⚙️ Configuration

### Environment Variables

All configuration can be overridden via environment variables. Here are the available options:

| Variable | Default | Description |
|----------|---------|-------------|
| **Database** |||
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `inventory` | Database name |
| `DB_USERNAME` | `inventory` | Database username |
| `DB_PASSWORD` | `inventory` | Database password |
| **Server** |||
| `SERVER_PORT` | `8080` | HTTP server port |
| **JWT Authentication** |||
| `JWT_SECRET` | *(dev default)* | Secret key for signing tokens (min 32 chars) |
| `JWT_ACCESS_EXPIRATION` | `900` | Access token lifetime in seconds (15 min) |
| `JWT_REFRESH_EXPIRATION` | `604800` | Refresh token lifetime in seconds (7 days) |
| **CORS** |||
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Allowed frontend origins |
| **Application** |||
| `LOW_STOCK_THRESHOLD` | `10` | Quantity threshold for low stock alerts |

### Spring Profiles

| Profile | Use Case | Database |
|---------|----------|----------|
| *(default)* | Development with Docker PostgreSQL | PostgreSQL (localhost:5432) |
| `dev` | Development with in-memory database | H2 (no Docker needed) |
| `docker` | Running inside Docker container | PostgreSQL (db:5432) |
| `prod` | Production deployment | PostgreSQL (via env vars) |

#### Using Profiles

```bash
# Run with H2 in-memory database (no Docker needed)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run with default PostgreSQL (requires Docker)
./mvnw spring-boot:run

# Run with custom environment variables
DB_HOST=mydb.example.com JWT_SECRET=my-super-secret-key ./mvnw spring-boot:run
```

### Development Setup Options

#### Option 1: Docker PostgreSQL (Recommended)

```bash
# Start PostgreSQL
docker compose up -d

# Run application
./mvnw spring-boot:run

# Stop PostgreSQL when done
docker compose down
```

#### Option 2: H2 In-Memory Database

```bash
# No Docker needed - uses embedded H2
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# H2 Console available at: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:inventory
# Username: sa (no password)
```

#### Option 3: External PostgreSQL

```bash
# Set environment variables for your database
export DB_HOST=your-postgres-host
export DB_PORT=5432
export DB_NAME=inventory
export DB_USERNAME=your-user
export DB_PASSWORD=your-password
export JWT_SECRET=your-production-secret-at-least-32-characters

# Run application
./mvnw spring-boot:run
```

### Production Checklist (THIS IS NOT MADE TO BE SAFE IN PROD!!!)

⚠️ **Before deploying to production:**

1. **Set a secure JWT secret** (at least 32 characters):
   ```bash
   export JWT_SECRET="your-very-long-and-secure-random-secret-key"
   ```

2. **Use production database credentials**:
   ```bash
   export DB_PASSWORD="strong-database-password"
   ```

3. **Enable production profile**:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   ```

---

## 📡 API

### Initial Admin Setup

When starting with a fresh database, you need to create the first admin account:

```bash
# 1. Check if setup is needed
curl http://localhost:8080/api/v1/auth/setup/status
# Returns: {"setupRequired": true}

# 2. Create the initial admin account (only works once!)
curl -X POST http://localhost:8080/api/v1/auth/setup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@example.com",
    "password": "your-secure-password"
  }'

# 3. Login with your admin account
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "your-secure-password"}'
```

> **Note:** The setup endpoint is disabled after the first admin is created.
> Regular users can register via `/api/v1/auth/register` but will have `USER` role (read-only).

### Authentication

All endpoints except `/api/v1/auth/**` and `/actuator/**` require JWT authentication.

```bash
# 1. Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "email": "admin@example.com", "password": "password123"}'

# 2. Login to get tokens
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password123"}'

# Response:
# {
#   "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
#   "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
#   "tokenType": "Bearer",
#   "expiresIn": 900
# }

# 3. Use the access token for API calls
curl http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."

# 4. Refresh token when access token expires
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "eyJhbGciOiJIUzUxMiJ9..."}'
```

### Endpoints Overview

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| **Admin Setup** ||||
| `GET` | `/api/v1/auth/setup/status` | ❌ | Check if admin setup is needed |
| `POST` | `/api/v1/auth/setup` | ❌ | Create initial admin (once only) |
| **Authentication** ||||
| `POST` | `/api/v1/auth/register` | ❌ | Register new user |
| `POST` | `/api/v1/auth/login` | ❌ | Login and get tokens |
| `POST` | `/api/v1/auth/refresh` | ❌ | Refresh access token |
| **Products** ||||
| `GET` | `/api/v1/products` | ✅ | List all products |
| `GET` | `/api/v1/products/{id}` | ✅ | Get product by ID |
| `POST` | `/api/v1/products` | ✅ | Create new product |
| `PUT` | `/api/v1/products/{id}` | ✅ | Update product |
| `DELETE` | `/api/v1/products/{id}` | ✅ | Delete product |
| **Purchase Orders** ||||
| `GET` | `/api/v1/purchase-orders` | ✅ | List purchase orders |
| `GET` | `/api/v1/purchase-orders/{id}` | ✅ | Get order by ID |
| `POST` | `/api/v1/purchase-orders` | ✅ | Create purchase order |
| `POST` | `/api/v1/purchase-orders/{id}/receive` | ✅ | Receive purchase order |
| **Stock Movements** ||||
| `GET` | `/api/v1/stock-movements` | ✅ | List stock movements |
| `GET` | `/api/v1/stock-movements/product/{id}` | ✅ | Get movements for product |
| **Metrics** ||||
| `GET` | `/api/v1/metrics/inventory-summary` | ✅ | Inventory overview |
| `GET` | `/api/v1/metrics/stock-levels` | ✅ | All product stock levels |
| `GET` | `/api/v1/metrics/low-stock-alerts` | ✅ | Products below threshold |
| `GET` | `/api/v1/metrics/slow-moving-items` | ✅ | Items with no recent movement |
| `GET` | `/api/v1/metrics/valuation-by-price-range` | ✅ | Inventory value by price range |
| `GET` | `/api/v1/metrics/receipts` | ✅ | Receipt time series |
| **Health** ||||
| `GET` | `/actuator/health` | ❌ | Application health check |

### Example Request

```bash
# Create a new product (with authentication)
TOKEN="your-access-token"

curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "sku": "WIDGET-001",
    "name": "Premium Widget",
    "description": "A high-quality widget",
    "quantityOnHand": 100,
    "unitPrice": 29.99
  }'
```

### Error Responses

All errors follow [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) Problem Details format:

```json
{
  "type": "https://api.inventory.example.com/errors/product-not-found",
  "title": "Product Not Found",
  "status": 404,
  "detail": "Product with ID 123 was not found",
  "instance": "/api/v1/products/123"
}
```

## 🧪 Testing

```bash
# Run all tests (requires Docker for Testcontainers)
./mvnw test

# Run with coverage report
./mvnw verify

# View coverage report
open target/site/jacoco/index.html
```

**Test Results:** 237 tests passing ✅

### Test Categories

| Type | Location | Framework |
|------|----------|-----------|
| Unit Tests | `src/test/java/**/domain/**` | JUnit 5, Mockito |
| Integration Tests | `src/test/java/**/adapter/**` | Testcontainers |
| Controller Tests | `src/test/java/**/web/**` | WebTestClient |

---

## 🗃️ Sample Data

The application includes sample data for development (loaded via Flyway migration V5):

- **25 products** across categories (Electronics, Office, Storage, Tools, Low Stock)
- **3 purchase orders** (received and pending)
- **Stock movements** for audit trail

This data is automatically loaded when the application starts.

---

## 🛠️ Tech Stack

<table>
<tr>
<td align="center" width="100">
<img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/java/java-original.svg" width="48" height="48" alt="Java" />
<br>Java 21
</td>
<td align="center" width="100">
<img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/spring/spring-original.svg" width="48" height="48" alt="Spring" />
<br>Spring Boot 4
</td>
<td align="center" width="100">
<img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/postgresql/postgresql-original.svg" width="48" height="48" alt="PostgreSQL" />
<br>PostgreSQL
</td>
<td align="center" width="100">
<img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/docker/docker-original.svg" width="48" height="48" alt="Docker" />
<br>Docker
</td>
</tr>
</table>

| Category | Technologies |
|----------|-------------|
| **Framework** | Spring Boot 4, Spring WebFlux, Spring Data R2DBC, Spring Security |
| **Database** | PostgreSQL 16, Flyway Migrations, H2 (dev) |
| **Security** | JWT (JJWT 0.12), BCrypt |
| **API** | OpenAPI 3.0, OpenAPI Generator |
| **Testing** | JUnit 5, Mockito, Testcontainers, StepVerifier |
| **Code Quality** | Lombok, MapStruct, JaCoCo |

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with ❤️ by [no-felix](https://github.com/no-felix)

</div>
