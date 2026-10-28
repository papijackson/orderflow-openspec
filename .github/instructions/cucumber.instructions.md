---
applyTo: "**/src/test/resources/**/*.feature,**/src/test/java/**/*Cucumber*.java,**/src/test/java/**/*Step*.java,**/src/test/java/**/*Steps*.java"
---

# Cucumber / Gherkin Development Guidelines

## 1. Purpose

Cucumber tests are the executable expression of the business requirements and acceptance criteria defined in the specification.

They must validate **observable business behavior**, not implementation details.

The preferred flow is:

```text
Specification
    ↓
Acceptance Criteria
    ↓
Gherkin Scenarios
    ↓
Cucumber Step Definitions
    ↓
Application
    ↓
Observable Business Result
```

Cucumber scenarios must remain understandable by a developer, product owner, business analyst, or other stakeholder who understands the business domain.

---

# 2. Relationship with Specification-Driven Development

For every significant feature developed through SDD:

```text
spec.md
   │
   ├── Functional requirements
   │
   ├── Business rules
   │
   └── Acceptance criteria
             │
             ▼
       feature file
             │
             ▼
      Cucumber scenarios
             │
             ▼
       implementation
```

Do not invent acceptance criteria while writing Cucumber tests.

If an expected behavior is unclear:

1. identify the ambiguity;
2. ask for clarification when possible;
3. otherwise document the assumption;
4. do not silently choose behavior only because it is convenient to implement.

---

# 3. Gherkin Language

Use Gherkin keywords consistently:

```gherkin
Feature:
  Background:
  Scenario:
  Scenario Outline:
  Given
  When
  Then
  And
  But
  Examples:
```

Use the language that is appropriate for the project and team.

The language of the scenario must reflect the business vocabulary defined by the specification.

Avoid mixing technical terminology with business terminology unless the technical concept is itself part of the business behavior.

---

# 4. Feature Structure

Each feature should represent a coherent business capability.

Preferred structure:

```gherkin
Feature: Create an order

  As a customer
  I want to create an order
  So that I can purchase products

  Scenario: Create an order with available products
    Given the customer exists
    And the requested products are available
    When the customer creates an order
    Then the order is created
    And the order contains the requested products
    And the order total is calculated correctly
```

The feature description should explain:

* who benefits from the capability;
* what the capability provides;
* why it exists.

Avoid technical feature names such as:

```gherkin
Feature: POST /orders
Feature: OrderController
Feature: OrderService
Feature: Save order in PostgreSQL
```

Prefer:

```gherkin
Feature: Create an order
```

---

# 5. Scenario Design

A scenario should describe one meaningful business behavior.

A good scenario generally follows:

```text
Given → initial business context
When  → business action
Then  → observable outcome
```

Example:

```gherkin
Scenario: Reject an order when a product is unavailable
  Given the customer exists
  And the requested product is unavailable
  When the customer creates an order
  Then the order is rejected
  And the customer is informed that the product is unavailable
```

Avoid scenarios that contain unrelated behaviors.

Bad:

```gherkin
Scenario: Create order and send email and update inventory and publish event
```

unless those outcomes are explicitly part of the same acceptance criterion.

Prefer separate scenarios when they represent independent business rules.

---

# 6. Business Language over Technical Language

Cucumber scenarios should describe what the system does, not how it does it.

Avoid:

```gherkin
Given a row exists in the orders table
When POST /orders is called
Then HTTP status 201 is returned
```

Prefer:

```gherkin
Given the customer is eligible to place an order
When the customer places an order
Then the order is created
```

HTTP status codes may be included when the API contract itself is part of the acceptance criterion.

For example:

```gherkin
Scenario: Reject an invalid order request
  Given the order does not contain any product
  When the customer submits the order
  Then the request is rejected
  And the response status is 400
```

The business behavior remains primary.

---

# 7. Given / When / Then Responsibilities

## Given

`Given` establishes the initial context.

It may describe:

* existing business entities;
* relevant configuration;
* business state;
* external system state;
* required permissions;
* existing events or messages.

Example:

```gherkin
Given the customer has an active account
And the customer has sufficient credit
```

Avoid implementation details:

```gherkin
Given customerRepository.findById returns a customer
```

---

## When

`When` describes the business action being tested.

Prefer one primary action:

```gherkin
When the customer places the order
```

Avoid:

```gherkin
When the controller receives the request
And the service calls the repository
And the repository saves the entity
```

Those are implementation details.

---

## Then

`Then` describes observable outcomes.

Examples:

```gherkin
Then the order is created
And the order total is 120 euros
```

or:

```gherkin
Then the payment is rejected
And the order remains pending
```

Avoid verifying internal implementation:

```gherkin
Then OrderRepository.save is called once
```

This belongs in unit tests, not acceptance tests.

---

# 8. Scenario Independence

Scenarios must be independent.

Do not rely on execution order:

```text
Scenario A creates customer
Scenario B uses customer created by Scenario A
```

Each scenario must establish its own required state.

This allows:

* parallel execution;
* isolated debugging;
* deterministic results;
* easier CI execution.

---

# 9. Test Data

Test data must be explicit and understandable.

Prefer:

```gherkin
Given the following products exist:
  | product | price | stock |
  | BOOK-1  | 20    | 10    |
  | BOOK-2  | 30    | 5     |
```

over opaque technical identifiers:

```gherkin
Given product ID "a7c83b2e-..."
```

Technical identifiers may be used internally by step definitions when necessary, but they should not dominate the feature file.

Do not use production data.

Avoid large shared datasets.

Prefer the smallest dataset necessary to express the scenario.

---

# 10. Scenario Outlines

Use `Scenario Outline` when the same business behavior must be validated against multiple meaningful inputs.

Example:

```gherkin
Scenario Outline: Reject an invalid order
  Given the order contains <quantity> products
  When the customer submits the order
  Then the order is rejected

  Examples:
    | quantity |
    | 0        |
    | -1       |
```

Do not use Scenario Outline simply to avoid writing two or three scenarios when the resulting scenarios become harder to understand.

Business clarity takes priority over reducing lines.

---

# 11. Background

Use `Background` only for context genuinely shared by all scenarios.

Good:

```gherkin
Background:
  Given the customer has an active account
```

Avoid large Background sections containing most of the test setup.

A scenario should remain understandable when read independently.

If the Background becomes large, reconsider the feature structure.

---

# 12. Tags

Use tags to classify scenarios when useful.

Recommended categories include:

```gherkin
@acceptance
@integration
@api
@messaging
@batch
@security
@performance
@critical
```

Examples:

```gherkin
@acceptance
Feature: Create an order
```

or:

```gherkin
@critical
Scenario: Reject an order when the account is blocked
```

Tags must have an agreed meaning within the project.

Do not create tags arbitrarily.

Avoid excessive tagging.

---

# 13. REST API Acceptance Tests

For REST APIs, Cucumber should validate the API contract and business behavior.

Example:

```gherkin
Scenario: Create an order through the REST API
  Given the customer exists
  And the requested products are available
  When the client creates the order
  Then the response status is 201
  And the order is returned
  And the order contains the requested products
```

Validate only contractually relevant fields.

Avoid asserting every implementation detail of the JSON response.

If a field is irrelevant to the business behavior, do not unnecessarily assert it.

---

# 14. Persistence

Cucumber tests may validate persistence when persistence is part of the observable behavior.

Example:

```gherkin
Scenario: Created order can be retrieved
  Given the customer exists
  When the customer creates an order
  Then the order is created
  And the customer can retrieve the order
```

This is preferable to:

```gherkin
Then the order exists in the orders table
```

Database-level assertions belong primarily to integration tests.

Use Cucumber to verify business behavior across the application boundary.

---

# 15. Messaging / SNS / SQS

For event-driven features, scenarios must explicitly consider asynchronous behavior.

Example:

```gherkin
Scenario: Publish an order-created event
  Given the customer successfully creates an order
  When the order creation is completed
  Then an order-created event is published
```

When relevant, test:

* event content;
* event type/version;
* correlation identifier;
* idempotency;
* duplicate delivery;
* retry behavior;
* failure handling;
* dead-letter behavior;
* eventual consistency.

Example:

```gherkin
Scenario: Processing the same event twice does not duplicate the order
  Given an order-created event has been received
  When the same event is received again
  Then the order is processed only once
```

Do not make asynchronous tests depend on arbitrary sleeps.

Prefer deterministic synchronization and polling with a bounded timeout.

---

# 16. External Systems

When a feature interacts with an external service:

* do not call a real production service;
* use controlled test doubles, stubs, containers, or local infrastructure;
* explicitly test success and failure behavior;
* test timeout behavior where relevant;
* test retry behavior where relevant.

Example:

```gherkin
Scenario: Create an order when the payment service is unavailable
  Given the payment service is unavailable
  When the customer places an order
  Then the order remains pending
  And the customer is informed that payment could not be completed
```

The scenario must validate the application's behavior, not the internals of the test double.

---

# 17. Security Scenarios

Security-relevant behavior must be represented explicitly when applicable.

Examples:

```gherkin
Scenario: Reject an unauthenticated request
  Given the client is not authenticated
  When the client requests the customer's orders
  Then the request is rejected
```

```gherkin
Scenario: Reject access to another customer's order
  Given the customer is authenticated
  And the requested order belongs to another customer
  When the customer requests the order
  Then access is denied
```

Security acceptance scenarios should cover authorization boundaries, not implementation mechanisms.

---

# 18. Error Scenarios

Every significant business capability should consider failure cases.

At minimum, consider where applicable:

* invalid input;
* missing resource;
* business rule violation;
* duplicate request;
* unauthorized access;
* external service failure;
* timeout;
* persistence failure;
* duplicate event;
* invalid event;
* concurrency conflict.

Do not force irrelevant scenarios.

The specification and risk profile determine the required coverage.

---

# 19. Step Definitions

Step definitions are an implementation detail.

They must remain thin.

Good:

```java
@When("the customer places the order")
void customerPlacesOrder() {
    orderApi.createOrder(...);
}
```

Avoid embedding business logic inside step definitions:

```java
@When("the customer places the order")
void customerPlacesOrder() {
    if (...) {
        ...
    }

    repository.save(...);
    calculateTotal(...);
    ...
}
```

Business behavior must remain implemented by the application.

Step definitions orchestrate the test and interact with the application.

---

# 20. Step Reuse

Reuse steps when they express genuinely reusable business concepts.

Good:

```gherkin
Given the customer has an active account
```

Avoid overly generic steps such as:

```gherkin
Given the system is configured with "value"
```

Excessive generic step reuse can make scenarios difficult to understand and maintain.

Optimize for readability, not maximum step reuse.

---

# 21. Cucumber and Test Isolation

Cucumber scenarios must not depend on:

* execution order;
* previous scenarios;
* local developer state;
* external production services;
* timing assumptions;
* arbitrary delays.

Each scenario should be reproducible from a clean test environment.

Database state should be controlled explicitly.

External resources should be reset or isolated as necessary.

---

# 22. Asynchronous Testing

For SNS/SQS and other asynchronous workflows:

Never use arbitrary:

```java
Thread.sleep(...)
```

as the primary synchronization mechanism.

Prefer:

* Awaitility or equivalent bounded polling;
* deterministic test hooks;
* explicit state transitions;
* controlled test queues;
* correlation identifiers.

Every asynchronous wait must have a bounded timeout.

A test must fail clearly when the expected event/result does not occur.

---

# 23. Cucumber and Unit Tests

Cucumber does not replace unit tests.

Use Cucumber for:

```text
Business behavior
API behavior
End-to-end application behavior
Cross-component behavior
Critical integration flows
```

Use unit tests for:

```text
Business rules
Calculations
Validation logic
Edge cases
Complex algorithms
Failure paths
```

Use integration tests for:

```text
JPA
PostgreSQL
Spring configuration
SNS/SQS integration
External infrastructure
Transaction behavior
Persistence behavior
```

The same acceptance criterion may therefore be supported by several test levels.

---

# 24. Cucumber and Architecture

Cucumber scenarios must not dictate unnecessary architecture.

Do not introduce:

* interfaces;
* facades;
* adapters;
* domain layers;
* event abstractions;
* generic testing frameworks;

solely to make Cucumber easier to implement.

The production architecture remains:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
```

with explicitly justified collaborators for external systems, messaging, batch, etc.

---

# 25. AI-Generated Cucumber Tests

AI-generated Cucumber tests must be critically reviewed.

The agent must not:

* generate scenarios only for the happy path;
* duplicate implementation details from production code;
* invent unspecified business rules;
* assert irrelevant fields;
* create tests that can pass when the feature is broken;
* hide uncertainty in step definitions;
* use arbitrary sleeps;
* weaken assertions to make tests pass.

When generating Cucumber scenarios, explicitly ask:

```text
Which acceptance criterion does this scenario validate?
```

and:

```text
What incorrect implementation would this scenario detect?
```

If the answer is unclear, the scenario should be reconsidered.

---

# 26. Traceability

Each significant acceptance criterion should be traceable to one or more scenarios.

Recommended mapping:

```text
AC-001 → Scenario: Create an order
AC-002 → Scenario: Reject an unavailable product
AC-003 → Scenario: Reject an invalid order
AC-004 → Scenario: Prevent duplicate order creation
```

The traceability may be represented through:

* scenario tags;
* comments;
* specification references;
* test reports;
* project conventions.

Do not require verbose metadata if the project already provides equivalent traceability.

---

# 27. Definition of Done

A feature involving Cucumber is complete only when:

* [ ] Acceptance criteria are defined.
* [ ] Relevant acceptance scenarios exist.
* [ ] Happy path is covered.
* [ ] Relevant business failures are covered.
* [ ] Relevant boundary cases are covered.
* [ ] Security behavior is covered where applicable.
* [ ] Messaging behavior is covered where applicable.
* [ ] Scenarios are independent.
* [ ] Step definitions remain thin.
* [ ] No arbitrary sleeps are used.
* [ ] Scenarios use business language.
* [ ] Cucumber tests pass locally.
* [ ] Cucumber tests pass in CI.
* [ ] No scenario relies on production infrastructure.
* [ ] Each significant acceptance criterion is traceable to a scenario.

---

# 28. Validation Before Completion

Before declaring a feature complete, verify:

```text
Specification
    ↓
Acceptance Criteria
    ↓
Gherkin Scenarios
    ↓
Step Definitions
    ↓
Application Behavior
    ↓
Assertions
```

The implementation must satisfy the specification, not merely make the generated tests pass.

If an acceptance criterion cannot be tested meaningfully, report the limitation instead of silently removing or weakening the criterion.
