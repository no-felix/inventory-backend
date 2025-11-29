# Inventory Backend — Best Practices & Conventions

This document outlines the coding standards, commit conventions, and best practices for the Inventory Backend project.

---

## Table of Contents

1. [Commit Conventions](#commit-conventions)
2. [Code Style & Clean Code](#code-style--clean-code)
3. [Spring Boot 4 Best Practices](#spring-boot-4-best-practices)
4. [Hexagonal Architecture Guidelines](#hexagonal-architecture-guidelines)
5. [API Design Guidelines](#api-design-guidelines)
6. [Testing Guidelines](#testing-guidelines)

---

## Commit Conventions

We use **Gitmoji** for commit messages to make the commit history more readable and expressive.

### Format

```
<gitmoji> <short description>
```

### Common Gitmojis

| Emoji | Code | Description |
|-------|------|-------------|
| 🎉 | `:tada:` | Initial commit |
| ✨ | `:sparkles:` | Introduce new features |
| 🐛 | `:bug:` | Fix a bug |
| 🔥 | `:fire:` | Remove code or files |
| 📝 | `:memo:` | Add or update documentation |
| 🎨 | `:art:` | Improve structure / format of the code |
| ♻️ | `:recycle:` | Refactor code |
| 🧪 | `:test_tube:` | Add or update tests |
| 🔧 | `:wrench:` | Add or update configuration files |
| 🧱 | `:bricks:` | Infrastructure related changes (Docker, CI/CD) |
| ⬆️ | `:arrow_up:` | Upgrade dependencies |
| ⬇️ | `:arrow_down:` | Downgrade dependencies |
| ➕ | `:heavy_plus_sign:` | Add a dependency |
| ➖ | `:heavy_minus_sign:` | Remove a dependency |
| 🔒 | `:lock:` | Fix security issues |
| 🚀 | `:rocket:` | Deploy stuff |
| 💄 | `:lipstick:` | Add or update the UI and style files |
| 🗃️ | `:card_file_box:` | Perform database related changes |
| 🏗️ | `:building_construction:` | Make architectural changes |
| 🔀 | `:twisted_rightwards_arrows:` | Merge branches |
| ⏪ | `:rewind:` | Revert changes |
| 🚧 | `:construction:` | Work in progress |
| 💚 | `:green_heart:` | Fix CI Build |
| 👷 | `:construction_worker:` | Add or update CI build system |
| 📦 | `:package:` | Add or update compiled files or packages |
| 🙈 | `:see_no_evil:` | Add or update .gitignore |

### Examples

```
🎉 Initial commit
✨ Add Product CRUD endpoints
🐛 Fix stock quantity calculation
📝 Update API documentation
🧱 Add Dockerfile and docker-compose
🗃️ Add Flyway migration for products table
♻️ Refactor ProductService to use hexagonal architecture
🧪 Add integration tests for PurchaseOrder flow
🔧 Configure OpenAPI generator plugin
```

---

## Code Style & Clean Code

### General Principles

1. **SOLID Principles**
   - **S**ingle Responsibility: Each class should have one reason to change
   - **O**pen/Closed: Open for extension, closed for modification
   - **L**iskov Substitution: Subtypes must be substitutable for base types
   - **I**nterface Segregation: Many specific interfaces over one general
   - **D**ependency Inversion: Depend on abstractions, not concretions

2. **DRY (Don't Repeat Yourself)**
   - Extract common logic into reusable methods/classes
   - Use inheritance and composition wisely

3. **KISS (Keep It Simple, Stupid)**
   - Avoid over-engineering
   - Write readable, straightforward code

4. **YAGNI (You Aren't Gonna Need It)**
   - Don't implement features until they're actually needed

### Naming Conventions

```java
// Classes: PascalCase
public class ProductService { }

// Interfaces: PascalCase (no "I" prefix)
public interface ProductRepository { }

// Methods: camelCase, verb-based
public Product findById(Long id) { }
public void createProduct(ProductRequest request) { }

// Variables: camelCase
private final ProductRepository productRepository;

// Constants: SCREAMING_SNAKE_CASE
public static final String DEFAULT_STATUS = "ACTIVE";

// Packages: lowercase
package de.nofelix.inventorybackend.domain.model;
```

### Code Organization

```java
public class ExampleClass {
    // 1. Static fields (constants first)
    private static final Logger log = LoggerFactory.getLogger(ExampleClass.class);
    
    // 2. Instance fields
    private final DependencyA dependencyA;
    private final DependencyB dependencyB;
    
    // 3. Constructors
    public ExampleClass(DependencyA dependencyA, DependencyB dependencyB) {
        this.dependencyA = dependencyA;
        this.dependencyB = dependencyB;
    }
    
    // 4. Public methods
    public void publicMethod() { }
    
    // 5. Package-private methods
    void packagePrivateMethod() { }
    
    // 6. Protected methods
    protected void protectedMethod() { }
    
    // 7. Private methods
    private void privateMethod() { }
}
```

### Method Guidelines

- Keep methods short (ideally < 20 lines)
- One level of abstraction per method
- Maximum 3-4 parameters; use objects for more
- Prefer immutability
- Return early to reduce nesting

```java
// ❌ Bad
public Product processProduct(Product product) {
    if (product != null) {
        if (product.getSku() != null) {
            // lots of nested logic
        }
    }
    return null;
}

// ✅ Good
public Product processProduct(Product product) {
    if (product == null) {
        throw new IllegalArgumentException("Product cannot be null");
    }
    if (product.getSku() == null) {
        throw new IllegalArgumentException("SKU cannot be null");
    }
    
    return doProcess(product);
}
```

---

## Spring Boot 4 Best Practices

### Constructor Injection

Always use constructor injection (Lombok `@RequiredArgsConstructor` is allowed):

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
}
```

### Configuration Properties

Use `@ConfigurationProperties` for type-safe configuration:

```java
@ConfigurationProperties(prefix = "inventory")
public record InventoryProperties(
    int lowStockThreshold,
    String defaultCurrency
) {}
```

### Exception Handling

Use `@RestControllerAdvice` for global exception handling:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
```

### Validation

Use Jakarta Validation annotations on DTOs:

```java
public record ProductRequest(
    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    String sku,
    
    @NotBlank(message = "Name is required")
    String name,
    
    @PositiveOrZero(message = "Quantity must be zero or positive")
    Integer quantityOnHand,
    
    @Positive(message = "Unit price must be positive")
    BigDecimal unitPrice
) {}
```

### Logging

Use SLF4J with Lombok `@Slf4j`:

```java
@Slf4j
@Service
public class ProductService {
    
    public Product createProduct(ProductRequest request) {
        log.info("Creating product with SKU: {}", request.sku());
        // ...
        log.debug("Product created successfully: {}", product);
        return product;
    }
}
```

### Transactional Boundaries

Place `@Transactional` at the service layer:

```java
@Service
@Transactional(readOnly = true)
public class ProductService {
    
    public List<Product> findAll() {
        return productRepository.findAll();
    }
    
    @Transactional
    public Product create(ProductRequest request) {
        // write operation
    }
}
```

---

## Hexagonal Architecture Guidelines

### Layer Structure

```
src/main/java/de/nofelix/inventorybackend/
├── domain/                    # Core business logic (innermost)
│   ├── model/                 # Domain entities
│   ├── port/                  # Interfaces (ports)
│   │   ├── in/               # Input/Driving ports (use cases)
│   │   └── out/              # Output/Driven ports (repositories)
│   └── service/              # Domain services
│
├── application/              # Application layer (orchestration)
│   ├── usecase/              # Use case implementations
│   └── dto/                  # Data Transfer Objects
│
├── adapter/                  # Adapters (outermost)
│   ├── in/                   # Input/Driving adapters
│   │   └── web/             # REST controllers
│   └── out/                  # Output/Driven adapters
│       └── persistence/     # JPA repositories, entities
│
└── infrastructure/           # Technical concerns
    ├── config/              # Spring configuration
    └── exception/           # Exception handling
```

### Dependency Rules

1. **Domain** has NO external dependencies
2. **Application** depends on Domain
3. **Adapters** depend on Application and Domain
4. **Infrastructure** can depend on all layers

### Port Naming

```java
// Input Ports (Use Cases) - in domain/port/in/
public interface CreateProductUseCase {
    ProductResponse createProduct(CreateProductCommand command);
}

// Output Ports - in domain/port/out/
public interface ProductRepositoryPort {
    Product save(Product product);
    Optional<Product> findById(Long id);
}
```

---

## API Design Guidelines

### RESTful Conventions

| Operation | HTTP Method | Path | Response |
|-----------|-------------|------|----------|
| List | GET | /api/products | 200 OK |
| Get one | GET | /api/products/{id} | 200 OK, 404 Not Found |
| Create | POST | /api/products | 201 Created |
| Update | PUT | /api/products/{id} | 200 OK |
| Partial Update | PATCH | /api/products/{id} | 200 OK |
| Delete | DELETE | /api/products/{id} | 204 No Content |

### Response Format

Use consistent response structures:

```json
{
  "id": 1,
  "sku": "SKU-001",
  "name": "Widget",
  "createdAt": "2025-11-29T10:30:00Z",
  "updatedAt": "2025-11-29T10:30:00Z"
}
```

### Error Responses (RFC 7807 - Problem Details)

```json
{
  "type": "https://api.inventory.com/errors/not-found",
  "title": "Product Not Found",
  "status": 404,
  "detail": "Product with ID 123 was not found",
  "instance": "/api/products/123"
}
```

---

## Testing Guidelines

### Test Naming

Use descriptive names following the pattern:
`methodName_stateUnderTest_expectedBehavior`

```java
@Test
void createProduct_withValidRequest_returnsCreatedProduct() { }

@Test
void createProduct_withDuplicateSku_throwsConflictException() { }

@Test
void findById_withNonExistentId_throwsNotFoundException() { }
```

### Test Structure (AAA Pattern)

```java
@Test
void createProduct_withValidRequest_returnsCreatedProduct() {
    // Arrange
    var request = new ProductRequest("SKU-001", "Widget", 100, BigDecimal.TEN);
    
    // Act
    var result = productService.createProduct(request);
    
    // Assert
    assertThat(result).isNotNull();
    assertThat(result.sku()).isEqualTo("SKU-001");
}
```

### Test Coverage Goals

- Unit tests: All service methods
- Integration tests: Critical flows (Testcontainers)
- Controller tests: Input validation, response formats

---

## Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Gitmoji Reference](https://gitmoji.dev/)
- [Clean Code by Robert C. Martin](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
