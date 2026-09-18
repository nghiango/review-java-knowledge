# Solution: Unbounded template cache

## Annotated code

```java
package lab.jvm.broken.unboundedcache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class TemplateCache {
    private static final Logger LOGGER = Logger.getLogger(TemplateCache.class.getName());
    // Memory issue: A static cache with no maximum size or expiry retains rendered templates for
    // the process lifetime.
    // Scalability issue: The key includes caller-controlled model content, so key cardinality can
    // grow with request input rather than with a bounded template catalogue.
    // Observability issue: There are no metrics for size, hit rate, miss rate or evictions, so the
    // cache can grow silently until heap pressure appears elsewhere.
    private static final Map<String, String> RENDERED_TEMPLATES = new ConcurrentHashMap<>();

    public String render(String tenantId, String templateName, Map<String, String> model) {
        // Design issue: Cache policy is hidden inside a static map instead of being an explicit
        // bounded dependency configured at the component boundary.
        String key = tenantId + ":" + templateName + ":" + model;
        return RENDERED_TEMPLATES.computeIfAbsent(key, this::renderTemplate);
    }

    public int size() {
        return RENDERED_TEMPLATES.size();
    }

    private String renderTemplate(String key) {
        // Observability issue: Logging every render at info level creates high-volume logs on a hot
        // path while still omitting the useful cache metrics.
        LOGGER.info(() -> "rendering template " + key);
        return "<html><body>" + key.repeat(128) + "</body></html>";
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Memory issue | High | `RENDERED_TEMPLATES` | Static cache has no capacity or expiry |
| 2 | Scalability issue | High | `render()` | Caller-controlled model content creates unbounded key cardinality |
| 3 | Observability issue | Medium | `RENDERED_TEMPLATES` | No size, hit, miss or eviction metrics |
| 4 | Design issue | Medium | `render()` | Cache policy is hidden in static state |
| 5 | Observability issue | Medium | `renderTemplate()` | Info logging on a hot render path creates noisy logs |

## Issue details

### Static cache without bounds

**Type:** Memory issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** The cache can grow for the lifetime of the process.

**Why it happens:** `ConcurrentHashMap` provides thread safety, not retention policy.

**Production impact:** A traffic spike or long-tail template set can fill the heap and trigger GC
pressure or `OutOfMemoryError`.

**Correct implementation:** `BoundedLruCache` caps retained entries and evicts the least recently
used item when capacity is exceeded.

**Why the solution works:** Retained cache state has a hard maximum.

**Trade-offs:** A small cache can evict useful entries; a production service would usually use
Caffeine for expiry, weight limits, metrics and better concurrency.

**How to detect it:** Heap dumps, cache-size metrics and load tests with high-cardinality inputs.

**Interview follow-up:** Why does `ConcurrentHashMap` not make a cache safe by itself?

### Caller-controlled cache key cardinality

**Type:** Scalability issue · **Severity:** High · **Difficulty:** Intermediate

**Problem:** The key includes the full model map, which can vary per request.

**Why it happens:** The cache key is derived from input convenience rather than a bounded domain.

**Production impact:** Attackers or ordinary long-tail traffic can force near-zero hit rate while
retaining large values.

**Correct implementation:** Cache only stable template metadata or use a bounded cache with a clear
key contract.

**Why the solution works:** The retained key space is tied to a known domain and capped.

**Trade-offs:** Fully rendered per-model output may be too specific to cache safely.

**How to detect it:** Key-cardinality metrics and hit-rate drops during traffic spikes.

**Interview follow-up:** What makes a good cache key in a multi-tenant service?

### Missing cache metrics

**Type:** Observability issue · **Severity:** Medium · **Difficulty:** Intermediate

**Problem:** Operators cannot see size, hit rate, miss rate or evictions.

**Why it happens:** The cache is an implementation detail hidden behind a static map.

**Production impact:** Memory growth appears as generic heap pressure with no obvious owner.

**Correct implementation:** Expose cache size and policy metrics; Caffeine can export standard
cache stats in production systems.

**Why the solution works:** Metrics connect cache behavior to memory and latency symptoms.

**Trade-offs:** Metrics add cardinality risk if tagged by raw tenant or template values.

**How to detect it:** Dashboards that correlate cache size, hit rate, evictions and heap usage.

**Interview follow-up:** Which cache metrics are useful, and which tags would you avoid?

### Hidden static cache policy

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

**Problem:** Capacity and lifetime cannot be configured, replaced or tested at the component
boundary.

**Why it happens:** The cache is a static implementation detail.

**Production impact:** Different environments cannot tune cache behavior without code changes.

**Correct implementation:** Inject a bounded cache dependency or construct it with explicit
capacity.

**Why the solution works:** Policy becomes part of component configuration and tests.

**Trade-offs:** More explicit wiring, but fewer global-state surprises.

**How to detect it:** Tests that need to clear global state or fail when run in different order.

**Interview follow-up:** Why is static mutable state hard to operate in long-running services?

### Hot-path info logging

**Type:** Observability issue · **Severity:** Medium · **Difficulty:** Basic

**Problem:** Every cache miss logs the full key at info level.

**Why it happens:** Logs are used as a substitute for cache metrics.

**Production impact:** High request volume creates expensive, noisy logs and may expose tenant data.

**Correct implementation:** Emit aggregate metrics and reserve logs for unusual failures or sampled
diagnostics.

**Why the solution works:** Operators see behavior without paying per-request log cost.

**Trade-offs:** Metrics are less detailed than individual logs, so targeted debugging may still need
sampling.

**How to detect it:** Log volume spikes, ingestion cost alerts and repeated template-render messages.

**Interview follow-up:** When would debug logging be acceptable on a cache path?

## Correct implementation

Package: `lab.jvm.boundedcache`

- `src/main/java/lab/jvm/boundedcache/BoundedLruCache.java`
- `docs/topics/jvm/solutions.md#bounded-lru-cache`
