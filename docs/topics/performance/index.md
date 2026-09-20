# Performance

Performance engineering connects user-visible latency and throughput to finite resources: CPU,
memory, threads, database connections, locks, and downstream capacity. This module uses measurements
and bounded experiments rather than folklore.

## Study path

1. [Concepts](concepts.md): metrics, saturation, capacity, and experimental method.
2. [Internals](internals.md): queueing, pools, GC, allocation, and contention.
3. [Code review](code-review.md): five production-shaped failure modes.
4. [Solutions](solutions.md) and [tests](tests.md): explicit resource policies and invariants.
5. [Production](production.md): JFR, load testing, and incident workflow.
6. [Questions](questions.md) and [exercises](exercises.md): interview practice.

## Progress

`Theory ✅` · `Questions ✅` · `Broken examples ✅` · `Correct code ✅` · `Tests ✅`

## Related

- [JVM](../jvm/index.md)
- [Observability](../observability/index.md)
- [Performance issue catalogue](../../issues/performance.md)
