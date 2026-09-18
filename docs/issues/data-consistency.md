# Data Consistency Issues

Lost updates, stale state, partial writes and identity/invariant failures.

## Entries

### Mutable key after HashMap insertion

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Core Java, HashMap · **Interview frequency:** High · **Production impact:** High

A HashMap records the insertion-time bucket; it cannot observe later mutation of equality/hash
state. The entry consumes memory but normal lookup and removal can miss it. Use an immutable
identity key and explicitly remove/reinsert when identity genuinely changes.

**Detection:** Compare lookup and hash values before/after mutation; inspect entries in a debugger.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

**Trade-off:** Immutable identity separates profile data and requires explicit identity migration.

**Interview follow-up:** Why does ConcurrentHashMap not repair a mutated key?

### Import mutates caller state before validation completes

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

Appending directly to caller-owned state makes a later parse failure leave a partial import. Build
an explicit result locally, then let the application choose atomic, chunked or compensating commit.

**Appears in:** [Core Java — resource and collection mutation](../topics/core-java/code-review.md#resource-and-collection-mutation)

### Parallel report loses deterministic order and rows

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

Parallel `forEach` does not order side effects, and an unsafe accumulator can lose values. Produce
one immutable result per input and join in the documented order.

**Appears in:** [Core Java — stream side effects](../topics/core-java/code-review.md#stream-and-parallel-side-effects)

### Request context not cleared after failure

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

When request context cleanup is not tied to `finally` or try-with-resources, exceptions leave stale
identity on the worker thread. Use a closeable scope so cleanup runs on success and failure.

**Appears in:** `modules/02-jvm/broken-examples/threadlocal-pool-leak`

## Related

- [Issue catalogue](index.md)
