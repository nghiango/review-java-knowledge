# Observability Interview Questions

<!-- --8<-- [start:basic] -->
## Basic Questions (8)

### Q01: What are the three pillars of observability, and how do their data models and trade-offs differ?

??? question "Reveal answer"
    **Short Answer:**
    The three pillars are:
    1. **Metrics**: Aggregable numeric time-series data (counters, gauges, timers); cheapest to store; alerts on system health but lacks individual request context.
    2. **Traces**: Directed acyclic graphs of spans tracking request journeys across microservices; reveals network latency bottlenecks; requires sampling at scale.
    3. **Logs**: Timestamped structured JSON events; richest contextual payload and stack traces; highest storage and ingestion costs.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q01ThreePillarsOfObservabilityExample.java"
        ```

---

### Q02: What is Mapped Diagnostic Context (MDC) in SLF4J, and how does it correlate log lines?

??? question "Reveal answer"
    **Short Answer:**
    MDC is an SLF4J abstraction backed by `ThreadLocal` storage that holds contextual key-value pairs (`traceId`, `userId`, `clientIp`). Logback automatically extracts these pairs and appends them to every log statement executed on that thread, allowing distributed log aggregators to query all log events belonging to a single request.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q02Slf4jMdcBasicsExample.java"
        ```

---

### Q03: What are the core meter types in Micrometer?

??? question "Reveal answer"
    **Short Answer:**
    The four primary Micrometer meter types are:
    1. `Counter`: Monotonically increasing value tracking occurrences (requests, errors).
    2. `Timer`: Measures short-duration latency distributions and execution rates.
    3. `Gauge`: Instantaneous sample of variable state (queue depth, active pool connections).
    4. `DistributionSummary`: Measures distributions of non-time numeric quantities (payload sizes, batch item counts).

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q03MicrometerCoreMetersExample.java"
        ```

---

### Q04: How does Spring Boot Actuator expose health, metrics, and application information?

??? question "Reveal answer"
    **Short Answer:**
    Actuator exposes production-ready HTTP and JMX endpoints (`/actuator/health` for Kubernetes probes, `/actuator/prometheus` for Prometheus scraping, `/actuator/metrics` for dimensional inspection). Sensitive endpoints (`/actuator/env`, `/actuator/beans`, `/actuator/heapdump`) must be secured via Spring Security or bound to an internal management port (`management.server.port`).

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q04ActuatorEndpointsBasicsExample.java"
        ```

---

### Q05: What is structured logging, and why is it superior to human-readable plaintext logs in production?

??? question "Reveal answer"
    **Short Answer:**
    Structured logging formats log lines as machine-parsable JSON with discrete top-level attributes (`timestamp`, `level`, `traceId`, `message`, `userId`). Unlike plaintext logs that require fragile, CPU-heavy regular expression parsing in Logstash/FluentBit, structured JSON logs are indexed instantly and reliably by Elasticsearch, Datadog, or ClickHouse with zero ingestion loss.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q05StructuredJsonLoggingExample.java"
        ```

---

### Q06: What is the difference between a trace ID and a span ID in the W3C TraceContext standard?

??? question "Reveal answer"
    **Short Answer:**
    A **Trace ID** (32 hex characters) is a globally unique identifier assigned to an end-to-end user request at ingress that remains unchanged across all microservice hops. A **Span ID** (16 hex characters) identifies a single contiguous segment or operation within a specific service, linking to its caller via a parent span ID.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q06W3cTraceContextBasicsExample.java"
        ```

---

### Q07: What is the RED method, and where does it apply?

??? question "Reveal answer"
    **Short Answer:**
    The RED method (Tom Wilkie) applies to request-driven services (HTTP REST, gRPC, GraphQL):
    1. **Rate**: Throughput in requests per second.
    2. **Errors**: Number of failed requests per second.
    3. **Duration**: Distribution of time taken by requests (percentiles p50, p95, p99).

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q07RedMethodMonitoringExample.java"
        ```

---

### Q08: What is the USE method, and where does it apply?

??? question "Reveal answer"
    **Short Answer:**
    The USE method (Brendan Gregg) applies to hardware and system resources (CPU, Memory, Disks, Network, HikariCP, Thread Pools):
    1. **Utilization**: Average percentage of time the resource was busy performing work.
    2. **Saturation**: Volume of queued work waiting because capacity is exceeded.
    3. **Errors**: Count of error events or dropped operations.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q08UseMethodMonitoringExample.java"
        ```
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Questions (8)

### Q09: How does the Micrometer Observation API unify metrics and distributed tracing?

??? question "Reveal answer"
    **Short Answer:**
    The Micrometer Observation API provides a single instrumentation lifecycle (`Observation.start() -> openScope() -> stop()`). Registering `ObservationHandler` listeners allows a single observation block to simultaneously update a Prometheus `Timer` and manage an OpenTelemetry distributed tracing `Span` without duplicated code.

    **Internal Mechanism:**
    Observations separate `lowCardinalityKeyValues` (exported to both metrics tags and trace tags) from `highCardinalityKeyValues` (exported only to trace spans). This dual routing preserves Prometheus TSDB stability while ensuring rich diagnostic attribution in distributed traces.

    **Common Mistake:**
    Creating separate manual calls to `Timer.record()` and `Tracer.nextSpan()` in business services, leading to inconsistent naming, mismatched tag keys, and redundant boilerplate.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q09MicrometerObservationApiExample.java"
        ```

---

### Q10: How do you propagate trace context across HTTP boundaries?

??? question "Reveal answer"
    **Short Answer:**
    Distributed trace context is propagated across HTTP boundaries via standardized headers. The modern standard is W3C TraceContext (`traceparent: 00-{traceId}-{spanId}-{flags}`), replacing legacy Zipkin B3 headers (`X-B3-TraceId`, `X-B3-SpanId`). HTTP clients inject active span headers; downstream servers extract them to create child spans.

    **Internal Mechanism:**
    `TextMapPropagator.inject()` reads the active span from `Tracer.currentSpan()` and writes formatted headers into outbound HTTP request metadata. The downstream server's filter runs `TextMapPropagator.extract()`, setting the remote trace ID and parent span ID into the local thread's tracing context and MDC.

    **Common Mistake:**
    Failing to configure `RestClient.Builder` or `WebClient.Builder` with Spring Boot's auto-configured tracing interceptors, causing outbound HTTP calls to drop trace headers and break distributed call trees.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q10TracePropagationHttpHeaderExample.java"
        ```

---

### Q11: How do you propagate distributed trace headers across Apache Kafka record headers?

??? question "Reveal answer"
    **Short Answer:**
    Kafka producers inject W3C trace context into `ProducerRecord` headers as UTF-8 byte arrays (header key: `traceparent`). Kafka consumers extract the header before invoking the `@KafkaListener` method, restoring the trace context so downstream processing logs share the originating trace ID.

    **Internal Mechanism:**
    Spring Kafka provides `KafkaTemplate.setObservationEnabled(true)` and `ContainerProperties.setObservationEnabled(true)`. The framework's interceptors automatically write `traceparent` bytes on publish and parse them on consumption, establishing parent-child span links across asynchronous brokers.

    **Common Mistake:**
    Publishing raw records using un-instrumented native `KafkaProducer` instances without passing record headers, causing consumers to generate unrelated root trace IDs that fracture distributed call graphs.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q11TracePropagationKafkaHeadersExample.java"
        ```

---

### Q12: How does metric cardinality explosion occur in Prometheus / Micrometer, and how do you prevent it?

??? question "Reveal answer"
    **Short Answer:**
    Metric cardinality explosion occurs when dynamic, high-cardinality attributes (UUIDs, user IDs, emails, timestamps) are added as metric tags. Every distinct combination creates a permanent `Meter` object in the registry. Prevent it by restricting metric dimensions to small, static enums ($< 100$ combinations) and routing dynamic identifiers to structured logs or tracing spans.

    **Internal Mechanism:**
    `MeterRegistry` stores registered meters in a `ConcurrentHashMap` keyed by `Meter.Id(name, tags)`. In Prometheus, each meter becomes an independent time-series entry. Hundreds of thousands of dynamic meters exhaust JVM tenured heap and cause Prometheus scraper timeouts on `/actuator/prometheus`.

    **Common Mistake:**
    Tagging HTTP request timers with `order_id` or `customer_id` thinking it will enable customer-specific metrics in Grafana, only to crash production instances with `OutOfMemoryError`.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q12MetricCardinalityPreventionExample.java"
        ```

---

### Q13: Why does arithmetic average latency mislead engineering teams, and how do percentiles solve this?

??? question "Reveal answer"
    **Short Answer:**
    Arithmetic means ($\frac{\sum \text{latency}}{N}$) mask bimodal latency spikes because the massive volume of fast requests dilutes rare, severe delays. Percentiles (p50, p95, p99, p999) sort all observed latencies to report the exact maximum response time experienced by 50%, 95%, or 99% of users, directly revealing tail latency stalls and GC pauses.

    **Internal Mechanism:**
    Micrometer `Timer.publishPercentiles()` maintains a decay-biased histogram (such as HdrHistogram) that bins observations into logarithmic buckets without retaining raw sample objects, allowing bounded-memory quantile estimations.

    **Common Mistake:**
    Setting SLA breach alerts on average latency. An average of 100ms can easily conceal a 10-second hang experienced by 1% of checkout transactions.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q13PercentilesVsAverageLatencyExample.java"
        ```

---

### Q14: How do you propagate MDC context across Java thread pools?

??? question "Reveal answer"
    **Short Answer:**
    Because SLF4J MDC relies on `ThreadLocal`, submitting tasks to `ExecutorService` pools leaves worker threads with empty MDC contexts. Propagate context using a `TaskDecorator` or wrapping runnable that captures `MDC.getCopyOfContextMap()` prior to dispatch, sets it on the worker thread, and guarantees `MDC.clear()` in a `finally` block.

    **Internal Mechanism:**
    Spring's `ThreadPoolTaskExecutor.setTaskDecorator(TaskDecorator)` wraps every submitted `Runnable`. The decorator snapshots caller MDC, applies it inside the worker thread prior to `run()`, and clears it upon completion, preventing cross-tenant context leakage in pooled threads.

    **Common Mistake:**
    Setting values in MDC inside an asynchronous worker without a `finally { MDC.clear(); }` block, causing subsequent unrelated tasks executed on that thread to inherit stale correlation IDs.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q14MdcContextTaskDecoratorExample.java"
        ```

---

### Q15: How does Micrometer Tracing integrate with OpenTelemetry (OTel) and Brave bridges in Spring Boot 3?

??? question "Reveal answer"
    **Short Answer:**
    Micrometer Tracing acts as a vendor-neutral facade. In Spring Boot 3, adding `micrometer-tracing-bridge-otel` or `micrometer-tracing-bridge-brave` automatically configures the underlying tracer implementation while preserving unified Micrometer Observation annotations (`@Observed`) and OpenTelemetry exporter protocols (OTLP).

    **Internal Mechanism:**
    The bridge translates Micrometer's `TraceContext`, `Span`, and `Tracer` contracts to native OpenTelemetry SDK objects. Spans are batched by an `OtlpHttpSpanExporter` and pushed to an OpenTelemetry Collector daemon over gRPC or HTTP/protobuf.

    **Common Mistake:**
    Mixing both Brave and OpenTelemetry bridge dependencies on the classpath, which causes conflicting auto-configuration beans and duplicate span emissions.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q15OpenTelemetryBridgeIntegrationExample.java"
        ```

---

### Q16: How do you configure Logback masking rules to prevent PII and secrets from leaking into log aggregators?

??? question "Reveal answer"
    **Short Answer:**
    PII and secret masking in Logback is enforced using custom `CompositeConverter` or `PatternLayout` extensions that evaluate regular expressions against formatted messages prior to appender output, replacing matching patterns (credit card PANs, SSNs, API tokens) with masked placeholders (`****-****-****-1234`).

    **Internal Mechanism:**
    The custom converter overrides `transform(ILoggingEvent event, String in)`. It executes pre-compiled `Pattern` matchers against the message string, substituting sensitive capture groups before the log event is passed to `AsyncAppender` or serialized into JSON.

    **Common Mistake:**
    Compiling regex patterns dynamically inside the `transform()` method on every log invocation, causing massive CPU contention and GC pressure under high logging throughput.

    ??? example "Example"
        ```java
        --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q16LogbackMaskingPatternExample.java"
        ```
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Questions (5)

### Q17: How do you architect a multi-window multi-burn-rate alerting strategy based on Service Level Objectives (SLOs) and Error Budgets?

??? question "Reveal answer"
    **Short Answer:**
    Google SRE multi-window, multi-burn-rate alerting replaces naive error-rate thresholds by calculating how rapidly the application is consuming its contractual Error Budget across multiple rolling time windows (e.g. 1 hour, 6 hours, 3 days). Critical pages fire only when burn rate is high enough to exhaust budget within days, while slow burns trigger ticketing.

    **Deep Explanation:**
    For an SLO of 99.9% availability over 30 days, the Error Budget is 0.1% of all requests. A burn rate of 1.0 means the budget will be exactly exhausted in 30 days. Alerting requires multiple windows:
    - **14.4x burn rate (1h window)**: Consumes 2% of monthly budget in 1 hour; requires an immediate page to on-call engineers.
    - **6x burn rate (6h window)**: Consumes 5% of budget in 6 hours; requires a high-priority page.
    - **1x burn rate (3-day window)**: Consumes 10% of budget over 3 days; creates a Jira bug ticket for daytime review without waking on-call staff.
    Evaluating both a short window (e.g. 5 minutes) and a long window (e.g. 1 hour) prevents alert flapping from transient spikes.

    **Internal Mechanism:**
    Prometheus evaluates PromQL expressions computing error ratios over duration ranges:
    `rate(http_requests_total{status=~"5.."}[1h]) / rate(http_requests_total[1h]) > (14.4 * 0.001)`.

    **Example:**
    ```java
    --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q17SloErrorBudgetMultiBurnRateExample.java"
    ```

    **Common Mistake:**
    Setting alerts on static error counts (e.g. `errors > 10`), which causes alert fatigue during peak traffic and completely misses outages during low-traffic night hours.

    **Production Consideration:**
    Automate freeze policies: when 100% of the monthly error budget is burned, automated CI/CD gating halts feature deployments until reliability is restored.

    **Follow-up Questions:**
    1. How do you define an SLI (Service Level Indicator) for an asynchronous Kafka consumer?
    2. What is the difference between an SLA, an SLO, and an SLI?

---

### Q18: How do you manage trace sampling rates (head-based vs tail-based sampling) in high-throughput distributed architectures?

??? question "Reveal answer"
    **Short Answer:**
    Head-based sampling makes the sampling decision at the ingress edge proxy when the root span begins (e.g. 1% random). Tail-based sampling collects 100% of spans in an OpenTelemetry Collector memory buffer until the trace finishes, ensuring 100% of error traces and p99 high-latency traces are retained while sampling down redundant 200 OK traffic.

    **Deep Explanation:**
    At 100,000 requests per second, storing 100% of distributed traces generates petabytes of trace data and overwhelms storage backends. Head-based sampling is computationally cheap and memory-efficient, but a 1% sample rate misses 99% of rare production errors. Tail-based sampling buffers all spans for a request in the OpenTelemetry Collector cluster for 10–30 seconds. Once the final root span reports an error status or latency $> 2\text{s}$, the entire trace DAG is retained; otherwise, happy-path traces are sampled down to 0.1%.

    **Internal Mechanism:**
    The OpenTelemetry Collector `tail_sampling` processor buffers spans in an LRU trace cache keyed by `traceId`. Rules evaluate span attributes (`error=true`, `http.status_code >= 500`, `duration > 1500ms`) to decide whether to forward the batch to Jaeger/Tempo or discard it.

    **Example:**
    ```java
    --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q18HeadVsTailSamplingStrategiesExample.java"
    ```

    **Common Mistake:**
    Attempting to perform tail-based sampling inside application JVM processes, which consumes vast heap memory and risks garbage collection pauses under traffic spikes.

    **Production Consideration:**
    Route all spans for a given `traceId` to the same OpenTelemetry Collector instance using load-balancer trace-ID hashing, ensuring the collector sees the complete distributed call graph.

    **Follow-up Questions:**
    1. How does W3C TraceContext propagate the `trace-flags` sampled bit?
    2. What happens if a child span emits an error in a trace that was not sampled at the head?

---

### Q19: How do you diagnose and troubleshoot production thread pool starvation and deadlocks using thread dumps and Actuator metrics?

??? question "Reveal answer"
    **Short Answer:**
    Diagnose thread pool starvation by monitoring Actuator thread pool metrics (`executor.active`, `executor.queued`) and capturing sequential JVM thread dumps via `jcmd <pid> Thread.dump_to_file` or `GET /actuator/threaddump`. Inspect thread states (RUNNABLE vs WAITING/BLOCKED) and identify lock contention on database connections or synchronized blocks.

    **Deep Explanation:**
    When thread pool starvation occurs, incoming requests queue in memory, latency spikes, and timeouts cascade. A thread dump reveals what threads are actually doing:
    - Many threads in `WAITING (parking)` inside `LinkedBlockingQueue.take()` indicate an idle pool waiting for work.
    - Many threads in `TIMED_WAITING` inside `HikariPool.getConnection()` indicate database connection pool exhaustion.
    - Threads in `BLOCKED` on monitor locks indicate synchronized method contention.
    Taking 3 consecutive thread dumps spaced 10 seconds apart allows identifying threads that are stuck in the exact same call stack versus threads that are actively making progress.

    **Internal Mechanism:**
    `ThreadMXBean.dumpAllThreads(true, true)` invokes JVM native safepoint mechanics to capture thread execution frames, monitor locks (`ObjectMonitor`), and owned synchronizers (`ReentrantLock`).

    **Example:**
    ```java
    --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q19ThreadDumpDiagnosticsStarvationExample.java"
    ```

    **Common Mistake:**
    Increasing thread pool maximum size when starvation occurs, which worsens database connection contention and increases OS context switching overhead.

    **Production Consideration:**
    Automate thread dump capture: trigger an automated thread dump script when the p99 latency alert fires before Kubernetes restarts or terminates the pod.

    **Follow-up Questions:**
    1. How do thread dumps differ between platform OS threads and Java 21 Virtual Threads?
    2. What Actuator endpoint generates thread contention and CPU profiling data?

---

### Q20: How do you design an audit logging architecture that satisfies PCI-DSS, SOC2, and GDPR without impacting hot-path latency?

??? question "Reveal answer"
    **Short Answer:**
    An audit logging architecture decouples audit emission from transaction commit using asynchronous, bounded in-memory queues (LMAX Disruptor or Kafka) and writes events to immutable, append-only Write-Once-Read-Many (WORM) storage (AWS S3 Object Lock) with KMS envelope encryption and cryptographic hash chaining.

    **Deep Explanation:**
    Regulatory compliance mandates strict auditability:
    1. **Immutability & Integrity**: Audit records must not be modifiable or deletable by database administrators. Cryptographic HMAC chaining ($H_n = \text{HMAC}(H_{n-1}, \text{event}_n)$) detects tampering.
    2. **Attribution**: Every event records `actorId`, `timestamp`, `action`, `resourceId`, `clientIp`, and `correlationId`.
    3. **Performance Isolation**: Audit I/O must never block transactional payment or checkout threads. Events are placed onto a non-blocking ring buffer and dispatched asynchronously by background workers.

    **Internal Mechanism:**
    Audit log events are serialized to JSON with encrypted payload segments using AES-256-GCM. Log shipping agents stream events directly to an immutable S3 bucket protected with Object Lock in Compliance Mode.

    **Example:**
    ```java
    --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q20CompliantAuditLoggingArchitectureExample.java"
    ```

    **Common Mistake:**
    Writing audit records synchronously to the primary relational database inside business transactions, multiplying database lock durations and risking audit record loss during transaction rollbacks.

    **Production Consideration:**
    Implement a separate emergency disk-spill buffer so audit events are never dropped even if the centralized logging cluster experiences a network partition.

    **Follow-up Questions:**
    1. How does GDPR "Right to be Forgotten" apply to immutable audit logs?
    2. Why must audit logging occur even when authorization checks fail (403 Forbidden)?

---

### Q21: How do you implement zero-overhead observability in high-frequency trading or sub-millisecond systems without GC allocation pressure?

??? question "Reveal answer"
    **Short Answer:**
    Zero-overhead telemetry eliminates heap object allocations on the hot path: using lock-free striped primitives (`LongAdder`), reusable thread-local telemetry buffers, pre-allocated off-heap memory, and compile-time log level gating (`if (log.isDebugEnabled())`) to achieve sub-microsecond recording latency without GC churn.

    **Deep Explanation:**
    In ultra-low-latency architectures, allocating objects (`new String()`, boxed `Double`, `HashMap$Node`) on every transaction generates Young Generation memory churn that triggers garbage collection pauses. Zero-overhead telemetry achieves observability through:
    1. **Zero-Allocation Metrics**: Counters and gauges use `LongAdder` or pre-allocated off-heap arrays.
    2. **Lock-Free Ring Buffers**: Events are published to pre-allocated ring buffers (LMAX Disruptor) without locks or synchronization.
    3. **Binary Serialization**: Encoders serialize fields directly into pre-allocated `ByteBuffer` slices using Chronicle Queue or SBE (Simple Binary Encoding).

    **Internal Mechanism:**
    `LongAdder` maintains a table of cell variables updated using `Unsafe.compareAndSwapLong()`. Threads hash to different cells, completely eliminating CPU cache-line bouncing and lock contention.

    **Example:**
    ```java
    --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q21ZeroOverheadTelemetryMemoryExample.java"
    ```

    **Common Mistake:**
    Using Java `String.format()` or string concatenation inside high-frequency trading loops, allocating millions of ephemeral strings per second.

    **Production Consideration:**
    Pin telemetry worker threads to isolated CPU cores to prevent telemetry I/O from descheduling time-critical trading threads.

    **Follow-up Questions:**
    1. What is Java Flight Recorder (JFR) event recording overhead compared to user-space logging?
    2. How does `Striped64` minimize cache false sharing?
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Scenario Questions (2)

### Q22: Incident: Prometheus scraper crashes with OutOfMemoryError and Grafana alerts go dark during high-traffic flash sale. How do you triage and resolve?

??? question "Reveal answer"
    **Short Answer:**
    The root cause is metric cardinality explosion caused by a developer adding a dynamic tag (e.g. `order_id` or `customer_email`) to a high-frequency metric. Hundreds of thousands of unique series overwhelmed the `MeterRegistry` and caused Prometheus scrape timeouts. Immediately apply a `MeterFilter` to drop the rogue metric tags at runtime, remove the tag in code, and clear Prometheus scrape buffers.

    **Deep Explanation:**
    During the flash sale, 500,000 customers created orders. A newly deployed feature tagged `orders.created.total` with `customer_email`. Each order registered a new `Counter` in `MeterRegistry`. Generating `/actuator/prometheus` scrape output required serializing 500,000 lines of OpenMetrics text (over 80 MB per scrape). The scrape timed out after 10s, Prometheus marked the target as down, and Grafana alert evaluation stopped because metrics were missing.

    **Internal Mechanism:**
    Heap dumps show `MeterRegistry`'s `ConcurrentHashMap` holding millions of `Meter.Id` and `Tag` objects in Tenured Space.

    **Example:**
    ```java
    --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q22IncidentPrometheusCardinalityOomExample.java"
    ```

    **Common Mistake:**
    Restarting pods without configuration changes, which temporarily alleviates heap pressure but crashes again within 10 minutes as customers continue purchasing.

    **Production Consideration:**
    Configure a global `MeterFilter` with maximum meter limits (`MeterFilter.maximumAllowableTags()`) to reject rogue series automatically before they exhaust heap memory.

    **Follow-up Questions:**
    1. How do you configure Prometheus `relabel_configs` to drop high-cardinality metrics at scrape time?
    2. What Micrometer metric tracks total registered meter count?

---

### Q23: Incident: Asynchronous payment processing fails silently for 10,000 customers while Grafana error dashboards report 0% failure rate. How do you identify the defect and remediate?

??? question "Reveal answer"
    **Short Answer:**
    The defect is swallowed exceptions in the payment consumer service: code caught `Exception`, logged only `e.getMessage()` without a stack trace, and returned `false` without incrementing an error metric counter. Remediate by tracking operations symmetrically (`status="success|failure"`), passing full `Throwable` objects in log statements, and propagating domain exceptions.

    **Deep Explanation:**
    The payment execution pipeline offloaded transactions to an asynchronous worker thread pool. When a downstream bank API changed its SSL certificate, socket connections threw `SSLHandshakeException`. The application caught `Exception`, printed `log.error("Payment failed: " + e.getMessage())`, and returned `false`. Because `successCounter.increment()` was placed in the try block, it never fired, while no failure counter existed. Grafana dashboards calculating `errors / total` saw 0 errors and evaluated the error rate as 0.00%.

    **Internal Mechanism:**
    Because MDC was not propagated to the worker threads, log lines lacked `correlationId` and were disconnected from the original user checkout requests in Kibana.

    **Example:**
    ```java
    --8<-- "modules/22-observability/src/examples/java/lab/observability/questions/Q23IncidentSilentFailureMdcLossExample.java"
    ```

    **Common Mistake:**
    Assuming that because error rate dashboards are green, the business process is operating successfully.

    **Production Consideration:**
    Implement end-to-end synthetic transaction probes (canary transactions) that verify business completion through the entire pipeline and alert if completion count drops below baseline.

    **Follow-up Questions:**
    1. How does Micrometer Observation handle exception recording automatically?
    2. Why must failure metrics record the exception class name as a tag?
<!-- --8<-- [end:scenarios] -->

---

## Related

- [Concepts](concepts.md) — Three pillars, structured logging, and metric dimensions
- [Internals](internals.md) — Observation lifecycle and Prometheus scraper mechanics
- [Code Review](code-review.md) — Broken examples demonstrating context loss and cardinality bugs
- [Solutions](solutions.md) — Production solutions for context propagation and histograms
