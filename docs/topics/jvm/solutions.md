# JVM Solutions

Production-grade implementations for the review exercises, explaining why each fix resolves the underlying failure and what trade-offs it introduces.

## Issue-to-fix matrix

| Exercise | Broken Example | Issue Categories | Correct Implementation | Key Fix |
|---|---|---|---|---|
| **Listener Lifecycle** | `static-listener-leak` | Memory, Resource leak, Design, Maintainability | `lab.jvm.listenerregistry.ListenerRegistry` | Instance scope + idempotent `Registration` handle + immutable snapshot |
| **Request Context** | `threadlocal-pool-leak` | Resource leak, Data consistency, Security, Design | `lab.jvm.requestcontext.RequestContext` | Lexical `ContextScope` restoring parent context on close |
| **Template Cache** | `unbounded-cache` | Memory, Scalability, Observability, Design | `lab.jvm.boundedcache.BoundedLruCache` | Access-order LRU eviction capping maximum entries |
| **Metric Encoding** | `excessive-hot-path-allocation` | Performance, Observability, Maintainability, Design | `lab.jvm.allocation.MetricLineEncoder` | Precompiled regex + single-pass `StringBuilder` + zero hot allocations |

---

## Lifecycle-aware listener registration

### Correct implementation

```java
--8<-- "modules/02-jvm/src/main/java/lab/jvm/listenerregistry/ListenerRegistry.java"
```

```java
--8<-- "modules/02-jvm/src/main/java/lab/jvm/listenerregistry/Registration.java"
```

### Why this fixes the issues

- **Instance ownership:** Replaces static storage with an instance-owned collection, tying listener
  lifetime to the lifecycle of the owning component.
- **Idempotent handle:** `register` returns an `AutoCloseable` `Registration` handle backed by an
  `AtomicBoolean`, ensuring safe removal and preventing double-deregistration races.
- **Defensive copying:** `snapshot()` returns an unmodifiable copy (`List.copyOf`), preventing callers
  from mutating internal registry state.

### Trade-offs

Requires callers to manage the `Registration` handle (e.g. in Spring bean `@PreDestroy` or `AutoCloseable`
lifecycle), introducing slightly more boilerplate than a fire-and-forget static registration.

---

## Scoped ThreadLocal context

### Correct implementation

```java
--8<-- "modules/02-jvm/src/main/java/lab/jvm/requestcontext/RequestContext.java"
```

```java
--8<-- "modules/02-jvm/src/main/java/lab/jvm/requestcontext/RequestIdentity.java"
```

### Why this fixes the issues

- **Lexical lifetime:** `open` returns a `ContextScope` (`AutoCloseable`) meant to be used inside
  `try-with-resources`. Even if an unhandled runtime exception is thrown, `close()` is guaranteed to execute.
- **Parent restoration:** Captures the previous context value when opening a new scope and restores it
  upon close, supporting clean nested task execution.
- **Thread cleanup:** When closing the root scope, `ThreadLocal.remove()` is invoked, removing the
  reference from the pooled thread's `ThreadLocalMap`.

### Trade-offs

Requires passing `RequestContext` as a dependency or standardizing request interceptors. In Java 21+,
Scoped Values (`java.lang.ScopedValue`) offer a first-class language alternative for structured concurrency.

---

## Bounded LRU cache

### Correct implementation

```java
--8<-- "modules/02-jvm/src/main/java/lab/jvm/boundedcache/BoundedLruCache.java"
```

### Why this fixes the issues

- **Hard memory bound:** Uses access-order `LinkedHashMap` overriding `removeEldestEntry` to evict the
  least-recently-used entry whenever `size > maximumSize`.
- **Thread safety:** Synchronizes cache operations to coordinate structural evictions safely.
- **Immutable snapshot:** Exposes an unmodifiable view for inspection without leaking internal references.

### Trade-offs

A synchronized JDK LRU cache uses coarse-grained locking on every get/put. For high-concurrency production
workloads, Caffeine (`com.github.ben-manes.caffeine`) provides near-optimal W-TinyLFU eviction, lock-striping,
and built-in Micrometer metrics.

---

## Allocation-conscious metric encoding

### Correct implementation

```java
--8<-- "modules/02-jvm/src/main/java/lab/jvm/allocation/MetricEvent.java"
```

```java
--8<-- "modules/02-jvm/src/main/java/lab/jvm/allocation/MetricLineEncoder.java"
```

### Why this fixes the issues

- **Precompiled Pattern:** Validates metric name once during `MetricEvent` record construction using a
  static precompiled regex.
- **Zero intermediate collections:** Replaces streams and `String.format` with a pre-sized `StringBuilder`
  and direct array iteration.
- **Single-pass escaping:** Escapes special characters (`\`, `,`, `=`) inline without allocating intermediate
  string copies.

### Trade-offs

Manual character iteration and buffer management is more verbose than a stream pipeline. Only apply on
measured hot paths (telemetry, serialization, packet parsing) where profilers prove allocation pressure.

## Related

- [Code review](code-review.md)
- [Concepts](concepts.md)
- [Production](production.md)
