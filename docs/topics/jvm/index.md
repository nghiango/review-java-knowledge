# JVM & Performance

## Why this matters

Framework configurations and syntax sugar cannot prevent OutOfMemory errors, thread pool exhaustion,
or GC pauses in production. Senior engineers must understand how bytecode executes, how memory is laid out,
how collectors reclaim memory, and how to diagnose incidents using standard JDK diagnostics.

## Core Concepts

- [JVM Architecture, Runtime Areas, Object Allocation, JIT, Garbage Collection & Error Taxonomy](concepts.md)
- [Class Loading Lifecycle, Bytecode Execution, TLABs, Tiered JIT & Collector Mechanics](internals.md)

## How it works internally

Understand class loading through parent delegation and initialization triggers, follow JIT compilation
from the template interpreter through C1 and C2 with deoptimization traps, trace allocation fast paths
via TLABs and escape analysis, and inspect generational garbage collection with remembered sets in [Internals](internals.md).

## Common Interview Questions

The [question bank](questions.md) progresses across Basic, Intermediate, Senior, and Scenario levels
covering memory leaks, JFR diagnostic profiling, container memory budgets, and GC tuning.

## Common Production Problems

Static listener retention, unclosed ThreadLocal request contexts on pooled threads, unbounded caches with
caller-controlled cardinality, and high-frequency hot-path young generation churn are analyzed in [Production](production.md).

## Broken Examples

1. [Static listener leak](code-review.md#static-listener-leak) — Memory & Resource Leak
2. [ThreadLocal pool leak](code-review.md#threadlocal-pool-leak) — Data Consistency, Security & Resource Leak
3. [Unbounded template cache](code-review.md#unbounded-template-cache) — Memory, Scalability & Observability
4. [Excessive hot-path allocation](code-review.md#excessive-hot-path-allocation) — Performance, Observability & Maintainability

## Correct Implementations

Each review exercise maps to tested, production-grade implementations in [Solutions](solutions.md):
- `lab.jvm.listenerregistry.ListenerRegistry`
- `lab.jvm.requestcontext.RequestContext`
- `lab.jvm.boundedcache.BoundedLruCache`
- `lab.jvm.allocation.MetricLineEncoder`

## Trade-offs

Zero-allocation code introduces procedural complexity over declarative streams; bounded caches require
eviction and concurrency tuning (or Caffeine); explicit lexical context handles demand disciplined
resource management.

## Production Checklist

- Long-lived listeners return closeable handles and are deregistered at component shutdown.
- ThreadLocal state on pooled or virtual threads is cleared in a deterministic `finally` / `AutoCloseable` block.
- In-memory caches enforce maximum size, weight, TTL/TTI expiration, and export eviction metrics.
- High-throughput formatting and telemetry hot paths avoid regex recompilation and intermediate wrapper allocations.
- Container memory budgets account for Metaspace, Code Cache, Thread Stacks, and Direct Buffers in addition to `-Xmx`.

## Senior-Level Questions

Can you distinguish why a process climbs in memory when business data is flat, or why a container is
OOMKilled when `-Xmx` is half the container limit? Use the [senior questions](questions.md#senior).

## Exercises

Explore real diagnostic scenarios including heap dump histogram analysis and bytecode execution in [Exercises](exercises.md).

## Further Experiments

Run the safe and opt-in diagnostic demos in `modules/02-jvm/src/examples/java/lab/jvm/examples/` with JFR,
`jcmd`, and `-Xlog:gc*` to observe JVM behavior firsthand.

## Related

- [Roadmap](../../roadmap.md)
- [Performance issues](../../issues/performance.md)
- [Maintainability issues](../../issues/maintainability.md)
- [Observability issues](../../issues/observability.md)
