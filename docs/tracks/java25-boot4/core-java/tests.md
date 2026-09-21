# Core Java Delta — Tests

!!! info "Delta from baseline"
    Unchanged: JUnit 5 + AssertJ, no Docker for unit tests →
    [baseline Core Java tests](../../../topics/core-java/tests.md)
    Changed: the track runs on a JDK 25 toolchain with `--enable-preview`
    New: gatherer, constructor-body, unnamed-variable, unpinning and guarded-pattern coverage

## How to run

```bash
./gradlew buildTrack-java25-boot4          # from the repository root: build + unit tests
cd tracks/java25-boot4 && ../../gradlew test
```

This module needs **no Docker**: it is pure language behaviour, so everything runs in `src/test`.
The track keeps the baseline split — `src/test` is fast and container-free, `src/integrationTest`
is for Testcontainers-backed tests — but module 01 has nothing to put in `integrationTest`.

!!! warning "Toolchain"
    Tests compile and run with `--enable-preview` because primitive patterns are a preview API on
    Java 25. Gradle resolves the JDK 25 toolchain through `foojay-resolver-convention`.

## What the tests prove

| Test class | Proves |
|---|---|
| `StreamGatherersTest` | `windowFixed` keeps the final partial batch; `windowSliding` overlaps; `scan` accumulates |
| `FlexibleConstructorValidationTest` | arguments are validated and normalised **before** `super(...)`; invalid input fails fast |
| `UnnamedVariablesPatternsTest` | unnamed pattern components, loop variables, `catch` parameters and lambda parameters all work as intended |
| `VirtualThreadSynchronizedSafetyTest` | 50 virtual threads complete synchronized work without carrier starvation |
| `SafeMetricConverterTest` | wrapper types map explicitly; out-of-range values throw instead of truncating |

### Unpinning under load

```java
--8<-- "tracks/java25-boot4/modules/01-core-java/src/test/java/lab/java25boot4/corejava/VirtualThreadSynchronizedSafetyTest.java"
```

The test submits 50 tasks to a virtual-thread-per-task executor, each sleeping **inside** a
`synchronized` block, and asserts that all 50 complete. On Java 21 this shape risks carrier
starvation; on Java 25 it is safe. It waits on a `CountDownLatch` — no `Thread.sleep` polling.

### Guarded narrowing

```java
--8<-- "tracks/java25-boot4/modules/01-core-java/src/test/java/lab/java25boot4/corejava/SafeMetricConverterTest.java"
```

The assertions are deliberately boundary-heavy: `Byte.MIN_VALUE` and `Byte.MAX_VALUE` must pass,
`300` must throw. That is the difference between the correct implementation and the
[broken one](code-review.md#primitive-pattern-matching-loss).

## What is not covered

- **Pinning behaviour itself** is a JVM property, not an assertion. `Q06SynchronizedVirtualThreadUnpinningExample`
  demonstrates it by constraining the carrier pool; treat it as an experiment, not a test.
- **`--enable-preview` semantics** — the build enables it, so a future JDK may change the API
  without a test failing to compile until the toolchain moves.

## Related

- [Code review](code-review.md)
- [Solutions](solutions.md)
- [Exercises](exercises.md)
- [Baseline Core Java tests](../../../topics/core-java/tests.md)
