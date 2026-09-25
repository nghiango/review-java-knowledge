# JVM Interview Questions

<!-- --8<-- [start:basic] -->
## Basic

### 1. What is the difference between JVM, JRE, and JDK?

??? question "Reveal answer"

    **Short Answer:** The JVM is the runtime virtual machine executing bytecode. The JRE includes the JVM plus runtime libraries. The JDK is the complete development kit including compiler, debugger, and diagnostic tools (`jcmd`, `jfr`, `javap`). [Concept](/topics/jvm/concepts.md#jvm-jre-and-jdk)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q01JvmJreJdkExample.java"
        ```

### 2. What is Java bytecode and how does the JVM execute it?

??? question "Reveal answer"

    **Short Answer:** Bytecode is the intermediate, stack-based instruction set produced by `javac`. The JVM executes it via an interpreter and compiles hot paths into native machine code using JIT compilers. [Concept](/topics/jvm/concepts.md#jit-compilation-and-tiered-execution)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q02BytecodeExecutionExample.java"
        ```

### 3. How is JVM memory divided between Stack and Heap?

??? question "Reveal answer"

    **Short Answer:** Heap is shared across all threads storing object instances and arrays, managed by GC. Stacks are per-thread storing stack frames (local variables, operand stacks, return links), reclaimed automatically when methods return. [Concept](/topics/jvm/concepts.md#runtime-data-areas)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q03StackVsHeapExample.java"
        ```

### 4. What is Metaspace and how does it differ from PermGen?

??? question "Reveal answer"

    **Short Answer:** Metaspace stores class metadata in off-heap native memory (introduced in Java 8), expanding dynamically by default up to system memory limits. PermGen was fixed-size contiguous heap space that frequently threw `OOM: PermGen space`. [Concept](/topics/jvm/concepts.md#runtime-data-areas)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q04MetaspaceMemoryExample.java"
        ```

### 5. How does the class loader hierarchy and parent delegation work?

??? question "Reveal answer"

    **Short Answer:** Class loaders delegate loading requests to their parent before attempting to load locally (Bootstrap $\to$ Platform $\to$ Application $\to$ Custom). This prevents application classes from overriding trusted core system classes like `java.lang.String`. [Internals](/topics/jvm/internals.md#class-loader-hierarchy-and-parent-delegation)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q05ClassLoaderDelegationExample.java"
        ```

### 6. What is the role of the Just-In-Time (JIT) compiler?

??? question "Reveal answer"

    **Short Answer:** The JIT compiler profiles running bytecode to identify "hot" methods and loops, compiling them into optimized native machine code at runtime with inlining, loop unrolling, and dead code elimination. [Internals](/topics/jvm/internals.md#jit-optimizations-and-deoptimization)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q06JitCompilationExample.java"
        ```

### 7. What is a Garbage Collection root and object reachability?

??? question "Reveal answer"

    **Short Answer:** A GC Root is an object directly accessible from outside the heap (thread stack variables, JNI references, loaded class static fields). An object is retained if a chain of strong references leads back to a live GC root. [Concept](/topics/jvm/concepts.md#gc-roots-and-reachability)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q07GcRootsReachabilityExample.java"
        ```

### 8. What is a Stop-The-World (STW) pause in garbage collection?

??? question "Reveal answer"

    **Short Answer:** A pause where all application threads are halted at safepoints to allow the GC engine to safely mutate reference graphs, scan root references, or compact memory regions without concurrent race conditions. [Internals](/topics/jvm/internals.md#safepoints-and-thread-coordination)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q08StopTheWorldPauseExample.java"
        ```

<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 1. What are the phases of the class loading and linking lifecycle?

??? question "Reveal answer"

    **Short Answer:** Loading (reading byte stream into `Class` object), Linking (Verification, Preparation allocating default static field values, Resolution of symbolic references), and Initialization (`<clinit>` execution). [Internals](/topics/jvm/internals.md#class-loading-and-lifecycle)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q09ClassLoadingLifecycleExample.java"
        ```

### 2. What events trigger class initialization in the JVM?

??? question "Reveal answer"

    **Short Answer:** Direct instantiation (`new`), accessing/mutating a non-constant static field, calling a static method, reflective loading with `initialize=true`, or subclass initialization. Accessing compile-time constants (`static final`) does not trigger initialization. [Internals](/topics/jvm/internals.md#class-initialization-triggers)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q10ClassInitializationTriggersExample.java"
        ```

### 3. What is a TLAB and how does Escape Analysis enable scalar replacement?

??? question "Reveal answer"

    **Short Answer:** A TLAB (Thread-Local Allocation Buffer) allows fast bump-the-pointer heap allocations in Eden without cross-thread lock contention. Escape Analysis detects objects that do not escape method scope, allowing scalar replacement directly onto stack frames or CPU registers. [Concept](/topics/jvm/concepts.md#object-allocation-tlabs-and-escape-analysis)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q11TlabAndEscapeAnalysisExample.java"
        ```

### 4. How does Tiered Compilation (C1 vs C2) and Deoptimization work?

??? question "Reveal answer"

    **Short Answer:** Tier 0 (Interpreter) starts instantly; C1 (Tiers 1–3) adds fast compilation and profiling; C2 (Tier 4) produces heavily optimized machine code. When optimistic profiling assumptions are violated by new class loading, deoptimization unwinds machine frames back to the interpreter. [Internals](/topics/jvm/internals.md#jit-optimizations-and-deoptimization)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q12TieredCompilationDeoptExample.java"
        ```

### 5. How does the Weak Generational Hypothesis shape GC design?

??? question "Reveal answer"

    **Short Answer:** It observes that most objects die young. Memory is divided into Young (Eden, Survivors) and Old generations, allowing frequent, cheap minor collections on Eden without scanning long-lived Old generation objects. [Concept](/topics/jvm/concepts.md#garbage-collection-and-the-generational-hypothesis)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q13GenerationalHypothesisExample.java"
        ```

### 6. How does G1 GC organize memory regions and Remembered Sets?

??? question "Reveal answer"

    **Short Answer:** G1 divides the heap into 1 MB–32 MB equal regions dynamically assigned as Eden, Survivor, or Old. Remembered Sets (RSets) track Old-to-Young references using Card Tables, enabling independent regional collection without full-heap scans. [Internals](/topics/jvm/internals.md#remembered-sets-rset-and-card-tables)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q14G1RegionsAndRememberedSetsExample.java"
        ```

### 7. When do you use a Thread Dump, Heap Dump, or Java Flight Recording (JFR)?

??? question "Reveal answer"

    **Short Answer:** Thread dumps diagnose deadlocks, thread pool exhaustion, and CPU spin. Heap dumps diagnose memory leaks and object retention trees. JFR provides low-overhead continuous event profiling (allocations, locks, I/O latency, GC pauses). [Concept](/topics/jvm/concepts.md#diagnostic-commands)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q15DiagnosticToolsComparisonExample.java"
        ```

### 8. What are the different types and causes of OutOfMemoryError?

??? question "Reveal answer"

    **Short Answer:** `Java heap space` (retained objects exceed `-Xmx`), `Metaspace` (classloader leaks), `Direct buffer memory` (off-heap NIO buffer leaks), `unable to create native thread` (OS thread limits), and `GC overhead limit exceeded` (>98% CPU in GC recovering <2% heap). [Concept](/topics/jvm/concepts.md#error-taxonomy-oom-and-stackoverflow)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q16OutOfMemoryTaxonomyExample.java"
        ```

### 9. What is Class Data Sharing (AppCDS) and how does it optimize microservice startup time?

??? question "Reveal answer"

    **Short Answer:** AppCDS dumps parsed class metadata into a memory-mapped archive (`.jsa`), bypassing class parsing, bytecode verification, and linking on subsequent launches to accelerate startup by 30–50%.

    **Internal Mechanism:** The JVM maps the `.jsa` archive into memory using OS `mmap`. Metadata pages are shared read-only across multiple JVM processes on the same host, reducing collective RSS memory footprint.

    **Common Mistake:** Attempting to use a CDS archive created with a different JVM build, classpath ordering, or JDK vendor patch version, which invalidates the archive checksum and triggers standard class loading. [Concepts](/topics/jvm/concepts.md#runtime-data-areas)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q24ClassDataSharingExample.java"
        ```

### 10. How does Generational ZGC achieve sub-millisecond GC pause times on large heaps?

??? question "Reveal answer"

    **Short Answer:** Generational ZGC separates young and old generations concurrently, using colored pointers (metadata in reference bits 42–45) and JIT-compiled load barriers to evacuate and relocate objects without global Stop-The-World compaction.

    **Internal Mechanism:** When an application thread dereferences an object pointer pointing into an evacuating page, the load barrier intercepts the access, resolves the forwarding pointer or relocates the object immediately (self-healing), allowing user threads to run concurrently during collection.

    **Common Mistake:** Assuming ZGC has higher peak throughput than G1 or Parallel GC; concurrent load barriers and GC worker threads trade ~5–10% raw throughput for sub-millisecond latency guarantees. [Concepts](/topics/jvm/concepts.md#garbage-collection-and-the-generational-hypothesis)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q25GenerationalZgcExample.java"
        ```

<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 1. Heap climbs steadily after each application redeployment while business data is flat. Diagnose it.

??? question "Reveal answer"

    **Short Answer:** Check for ClassLoader memory leaks caused by static references, ThreadLocals, or daemon threads retaining classes from undeployed modules.

    **Deep Explanation:** A `ClassLoader` instance holds references to every `Class<?>` it defines, each holding static state. If a long-lived parent context or system class retains even a single object or listener from the child application classloader, the entire class graph and its Metaspace/heap metadata cannot be reclaimed.

    **Internal Mechanism:** JVM root reachability pins the child `ClassLoader` as a live GC root.

    **Example:** [Static listener leak](/topics/jvm/code-review.md#static-listener-leak).

    **Common Mistake:** Increasing `-XX:MaxMetaspaceSize` or `-Xmx` which only delays eventual failure.

    **Production Consideration:** Implement explicit deregistration hooks in servlet/context destroy callbacks and inspect `jcmd <pid> VM.classloader_stats`.

    **Follow-up Questions:** How do you verify classloader unloading with `-Xlog:class+unload=info`?

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q17RedeployClassLoaderLeakExample.java"
        ```

### 2. A user inherits another user's authentication context after an exception occurs. Diagnose it.

??? question "Reveal answer"

    **Short Answer:** A `ThreadLocal` storage was set at request start but not cleared in a `finally` or closeable block when an exception aborted execution, leaving stale state on the pooled thread.

    **Deep Explanation:** Worker thread pools (e.g. Tomcat/Jetty dispatchers) reuse threads indefinitely. `ThreadLocalMap` entries remain attached to the `Thread` instance. If an uncaught exception skips manual `ThreadLocal.remove()`, the next request handled by that worker inherits the previous request's identity.

    **Internal Mechanism:** `ThreadLocalMap` keys are weak references, but entry values are strong references held by the `Thread` instance as a GC root.

    **Example:** [ThreadLocal pool leak](/topics/jvm/code-review.md#threadlocal-pool-leak).

    **Common Mistake:** Setting `ThreadLocal` without an `AutoCloseable` scope or try-finally block.

    **Production Consideration:** Wrap context propagation in scoped handles (`ContextScope`) and verify isolation with thread pool test harnesses.

    **Follow-up Questions:** How do Java 21 Scoped Values improve upon ThreadLocal context isolation?

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q18ThreadLocalPollutionExample.java"
        ```

### 3. Young GC frequency and CPU are high, but heap occupancy after collection remains low and flat. Redesign it.

??? question "Reveal answer"

    **Short Answer:** High young generation allocation churn in hot-path loops (e.g., regex compilation, intermediate string formatting, streams).

    **Deep Explanation:** When methods allocate high rates of short-lived objects, TLABs fill rapidly and trigger frequent Minor GCs. Because the objects die immediately, post-GC heap occupancy drops to baseline, but CPU is wasted on GC and memory allocation overhead.

    **Internal Mechanism:** JFR allocation events (`jdk.ObjectAllocationInNewTLAB`) pinpoint high-frequency call sites allocating transient byte arrays and objects.

    **Example:** [Excessive hot-path allocation](/topics/jvm/code-review.md#excessive-hot-path-allocation).

    **Common Mistake:** Increasing `-Xmx` or Old generation size, which does not solve high young allocation rate.

    **Production Consideration:** Replace `String.format`, regex compiling and stream pipelines on hot paths with precompiled patterns and single-pass `StringBuilder` buffers.

    **Follow-up Questions:** How does Escape Analysis with scalar replacement eliminate heap allocation?

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q19YoungGenChurnAllocationExample.java"
        ```

### 4. OutOfMemoryError occurs while Java heap occupancy is only 20% of max heap. Diagnose it.

??? question "Reveal answer"

    **Short Answer:** Exhaustion of off-heap native memory areas: Metaspace, Direct ByteBuffers, or OS Native Threads.

    **Deep Explanation:** Java processes consume off-heap memory outside the heap. Direct buffers (`ByteBuffer.allocateDirect`), Metaspace class metadata, or allocating thousands of platform threads (`-Xss 1MB` per thread) exhaust native virtual memory even when heap usage is minimal.

    **Internal Mechanism:** Native memory allocations bypass GC triggers until explicit thresholds (`-XX:MaxDirectMemorySize`, `-XX:MaxMetaspaceSize`) or OS virtual memory limits fail.

    **Example:** [Container memory demo](/topics/jvm/concepts.md#container-memory-dynamics).

    **Common Mistake:** Looking only at heap usage metrics in Grafana while neglecting process RSS and native memory tracking.

    **Production Consideration:** Enable Native Memory Tracking (`-XX:NativeMemoryTracking=summary`) and inspect with `jcmd <pid> VM.native_memory baseline/detail`.

    **Follow-up Questions:** How do Java 21 Virtual Threads prevent native thread stack exhaustion?

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q20LowHeapOffHeapOomExample.java"
        ```

### 5. A containerized Java service is killed by Linux OOMKiller (Exit 137) despite -Xmx being 50% of container limit. Diagnose it.

??? question "Reveal answer"

    **Short Answer:** Process RSS (Resident Set Size) exceeded cgroup memory limits due to unbudgeted native memory: Metaspace, Code Cache, thread stacks, Direct Buffers, and JVM overhead.

    **Deep Explanation:** Total process memory is $\text{Heap} + \text{Metaspace} + \text{CodeCache} + (\text{Threads} \times \text{StackSize}) + \text{DirectMemory} + \text{glibc malloc arena overhead}$. If a container has 1GB RAM and `-Xmx512m` is set, 300 threads ($\approx 300\text{MB}$) plus 150MB Metaspace easily pushes total RSS beyond 1GB, causing Linux OOMKiller to send `SIGKILL (137)`.

    **Internal Mechanism:** Linux cgroup memory subsystem detects limit violation and immediately terminates the highest OOM-score process without JVM crash dump.

    **Example:** [Container memory sizing](/topics/jvm/concepts.md#container-memory-dynamics).

    **Common Mistake:** Assuming `-Xmx` defines the maximum process memory.

    **Production Consideration:** Budget container memory using `-XX:MaxRAMPercentage=60-70%`, tune thread pools, cap direct memory, and set `MALLOC_ARENA_MAX=2` to limit glibc fragmentation.

    **Follow-up Questions:** How does cgroups v2 memory controller notify container runtimes before OOMKill?

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q21ContainerOomKillBudgetExample.java"
        ```

### 6. High-throughput microservice suffers severe JIT performance cliffs on polymorphic dispatch. Diagnose and redesign it.

??? question "Reveal answer"

    **Short Answer:** Megamorphic call sites ($\ge 3$ concrete implementations invoked at the same call site) cause C2 inline caches to fail, preventing devirtualization, method inlining, and scalar replacement.

    **Deep Explanation:** When a call site is monomorphic (1 type) or bimorphic (2 types), the C2 compiler generates optimized direct branch checks and inlines target method bodies. Once 3 or more concrete classes are seen, the site becomes megamorphic, falling back to full vtable lookup (`invokevirtual`), disabling escape analysis and vectorization.

    **Internal Mechanism:** HotSpot inline caches track receiver classes in an internal IC stub; megamorphic transition invalidates the inline cache and routes dispatch through the receiver class's virtual method table.

    **Example:** [JIT inlining devirtualization](/topics/jvm/concepts.md#jit-compilation-and-tiered-execution).

    **Common Mistake:** Blaming GC or network latency when the root cause is CPU branch mispredictions and inlining failure on hot polymorphic paths.

    **Production Consideration:** Use Java Flight Recorder (JFR) `jdk.Compilation` and JITWatch to identify megamorphic call sites; split heterogeneous processing loops into type-specific batches or use pattern matching switch.

    **Follow-up Questions:**
    - How does pattern matching switch compile down to avoid megamorphic vtable degradation? See [Core Java: Pattern Matching Switch](/topics/core-java/questions.md#9-how-do-pattern-matching-for-switch-and-record-patterns-enhance-type-safety-and-exhaustiveness)
    - What CPU profiling tools measure branch mispredictions and instructions per cycle (IPC)? See [Performance: Hardware Counters and Profiling](/topics/performance/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q26JitInliningDevirtualizationExample.java"
        ```

### 7. Unbounded runtime dynamic proxy generation exhausts Metaspace memory. Diagnose and remediate it.

??? question "Reveal answer"

    **Short Answer:** Bytecode generation tools (CGLIB, ByteBuddy, dynamic proxies) create unique generated classes per invocation or tenant; if loaded into distinct classloaders without class unloading, Metaspace grows until `OutOfMemoryError: Metaspace`.

    **Deep Explanation:** Metaspace stores native class metadata (Klass structures, constant pools, vtables). Unlike the Java heap, a class cannot be collected individually; all classes defined by a `ClassLoader` remain in Metaspace until their defining `ClassLoader` is unreachable and collected.

    **Internal Mechanism:** If a framework instantiates a new `ClassLoader` for each generated proxy without reusing class definitions, native Metaspace allocations cannot be reclaimed by minor heap GCs.

    **Example:** [Metaspace memory architecture](/topics/jvm/concepts.md#2-metaspace).

    **Common Mistake:** Increasing `-XX:MaxMetaspaceSize` without caching generated proxy classes, merely postponing the inevitable OOM failure.

    **Production Consideration:** Always configure proxy enhancers to cache generated classes using static ClassLoader caches, and monitor `jvm.buffer.metaspace` metrics with alerting thresholds.

    **Follow-up Questions:**
    - How do Spring Core dynamic proxies handle class caching across application context reloads? See [Spring Core: Proxy Mechanisms](/topics/spring-core/questions.md#11-how-do-jdk-dynamic-proxies-differ-from-cglib-proxies-in-spring)
    - How does class identity and ClassCastException prevent cross-loader casting? See [Core Java: ClassLoader Isolation](/topics/core-java/questions.md#7-a-plugin-system-encounters-classcastexception-and-metaspace-leaks-during-dynamic-reloading-diagnose-and-redesign-it)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q27MetaspaceBytecodeLeakExample.java"
        ```

### 8. P99.9 latency spikes correlate with Stop-The-World safepoint sync pauses. Diagnose and tune it.

??? question "Reveal answer"

    **Short Answer:** Long-running threads failing to reach safepoint polling locations delay global Stop-The-World operations, causing "Time-To-Safepoint" (TTSP) stalls that manifest as unexplained p99.9 latency spikes.

    **Deep Explanation:** For operations like GC pauses, code deoptimization, or thread dumps, all application threads must yield at safepoints. If a single thread is stuck in an uncounted loop, JNI native method, or heavy page fault, all other paused threads wait, stalling request execution.

    **Internal Mechanism:** Modern JVMs insert safepoint polls at method entries, loop back-edges, and return instructions; Thread-Local Handshakes (JEP 312) allow per-thread callbacks without stopping the entire world.

    **Example:** [Safepoints and handshakes](/topics/jvm/concepts.md#garbage-collection-and-the-generational-hypothesis).

    **Common Mistake:** Diagnosing GC algorithm pause times without analyzing the Time-To-Safepoint (TTSP) delay preceding the GC cycle.

    **Production Consideration:** Inspect safepoint duration via `-Xlog:safepoint=debug` or JFR `jdk.SafepointBegin` and `jdk.SafepointWaitBlocked`; enable loop strip mining via `-XX:+UseCountedLoopSafepoints`.

    **Follow-up Questions:**
    - How do Java thread states transition when yielding at a safepoint poll? See [Concurrency: Thread Lifecycle States](/topics/concurrency/questions.md#2-what-are-the-six-thread-lifecycle-states-in-java)
    - What Micrometer and OpenTelemetry metrics track tail latency anomalies? See [Observability: Metrics and SLIs](/topics/observability/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q28SafepointPollingHandshakeExample.java"
        ```

<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenarios

### Scenario 1: Memory grows continuously over days and GC pauses degrade latency

??? question "Reveal answer"

    **Short Answer:** An in-memory cache lacks bounded eviction, retaining user-generated request keys and large values indefinitely.

    **Deep Explanation:** Using standard `ConcurrentHashMap` or unevicted static collections as caches accumulates entries indefinitely. As Old generation fills, G1/ZGC triggers frequent concurrent mark cycles, eventually degrading into Full GC STW pauses.

    **Internal Mechanism:** Retained cache entries survive multiple Minor GCs and tenure into Old generation as permanent GC roots.

    **Example:** [Unbounded template cache](/topics/jvm/code-review.md#unbounded-template-cache).

    **Common Mistake:** Implementing a cache with `ConcurrentHashMap` without bounding capacity or time-based expiry.

    **Production Consideration:** Use bounded LRU caches or production libraries like Caffeine with maximum size, weight, TTL/TTI expiration, and export eviction/hit-rate metrics.

    **Follow-up Questions:** How do you calculate maximum cache memory footprint from sample object sizes?

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q22UnboundedCacheGrowthScenarioExample.java"
        ```

### Scenario 2: Asynchronous task workers leak memory and retain tenant listeners

??? question "Reveal answer"

    **Short Answer:** Event listeners registered during short-lived tenant operations are stored in static or long-lived registries without unregistration handles.

    **Deep Explanation:** Event listener patterns often attach lambdas capturing outer enclosing service instances. If registration does not return an `AutoCloseable` handle or hook into tenant shutdown lifecycle, listeners accumulate permanently in memory.

    **Internal Mechanism:** Strong reference chains from the static event registry keep outer tenant class instances and their dependent data structures reachable.

    **Example:** [Static listener leak](/topics/jvm/code-review.md#static-listener-leak).

    **Common Mistake:** Relying on weak references instead of explicit lifecycle management.

    **Production Consideration:** Always return an explicit `Registration` handle implementing `AutoCloseable` with idempotent closure and instance-scoped registries.

    **Follow-up Questions:** When would `WeakHashMap` or weak listener references be suitable versus explicit handles?

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q23StaticListenerRetentionScenarioExample.java"
        ```

### Scenario 3: P99 latency spikes caused by safepoint stalls in long-running counted loops

??? question "Reveal answer"

    **Short Answer:** In HotSpot C2, integer counted loops have safepoint polls stripped during loop unrolling; if the loop iterates millions of times, any GC or deoptimization request stalls until the loop finishes.

    **Deep Explanation:** When another thread initiates a Stop-The-World event (such as young GC or thread dump), it sets the global safepoint poll page to inaccessible. Threads running normal code hit the safepoint immediately, but the thread inside an unrolled counted loop without polls continues executing, causing all other application threads to stay frozen waiting for it.

    **Internal Mechanism:** The Time-To-Safepoint (TTSP) metric surges while GC actual pause time remains low.

    **Example:** [Counted loop safepoint stalls](/topics/jvm/code-review.md).

    **Common Mistake:** Blaming database queries or external HTTP services when latency spikes occur uniformly across all endpoints due to a background batch thread executing an unpolled loop.

    **Production Consideration:** Enable `-XX:+UseCountedLoopSafepoints` (loop strip mining) in JDK 10+ to break large loops into outer loops with polls, or insert small method calls or blackholes inside hot loops.

    **Follow-up Questions:**
    - How do Virtual Threads interact with safepoint polls when performing CPU-heavy tasks? See [Concurrency: Virtual Threads Internals](/topics/concurrency/questions.md#19-how-do-java-21-virtual-threads-work-and-what-causes-carrier-thread-pinning)
    - How do you configure JFR to capture safepoint stall events with zero overhead? See [Performance: JFR Profiling](/topics/performance/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q29SafepointLoopStallScenarioExample.java"
        ```

### Scenario 4: Container terminated by Linux OOMKiller (Exit 137) while Java heap remains under 40%

??? question "Reveal answer"

    **Short Answer:** Native memory allocated by unclosed native resources (`java.util.zip.Deflater`/`Inflater`, JNI bindings, or direct buffers) exceeds the container cgroup memory limit, triggering Linux kernel OOMKiller.

    **Deep Explanation:** Java classes wrapping native C libraries (like zlib via `Deflater`) allocate heap metadata that is tiny (a few bytes) but allocate megabytes of off-heap memory via native `malloc`. If application code omits `deflater.end()`, the memory is only freed when the finalizer/Cleaner runs, which never triggers because heap usage is low.

    **Internal Mechanism:** The Linux cgroup memory controller monitors total process RSS (Resident Set Size). When $\text{RSS} > \text{cgroup limit}$, the kernel sends `SIGKILL (137)` without producing an hs_err_pid crash log or heap dump.

    **Example:** [Native memory leak scenario](/topics/jvm/code-review.md).

    **Common Mistake:** Increasing container `-Xmx` or adding swap, which accelerates memory consumption without releasing native allocations.

    **Production Consideration:** Always manage native resources with `try-with-resources` or explicit `end()` calls; track native allocations with `-XX:NativeMemoryTracking=summary` (NMT) and alert on non-heap RSS growth.

    **Follow-up Questions:**
    - How do Docker container memory limits and cgroups v2 interact with JVM memory budgeting? See [Docker: Container Resource Limits](/topics/docker/questions.md)
    - How does DirectByteBuffer off-heap memory differ from JNI native malloc? See [Core Java: Direct Memory Leaks](/topics/core-java/questions.md#scenario-3-container-killed-by-os-oom-killer-despite-low-heap-occupancy-during-high-throughput-io)

    ??? example "Example"

        ```java
        --8<-- "modules/02-jvm/src/examples/java/lab/jvm/questions/Q30NativeMemoryExhaustionScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Code review](code-review.md)
- [Solutions](solutions.md)
