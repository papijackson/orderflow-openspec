# Spec Delta

## Purpose

Defines the initial order capability: clients can submit a structurally valid order and retrieve its persisted state through a stable, documented REST contract.

## ADDED Requirements

### Requirement: Order requests enforce the agreed structural constraints
The system SHALL accept `POST /api/v1/orders` requests with a UUID `customerId` and 1–100 `items`, each containing a UUID `productId` and a positive 32-bit integer `quantity`. Customer and product identifiers SHALL be treated as opaque references.

#### Scenario: Structurally valid order references unknown customer and products
- **WHEN** a request contains valid UUID references and valid item quantities even though no customer or product records exist
- **THEN** the request passes structural validation without customer/product lookups, price calculation, or inventory checks

#### Scenario: Rejected order input violates a required field or constraint
- **WHEN** a request omits a required field, contains an invalid UUID, has no items, has more than 100 items, or contains a quantity less than 1 or greater than 2147483647
- **THEN** the system returns HTTP 400 with `application/problem+json`
- **AND** no order is persisted

### Requirement: Accepted orders retain their persisted representation
The system SHALL persist each accepted order with a server-generated UUID, status `CREATED`, a server-generated UTC creation timestamp, and the submitted customer and item references.

#### Scenario: New order representation is returned
- **WHEN** the system accepts a new order
- **THEN** the representation contains `id`, `customerId`, `items`, `status`, and `createdAt`
- **AND** each item contains `productId` and `quantity`
- **AND** `id` is a UUID, `status` is `CREATED`, and `createdAt` is an ISO 8601 UTC timestamp
- **AND** no price or total is calculated or returned

#### Scenario: Repeated product identifiers are submitted
- **WHEN** the request contains multiple item entries with the same `productId`
- **THEN** the system preserves each entry as a separate item in the submitted order
- **AND** the system preserves the order of the item entries

### Requirement: Order creation returns the created resource
The system SHALL return HTTP 201 with an `application/json` order representation and a `Location` header identifying `/api/v1/orders/{id}` when a new order is created.

#### Scenario: Order is created without an idempotency key
- **WHEN** a valid request creates a new order without `Idempotency-Key`
- **THEN** the response is HTTP 201 with the persisted order representation
- **AND** the `Location` header identifies the created order

### Requirement: Order creation supports customer-scoped idempotent retries
The system SHALL accept an optional non-blank `Idempotency-Key` of 1–128 characters, scoped by `customerId`, and SHALL retain the key for the lifetime of its order.

#### Scenario: Identical request is retried with the same customer and key
- **WHEN** the same customer submits the same ordered item data again with the same key
- **THEN** the system returns the original HTTP 201 response, including the same order identifier and `Location`
- **AND** the system does not create another order

#### Scenario: Same key is used by a different customer
- **WHEN** a different `customerId` submits an order using a key already used by another customer
- **THEN** the system treats the key as a separate customer-scoped key and processes the request independently

#### Scenario: Request is retried without an idempotency key
- **WHEN** a customer submits the same valid order more than once without `Idempotency-Key`
- **THEN** each request creates a distinct order

### Requirement: Idempotency-key reuse with different order data is rejected
The system SHALL return HTTP 409 with `application/problem+json` when the same `customerId` and `Idempotency-Key` are reused with different order data.

#### Scenario: Existing key is reused with changed order data
- **WHEN** a customer submits a request whose ordered item data differs from the request previously stored under the same key
- **THEN** the system returns HTTP 409 with a Problem Details response
- **AND** the system does not create another order

#### Scenario: Idempotency key violates its constraint
- **WHEN** a supplied `Idempotency-Key` is blank or longer than 128 characters
- **THEN** the system returns HTTP 400 with `application/problem+json`
- **AND** no order is persisted

### Requirement: Order persistence is atomic
The system SHALL commit the order, its item entries, and any supplied idempotency key as one database transaction.

#### Scenario: Persistence fails while creating an order
- **WHEN** persistence of an order, one of its items, or its idempotency key fails
- **THEN** none of those records is committed

### Requirement: Existing orders can be retrieved by UUID
The system SHALL return HTTP 200 with the persisted order representation when `GET /api/v1/orders/{id}` uses the UUID of an existing order.

#### Scenario: Existing order is requested
- **WHEN** a client requests an existing order by its UUID
- **THEN** the response is HTTP 200 with `application/json`
- **AND** the returned order fields and item order match the persisted representation

#### Scenario: Order identifier is not a UUID
- **WHEN** a client requests an order using a path identifier that is not a UUID
- **THEN** the system returns HTTP 400 with `application/problem+json`

### Requirement: Missing orders return a consistent not-found problem
The system SHALL return HTTP 404 with `application/problem+json` when a syntactically valid UUID does not identify a persisted order.

#### Scenario: Requested order does not exist
- **WHEN** a client requests an order by a valid UUID that is not present
- **THEN** the system returns HTTP 404 with a Problem Details response
- **AND** the problem status matches the HTTP status

### Requirement: REST errors use safe Problem Details responses
The system SHALL represent REST errors with `application/problem+json` and an RFC 9457 body containing `type`, `title`, `status`, and `detail`, with `status` matching the HTTP status. Validation responses SHALL NOT echo submitted values, and unexpected-failure responses SHALL NOT expose internal or sensitive details.

#### Scenario: Unexpected failure occurs during an order request
- **WHEN** an order request fails because of an unexpected server error
- **THEN** the system returns HTTP 500 with a generic Problem Details response
- **AND** the response contains no stack trace, SQL, credentials, or infrastructure details

#### Scenario: Invalid request does not disclose submitted values
- **WHEN** an order request fails validation
- **THEN** the Problem Details response describes the validation failure without echoing submitted field values
