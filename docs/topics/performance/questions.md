# Performance Interview Questions

<!-- --8<-- [start:basic] -->
## Basic

### Q: What is the difference between latency and throughput?

??? question "Reveal answer"

    **Short Answer:** Latency is elapsed time for one operation. Throughput is completed operations
    per unit time. Report both with workload and concurrency because improving one may worsen the other.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q01LatencyVsThroughputExample.java"
        ```

    [Concept](/topics/performance/concepts.md#latency-throughput-and-percentiles) · [Review exercises](/topics/performance/code-review.md) · [Solutions](/topics/performance/solutions.md)

### Q: Why is average latency insufficient?

??? question "Reveal answer"

    **Short Answer:** Latency is usually skewed. A small set of very slow requests can harm users
    while barely moving the mean, so p95, p99, timeouts, and the histogram matter.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q02PercentileExample.java"
        ```

    [Concept](/topics/performance/concepts.md#latency-throughput-and-percentiles) · [Review exercises](/topics/performance/code-review.md) · [Solutions](/topics/performance/solutions.md)

### Q: What is resource saturation?

??? question "Reveal answer"

    **Short Answer:** Saturation means demand waits because a resource is at capacity. Examples are
    CPU run queues, executor queues, Hikari pending callers, and database lock waits.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q03SaturationExample.java"
        ```

    [Concept](/topics/performance/concepts.md#utilization-and-saturation) · [Review exercises](/topics/performance/code-review.md) · [Solutions](/topics/performance/solutions.md)

### Q: Which CPU and memory signals begin a performance investigation?

??? question "Reveal answer"

    **Short Answer:** Start with CPU utilization and run queue, allocation rate, live set, GC pause
    and frequency, swap/page faults, and process/container limits. Correlate them with request latency.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q04CpuVsMemoryExample.java"
        ```

    [Concept](/topics/performance/internals.md#allocation-and-gc) · [Review exercises](/topics/performance/code-review.md) · [Solutions](/topics/performance/solutions.md)

### Q: Which Hikari metrics indicate pool exhaustion?

??? question "Reveal answer"

    **Short Answer:** Active equals maximum, idle is zero, pending rises, and acquisition timeouts
    occur. Pair them with DB CPU, locks, transaction duration, and traces to find why connections stay held.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q05PoolMetricExample.java"
        ```

    [Concept](/topics/performance/concepts.md#resource-budgets) · [Review exercise](/topics/performance/code-review.md#hikari-exhaustion) · [Solution](/topics/performance/solutions.md#connection-pool-budget-and-short-resource-scopes)

### Q: How do CPU-bound and blocking workloads affect thread pool sizing?

??? question "Reveal answer"

    **Short Answer:** CPU work starts near processor count. Blocking work may use more workers based
    on measured wait/service ratio, while every downstream resource remains separately bounded.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q06ThreadSizingExample.java"
        ```

    [Internals](/topics/performance/internals.md#executor-sizing) · [Review exercise](/topics/performance/code-review.md#oversized-thread-pool) · [Solution](/topics/performance/solutions.md#bounded-workload-aware-executors)

### Q: What is Java Flight Recorder used for?

??? question "Reveal answer"

    **Short Answer:** JFR records time-bounded JVM and application events such as CPU samples,
    allocations, GC, locks, threads, and I/O with low enough overhead for production diagnostics.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q07JfrExample.java"
        ```

    [Concept](/topics/performance/concepts.md#experimental-method) · [Production](/topics/performance/production.md#jfr-recipes) · [Solutions](/topics/performance/solutions.md)

### Q: Why use JMH instead of a stopwatch loop?

??? question "Reveal answer"

    **Short Answer:** JMH controls warmup, forks, state scope, timing, and result consumption, reducing
    errors from JIT compilation, dead-code elimination, constant folding, and shared state.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q08BenchmarkExample.java"
        ```

    [Internals](/topics/performance/internals.md#jmh-and-profiling) · [Review exercises](/topics/performance/code-review.md) · [Solutions](/topics/performance/solutions.md)
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### Q: How does Little's Law help capacity planning?

??? question "Reveal answer"

    **Short Answer:** In a stable system, in-flight work is throughput multiplied by average time.

    **Internal Mechanism:** More service time at the same arrival rate requires more concurrency;
    once a finite resource caps concurrency, the excess becomes queue time.

    **Common Mistake:** Treating the average estimate as a safe hard limit without burst or tail headroom.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q09LittlesLawExample.java"
        ```

    [Concept](/topics/performance/concepts.md#capacity-and-littles-law) · [Review exercise](/topics/performance/code-review.md#hikari-exhaustion) · [Solution](/topics/performance/solutions.md#connection-pool-budget-and-short-resource-scopes)

### Q: What is coordinated omission in load testing?

??? question "Reveal answer"

    **Short Answer:** A closed generator waits for slow responses and stops sending arrivals during a stall,
    so the missing requests are absent from the latency distribution.

    **Internal Mechanism:** The system's pause throttles the generator instead of creating the queue a real
    independent arrival stream would create.

    **Common Mistake:** Reporting excellent percentiles from a generator whose achieved rate collapsed.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q10CoordinatedOmissionExample.java"
        ```

    [Concept](/topics/performance/concepts.md#experimental-method) · [Production](/topics/performance/production.md#load-models) · [Review exercises](/topics/performance/code-review.md)

### Q: How should Hikari pool size account for multiple replicas?

??? question "Reveal answer"

    **Short Answer:** Reserve database capacity, divide the remainder across maximum replicas and other clients,
    then validate under representative queries.

    **Internal Mechanism:** Every replica's maximum can be checked out simultaneously; local settings multiply.

    **Common Mistake:** Giving every replica the whole database connection limit.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q11PoolBudgetExample.java"
        ```

    [Concept](/topics/performance/concepts.md#resource-budgets) · [Review exercise](/topics/performance/code-review.md#hikari-exhaustion) · [Solution](/topics/performance/solutions.md#connection-pool-budget-and-short-resource-scopes)

### Q: Why does a bounded executor queue improve reliability?

??? question "Reveal answer"

    **Short Answer:** It caps retained work and queue delay, turning overload into an explicit signal.

    **Internal Mechanism:** Once workers and queue are full, the rejection policy applies backpressure.

    **Common Mistake:** Choosing a huge capacity that only postpones timeout and OOM.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q12QueueingExample.java"
        ```

    [Concept](/topics/performance/internals.md#queues-and-tail-latency) · [Review exercise](/topics/performance/code-review.md#oversized-thread-pool) · [Solution](/topics/performance/solutions.md#bounded-workload-aware-executors)

### Q: How can GC affect p99 latency when average CPU looks healthy?

??? question "Reveal answer"

    **Short Answer:** Stop-the-world pauses and allocation stalls add directly to unlucky requests.

    **Internal Mechanism:** Allocation fills regions, collections pause threads at safepoints, and promotion or
    remembered-set work can create infrequent long pauses.

    **Common Mistake:** Tuning a collector before measuring allocation rate, live set, and pause causes.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q13GcTailLatencyExample.java"
        ```

    [Internals](/topics/performance/internals.md#allocation-and-gc) · [Review exercise](/topics/performance/code-review.md#excessive-allocation) · [Solution](/topics/performance/solutions.md#allocation-conscious-encoding)

### Q: How do you prove an N+1 query problem?

??? question "Reveal answer"

    **Short Answer:** Count statements while varying result size and inspect trace spans or SQL logs.

    **Internal Mechanism:** One parent query triggers a child lookup per row, so statements grow as `1 + N`.

    **Common Mistake:** Inferring N+1 only from slow elapsed time or fixing it with an unbounded join fetch.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q14NPlusOneExample.java"
        ```

    [Concept](/topics/performance/concepts.md#resource-budgets) · [Review exercise](/topics/performance/code-review.md#n1-summaries) · [Solution](/topics/performance/solutions.md#bounded-batch-loading)

### Q: Why is allocation rate often more useful than object count?

??? question "Reveal answer"

    **Short Answer:** Allocation rate expresses ongoing collector work and scales with traffic.

    **Internal Mechanism:** Short-lived objects repeatedly consume TLAB and young-generation capacity even when
    the live set remains small.

    **Common Mistake:** Optimizing small allocations without a profile showing their aggregate rate.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q15AllocationRateExample.java"
        ```

    [Internals](/topics/performance/internals.md#allocation-and-gc) · [Review exercise](/topics/performance/code-review.md#excessive-allocation) · [Solution](/topics/performance/solutions.md#allocation-conscious-encoding)

### Q: How do you distinguish lock contention from high CPU?

??? question "Reveal answer"

    **Short Answer:** Contended threads spend wall time blocked rather than executing; inspect JFR monitor events,
    thread states, blocked time, and throughput as concurrency rises.

    **Internal Mechanism:** A monitor permits one owner and parks competing threads until release.

    **Common Mistake:** Adding threads to a serialized critical section.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q16LockContentionExample.java"
        ```

    [Internals](/topics/performance/internals.md#locks-and-striped-counters) · [Review exercise](/topics/performance/code-review.md#lock-contention) · [Solution](/topics/performance/solutions.md#contention-friendly-accumulation)

### Q: How does JFR (Java Flight Recorder) continuous event streaming work in production environments?

??? question "Reveal answer"

    **Short Answer:** JDK 14+ JFR Event Streaming (`jdk.jfr.consumer.RecordingStream`) enables applications to consume flight recording events asynchronously in-flight without writing recordings to disk or triggering manual snapshot dumps.

    **Internal Mechanism:** JVM threads write events into thread-local ring buffers that flush to disk chunks. `RecordingStream` attaches a parser directly to in-flight chunks, notifying registered event listeners (`onEvent("jdk.CPULoad", ...)`) in real time with sub-1% CPU overhead.

    **Common Mistake:** Running heavy disk-dump commands (`jcmd JFR.dump`) under high load, causing I/O pauses instead of streaming lightweight targeted events.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q24JfrEventStreamingProductionProfilingExample.java"
        ```

    [Internals](/topics/performance/internals.md#jmh-and-profiling) · [Production](/topics/performance/production.md#jfr-recipes) · [Solutions](/topics/performance/solutions.md)

### Q: How do you size HikariCP database connection pools using hardware spindle and core formulas?

??? question "Reveal answer"

    **Short Answer:** The PostgreSQL / HikariCP pool sizing formula is $\text{connections} = ((\text{CPU cores} \times 2) + \text{effective spindles})$. For modern multi-tenant cloud databases backed by fast NVMe SSDs, a small pool (e.g. 10–20 connections per pod) outperforms large pools by eliminating disk head thrashing and OS context switching.

    **Internal Mechanism:** Relational databases execute queries synchronously on disk blocks and OS pages. When connection count exceeds CPU core capacity, database worker processes spend more time context switching and fighting over cache lines than executing query plans.

    **Common Mistake:** Sizing connection pools to match web thread counts (e.g. 200 connections for 200 Tomcat threads), which overwhelms database CPU and causes lock contention.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q25HikariPoolSizingFormulaHardwareLimitsExample.java"
        ```

    [Concept](/topics/performance/concepts.md#resource-budgets) · [Review exercise](/topics/performance/code-review.md#hikari-exhaustion) · [Solutions](/topics/performance/solutions.md#connection-pool-budget-and-short-resource-scopes)
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### Q: A team proposes caching, a larger pool, and a new GC to fix p99. How do you lead the investigation?

??? question "Reveal answer"

    **Short Answer:** Freeze speculative changes, define the objective and workload, baseline distributions and
    saturation, profile the matching interval, then test one causal change.

    **Deep Explanation:** Each proposal targets a different bottleneck and can move pressure elsewhere.

    **Internal Mechanism:** Queue time, service time, allocation, locks, and downstream waits leave distinct evidence.

    **Common Mistake:** Comparing different loads or changing several controls together.

    **Production Consideration:** Record environment, commit, flags, traffic mix, errors, and resource ceilings.

    **Follow-up Questions:** How do you decide rollback criteria? Which data must be retained?

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q17EvidenceDrivenTuningExample.java"
        ```

    [Concept](/topics/performance/concepts.md#experimental-method) · [Review exercises](/topics/performance/code-review.md) · [Diagnostic sequence](/topics/performance/production.md#diagnostic-sequence)

### Q: Design connection and thread capacity for an autoscaled service.

??? question "Reveal answer"

    **Short Answer:** Model global downstream budgets at maximum replicas, then assign per-instance caps, timeouts,
    queues, and overload behavior.

    **Deep Explanation:** Autoscaling multiplies every local pool while a database limit stays fixed.

    **Internal Mechanism:** Little's Law estimates concurrency; pool and queue caps constrain admitted work.

    **Common Mistake:** Scaling application replicas until PostgreSQL becomes the queue.

    **Production Consideration:** Reserve operational connections and alert before pending requests exhaust deadlines.

    **Follow-up Questions:** How do virtual threads alter the model? What if replicas scale unevenly?

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q18CapacityModelExample.java"
        ```

    [Concept](/topics/performance/concepts.md#resource-budgets) · [Review exercise](/topics/performance/code-review.md#hikari-exhaustion) · [Pool solution](/topics/performance/solutions.md#connection-pool-budget-and-short-resource-scopes)

### Q: Why can ten parallel downstream calls worsen end-to-end tail latency?

??? question "Reveal answer"

    **Short Answer:** Completion waits for the slowest call, so the chance that at least one call hits its tail rises
    with fan-out.

    **Deep Explanation:** Fan-out also multiplies downstream concurrency and connection demand.

    **Internal Mechanism:** If each call is fast 99% of the time, all ten are fast only about 90.4% of the time.

    **Common Mistake:** Treating parallelism as free latency reduction.

    **Production Consideration:** Bound concurrency, use deadlines, cache or aggregate, and measure partial results.

    **Follow-up Questions:** When can hedging help? How do cancellation and idempotency interact?

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q19TailAmplificationExample.java"
        ```

    [Concepts](/topics/performance/concepts.md) · [Review exercise](/topics/performance/code-review.md#oversized-thread-pool) · [Solution](/topics/performance/solutions.md#bounded-workload-aware-executors)

### Q: Design a load test that can support a capacity decision.

??? question "Reveal answer"

    **Short Answer:** Reproduce traffic mix, payloads, arrival model, dependencies, limits, warmup, steady state,
    spikes, and soak while collecting distributions and saturation.

    **Deep Explanation:** A single endpoint and constant closed-user test rarely exercises production bottlenecks.

    **Internal Mechanism:** Open arrivals expose queueing; closed users model interactive think time but risk omission.

    **Common Mistake:** Reporting requested rate when achieved throughput fell.

    **Production Consideration:** Isolate test data, cap cost, and define abort conditions.

    **Follow-up Questions:** How do you test cache warmup? How do you validate the generator is not saturated?

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q20LoadTestModelExample.java"
        ```

    [Concept](/topics/performance/concepts.md#experimental-method) · [Review exercises](/topics/performance/code-review.md) · [Load models](/topics/performance/production.md#load-models)

### Q: When is lower-level allocation optimization justified in application code?

??? question "Reveal answer"

    **Short Answer:** When a representative profile identifies the path, its aggregate cost threatens an objective,
    and a simpler systemic fix is unavailable.

    **Deep Explanation:** Tiny per-call savings matter at high frequency, but specialized code carries maintenance cost.

    **Internal Mechanism:** Reduced allocation lowers TLAB refill and collection pressure; JIT escape analysis may
    already remove some source-level objects.

    **Common Mistake:** Reusing a mutable builder across threads or trusting a synthetic one-shot timer.

    **Production Consideration:** Preserve correctness tests and rerun the same JFR or JMH experiment.

    **Follow-up Questions:** What evidence would make you revert the optimization? How do you prevent benchmark drift?

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q21OptimizationTradeoffExample.java"
        ```

    [Internals](/topics/performance/internals.md#allocation-and-gc) · [Review exercise](/topics/performance/code-review.md#excessive-allocation) · [Allocation solution](/topics/performance/solutions.md#allocation-conscious-encoding)

### Q: What is the 32GB Compressed OOPs cliff and how does it impact JVM heap sizing decisions?

??? question "Reveal answer"

    **Short Answer:** Under 32GB heap size, HotSpot enables Compressed Ordinary Object Pointers (`-XX:+UseCompressedOops`), encoding 64-bit object references in 32 bits via an 8-byte alignment shift. At 32GB or higher, pointer compression fails, and all references expand from 4 bytes to 8 bytes, causing a 32GB heap to hold *less* live data than a 31GB heap.

    **Internal Mechanism:** 32-bit addresses can reference $2^{32} = 4\text{ GB}$. Because Java objects align on 8-byte boundaries, HotSpot shifts the 32-bit address left by 3 bits ($\times 8$), covering up to $4\text{ GB} \times 8 = 32\text{ GB}$. Exceeding 32GB requires full 64-bit pointers, increasing reference footprint by 50–100% and saturating CPU L1/L2/L3 caches.

    **Common Mistake:** Sizing heaps to 32GB or 34GB to "add a little extra memory", which actually reduces usable memory by ~20% due to reference expansion. A heap should either stay below 31GB (e.g. `-Xmx31g`) or jump straight to $\ge 48\text{GB}$.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q26CompressedOopsThirtyTwoGigabyteCliffExample.java"
        ```

    [Internals](/topics/performance/internals.md#allocation-and-gc) · [Concepts](/topics/performance/concepts.md#resource-budgets) · [Solutions](/topics/performance/solutions.md)

### Q: How do you interpret Async-Profiler allocation flame graphs (`-e alloc`) to eliminate garbage collection pauses?

??? question "Reveal answer"

    **Short Answer:** Async-Profiler allocation profiling hooks into HotSpot TLAB (Thread-Local Allocation Buffer) refills and out-of-TLAB allocations. Flame graphs visualize call stacks weighted by total bytes allocated (MB/s) rather than CPU cycles, isolating the exact methods responsible for Young Gen exhaustion and frequent GC pause spikes.

    **Internal Mechanism:** Profiling via bytecode instrumentation or standard JVMTI introduces severe safe-point bias and observer overhead. Async-Profiler intercepts HotSpot internal C++ runtime allocation stubs via `AsyncGetCallTrace`, capturing realistic allocation stacks without safe-point skew.

    **Common Mistake:** Focus only on CPU flame graphs (`-e cpu`) when diagnosing high p99 latency caused by GC pauses; CPU flame graphs show GC threads executing work, but do not show *which application methods* generated the allocated garbage.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q27AsyncProfilerFlameGraphAllocationProfilingExample.java"
        ```

    [Internals](/topics/performance/internals.md#jmh-and-profiling) · [Review exercise](/topics/performance/code-review.md#excessive-allocation) · [Solutions](/topics/performance/solutions.md#allocation-conscious-encoding)

### Q: How does Linux cgroups v2 CFS quota CPU throttling degrade Java latency in containerized Kubernetes pods?

??? question "Reveal answer"

    **Short Answer:** In Linux cgroups v2, CPU limits are enforced via Completely Fair Scheduler (CFS) quotas (`cpu.cfs_quota_us` and `cpu.cfs_period_us`). When multi-threaded JVM applications spike all CPU cores simultaneously, their millisecond quota is consumed in the first fraction of the 100ms period. The Linux kernel suspends all container threads for the remainder of the period, causing massive tail latency spikes (50–100ms freezes) without reporting high 1-minute CPU averages.

    **Internal Mechanism:** A container with a limit of 2.0 CPUs has a 200ms quota per 100ms period. If 8 parallel worker threads execute simultaneously, 200ms of CPU time is consumed in just $200\text{ms} / 8 = 25\text{ms}$. For the remaining 75ms of the period, the container's threads are frozen in an OS wait state (`nr_throttled` counter increments).

    **Common Mistake:** Looking at 1-minute average CPU utilization in Grafana (e.g. showing 40% CPU) and concluding the pod is not CPU-starved, unaware of micro-throttling occurring at 100ms intervals.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q28LinuxCgroupsV2CfsQuotaCpuThrottlingExample.java"
        ```

    [Concept](/topics/performance/concepts.md#resource-budgets) · [Production](/topics/performance/production.md) · [Solutions](/topics/performance/solutions.md)
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenarios

### Q: API p99 is 9 seconds; Hikari active is 20/20, idle 0, pending 75, and DB CPU 20%. Diagnose and redesign.

??? question "Reveal answer"

    **Short Answer:** The pool is saturated while the DB is not; find where checked-out connections wait, especially
    remote calls, long transactions, and locks.

    **Deep Explanation:** Increasing the pool may hide the holder and move saturation into PostgreSQL.

    **Internal Mechanism:** Connections are acquired before work and remain unavailable until scope close or commit.

    **Common Mistake:** Raising `maximumPoolSize` from the symptom alone.

    **Production Consideration:** Capture pending-thread stacks and transaction spans, shorten boundaries, add finite
    acquisition timeout, and budget connections over maximum replicas.

    **Follow-up Questions:** What if payment succeeds before final commit? How is reconciliation designed?

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q22HikariIncidentExample.java"
        ```

    [Concept](/topics/performance/concepts.md#resource-budgets) · [Review exercise](/topics/performance/code-review.md#hikari-exhaustion) · [Solution](/topics/performance/solutions.md#connection-pool-budget-and-short-resource-scopes)

### Q: After a release, throughput is flat but p99 triples; allocation is 900 MB/s and GC pause p99 is 240 ms. What next?

??? question "Reveal answer"

    **Short Answer:** Correlate the regression window, capture a bounded JFR allocation profile, compare call paths to
    the prior release, and reproduce with the same request mix.

    **Deep Explanation:** High allocation is evidence, while the causal site and live-set behavior still need proof.

    **Internal Mechanism:** More short-lived objects increase collection frequency; promoted objects can lengthen pauses.

    **Common Mistake:** Switching collectors before identifying the new allocation path.

    **Production Consideration:** Roll back if the objective is breached, then optimize and validate correctness plus
    allocation, CPU, pause, and latency distributions.

    **Follow-up Questions:** Which JFR views do you inspect? When would heap sizing help?

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q23LatencyIncidentExample.java"
        ```

    [Internals](/topics/performance/internals.md#allocation-and-gc) · [Review exercise](/topics/performance/code-review.md#excessive-allocation) · [Solution](/topics/performance/solutions.md#allocation-conscious-encoding)

### Q: Incident: A production microservice crashes with `OutOfMemoryError: Metaspace` after 48 hours of uptime. How do you triage and fix classloader leaks?

??? question "Reveal answer"

    **Short Answer:** Metaspace leaks are caused by continuous dynamic bytecode generation (e.g. CGLIB proxies, Groovy script evaluation, un-cached reflection proxies, or XML parsers) creating anonymous `ClassLoader` instances that cannot be garbage collected because references are held in static fields or thread-local maps. Remediate by capturing a Metaspace heap dump (`jcmd <pid> GC.class_stats` or `-XX:+HeapDumpOnOutOfMemoryError`), identifying the leaking `ClassLoader`, and caching generated proxy classes or setting `-XX:MaxMetaspaceSize`.

    **Deep Explanation:** Unlike JVM heap memory, Metaspace resides in off-heap native memory allocated by OS `mmap()`. Classes can only be unloaded when their defining `ClassLoader` itself becomes unreachable and unreferenced. If an application generates dynamic proxies per-request rather than caching them statically, thousands of distinct `ClassLoader` objects are pinned in memory.

    **Internal Mechanism:** HotSpot allocates metadata chunks for each `ClassLoader`. When the total allocated chunks reach `-XX:MaxMetaspaceSize`, the JVM triggers a full GC. If classloaders remain strongly referenced, the collector cannot reclaim space and throws `java.lang.OutOfMemoryError: Metaspace`.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q29IncidentMetaspaceLeakProxyGenerationExample.java"
        ```

    **Common Mistake:** Arbitrarily doubling `-XX:MaxMetaspaceSize` from 256MB to 1GB without investigating the leak; this only delays the crash by a few days while bloating native process memory.

    **Production Consideration:** Monitor `jvm.memory.used{area="nonheap",id="Metaspace"}` in Prometheus and alert on steady upward trends that do not drop after full garbage collection.

    [Internals](/topics/performance/internals.md#allocation-and-gc) · [Review exercise](/topics/performance/code-review.md) · [Solutions](/topics/performance/solutions.md)

### Q: Incident: A service freezes under load with 0% CPU utilization and all threads blocked. How do you identify thread pool starvation deadlocks?

??? question "Reveal answer"

    **Short Answer:** Thread pool starvation deadlock occurs when parent tasks submitted to a bounded thread pool spawn child sub-tasks to the *same* pool and block waiting for child completion (`childFuture.get()`). Under load, all worker threads become occupied by parent tasks waiting for child tasks that sit indefinitely in the executor queue, causing a complete system deadlock with zero CPU usage. Remediate by separating thread pools (never share worker pools between parent and child tasks) or migrating to non-blocking async composition or Java 21+ Virtual Threads.

    **Deep Explanation:** If a thread pool has 8 threads, and 8 concurrent requests arrive, all 8 worker threads pick up parent tasks. Each parent task submits 2 child tasks to the queue and calls `.get()`. Because the pool has 0 idle threads, the 16 child tasks sit in the queue forever. The parent threads cannot finish until child tasks execute, creating a cyclic dependency on pool capacity.

    **Internal Mechanism:** Thread dumps reveal all worker threads in `WAITING` state on `CompletableFuture.reportGet()` or `CountDownLatch.await()`, while queue metrics (`executor.queued`) show non-zero waiting tasks and CPU utilization drops to zero.

    ??? example "Example"

        ```java
        --8<-- "modules/23-performance/src/examples/java/lab/performance/questions/Q30IncidentParentChildTaskThreadPoolDeadlockExample.java"
        ```

    **Common Mistake:** Sizing thread pools larger to "fix" the problem; increasing the pool to 32 threads merely raises the deadlock threshold from 8 concurrent requests to 32 concurrent requests.

    **Production Consideration:** Enforce separate, isolated `ThreadPoolExecutor` instances for child tasks, or use `Executors.newVirtualThreadPerTaskExecutor()` where tasks do not hoard platform threads while waiting.

    [Concept](/topics/performance/concepts.md#resource-budgets) · [Review exercise](/topics/performance/code-review.md#lock-contention) · [Solutions](/topics/performance/solutions.md)
<!-- --8<-- [end:scenarios] -->

## Related

- [Concepts](concepts.md)
- [Code review](code-review.md)
- [Production](production.md)
