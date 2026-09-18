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

## Related

- [Issue catalogue](index.md)
