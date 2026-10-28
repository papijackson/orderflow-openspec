---
applyTo: "**/src/main/java/**/*.java,**/src/test/java/**/*.java,**/src/main/resources/**/*.yml,**/src/main/resources/**/*.yaml,**/src/main/resources/**/*.properties,**/Dockerfile,**/*.json"
---

# AWS and ECS Instructions

## Principles
Use managed AWS services appropriately. Design for:
- least privilege;
- bounded retries;
- explicit timeouts;
- resilience;
- stateless ECS tasks;
- observability;
- graceful shutdown.

## ECS
Consider:
- multiple tasks;
- CPU/memory requests and limits;
- autoscaling;
- startup time;
- readiness/liveness;
- graceful shutdown;
- connection pools;
- downstream capacity.

Do not assume a single task.

## Configuration
Externalize environment-specific configuration. Never embed credentials.

Use the project's approved secret mechanism.

## AWS SDK
AWS clients should be:
- injected;
- centrally configured;
- reused;
- configured with timeouts/retry policy;
- isolated from controllers/entities.

Do not instantiate SDK clients inside business methods.

Architecture:
`Controller -> Service -> AWS-facing collaborator -> AWS SDK`

## IAM
Use least privilege. Avoid `*` actions/resources unless explicitly justified.

Separate deployment permissions from runtime task permissions.

## SNS/SQS
See `messaging.instructions.md`.

Consider:
- visibility timeout;
- retry;
- DLQ;
- encryption;
- IAM;
- message size;
- throughput;
- consumer concurrency;
- monitoring.

## Resilience
Define:
- timeout;
- retryable errors;
- max attempts;
- backoff;
- fallback where appropriate;
- DLQ/recovery.

Avoid retry storms.

## Transactions
Database and AWS operations are not one atomic transaction.

Use Transactional Outbox where reliable DB/event consistency is required.

## Deployment
Prefer rolling/backward-compatible deployment behavior:
- graceful shutdown;
- readiness;
- compatible schema;
- compatible message contracts;
- expand/contract migrations;
- rollback evidence.

## Health
Liveness should detect process viability. Readiness should reflect ability to serve traffic. Avoid cascading dependency failures into liveness.

## Observability
Use standard Micrometer metrics for:
- HTTP;
- JVM;
- CPU/memory;
- datasource;
- external dependencies;
- messaging;
- batch.

Do not use AWS resource IDs, request IDs, business IDs, UUIDs, or user data as metric labels.

## Cost
Consider:
- task count;
- CPU/memory;
- logs;
- polling;
- message volume;
- cross-service calls;
- idle capacity.

Cost is a design constraint but must not override reliability/security requirements without explicit decision.

## Testing
Do not call live production AWS services from normal unit tests.

Use:
- mocks;
- contract tests;
- local emulators where appropriate;
- controlled non-production integration tests.

## AI Review
Ask:
1. Are credentials externalized?
2. Is IAM least privilege?
3. Are SDK clients reused?
4. Are timeouts explicit?
5. Are retries bounded?
6. Could concurrency overload downstream systems?
7. Is ECS shutdown graceful?
8. Are readiness/liveness correct?
9. Is DB/event consistency addressed?
10. Are AWS costs and observability considered?

## Validation
Verify configuration, IAM assumptions, retry behavior, deployment compatibility, health checks, and tests before completion.
## ECS Scaling Interaction
When task count increases, also multiply:
- DB connections;
- message consumers;
- outbound calls;
- cache footprint;
- log volume.

Review these multipliers before increasing autoscaling limits.

## Graceful Deployment
For consumers, deployment must account for messages already being processed. Visibility timeout, shutdown grace period, and acknowledgement order must be compatible.

## IAM Review
For every AWS action, identify:
- API action;
- resource;
- runtime role;
- reason.

Avoid broad permissions copied from examples.

## AWS Failure
Treat AWS SDK failures as distributed failures. Define timeout, retry, and recovery semantics rather than exposing raw SDK exceptions to business code.

## Cost Review
Polling frequency, task count, logging verbosity, message volume, and cross-AZ/service traffic can materially affect cost. Document material trade-offs.

## Compléments détaillés v1.0.2
## 14. Detailed AWS/ECS operational review

### 14.1 Runtime roles and configuration
- Distinguish ECS task execution role, application task role, and deployment role; grant each only its required permissions.
- Do not bake environment-specific endpoints, credentials, or secrets into the image.
- Document configuration keys, defaults, units, and safe ranges.
- Reuse injected/configured AWS SDK clients; do not instantiate clients inside request loops or business methods.
- Configure connection/request timeouts and bounded retry policies centrally where possible.

### 14.2 ECS lifecycle and scaling
- Make services stateless across tasks unless durable state is explicitly designed.
- Ensure readiness prevents traffic from reaching an unready task and liveness detects a genuinely stuck process.
- Handle SIGTERM/graceful shutdown so HTTP requests, SQS processing, and batch work stop safely within the deployment grace period.
- Review startup time, memory/CPU limits, task count, autoscaling metrics, and rolling-deployment overlap.
- Account for database connection pools multiplied by the maximum task count and deployment surge.
- Avoid local disk as durable storage; define temporary-file cleanup and capacity if disk is used.

### 14.3 SNS/SQS and failure behavior
- Assume at-least-once delivery and design idempotent consumers.
- Set visibility timeout based on measured processing time and batch size; configure a DLQ and a bounded redrive policy.
- Ensure retries do not amplify outages or overload downstream services.
- Define what happens to in-flight messages on task shutdown, deployment, and dependency outage.
- Use FIFO/ordering only when the business requirement justifies its throughput and failure trade-offs.
- Use an outbox or another explicit consistency mechanism when a database change and message publication must not diverge.

### 14.4 Deployment and schema compatibility
- Prefer backward-compatible rolling changes for APIs, event schemas, and database schemas.
- Use expand/contract for incompatible database changes; deploy consumers/producers in a compatible sequence.
- Do not rely on rollback to undo irreversible data transformations.
- Define rollback or forward-fix steps, migration status checks, and post-deployment verification for risky changes.
- Production deployment requires all mandatory quality gates and explicit human approval.

### 14.5 Cost and operational controls
- Consider task sizing/count, log volume, polling frequency, message size, data transfer, retry volume, and idle resources.
- Do not reduce observability so far that production failures become uninvestigable; control volume through levels, sampling, and aggregation.
- Avoid aggressive polling or retry loops that increase cost and downstream load.
- Include capacity and cost trade-offs in the plan when the change materially alters throughput or resource use.

### 14.6 AWS Definition of Done
- [ ] Runtime IAM permissions are least-privilege and distinct from deployment permissions.
- [ ] Timeouts, retries, and concurrency are bounded.
- [ ] ECS readiness, graceful shutdown, scaling, and connection limits are reviewed.
- [ ] SNS/SQS idempotency, visibility, retry, and DLQ behavior are tested.
- [ ] Deployment and database/event compatibility are explicit.
- [ ] Operational metrics and cost impact are considered.
