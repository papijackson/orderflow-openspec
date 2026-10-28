# Proposal

## Why

OrderFlow has a tested Spring Boot and PostgreSQL foundation but no order capability. This change establishes the first persisted order use case and a documented HTTP contract for creating and retrieving orders without introducing customer, catalog, pricing, or fulfillment behavior.

## What Changes

- Add `POST /api/v1/orders` and `GET /api/v1/orders/{id}` for creating and retrieving an order.
- Persist an order with a generated UUID, initial `CREATED` status, creation timestamp, customer reference, and ordered product/quantity lines.
- Validate the request contract and use the existing RFC 9457 Problem Details format for invalid input, missing orders, idempotency conflicts, and unexpected failures.
- Support an optional `Idempotency-Key` scoped by `customerId` to safely replay order creation.
- Add unit, PostgreSQL integration, and acceptance tests, plus documentation of request, response, and error contracts.

Out of scope: customer or product existence checks, pricing or total calculation, inventory, status transitions, authentication/authorization, event publication, AWS, Spring Batch, and production deployment. The API is unauthenticated in this change and must not be exposed to untrusted users before access controls are added.

## Capabilities

### New Capabilities
- `orders`: Create and retrieve persisted orders through the versioned REST API.

### Modified Capabilities
- None. The repository has no existing main capability specifications.

## Impact

- **API/application:** New controller, service, repository, entity, request/response types, and order-specific error mapping under the existing `com.orderflow` layered architecture.
- **Database:** A versioned Flyway migration and JPA mapping for orders, ordered line items, and idempotency records.
- **Validation and errors:** Bean Validation for the agreed request constraints; existing Problem Details handling is extended for not-found and conflict responses.
- **Testing:** Focused unit tests, PostgreSQL Testcontainers integration tests, and Cucumber acceptance coverage.
- **Documentation:** The HTTP contract, including field constraints, status codes, idempotency, and error formats.
- **Dependencies/integrations:** Reuse the current Spring Boot, JPA, PostgreSQL, Flyway, and test stack; add no AWS or Batch integration.
