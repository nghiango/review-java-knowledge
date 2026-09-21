# Concurrency in Java 25

!!! info "Delta from baseline"
    Baseline module [`modules/03-concurrency`](../../../topics/concurrency/index.md) covers Java 21 Concurrency: Virtual Threads, Carrier Threads, Executors, Locks, and Memory Visibility.
    This track module teaches the **Java 25 Concurrency evolution**:
    
    - **Structured Concurrency (`StructuredTaskScope`)** (JEP 480): treating multi-threaded subtasks as a single lexical unit of work with clean cancellation propagation and zero orphan threads.
    - **Scoped Values (`ScopedValue`)** (JEP 481): lightweight, immutable, stack-confined data sharing across virtual threads replacing memory-heavy `ThreadLocal`.
    - **ObjectMonitor Virtual Thread Unpinning**: `synchronized` methods and blocks unmounting virtual threads cleanly in Java 25, eliminating carrier thread starvation issues present in Java 21.

---

## 1. The Concurrency Paradigm Shift

| Feature | Java 21 Baseline | Java 25 Modern Standard |
|---|---|---|
| **Subtask Coordination** | `CompletableFuture.allOf()` / unstructured Executors | `StructuredTaskScope.ShutdownOnFailure` / `ShutdownOnSuccess` |
| **Orphan Task Risk** | High: failed sibling leaves other futures running | Zero: failure cancels all active siblings immediately |
| **Context Propagation** | `ThreadLocal` / `InheritableThreadLocal` (heavy heap footprint, manual cleanup) | `ScopedValue` (zero cleanup boilerplate, immutable, stack-confined) |
| **`synchronized` Pinning** | Pins carrier thread on blocking I/O | Fully unpinned: virtual thread unmounts cleanly from carrier |
| **Native Frame Pinning** | Pins carrier thread | Pins carrier thread (unchanged) |

---

## 2. Module Roadmap

1. [Concepts](concepts.md) — Structured Concurrency, Scoped Values, and Unpinning mechanisms.
2. [Internals](internals.md) — Loom Continuation scheduler, state machine of `StructuredTaskScope`, and ScopedValue cache tables.
3. [Interview Questions](questions.md) — 13 questions with runnable examples.
4. [Code Review](code-review.md) — Real-world code review scenarios with intentional anti-patterns.
5. [Solutions](solutions.md) — Step-by-step refactoring with production patterns.
6. [Testing Guide](tests.md) — Asserting cancellation semantics and virtual thread behavior.
7. [Production Scenarios](production.md) — Diagnosing carrier starvation and monitoring JFR events.
8. [Exercises](exercises.md) — Hands-on migration katas.
