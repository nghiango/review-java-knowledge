# JVM Code Review

Review each clean source before expanding its answer.

## Static listener leak

An event registry retains listeners for the lifetime of the application.

```java
--8<-- "modules/02-jvm/broken-examples/static-listener-leak/GlobalEventRegistry.java"
```

Consider listener lifetimes, static references, duplicate registration, encapsulation and unregistration handles.

??? warning "Reveal issues"
    **Memory issue — static retention:** Storing listeners in a static list keeps strong references to
    listener instances and whatever outer service or class loader they capture for the process lifetime.
    Prefer an instance-scoped registry.

    **Resource leak issue — missing registration handle:** Registering does not return an `AutoCloseable`
    handle, making unregistration impossible or dependent on error-prone manual list management.

    **Design issue — duplicate registrations:** The registry permits registering identical listeners
    multiple times, multiplying event invocations and retained references.

    **Maintainability issue — exposed mutable backing collection:** Returning the raw mutable list
    allows external callers to tamper with internal state.

[Correct design](solutions.md#lifecycle-aware-listener-registration)

## ThreadLocal pool leak

A request handler manages ambient tenant identity on pooled worker threads.

```java
--8<-- "modules/02-jvm/broken-examples/threadlocal-pool-leak/RequestContext.java"
```

```java
--8<-- "modules/02-jvm/broken-examples/threadlocal-pool-leak/RequestHandler.java"
```

Consider thread pooling, exception paths, nested execution and lexical lifecycle.

??? warning "Reveal issues"
    **Resource leak issue — ThreadLocal value retention:** Worker threads are reused across requests.
    Setting ThreadLocal state without clearing it in a `finally` block leaves stale identity on the thread.

    **Data consistency & Security issue — cross-request identity contamination:** When an uncaught exception
    occurs, uncleaned ThreadLocal state causes subsequent unrelated requests to execute under the prior user's identity.

    **Design issue — unmanaged ambient context:** The context lacks a lexical scope abstraction (`AutoCloseable`),
    relying on fragile manual removal calls.

    **Design issue — broken nesting:** Re-opening a context overwrites previous parent context without
    restoring it upon completion.

[Correct design](solutions.md#scoped-threadlocal-context)

## Unbounded template cache

A service caches rendered templates to avoid repeated rendering.

```java
--8<-- "modules/02-jvm/broken-examples/unbounded-cache/TemplateCache.java"
```

Consider key cardinality, maximum capacity, eviction policy, cache observability and logging.

??? warning "Reveal issues"
    **Memory issue — unbounded static cache:** A static `ConcurrentHashMap` with no maximum size or
    eviction retains templates permanently until the JVM runs out of heap.

    **Scalability issue — caller-controlled key cardinality:** Deriving cache keys from unconstrained
    request parameters allows arbitrary key explosion and low hit rates.

    **Observability issue — missing cache metrics:** No metrics exist for cache size, hit rate, miss rate,
    or evictions, obscuring memory growth until OOM occurs.

    **Design issue — static hidden policy:** Cache capacity and lifetime are hardcoded in static state
    rather than configured at the component boundary.

    **Observability issue — hot-path info logging:** Logging every cache render at INFO level produces
    excessive log volume on high-throughput paths.

[Correct design](solutions.md#bounded-lru-cache)

## Excessive hot-path allocation

A telemetry line encoder formats high-frequency metric events.

```java
--8<-- "modules/02-jvm/broken-examples/excessive-hot-path-allocation/MetricLineEncoder.java"
```

Consider regex compilation, short-lived object churn, streams vs direct buffers, and logging on the hot path.

??? warning "Reveal issues"
    **Performance issue — regex compiled on hot path:** Compiling a `Pattern` inside the method body on
    every invocation repeatedly parses regex syntax and allocates pattern node graphs.

    **Performance issue — excessive young-generation allocations:** Chaining stream pipelines, entrySet
    iterators, lambdas, and `String.format` allocates dozens of short-lived objects per event, causing high TLAB churn.

    **Observability issue — hot-path info logging:** Logging every telemetry encoding event at INFO level
    allocates log records and saturates log appenders on critical paths.

    **Maintainability issue — chained regex string replacements:** Calling `replaceAll` repeatedly
    creates multiple intermediate string copies per tag.

    **Design issue — untyped loose domain arguments:** Passing loose strings and mutable maps bypasses
    domain validation and thread safety.

[Correct design](solutions.md#allocation-conscious-metric-encoding)

## Related

- [Concepts](concepts.md)
- [Solutions](solutions.md)
- [Production](production.md)
