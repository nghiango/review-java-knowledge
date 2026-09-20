# Concurrency Issues

Atomicity, visibility, ordering, shared mutation, deadlock and starvation failures.

## Entries

### Shared mutable accumulator in a parallel stream

**Type:** Concurrency issue · **Severity:** High · **Difficulty:** Intermediate

Parallel workers cannot safely append to one ArrayList. Use a pure mapping/collector whose reduction
owns its state, or submit independent values to an explicit executor and join immutable results.

**Appears in:** [Core Java — stream side effects](../topics/core-java/code-review.md#stream-and-parallel-side-effects)

### Non-atomic counter increment on shared mutable state

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Basic

Compound read-modify-write (`count++`) operations executed by multiple threads interleave and silently drop updates. Replace with `AtomicLong` or `LongAdder`.

**Appears in:** [Concurrency — lost update on shared counter](../topics/concurrency/code-review.md#lost-update-on-shared-counter)

### Deadlock from inconsistent lock acquisition order

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

Acquiring multiple locks in arbitrary caller-dependent order causes circular wait deadlock under concurrent reverse operations. Enforce strict canonical resource ordering (such as comparable IDs).

**Appears in:** [Concurrency — lock ordering deadlock](../topics/concurrency/code-review.md#lock-ordering-deadlock)

### Volatile used for compound check-then-act

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

`volatile` guarantees visibility and memory ordering but provides zero atomicity for check-then-act logic, causing race conditions and overselling. Replace with atomic CAS retry loops (`AtomicInteger.updateAndGet`).

**Appears in:** [Concurrency — volatile compound action](../topics/concurrency/code-review.md#volatile-compound-action)

### Unbounded thread pools and thread creation

**Type:** Resource management issue · **Severity:** Critical · **Difficulty:** Intermediate

Unbounded thread creation (`newCachedThreadPool()` or ad-hoc `new Thread()`) exhausts OS memory under load, causing JVM native thread OOM. Use a bounded `ThreadPoolExecutor` with a bounded queue and backpressure policy.

**Appears in:** [Concurrency — unbounded thread creation](../topics/concurrency/code-review.md#unbounded-thread-creation)

### Serialized `.join()` inside loops and common-pool starvation

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

Invoking `.join()` on `CompletableFuture` inside each loop iteration eliminates parallelism and serializes execution. Running blocking I/O on `ForkJoinPool.commonPool()` starves CPU tasks. Use `CompletableFuture.allOf()` and dedicated I/O thread pools.

**Appears in:** [Concurrency — completable future unhandled join](../topics/concurrency/code-review.md#completablefuture-unhandled-join)

## Related

- [Issue catalogue](index.md)
