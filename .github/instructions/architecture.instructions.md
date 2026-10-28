---
applyTo: "**/src/main/java/**/*.java,**/src/test/java/**/*.java"
---

# Architecture Instructions — Layered Spring Boot Applications

## 1. Objective

Enforce a simple, consistent and maintainable layered architecture for Java and Spring Boot applications.

The default architecture is:

`Controller -> Service -> Repository -> Entity`

The architecture MUST remain simple and proportional to the application's business complexity.

Do not introduce Clean Architecture, Hexagonal Architecture, CQRS or additional architectural layers by default.

## 2. Package Organization

Use the following package organization as the default for a service-oriented application:

```text
com.company.application
├── controller
│   ├── OrderController.java
│   └── dto
│       ├── CreateOrderRequest.java
│       └── OrderResponse.java
├── service
│   └── OrderService.java
├── repository
│   └── OrderRepository.java
├── entity
│   └── OrderEntity.java
├── exception
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── mapper
│   └── OrderMapper.java
├── client
│   └── ExternalServiceClient.java
├── messaging
│   ├── publisher
│   └── consumer
├── batch
│   ├── configuration
│   ├── reader
│   ├── processor
│   └── writer
└── configuration
```

This structure is a default, not a mandatory requirement to create every package.

Create a package only when the corresponding responsibility exists.

Follow the existing repository structure when it already establishes a consistent convention.

Do not reorganize an entire application solely to match this example.

For larger applications, package organization MAY be adapted to business features, provided the layer boundaries remain explicit.

## 3. Controller Responsibilities

Controllers MUST handle interface-specific concerns.

Responsibilities include:

* Receiving HTTP requests.
* Validating incoming request structures.
* Mapping requests to service inputs.
* Calling application services.
* Mapping results to HTTP responses.
* Selecting appropriate HTTP status codes.
* Handling protocol-specific concerns.

Controllers MUST NOT:

* Contain business rules.
* Execute business workflows directly.
* Access Spring Data repositories.
* Execute persistence queries.
* Manage complex transaction workflows.
* Publish business events directly when publication belongs to the application service.
* Contain substantial data transformation logic.

Controllers SHOULD remain thin.

Example:

```java
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
class OrderController {

private final OrderService orderService;

@PostMapping
ResponseEntity<OrderResponse> create(
@Valid @RequestBody CreateOrderRequest request) {

OrderResponse response = orderService.create(request);

return ResponseEntity.status(HttpStatus.CREATED)
.body(response);
}
}
```

The example is illustrative. Use the project's actual DTO, visibility and mapping conventions.

## 4. Service Responsibilities

Services MUST orchestrate application use cases.

Responsibilities include:

* Applying business rules.
* Coordinating repositories and external dependencies.
* Enforcing business invariants.
* Managing appropriate transaction boundaries.
* Coordinating persistence and event publication.
* Handling application-level error scenarios.

Services MUST NOT:

* Depend on controllers.
* Contain HTTP-specific response handling.
* Return `ResponseEntity` as an application result.
* Delegate all meaningful logic to a collection of unnecessary helper classes.
* Introduce abstractions without a concrete benefit.

Services SHOULD be cohesive and focused on related application behavior.

Use constructor injection.

Use `@Transactional` at an appropriate application-service boundary when a database transaction is required.

Do not use `@Transactional` indiscriminately on every method.

For operations involving database changes and external messaging, explicitly consider transaction consistency and failure recovery.

## 5. Repository Responsibilities

Repositories MUST encapsulate persistence operations.

Spring Data JPA repositories SHOULD be used for standard CRUD operations and straightforward queries.

Repositories MAY define custom queries when justified by data access requirements.

Repositories MUST NOT:

* Implement business workflows.
* Depend on controllers.
* Publish application events.
* Orchestrate calls to external services.
* Contain unrelated application logic.

Repository methods SHOULD communicate their intent clearly.

Avoid loading unnecessary data.

Use pagination, projections or targeted queries when they better match the access requirements.

## 6. Entity Responsibilities

JPA entities are the default application data model.

Entities MUST:

* Represent persistent business data.
* Respect relational integrity.
* Define relationships deliberately.
* Use appropriate identifiers and constraints.
* Avoid exposing persistence details unnecessarily to external APIs.

Entities MAY contain business behavior when that behavior is naturally associated with the entity and does not require infrastructure orchestration.

Entities MUST NOT depend on controllers, repositories or application services.

Avoid turning entities into passive data structures when a simple business invariant can be expressed directly in the entity.

However, do not move workflows, external calls or repository access into entities.

Do not create a separate domain model by default.

Separate models MAY be introduced when required by:

* A public API contract.
* Different read and write representations.
* Serialization constraints.
* Security or data-exposure concerns.
* Complex mapping requirements.
* An explicit architectural decision.

## 7. DTOs and Mapping

Use DTOs at external boundaries when they provide a meaningful separation between the API contract and the persistence model.

For REST APIs:

* Request DTOs represent accepted input.
* Response DTOs represent the public output contract.
* Entities remain persistence-aware objects.

Do not expose entities directly when that would expose internal fields, persistence relationships or an unstable database representation.

Use explicit mapping.

Manual mapping is acceptable for simple mappings.

A mapping library MAY be used when the project already standardizes it or when mapping complexity justifies it.

Do not introduce a mapper class for trivial transformations unless it improves clarity or follows an established project convention.

## 8. Dependency Rules

The following dependencies are allowed:

| Source        | Allowed dependencies                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| Controller    | Service, request/response DTOs, validation and web-related components                                |
| Service       | Entity, repository, application collaborators, messaging abstractions and external client interfaces |
| Repository    | Entity, Spring Data JPA and persistence-specific components                                          |
| Entity        | Java domain data types and persistence annotations                                                   |
| Mapper        | Source and target models required for mapping                                                        |
| Configuration | Components and framework types required for configuration                                            |

The following dependencies are forbidden:

* Controller -> Repository.
* Repository -> Controller.
* Repository -> Service.
* Entity -> Controller.
* Entity -> Service.
* Entity -> Repository.
* Service -> Controller.
* Service -> HTTP response types.
* Entity -> AWS SDK clients.
* Entity -> HTTP clients.
* Entity -> Spring application services.

Services MAY coordinate messaging publishers and external clients when required by a use case.

External infrastructure components MUST NOT introduce reverse dependencies into controllers or entities.

## 9. Interfaces and Abstractions

Do not create an interface for every service or repository implementation.

Avoid unnecessary pairs such as:

```text
OrderService
OrderServiceImpl
```

when there is only one implementation and no meaningful abstraction requirement.

Spring Data JPA repository interfaces are an established exception.

Introduce interfaces when they provide a concrete benefit, such as:

* Multiple meaningful implementations.
* An external system boundary.
* A stable contract between independently evolving components.
* A testing seam that cannot be achieved more simply.
* An existing project convention.
* An explicit architectural requirement.

Do not introduce ports and adapters merely to follow a pattern.

## 10. External Services and Messaging

Controllers, services and repositories MUST retain their defined responsibilities when AWS integrations are involved.

REST controllers MUST NOT directly instantiate AWS SDK clients.

Repositories MUST NOT publish SNS messages.

SQS listeners MUST NOT become containers for complex business workflows.

Message consumers SHOULD validate incoming messages and delegate application behavior to services.

Services SHOULD coordinate business processing, persistence and event publication.

When reliable consistency between database changes and event publication is required, evaluate a transactional outbox or another explicitly justified consistency mechanism.

The appropriate design MUST be documented in the technical plan.

## 11. Transactions

Transactions MUST correspond to coherent application operations.

Prefer defining transaction boundaries in the service layer.

Avoid:

* Unnecessarily long transactions.
* External network calls inside database transactions without justification.
* Transactions spanning unrelated business operations.
* Assuming that a database transaction also makes SNS/SQS publication atomic.

For operations involving multiple resources, document the required consistency guarantees and failure behavior.

## 12. Error Handling

Use explicit and meaningful exception handling.

Business exceptions SHOULD represent recognizable business failures.

Technical exceptions MUST NOT be silently converted into successful results.

HTTP-specific exception mapping belongs at the web boundary, commonly through a global exception handler.

Do not introduce broad exception-catching blocks that hide failures.

Preserve useful diagnostic information without exposing sensitive data.

## 13. Testing Architecture

Architecture tests SHOULD use ArchUnit where supported by the project.

At minimum, enforce:

* Controllers do not depend directly on repositories.
* Repositories do not depend on services or controllers.
* Entities do not depend on application or infrastructure components.
* Services do not depend on controllers.
* Package dependencies remain consistent with the declared layering.

Architecture rules MUST be validated in CI.

Tests MUST inspect actual compiled dependencies rather than relying exclusively on naming conventions or documentation.

## 14. Forbidden Patterns

Unless explicitly justified, Copilot MUST NOT generate:

* Unnecessary interfaces for single implementations.
* Empty service classes that only forward every call.
* Business logic inside controllers.
* Business workflows inside repositories.
* Entities that depend on infrastructure services.
* Generic base classes without a clear reuse requirement.
* Unnecessary factories, facades, mediators or dispatchers.
* Additional architectural layers without a concrete benefit.
* Direct AWS SDK calls from controllers or entities.
* Unbounded entity graph loading.
* Unrelated refactoring.

## 15. Required Validation

Before completing an architecture-related change:

1. Verify the package structure.
2. Verify the responsibilities of modified classes.
3. Verify allowed dependencies.
4. Verify transaction boundaries.
5. Verify API and persistence separation.
6. Verify integration boundaries.
7. Run the relevant unit and architecture tests.
8. Report any architectural exception and its justification.

Do not claim that architecture compliance has been verified unless the relevant checks were executed.

## 16. Decision Rule

When several designs satisfy the requirements, prefer the simplest design that:

* Satisfies the specification.
* Preserves data integrity.
* Meets security requirements.
* Meets relevant performance constraints.
* Remains testable and maintainable.
* Fits the existing architecture.

Architectural sophistication is not a goal in itself.
