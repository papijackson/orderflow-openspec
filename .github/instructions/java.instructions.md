---
applyTo: "**/*.java"
---

# Java 21 Development Instructions

## 1. Objective

Produce Java 21 code that is readable, maintainable, explicit and consistent with the project's layered architecture.

Prefer simple solutions over sophisticated abstractions.

Generated code MUST use the capabilities of Java 21 appropriately without introducing complexity merely to demonstrate language features.

---

# 2. Java Version

The project targets Java 21.

Use Java 21 language features when they improve readability or correctness.

Appropriate features include:

* Records.
* Pattern matching.
* Sealed classes when they provide a meaningful domain constraint.
* Switch expressions.
* Text blocks.
* Local variable type inference where readability is preserved.
* Stream API where appropriate.

Do not use language features merely because they are available.

The implementation MUST remain understandable to developers familiar with standard Java.

---

# 3. Immutability

Prefer immutable objects whenever practical.

Use:

* `final` fields.
* Constructor initialization.
* Records for immutable data carriers.
* Unmodifiable collections where appropriate.

Avoid mutable shared state.

Example:

```java
public record CreateOrderRequest(
        String customerId,
        List<OrderLineRequest> lines) {
}
```

If a mutable structure is required, its ownership and mutation responsibilities MUST be clear.

Do not expose mutable internal collections directly.

---

# 4. Records

Use records for simple immutable data carriers when:

* The object is primarily data.
* Identity beyond its values is not required.
* Mutability is not required.
* The framework being used supports the record appropriately.

Typical candidates include:

* API request DTOs.
* API response DTOs.
* Commands.
* Queries.
* Value-like results.
* Configuration projections where supported.

Do not use records for JPA entities unless the persistence strategy explicitly supports and requires such a design.

Do not force records onto objects that have substantial mutable lifecycle behavior.

---

# 5. Classes and Responsibilities

Each class MUST have a clear primary responsibility.

Avoid classes that combine:

* HTTP handling.
* Business logic.
* Persistence.
* Messaging.
* Mapping.
* Logging and monitoring logic.

Classes SHOULD be cohesive.

If a class grows significantly, first identify whether it contains multiple responsibilities.

Do not automatically extract every private method into a separate class.

Extract a class when it represents a meaningful responsibility, concept or reusable component.

---

# 6. Methods

Methods SHOULD:

* Have a clear purpose.
* Use meaningful names.
* Have a manageable number of parameters.
* Avoid hidden side effects.
* Return meaningful results.
* Fail explicitly when their contract cannot be fulfilled.

Prefer:

```java
calculateOrderTotal(order)
```

over:

```java
process(order)
```

when the actual responsibility is specifically calculating a total.

Avoid methods that:

* Perform unrelated operations.
* Have deeply nested conditional logic.
* Return different conceptual types depending on execution paths.
* Hide significant side effects behind generic names.

Do not impose an arbitrary line-count limit.

Method complexity SHOULD be evaluated based on readability and responsibility.

---

# 7. Null Handling

Avoid returning `null` when an explicit alternative exists.

Use:

* `Optional<T>` for methods where absence is a meaningful result.
* Empty collections for collection-returning methods.
* Explicit exceptions for invalid states.
* Validation at appropriate boundaries.

Do not use `Optional`:

* As a field in JPA entities.
* As a method parameter unless a project convention explicitly requires it.
* Merely to avoid thinking about nullability.

Example:

```java
Optional<Order> findById(OrderId id)
```

is preferable to:

```java
Order findById(OrderId id)
```

when absence is an expected outcome.

Do not chain `Optional` operations into unreadable expressions.

---

# 8. Collections

Prefer interfaces in declarations:

```java
List<Order> orders
```

rather than:

```java
ArrayList<Order> orders
```

Use the most appropriate collection type.

Consider:

* `List` for ordered collections.
* `Set` when uniqueness is part of the requirement.
* `Map` for key-based lookup.
* Immutable or unmodifiable collections where appropriate.

Do not use a `Set` merely to remove duplicates without understanding whether ordering or business semantics are affected.

Do not expose mutable internal collections.

Prefer defensive copying when ownership is transferred.

---

# 9. Streams

Streams SHOULD be used when they improve readability and express a natural transformation or aggregation.

Good use:

```java
var activeOrders = orders.stream()
        .filter(Order::isActive)
        .toList();
```

Avoid complex stream pipelines containing:

* Multiple nested lambdas.
* Significant side effects.
* Exception-heavy logic.
* Difficult-to-understand collectors.
* Multiple levels of nested streams.

When a traditional loop is clearer, use a loop.

Do not use streams merely to reduce the number of lines of code.

Streams MUST NOT be used to hide business logic that would be clearer as named methods.

---

# 10. Loops and Side Effects

Prefer explicit loops when processing requires:

* Multiple state changes.
* Complex branching.
* Early exits.
* Exception handling.
* Significant side effects.

Avoid stream operations whose primary purpose is mutation.

Do not use `forEach` to perform complex workflows that would be clearer as an explicit loop.

---

# 11. Optional

Use `Optional` to represent an explicit possibility of absence.

Prefer:

```java
return repository.findById(id)
        .orElseThrow(() -> new OrderNotFoundException(id));
```

Avoid:

```java
if (optional.isPresent()) {
    ...
}
```

when a direct `map`, `orElse`, `orElseThrow` or equivalent expression is clearer.

Do not use `Optional.get()` without a previously established guarantee.

Avoid deeply chained Optional expressions.

---

# 12. Equality and Identity

Implement `equals` and `hashCode` consistently.

For JPA entities, equality MUST follow the project's established JPA identity strategy.

Do not automatically generate equality based on every mutable field of a JPA entity.

Do not use mutable business attributes as hash-based collection identity when those attributes may change.

For value objects, equality SHOULD normally reflect their value.

---

# 13. Exceptions

Exceptions MUST represent meaningful failure conditions.

Use:

* Business exceptions for expected business rule violations.
* Technical exceptions for infrastructure failures.
* Validation mechanisms for invalid external input.

Do not use exceptions for ordinary control flow.

Do not catch `Exception` broadly unless there is a clear boundary-level reason.

Bad:

```java
try {
    ...
} catch (Exception e) {
    return null;
}
```

Do not silently swallow exceptions.

Do not expose internal exception details through public APIs.

Exception translation SHOULD occur at appropriate architectural boundaries.

---

# 14. Exception Messages

Exception messages MUST be useful for diagnosis without exposing sensitive information.

Messages SHOULD identify:

* What operation failed.
* Which non-sensitive identifier is relevant.
* What condition caused the failure.

Do not include:

* Passwords.
* Access tokens.
* Credentials.
* Sensitive personal data.
* Full request payloads unless explicitly safe.

---

# 15. Constants

Avoid unexplained magic numbers and strings.

Prefer named constants when a value has semantic meaning.

Good:

```java
private static final int MAX_RETRY_ATTEMPTS = 3;
```

Bad:

```java
if (attempts > 3) {
    ...
}
```

Do not create constants for trivial values whose meaning is already obvious.

Avoid global utility constant classes containing unrelated values.

---

# 16. Enums

Use enums when the set of values is constrained and meaningful.

Example:

```java
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    CANCELLED
}
```

Do not use string literals throughout the code when a closed domain vocabulary exists.

Do not use enums when values are expected to be dynamically configured or extended outside the application lifecycle.

---

# 17. Switch Expressions

Use switch expressions when they improve clarity.

Example:

```java
var result = switch (status) {
    case CREATED -> handleCreated();
    case CONFIRMED -> handleConfirmed();
    case CANCELLED -> handleCancelled();
};
```

Prefer exhaustive handling when the domain permits it.

Do not use switch expressions to hide large business workflows.

---

# 18. Pattern Matching

Pattern matching MAY be used when it makes type handling clearer.

Do not introduce complex pattern matching merely to reduce line count.

Business logic MUST remain understandable.

---

# 19. Sealed Types

Sealed classes and interfaces MAY be used when the domain genuinely benefits from a closed hierarchy.

Do not introduce sealed hierarchies simply because Java 21 supports them.

Prefer simpler types when the number of variants is unlikely to require compiler-enforced exhaustiveness.

---

# 20. Concurrency

Shared mutable state MUST be minimized.

Do not introduce:

* Global mutable state.
* Static mutable collections.
* Uncontrolled thread creation.
* Custom thread pools without justification.

Use the framework's managed concurrency facilities where appropriate.

When implementing asynchronous processing, explicitly consider:

* Thread safety.
* Ordering.
* Idempotency.
* Resource limits.
* Failure handling.

Do not assume that a singleton Spring bean is thread-safe merely because Spring manages it.

Stateless services SHOULD be preferred.

---

# 21. Date and Time

Use `java.time`.

Prefer:

* `Instant` for timestamps representing an absolute point in time.
* `LocalDate` for dates without time-zone semantics.
* `LocalDateTime` only when the absence of timezone information is intentional.
* `OffsetDateTime` or `ZonedDateTime` when offset/time-zone semantics matter.

Do not use legacy `Date` or `Calendar` APIs unless required by an external library.

Time handling MUST be explicit when it affects business behavior.

---

# 22. Logging

Use the project's standard logging framework through the Spring Boot logging stack.

Logs MUST be useful for diagnosis.

Do not log:

* Passwords.
* Tokens.
* Credentials.
* Sensitive personal data.
* Entire request or response payloads without justification.

Avoid excessive logging inside loops or high-throughput paths.

Use appropriate log levels.

Do not use `System.out.println`.

---

# 23. Comments

Code SHOULD explain itself through meaningful naming and structure.

Comments SHOULD explain:

* Why a non-obvious decision exists.
* Important constraints.
* External limitations.
* Non-obvious performance considerations.
* Workarounds.

Comments SHOULD NOT merely restate the code.

Avoid comments such as:

```java
// increment counter
counter++;
```

Do not generate large explanatory comments for straightforward code.

---

# 24. TODOs

Do not introduce TODOs without a concrete reason.

A TODO representing required future work SHOULD include sufficient context to understand:

* What remains to be done.
* Why it matters.
* What condition triggers the work.

Do not use TODOs as a substitute for implementing a required part of the specification.

---

# 25. Utility Classes

Do not create generic utility classes containing unrelated static methods.

Prefer cohesive components.

Before creating a utility method, search the project and existing libraries for an existing solution.

Avoid unnecessary static methods when dependency injection or an existing domain component is more appropriate.

---

# 26. Dependency Introduction

Before adding a dependency:

1. Search the project for an existing capability.
2. Check whether Java or Spring already provides the functionality.
3. Check whether an existing dependency already solves the problem.
4. Evaluate security, maintenance and operational impact.
5. Add the dependency only when justified.

Do not introduce libraries merely for convenience in a small implementation.

---

# 27. Generated Code

AI-generated Java code MUST be reviewed for:

* Unnecessary abstractions.
* Incorrect exception handling.
* Null handling.
* Thread safety.
* Hidden side effects.
* Excessive use of streams.
* Incorrect collection choices.
* Inappropriate Optional usage.
* JPA-specific issues.
* Logging of sensitive information.
* Unnecessary dependencies.

The agent MUST prefer consistency with the existing codebase over introducing a new coding style.

---

# 28. Validation

For Java changes, the agent SHOULD execute, as applicable:

```text
mvn compile
mvn test
```

and project-specific quality checks.

The agent MUST report:

* Tests executed.
* Tests not executed.
* Failures.
* Static-analysis findings.
* Remaining risks.

Do not claim that Java code is validated merely because it compiles.
