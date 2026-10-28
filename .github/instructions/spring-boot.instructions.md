---
applyTo: "**/src/main/java/**/*.java,**/src/test/java/**/*.java,**/src/main/resources/**/*.yml,**/src/main/resources/**/*.yaml,**/src/main/resources/**/*.properties"
---

# Spring Boot Development Guidelines

## 1. Purpose

Spring Boot applications must remain:

* simple;
* testable;
* observable;
* operationally safe;
* secure;
* maintainable.

Follow the project architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
```

Do not introduce unnecessary architectural layers.

---

# 2. Dependency Injection

Use constructor injection.

Prefer:

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

Avoid field injection:

```java
@Autowired
private OrderRepository orderRepository;
```

Constructor injection makes dependencies explicit and improves testability.

---

# 3. Spring Stereotypes

Use stereotypes according to responsibility:

* `@RestController` for HTTP/API endpoints;
* `@Service` for application/business logic;
* `@Repository` for persistence components;
* `@Configuration` for configuration;
* `@Component` only when no more specific stereotype applies.

Do not use stereotypes merely because they are available.

A class should have a clear responsibility.

---

# 4. Controllers

Controllers must remain thin.

They are responsible for:

* receiving requests;
* validating external input;
* mapping input;
* invoking application services;
* mapping application results to HTTP responses.

Controllers must not:

* contain business logic;
* directly access repositories;
* contain SQL;
* manage transactions;
* contain complex orchestration.

Prefer:

```java
@PostMapping
public ResponseEntity<OrderResponse> create(
        @Valid @RequestBody CreateOrderRequest request) {

    Order order = orderService.create(request);

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderMapper.toResponse(order));
}
```

over implementing business logic directly in the controller.

---

# 5. Services

Services represent application use cases.

A service may:

* apply business rules;
* coordinate repositories;
* coordinate external clients;
* publish messages;
* manage transaction boundaries;
* coordinate multiple operations.

Services should remain cohesive.

Avoid:

* generic "god services";
* services that only forward calls;
* services containing HTTP concerns;
* services containing SQL.

Do not create `Service` / `ServiceImpl` pairs by default.

---

# 6. Transactions

Transaction boundaries should generally be defined at the service/use-case level.

Example:

```java
@Transactional
public Order createOrder(...) {
    ...
}
```

Use:

```java
@Transactional(readOnly = true)
```

for appropriate read-only use cases.

Do not put `@Transactional` indiscriminately on every method.

Transaction boundaries must be designed according to:

* atomicity requirements;
* database operations;
* concurrency;
* external calls;
* transaction duration.

A database transaction does not automatically include:

* HTTP calls;
* SNS publication;
* SQS processing;
* other external systems.

When database state and event publication must be reliable together, consider Transactional Outbox.

---

# 7. Configuration

Use externalized configuration.

Prefer typed configuration:

```java
@ConfigurationProperties(prefix = "application.orders")
public record OrderProperties(
        Duration timeout,
        int maxRetries) {
}
```

Avoid scattering:

```java
@Value("${...}")
```

throughout business code when a coherent configuration object is appropriate.

Never store secrets directly in source-controlled configuration.

Configuration should be validated at startup when appropriate.

---

# 8. Profiles

Use Spring profiles only when there is a meaningful environment difference.

Avoid excessive profiles.

Do not use profiles as a substitute for proper configuration management.

Test configuration should remain deterministic.

---

# 9. Validation

Use Jakarta Bean Validation for input constraints.

Examples:

```java
@NotBlank
private String name;

@Positive
private BigDecimal amount;
```

Use validation at external boundaries.

Business invariants that require application state or business logic belong in the service/entity layer.

Do not rely exclusively on controller validation for business rules.

---

# 10. REST API Design

REST APIs should:

* use meaningful resources;
* use consistent HTTP methods;
* use appropriate status codes;
* validate input;
* return stable contracts;
* provide meaningful error responses.

Avoid exposing JPA entities directly when doing so creates:

* persistence/API coupling;
* recursive serialization;
* lazy-loading issues;
* accidental field exposure;
* unstable API contracts.

Use DTOs when an explicit API boundary is required.

DTOs are not a requirement to create unnecessary layers.

---

# 11. Error Handling

Use centralized error handling.

Prefer:

```java
@RestControllerAdvice
```

for API-level exception mapping.

Error responses should be:

* consistent;
* meaningful;
* safe;
* free of sensitive internal information.

Do not expose:

* stack traces;
* SQL statements;
* credentials;
* internal infrastructure details.

---

# 12. HTTP Clients and External Services

External HTTP calls must define:

* connection timeout;
* response timeout;
* retry behavior where appropriate;
* failure behavior.

Do not retry blindly.

Consider whether the operation is idempotent before adding retries.

External failures must have explicit application behavior.

---

# 13. AWS Clients

AWS clients should be managed and injected through Spring configuration.

Do not instantiate AWS clients repeatedly inside business methods.

Configuration must support:

* credentials through the AWS credential provider chain;
* appropriate region configuration;
* testability;
* connection/resource management.

Use least-privilege IAM permissions.

---

# 14. Database Schema Management with Flyway

**Flyway is the mandatory database migration mechanism.**

Spring Boot must integrate with Flyway for application database initialization and schema evolution.

Typical migration location:

```text
src/main/resources/db/migration/
```

Example:

```text
V20261010_001__add_order_status.sql
```

Migration naming and versioning must follow the project's Flyway conventions.

### Hibernate schema generation

Hibernate must not be used as the production schema evolution mechanism.

Do not use:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

or:

```yaml
ddl-auto: create
ddl-auto: create-drop
```

for production schema management.

JPA entities describe the application's persistence model.

Flyway migrations describe database schema evolution.

These responsibilities must remain distinct:

```text
JPA Entity
    ↓
Persistence Model

Flyway
    ↓
Database Schema Evolution
```

### Migration compatibility

When an application is deployed using rolling deployment, consider that old and new application versions may temporarily coexist.

Migrations must therefore be compatible with the deployment strategy.

For potentially incompatible changes, prefer:

```text
Expand
    ↓
Compatible Application
    ↓
Data Migration
    ↓
Application Switch
    ↓
Contract
```

### Migration validation

When relevant, integration tests must validate:

* migration execution;
* schema compatibility;
* constraints;
* indexes;
* migrated data;
* application/JPA compatibility.

Use PostgreSQL Testcontainers for realistic integration validation.

---

# 15. JPA Integration

Follow the dedicated JPA/PostgreSQL instructions.

Pay particular attention to:

* lazy loading;
* N+1 queries;
* transaction boundaries;
* entity relationships;
* query performance;
* persistence context;
* optimistic/pessimistic locking.

Do not use Spring Boot configuration to hide persistence problems.

---

# 16. Caching

Caching must only be introduced when justified by:

* measured performance requirements;
* access patterns;
* acceptable staleness;
* invalidation strategy.

Do not introduce caching speculatively.

---

# 17. Scheduling

Scheduled jobs must:

* have explicit ownership;
* be observable;
* be idempotent where required;
* handle failures;
* avoid accidental concurrent execution when not supported.

Use scheduling only when appropriate.

For complex processing, prefer Spring Batch.

---

# 18. Observability

Applications must be observable in production.

The standard metrics stack is:

```text
Application
    ↓
Micrometer
    ↓
Spring Boot Actuator
    ↓
Prometheus
```

## 18.1 Standard Metrics

Before creating a custom metric, verify whether Spring Boot/Micrometer already provides the required information.

Existing metrics should be reused whenever possible.

Examples include metrics related to:

* HTTP requests;
* JVM;
* memory;
* CPU;
* connection pools;
* application infrastructure.

Do not create duplicate metrics without justification.

---

## 18.2 Custom Metrics

Custom metrics must have a clear purpose.

Use the appropriate Micrometer type:

### Counter

For monotonically increasing events:

```text
orders_created_total
orders_failed_total
```

### Timer

For duration and latency:

```text
order_processing_duration
```

### DistributionSummary

For measured distributions such as:

* payload size;
* order amount;
* batch item size.

### Gauge

For meaningful current-state values.

Use gauges carefully because their values represent current state rather than accumulated events.

---

## 18.3 Metric Tags

Metric tags must have bounded cardinality.

Do not use:

```text
customerId
orderId
UUID
email
requestId
user-controlled values
full URLs
```

as metric tags.

Prefer:

```text
operation
status
outcome
eventType
channel
```

Example:

```java
Timer.builder("order.processing")
        .tag("operation", "create")
        .tag("outcome", "success")
        .register(meterRegistry);
```

Avoid dynamic identifiers.

---

## 18.4 Sensitive Information

Never expose sensitive information through:

* metrics;
* metric tags;
* Actuator endpoints;
* logs.

Metric labels must not contain personal or confidential data.

---

## 18.5 Actuator

Use Spring Boot Actuator for operational endpoints.

The Prometheus endpoint should be exposed through the standard Actuator integration:

```text
/actuator/prometheus
```

Expose only the endpoints required by the application.

Secure Actuator endpoints appropriately.

Do not expose sensitive operational endpoints publicly without explicit justification.

---

## 18.6 Business Metrics

Business metrics should be defined when they provide operational value.

Examples:

```text
orders_created_total
orders_rejected_total
payments_failed_total
```

Business metrics must have:

* a clear name;
* a clear semantic meaning;
* bounded tags;
* an identified consumer.

Do not instrument every method.

---

## 18.7 Messaging Metrics

For SNS/SQS processing, consider metrics such as:

```text
messages_processed_total
messages_failed_total
message_processing_duration
```

where useful.

Prefer existing AWS/Micrometer instrumentation where available before creating custom metrics.

---

## 18.8 Batch Metrics

For Spring Batch, consider:

* job duration;
* step duration;
* items processed;
* items skipped;
* items failed;
* throughput.

Prefer existing Spring Batch/Micrometer instrumentation where available.

---

# 19. Health and Lifecycle

Applications must expose meaningful health information.

Health checks should allow ECS to determine whether the application is capable of serving traffic.

Consider:

* startup;
* readiness;
* liveness;
* graceful shutdown.

Do not make health checks depend on unnecessarily fragile external dependencies.

---

# 20. Graceful Shutdown

Applications must support graceful shutdown where appropriate.

Consider:

* in-flight HTTP requests;
* message processing;
* database transactions;
* batch processing;
* ECS task termination.

For message consumers, avoid acknowledging messages before processing has safely completed.

---

# 21. Testing Spring Boot Applications

Use the smallest Spring test scope appropriate.

Examples:

```text
@WebMvcTest
@DataJpaTest
@SpringBootTest
```

Use `@WebMvcTest` for controller behavior where appropriate.

Use `@DataJpaTest` for persistence-focused tests.

Use `@SpringBootTest` for application-level integration tests where necessary.

Use Testcontainers for PostgreSQL integration.

When Flyway is part of the production startup path, integration tests should validate the application against the migrated PostgreSQL schema rather than bypassing Flyway unnecessarily.

---

# 22. Forbidden Patterns

Avoid:

* field injection;
* controllers accessing repositories;
* business logic in controllers;
* business workflows in repositories;
* entities depending on infrastructure;
* service interfaces without a real need;
* unnecessary `ServiceImpl` classes;
* generic base services;
* uncontrolled caching;
* unbounded asynchronous processing;
* hard-coded configuration;
* secrets in source code;
* Hibernate schema auto-update for production;
* manual production schema changes;
* high-cardinality metric tags;
* exposing sensitive Actuator endpoints.

---

# 23. AI Validation Checklist

Before accepting AI-generated Spring Boot code, verify:

### Architecture

* [ ] Controller is thin.
* [ ] Business logic is in the appropriate service/entity.
* [ ] Repository contains persistence logic only.
* [ ] Entity does not depend on infrastructure.

### Persistence

* [ ] JPA mappings are appropriate.
* [ ] Queries are understood.
* [ ] N+1 risks are considered.
* [ ] Flyway migration exists when required.
* [ ] Hibernate is not being used to evolve the production schema.

### Transactions

* [ ] Transaction boundary is intentional.
* [ ] External calls are not accidentally held inside long transactions.
* [ ] Consistency requirements are understood.

### Testing

* [ ] Unit tests exist where appropriate.
* [ ] Integration tests exist where appropriate.
* [ ] Cucumber scenarios cover acceptance criteria.

### Security

* [ ] No secrets are present.
* [ ] Authorization is correct.
* [ ] Sensitive information is protected.

### Observability

* [ ] Existing Micrometer metrics were considered.
* [ ] Custom metrics have a clear purpose.
* [ ] Metric tags have bounded cardinality.
* [ ] No sensitive information is exposed.
* [ ] Actuator exposure is appropriate.

### Performance

* [ ] Queries are appropriate.
* [ ] External calls have timeouts.
* [ ] Concurrency is understood.
* [ ] Resource usage is considered.

### Operations

* [ ] Health checks are meaningful.
* [ ] Graceful shutdown is considered.
* [ ] Configuration is externalized.
