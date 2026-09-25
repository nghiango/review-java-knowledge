# Concurrency Interview Questions

Four levels of interview questions covering memory consistency, synchronization primitives, thread pools, asynchronous pipelines, deadlock prevention, and modern Java 21 virtual threads.

<!-- --8<-- [start:basic] -->
## Basic

### 1. What is the difference between a process and a thread in Java?

??? question "Reveal answer"
    A **process** is an execution instance created and managed by the operating system with its own isolated address space, file handles, and security context. A **thread** is the smallest unit of execution scheduled within a process.
    
    All threads within a JVM process share the common heap and metaspace memory, allowing fast data exchange but introducing data races, while each thread maintains a private program counter and call stack.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q01ProcessVsThreadExample.java"
        ```

### 2. What are the six thread lifecycle states in Java?

??? question "Reveal answer"
    `java.lang.Thread.State` defines:
    1. **NEW**: Instantiated but `start()` has not been invoked.
    2. **RUNNABLE**: Executing in the JVM or ready and waiting for OS CPU scheduling.
    3. **BLOCKED**: Waiting to acquire an intrinsic monitor lock (`synchronized`).
    4. **WAITING**: Waiting indefinitely for another thread via `Object.wait()`, `Thread.join()`, or `LockSupport.park()`.
    5. **TIMED_WAITING**: Waiting for a specified time via `Thread.sleep()`, timed `wait()`, `join()`, or `parkNanos()`.
    6. **TERMINATED**: Execution run loop completed or terminated due to an uncaught exception.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q02ThreadLifeCycleExample.java"
        ```

### 3. What is the difference between a race condition, a data race, and an atomicity violation?

??? question "Reveal answer"
    - **Race Condition**: A semantic flaw where the final program output depends on unpredictable thread execution timing or interleaving.
    - **Data Race**: A memory-level occurrence where two or more threads concurrently access the same memory location without synchronization, and at least one access is a write.
    - **Atomicity Violation**: When an operation that must execute as an indivisible unit (such as `count++` or check-then-act) gets interleaved with other thread actions.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q03RaceConditionVsDataRaceExample.java"
        ```

### 4. What does the `volatile` keyword guarantee in Java, and what does it NOT guarantee?

??? question "Reveal answer"
    - **Guaranteed**: Visibility and memory ordering. Writes to a `volatile` variable are immediately flushed to main memory and establish a happens-before relationship with subsequent reads by other threads. The compiler and CPU are prevented from reordering volatile accesses across memory barriers.
    - **NOT Guaranteed**: Atomicity for compound operations (such as `count++`, `i = i + 1`, or check-then-act sequences).

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q04VolatileGuaranteesExample.java"
        ```

### 5. How does a `synchronized` block differ from a `synchronized` method?

??? question "Reveal answer"
    A synchronized instance method implicitly locks `this`. A synchronized static method implicitly locks the class's `Class` object (`Target.class`).
    
    A synchronized block allows specifying an explicit private lock object (`synchronized(lock)`), narrowing the critical section scope, avoiding accidental lock contention with external callers, and protecting class invariants.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q05SynchronizedMonitorsExample.java"
        ```

### 6. What is the difference between `Thread.sleep()`, `Object.wait()`, and `LockSupport.park()`?

??? question "Reveal answer"
    - `Thread.sleep(ms)`: Suspends execution for a duration; does **not** release acquired monitor locks.
    - `Object.wait()`: Must be called inside a `synchronized` block on that object; releases the object's monitor and suspends the thread until `notify()` or `notifyAll()`.
    - `LockSupport.park()`: Low-level primitive that suspends the thread using permit-based mechanics without requiring monitor ownership. `unpark()` can precede `park()` without losing the permit.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q06SleepWaitParkExample.java"
        ```

### 7. How does thread interruption work in Java, and why should `InterruptedException` never be swallowed?

??? question "Reveal answer"
    Thread interruption is a cooperative signaling mechanism. Calling `thread.interrupt()` sets a boolean flag on the target thread. If the thread is blocked in a method like `sleep()`, `wait()`, or `join()`, the blocking call clears the flag and throws `InterruptedException`.
    
    Swallowing the exception without restoring the flag (`Thread.currentThread().interrupt()`) destroys the cancellation signal, preventing graceful thread pool and container shutdowns.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q07InterruptionHandlingExample.java"
        ```

### 8. What are the differences between `Runnable`, `Callable`, and `Future`?

??? question "Reveal answer"
    - `Runnable`: Single `run()` method returning `void` and unable to declare checked exceptions.
    - `Callable<V>`: Single `call()` method returning type `V` and permitted to throw checked exceptions.
    - `Future<V>`: Represents the lifecycle and eventual result of an asynchronous computation, providing methods to check completion (`isDone`), cancel (`cancel`), or block for the result with optional timeouts (`get(timeout, unit)`).

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q08RunnableCallableFutureExample.java"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 9. What is the Java Memory Model (JMM) happens-before relationship and what are its primary rules?

??? question "Reveal answer"
    The happens-before relationship guarantees that memory writes performed by one action are visible to another action without data races.
    
    Key rules include:
    1. **Program Order**: Each action in a single thread happens-before later actions in that thread.
    2. **Monitor Lock**: Releasing a lock happens-before any subsequent acquisition of the same lock.
    3. **Volatile Variable**: A write to a `volatile` field happens-before every subsequent read of that field.
    4. **Thread Start/Join**: Calling `start()` happens-before actions in the new thread; actions in a thread happen-before a caller's successful `join()`.
    5. **Transitivity**: If $A \to_{hb} B$ and $B \to_{hb} C$, then $A \to_{hb} C$.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q09HappensBeforeRulesExample.java"
        ```

### 10. How do atomic variables and Compare-And-Swap (CAS) work, and how is the ABA problem prevented?

??? question "Reveal answer"
    Atomic classes (`AtomicInteger`, `AtomicReference`) rely on CPU-level atomic instructions (`CMPXCHG`) to update memory without locking. The CPU compares the current memory value with an expected value; if equal, it swaps in the new value atomically.
    
    The **ABA problem** occurs when value $A$ changes to $B$ and back to $A$, causing a naive CAS check to succeed despite intermediate state changes. In Java, `AtomicStampedReference` solves this by pairing the reference with an integer version stamp.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q10AtomicVariablesCasExample.java"
        ```

### 11. How does `ReentrantLock` differ from intrinsic `synchronized` locks?

??? question "Reveal answer"
    `ReentrantLock` provides advanced synchronization capabilities:
    1. **Timed and Polled Acquisition**: `tryLock(timeout, unit)` prevents indefinite deadlock waiting.
    2. **Interruptible Locking**: `lockInterruptibly()` responds immediately to interruption while waiting.
    3. **Fairness**: Can enforce FIFO ordering for waiting threads.
    4. **Multiple Condition Variables**: Supports multiple `Condition` objects (`await()` / `signal()`) per lock rather than a single wait set.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q11ReentrantLockConditionExample.java"
        ```

### 12. How does `StampedLock` improve read performance over `ReentrantReadWriteLock`?

??? question "Reveal answer"
    `ReentrantReadWriteLock` read locks still perform atomic CAS operations on the shared lock state, causing cache line invalidation under high concurrent read loads.
    
    `StampedLock` provides **optimistic reading** (`tryOptimisticRead()`) which acquires no locks and performs zero write operations to memory. It returns a numeric stamp that can be validated with `validate(stamp)` after reading fields, falling back to a pessimistic read lock only if an intervening write occurred.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q12ReadWriteStampedLockExample.java"
        ```

### 13. Compare `CountDownLatch`, `CyclicBarrier`, `Semaphore`, and `Phaser`.

??? question "Reveal answer"
    - **CountDownLatch**: One-shot gate where threads wait until a counter reaches zero. Cannot be reset.
    - **CyclicBarrier**: Reusable barrier where a fixed number of threads wait for each other to reach a common barrier point before proceeding.
    - **Semaphore**: Manages a set of permits to throttle concurrent access to a shared resource pool.
    - **Phaser**: Dynamic reusable barrier that allows variable numbers of registered parties across multiple phased stages.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q13CoordinationUtilitiesExample.java"
        ```

### 14. What are the core parameters of `ThreadPoolExecutor` and how do its 4 rejection policies work?

??? question "Reveal answer"
    Parameters: `corePoolSize`, `maximumPoolSize`, `keepAliveTime`, `workQueue`, `threadFactory`, `rejectedExecutionHandler`.
    
    When pool + queue are saturated:
    - `AbortPolicy`: Throws `RejectedExecutionException`.
    - `CallerRunsPolicy`: Runs the task on the submitting caller thread, providing automatic backpressure.
    - `DiscardPolicy`: Drops task silently.
    - `DiscardOldestPolicy`: Discards the oldest unhandled task from the queue head.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q14ThreadPoolRejectionPoliciesExample.java"
        ```

### 15. How is `ConcurrentHashMap` implemented internally in modern Java?

??? question "Reveal answer"
    Modern `ConcurrentHashMap` uses **fine-grained bucket-level synchronization**:
    - Empty buckets are populated using lock-free `CAS` (`compareAndSetObject`).
    - Occupied buckets synchronize exclusively on the head node of that single bucket list or red-black `TreeBin`.
    - Reads are completely lock-free via `volatile` node `val` and `next` pointers.
    - Resizing is distributed across concurrent writer threads using `ForwardingNode` references.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q15ConcurrentHashMapInternalsExample.java"
        ```

### 16. How do `thenApply`, `thenCompose`, and `thenCombine` differ in `CompletableFuture`?

??? question "Reveal answer"
    - `thenApply(fn)`: Synchronous mapping transformation ($T \to U$).
    - `thenCompose(fn)`: Asynchronous monadic flattening ($T \to CompletableFuture<U>$), preventing nested `CompletableFuture<CompletableFuture<U>>`.
    - `thenCombine(other, biFn)`: Runs two independent futures in parallel and combines their results ($T, U \to V$).

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q16CompletableFutureCompositionExample.java"
        ```

### 24. What is Structured Concurrency and how does it prevent thread leaks during subtask failure?

??? question "Reveal answer"

    **Short Answer:** Structured concurrency treats concurrent tasks executed in different threads as a single atomic unit of work, ensuring subtask lifetimes are strictly bounded by lexical code blocks and cancelling orphan sibling tasks when any subtask fails.

    **Internal Mechanism:** With structured task scopes (e.g. `StructuredTaskScope.ShutdownOnFailure`), failure of a child task automatically triggers cancellation (interruption) of all other concurrent children in the scope, ensuring no rogue background threads outlive the enclosing method.

    **Common Mistake:** Spawning unbounded uncoordinated threads using `CompletableFuture.runAsync` or `executor.submit` without cancellation handles, leaving orphaned threads running and leaking database connections or CPU cycles after a request times out. [Concepts](/topics/concurrency/concepts.md#7-java-21-virtual-threads-structured-concurrency)

    ??? example "Example"

        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q24StructuredConcurrencyExample.java"
        ```

### 25. How do VarHandle memory access modes (plain, opaque, acquire/release, volatile) differ?

??? question "Reveal answer"

    **Short Answer:** `VarHandle` offers fine-grained control over hardware memory barriers: `plain` has no memory fences; `opaque` guarantees coherence without ordering; `acquire/release` enforces one-way ordering; `volatile` enforces bidirectional global memory fences.

    **Internal Mechanism:** `setRelease` prevents prior memory writes from being reordered after the store; `getAcquire` prevents subsequent memory reads from being reordered before the load. This allows lock-free message passing with significantly fewer CPU pipeline stalls than full volatile barriers.

    **Common Mistake:** Using full volatile reads and writes when only unidirectional one-way release/acquire synchronization is required, incurring unnecessary memory bus invalidation on ARM/x86 architectures. [Concepts](/topics/concurrency/concepts.md#3-java-memory-model-jmm-happens-before)

    ??? example "Example"

        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q25VarHandleAccessModesExample.java"
        ```

<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 17. What are the four Coffman conditions for deadlock and how are they prevented?

??? question "Reveal answer"
    The four conditions are:
    1. **Mutual Exclusion**: Resources cannot be shared.
    2. **Hold and Wait**: A thread holds resources while waiting for others.
    3. **No Preemption**: Resources cannot be forcibly taken away.
    4. **Circular Wait**: A closed chain of threads exists where each waits for a resource held by the next.

    Prevention strategies include enforcing a **strict canonical global lock order** across all resources (breaking Circular Wait) or using timed `tryLock` with exponential backoff (breaking Hold and Wait).

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q17DeadlockPreventionStrategiesExample.java"
        ```

### 18. How does `ForkJoinPool` work-stealing differ from standard `ThreadPoolExecutor` task scheduling?

??? question "Reveal answer"
    Standard `ThreadPoolExecutor` uses a single shared blocking queue from which all workers compete to dequeue tasks.
    
    `ForkJoinPool` assigns each worker thread its own double-ended queue (`WorkQueue`). Workers push/pop sub-tasks from their own deque's tail in LIFO order (maximizing cache warmth). When a worker runs out of work, it steals tasks from the head of another worker's deque in FIFO order, drastically reducing lock contention.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q18ForkJoinWorkStealingExample.java"
        ```

### 19. How do Java 21 Virtual Threads work, and what causes carrier thread pinning?

??? question "Reveal answer"
    Virtual threads are lightweight, user-mode threads managed by the JVM runtime and scheduled onto a small pool of OS carrier threads. When a virtual thread executes blocking I/O, the JVM unmounts its call stack to the heap, freeing the carrier thread.
    
    **Pinning** occurs when a virtual thread enters a `synchronized` block or executes native JNI methods. In Java 21, the carrier thread cannot unmount during blocking calls inside `synchronized` blocks. To avoid pinning in I/O-heavy paths, replace `synchronized` with `ReentrantLock`.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q19VirtualThreadPinningExample.java"
        ```

### 20. Why are `ThreadLocal` variables hazardous with virtual threads and thread pools, and how does `ScopedValue` address this?

??? question "Reveal answer"
    In pooled thread environments, `ThreadLocal` values persist across reused tasks, leading to memory leaks and cross-request security pollution unless explicitly cleaned with `remove()`. With millions of virtual threads, `ThreadLocal` memory overhead scales poorly.
    
    `ScopedValue` (introduced in modern Java) provides immutable, bounded context propagation that is strictly bound to the lexical execution scope of a method and cleaned up automatically upon exit.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q20ScopedValueStructuredConcurrencyExample.java"
        ```

### 21. What constitutes safe publication of shared objects in the Java Memory Model?

??? question "Reveal answer"
    Safe publication guarantees that an object's initialized state is fully visible to other threads without seeing partially initialized fields.
    
    Safe publication techniques:
    1. Initializing reference in a static initializer block.
    2. Storing the reference in a `volatile` field or `AtomicReference`.
    3. Making all fields `final` inside a properly constructed object (where `this` does not escape the constructor).
    4. Storing the reference in a thread-safe collection (such as `ConcurrentHashMap` or `BlockingQueue`).

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q21SafePublicationPatternsExample.java"
        ```

### 26. How do lock-free data structures prevent the ABA problem using versioned atomic references?

??? question "Reveal answer"

    **Short Answer:** The ABA problem occurs in CAS loops when a memory address changes from value A to B and back to A; CAS succeeds despite intervening structural modifications. It is resolved using monotonic versioning via `AtomicStampedReference`.

    **Deep Explanation:** In a lock-free Treiber stack, if thread 1 reads top $A$ (pointing to $B$), pauses, and thread 2 pops $A$, pops $B$, and pushes $A$ back onto the stack, thread 1's CAS $(A \to B)$ succeeds. However, node $B$ was already unlinked and may contain recycled garbage, corrupting the stack structure.

    **Internal Mechanism:** `AtomicStampedReference` pairs the memory reference with an integer stamp (version number); both must match atomically via CAS for the update to succeed.

    **Example:** [Lock-free Treiber stack](/topics/concurrency/concepts.md#4-synchronization-lock-free-primitives).

    **Common Mistake:** Assuming raw CAS on object identity (`AtomicReference`) is sufficient for dynamic linked-node data structures with object recycling.

    **Production Consideration:** Use standard Java concurrent collections (`ConcurrentLinkedQueue`) which utilize garbage collection and unlinking markers, avoiding manual ABA management.

    **Follow-up Questions:**
    - How does optimistic concurrency control in JPA mirror ABA prevention through row versioning? See [JPA / Hibernate: Optimistic vs Pessimistic Locking](/topics/jpa-hibernate/questions.md#15-how-do-optimistic-locking-and-pessimistic-locking-differ-in-jpa)
    - How does Java garbage collection eliminate pointer recycling ABA hazards compared to manual C/C++ memory management? See [JVM: GC Reachability](/topics/jvm/questions.md#7-what-is-a-garbage-collection-root-and-object-reachability)

    ??? example "Example"

        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q26LockFreeStackAbaExample.java"
        ```

### 27. What causes virtual thread carrier pinning and why does ReentrantLock avoid it?

??? question "Reveal answer"

    **Short Answer:** Virtual threads mount on OS platform carrier threads. Blocking inside a `synchronized` block/method or native call pins the virtual thread to its carrier, preventing other virtual threads from executing on that worker. `ReentrantLock` uses `LockSupport.park()`, allowing the virtual thread to cleanly unmount.

    **Deep Explanation:** HotSpot's object monitor implementation (`synchronized`) ties thread state to native OS thread control blocks. When a virtual thread blocks on an intrinsic lock monitor, HotSpot cannot detach the continuation from the carrier stack. In contrast, `ReentrantLock` uses `AbstractQueuedSynchronizer` and Java-level virtual thread parking.

    **Internal Mechanism:** When `LockSupport.park()` is called on a virtual thread, HotSpot captures its call frame into the heap continuation and unmounts it from the `ForkJoinPool` carrier thread, freeing the carrier to execute other virtual threads.

    **Example:** [Virtual thread carrier pinning](/topics/concurrency/concepts.md#7-java-21-virtual-threads-structured-concurrency).

    **Common Mistake:** Migrating an I/O-intensive legacy codebase to virtual threads without replacing `synchronized` database connection blocks or JDBC driver internal monitors.

    **Production Consideration:** Detect carrier pinning by running with `-Djdk.tracePinnedThreads=full` and replace synchronized critical sections with `ReentrantLock` or `StampedLock`.

    **Follow-up Questions:**
    - How does prolonged carrier pinning exhaust HikariCP database connection pool availability? See [Spring Transactions: Connection Pool Starvation](/topics/spring-transactions/questions.md#3-compare-propagationrequired-and-propagationrequires_new-what-are-the-connection-pool-implications-of-each)
    - How do database driver connection timeouts interact with virtual thread unmounting? See [Database / SQL: Connection Timeouts](/topics/database-sql/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q27VirtualThreadCarrierPinningExample.java"
        ```

### 28. Why does LongAdder outperform AtomicLong under high write contention across multiple CPU sockets?

??? question "Reveal answer"

    **Short Answer:** `AtomicLong` forces all threads to CAS-update a single 64-bit memory address, causing CPU cache-line bouncing (false sharing). `LongAdder` maintains a dynamically scaled array of cache-line padded `Cell`s, distributing updates across distinct cores.

    **Deep Explanation:** On multi-core SMP/NUMA systems, when a core modifies a shared volatile address, hardware MESI cache coherency protocols invalidate the L1/L2 cache lines of all other cores. With dozens of threads incrementing an `AtomicLong`, CPU cycles are wasted spinning and refetching cache lines over the bus.

    **Internal Mechanism:** `LongAdder` extends `Striped64`. Each thread hashes its thread ID to a separate `Cell` slot. Under thread contention, the array resizes up to the number of CPU cores, ensuring zero false sharing.

    **Example:** [LongAdder striped contention](/topics/concurrency/concepts.md#4-synchronization-lock-free-primitives).

    **Common Mistake:** Using `LongAdder` when an atomic read-modify-write operation (like `compareAndSet` or exact instantaneous monotonic sequencing) is required; `LongAdder.sum()` returns an eventually consistent snapshot.

    **Production Consideration:** Default to `LongAdder` for metrics, telemetry counters, and request rate tracking; reserve `AtomicLong` for sequence generators and lock-free state machines.

    **Follow-up Questions:**
    - How does hardware cache-line padding and `@Contended` prevent false sharing at the byte level? See [Core Java: False Sharing Padding](/topics/core-java/questions.md#8-high-frequency-counter-updates-on-multi-socket-servers-cause-heavy-cpu-pipeline-stalls-diagnose-and-redesign-it)
    - What Micrometer counter implementations utilize LongAdder for thread-safe throughput tracking? See [Observability: Metrics and Counters](/topics/observability/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q28LongAdderCellContentionExample.java"
        ```

<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenarios

### 22. Production incident: A thread pool experiences self-deadlock when parent tasks block on subtasks. How do you diagnose and resolve it?

??? question "Reveal answer"
    **Symptom**: Under load, application latency spikes to infinite timeouts, active worker count equals pool maximum, but CPU utilization is 0%.
    
    **Root Cause**: Parent tasks submit child tasks to the same bounded `ThreadPoolExecutor` and block waiting via `future.get()`. When parent tasks consume all core/max worker threads, no worker remains to pick up the child tasks from the queue, causing a permanent circular wait.
    
    **Fix**:
    1. Decouple thread pools: Use dedicated executors for parent orchestrations and child subtasks.
    2. Use non-blocking asynchronous composition (`CompletableFuture.allOf`) instead of synchronous `.get()` / `.join()`.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q22ThreadPoolSelfDeadlockScenarioExample.java"
        ```

### 23. Production incident: Enabling virtual threads in a Spring Boot application causes throughput collapse due to carrier pinning. How do you detect and fix it?

??? question "Reveal answer"
    **Symptom**: Enabling `spring.threads.virtual.enabled=true` fails to scale concurrent HTTP requests; carrier threads become exhausted and response latency degrades.
    
    **Diagnosis**: Run the JVM with `-Djdk.tracePinnedThreads=full` or inspect Java Flight Recorder (JFR) `jdk.VirtualThreadPinned` events to find call stacks blocking inside `synchronized` monitors.
    
    **Fix**: Refactor third-party drivers or application locking from `synchronized` methods/blocks to `ReentrantLock`.

    ??? example "Example"
        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q23VirtualThreadPinningScenarioExample.java"
        ```

### 29. Production incident: Single-pool task delegation causes thread pool starvation deadlock

??? question "Reveal answer"

    **Short Answer:** Submitting dependent subtasks to the same bounded thread pool that executes their parent tasks causes self-deadlock when parent tasks occupy all worker threads while waiting on queued subtasks.

    **Deep Explanation:** When high concurrency arrives, parent tasks are scheduled onto all available worker threads in the pool. Each parent then submits child tasks into the pool's work queue and invokes `future.get()` or `future.join()`. Because all worker threads are parked waiting for results, no thread is free to dequeue and run the child tasks, creating a permanent deadlock with 0% CPU utilization.

    **Internal Mechanism:** Circular wait condition on worker thread availability in a single shared bounded queue.

    **Example:** [Thread pool self-deadlock](/topics/concurrency/code-review.md).

    **Common Mistake:** Sizing the thread pool larger without isolating parent and child execution pipelines; higher thread counts merely delay deadlock until traffic spikes.

    **Production Consideration:** Use separate, dedicated thread pools for orchestration tasks versus blocking subtasks, or use asynchronous non-blocking composition (`CompletableFuture.thenCompose`) which never blocks worker threads during subtask execution.

    **Follow-up Questions:**
    - How does Spring's `@Async` annotation configure separate task executors to avoid pool starvation? See [Spring Core: Asynchronous Execution](/topics/spring-core/questions.md)
    - How does the Bulkhead pattern isolate downstream call dependencies from crashing upstream services? See [Resilience: Bulkhead Pattern](/topics/resilience/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q29ThreadPoolStarvationDeadlockScenarioExample.java"
        ```

### 30. Production incident: MDC trace context and SecurityContext silently lost across CompletableFuture pipeline stages

??? question "Reveal answer"

    **Short Answer:** Asynchronous tasks dispatched to standard `ForkJoinPool` or `ExecutorService` run on worker threads that do not inherit `ThreadLocal` context from the request-handling thread, stripping correlation IDs and authentication tokens.

    **Deep Explanation:** In Spring Boot / servlet architectures, request tracing (`MDC`) and authentication (`SecurityContextHolder`) are stored in `ThreadLocal` variables. When application logic calls `CompletableFuture.supplyAsync()` or switches execution pools, worker threads have empty `ThreadLocal` maps. Logs become un-correlated, and downstream secured method calls throw `AccessDeniedException` or operate as anonymous users.

    **Internal Mechanism:** `ThreadLocal` storage is isolated per OS thread; standard task dispatch does not copy or propagate thread-local values across execution boundaries.

    **Example:** [Async context propagation](/topics/concurrency/code-review.md).

    **Common Mistake:** Using `InheritableThreadLocal`, which only copies context at thread *creation* time, failing completely with pooled reusable threads.

    **Production Consideration:** Use Micrometer Observation / Context Propagation library, or wrap executor services with task decorators (`ContextPropagatingTaskDecorator`) that snapshot context on submission and restore/clean it up on worker threads.

    **Follow-up Questions:**
    - How does Spring Security configure `DelegatingSecurityContextAsyncTaskExecutor` for context handover? See [Spring Security: Asynchronous Context Propagation](/topics/spring-security/questions.md)
    - How do OpenTelemetry and W3C TraceContext standards structure correlation identifiers across microservice boundaries? See [Observability: Distributed Tracing](/topics/observability/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q30AsyncContextPropagationScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->

## Related

- [Concurrency Concepts](concepts.md)
- [Concurrency Internals](internals.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
