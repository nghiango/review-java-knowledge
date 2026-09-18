# Core Java in Production

## Cache entries exist but cannot be found

**Symptoms:** misses rise after profile updates; iteration still shows entries; memory grows.

**Investigate:** capture key fields/hash before insertion and lookup; reproduce mutation; inspect
equality changes between releases.

**Root cause:** mutable or incompatible equality/hash state. Use immutable identity and explicit
remove/reinsert migration. Verify lookup/removal before and after refresh.

## The process stops opening files

**Symptoms:** imports fail after hours; heap and CPU are normal; open descriptors climb.

**Investigate:** `lsof -p <pid>`, OS descriptor metrics, stack traces and closure tests on failure.

**Root cause:** owned readers are not deterministically closed. Use try-with-resources and monitor
the resource, not only JVM memory. Raising limits delays failure but does not fix ownership.

## Reports lose rows while CPU is low

**Symptoms:** missing/reordered output, common-pool workers parked in HTTP clients, unrelated
CompletableFuture latency rises.

**Investigate:** thread dump, ForkJoinPool queue/active counts, output cardinality/order, downstream
latency and concurrent-call count.

**Root cause:** shared mutation plus blocking common-pool work. Use pure transformations and an
explicit bounded executor aligned to downstream capacity.

## Checklist

- Stable immutable map/set keys
- Equality and ordering compatibility tested
- Resource ownership documented and failure paths tested
- Absence distinct from outage
- No external mutation in stream pipelines
- Blocking concurrency bounded, named and monitored
- Exceptions retain safe operation identity and cause

## Related

- [Performance issues](../../issues/performance.md)
- [Data consistency issues](../../issues/data-consistency.md)
- [Scenarios](questions.md#scenarios)
