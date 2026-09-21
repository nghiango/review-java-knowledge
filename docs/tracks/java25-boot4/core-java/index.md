# Core Java — Java 25 Delta

!!! info "Delta from baseline"
    Unchanged: OOP and SOLID, `equals`/`hashCode`, collections, generics, exceptions, streams
    basics, `java.time` → [baseline Core Java](../../../topics/core-java/index.md)
    Changed: `synchronized` no longer pins carrier threads; GC defaults; deprecations that now warn
    New: stream gatherers, flexible constructor bodies, unnamed variables, primitive patterns,
    scoped values

## Why this matters

Java 25 changes the answer to a question the baseline teaches: *is it safe to block inside
`synchronized` on a virtual thread?* On Java 21 the answer was no, and teams rewrote their locking
code around it. On Java 25 that workaround is obsolete — and keeping it introduces the very
deadlock it was meant to avoid.

## What is in the delta

| Feature | Java | Status | Baseline equivalent |
|---|---|---|---|
| Stream gatherers (`Stream::gather`) | 24 | Final | hand-written loops, `collect` with custom `Collector` |
| Flexible constructor bodies | 25 | Final | static factory methods for validation before `super(...)` |
| Unnamed variables and patterns | 22 | Final | named-but-unused parameters, `ignored` |
| Primitive types in patterns | 25 | Preview | `instanceof Integer` + manual unboxing |
| Unpinned `synchronized` virtual threads | 24 | Final | `ReentrantLock` workaround |
| Scoped values | 25 | Final | `ThreadLocal` |

## Broken examples

1. [Obsolete pinning refactor](code-review.md#obsolete-pinning-refactor) — a `ReentrantLock`
   rewrite that outlived its reason and leaks the lock on the exception path.
2. [Primitive pattern matching loss](code-review.md#primitive-pattern-matching-loss) — silent
   narrowing and pattern-order hazards in a metrics formatter.

## Correct implementations

Each broken example maps to tested source in [Solutions](solutions.md) and to runnable
demonstrations under `src/main/java/lab/java25boot4/corejava/`.

## Production checklist

- No locking code exists purely to avoid carrier pinning.
- Every `ReentrantLock` releases in `finally` — or has been replaced by `synchronized`.
- Primitive patterns are guarded when a narrowing conversion follows.
- Preview features are used deliberately, never accidentally.
- Request-scoped context uses scoped values rather than mutable `ThreadLocal` state.

## Navigate

- [Concepts](concepts.md) — the language and runtime delta
- [Internals](internals.md) — how gatherers, constructor bodies and unpinning work
- [Interview questions](questions.md) — 4 basic · 4 intermediate · 4 senior · 1 scenario
- [Code review](code-review.md) — the two broken examples
- [Solutions](solutions.md) — correct implementations and trade-offs
- [Tests](tests.md) — what the tests prove
- [Production](production.md) — symptoms, diagnostics, checklist
- [Exercises](exercises.md) — hands-on tasks

## Related

- [Java 25 / Boot 4 track](../index.md)
- [What's new in Java 22 → 25](../whats-new-java.md)
- [Migration guide](../migration.md)
- [Baseline Core Java](../../../topics/core-java/index.md)
