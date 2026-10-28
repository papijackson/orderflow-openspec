# Tasks

## 1. Order persistence and schema

- [x] 1.1 Add the versioned Flyway migration and JPA mappings for orders and ordered items, including named integrity constraints; verify migration, mappings, positive quantities, and preservation of duplicate ordered items with PostgreSQL Testcontainers. (Orders: Accepted orders retain their persisted representation; Order persistence is atomic)
- [x] 1.2 Add the repository operation for customer-scoped idempotency reservation using PostgreSQL `ON CONFLICT`; verify the unique reservation behavior and independent keys for different customers against PostgreSQL Testcontainers. (Orders: Order creation supports customer-scoped idempotent retries; Idempotency-key reuse with different order data is rejected)

## 2. Order use cases

- [x] 2.1 Implement transactional order creation and UUID retrieval in the service, mapping entities to an immutable application result independent of HTTP; add unit tests for server-generated UUID, `CREATED` status, UTC timestamp, item order, and distinct no-key creations. (Orders: Accepted orders retain their persisted representation; Existing orders can be retrieved by UUID)
- [x] 2.2 Implement atomic idempotent replay and ordered-item comparison, including the conflict outcome; add PostgreSQL integration tests for identical replay, changed-data conflict, customer scope, concurrent same-key requests, and rollback when persistence fails. (Orders: Order creation supports customer-scoped idempotent retries; Idempotency-key reuse with different order data is rejected; Order persistence is atomic)

## 3. HTTP contract and acceptance coverage

- [x] 3.1 Add validated request/response types and thin controller endpoints; add MVC tests for required fields, UUIDs, item-count and quantity boundaries, idempotency-key limits, 201 plus `Location`, 200 retrieval, and invalid path UUIDs returning 400 without invoking persistence for invalid bodies. (Orders: Order requests enforce the agreed structural constraints; Accepted orders retain their persisted representation; Order creation returns the created resource; Existing orders can be retrieved by UUID)
- [x] 3.2 Extend `RestExceptionHandler` for not-found and idempotency conflict responses while preserving safe generic failures; add tests asserting RFC 9457 fields, matching status, `application/problem+json`, no echoed input, HTTP 404, HTTP 409, and generic HTTP 500. (Orders: Missing orders return a consistent not-found problem; Idempotency-key reuse with different order data is rejected; REST errors use safe Problem Details responses)
- [x] 3.3 Document request/response schemas, examples, validation constraints, `Location`, idempotency behavior, status codes, and Problem Details in `docs/api/orders.md`; verify the documented contract matches `specs/orders/spec.md`. (Orders: Order requests enforce the agreed structural constraints; Order creation returns the created resource; REST errors use safe Problem Details responses)
- [x] 3.4 Add Cucumber dependencies, runner, and business-readable acceptance scenarios for create/retrieve with opaque references, invalid input, missing orders, and idempotent replay/conflict; verify the Cucumber suite passes against the application and PostgreSQL Testcontainers. (Orders: Order requests enforce the agreed structural constraints; Order creation returns the created resource; Order creation supports customer-scoped idempotent retries; Idempotency-key reuse with different order data is rejected; Existing orders can be retrieved by UUID; Missing orders return a consistent not-found problem)

## 4. Integrated verification

- [x] 4.1 Run `./mvnw -B verify` and confirm the existing GitHub Actions build-and-test workflow passes with unit, PostgreSQL integration, Cucumber, and architecture tests; record code validation separately from OpenSpec validation.
- [x] 4.2 Run `openspec validate "manage-orders-api" --strict` and `openspec status --change "manage-orders-api"`; confirm all planning artifacts are valid and complete independently of the Maven/code results.
