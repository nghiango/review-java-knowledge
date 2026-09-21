# Core Java Delta — Solutions

!!! info "Delta from baseline"
    Unchanged: review method, issue taxonomy, trade-off analysis →
    [baseline Core Java solutions](../../../topics/core-java/solutions.md)
    Changed: the pinning workaround is now the defect being removed
    New: guarded primitive patterns

Correct implementations for the two [review targets](code-review.md), with the reason each issue is
fixed and what the fix costs.

## Obsolete pinning refactor

**Fixes:** [obsolete carrier-pinning workaround](code-review.md#obsolete-carrier-pinning-workaround)
and the [lock leak](code-review.md#lock-leak-on-the-exception-path).

```java
--8<-- "tracks/java25-boot4/modules/01-core-java/src/main/java/lab/java25boot4/corejava/VirtualThreadSynchronizedSafety.java"
```

### Why it works

- **No lock object.** `synchronized` needs no manual release, so the exception path cannot leak.
- **No pinning.** On Java 24+ a virtual thread blocked inside `synchronized` unmounts, so the
  carrier is free — the original reason for the workaround is gone.
- **Fewer moving parts.** Nothing to allocate, nothing to forget to release.

### Alternative

For a plain monotonic counter, drop locking entirely:

```java
private final AtomicLong sequence = new AtomicLong(1000L);

public long nextSequence() {
    return sequence.incrementAndGet();
}
```

### Trade-offs

| Dimension | `synchronized` on Java 25 | Hand-written `ReentrantLock` |
|---|---|---|
| Carrier pinning | No | No |
| Release on exception | Guaranteed by the compiler | Manual — easy to miss |
| `tryLock` / fairness / multiple conditions | Not available | Available |
| Allocation | None | One lock object |
| Readability | Highest | Boilerplate `try`/`finally` |

Keep `ReentrantLock` only where you need `tryLock`, fairness or several `Condition`s — never as a
reflex against pinning.

## Primitive pattern matching loss

**Fixes:** [unchecked narrowing](code-review.md#unchecked-narrowing-truncates-silently) and the
[pattern-order ambiguity](code-review.md#pattern-order-and-boxing-ambiguity).

```java
--8<-- "tracks/java25-boot4/modules/01-core-java/src/main/java/lab/java25boot4/corejava/SafeMetricConverter.java"
```

### Why it works

- **The range check is explicit.** A matched `Integer` is checked against
  `Byte.MIN_VALUE`/`Byte.MAX_VALUE` before it is returned, so no unchecked narrowing remains.
- **The out-of-range case fails loudly.** A thrown exception surfaces the corrupt value instead of
  storing `44` and calling it a metric.
- **Explicit wrapper cases.** A `switch` over `Byte`/`Integer`/`Long`/`Double` documents the
  intended mapping instead of relying on branch order.

### Tooling note

A `when` guard expresses the same bound inside the pattern and is the more idiomatic form:

```java
case Integer i when i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE -> i;
```

`google-java-format 1.28.0` cannot parse `when` guards, so the formatted production class uses the
equivalent explicit check, and the guard form is demonstrated in
`Q11GuardedPrimitivePatternExample` (excluded from formatting, like the other preview-syntax
examples). This is a real upgrade hazard: a formatter that lags the language forces a style choice.

### Trade-offs

| Choice | Gain | Cost |
|---|---|---|
| Guarded pattern | No silent truncation; bound is explicit | More verbose than a bare cast |
| Throw on out-of-range | Corruption is visible immediately | Caller must handle the failure |
| Keep a `default -> -1` sentinel | Never throws | `-1` is indistinguishable from a real value — avoid |

## Related

- [Code review](code-review.md)
- [Concepts](concepts.md)
- [Tests](tests.md)
- [Baseline Core Java solutions](../../../topics/core-java/solutions.md)
