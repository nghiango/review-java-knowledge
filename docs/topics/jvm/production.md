# JVM Production Incidents & Diagnostics

Real-world production failure modes, diagnostic runbooks, and JVM operational triage.

## 1. Heap memory leaks and dominator trees

### Symptoms
- JVM heap usage steadily climbs over days/weeks with a "sawtooth" pattern whose troughs continually rise.
- GC pause frequency and duration escalate until the process throws `OutOfMemoryError: Java heap space`.

### Diagnostic runbook
1. Capture an on-demand heap dump:
   ```bash
   jcmd <pid> GC.heap_dump /tmp/live_heap.hprof
   ```
2. Open the `.hprof` file in Eclipse Memory Analyzer (MAT) or IntelliJ Memory Profiler.
3. Compute the **Dominator Tree**:
   - The **Shallow Size** is the memory consumed by the object itself (e.g. 24 bytes for a HashMap).
   - The **Retained Size** is the total heap memory kept alive by the object's reference tree.
4. Identify top dominators and inspect the shortest path to **GC Roots** (e.g. static maps, uncleaned registries, cached buffers).

---

## 2. Metaspace and ClassLoader leaks

### Symptoms
- `OutOfMemoryError: Metaspace` occurs after application redeployments or continuous dynamic proxy generation.
- Heap occupancy is low, but off-heap Metaspace usage steadily increases to `-XX:MaxMetaspaceSize`.

### Diagnostic runbook
1. Inspect live classloader statistics:
   ```bash
   jcmd <pid> VM.classloader_stats
   ```
2. Check for multiple instances of custom classloaders that should have been unloaded.
3. Enable class unloading logs:
   ```bash
   -Xlog:class+load=info,class+unload=info
   ```
4. Verify whether a static field or framework thread is retaining references to classes loaded by child classloaders.

---

## 3. ThreadLocal cross-request state contamination

### Symptoms
- Request logs intermittently display tenant IDs, user permissions, or authorization tokens belonging to a completely different user.
- Occurs primarily under high load following an unhandled runtime exception or timeout.

### Diagnostic runbook
1. Identify all `ThreadLocal` usages in the codebase.
2. Verify whether `ThreadLocal.set()` is accompanied by an `AutoCloseable` scope or `try ... finally { ThreadLocal.remove(); }`.
3. Check for asynchronous context propagation across thread pool boundaries (e.g., `CompletableFuture.supplyAsync`, Spring `@Async`).

---

## 4. High young-gen allocation rate and GC churn

### Symptoms
- Minor GC runs multiple times per second, consuming 15%+ of total process CPU.
- Heap usage immediately drops back to baseline after each collection (live data is flat).
- Application p99/p999 latency spikes due to frequent allocation stalls.

### Diagnostic runbook
1. Profile allocation events using JFR:
   ```bash
   jcmd <pid> JFR.start name=alloc duration=60s filename=/tmp/alloc.jfr settings=profile
   ```
2. Open in JDK Mission Control (JMC) and navigate to **Memory $\to$ Allocations in New TLAB**.
3. Identify top allocating classes and stack traces (e.g., `String.format`, regex compilation, Stream pipelines, byte array copies in telemetry hot paths).
4. Refactor critical call sites to use precompiled patterns and single-pass buffers.

---

## 5. Container OOMKill (Exit code 137)

### Symptoms
- Pod or container terminates abruptly without an `OutOfMemoryError` in logs and without generating a heap dump (`-XX:+HeapDumpOnOutOfMemoryError` did not trigger).
- `kubectl describe pod` displays `Last State: Terminated, Reason: OOMKilled, Exit Code: 137`.

### Diagnostic runbook
1. Verify container memory limits vs JVM heap:
   - Calculate total native memory:
     $$\text{RSS} = \text{Heap} + \text{Metaspace} + \text{CodeCache} + (\text{PlatformThreads} \times 1\text{MB}) + \text{DirectMemory} + \text{NativeAllocations}$$
2. Enable Native Memory Tracking (NMT):
   ```bash
   -XX:NativeMemoryTracking=summary
   ```
   Inspect baseline and growth with:
   ```bash
   jcmd <pid> VM.native_memory summary
   ```
3. Set `-XX:MaxRAMPercentage=65%` and adjust container memory limits to allow sufficient headroom for OS and native JVM components.

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Code review](code-review.md)
