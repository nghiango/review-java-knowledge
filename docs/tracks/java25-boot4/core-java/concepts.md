# Core Java Delta — Concepts

!!! info "Delta from baseline"
    Unchanged: object design, equality, hashing, ordering, immutability, strings, exceptions →
    [baseline Core Java concepts](../../../topics/core-java/concepts.md)
    Changed: what `synchronized` costs on a virtual thread
    New: stream gatherers, flexible constructor bodies, unnamed variables, primitive patterns,
    scoped values

## Stream gatherers

A gatherer is a **custom intermediate operation**. `map` and `filter` are one-in-one-out;
`flatMap` is one-in-many-out; `gather` can buffer, window, fold and scan — statefully, in the
middle of a pipeline.

```java
--8<-- "tracks/java25-boot4/modules/01-core-java/src/main/java/lab/java25boot4/corejava/StreamGatherersDemo.java"
```

The built-in factories cover most real needs:

| Factory | Shape | Typical use |
|---|---|---|
| `windowFixed(n)` | non-overlapping batches of `n` | batch writes to a database |
| `windowSliding(n)` | overlapping windows of `n` | moving averages, rate smoothing |
| `scan(seed, op)` | running accumulation after each element | cumulative totals |
| `fold(seed, op)` | single final value | reduce to one result |
| `mapConcurrent(n, fn)` | virtual-thread mapped, order preserved | parallel I/O inside one stream |

**Failure it prevents:** hand-written batching loops that re-implement `windowFixed` incorrectly at
the tail of the stream (dropping the final partial batch).

**Trade-off:** a gatherer is not always clearer than a loop. Use it when the operation is reused or
composed; keep the loop when it is genuinely simpler.

## Flexible constructor bodies

Before Java 25 a constructor had to call `super(...)` on the first statement. Validation had to move
to a static factory, or run after the parent constructor had already executed.

```java
--8<-- "tracks/java25-boot4/modules/01-core-java/src/main/java/lab/java25boot4/corejava/FlexibleConstructorValidation.java"
```

**Failure it prevents:** a half-initialised object. When validation runs *after* `super(...)`, the
parent has already observed — or worse, published — invalid state.

**Trade-off:** validation inside a constructor keeps invariants in one place, but it also makes the
constructor do work. Heavy computation still belongs in a factory or builder.

## Unnamed variables and patterns

`_` marks a value you are required to declare but do not use: loop variables, `catch` parameters,
lambda parameters and pattern components.

```java
--8<-- "tracks/java25-boot4/modules/01-core-java/src/main/java/lab/java25boot4/corejava/UnnamedVariablesPatterns.java"
```

**Failure it prevents:** `catch (NumberFormatException ignored)` and `for (Coordinate c : ...)` that
lie about intent, plus accidental use of a variable that was never meant to be read.

**Trade-off:** `_` is only legal where the value is genuinely unused; it is a statement of intent,
not a way to silence the compiler.

## Primitive types in patterns

A primitive pattern matches by **exact conversion**, so it avoids the old
`instanceof Integer` + cast dance. The hazard is what happens *after* the match.

```java
if (value instanceof int i) {
    return (byte) i; // 300 becomes 44 — silently
}
```

**Failure it prevents:** verbose unboxing boilerplate.

**Failure it introduces:** unchecked narrowing. A matched `int` is not automatically safe to narrow
to `byte`. Guard the pattern (`when i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE`) or use
`Math.toIntExact`.

**Trade-off:** primitive patterns are still **preview** on Java 25; they need `--enable-preview` and
may change. Pattern order also matters: put the narrowest pattern first.

## `synchronized` no longer pins virtual threads

On Java 21 a virtual thread that blocked inside `synchronized` pinned its carrier thread, so the
carrier could not run other virtual threads. The recommended workaround was `ReentrantLock`.

```java
--8<-- "tracks/java25-boot4/modules/01-core-java/src/main/java/lab/java25boot4/corejava/VirtualThreadSynchronizedSafety.java"
```

**Failure it prevents:** carrier starvation when virtual threads block inside monitors.

**Failure it introduces when you keep the workaround:** a hand-written `ReentrantLock` without
`try`/`finally` leaks the lock on the exception path, and every later caller blocks forever. This is
the exact shape of [broken example 1](code-review.md#obsolete-pinning-refactor).

**Trade-off:** `ReentrantLock` is still correct when you need `tryLock`, fairness or multiple
conditions. It is not correct as a reflex.

## Scoped values

`ScopedValue` carries immutable, request-scoped data down a call chain. Unlike `ThreadLocal` it is
bound for a bounded scope, cannot be mutated by callees, and is inherited by structured-concurrency
child tasks.

| | `ThreadLocal` | `ScopedValue` |
|---|---|---|
| Mutability | Mutable | Immutable while bound |
| Lifetime | Until `remove()` | The `run`/`call` scope |
| Leak risk | High with pooled threads | None — binding is structural |
| Inheritance | Copied per thread | Inherited by child tasks |

**Trade-off:** scoped values cannot replace a mutable cache or accumulator. If a callee must write
back, you still need another mechanism.

## Related

- [Internals](internals.md)
- [Interview questions](questions.md)
- [Baseline Core Java concepts](../../../topics/core-java/concepts.md)
