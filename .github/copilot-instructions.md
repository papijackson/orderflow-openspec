# GitHub Copilot Instructions

## Project Context

This project uses the following technology stack:

* Java 21
* Spring Boot 4
* Spring Data JPA
* PostgreSQL
* Flyway
* AWS ECS
* AWS SNS/SQS
* Spring Batch
* Maven
* GitHub
* GitHub Actions
* JUnit 5 / Jupiter
* Mockito
* AssertJ
* Testcontainers
* Cucumber / Gherkin
* Micrometer
* Spring Boot Actuator
* Prometheus

The project follows Specification-Driven Development.

The engineering constitution is authoritative and must be followed.

---

# 1. General Principles

Before modifying code:

1. Understand the existing implementation.
2. Read the relevant specification.
3. Read the technical plan.
4. Inspect existing tests.
5. Search for existing implementations and reusable components.
6. Understand existing architectural conventions.
7. Identify the smallest safe change required.

Prefer:

* simple solutions;
* readable code;
* explicit behavior;
* existing project patterns;
* small cohesive changes;
* testable implementations.

Avoid:

* unnecessary abstractions;
* speculative architecture;
* unrelated refactoring;
* duplicate implementations;
* unnecessary dependencies;
* premature optimization.

Do not invent business requirements.

If a requirement is ambiguous and materially affects the implementation, report the ambiguity instead of silently making a business decision.

---

# 2. Architecture

The default application architecture is:

```text id="e6jh8x"
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
```

### Controller

Controllers are responsible for:

* HTTP/API interaction;
* request validation;
* response mapping;
* protocol-specific concerns.

Controllers must remain thin.

Controllers must not:

* contain business logic;
* directly access repositories;
* manage database transactions;
* contain persistence logic.

### Service

Services are responsible for:

* application use cases;
* business rules;
* orchestration;
* transaction boundaries;
* coordination with repositories and external collaborators.

Services must not depend on HTTP response types.

### Repository

Repositories are responsible for:

* persistence;
* queries;
* database-specific operations.

Repositories must not contain business workflows.

### Entity

JPA Entities are the default application data model.

Entities:

* represent persistent business data;
* may contain business behavior and invariants;
* must not depend on infrastructure services;
* must not depend on controllers or repositories;
* must not depend directly on AWS clients.

Do not introduce a separate domain model by default.

Additional layers require explicit justification.

---

# 3. Specification-Driven Development

Follow this workflow:

```text id="l3b7pz"
Specification
    ↓
Clarification
    ↓
Technical Plan
    ↓
Tasks
    ↓
Implementation
    ↓
Tests
    ↓
Quality Gates
```

Do not skip directly from a business request to implementation when the feature is significant enough to require a specification.

Implementation must follow the approved plan.

If implementation reveals an inconsistency in the specification:

1. stop and identify the issue;
2. explain the impact;
3. update the appropriate specification/plan;
4. then continue.

Do not silently diverge from the specification.

---

# 4. Testing Policy

Every significant feature follows the baseline:

```text id="5lh2u6"
Unit Tests
    +
Integration Tests
    +
Cucumber Acceptance Tests
```

Tests must derive from:

* functional requirements;
* business rules;
* acceptance criteria;
* technical risks.

Use:

* JUnit Jupiter;
* Mockito;
* AssertJ;
* Spring Boot test support;
* Testcontainers;
* Cucumber/Gherkin.

Do not generate tests merely to increase coverage.

A test must provide meaningful evidence.

AI-generated tests must be reviewed for:

* correctness;
* independence;
* meaningful assertions;
* missing edge cases;
* false positives;
* implementation coupling.

---

# 5. Database and Flyway

PostgreSQL is the standard relational database.

**Flyway is mandatory for database evolution.**

Whenever a feature changes the database schema or requires a controlled data migration:

1. create or update a versioned Flyway migration;
2. review the impact on existing data;
3. review deployment compatibility;
4. update the JPA Entity if required;
5. update repositories if required;
6. add or update integration tests.

Typical migration location:

```text id="4dxpky"
src/main/resources/db/migration/
```

Example:

```text id="o9y8n4"
V20261010_001__add_order_status.sql
```

Do not use Hibernate schema generation as a production migration mechanism.

The following are forbidden for production schema evolution:

```text id="psd1fr"
ddl-auto=update
ddl-auto=create
ddl-auto=create-drop
```

Do not modify production databases manually.

### Destructive or incompatible changes

For changes such as:

* dropping columns;
* renaming columns;
* changing incompatible types;
* changing nullable columns;
* removing constraints;

consider:

* existing data;
* old application versions;
* rolling deployments;
* consumers;
* rollback;
* expand/contract strategy.

Preferred approach when compatibility requires multiple deployment steps:

```text id="s8zqfu"
Expand
   ↓
Deploy compatible version
   ↓
Migrate data
   ↓
Switch application usage
   ↓
Contract
```

---

# 6. JPA and PostgreSQL

Follow the dedicated JPA/PostgreSQL instructions.

In particular:

* understand generated SQL;
* avoid N+1 queries;
* use appropriate indexes;
* consider pagination;
* avoid uncontrolled entity graphs;
* use transactions deliberately;
* consider locking and concurrency;
* measure query performance where relevant.

Do not assume JPA automatically produces efficient SQL.

Database behavior must be validated using PostgreSQL where appropriate.

---

# 7. Messaging

For SNS/SQS:

* assume at-least-once delivery;
* design consumers to be idempotent;
* explicitly consider duplicates;
* define retry behavior;
* define DLQ behavior;
* consider visibility timeout;
* consider poison messages;
* consider ordering requirements;
* consider replay.

Do not assume database transactions automatically include message publication.

Consider Transactional Outbox when required by reliability/consistency requirements.

---

# 8. Spring Batch

For Spring Batch:

* understand expected volume;
* define chunking appropriately;
* consider restartability;
* define retry/skip behavior;
* consider idempotency;
* consider memory;
* consider database load;
* consider concurrency;
* verify failure recovery.

Do not introduce concurrency without understanding its effect on:

* database;
* memory;
* ordering;
* duplicate processing;
* downstream systems.

---

# 9. Observability and Metrics

Applications use the following standard metrics stack:

```text id="9nhgdx"
Application
    ↓
Micrometer
    ↓
Spring Boot Actuator
    ↓
Prometheus
```

Micrometer is the standard application metrics API.

Prometheus is the standard metrics exposition/collection target.

Spring Boot Actuator is the standard mechanism for exposing operational endpoints.

Prefer standard Spring Boot/Micrometer metrics before creating custom metrics.

Before adding a custom metric:

1. check whether an existing Micrometer metric already provides the information;
2. define the operational or business purpose;
3. choose the appropriate instrument;
4. define bounded tags;
5. verify that no sensitive information is exposed.

Use:

```text id="e9z5hz"
Counter
Timer
DistributionSummary
Gauge
```

according to the semantics of the measurement.

### Metric cardinality

Never use unbounded identifiers as metric tags.

Forbidden examples:

```text id="ih9tw3"
customerId
orderId
UUID
email
requestId
user-controlled values
```

Prefer bounded dimensions:

```text id="p6s4if"
operation
status
outcome
eventType
channel
```

Do not expose sensitive information through metrics.

### Actuator

Expose only the Actuator endpoints required by the application.

Secure operational endpoints appropriately.

The Prometheus endpoint should be exposed through the standard Actuator integration.

---

# 10. Performance

Performance must be considered before implementation is complete.

For database operations, consider:

* query complexity;
* N+1 queries;
* indexes;
* pagination;
* fetch strategy;
* connection pool usage;
* transaction duration.

For ECS:

* CPU;
* memory;
* task count;
* horizontal scaling;
* database connection multiplication.

For SNS/SQS:

* throughput;
* message size;
* consumer concurrency;
* queue lag;
* retry behavior.

For external services:

* timeout;
* retry;
* latency;
* failure behavior.

Do not claim performance improvements without measurement.

When a feature has explicit performance requirements, include performance validation in the implementation plan and tasks.

---

# 11. Security

Never:

* hard-code credentials;
* commit secrets;
* log passwords/tokens;
* expose sensitive data unnecessarily;
* grant unnecessary AWS permissions.

Use:

* externalized configuration;
* secret management;
* least privilege;
* input validation;
* appropriate authorization.

Security-sensitive behavior must be explicitly tested.

---

# 12. Existing Code and Dependencies

Before creating a new component:

1. search for an existing implementation;
2. search for similar use cases;
3. search for existing utilities;
4. inspect existing dependencies;
5. inspect existing configuration.

Prefer extending existing components when appropriate.

Do not introduce a new library if an existing project dependency provides the required capability.

Do not perform unrelated cleanup while implementing a feature.

---

# 13. Validation

Before declaring implementation complete, validate as appropriate:

```text id="4b2v5o"
Compile
  ↓
Unit Tests
  ↓
Integration Tests
  ↓
Cucumber Tests
  ↓
Architecture Tests
  ↓
Static Analysis
  ↓
Flyway Validation
  ↓
Security Checks
  ↓
Observability / Metrics Validation
```

The exact validation sequence may vary depending on the feature.

A feature is not complete merely because the application compiles.

---

# 14. Completion Reporting

When implementation is complete, report:

### Specification

```text id="w7sx4y"
Status:
Open questions:
```

### Implementation

```text id="m0b6mw"
Components created:
Components modified:
```

### Testing

```text id="h4y1i8"
Unit:
Integration:
Cucumber:
```

### Architecture

```text id="v0u2xl"
Architecture validation:
ArchUnit:
```

### Database

```text id="0xkn0k"
Flyway migration:
Migration validated:
Data impact:
```

### Performance

```text id="g3f5ck"
Performance considerations:
Performance validation:
```

### Security

```text id="b2k5xj"
Security validation:
```

### Observability

```text id="3r9h1x"
Metrics:
Logging:
Tracing:
```

### Remaining Issues

```text id="2jz1wo"
<unresolved issues>
```

Do not report a feature as complete if important validation remains unresolved.

---

# 15. Specialized Instructions

When working on specific areas, follow the corresponding instructions:

* Architecture:
  `.github/instructions/architecture.instructions.md`

* Java:
  `.github/instructions/java.instructions.md`

* Spring Boot:
  `.github/instructions/spring-boot.instructions.md`

* JPA / PostgreSQL / Flyway:
  `.github/instructions/jpa-postgresql.instructions.md`

* Testing:
  `.github/instructions/testing.instructions.md`

* Cucumber:
  `.github/instructions/cucumber.instructions.md`

* AWS:
  `.github/instructions/aws.instructions.md`

* Messaging:
  `.github/instructions/messaging.instructions.md`

* Spring Batch:
  `.github/instructions/spring-batch.instructions.md`

* Performance:
  `.github/instructions/performance.instructions.md`

* Security:
  `.github/instructions/security.instructions.md`

* Observability:
  `.github/instructions/observability.instructions.md`

Specialized instructions complement this file and must not contradict the engineering constitution.

---

# 16. Language and Documentation

Code, class names, method names and technical identifiers should follow project conventions.

Use English for:

* code;
* identifiers;
* technical documentation;
* logs;
* metric names;
* event names.

Business-facing terminology may follow the organization's established language.

Comments should explain **why**, not simply repeat what the code does.

Avoid comments that become stale as implementation evolves.

---

# 17. AI-Specific Rules

The AI agent must:

* inspect before modifying;
* search before creating;
* follow the specification;
* follow the technical plan;
* respect existing architecture;
* generate meaningful tests;
* validate generated code;
* identify uncertainty;
* avoid unrelated changes.

The AI agent must never:

* invent business rules;
* silently change API contracts;
* silently change database behavior;
* bypass Flyway;
* weaken tests;
* remove failing quality gates;
* introduce high-cardinality metrics;
* expose secrets or sensitive data;
* declare completion without validation.

When uncertain, prefer explicit clarification over silent assumptions.
