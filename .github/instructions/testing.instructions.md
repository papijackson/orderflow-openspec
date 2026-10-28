---
applyTo: "**/src/test/**/*.java,**/src/main/java/**/*.java,**/src/test/resources/**/*"
---

# Testing Instructions

## 1. Objective

Tests MUST provide evidence that the implementation satisfies the specification.

Tests MUST be derived primarily from:

1. Functional requirements.
2. Business rules.
3. Acceptance criteria.
4. Technical constraints.
5. Identified risks.
6. Error and boundary conditions.

Tests MUST NOT simply reproduce the implementation.

The implementation and its tests SHOULD be developed from the specification independently enough that tests can detect incorrect implementations.

---

# 2. Testing Strategy

Every significant feature MUST include the following testing levels unless an explicit exception is documented:

```text
Specification
      │
      ▼
Acceptance Criteria
      │
      ▼
Gherkin Scenarios
      │
      ├──────────────────┐
      ▼                  ▼
Unit Tests        Integration Tests
JUnit/Mockito     Spring/Testcontainers
      │                  │
      └─────────┬────────┘
                ▼
         Acceptance Tests
             Cucumber
```

The three levels have different responsibilities.

| Level       | Purpose                            | Typical tools                                |
| ----------- | ---------------------------------- | -------------------------------------------- |
| Unit        | Verify isolated behavior           | JUnit, Mockito, AssertJ                      |
| Integration | Verify real component interactions | Spring Boot Test, Testcontainers, PostgreSQL |
| Acceptance  | Verify business behavior           | Cucumber, Gherkin                            |

Do not use one level as a replacement for another when the behavior requires multiple levels.

---

# 3. Testability Before Implementation

Before implementing a significant feature, the agent MUST identify:

* The acceptance criteria.
* The main business scenarios.
* The error scenarios.
* The relevant unit-level behaviors.
* The infrastructure interactions.
* The integration scenarios.
* The required Cucumber scenarios.
* Performance-sensitive behavior.
* Relevant architecture constraints.

The agent SHOULD produce or update the test strategy before implementing complex functionality.

---

# 4. Acceptance Criteria First

Acceptance criteria MUST be observable and testable.

Bad:

```text
The service must use the repository correctly.
```

Good:

```text
When a customer creates a valid order,
the order is persisted and an order-created event is published.
```

Acceptance criteria SHOULD describe:

* Given conditions.
* When actions.
* Then observable outcomes.

Acceptance criteria MUST NOT prescribe implementation details unless those details are themselves requirements.

---

# 5. Gherkin as Business Contract

For significant business features, Cucumber/Gherkin MUST express the key acceptance scenarios.

Example:

```gherkin
Feature: Order creation

  Scenario: Create an order with valid products
    Given an active customer exists
    And the requested products are available
    When the customer creates an order
    Then the order is created
    And the order total is calculated correctly
```

Gherkin scenarios MUST use business language.

Avoid implementation-specific wording:

```gherkin
When OrderService.create() is called
Then OrderRepository.save() is invoked
```

Gherkin MUST describe behavior, not implementation.

---

# 6. Scenario Coverage

For each significant feature, consider at minimum:

1. Happy path.
2. Validation failure.
3. Business rule violation.
4. Not-found scenario where applicable.
5. Boundary conditions.
6. Duplicate or repeated operation.
7. External dependency failure where applicable.
8. Persistence failure where relevant.
9. Concurrency behavior where relevant.
10. Security/authorization behavior where relevant.

Not every category applies to every feature.

The agent MUST explicitly determine which categories apply.

---

# 7. Unit Testing

Unit tests MUST focus on isolated application behavior.

Typical targets include:

* Services.
* Business logic.
* Entity business rules when applicable.
* Mappers containing meaningful logic.
* Pure utility/domain components.

Unit tests MUST NOT require:

* PostgreSQL.
* AWS.
* Network access.
* External systems.
* A full Spring application context.

---

# 8. JUnit

Use JUnit Jupiter.

Prefer clear test names that describe behavior.

Example:

```java id="6u9l8x"
@Test
void shouldRejectOrderWhenCustomerIsInactive() {
    // ...
}
```

Avoid names such as:

```java id="u9k4od"
@Test
void test1() {
    // ...
}
```

Tests SHOULD follow:

```text
Arrange
Act
Assert
```

Example:

```java id="r0r9zo"
@Test
void shouldCreateOrderWhenRequestIsValid() {

    // Arrange
    var command = validCreateOrderCommand();

    when(orderRepository.save(any(OrderEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    var result = orderService.create(command);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
}
```

---

# 9. AssertJ

AssertJ SHOULD be used for expressive assertions.

Prefer:

```java id="e5m3l6"
assertThat(order.getStatus())
        .isEqualTo(OrderStatus.CONFIRMED);
```

over unnecessarily verbose JUnit assertions when AssertJ is already part of the project.

Assertions MUST communicate the expected behavior clearly.

---

# 10. Mockito

Mockito SHOULD be used to isolate external collaborators in unit tests.

Mock:

* Repositories.
* External clients.
* Messaging publishers.
* Infrastructure dependencies.

Do NOT mock:

* The class under test.
* Simple value objects.
* Domain entities without a specific reason.
* Every dependency merely because Mockito is available.

Mocks SHOULD represent meaningful boundaries.

---

# 11. Avoid Over-Mocking

Avoid tests containing excessive interaction verification such as:

```java id="m5w6e8"
verify(repository).save(entity);
verify(repository).findById(id);
verify(client).call(...);
verify(mapper).map(...);
verify(logger).info(...);
```

when these interactions are implementation details rather than behavior.

Prefer asserting observable outcomes.

Interaction verification SHOULD be used when the interaction itself is part of the required behavior.

Examples:

* An event MUST be published.
* A payment provider MUST NOT be called after validation failure.
* A repository MUST NOT be called when a precondition fails.
* A retry MUST occur under a defined failure condition.

---

# 12. Test Data

Test data MUST be explicit and meaningful.

Prefer builders or factory methods when they improve readability.

Example:

```java id="4xxv0k"
var order = anOrder()
        .forCustomer(activeCustomer())
        .withLine(product("P1"), 2)
        .build();
```

Test factories MUST NOT hide important scenario details.

Avoid giant generic fixtures containing dozens of irrelevant properties.

Each test SHOULD make its relevant conditions obvious.

---

# 13. Test Independence

Tests MUST be independent.

Tests MUST NOT depend on:

* Execution order.
* Shared mutable state.
* Another test having executed first.
* Developer-specific configuration.
* A running local database.
* External services.

Tests SHOULD be deterministic.

---

# 14. Time

Time-dependent behavior MUST be testable deterministically.

Avoid direct calls to:

```java id="v4l0yt"
Instant.now()
```

inside business logic when time affects behavior.

Prefer an injectable `Clock`.

Example:

```java id="n0hknh"
public class OrderService {

    private final Clock clock;

    public OrderService(Clock clock) {
        this.clock = clock;
    }
}
```

Tests can then use a fixed clock.

Avoid arbitrary sleeps such as:

```java id="c14k2m"
Thread.sleep(1000);
```

to make asynchronous tests pass.

---

# 15. Exception Testing

Tests MUST verify meaningful failure behavior.

Example:

```java id="ay2p7h"
assertThatThrownBy(() -> orderService.create(command))
        .isInstanceOf(CustomerInactiveException.class);
```

Tests SHOULD verify relevant properties of the error.

Do not assert only that "some exception" occurred when the exception type or business error is meaningful.

Avoid testing stack traces or exact technical error messages unless they are part of a contract.

---

# 16. Parameterized Tests

Use parameterized tests when several inputs share the same behavioral rule.

Example:

```java id="dzb2x4"
@ParameterizedTest
@ValueSource(strings = {"", " ", "invalid"})
void shouldRejectInvalidCustomerIdentifiers(String customerId) {
    // ...
}
```

Do not use parameterized tests when the scenarios have significantly different business meaning.

Readability is more important than reducing test method count.

---

# 17. Integration Tests

Integration tests MUST validate real interactions between application components.

Typical integration targets include:

* Spring configuration.
* Spring Data JPA.
* PostgreSQL.
* Transactions.
* Messaging infrastructure.
* Serialization.
* External integration boundaries.

Integration tests MUST use the smallest Spring context appropriate to the behavior being tested.

---

# 18. Testcontainers

Use Testcontainers for infrastructure dependencies when realistic behavior matters.

For PostgreSQL:

```java id="t5h2cd"
@Testcontainers
class OrderRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");
}
```

The PostgreSQL version MUST match the project's supported production version as closely as practical.

Do not depend on a developer's local PostgreSQL installation.

---

# 19. Database Integration Tests

Database integration tests SHOULD verify:

* Entity mappings.
* Repository queries.
* Constraints.
* Relationships.
* Transactions.
* Pagination.
* Sorting.
* Locking when relevant.
* Migration compatibility.
* Important PostgreSQL-specific behavior.

Mockito MUST NOT be used to test whether a repository query actually works.

A mocked repository verifies application interaction, not SQL correctness.

---

# 20. Spring Test Slices

Use focused Spring test slices when appropriate.

Typical choices:

```text
@WebMvcTest
    → Controller/web behavior

@DataJpaTest
    → JPA/repository behavior

@SpringBootTest
    → Full application integration
```

Do not use `@SpringBootTest` by default.

Use the smallest context that provides meaningful evidence.

---

# 21. Full Application Tests

`@SpringBootTest` SHOULD be reserved for scenarios requiring multiple application components together.

Appropriate cases include:

* End-to-end application wiring.
* Complex transaction flows.
* Messaging integration.
* Configuration validation.
* Multiple infrastructure components.

Full-context tests SHOULD remain limited because they are more expensive and can become fragile.

---

# 22. REST API Testing

REST endpoints MUST be tested at the appropriate level.

Controller tests SHOULD verify:

* Request validation.
* HTTP status.
* Request mapping.
* Response structure.
* Error mapping.
* Security behavior where relevant.

Integration tests SHOULD verify important end-to-end API behavior when required.

Do not test business rules exclusively through controller tests.

Business rules SHOULD primarily be tested at service/unit level.

---

# 23. Repository Tests

Repository tests MUST execute against PostgreSQL when SQL behavior matters.

They SHOULD verify:

* Query correctness.
* Mapping behavior.
* Constraints.
* Pagination.
* Sorting.
* Fetch behavior.

Repository mocks MUST NOT be considered sufficient repository testing.

---

# 24. Messaging Tests

For SNS/SQS features, tests MUST consider:

* Message serialization.
* Message deserialization.
* Contract compatibility.
* Duplicate delivery.
* Idempotency.
* Retry behavior.
* Failure handling.
* Dead-letter behavior where applicable.

Consumer unit tests SHOULD isolate business behavior.

Integration tests SHOULD validate the messaging infrastructure behavior when realistic infrastructure behavior matters.

---

# 25. Idempotency Tests

Any operation that can be invoked more than once MUST be evaluated for idempotency.

For example:

```text
Given an order-created message
When the same message is delivered twice
Then the order must not be created twice
```

The exact expected behavior MUST come from the specification.

Idempotency MUST be tested when duplicate processing could affect business correctness.

---

# 26. Cucumber Tests

Cucumber tests MUST represent business behavior.

Feature files SHOULD be organized around business capabilities rather than technical classes.

Prefer:

```text
features/
├── order-creation.feature
├── order-cancellation.feature
└── order-payment.feature
```

over:

```text
features/
└── OrderService.feature
```

Feature files MUST remain readable by non-developers familiar with the business domain.

---

# 27. Gherkin Style

Use:

```text
Feature
Scenario
Given
And
When
Then
```

Scenarios SHOULD be:

* Short.
* Independent.
* Business-readable.
* Deterministic.
* Focused on one behavior.

Avoid overly long scenarios containing many unrelated business operations.

Avoid implementation details.

---

# 28. Step Definitions

Step definitions MUST remain thin.

They SHOULD delegate behavior to application-level components or test helpers.

Do not put substantial business logic inside Cucumber step definitions.

Bad:

```java id="ny5c2x"
@When("the customer creates an order")
void createOrder() {
    // 100 lines of implementation
}
```

Good:

```java id="z3g7n9"
@When("the customer creates an order")
void createOrder() {
    testContext.createOrder();
}
```

The exact implementation depends on the project's testing architecture.

---

# 29. Cucumber and Unit Tests

Cucumber MUST NOT replace unit tests.

A Cucumber scenario verifies an observable business behavior.

Unit tests verify detailed business rules and edge cases.

For example:

```text
Cucumber
    → "An order cannot be created without products"

Unit tests
    → empty list
    → null list
    → one invalid product
    → unavailable product
    → maximum number of products
```

The appropriate level depends on the specification and risk.

---

# 30. Test Naming and Organization

Tests SHOULD follow the same conceptual organization as production code.

Prefer:

```text
OrderServiceTest
OrderRepositoryIT
OrderControllerTest
CreateOrderSteps
```

Names MUST clearly communicate:

* What is tested.
* At which level.
* What behavior is expected.

Use suffixes such as:

```text
Test
IT
IntegrationTest
```

according to project conventions.

---

# 31. Test Duplication

Avoid testing the exact same implementation behavior at every level.

For example, do not duplicate every service unit test as a Cucumber scenario.

Each level should provide distinct evidence.

A business rule MAY intentionally be tested at multiple levels when the additional confidence justifies the cost.

---

# 32. Coverage

Code coverage MUST be used as a diagnostic metric, not as the sole definition of test quality.

High coverage with weak assertions is insufficient.

The agent SHOULD prioritize coverage of:

* Business rules.
* Error paths.
* Boundary conditions.
* Security-sensitive behavior.
* Data integrity.
* Important integrations.

Coverage thresholds MAY be enforced by CI according to project policy.

---

# 33. Mutation Testing

Mutation testing MAY be introduced for critical business components.

It can be used to identify tests that execute code without detecting meaningful behavioral changes.

Mutation testing SHOULD be targeted rather than necessarily executed across the entire codebase.

---

# 34. Performance Tests

Performance requirements MUST be tested separately from ordinary functional tests when appropriate.

Do not introduce arbitrary performance assertions into unit tests.

Performance tests SHOULD define:

* Workload.
* Dataset size.
* Concurrency.
* Measurement methodology.
* Thresholds.
* Environment assumptions.

Performance tests MUST distinguish local measurements from production-like measurements.

---

# 35. Concurrency Tests

Concurrency-sensitive behavior MUST be tested when required.

Examples include:

* Optimistic locking.
* Duplicate message processing.
* Concurrent updates.
* Idempotency.
* Batch processing.
* Scheduled jobs.

Avoid flaky timing-based concurrency tests.

Prefer synchronization primitives and deterministic coordination.

---

# 36. Test Isolation from External Services

Tests SHOULD NOT depend on external production-like services unless the test explicitly belongs to an environment-level validation stage.

Use:

* Testcontainers.
* Test doubles.
* Local emulators where appropriate.
* Contract tests.

Do not silently call production services from automated tests.

---

# 37. Test Failures

When a test fails, the agent MUST investigate the failure.

Do NOT:

* Remove the test.
* Weaken assertions.
* Increase arbitrary timeouts.
* Add sleeps.
* Mock away the failing behavior.
* Change expected results merely to make the test pass.

The agent MUST determine whether:

1. The implementation is incorrect.
2. The test is incorrect.
3. The specification is ambiguous or incorrect.
4. The environment is incorrect.

The appropriate fix MUST follow from that analysis.

---

# 38. Test-Driven Development and SDD

The project does not require strict TDD for every change.

However, the agent SHOULD use a specification-driven testing sequence:

```text
Specification
     ↓
Acceptance Criteria
     ↓
Gherkin scenarios
     ↓
Unit / integration test design
     ↓
Implementation
     ↓
Test execution
     ↓
Conformance review
```

For complex business logic, writing tests before or alongside implementation is strongly preferred.

---

# 39. AI-Specific Testing Rules

AI-generated tests MUST be treated as untrusted until reviewed.

The agent MUST verify that:

* Tests correspond to requirements.
* Assertions are meaningful.
* Edge cases are covered.
* Tests do not merely reproduce implementation details.
* Mocks are appropriate.
* Integration tests use real infrastructure when required.
* Cucumber scenarios describe business behavior.
* Tests fail when the relevant behavior is intentionally broken.

The agent SHOULD actively challenge its own tests by asking:

```text
Would this test fail if the implementation were subtly incorrect?
```

and:

```text
Which incorrect implementation could still pass these tests?
```

---

# 40. Mutation-Oriented Test Review

For important behaviors, the agent SHOULD mentally or explicitly consider mutations such as:

* Removing a validation.
* Reversing a condition.
* Changing a calculation.
* Returning the wrong status.
* Skipping persistence.
* Publishing an incorrect event.
* Processing a duplicate message twice.
* Ignoring an exception.

The test suite SHOULD detect these changes.

---

# 41. Required Validation Before Completion

Before declaring a feature complete, the agent MUST:

1. Verify all acceptance criteria.
2. Verify all required Gherkin scenarios.
3. Verify unit tests.
4. Verify integration tests.
5. Verify architecture tests when applicable.
6. Verify persistence behavior when applicable.
7. Verify messaging behavior when applicable.
8. Verify security-related behavior when applicable.
9. Verify performance requirements when applicable.
10. Execute the appropriate Maven validation.

Typical validation:

```text
mvn test
mvn verify
```

Additional project-specific commands MUST be executed when required.

---

# 42. Test Completion Report

When completing a significant feature, the agent SHOULD report:

```text
Acceptance criteria:
- AC-01: PASS
- AC-02: PASS
- AC-03: PASS

Unit tests:
- PASS

Integration tests:
- PASS

Acceptance tests:
- PASS

Architecture checks:
- PASS

Performance checks:
- NOT REQUIRED

Security checks:
- PASS

Known limitations:
- ...
```

The agent MUST NOT report PASS for checks that were not executed.

---

# 43. Forbidden Testing Patterns

Copilot MUST NOT generate:

* Tests with meaningless assertions.
* Tests that only verify mocks were called.
* Tests that reproduce implementation details without validating behavior.
* `Thread.sleep()` to stabilize asynchronous tests.
* Tests dependent on execution order.
* Tests depending on a developer's local database.
* Tests calling production services.
* Tests with disabled assertions.
* Tests silently catching exceptions.
* Tests removed solely because they fail.
* Arbitrary timeout increases to hide failures.
* Excessive mocking of the system under test.
* Cucumber scenarios describing Java implementation details.

---

# 44. Decision Rule

When deciding how to test a behavior, prefer the smallest set of tests that provides strong evidence at the appropriate levels.

The test strategy MUST balance:

* Confidence.
* Maintainability.
* Execution time.
* Diagnostic value.
* Production risk.

The objective is not to maximize the number of tests.

The objective is to maximize confidence that the implementation satisfies the specification.
