# Concurrency

## Why this matters

Concurrent execution is at the core of high-throughput backend services. Subtle bugs—such as lost updates, data races, deadlocks, and thread pool starvation—rarely appear in local single-threaded tests but manifest catastrophically under production load. Senior engineers must understand the Java Memory Model, lock-free coordination, executor boundaries, and Java 21 Virtual Threads to build resilient, high-performance systems.

## Core Concepts

- [Java Memory Model, visibility, atomicity, synchronization primitives, and Virtual Threads](concepts.md)
- [Monitors, AQS, CAS hardware primitives, ThreadPoolExecutor internals, and carrier scheduling](internals.md)

## How it works internally

Follow how intrinsic monitor locks transition from biased to thin to fat locks, how `AbstractQueuedSynchronizer` manages wait queues, how CAS instructions drive lock-free structures, and how Java 21 unmounts virtual threads in [Internals](internals.md).

## Common Interview Questions

The [question bank](questions.md) spans 23 structured questions: 8 Basic, 8 Intermediate, 5 Senior, and 2 Production Scenarios with dedicated runnable code examples.

## Common Production Problems

Lost updates, cyclic lock deadlocks, `volatile` check-then-act overselling, thread pool self-deadlocks, carrier pinning, and common ForkJoinPool starvation are diagnosed and resolved in [Production](production.md).

## Broken Examples

1. [Lost update on shared counter](code-review.md#lost-update-on-shared-counter)
2. [Lock ordering deadlock](code-review.md#lock-ordering-deadlock)
3. [Volatile compound action](code-review.md#volatile-compound-action)
4. [Unbounded thread creation](code-review.md#unbounded-thread-creation)
5. [CompletableFuture unhandled join](code-review.md#completablefuture-unhandled-join)

## Correct Implementations

Each broken review exercise maps to production-grade implementations and stress tests in [Solutions](solutions.md) and [Tests](tests.md).

## Trade-offs

Fine-grained locking increases throughput but raises deadlock risk; lock-free CAS eliminates blocking but increases CPU consumption under heavy contention; bounded queues provide backpressure but require rejection handling; virtual threads drastically scale I/O concurrency but must avoid carrier pinning on `synchronized` blocks.

## Production Checklist

- All shared mutable state is protected by atomic variables, locks, or encapsulated within thread-safe collections.
- Thread pools have explicit core and maximum bounds, named threads, and defined rejection policies.
- Background tasks and executors implement graceful two-phase shutdown.
- Multiple lock acquisitions follow a strict, deterministic global ordering.
- Blocking I/O is never executed on `ForkJoinPool.commonPool()`.
- Virtual thread workloads use `ReentrantLock` instead of `synchronized` across blocking operations to prevent carrier pinning.

## Senior-Level Questions

Explore advanced topics like work-stealing scheduling, ABA prevention, JMM happens-before transitive closures, and carrier pinning diagnostics in [Senior Questions](questions.md#senior).

## Exercises

Reinforce concepts with hands-on concurrency katas and review drills in [Exercises](exercises.md).

## Related

- [JVM & Performance](../jvm/index.md)
- [Concurrency Issues](../../issues/concurrency.md)
- [Performance Issues](../../issues/performance.md)
- [Interview Checklist](../../interview-checklist.md)
