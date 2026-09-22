# Solution: Unsafe Off-Heap Buffer

## Annotated code

```java
package lab.java25boot4.whatsnew.broken.unsafebuffer;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

public class UnsafeOffHeapBuffer {

    // Deployment issue: sun.misc.Unsafe memory access is deprecated for removal (JEP 498).
    // On Java 25 every call below warns; a future release will remove it entirely.
    // Maintainability issue: Unsafe is reachable only through the private `theUnsafe` field
    // via reflection, defeating module encapsulation and breaking when internals move.
    private static final Unsafe UNSAFE = loadUnsafe();

    private final long address;
    private final int capacity;

    public UnsafeOffHeapBuffer(int capacity) {
        this.capacity = capacity;
        // Resource leak issue: raw native memory is allocated with no owner. If the constructor
        // succeeds but the caller never calls close(), or an exception escapes between here and
        // close(), the memory is leaked for the lifetime of the process.
        this.address = UNSAFE.allocateMemory((long) capacity * Long.BYTES);
    }

    // Memory issue: index is not bounds-checked, so set(capacity, v) or set(-1, v) writes
    // outside the allocation and corrupts unrelated native memory.
    public void set(int index, long value) {
        UNSAFE.putLong(address + (long) index * Long.BYTES, value);
    }

    // Memory issue: get() has the same missing bounds check, so a read can return data from
    // outside the buffer instead of failing.
    public long get(int index) {
        return UNSAFE.getLong(address + (long) index * Long.BYTES);
    }

    public int capacity() {
        return capacity;
    }

    // Resource leak issue: close() is not idempotent and is not tied to a try-with-resources
    // scope — the class does not implement AutoCloseable, so the compiler cannot enforce release.
    public void close() {
        UNSAFE.freeMemory(address);
    }

    private static Unsafe loadUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("sun.misc.Unsafe is not available", e);
        }
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Deployment issue | High | `UnsafeOffHeapBuffer` field | `sun.misc.Unsafe` memory access is deprecated for removal (JEP 498) |
| 2 | Resource leak issue | Critical | `UnsafeOffHeapBuffer.close()` | Native memory has no `try`/`finally` owner; leaked on any abnormal path |
| 3 | Memory issue | Critical | `set()` / `get()` | No bounds check — writes and reads outside the allocation corrupt memory |
| 4 | Maintainability issue | Medium | `loadUnsafe()` | JDK-internal field reached by reflection; fragile and encapsulation-breaking |

## Issue details

## Unsafe memory access deprecated for removal

**Type:** Deployment issue · **Severity:** High · **Difficulty:** Intermediate
**Track:** `java25-boot4` · **Technology:** `sun.misc.Unsafe`, Foreign Function & Memory API
**Interview frequency:** High · **Production impact:** Medium

**Location:** `UnsafeOffHeapBuffer` (`UNSAFE` field, all memory calls)

### Problem
The buffer is built entirely on `sun.misc.Unsafe` memory-access methods (`allocateMemory`,
`putLong`, `getLong`, `freeMemory`). Since Java 23 (JEP 498) these methods are deprecated for
removal: they emit warnings now and are scheduled to disappear.

### Why it happens
`Unsafe` predates any supported low-level memory API. Teams reached for it because it was the only
way to get off-heap access without JNI. Java 22 finalised the Foreign Function & Memory API
(`java.lang.foreign`), which is the supported replacement.

### Production impact
```text
Java 25 : warning per call site            → noise now
Java N+ : method removed                   → application fails to start
```
A dependency on a JDK-internal API also breaks the moment a new JDK changes the reflective access
path, which is exactly the situation a migration is supposed to avoid.

### Broken implementation
```java
this.address = UNSAFE.allocateMemory((long) capacity * Long.BYTES);
UNSAFE.putLong(address + (long) index * Long.BYTES, value);
```

### Correct implementation
```java
try (Arena arena = Arena.ofConfined()) {
    MemorySegment segment = arena.allocate((long) capacity * Long.BYTES);
    segment.set(ValueLayout.JAVA_LONG, (long) index * Long.BYTES, value);
}
```

### Why the solution works
`java.lang.foreign` is a supported, stable API. `Arena` owns the native memory, so closing the arena
frees everything it allocated, and `MemorySegment` performs bounds checking on every access.

### Trade-offs
The FFM API is more verbose than a raw pointer arithmetic call, and `Arena`/`MemorySegment` are new
concepts for a team that has only used `Unsafe`. In exchange, the memory has a defined lifetime and
every access is checked. For pure-Java data, a heap `ByteBuffer` is still simpler — off-heap is only
worth it when GC pressure is genuinely the bottleneck.

### How to detect it
```bash
./gradlew :modules:40-whats-new:compileJava --warning-mode all | grep -i unsafe
```
JEP 498 warnings name every `Unsafe` memory call site. In a running JVM,
`-XX:+UnlockDiagnosticVMOptions -XX:MaxUnsafeMemoryAccessWarnings=...` controls the warning budget.

### Interview follow-up
> Why is `sun.misc.Unsafe` memory access being removed rather than just deprecated, and what is the
> supported replacement for each of its use cases?

### Related
- Foreign Function & Memory API · JEP 498 · JEP 454 · Off-heap memory

## Native memory leak with no try/finally owner

**Type:** Resource leak issue · **Severity:** Critical · **Difficulty:** Intermediate
**Track:** `java25-boot4` · **Technology:** `Unsafe`, `AutoCloseable`
**Interview frequency:** High · **Production impact:** Critical

**Location:** `UnsafeOffHeapBuffer` constructor / `close()`

### Problem
`allocateMemory` reserves native memory that the garbage collector never reclaims. The class is not
`AutoCloseable`, and no code path guarantees `freeMemory` runs. Any exception between allocation and
`close()` — or a caller who simply forgets — leaks the allocation permanently.

### Why it happens
Native memory is not managed by the GC, so there is no finaliser safety net by design. Ownership
must be expressed structurally, and here it is expressed only in a comment.

### Production impact
```text
buffer allocated per request → close() skipped on the error path
→ native memory grows unbounded → RSS climbs
→ container OOM-killed while the Java heap looks healthy
```

### Broken implementation
```java
this.address = UNSAFE.allocateMemory((long) capacity * Long.BYTES);
// ... no try/finally, class does not implement AutoCloseable
```

### Correct implementation
```java
public final class ArenaMemoryBuffer implements AutoCloseable {

    private final Arena arena;
    private final MemorySegment segment;

    public ArenaMemoryBuffer(int capacity) {
        this.arena = Arena.ofConfined();
        this.segment = arena.allocate((long) capacity * Long.BYTES);
    }

    @Override
    public void close() {
        arena.close();
    }
}
```

### Why the solution works
`Arena` is `AutoCloseable` and owns every segment it allocates, so a single `close()` releases the
whole allocation. Because the buffer implements `AutoCloseable`, callers can use
try-with-resources and the compiler enforces release on every exit path.

### Trade-offs
An `Arena` per buffer adds an object and a confinement rule (a confined arena may only be used by
its creating thread). `Arena.ofShared()` lifts the thread restriction at a small synchronisation
cost. The lifetime is now explicit, which is the point.

### How to detect it
```bash
jcmd <pid> VM.native_memory summary      # native memory tracking
```
Watch RSS/container memory grow while heap usage is flat and GC pauses are unchanged.

### Interview follow-up
> Native memory is invisible to the GC. How would you prove a leak is native rather than heap?

### Related
- Foreign Function & Memory API · Arena · Native memory leak

## Missing bounds check corrupts native memory

**Type:** Memory issue · **Severity:** Critical · **Difficulty:** Intermediate
**Track:** `java25-boot4` · **Technology:** `Unsafe`, pointer arithmetic
**Interview frequency:** High · **Production impact:** Critical

**Location:** `set(int, long)` / `get(int)`

### Problem
Both accessors compute `address + index * Long.BYTES` and pass it straight to `Unsafe`. Nothing
checks that `0 <= index < capacity`. `set(capacity, v)` writes past the end of the allocation; a
negative index writes before it.

### Why it happens
`Unsafe` is deliberately unchecked — that is what makes it fast. The safety that a normal Java array
provides (`ArrayIndexOutOfBoundsException`) is exactly what is given up.

### Production impact
A write outside the allocation silently corrupts unrelated native memory. The symptom appears far
from the cause: a crash in another component, a corrupted TLS buffer, or a JVM fatal error with no
Java stack trace.

### Broken implementation
```java
UNSAFE.putLong(address + (long) index * Long.BYTES, value);
```

### Correct implementation
```java
segment.set(ValueLayout.JAVA_LONG, (long) index * Long.BYTES, value); // bounds-checked
```

### Why the solution works
`MemorySegment` validates the offset against the segment's size and throws
`IndexOutOfBoundsException`, converting a memory-corruption bug into a normal Java exception with a
stack trace.

### Trade-offs
The bounds check costs a comparison on each access. For a hot loop this is measurable; it is still
far cheaper than the alternative — a silent memory corruption that no test can reproduce reliably.

### How to detect it
There is no reliable runtime detector for an out-of-bounds `Unsafe` write. The only detection is at
review time; after the fact, look for crashes without Java stack traces.

### Interview follow-up
> Why can't the JVM reliably detect an out-of-bounds `Unsafe` write, while it can detect an
> out-of-bounds array access?

### Related
- Memory safety · Pointer arithmetic · `IndexOutOfBoundsException`

## Unsafe obtained through reflective internal access

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic
**Track:** `java25-boot4` · **Technology:** `sun.misc.Unsafe`, reflection, JPMS
**Interview frequency:** Medium · **Production impact:** Medium

**Location:** `loadUnsafe()`

### Problem
`Unsafe` cannot be instantiated normally, so the code reflects on the private static field
`theUnsafe` and calls `setAccessible(true)`. This reaches into JDK internals and is exactly the
access pattern the module system is designed to prevent.

### Why it happens
It is the well-known workaround for obtaining `Unsafe`, copied from older code. It works today only
because `sun.misc.Unsafe` is exported to the unnamed module for compatibility.

### Production impact
A future JDK that tightens access to `theUnsafe` turns this into an immediate
`InaccessibleObjectException` at class initialisation — the application fails to start.

### Broken implementation
```java
Field field = Unsafe.class.getDeclaredField("theUnsafe");
field.setAccessible(true);
return (Unsafe) field.get(null);
```

### Correct implementation
No reflection is needed: the FFM API is public and does not require privileged access.
```java
Arena arena = Arena.ofConfined();
MemorySegment segment = arena.allocate(bytes);
```

### Why the solution works
Using a public API removes the dependency on JDK internals entirely, so a JDK upgrade cannot break
class initialisation.

### Trade-offs
None of substance — this is a strict improvement. The only cost is learning the FFM API.

### How to detect it
```bash
java --illegal-access=deny ...     # legacy; on modern JDKs, expect warnings/errors on reflective access
grep -rn "setAccessible(true)" src/main
```

### Interview follow-up
> What is the difference between a public API, a supported internal API, and a private field reached
> by reflection, in terms of upgrade risk?

### Related
- JPMS · Reflection · Foreign Function & Memory API

## Correct implementation

Package `lab.java25boot4.whatsnew.unsafebuffer`, source
`src/main/java/lab/java25boot4/whatsnew/unsafebuffer/ArenaMemoryBuffer.java`, tests in
`ArenaMemoryBufferTest`.

Walkthrough and trade-offs: [What's New — Solutions](../../../../../docs/tracks/java25-boot4/whats-new/solutions.md#unsafe-off-heap-buffer).
