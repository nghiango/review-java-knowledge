# Core Java Delta — Code Review

!!! info "Delta from baseline"
    Unchanged: the baseline review method — read the diff, name the dimension, propose the fix →
    [baseline Core Java code review](../../../topics/core-java/code-review.md)
    Changed: the pinning workaround is now the risk, not the fix
    New: primitive-pattern narrowing and pattern-order hazards

Two review targets. Each is real code from `broken-examples/`; read the code before revealing the
issues.

## Obsolete pinning refactor

### Context

A pull request migrates `PaymentSequenceGenerator` to Java 25 as part of a virtual-thread rollout.
The author explains:

> "In Java 21, `synchronized` pinned the carrier thread. I refactored our synchronized methods to
> `ReentrantLock` to prevent pinning under high virtual-thread concurrency."

### Review target

```java
--8<-- "tracks/java25-boot4/modules/01-core-java/broken-examples/obsolete-synchronized-pinning-refactor/PaymentSequenceGenerator.java"
```

### Review prompt

1. Is the premise true on Java 25?
2. What does the manual lock add compared with the code it replaced?
3. What happens on the exception path?
4. Would `synchronized` or `AtomicLong` be the better production design here?

??? warning "Reveal issues"

    ### Obsolete carrier-pinning workaround

    **Type:** Concurrency issue · **Severity:** Medium · **Difficulty:** Intermediate
    **Track:** `java25-boot4` · **Technology:** Virtual threads, ObjectMonitor
    **Interview frequency:** High · **Production impact:** Medium

    **Problem:** The `ReentrantLock` exists only to avoid carrier pinning, a limitation that no
    longer exists.

    **Why it happens:** On Java 21 a virtual thread blocking inside `synchronized` pinned its
    carrier. Java 24 reworked `ObjectMonitor` so ownership follows the virtual thread, so blocking
    on a monitor now unmounts normally.

    **Production impact:** The workaround adds a heap-allocated lock, verbose code and a new failure
    mode, while buying nothing.

    **Fix:** revert to `synchronized`, or use `AtomicLong` for a monotonic counter.

    ### Lock leak on the exception path

    **Type:** Reliability issue · **Severity:** Critical · **Difficulty:** Intermediate
    **Track:** `java25-boot4` · **Technology:** `ReentrantLock`
    **Interview frequency:** High · **Production impact:** Critical

    **Problem:** `lock.unlock()` is not in a `finally` block, so the designed
    `IllegalStateException("Sequence exhausted")` path leaves the lock held forever.

    **Why it happens:** `synchronized` is released by the compiler on every exit path; a manual
    `lock()`/`unlock()` pair has no such guarantee.

    **Production impact:** every subsequent caller parks on `lock()` and never proceeds — a
    permanently stuck subsystem with low CPU and flat error rate.

    **Fix:** remove the lock (see above). If a `ReentrantLock` is genuinely required, always use
    `try { lock.lock(); ... } finally { lock.unlock(); }`.

    ### How to detect it

    Thread dump shows many callers parked on one monitor while CPU stays low; latency climbs with no
    corresponding load increase.

Correct implementation: [Solutions — obsolete pinning refactor](solutions.md#obsolete-pinning-refactor).

## Primitive pattern matching loss

### Context

A pull request adds `MetricConverter`, using Java 25 primitive patterns to format metric values and
convert integer samples to a compact representation.

### Review target

```java
--8<-- "tracks/java25-boot4/modules/01-core-java/broken-examples/primitive-pattern-matching-loss/MetricConverter.java"
```

### Review prompt

1. What does `value instanceof byte b` match, and in what order are the branches evaluated?
2. What happens to an integer larger than `Byte.MAX_VALUE`?
3. Is the cascade `if`/`else` the right shape for this problem?
4. How would you make the conversion fail loudly instead of corrupting data?

??? warning "Reveal issues"

    ### Unchecked narrowing truncates silently

    **Type:** Reliability issue · **Severity:** Critical · **Difficulty:** Intermediate
    **Track:** `java25-boot4` · **Technology:** Primitive patterns, narrowing conversion
    **Interview frequency:** High · **Production impact:** Critical

    **Problem:** `(byte) i` wraps without a bounds check, so `castToSmallInt(300)` returns `44` with
    no warning.

    **Why it happens:** a primitive pattern proves the value's **type**, never its **range**.
    Narrowing conversion is defined to keep the low-order bits.

    **Production impact:** corrupt metrics are stored and reported as if valid; nothing in logs or
    monitoring flags the corruption.

    **Fix:** check the range before returning and fail loudly otherwise:

    ```java
    if (value instanceof Integer i) {
        if (i < Byte.MIN_VALUE || i > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Integer exceeds byte capacity: " + i);
        }
        return i;
    }
    throw new IllegalArgumentException("Not an Integer: " + value);
    ```

    A `when` guard (`case Integer i when i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE -> i`) expresses
    the same bound inside the pattern; the production class uses the explicit check because
    `google-java-format 1.28.0` cannot parse guards yet.

    ### Pattern-order and boxing ambiguity

    **Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate
    **Track:** `java25-boot4` · **Technology:** Primitive patterns
    **Interview frequency:** Medium · **Production impact:** Medium

    **Problem:** the `instanceof byte` branch is evaluated first on an `Object` target. Whether a
    boxed `Integer` reaches that branch depends on the exact-conversion rules, so the cascade's
    behaviour is hard to reason about.

    **Why it happens:** primitive patterns unbox wrapper types, and the first matching branch wins.

    **Fix:** use a `switch` with explicit wrapper cases (`Byte`, `Integer`, `Long`, `Double`) and
    guarded clauses, so the intended mapping is visible.

Correct implementation: [Solutions — primitive pattern matching loss](solutions.md#primitive-pattern-matching-loss).

## Related

- [Concepts](concepts.md)
- [Solutions](solutions.md)
- [Interview questions](questions.md)
- [Baseline Core Java code review](../../../topics/core-java/code-review.md)
