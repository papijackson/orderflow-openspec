---
applyTo: "**/src/main/java/**/*.java,**/src/test/java/**/*.java,**/src/main/resources/**/*.yml,**/src/main/resources/**/*.yaml,**/src/main/resources/**/*.properties"
---

# Performance Instructions

## Purpose
Performance is a design constraint. Optimize based on evidence, not intuition.

## Core Principles
1. Define workload and targets.
2. Prefer simple predictable resource usage.
3. Avoid speculative optimization.
4. Measure before and after material optimization.
5. Consider DB, network, ECS, messaging, batch, and external dependencies together.

## Performance Model
A performance specification should consider:
- request/message rate;
- peak rate;
- concurrency;
- data volume and growth;
- latency target;
- throughput target;
- batch completion window;
- CPU/memory;
- external service latency;
- DB workload;
- ECS scaling.

Examples:
- p95 latency < 300 ms;
- throughput >= 500 requests/s;
- batch completion < 20 minutes;
- consumer lag < 60 seconds.

## API
Consider:
- serialization/deserialization;
- payload size;
- pagination;
- deterministic sorting;
- unbounded result sets;
- remote calls;
- controlled parallelism.

Controllers must remain thin.

## Service
Avoid:
- repeated DB queries;
- repeated external calls;
- unnecessary object creation in hot paths;
- unbounded loops;
- long transactions around slow external calls.

## JPA/Hibernate
Review:
- N+1;
- fetch plans;
- entity graphs;
- projections;
- eager loading;
- pagination;
- count-query cost;
- persistence-context growth;
- bulk operations;
- flush frequency;
- JDBC batching.

Do not load large graphs by convenience.

## PostgreSQL
Review:
- actual query patterns;
- indexes;
- composite index order/selectivity;
- joins;
- filtering;
- sorting;
- constraints;
- lock behavior;
- connection pools.

Use `EXPLAIN`/`EXPLAIN ANALYZE` for important queries.

Do not add indexes without a workload/query reason.

## Flyway Performance
Migration performance must consider:
- duration;
- locks;
- large-table rewrites;
- index creation;
- transaction behavior;
- `CREATE INDEX CONCURRENTLY` where appropriate;
- backward compatibility;
- expand/contract deployment.

Applied migrations remain immutable.

## Connection Pools
Consider total possible connections:
`ECS task count × pool size`

Compare against PostgreSQL capacity and downstream limits. Longer queries/transactions reduce effective concurrency.

## SNS/SQS
Consider:
- producer throughput;
- consumer concurrency;
- receive/delete batching;
- message size;
- visibility timeout;
- retry/DLQ;
- downstream DB capacity;
- consumer lag.

Do not increase consumer concurrency without checking downstream capacity.

## Spring Batch
Consider:
- input volume;
- chunk size;
- transaction size;
- reader/processor/writer cost;
- persistence context;
- DB locks;
- restartability;
- parallelism.

Do not parallelize without evidence.

## ECS
Consider:
- CPU;
- memory;
- task count;
- autoscaling;
- startup/shutdown;
- connection pools;
- downstream capacity.

## Caching
Caching requires:
- explicit use case;
- key;
- TTL;
- invalidation;
- consistency;
- max size;
- memory impact;
- failure behavior.

## Concurrency
Bound:
- thread pools;
- executors;
- async tasks;
- DB connections;
- external calls.

Do not use concurrency to hide poor latency or capacity planning.

## Observability
Use standard Micrometer metrics first:
- request timers;
- external call timers;
- message processing timers;
- batch metrics;
- pool metrics.

Use bounded tags. Never use customerId, orderId, UUID, email, requestId, user input, or full URLs as metric labels.

## Performance Testing
Use as appropriate:
- smoke;
- load;
- stress;
- soak.

Record:
- workload;
- duration;
- concurrency;
- target;
- environment;
- result.

A developer laptop is not capacity evidence.

## Anti-Patterns
Avoid:
- optimization without measurements;
- caching without invalidation;
- speculative indexes;
- blind thread/pool increases;
- loading large datasets into memory;
- asynchronous execution merely to hide latency;
- complex concurrency without need;
- high-cardinality metrics;
- unsupported performance claims.

## AI Review
Ask:
1. What workload is expected?
2. What target is being protected?
3. Which query/call is potentially expensive?
4. Is there an N+1 risk?
5. Is transaction scope appropriate?
6. Is concurrency bounded?
7. Does ECS scaling multiply DB connections?
8. Are retries multiplying load?
9. Is observability sufficient?
10. Is there measurement evidence?

## Validation
Before completion:
- workload and target defined;
- hot paths reviewed;
- DB queries reviewed;
- indexes justified;
- pool/concurrency reviewed;
- external calls bounded;
- relevant performance tests/evidence produced;
- telemetry safe and useful.
## Performance Classification
Classify the change:
- latency-sensitive API;
- throughput-sensitive consumer;
- batch window-sensitive job;
- data-intensive persistence;
- startup/scaling-sensitive service;
- mixed.

The classification determines what evidence is required.

## Latency Budget
Do not set a global latency target without decomposing it. Consider:
application + DB + external calls + network + serialization.

A retry can multiply latency. A synchronous external call must fit the latency budget or be explicitly asynchronous.

## Database Capacity
Consider the total query rate produced by all ECS tasks, not one local process. Connection pools and consumer concurrency can multiply database load.

## Queue Capacity
Consumer concurrency must be aligned with:
- message processing time;
- DB write capacity;
- downstream APIs;
- visibility timeout;
- ECS autoscaling.

## Performance Regression Evidence
For a meaningful performance change record:
- baseline;
- change;
- workload;
- environment;
- result;
- conclusion.

Do not claim “faster” from code inspection alone.

## Memory
Watch for:
- loading all rows into collections;
- large JSON payloads;
- entity graphs;
- batch persistence contexts;
- unbounded queues;
- caches without limits.

## Retry Amplification
If a downstream dependency is slow, increasing retries/concurrency may make the problem worse. Model the combined load before changing retry or concurrency settings.

## Compléments détaillés v1.0.2
## 16. Detailed performance engineering and evidence

### 16.1 Classify the requirement
Classify each relevant change as:
- latency-sensitive API;
- throughput-sensitive API or consumer;
- large-data persistence/query;
- scheduled or window-constrained batch;
- resource/cost-sensitive workload;
- scalability or burst-handling change.
If performance is not material, record why rather than inventing targets.

### 16.2 Build a workload model
Capture the expected average and peak request/message rate, concurrency, payload size, data volume and growth, hot-key distribution, batch input size/window, external-service latency, and ECS task limits. Identify which values are measured, estimated, or assumed. Unknown assumptions that could change the design must remain visible.

### 16.3 Latency budget and capacity
- Allocate the end-to-end latency target across application processing, database, network, serialization, and downstream calls.
- Distinguish average, p95, p99, throughput, error rate, queue lag, and batch completion time.
- Estimate the maximum connection demand across all ECS tasks and compare it with database capacity.
- For queues, estimate arrival rate versus sustainable processing rate and recovery time after a backlog.
- Do not claim capacity from a developer laptop or from a test with an unrepresentative dataset.

### 16.4 Database and JPA performance
- Measure query count and duration for critical flows.
- Review N+1, selected columns, joins, sort cost, count queries, pagination strategy, transaction length, and persistence-context growth.
- Use projection or keyset pagination when justified by access pattern and scale.
- Avoid loading all records into memory; use bounded chunks/streams and understand transaction/persistence-context cleanup.
- Evaluate indexes against actual predicates and query plans; consider write cost and storage.
- Test with representative cardinality and data distribution, not only a handful of rows.

### 16.5 Retry amplification and queue stability
- Retries increase load during an outage. Bound attempts, add backoff/jitter where appropriate, and avoid retrying permanent errors.
- Set SQS visibility timeout relative to measured processing duration and batch behavior.
- Monitor queue depth/age/lag, retry rate, DLQ volume, and consumer throughput.
- Limit consumer concurrency to downstream capacity; increasing concurrency can saturate PostgreSQL or an external API.
- Define backlog recovery objectives and test partial outage/recovery where material.

### 16.6 Spring Batch performance
- Define input volume, expected processing time, chunk size, transaction size, restart behavior, memory bound, and database impact.
- Tune chunk size and reader/processor/writer only with evidence.
- Assess skip/retry rules, restartability, checkpointing, and idempotency before parallelizing.
- Avoid retaining every processed record in memory or logging every record.
- Record counts, duration, skip/retry/failure, and throughput with bounded tags.

### 16.7 ECS and resource behavior
- Check startup time, heap and container memory, CPU utilization, task count, autoscaling signals, graceful shutdown, and connection pools.
- Consider transient overlap during rolling deployment when calculating database connections.
- Ensure autoscaling signals correlate with demand and avoid oscillation.
- Test behavior under resource pressure and slow dependencies when it is a material risk.

### 16.8 Measurement protocol
A performance result must state workload, environment, data size, warm-up, duration, concurrency, version/commit, metrics, and result. Separate baseline from candidate and repeat noisy tests. Use smoke tests in CI for gross regressions and a representative environment for capacity claims.

### 16.9 Optimization decision
- Fix a measured bottleneck, not a stylistic suspicion.
- State expected benefit, complexity, operational cost, and regression risk.
- Prefer the simplest change that addresses the bottleneck.
- Re-run the same workload after optimization and retain the comparison.
- If no representative measurement exists, describe the hypothesis and avoid asserting that performance improved.

### 16.10 Performance Definition of Done
- [ ] Workload and targets are explicit or non-applicability is justified.
- [ ] Database, connection, queue, and batch capacity risks are assessed.
- [ ] Relevant performance telemetry uses standard Micrometer metrics first.
- [ ] Custom tags have bounded cardinality and contain no business identifiers.
- [ ] Results include reproducible workload/environment details.
- [ ] Trade-offs and remaining risks are documented.
