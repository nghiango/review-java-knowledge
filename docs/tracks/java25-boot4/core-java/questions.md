# Core Java Delta — Interview Questions

!!! info "Delta from baseline"
    Unchanged: Java 21 core questions → [baseline Core Java questions](../../../topics/core-java/questions.md)
    Changed: virtual-thread locking guidance
    New: gatherers, flexible constructor bodies, unnamed variables, primitive patterns, scoped values

Answers are collapsed. Reason from prose first, then open the example.

<!-- --8<-- [start:basic] -->
## Basic

### 1. What does `Stream::gather` add that `map` and `filter` cannot?

??? question "Reveal answer"

    **Short Answer:** `gather` adds a **stateful intermediate operation**. `map` is one-in-one-out
    and `filter` is one-in-zero-or-one-out; a gatherer can buffer elements and emit zero, one or
    many results — windowing, folding and scanning without collecting the stream first.

    See [Stream gatherers](concepts.md#stream-gatherers).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q01StreamGatherersWindowFixedExample.java"
        ```

### 2. What do flexible constructor bodies allow?

??? question "Reveal answer"

    **Short Answer:** Statements **before** `super(...)`. A constructor can validate and normalise
    its arguments and throw before the superclass constructor runs, so an invalid object is never
    partially constructed.

    See [Flexible constructor bodies](concepts.md#flexible-constructor-bodies).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q02FlexibleConstructorBodiesExample.java"
        ```

### 3. What are unnamed variables for?

??? question "Reveal answer"

    **Short Answer:** `_` declares a value you must name but do not use — loop variables, `catch`
    parameters, lambda parameters and unused pattern components. It states intent instead of
    inventing an `ignored` name.

    See [Unnamed variables and patterns](concepts.md#unnamed-variables-and-patterns).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q03UnnamedVariablesAndPatternsExample.java"
        ```

### 4. Can you pattern-match a primitive type in `instanceof` and `switch`?

??? question "Reveal answer"

    **Short Answer:** Yes — but it is a **preview** feature on Java 25, so it needs
    `--enable-preview`. A primitive pattern matches by **exact conversion** and removes the
    `instanceof Integer` plus cast boilerplate.

    See [Primitive types in patterns](concepts.md#primitive-types-in-patterns).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q04PrimitiveTypesInPatternsExample.java"
        ```

<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 5. `windowFixed`, `windowSliding`, `scan` and `fold` — when do you use each?

??? question "Reveal answer"

    **Short Answer:** `windowFixed(n)` produces non-overlapping batches (batch writes);
    `windowSliding(n)` produces overlapping windows (moving averages); `scan` emits a running
    accumulation after every element; `fold` emits a single final value.

    **Internal Mechanism:** all four are gatherers. Each keeps private state in the gatherer's
    initializer, pushes results from its integrator, and `windowFixed`/`fold` use the **finisher** to
    emit trailing state when the stream ends — which is why the last partial batch is not lost.

    **Common Mistake:** re-implementing batching with a hand-written loop and dropping the final
    partial window, or using `windowSliding` where non-overlapping batches were intended.

    See [Stream gatherers](concepts.md#stream-gatherers).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q05StreamGatherersSlidingAndScanExample.java"
        ```

### 6. Does `synchronized` still pin a carrier thread on Java 25?

??? question "Reveal answer"

    **Short Answer:** **No.** Since Java 24 a virtual thread that blocks inside `synchronized`
    unmounts normally, so the carrier is released. The Java 21 workaround — rewriting every monitor
    into `ReentrantLock` — is obsolete.

    **Internal Mechanism:** `ObjectMonitor` ownership was reworked so it follows the *virtual*
    thread, not the carrier. Blocking on a monitor no longer prevents unmounting.

    **Common Mistake:** keeping the workaround "to be safe". A `ReentrantLock` without a `finally`
    release leaks the lock on the exception path — a permanent outage where pinning was a
    performance risk.

    See [How `synchronized` stops pinning](internals.md#how-synchronized-stops-pinning).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q06SynchronizedVirtualThreadUnpinningExample.java"
        ```

### 7. Do primitive patterns match `null`?

??? question "Reveal answer"

    **Short Answer:** **No.** A primitive pattern requires an exact conversion, and `null` cannot be
    converted to a primitive. A `switch` over a possibly-null value needs an explicit `case null`.

    **Internal Mechanism:** the pattern is evaluated against the boxed value. With no value to
    unbox, there is nothing to convert, so the case does not match and evaluation falls through.

    **Common Mistake:** omitting `case null` and getting a `NullPointerException` from an
    exhaustively-looking `switch`.

    See [Exact conversions in primitive patterns](internals.md#exact-conversions-in-primitive-patterns).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q07PrimitivePatternNullSafetyExample.java"
        ```

### 8. What happens when a primitive pattern is followed by a narrowing cast?

??? question "Reveal answer"

    **Short Answer:** The value is **silently truncated**. The pattern proved the value is an `int`;
    it proved nothing about the range, so `(byte) 300` becomes `44` with no warning and no
    exception.

    **Internal Mechanism:** narrowing primitive conversion is defined to keep the low-order bits.
    Nothing in the pattern mechanism checks bounds.

    **Common Mistake:** assuming a successful pattern match validates the value, and letting a
    corrupt metric reach a database.

    See [Primitive types in patterns](concepts.md#primitive-types-in-patterns).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q08PrimitivePatternNarrowingExample.java"
        ```

<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 9. You are upgrading a Java 21 service to Java 25. How do you retire the pinning workarounds?

??? question "Reveal answer"

    **Short Answer:** Treat the removal as **migration work**, not cleanup. Identify every lock that
    exists only to avoid pinning, confirm it does not need `tryLock`/fairness/conditions, then
    replace it with `synchronized` or an atomic — and verify under virtual-thread load.

    **Deep Explanation:** The Java 21 rule was "do not block inside `synchronized` on a virtual
    thread". Teams encoded that rule in code as `ReentrantLock`. On Java 25 the rule is gone, but the
    encoded workaround remains — and it carries new risk: a lock acquired without `try`/`finally`
    leaks on the exception path, and every later caller blocks forever.

    **Internal Mechanism:** Java 24 reworked `ObjectMonitor` so ownership follows the virtual thread,
    allowing unmount while blocked. `synchronized` is released by the compiler on any exit path,
    which is exactly the guarantee a hand-written lock loses.

    **Common Mistake:** reverting all locks at once without checking the ones that genuinely need
    `tryLock`, fairness or multiple `Condition`s.

    **Production Consideration:** run the change under a virtual-thread load test with a constrained
    carrier pool; measure that a thread blocked in `synchronized` no longer delays unrelated tasks.

    **Follow-up Questions:** Which locks would you keep, and why? How would you detect a leaked lock
    in production?

    See [Migration guide](../migration.md).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q09LockWorkaroundMigrationExample.java"
        ```

### 10. Scoped values or `ThreadLocal` for request-scoped context on virtual threads?

??? question "Reveal answer"

    **Short Answer:** Prefer **scoped values**. They are immutable while bound, have a structural
    lifetime, and cannot leak into an unrelated request when a thread is reused.

    **Deep Explanation:** `ThreadLocal` is mutable, lives until someone calls `remove()`, and on
    pooled or reused carriers a missed `remove()` leaks one request's context into the next.
    `ScopedValue` binds a value for the dynamic extent of a `run`/`call` scope; when the scope exits,
    the binding is gone.

    **Internal Mechanism:** the binding is inherited by child tasks of structured concurrency without
    copying mutable state, and reads outside the scope throw rather than returning stale data.

    **Common Mistake:** reaching for scoped values as a general mutable cache. They cannot be
    written back to by callees.

    **Production Consideration:** keep correlation ids, tenant ids and auth principals in scoped
    values; keep mutable accumulators out.

    **Follow-up Questions:** What breaks if a callee needs to add to the context? How does this
    interact with structured concurrency?

    See [Scoped values](concepts.md#scoped-values).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q10ScopedValuesRequestContextExample.java"
        ```

### 11. How do you make primitive pattern matching safe?

??? question "Reveal answer"

    **Short Answer:** Put the range check **in the pattern** with a `when` guard, and let the
    unmatched case fail loudly instead of truncating.

    **Deep Explanation:** A pattern match proves type, not range. Guarding with
    `when i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE` makes the language enforce the bound, and a
    fall-through case can throw a domain exception rather than write corrupt data.

    **Internal Mechanism:** guarded patterns are evaluated in declaration order, so the narrowest
    guard must come first; a later unguarded case becomes the explicit "out of range" path.

    **Common Mistake:** guarding only the happy path and letting the remaining case return a
    sentinel like `-1`, which is indistinguishable from a real value.

    **Production Consideration:** prefer throwing at the boundary and mapping to a problem detail
    response, so corrupt values are visible rather than stored.

    **Follow-up Questions:** Why is a sentinel return value dangerous here? Where would you put the
    guard if the value comes from JSON?

    See [Primitive types in patterns](concepts.md#primitive-types-in-patterns).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q11GuardedPrimitivePatternExample.java"
        ```

### 12. Is a hand-written `ReentrantLock` safe if an exception can be thrown while it is held?

??? question "Reveal answer"

    **Short Answer:** Only if the release is in a `finally` block. Without it, the first exception
    leaks the lock and every subsequent caller blocks forever.

    **Deep Explanation:** `synchronized` is released by the compiler on every exit path, including
    exceptions. A manual `lock()`/`unlock()` pair has no such guarantee — the failure mode is not a
    slow request but a permanently stuck subsystem.

    **Internal Mechanism:** the lock remains owned by the thread that took it. Subsequent
    `lock()` calls wait indefinitely; only `tryLock` with a timeout can detect the condition.

    **Common Mistake:** assuming the exception path is rare. In the sequence-generator shape it is
    the *designed* terminal state, so the leak is guaranteed to happen.

    **Production Consideration:** alert on thread dumps showing callers parked on one lock, and
    prefer `synchronized` unless the extra `ReentrantLock` features are genuinely required.

    **Follow-up Questions:** How would you detect this from a thread dump? What does the migration
    guide say about reverting these locks?

    See [Code review: obsolete pinning refactor](code-review.md#obsolete-pinning-refactor).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q12ReentrantLockLeakOnExceptionExample.java"
        ```

<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenario

### 13. After a Java 25 upgrade, one subsystem stops responding under load. Threads are parked on a single lock. What happened?

??? question "Reveal answer"

    **Short Answer:** The upgrade left an obsolete pinning workaround in place. A `ReentrantLock`
    acquired without `finally` leaked on an exception path, and every later caller now blocks
    permanently on that lock.

    **Deep Explanation:** Start from the symptom, not the concept. Latency climbs while CPU stays
    **low** and error rate is flat — the classic signature of threads blocked on a monitor rather
    than a saturated CPU. A thread dump shows many callers parked in `lock()` on the *same* object,
    and exactly one thread that will never release it because it left via an exception.

    **Internal Mechanism:** `ReentrantLock` ownership is manual. If the code path throws between
    `lock()` and `unlock()` — here, the designed "sequence exhausted" terminal state — the lock is
    never released. Java 25 makes this strictly worse than the `synchronized` it replaced, because
    `synchronized` cannot leak.

    **Common Mistake:** assuming the pinning workaround is still needed, and "fixing" it by adding a
    timeout to the caller instead of removing the lock.

    **Production Consideration:** roll forward with `synchronized` (or an atomic counter), add an
    alert on threads parked on one lock, and re-run the virtual-thread load test. Revert any other
    lock that exists only to avoid pinning.

    **Follow-up Questions:** Which metrics separate a lock leak from CPU saturation? Why is
    `tryLock` with a timeout a diagnostic, not a fix?

    See [Production](production.md) and [Code review: obsolete pinning refactor](code-review.md#obsolete-pinning-refactor).

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/01-core-java/src/examples/java/lab/java25boot4/corejava/questions/Q13PinningWorkaroundDeadlockScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Code review](code-review.md)
- [Solutions](solutions.md)
- [Baseline Core Java questions](../../../topics/core-java/questions.md)
