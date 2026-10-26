# JVM — Practice Q&A

Senior-depth practice from interview-prep §3 and §19. Numeric memory defaults depend on JVM and platform.
Use [Internals](internals.md) and [Solutions](solutions.md) for the lab's examples.

### Q1. What belongs in a JVM container memory budget?

??? question "Reveal answer"
    - **Budget heap plus native memory and operating-system headroom.**
    - **Mechanism:** metaspace, code cache, direct buffers, platform-thread stacks, JVM structures, and libraries consume memory outside `-Xmx`. Stack reservation is not the same as committed resident memory.
    - **Failure:** a container can be killed while the Java heap is healthy. A universal 75% heap fraction is unsafe for some workloads.
    - **Fix:** measure RSS/cgroup usage, heap occupancy, thread counts, and Native Memory Tracking when enabled; tune headroom against peak behavior.
    - **Trade-off:** reducing heap increases GC pressure; increasing limits without fixing unbounded native allocations postpones failure.
    - **Follow-up:** Which native allocations are not fully explained by NMT?
    - **Takeaway:** The heap is only one part of the process.

### Q2. How do you choose G1, ZGC, or Parallel GC?

??? question "Reveal answer"
    - **Choose using pause, throughput, CPU, and memory evidence under representative load.**
    - **Mechanism:** G1 uses regions and concurrent marking; Java 21 offers generational ZGC as an option; Parallel GC emphasizes throughput with stop-the-world collection.
    - **Failure:** a pause target is not an SLO guarantee; low pauses can coexist with allocation stalls, CPU exhaustion, or poor end-to-end latency.
    - **Fix:** capture GC logs and JFR, distinguish retained heap from allocation rate, then compare collectors with the same warmed workload and resource limits.
    - **Trade-off:** concurrent collectors need CPU and headroom. Reduce excessive allocation or retention before blindly increasing heap size.
    - **Follow-up:** Why might p99 degrade despite shorter individual GC pauses?
    - **Takeaway:** Tune service outcomes, not one collector metric.

### Q3. Memory grows and pods die: how do you distinguish a leak, heap pressure, and native exhaustion?

Merged from: §3 memory diagnosis + §19.5

??? question "Reveal answer"
    - **Establish which memory boundary failed before capturing invasive diagnostics.**
    - **Mechanism:** retained heap rising after comparable full collection suggests retention; high allocation alone may be healthy. `OutOfMemoryError` also has native-thread, metaspace, and direct-memory variants.
    - **Diagnosis:** inspect exception detail and container events; exit 137 alone proves SIGKILL, not necessarily OOM. Compare histograms, heap trends, NMT, and thread/direct-buffer growth.
    - **Fix:** follow dominator paths to GC roots; bound caches, remove listeners, clear scoped state, and stream large results.
    - **Safety:** histograms and heap dumps can pause and contain secrets; collect on a safe replica with access-controlled storage and sufficient disk.
    - **Follow-up:** Why can high RSS persist after objects are collected?
    - **Takeaway:** Prove retention and identify its owner before increasing limits.

### Q4. CPU is saturated: how do you separate hot code, GC, and throttling?

Merged from: §3 CPU diagnosis + §19.4

??? question "Reveal answer"
    - **Correlate CPU consumption, completed work, profiles, and resource limits.**
    - **Mechanism:** CPU may be spent in application loops, allocation/GC, contention, or repeated retries. Container quotas can throttle bursts even below a misleading averaged CPU graph.
    - **Diagnosis:** compare pods and revisions; use JFR samples, allocation and GC events, throttling counters, and repeated thread dumps. OS thread-ID matching is platform-specific.
    - **Fix:** bound pathological input, remove quadratic scans, cap retries, or correct CPU requests/limits after measurement.
    - **Trade-off:** adding replicas helps parallelizable load, not one hot key or shared bottleneck; profiling has overhead and sampling blind spots.
    - **Follow-up:** How would you validate a regex-backtracking hypothesis without replaying harmful load into production?
    - **Takeaway:** Optimize the measured hot path, not the busiest-looking stack once.

### Q5. How does JIT warm-up affect deployment and benchmark design?

??? question "Reveal answer"
    - **A newly ready JVM may not yet have steady-state latency or throughput.**
    - **Mechanism:** tiered compilation uses runtime profiles for optimization; class loading, cache population, and connection establishment also contribute to cold behavior.
    - **Failure:** a probe succeeds before representative paths are warm; load tests mix startup and steady-state results or suffer deoptimization after workload changes.
    - **Fix:** measure cold and warm behavior separately; use representative warm-up, gradual traffic, and startup probes with explicit readiness criteria.
    - **Trade-off:** warming every path delays deployment; CDS or other startup techniques reduce selected costs, not every runtime dependency.
    - **Follow-up:** Why can a microbenchmark optimize away the work being measured?
    - **Takeaway:** Readiness, warm-up, and steady-state performance are different contracts.

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Interview Questions](questions.md)
- [Solutions](solutions.md)
- [Performance practice](../performance/practice.md)
