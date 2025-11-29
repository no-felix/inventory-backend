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
- 🗃️ **PostgreSQL** — Production-ready database with Flyway migrations
- 🧪 **Comprehensive Testing** — Unit, integration, and contract tests with Testcontainers
- 📊 **Code Coverage** — JaCoCo reports with Lombok exclusions
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
- **Docker** & Docker Compose
- **Maven 3.9+** (or use included wrapper)

### Quick Start

```bash
# Clone the repository
git clone https://github.com/no-felix/inventory-backend.git
cd inventory-backend

# Start PostgreSQL with Docker Compose
docker compose up -d

# Run the application
./mvnw spring-boot:run

# The API is now available at http://localhost:8080
```

### Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `spring.r2dbc.url` | `r2dbc:postgresql://localhost:5432/inventory` | Database URL |
| `server.port` | `8080` | Server port |

## 📡 API

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/products` | List all products |
| `GET` | `/api/v1/products/{id}` | Get product by ID |
| `POST` | `/api/v1/products` | Create new product |
| `PUT` | `/api/v1/products/{id}` | Update product |
| `DELETE` | `/api/v1/products/{id}` | Delete product |
| `GET` | `/api/v1/purchase-orders` | List purchase orders |
| `POST` | `/api/v1/purchase-orders` | Create purchase order |
| `POST` | `/api/v1/purchase-orders/{id}/receive` | Receive purchase order |
| `GET` | `/api/v1/stock-movements` | List stock movements |

### Example Request

```bash
# Create a new product
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
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
# Run all tests
./mvnw test

# Run with coverage report
./mvnw verify

# View coverage report
open target/site/jacoco/index.html
```

### Test Categories

| Type | Location | Framework |
|------|----------|-----------|
| Unit Tests | `src/test/java/**/domain/**` | JUnit 5, Mockito |
| Integration Tests | `src/test/java/**/adapter/**` | Testcontainers |
| Contract Tests | `src/test/java/**/web/**` | WebTestClient |

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
| **Framework** | Spring Boot 4, Spring WebFlux, Spring Data R2DBC |
| **Database** | PostgreSQL 16, Flyway Migrations |
| **API** | OpenAPI 3.0, OpenAPI Generator |
| **Testing** | JUnit 5, Mockito, Testcontainers, AssertJ |
| **Code Quality** | Lombok, MapStruct, JaCoCo |

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with ❤️ by [no-felix](https://github.com/no-felix)

</div>
