# Solution: Excessive hot-path allocation in metric line encoder

## Annotated code

```java
package lab.jvm.broken.excessivehotpathallocation;

import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MetricLineEncoder {
    private static final Logger LOGGER = Logger.getLogger(MetricLineEncoder.class.getName());

    public String encode(String name, Map<String, String> tags, long value) {
        // Performance issue: Compiling a regex pattern on every method call inside a hot loop
        // repeatedly allocates Pattern and Node graph objects and recompiles the regex.
        if (!Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$").matcher(name).matches()) {
            throw new IllegalArgumentException("invalid name: " + name);
        }

        // Observability issue: Logging every metric line at info level allocates LogRecord objects,
        // message strings, and saturates logging appenders on critical telemetry hot paths.
        LOGGER.info(() -> "encoding metric for " + name);

        // Performance issue: Chaining Stream pipelines, entrySet wrappers, lambda instances,
        // intermediate Collectors, and String.format allocates dozens of short-lived objects per event.
        // Maintainability issue: Chained replaceAll calls perform repeated regex passes and copy
        // intermediate strings instead of using a structured single-pass character appender.
        String formattedTags =
                tags.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(
                                entry -> {
                                    String escapedKey =
                                            entry.getKey()
                                                    .replaceAll("\\\\", "\\\\\\\\")
                                                    .replaceAll(",", "\\\\,")
                                                    .replaceAll("=", "\\\\=");
                                    String escapedVal =
                                            entry.getValue()
                                                    .replaceAll("\\\\", "\\\\\\\\")
                                                    .replaceAll(",", "\\\\,")
                                                    .replaceAll("=", "\\\\=");
                                    return String.format("%s=%s", escapedKey, escapedVal);
                                })
                        .collect(Collectors.joining(","));

        // Design issue: Operating directly on raw untyped strings and maps bypasses validation at
        // creation time and forces repetitive defensive checks and string formatting downstream.
        if (formattedTags.isEmpty()) {
            return String.format("%s=%d", name, value);
        }
        return String.format("%s{%s}=%d", name, formattedTags, value);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Performance issue | High | `encode()` | Regex compiled on every call in hot path |
| 2 | Observability issue | High | `encode()` | Info logging on critical telemetry path |
| 3 | Performance issue | High | `formattedTags` | Stream pipeline and `String.format` allocate heavily in young gen |
| 4 | Maintainability issue | Medium | `replaceAll` | Chained regex replacement allocates intermediate string copies |
| 5 | Design issue | Medium | `encode()` | Operates on untyped loose arguments without boundary validation |

## Issue details

### Regex pattern compiled on hot path

**Type:** Performance issue · **Severity:** High · **Difficulty:** Basic

**Problem:** `Pattern.compile` is executed on every invocation of `encode`.

**Why it happens:** The regex string is passed inline to `Pattern.compile` rather than held in a static final constant.

**Production impact:** Wastes significant CPU cycles parsing regex syntax and creates high young-generation garbage churn under high throughput.

**Correct implementation:** Precompile the pattern into a `private static final Pattern NAME_PATTERN` constant or validate once at domain object construction (`MetricEvent`).

**Why the solution works:** Pattern compilation happens once during class initialization; subsequent validations only perform matching.

**Trade-offs:** Precompiled patterns consume a tiny amount of static memory, which is negligible compared to repeated compilation.

**How to detect it:** CPU profiler flame graphs showing high time spent in `java.util.regex.Pattern.compile` or JFR object allocation profiles.

**Interview follow-up:** When is `Pattern.compile` acceptable inside a method body?

### Hot-path info logging

**Type:** Observability issue · **Severity:** High · **Difficulty:** Basic

**Problem:** Every single metric encoding triggers a logger call at INFO level.

**Why it happens:** Developers add logging to verify telemetry processing without considering volume.

**Production impact:** At millions of events per second, logging queues block worker threads, disk I/O saturates, and log storage costs skyrocket.

**Correct implementation:** Remove per-event INFO logging from telemetry encoders. Use aggregate counters or sampled debug logging if tracing is required.

**Why the solution works:** Eliminates I/O and object allocations from the hot execution path.

**Trade-offs:** Individual metric encoding operations are not logged, requiring aggregate metrics or distributed tracing for visibility.

**How to detect it:** Sudden surges in log volume, disk I/O saturation, and thread contention on logger appenders.

**Interview follow-up:** How do you safely log in high-throughput components?

### Excessive young-gen allocation from streams and String.format

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** Converting tags via Streams, lambdas, Collectors, and `String.format` creates dozens of temporary objects per event.

**Why it happens:** Functional stream abstractions and `String.format` are convenient for readable code but unoptimized for zero-allocation telemetry encoders.

**Production impact:** Drives high TLAB churn, triggers frequent minor GCs, and increases tail latency (p99/p999) across request handling threads.

**Correct implementation:** Use a single operation-local `StringBuilder` sized with initial capacity and iterate directly without intermediate wrapper collections.

**Why the solution works:** Minimizes heap allocations to a single contiguous buffer that can be allocated directly in TLAB or scalar-replaced by JIT.

**Trade-offs:** Imperative loop code is slightly more verbose than functional stream pipelines.

**How to detect it:** Java Flight Recorder (JFR) `jdk.ObjectAllocationInNewTLAB` and `jdk.ObjectAllocationOutsideTLAB` events pointing to stream pipelines.

**Interview follow-up:** Why does `String.format` cause high allocation overhead?

### Chained regex replacements for character escaping

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

**Problem:** Multiple `replaceAll` calls parse regex expressions and allocate intermediate strings for each replaced character.

**Why it happens:** `replaceAll` is used as a quick shortcut for character escaping.

**Production impact:** Repeated string copying and regex matcher allocations per tag key and value.

**Correct implementation:** A single-pass character scan appending escaped characters directly to the target `StringBuilder`.

**Why the solution works:** Traverses the string in $O(N)$ with zero intermediate `String` or `Pattern` allocations.

**Trade-offs:** Requires a helper method rather than a one-line standard library call.

**How to detect it:** High allocation of `String` and `char[]`/`byte[]` objects in profilers.

**Interview follow-up:** How does `String.replace(CharSequence, CharSequence)` differ from `String.replaceAll(String, String)`?

### Untyped loose arguments without boundary validation

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

**Problem:** Method takes loose parameters `String name, Map<String, String> tags, long value` allowing mutable external maps and unvalidated state.

**Why it happens:** Data model encapsulation was skipped in favor of a procedural utility method.

**Production impact:** Callers can pass nulls, invalid characters, or mutate tags concurrently while encoding is taking place.

**Correct implementation:** Model metric data as an immutable `MetricEvent` record with constructor validation and defensive copying (`Map.copyOf`).

**Why the solution works:** Guarantees valid, thread-safe, immutable data before entering the encoder.

**Trade-offs:** Small one-time object creation for event instance if not already modeled as a record.

**How to detect it:** `NullPointerException` or corrupted metric lines during concurrent tag modifications.

**Interview follow-up:** Why are Java records ideal for telemetry and messaging domain payloads?

## Correct implementation

Package: `lab.jvm.allocation`

- `src/main/java/lab/jvm/allocation/MetricEvent.java`
- `src/main/java/lab/jvm/allocation/MetricLineEncoder.java`
- `docs/topics/jvm/solutions.md#allocation-conscious-metric-encoding`
