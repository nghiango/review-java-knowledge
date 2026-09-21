# Core Java Delta — Exercises

!!! info "Delta from baseline"
    Unchanged: the exercise method — predict the outcome, then check →
    [baseline Core Java exercises](../../../topics/core-java/exercises.md)
    Changed: one exercise is about *removing* a workaround, not adding a feature
    New: gatherers, guarded patterns, scoped values

Try each task before revealing the solution. Every exercise maps to code in this module.

## 1. Re-implement batching with a gatherer

Rewrite a loop that batches a list into chunks of five so that it uses a gatherer, and make sure the
final partial batch is still emitted.

??? note "Reveal solution"
    ```java
    List<List<Integer>> batches = numbers.stream().gather(Gatherers.windowFixed(5)).toList();
    ```

    The gatherer's **finisher** emits the trailing partial window — the step hand-written loops
    usually omit. Compare with [Stream gatherers](concepts.md#stream-gatherers).

## 2. Predict the output

```java
Object value = 300;
System.out.println(value instanceof int i ? (byte) i : -1);
```

??? note "Reveal solution"
    Prints **`44`**. The pattern matches (an `Integer` converts exactly to `int`), then the
    narrowing cast truncates silently. See
    [broken example 2](code-review.md#unchecked-narrowing-truncates-silently).

## 3. Fix the truncation

Make the conversion in exercise 2 fail loudly for out-of-range values instead of truncating.

??? note "Reveal solution"
    ```java
    static int toSmallInt(Object value) {
        return switch (value) {
            case Integer i when i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE -> i;
            case Integer i -> throw new IllegalArgumentException("Integer exceeds byte capacity: " + i);
            default -> throw new IllegalArgumentException("Not an Integer: " + value);
        };
    }
    ```

    The guard moves the range check into the pattern. See
    [SafeMetricConverter](solutions.md#primitive-pattern-matching-loss).

    Note: `google-java-format 1.28.0` cannot parse `when` guards, so the formatted production class
    uses an equivalent explicit range check.

## 4. Find the leak

This method is called under load and one call throws:

```java
lock.lock();
if (sequence >= LIMIT) {
    throw new IllegalStateException("exhausted");
}
long next = ++sequence;
lock.unlock();
```

What happens to every later caller, and what are the two possible fixes?

??? note "Reveal solution"
    The lock is **never released** on the exception path, so every later caller blocks forever.
    Fixes: (1) move `unlock()` into a `finally` block, or (2) delete the lock and use `synchronized`,
    which the compiler releases on every exit path. On Java 25 the second fix is preferred because
    the pinning reason for the lock is gone. See
    [broken example 1](code-review.md#obsolete-pinning-refactor).

## 5. Replace a mutable `ThreadLocal` context

A request-scoped `ThreadLocal<String> tenantId` is set on entry and removed on exit, but occasionally
leaks between requests on a reused carrier. Rewrite it with a scoped value.

??? note "Reveal solution"
    ```java
    static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();

    ScopedValue.where(TENANT_ID, tenant).run(() -> handleRequest());
    ```

    The binding ends when the scope exits, so there is nothing to forget. See
    [Scoped values](concepts.md#scoped-values).

## 6. Decide whether to keep a lock

A teammate says: "This `ReentrantLock` was added for virtual-thread pinning — keep it, it can't
hurt." Give the two questions you ask before agreeing.

??? note "Reveal solution"
    1. **Does it need `tryLock`, fairness or multiple `Condition`s?** If not, `synchronized` is
       strictly safer.
    2. **Is every release in a `finally`?** If not, the lock is a latent outage.

    If both answers are "no" and "no", revert it. See
    [migration pitfalls](../migration.md#pitfalls).

## Related

- [Concepts](concepts.md)
- [Code review](code-review.md)
- [Solutions](solutions.md)
- [Baseline Core Java exercises](../../../topics/core-java/exercises.md)
