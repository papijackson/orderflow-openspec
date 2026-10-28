---
applyTo: "**/src/main/java/**/*.java,**/src/test/java/**/*.java,**/src/main/resources/**/*.yml,**/src/main/resources/**/*.yaml,**/src/main/resources/**/*.properties"
---

# Messaging / SNS / SQS Instructions

## Model
Treat messaging as an unreliable distributed boundary.

Default architecture:
`Producer -> SNS -> SQS -> Consumer`

Assume at-least-once delivery.

## Core Rules
Consumers must be:
- idempotent;
- contract-aware;
- bounded in retry;
- protected against poison messages;
- correctly configured for visibility timeout;
- observable.

## Message Contract
Define:
- contract name;
- version;
- producer;
- consumer;
- schema;
- required/optional fields;
- semantic meaning;
- failure behavior.

Avoid implicit contracts based on Java classes.

## Versioning
Prefer backward-compatible evolution:
- additive optional fields;
- tolerant consumers;
- explicit versioning where required;
- coordinated removal only after compatibility window.

## Producer
Producer must:
- validate message data;
- publish the correct contract;
- handle publication failure;
- provide telemetry;
- address consistency with DB state.

Use Transactional Outbox when DB state and event publication must be reliable together.

## Consumer
Consumer must:
1. receive;
2. validate;
3. check idempotency;
4. perform business/persistence change;
5. commit;
6. acknowledge/delete only after successful processing.

## Idempotency
Possible durable strategies:
- idempotency key;
- processed-event table;
- unique constraint;
- business state transition.

An in-memory set is not sufficient for distributed ECS tasks.

## Retry
Retry transient failures only.

Use:
- bounded attempts;
- backoff;
- clear retryable/permanent classification.

Do not retry malformed/permanent messages forever.

## DLQ
Define:
- max receive count;
- alerting;
- recovery;
- replay procedure;
- operational ownership.

DLQ must not become silent message storage.

## Visibility Timeout
Base it on actual processing duration plus safety margin.

Too short -> duplicate processing.
Too long -> slow recovery.

## Ordering
Use ordering only when required. Document:
- ordering key;
- mechanism;
- failure behavior;
- throughput trade-off.

Do not sacrifice scalability for unnecessary global ordering.

## Failure Modes
Consider:
- duplicate delivery;
- consumer crash;
- timeout;
- DB failure;
- downstream failure;
- malformed message;
- poison message;
- partial batch;
- AWS outage;
- deployment/restart during processing.

## Consumer Transaction Pattern
Typical pattern:
`Receive -> DB transaction -> idempotent change -> commit -> acknowledge`

Do not acknowledge before durable business success.

## Outbox
For outbound events requiring reliable consistency:
`DB transaction -> Outbox row -> publisher -> SNS`

Define retry and cleanup semantics.

## Batch Consumption
For partial batch behavior, define whether:
- all messages fail together;
- individual failures are returned/retried;
- successful messages are acknowledged independently.

Test the chosen behavior.

## Serialization
Use explicit, versioned schemas. Avoid unsafe polymorphic deserialization.

## Security
Do not put unnecessary:
- secrets;
- tokens;
- credentials;
- sensitive personal data

into messages.

## Observability
Track:
- received;
- successful;
- failed;
- retried;
- DLQ;
- processing duration;
- lag.

Do not use message IDs, business IDs, UUIDs, or arbitrary payload values as metric labels.

## Testing
Test:
- serialization;
- malformed messages;
- duplicate delivery;
- idempotency;
- transient retry;
- permanent failure;
- DLQ;
- partial batch;
- persistence failure;
- consumer restart.

## Performance
Consider:
- throughput;
- consumer concurrency;
- receive/delete batching;
- visibility timeout;
- DB capacity;
- downstream limits;
- ECS scaling.

## AI Review
Ask:
1. Is at-least-once assumed?
2. Is the consumer idempotent?
3. Is the contract versioned?
4. Are retries bounded?
5. Is DLQ recovery defined?
6. Is visibility timeout appropriate?
7. Is acknowledgement after success?
8. Is DB/event consistency addressed?
9. Are duplicates tested?
10. Are telemetry tags bounded?

## Validation
A messaging feature is complete only when contract, failure, retry, idempotency, DLQ, observability, security, and test behavior are defined.
## Consumer Idempotency
Idempotency must survive:
- ECS task restart;
- multiple tasks;
- duplicate delivery;
- retry after timeout;
- replay from DLQ.

Use durable state or business constraints.

## Acknowledgement
Never acknowledge a message before the durable business effect is complete unless the design explicitly uses another durable handoff mechanism.

## Poison Messages
A message that cannot ever succeed must not consume infinite processing capacity. Classify it and route it to DLQ after bounded attempts.

## Contract Compatibility
During rolling deployments, old and new consumers may coexist. Message producers must remain compatible with supported consumer versions.

## Replay
A replay mechanism must consider:
- idempotency;
- side effects;
- ordering;
- observability;
- operational approval.

## Transactional Outbox
An outbox is appropriate when:
- DB state and event publication must be atomic from the business perspective;
- losing the event after DB commit is unacceptable.

The outbox publisher itself requires retry/idempotency/cleanup behavior.

## Visibility Timeout Review
Set timeout from measured or conservative processing duration. If processing can exceed it, consider extension/heartbeat mechanisms or redesign.

## Compléments détaillés v1.0.2
## 14. Detailed message-contract and recovery rules

### 14.1 Contract ownership and compatibility
- Every event/message has an explicit owner, purpose, schema/version, producer, consumer(s), required fields, optional fields, and semantic meaning.
- Use stable event types and explicit schema evolution rules; adding a required field or changing field meaning is potentially breaking.
- Prefer additive, backward-compatible changes and test old/new producer-consumer combinations where rolling deployment creates overlap.
- Do not leak internal JPA entity structures as public event contracts.
- Document whether ordering is required and the key by which it is preserved.

### 14.2 Durable idempotency
- Identify the idempotency key and its scope (event, business operation, aggregate, or external request).
- Persist deduplication/processed-event state when the side effect must survive restarts; an in-memory set is not sufficient.
- Use database uniqueness/conditional updates where possible to close check-then-act race conditions.
- Define retention of deduplication records relative to replay windows and business requirements.
- Test duplicate delivery before, during, and after a successful business transaction.

### 14.3 Acknowledgement and transaction semantics
- Acknowledge/delete a message only after the required business outcome is durable.
- Define what happens if the database commits but acknowledgement fails; the consumer must safely handle redelivery.
- Avoid acknowledging before persistence unless message loss is explicitly acceptable.
- For outbound events coupled to a database change, consider a transactional outbox and a retryable publisher.
- Document partial-success behavior for batch receives/deletes and ensure failed items remain recoverable.

### 14.4 Retry, poison messages, and DLQ
- Classify failures as transient or permanent.
- Retry only transient failures with bounded attempts and an intentional delay/backoff strategy.
- Malformed or permanently invalid messages should not loop indefinitely; route them to the configured failure/DLQ path according to policy.
- Configure maximum receive count, DLQ retention, access controls, monitoring, and ownership.
- Define how operators inspect, repair, and replay a DLQ message safely without reintroducing duplicate side effects.
- Avoid replaying a whole DLQ blindly; validate the corrected payload and its compatibility first.

### 14.5 Visibility timeout and processing time
- Set visibility timeout based on observed worst-case processing duration, batch size, and retry behavior.
- Ensure long-running handlers extend visibility when the platform design requires it.
- Too-short visibility creates concurrent duplicate processing; too-long visibility delays recovery after crashes.
- Test task termination and dependency timeouts while a message is in flight.

### 14.6 Observability and test matrix
Record bounded metrics for receive, success, failure, retry, DLQ, duration, and lag/backlog. Never use message IDs, business IDs, raw payload fields, or arbitrary exception strings as metric tags.
Tests should cover valid/malformed schema, duplicate delivery, idempotency, transient/permanent errors, retry exhaustion, DLQ, redrive/replay, visibility timeout, consumer restart, partial batch failure, downstream outage, and contract compatibility.

### 14.7 Messaging Definition of Done
- [ ] Contract/version and compatibility policy are documented.
- [ ] Idempotency is durable for side effects that require it.
- [ ] Acknowledgement follows the intended durability boundary.
- [ ] Retry, visibility timeout, and DLQ behavior are bounded and tested.
- [ ] Replay and operator recovery are safe and documented.
- [ ] Metrics/logs are useful without high-cardinality or sensitive data.
