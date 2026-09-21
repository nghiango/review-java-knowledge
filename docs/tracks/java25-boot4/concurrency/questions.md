# Interview Questions: Modern Concurrency in Java 25

!!! info "Delta from baseline"
    Baseline questions in [`docs/topics/concurrency/questions.md`](../../../topics/concurrency/questions.md) test Java 21 Concurrency: Threads, JMM, Locks, Atomics, and Virtual Thread basics.
    These 13 questions focus on **Java 25 Concurrency innovations**: Structured Concurrency, Scoped Values, ObjectMonitor unmounting, and migration strategies.

---

### 1. What is Structured Concurrency in Java 25 and what problem does it solve?

??? question "Reveal answer"
    Structured Concurrency (`StructuredTaskScope`) treats multiple concurrent tasks running on behalf of a single operation as a single unit of work. In unstructured concurrency (e.g. `CompletableFuture` or thread pools), child tasks have independent lifecycles: if one fails or is cancelled, sibling tasks continue running in the background as orphans, leaking threads, connections, and CPU.
    
    Structured Concurrency ensures that:
    1. Parent and child tasks form a strict lexical tree.
    2. Errors in one subtask propagate and immediately cancel sibling subtasks.
    3. The scope blocks at `.join()` until all subtasks have terminated, guaranteeing no orphan tasks escape.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q01StructuredConcurrencyBasicsExample.java"
        ```

---

### 2. How does `ScopedValue` differ from `ThreadLocal` in virtual thread architectures?

??? question "Reveal answer"
    `ThreadLocal` has three major liabilities when used with millions of virtual threads:
    1. **Memory overhead**: `InheritableThreadLocal` clones hash tables into every child thread. Spawning millions of virtual threads causes severe heap bloat.
    2. **Lifecycle risk**: `ThreadLocal` values persist until explicitly removed via `remove()`, frequently leaking context across requests.
    3. **Mutability**: Any code along the call chain can mutate the value.
    
    `ScopedValue` solves this by being:
    - **Immutable**: Values cannot be altered once bound.
    - **Stack-bounded**: Values automatically cease to exist when the execution block terminates, with zero manual cleanup.
    - **Lightweight**: Inherited across structured scopes via carrier snapshots without map copying.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q02ScopedValueBasicsExample.java"
        ```

---

### 3. How was the carrier thread pinning issue with `synchronized` resolved in Java 25?

??? question "Reveal answer"
    In Java 21, when a virtual thread blocked inside a `synchronized` block or method (e.g. on I/O, `Thread.sleep()`, or lock acquisition), it pinned its carrier platform thread, preventing the carrier from executing other virtual threads.
    
    In Java 25, HotSpot `ObjectMonitor` was re-engineered:
    - Virtual threads encountering blocking operations inside `synchronized` blocks yield their continuation back to the carrier's `ForkJoinPool`.
    - The carrier is immediately freed to execute other waiting virtual threads.
    - When the monitor is ready, the virtual thread is re-scheduled on any available carrier thread.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q03VirtualThreadPinningFixExample.java"
        ```

---

### 4. What is the behavior of `StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())`?

??? question "Reveal answer"
    `Joiner.awaitAllSuccessfulOrThrow()` coordinates a fan-out where every subtask must succeed.
    - If any subtask throws an exception, the joiner cancels all remaining running subtasks immediately.
    - The scope's `join()` method throws a `FailedException` encapsulating the root failure.
    - No sibling subtasks continue running after `join()` returns.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q04ShutdownOnFailureExample.java"
        ```

---

### 5. How does `Joiner.anySuccessfulResultOrThrow()` implement fastest-first racing?

??? question "Reveal answer"
    `Joiner.anySuccessfulResultOrThrow()` is used for redundant or speculative queries across multiple replicas:
    - As soon as the first subtask completes successfully, the joiner records its result and issues a cancellation signal to all other subtasks.
    - The scope's `join()` call returns the winning result directly.
    - If all subtasks fail, `join()` throws `FailedException` containing the suppressed failure causes.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q05ShutdownOnSuccessExample.java"
        ```

---

### 6. Can a `ScopedValue` be rebound in an inner execution scope?

??? question "Reveal answer"
    Yes. `ScopedValue` supports rebinding in nested scopes. When an inner block invokes `ScopedValue.where(KEY, newValue).run(...)`, the inner scope observes `newValue`. Once the inner block exits, the outer scope transparently observes the original value again, without mutation or side effects.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q06ScopedValueRebindingExample.java"
        ```

---

### 7. How do you implement a custom `StructuredTaskScope.Joiner` in Java 25?

??? question "Reveal answer"
    A custom joiner implements `StructuredTaskScope.Joiner<T, R>`:
    - Implement `onFork(Subtask)` to inspect scheduled tasks.
    - Implement `onComplete(Subtask)` to track results and return `true` if joining should stop and cancel remaining subtasks, or `false` to keep waiting.
    - Implement `result()` to return the aggregated output from `join()`.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q07CustomStructuredTaskScopeExample.java"
        ```

---

### 8. How do you detect and monitor carrier thread pinning in Java 25?

??? question "Reveal answer"
    Use JVM diagnostics and Flight Recorder:
    1. JVM Flag: `-Djdk.tracePinnedThreads=full` prints stack traces whenever pinning occurs.
    2. JFR Event: `jdk.VirtualThreadPinned` records pinning events and durations.
    3. Since `synchronized` blocks are unpinned in Java 25, remaining pinning events point exclusively to native calls (JNI) or class initialization.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q08VirtualThreadJfrPinningMetricsExample.java"
        ```

---

### 9. How do you migrate legacy `ThreadLocal` context holders to Java 25 `ScopedValue`?

??? question "Reveal answer"
    Migration steps:
    1. Replace `private static final ThreadLocal<T> CONTEXT = new ThreadLocal<>()` with `private static final ScopedValue<T> CONTEXT = ScopedValue.newInstance()`.
    2. Remove all `CONTEXT.set(...)` and `CONTEXT.remove()` calls inside manual `try-finally` blocks.
    3. Wrap entry points (e.g. HTTP filters, message listeners) with `ScopedValue.where(CONTEXT, value).run(...)` or `.call(...)`.
    4. Access context downstream via `CONTEXT.get()`.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q09MigrationThreadLocalToScopedValueExample.java"
        ```

---

### 10. How do you migrate `CompletableFuture.allOf()` pipelines to Structured Concurrency?

??? question "Reveal answer"
    Migration steps:
    1. Replace `CompletableFuture.supplyAsync()` with `scope.fork(...)` inside a `try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow()))`.
    2. Replace `CompletableFuture.allOf(...).join()` with `scope.join()`.
    3. Subtask values are safely retrieved via `subtask.get()`. If any subtask fails, `scope.join()` fails fast and cancels all siblings, eliminating orphan threads.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q10MigrationCompletableFutureToStructuredTaskScopeExample.java"
        ```

---

### 11. How do `ScopedValue` bindings propagate to subtasks within a `StructuredTaskScope`?

??? question "Reveal answer"
    `ScopedValue` bindings established on the parent thread are automatically inherited by any virtual thread forked within a nested `StructuredTaskScope`. The child tasks share the parent's carrier snapshot with $O(1)$ constant time overhead and zero memory allocation, unlike `InheritableThreadLocal` which performs expensive deep copies.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q11ScopedValueInheritanceAcrossThreadsExample.java"
        ```

---

### 12. What operations still pin virtual threads to carrier threads in Java 25?

??? question "Reveal answer"
    While `synchronized` monitor acquisition and wait/notify have been completely unpinned in Java 25, carrier pinning still occurs when:
    1. A virtual thread executes a **native method or JNI call** (e.g., native crypto, compression, or legacy C bindings).
    2. A virtual thread blocks inside a class initializer `<clinit>`.
    To prevent carrier pool starvation, blocking native calls must be delegated to dedicated, bounded platform thread pools.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q12NativeFramesCarrierPinningExample.java"
        ```

---

### 13. High-volume service experiences latency spikes after adopting virtual threads. How do you troubleshoot carrier starvation?

??? question "Reveal answer"
    Diagnosis and mitigation:
    1. **Check JFR Events**: Inspect `jdk.VirtualThreadPinned` events to verify if blocking native JNI calls are pinning carrier threads.
    2. **Examine Thread Dumps**: Verify if carrier threads (`ForkJoinPool-1-worker-*`) are locked in native state (`in Object.wait()` or native socket reads).
    3. **Isolate Native Calls**: Offload any third-party native libraries performing blocking I/O from virtual threads to a bounded platform thread executor (`Executors.newFixedThreadPool`).
    4. **Audit Thread Pools**: Ensure application code is not spawning fixed-size platform thread pools that block virtual thread handoffs.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/03-concurrency/src/examples/java/lab/java25boot4/concurrency/questions/Q13ScenarioDiagnosingCarrierThreadStarvationExample.java"
        ```
