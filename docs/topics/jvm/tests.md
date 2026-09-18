# JVM Tests & Diagnostic Verification

Testing JVM behavior requires distinguishing **deterministic contract verification** from **non-deterministic runtime diagnostics**.

## Deterministic contract tests

Unit tests in `modules/02-jvm/src/test/java` execute fast without external Docker infrastructure or sleep calls:

| Test Class | Focus | Key Invariants Verified |
|---|---|---|
| [`ListenerRegistryTest`](file:///modules/02-jvm/src/test/java/lab/jvm/listenerregistry/ListenerRegistryTest.java) | Listener lifecycle | Instance isolation, immutable snapshots, idempotent `Registration.close()`, null rejection |
| [`RequestContextTest`](file:///modules/02-jvm/src/test/java/lab/jvm/requestcontext/RequestContextTest.java) | ThreadLocal scoping | Lexical scope cleanup, nested context restoration, exception recovery, single-thread executor isolation |
| [`BoundedLruCacheTest`](file:///modules/02-jvm/src/test/java/lab/jvm/boundedcache/BoundedLruCacheTest.java) | Cache bounds | Maximum capacity enforcement, LRU eviction order, access order refresh, immutable snapshots |
| [`MetricLineEncoderTest`](file:///modules/02-jvm/src/test/java/lab/jvm/allocation/MetricLineEncoderTest.java) | Allocation encoding | Deterministic key ordering, character escaping, defensive copy immutability, boundary validation |

Run all module unit tests:

```bash
./gradlew :modules:02-jvm:test
```

## Non-deterministic diagnostic demos

Runtime memory dynamics (GC pauses, JIT compilation thresholds, Metaspace exhaustion) cannot be reliably
asserted in unit tests due to machine variation and JVM heuristics. Instead, these behaviors are observed
via opt-in diagnostic programs in `src/examples/java/lab/jvm/examples/`:

### 1. Compile examples without packaging

```bash
./gradlew :modules:02-jvm:compileExamples
```

### 2. Inspect bytecode disassembly

```bash
javap -c -v modules/02-jvm/build/classes/java/examples/lab/jvm/examples/BytecodeDemo.class
```

### 3. Profile allocation with Java Flight Recorder (JFR)

```bash
java -XX:StartFlightRecording=duration=10s,filename=alloc.jfr,settings=profile \
     -cp modules/02-jvm/build/classes/java/examples \
     lab.jvm.examples.AllocationDemo 1000000
```

### 4. Opt-in dangerous demos

Memory and stack pressure demos require an explicit `--i-understand` argument to prevent accidental CI or IDE crashes:

```bash
# Stack overflow observation with constrained stack size
java -Xss256k -cp modules/02-jvm/build/classes/java/examples lab.jvm.examples.StackDepthDemo --i-understand

# Heap pressure observation with heap dump generation
java -Xmx32m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/oom.hprof \
     -cp modules/02-jvm/build/classes/java/examples \
     lab.jvm.examples.HeapPressureDemo --i-understand
```

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Solutions](solutions.md)
