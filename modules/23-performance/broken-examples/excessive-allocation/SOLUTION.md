# Solution: metric line encoder

## Annotated code

```java
package lab.performance.broken.allocation;

import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MetricLineEncoder {
    private static final Logger LOGGER = Logger.getLogger(MetricLineEncoder.class.getName());

    public String encode(String name, Map<String, String> tags, long value) {
        // Performance issue: A regular expression is compiled for every metric.
        Pattern invalid = Pattern.compile("[^a-zA-Z0-9_.-]");
        String normalized = invalid.matcher(name).replaceAll("_");
        // Performance issue: Intermediate maps, stream nodes, and strings inflate allocation rate.
        Map<String, String> escaped =
                tags.entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().replace(",", "\\,")));
        String encodedTags =
                escaped.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(","));
        // Performance issue: String.format builds formatter machinery on the hot path.
        String line = String.format("%s{%s}=%d", normalized, encodedTags, value);
        // Observability issue: Logging every metric duplicates a high-volume event stream.
        LOGGER.info(line);
        return line;
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Performance issue | High | `encode()` | Regex compiled per event |
| 2 | Performance issue | High | `encode()` | Multiple intermediate allocations |
| 3 | Performance issue | Medium | `encode()` | General formatter used in hot path |
| 4 | Observability issue | High | `encode()` | Every encoded value is logged |

## Issue details

### Regex compiled per event
**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate  
**Technology:** Java regex · **Interview frequency:** Medium · **Production impact:** High

**Problem:** invariant parsing work repeats. **Why it happens:** `Pattern.compile` is inside the
method. **Production impact:** CPU and allocation rise with event rate. **Correct implementation:**
validate once at construction or avoid regex for simple escaping. **Trade-offs:** specialized code
is less declarative. **How to detect it:** JFR allocation and CPU profiles. **Interview follow-up:**
Why must a benchmark include warmup?

### Intermediate allocation chain
**Type:** Performance issue · **Severity:** High · **Difficulty:** Senior  
**Technology:** streams, GC · **Interview frequency:** High · **Production impact:** High

**Problem:** maps and strings exist only to build another string. **Why it happens:** each pipeline
materializes a representation. **Production impact:** young GC frequency and tail latency rise.
**Correct implementation:** append into one operation-local builder. **Trade-offs:** imperative
code needs focused tests. **How to detect it:** JFR allocation flame graph. **Interview follow-up:**
Why is a shared builder unsafe?

### General formatter in hot path
**Type:** Performance issue · **Severity:** Medium · **Difficulty:** Intermediate  
**Technology:** formatting · **Interview frequency:** Medium · **Production impact:** Medium

**Problem:** a general formatter handles a fixed wire shape. **Why it happens:** convenience wins
without measurement. **Production impact:** extra allocation and CPU. **Correct implementation:**
direct appends. **Trade-offs:** preserve protocol tests. **How to detect it:** profile before and
after with identical input. **Interview follow-up:** Why is wall-clock unit testing invalid?

### Per-event logging
**Type:** Observability issue · **Severity:** High · **Difficulty:** Intermediate  
**Technology:** logging · **Interview frequency:** High · **Production impact:** High

**Problem:** telemetry payloads become another telemetry stream. **Why it happens:** debug output
was left at info. **Production impact:** I/O, storage cost, and log backpressure. **Correct
implementation:** aggregate counters and sampled diagnostics. **Trade-offs:** individual events
are unavailable by default. **How to detect it:** log rate and logger CPU. **Interview follow-up:**
What sampling preserves rare failures?

## Correct implementation

See package `lab.performance.allocation` and
[solutions](../../../docs/topics/performance/solutions.md#allocation-conscious-encoding).
