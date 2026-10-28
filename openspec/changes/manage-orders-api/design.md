# Design

## Context

See `proposal.md` for the motivation and scope. The current application is a Java 21 / Spring Boot 4 layered monolith using Spring Data JPA, PostgreSQL, and Flyway. Its existing `RestExceptionHandler` maps request validation failures to RFC 9457 responses and masks unexpected failures, but it does not yet map order-not-found, idempotency-conflict, or invalid UUID path errors. The Maven build has PostgreSQL Testcontainers support but no Cucumber dependencies.

The observable API and persistence requirements are defined in `specs/orders/spec.md`. Existing architecture rules require controllers to remain thin, services to own use cases and transactions, repositories to own persistence, and entities to remain independent of application and infrastructure layers.

## Goals / Non-Goals

**Goals:**
- Implement the specified order use cases within the existing layered application.
- Make database constraints authoritative for item integrity and idempotency under concurrent requests.
- Return stable response DTOs and safe Problem Details without exposing persistence entities or internals.
- Verify behavior against PostgreSQL and express the primary business outcomes as Cucumber acceptance scenarios.

**Non-Goals:**
- Add customer or product persistence/lookups, pricing, totals, inventory, order status transitions, or deletion.
- Add authentication, authorization, event publication, AWS integrations, Spring Batch, or deployment automation.
- Introduce a separate domain model, generic repository/service abstractions, or another application module.

## Decisions

### Keep the use case in the existing layers

Add a thin controller under `com.orderflow.controller` with request/response types at that HTTP boundary, an `OrderService` under `com.orderflow.service`, persistence components under `com.orderflow.repository`, and `Order`/`OrderItem` entities under `com.orderflow.entity`. The service maps entities to an immutable application result inside the transaction; it neither depends on HTTP types nor returns JPA entities. The controller maps that result to the public response DTO and handles HTTP status, `Location`, and request validation only.

No customer or product repositories are introduced: their UUIDs are opaque references in this change. A separate domain model or feature-layer architecture was considered but is not justified for this single capability and would conflict with the established architecture rules.

### Store orders and their ordered items relationally

Add one forward-only Flyway migration using lowercase PostgreSQL names and explicitly named constraints:

- `orders`: server-generated UUID primary key, `customer_id` UUID, `status` string, and `created_at` `timestamptz`, all non-null. The service sets `CREATED`; `Instant` is used for the instant in time and serialized in UTC.
- `order_items`: order foreign key, zero-based line position, `product_id` UUID, and positive 32-bit `quantity`. A unique `(order_id, line_number)` constraint preserves submitted order while allowing duplicate product identifiers.
- `order_idempotency`: exact `customer_id` and `idempotency_key` pair as the primary key, plus a unique `order_id`. The key is bounded to 128 characters and non-blank at the database boundary. Its foreign key to `orders` is deferred until commit so the key can be reserved before the order is inserted; deleting an order cascades to its key record.

The primary key supports retrieval by order UUID; the composite idempotency key supports the only idempotency lookup. No additional indexes are added without another query requirement. Database constraints complement, rather than replace, request validation. Flyway remains the schema source of truth; Hibernate must not create or update production schema.

### Make idempotency atomic under PostgreSQL concurrency

For a request with an idempotency key, generate the candidate order UUID and, inside the service transaction, reserve `(customer_id, idempotency_key)` using PostgreSQL `INSERT ... ON CONFLICT DO NOTHING`. The deferred foreign key permits this reservation to precede insertion of the order. If the reservation succeeds, persist the order and ordered items in the same transaction. If another committed reservation already exists, load its order: return it when its ordered `productId`/`quantity` entries match exactly, otherwise raise the conflict mapped to HTTP 409. An identical customer-scoped key therefore cannot create a second order, including when requests race. A failed order or item write rolls back the reservation with the rest of the transaction. Requests without a key skip reservation and each create a new order.

The unique database key plus `ON CONFLICT` is chosen over an application-only existence check because the latter can race. A single nullable key column on `orders` was considered, but separating the reservation makes the atomic conflict path explicit and lets PostgreSQL resolve concurrent requests without catching a failed transaction and retrying outside it. This small native query is PostgreSQL-specific and must be exercised against PostgreSQL Testcontainers; it does not change the default transaction isolation level.

### Extend the existing Problem Details boundary

Continue using Spring `ProblemDetail` and `RestExceptionHandler`. Add explicit mappings for order-not-found (404), idempotency conflict (409), and invalid path UUID conversion (400), while preserving existing validation behavior and the generic masked 500 response. All error bodies use `application/problem+json` with RFC 9457 fields and matching HTTP/body status. Validation details remain generic and do not echo submitted values. The controller returns 201 with the persisted response body and a `Location` for both a new creation and an identical replay; retrieval returns 200.

The error mapper is extended instead of adding a second exception format or dependency. Error handling must distinguish known application errors from unexpected persistence/infrastructure failures; unexpected failures remain generic to clients and diagnostic details stay in server logs.

### Test each behavior at its useful level

- Unit tests cover request-to-entity/response mapping and service decisions that do not require database concurrency.
- MVC tests cover request validation, invalid UUID paths, status codes, media types, response fields, `Location`, and safe Problem Details.
- PostgreSQL Testcontainers integration tests execute the Flyway migration and exercise persistence, ordered duplicate items, idempotency replay/conflict/customer scope, independent requests without keys, transaction rollback, and concurrent identical keys.
- Add Cucumber test dependencies and capability-oriented scenarios for create/retrieve, invalid input, missing orders, and idempotent retry/conflict. Step definitions delegate to the application/test context rather than containing business logic.
- Add `docs/api/orders.md` with request and response examples, field constraints, headers, status codes, idempotency semantics, and Problem Details examples. Use Markdown rather than introducing an OpenAPI toolchain because the repository has no existing API-description tooling; the document must remain aligned with the spec.

The existing GitHub Actions workflow runs `./mvnw -B verify`; Maven verification must include the new Cucumber and PostgreSQL integration tests. These checks are distinct from OpenSpec artifact validation.

## Risks / Trade-offs

- [The PostgreSQL upsert and deferred foreign key are more specialized than a basic JPA save] → Keep the native operation isolated in the repository, document why it is required for concurrent idempotency, and test the migration and race behavior against PostgreSQL.
- [A replay comparison can accidentally ignore item order or duplicates] → Compare the ordered item sequence, not a set or product-keyed map; cover duplicate products and reordered payloads in focused tests.
- [The API is unauthenticated] → Do not expose it to untrusted users before access controls are added; authentication and authorization remain an explicit follow-up, not a hidden implementation assumption.
- [Opaque customer/product references may be mistaken for verified entities] → Document that this API performs no existence checks and does not calculate price, totals, or inventory.
- [Acceptance and database tests require container support in CI] → Use the existing PostgreSQL Testcontainers baseline and ensure the CI runner supports Docker; do not substitute H2 for PostgreSQL behavior.

## Migration Plan

The migration is additive and there is no existing order data or deployed order API to transform. Apply the versioned Flyway migration before the new application code uses the tables; the existing application behavior remains compatible with the added tables. Validate the migration and JPA mappings against PostgreSQL Testcontainers and through `./mvnw -B verify`.

If the application change must be rolled back, roll back the application version but leave the additive order tables and any created order data in place. Do not drop them automatically; data removal requires a separately reviewed migration and retention decision. No production deployment is part of this change.

## Open Questions

None. The API behavior and constraints are specified in `specs/orders/spec.md`; the remaining choices in this document are implementation decisions.
