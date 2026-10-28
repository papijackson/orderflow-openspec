---
applyTo: "**/src/main/java/**/*.java,**/src/test/java/**/*.java,**/src/main/resources/**/*.yml,**/src/main/resources/**/*.yaml,**/src/main/resources/**/*.properties"
---

# Observability Instructions

## Standard
The application observability flow is:
`Application -> Micrometer -> Spring Boot Actuator -> Prometheus`

Prefer standard instrumentation before custom instrumentation.

## Logging
Use levels deliberately:
- ERROR: actionable failures;
- WARN: degraded/unexpected but handled conditions;
- INFO: important lifecycle/business operational events;
- DEBUG: diagnostic detail.

Prefer structured operational logs.

Never log:
- passwords;
- access tokens;
- refresh tokens;
- API keys;
- credentials;
- authentication headers;
- private keys;
- sensitive request/response bodies.

Avoid duplicate exception logging at every layer.

## Correlation and Tracing
Use the project's standard correlation/tracing mechanism. Correlation identifiers may appear in logs where appropriate.

Do not use correlation/request IDs as metric labels.

## Metrics
Prefer standard:
- HTTP;
- JVM;
- process;
- datasource;
- connection pool;
- executor;
- cache;
- messaging;
- batch.

Custom metrics use:
- Counter;
- Timer;
- DistributionSummary;
- Gauge.

Every custom metric needs a clear operational or business purpose.

## Naming
Metric names must be:
- stable;
- meaningful;
- consistent;
- documented where custom.

Do not generate metric names dynamically.

## Cardinality
Metric tags must be bounded.

Forbidden tags:
- customerId;
- orderId;
- UUID;
- email;
- requestId;
- sessionId;
- arbitrary user strings;
- full URL;
- exception message;
- stack trace.

Preferred tags:
- operation;
- status;
- outcome;
- eventType;
- channel;
- dependency.

## Business Metrics
Examples:
- `orders_created_total`;
- `payments_failed_total`;
- `batch_records_processed_total`;
- `message_processing_failures_total`.

Use bounded dimensions such as status/channel.

## Timers
Use timers for:
- HTTP requests;
- external calls;
- message processing;
- batch steps;
- meaningful DB operations where instrumentation is justified.

Do not instrument every method mechanically.

## Actuator
Expose only required endpoints. Typical useful endpoints:
- health;
- info;
- metrics;
- prometheus.

Secure sensitive endpoints. Do not expose environment/configuration/secrets unnecessarily.

## Health
Distinguish:
- liveness: process can continue;
- readiness: process can accept traffic.

Do not make liveness fail because one external dependency is temporarily unavailable.

## External Dependencies
Capture:
- dependency;
- operation;
- duration;
- success/failure;
- timeout;
- retry outcome.

Do not log payloads or use resource IDs as metric labels.

## Messaging
Observe:
- received;
- processed successfully;
- failed;
- retried;
- DLQ;
- processing duration;
- lag where meaningful.

No message IDs or business IDs as metric tags.

## Batch
Observe:
- job execution;
- status;
- duration;
- step status;
- records processed;
- skip/retry/failure.

Do not put unbounded job parameters into metric labels.

## Errors
Do not swallow errors. Do not log and rethrow at every layer. Log at the boundary where the failure becomes actionable and preserve context appropriately.

## Performance
Telemetry must be lightweight:
- no per-record logs in high-volume loops;
- no metric creation inside loops;
- no dynamic names;
- no unbounded tags.

## AI Review
Ask:
1. Can operators diagnose the failure?
2. Are standard metrics already sufficient?
3. Does each custom metric have a purpose?
4. Are labels bounded?
5. Is sensitive information excluded?
6. Are external calls observable?
7. Are messaging retries/DLQ visible?
8. Are batch outcomes visible?
9. Are health checks semantically correct?
10. Is logging duplicated or noisy?

## Validation
Verify:
- standard Micrometer metrics enabled where appropriate;
- Prometheus endpoint secured;
- logs contain useful context;
- secrets excluded;
- custom metrics bounded;
- messaging/batch telemetry present where relevant.
## Logging Content
Useful operational context may include:
- operation;
- outcome;
- bounded business classification;
- dependency;
- duration;
- error category.

Do not log entire payloads by default.

## Correlation
Correlation IDs are diagnostic data, not dimensions for metrics. Keep their cardinality out of metric systems.

## Metric Lifecycle
Register metrics once through Micrometer instrumentation. Never dynamically create a new metric for every customer/order/request.

## Standard Metrics First
Before creating a custom metric, check whether Micrometer/Spring Boot already provides:
- HTTP server metrics;
- JVM/process;
- datasource/pool;
- executor;
- client observations;
- messaging/batch instrumentation.

## Alerts
Metrics should support actionable questions such as:
- Is the service unavailable?
- Are requests slowing?
- Are failures increasing?
- Are messages accumulating?
- Are consumers sending messages to DLQ?
- Is a batch missing its window?

Avoid metrics that look interesting but cannot drive an operational decision.

## Sensitive Data
Observability data is production data. Apply the same confidentiality rules to logs, traces, metrics, dashboards, and exported diagnostics.

## Compléments détaillés v1.0.2
## 14. Detailed observability contract

### 14.1 Standard-first instrumentation
- Prefer Spring Boot/Micrometer standard HTTP, JVM, process, datasource, executor, cache, and messaging instrumentation before writing custom metrics.
- Keep the standard pipeline: application → Micrometer → Actuator → Prometheus.
- Use `/actuator/prometheus` where Prometheus scraping is the agreed deployment pattern.
- Add a custom metric only when it answers a concrete operational or business question not already covered by standard metrics.
- Name and document custom metrics consistently, including unit and intended interpretation.

### 14.2 Metric type and lifecycle
- Counter: monotonically increasing events or failures.
- Timer: duration and count of an operation.
- DistributionSummary: distribution of non-time values such as payload size or records per operation.
- Gauge: current state only when the measured object has a well-defined lifecycle and cannot be garbage-collected unexpectedly.
- Avoid dynamically registering a new metric per request, record, customer, or arbitrary label combination.
- Do not increment counters in tight loops if an aggregate count can be recorded safely at a meaningful boundary.

### 14.3 Cardinality and sensitive data
Forbidden metric tags include customer/order/account IDs, UUIDs, email addresses, session/request/correlation IDs, arbitrary exception messages, full URLs, raw user input, and unbounded job parameters.
Prefer bounded tags such as operation, status, outcome, event type, channel, dependency name, or controlled error category. Correlation identifiers may be logged or traced where approved but must not be metric dimensions. Never put secrets, tokens, credentials, personal data, or sensitive payloads in logs, metrics, traces, or error responses.

### 14.4 Logs, traces, and correlation
- Use consistent structured fields for service, environment, operation, outcome, and correlation/trace identifiers where the platform supports them.
- Log at the layer that can add useful context; avoid duplicate stack traces as exceptions bubble up.
- Do not log request/response bodies by default. If payload logging is explicitly approved, redact and bound it.
- Use INFO for meaningful lifecycle/business-operational events, WARN for recoverable abnormal conditions, ERROR for failures requiring attention, and DEBUG for diagnostic detail.
- Ensure log volume remains bounded for batch and message-per-record flows.

### 14.5 Health and alertability
- Separate liveness (the process can run) from readiness (the instance can accept work).
- Do not make liveness depend on every downstream system; this can cause restart storms during dependency outages.
- Expose only necessary Actuator endpoints and secure them at both application and network levels.
- Every operational metric added for a known risk should have a clear consumer: dashboard, alert, or documented diagnostic use.
- Avoid alerting on a metric without a threshold, owner, and expected response where alerting is required.

### 14.6 Messaging and batch signals
Messaging should expose meaningful counts/durations for received, processed, failed, retried, sent to DLQ, and backlog/age where available. Batch should expose execution status/duration, records read/written/skipped, retry/failure, and throughput. Keep job names, step names, channels, and outcomes bounded; do not add arbitrary job parameters as tags.

### 14.7 Observability Definition of Done
- [ ] Standard metrics are enabled before custom instrumentation is added.
- [ ] Custom metric names, types, units, and purpose are documented.
- [ ] Tags are bounded and contain no sensitive identifiers.
- [ ] Logs are actionable, structured where supported, and free of secrets.
- [ ] Health endpoints have correct liveness/readiness semantics and restricted exposure.
- [ ] Important failure, retry, latency, and backlog signals are observable.
