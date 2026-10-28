---
applyTo: "**/src/main/java/**/*.java,**/src/test/java/**/*.java,**/src/main/resources/**/*.yml,**/src/main/resources/**/*.yaml,**/src/main/resources/**/*.properties"
---

# Spring Batch Instructions

## Scope

Use Spring Batch for substantial, restartable batch processing. Do not use it
for a small synchronous workflow solely because a scheduler is available.

## Job Design

Define before implementation:

- input source, expected volume, and completion window;
- job and step ownership;
- chunk size and transaction boundary;
- restartability and checkpoint behavior;
- idempotency and duplicate-processing behavior;
- skip, retry, and failure policy;
- resource, database, and downstream-service limits.

Use stable, meaningful job and step names. A job must be safe to observe and
operate repeatedly according to its specification.

## Readers, Processors, and Writers

Keep each component focused:

- readers acquire and validate input;
- processors apply transformation and business rules;
- writers persist or publish the approved output.

Do not put a complete business workflow in a reader or writer. Delegate
application behavior to services where orchestration or business rules are
required.

Avoid retaining an unbounded number of items in memory. For JPA processing,
bound the persistence context through an appropriate chunk size and explicit
flush/clear strategy when needed.

## Transactions and Restartability

Define transactions at the step/chunk boundary deliberately. A chunk retry,
restart, or task restart can process an item again; make externally visible
effects idempotent or protect them with durable constraints.

Do not hold database transactions open while making slow external calls unless
the design explicitly justifies the consistency and capacity trade-off.

Record the behavior for:

- a failure before writing;
- a failure after persistence but before acknowledgement or publication;
- a retry of the same item;
- a job restart after a partial chunk;
- concurrent or overlapping job executions.

## Error Handling

Classify failures as:

- retryable transient failures;
- skippable data failures;
- non-retryable technical or business failures.

Set bounded retry and skip limits. Log sufficient safe context to diagnose a
failed item without logging secrets or complete sensitive payloads. Escalate
when a skip or failure threshold is reached; do not silently complete a job
with unknown data loss.

## Concurrency and Performance

Do not introduce parallel steps, partitioning, or asynchronous item processing
without a workload and data-integrity reason. Before increasing concurrency,
review:

- ordering requirements;
- idempotency;
- database locks and connection-pool capacity;
- downstream rate limits;
- memory and CPU limits;
- ECS task overlap during deployment.

Measure the effects of chunk size and concurrency using representative data.
Do not claim a throughput improvement from configuration changes alone.

## Observability

Prefer Spring Batch and Micrometer standard instrumentation. Ensure operators
can determine job and step status, duration, read/write/skip/retry/failure
counts, and whether the completion window was met.

Use only bounded metric tags, such as job name, step name, and outcome. Never
use job parameters, record identifiers, customer data, or exception messages
as metric tags.

## Testing

Test the selected behavior rather than implementation details:

- normal completion;
- restart after failure;
- retry and retry exhaustion;
- skip policy and limits;
- idempotent reprocessing;
- transaction and persistence behavior;
- input validation and malformed records;
- relevant concurrency and performance risks.

Use PostgreSQL Testcontainers when persistence semantics matter. Do not use
arbitrary sleeps to test asynchronous or batch completion behavior.

## Definition of Done

- [ ] Volume, completion window, and chunk/transaction boundaries are defined.
- [ ] Restartability, idempotency, skip, and retry behavior are explicit.
- [ ] Memory, database, and downstream capacity are bounded and reviewed.
- [ ] Failure and partial-completion behavior are observable and tested.
- [ ] Metrics and logs are useful, bounded, and free of sensitive data.
