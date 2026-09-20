# Observability Issues

Missing context, unsafe or absent logs, weak metrics and broken trace propagation.

## Entries

### Asynchronous failure loses operation context

**Type:** Observability issue · **Severity:** High · **Difficulty:** Basic

Replacing an exception without its cause and input identity makes concurrent failures impossible to
attribute. Preserve the cause and a safe domain identifier in a typed exception.

**Appears in:** [Core Java — stream side effects](../topics/core-java/code-review.md#stream-and-parallel-side-effects)

### Cache has no size or hit-rate metrics

**Type:** Observability issue · **Severity:** Medium · **Difficulty:** Intermediate

Without cache size, hit, miss and eviction metrics, memory pressure looks detached from the cache
that caused it. Export aggregate cache metrics and avoid high-cardinality tags.

**Appears in:** `modules/02-jvm/broken-examples/unbounded-cache`

### Hot-path cache logging

**Type:** Observability issue · **Severity:** Medium · **Difficulty:** Basic

Logging every cache miss at info level creates noise and ingestion cost while hiding the aggregate
behavior operators need. Prefer metrics plus sampled diagnostics.

**Appears in:** `modules/02-jvm/broken-examples/unbounded-cache`

### Hot-path telemetry logging

**Type:** Observability issue · **Severity:** High · **Difficulty:** Basic

Emitting INFO logs on every metric encoding invocation saturates logging appenders, generates huge
log volumes and adds latency overhead to critical hot paths. Reserve logging for aggregate summaries
or sampled diagnostics.

**Appears in:** `modules/02-jvm/broken-examples/excessive-hot-path-allocation`

---

### String Concatenation Instead of Parameterized Logging

**Type:** Observability issue · **Severity:** Medium · **Difficulty:** Basic

**Technology:** SLF4J, Logback, Structured Logging · **Interview frequency:** Medium · **Production impact:** Medium

Using string concatenation (`+`) inside log statements forces the JVM to eagerly construct and allocate `String` objects regardless of whether the target logging level is enabled. Furthermore, string concatenation allows unescaped newline characters (`\r\n`) to be injected into logs, enabling log-forging attacks. Always use SLF4J parameterized placeholders (`log.info("user={} ip={}", username, clientIp)`).

**Appears in:** `modules/22-observability/broken-examples/logging-secrets-pii`

---

### MDC Context Lost Across Asynchronous Thread Pool Boundaries

**Type:** Observability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** SLF4J MDC, Logback, ExecutorService · **Interview frequency:** High · **Production impact:** High

Because SLF4J's Mapped Diagnostic Context (MDC) is backed by `ThreadLocal`, submitting asynchronous tasks to thread pools (`ExecutorService`, `ForkJoinPool`, `CompletableFuture.runAsync`) causes worker threads to run with empty MDC contexts. Asynchronous background logs become unlinked from incoming HTTP request trace IDs and correlation IDs, preventing unified trace correlation in log aggregators. Capture `MDC.getCopyOfContextMap()` before dispatch and restore it on worker threads.

**Appears in:** `modules/22-observability/broken-examples/missing-correlation-id-async`

---

### Unbounded Metric Tags Cause Scraper Timeouts and TSDB Failures

**Type:** Observability issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Micrometer, Prometheus, Actuator · **Interview frequency:** High · **Production impact:** Critical

Adding unbounded dynamic values (e.g., `order_id`, `user_id`, `email`) as tags to Micrometer meters causes metric cardinality explosion. Prometheus endpoints (`/actuator/prometheus`) must format millions of distinct time-series entries on every scrape, causing scrape timeouts, CPU spikes, and TSDB storage crashes. Restrict metric tags strictly to low-cardinality categorical dimensions and record high-cardinality identifiers in structured logs or distributed tracing spans.

**Appears in:** `modules/22-observability/broken-examples/high-cardinality-metric-tags`

---

### Exception Stack Trace Dropped by Logging Only e.getMessage()

**Type:** Observability issue · **Severity:** High · **Difficulty:** Basic

**Technology:** SLF4J, Logback · **Interview frequency:** High · **Production impact:** High

Calling `log.error("Failed: " + e.getMessage())` or `log.error("Failed: {}", e.getMessage())` logs only the exception's message string and completely omits the `Throwable` causal stack trace and line numbers. When exceptions have null messages (common in `NullPointerException`), the log prints `"Failed: null"`. Always pass the `Throwable` as the final unformatted argument to preserve the causal stack trace.

**Appears in:** `modules/22-observability/broken-examples/swallowed-exceptions-observability`

---

### Asymmetric Metrics Hide Production Failure Rate

**Type:** Observability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Micrometer, Prometheus · **Interview frequency:** High · **Production impact:** High

Incrementing metrics only on happy-path success while catching and logging exceptions without failure metric increments conceals outages from monitoring dashboards. Operational dashboards calculating error ratios observe 0 errors, preventing alert triggers during downstream system outages. Track operations symmetrically using status tags (`status="success|failure"`) or separate error counters.

**Appears in:** `modules/22-observability/broken-examples/swallowed-exceptions-observability`

---

### Arithmetic Mean Latency Obscures Severe Tail Latency Spikes

**Type:** Observability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Micrometer, Prometheus, Histograms · **Interview frequency:** High · **Production impact:** High

Measuring service latency using arithmetic means (`totalTime / totalRequests`) masks bimodal and heavily skewed latency distributions. A reported average of 100ms can easily conceal a 10,000ms p99 tail latency spike experienced by thousands of customers. Configure Micrometer timers with explicit percentiles (`publishPercentiles(0.5, 0.95, 0.99)`) or percentile histograms to expose tail outliers and GC pauses.

**Appears in:** `modules/22-observability/broken-examples/missing-latency-histogram`

---

### Missing Service Level Objective (SLO) Histogram Boundaries

**Type:** Observability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Micrometer, Prometheus, SLO Alerts · **Interview frequency:** High · **Production impact:** High

Without defining explicit latency histogram buckets (`serviceLevelObjectives(...)`), Prometheus cannot calculate precise multi-window error budgets or burn rates against contractual Service Level Agreements (e.g. $99\%$ of requests $< 500\text{ms}$). Define explicit SLO duration boundaries on critical business timers to enable reliable SLO alerting.

**Appears in:** `modules/22-observability/broken-examples/missing-latency-histogram`

## Related

- [Issue catalogue](index.md)
