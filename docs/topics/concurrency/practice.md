# Concurrency — Practice Q&A

Senior-depth practice from interview-prep §2, §3, §15, and §17. Case-study policies are not lab facts.
Use [Internals](internals.md) and [Solutions](solutions.md) to connect these answers to runnable examples.

### Q1. How do race conditions, visibility failures, and atomicity violations differ?

Canonical: [Concurrency failures](questions.md#3-what-is-the-difference-between-a-race-condition-a-data-race-and-an-atomicity-violation)

??? question "Reveal answer"
    - **Choose synchronization according to the invariant, not the symptom's name.**
    - **Mechanism:** a race depends on interleaving; a data race involves conflicting unordered accesses; an atomicity violation splits an indivisible business action.
    - **Failure:** a visible counter still loses increments. A JVM lock cannot serialize writes from another pod.
    - **Fix:** volatile for independent flags, atomic operations for one value, locks for compound state, database constraints for shared persistent invariants.
    - **Verify:** coordinate competing operations with barriers and assert final invariants; absence of failure in stress tests is not proof.
    - **Follow-up:** Can individually thread-safe methods compose into a racy check-then-act?
    - **Takeaway:** Synchronize the complete decision and effect.

### Q2. What does happens-before guarantee, including final-field initialization?

Canonical: [JMM rules](questions.md#9-what-is-the-java-memory-model-jmm-happens-before-relationship-and-what-are-its-primary-rules)

??? question "Reveal answer"
    - **Happens-before defines observable ordering and visibility, not wall-clock execution order.**
    - **Mechanism:** monitor unlock/lock, volatile write/read, start/join, and executor submission/result retrieval create defined edges that compose transitively.
    - **Failure:** an ordinary reference assignment does not safely publish arbitrary mutable state. Final-field initialization safety is a special construction guarantee, not a general constructor-to-reader happens-before edge.
    - **Fix:** avoid constructor escape; publish through volatile references, locks, initialization, or concurrent containers. Later mutations need their own synchronization.
    - **Trade-off:** immutable values reduce synchronization; mutable graphs require a documented ownership protocol.
    - **Follow-up:** Why is double-checked locking invalid without volatile?
    - **Takeaway:** Explain the publication path, not just the field modifier.

### Q3. Which lock should protect a read-heavy shared structure?

??? question "Reveal answer"
    - **Start with simple locking; adopt specialized locks only with measured benefit.**
    - **Mechanism:** synchronized is reentrant and scoped; `ReentrantLock` adds interruptible/timed acquisition and conditions. Read/write locks permit concurrent readers; `StampedLock` adds validated optimistic reads.
    - **Failure:** optimistic reads can observe inconsistent temporary state; non-reentrant stamped locking can self-deadlock. Fairness changes throughput and does not solve every starvation problem.
    - **Fix:** copy values into locals, validate the stamp, and retry under a read lock before using results. Unlock explicit locks in finally.
    - **Production:** on Java 21, blocking within synchronized can pin virtual-thread carriers; do not generalize that behavior to all later JDKs.
    - **Follow-up:** When would immutable snapshot replacement outperform a read/write lock?
    - **Takeaway:** Complexity must buy demonstrated contention reduction.

### Q4. CAS, `AtomicLong`, `LongAdder`, or an atomic snapshot?

??? question "Reveal answer"
    - **Use exact atomics for decisions, striped adders for statistics, and one snapshot for coupled state.**
    - **Mechanism:** CAS retries if an expected value changed; `LongAdder` spreads writes, but `sum` is not an atomic snapshot during updates.
    - **Failure:** separately updated healthy/lag fields can expose a combination that never existed. ABA matters when intervening changes invalidate a protocol, not in every counter.
    - **Fix:** publish related fields as one immutable record through `AtomicReference`; use versions/stamps where ABA matters.
    - **Trade-off:** retry loops waste CPU under contention; locks may perform better. Record failed retries and profile before assuming lock-free means fast.
    - **Follow-up:** Why is `LongAdder` inappropriate for enforcing an exact admission limit?
    - **Takeaway:** Match observation consistency to business correctness.

### Q5. How do you size executors and choose a rejection policy?

??? question "Reveal answer"
    - **Bound work by CPU, downstream capacity, memory, and deadline—not thread count alone.**
    - **Mechanism:** CPU work needs roughly available cores; I/O concurrency depends on waiting time. Little's law uses mean throughput and time in a stable system.
    - **Failure:** unbounded queues accumulate expired work; CallerRuns on a servlet, event-loop, or scheduler thread can block critical progress.
    - **Fix:** use bounded queues, explicit rejection mapped to a documented response, and bulkheads per dependency. Remove canceled work where supported.
    - **Verify:** measure queue wait, execution time, rejections, and downstream saturation under overload. Size for container CPU entitlement, not host cores alone.
    - **Follow-up:** What happens when parent tasks wait for children submitted to the same saturated pool?
    - **Takeaway:** A queue is a latency and memory commitment.

### Q6. How do you compose parallel calls with a real deadline?

Merged from: §3 CompletableFuture + §17.8

??? question "Reveal answer"
    - **Compose results asynchronously, but enforce deadlines in the actual clients too.**
    - **Mechanism:** `thenApply` transforms; `thenCompose` flattens; `thenCombine` joins independent results. Async variants need an explicit executor for blocking work.
    - **Failure:** `orTimeout` or `completeOnTimeout` completes a future without stopping underlying I/O. `allOf` is not fail-fast cancellation of siblings.
    - **Fix:** pass a remaining request budget into HTTP/JDBC calls, cap concurrency, classify fallback eligibility, and propagate cancellation where the client supports it.
    - **Production:** own executor shutdown; preserve interruption; unwrap errors without assuming every callback receives the same wrapper shape. Do not shut down a shared executor per request.
    - **Follow-up:** How do late results avoid mutating state after the caller has timed out?
    - **Takeaway:** A future timeout is not a resource-release guarantee.

### Q7. How do you detect and prevent deadlocks, livelocks, and starvation?

??? question "Reveal answer"
    - **Identify the wait graph and remove a cycle or an unfair resource dependency.**
    - **Mechanism:** deadlock requires mutual exclusion, hold-and-wait, no preemption, and circular wait. Livelock makes activity without progress; starvation denies one actor service.
    - **Diagnosis:** compare repeated thread dumps, owners, queues, and database lock waits. JVM deadlock detection does not cover every executor or distributed wait cycle.
    - **Fix:** establish lock ordering, reduce critical sections, use timed interruptible acquisition, and avoid nested pool dependencies.
    - **Trade-off:** timeout-and-retry requires bounded jitter and safe rollback; otherwise it merely converts deadlock into livelock or overload.
    - **Follow-up:** Why must PostgreSQL deadlock retries restart the complete transaction?
    - **Takeaway:** Count completed work, not merely active threads.

### Q8. How do ThreadLocal and MDC leak across requests or disappear across async work?

??? question "Reveal answer"
    - **Thread association is not request association; propagate and restore context explicitly.**
    - **Mechanism:** pooled threads outlive requests; ordinary ThreadLocals do not follow arbitrary executor or CompletableFuture boundaries.
    - **Failure:** missing cleanup exposes another tenant's context; capturing entire requests retains memory. Copying only MDC does not propagate transactions or authorization safely.
    - **Fix:** use framework-supported context snapshots/decorators, capture only necessary values, and restore prior context in finally rather than blindly clearing nested callers' state.
    - **Verify:** alternate principals and correlation IDs on reused threads, including exceptional completion. Never transplant a JDBC transaction to another thread.
    - **Follow-up:** What are the costs of thousands of virtual-thread-local instances?
    - **Takeaway:** Context needs an owner, scope, and cleanup path.

### Q9. When should a Java 21 service adopt virtual threads?

Merged from: §3 virtual threads + §15 virtual-thread follow-up

??? question "Reveal answer"
    - **Use them for blocking concurrency after qualifying the whole dependency path.**
    - **Mechanism:** many virtual threads share carrier threads; supported blocking operations can unmount them. CPU work still requires CPU time.
    - **Failure:** unbounded request concurrency overloads a small connection pool. Java 21 synchronized/native pinning and large per-thread context can erase benefits.
    - **Fix:** use per-task threads, not virtual-thread pools; bound downstream calls with admission control and deadlines. Measure JFR pinning events, throughput, tail latency, memory, and shutdown.
    - **Trade-off:** reactive code can also coexist with JDBC through explicit isolation; it is not inherently incompatible with outbox SQL, but adds programming-model complexity.
    - **Follow-up:** Which qualification results would justify retaining platform threads?
    - **Takeaway:** Cheaper waiting does not create downstream capacity.

### Q10. What does modern `ConcurrentHashMap` guarantee?

??? question "Reveal answer"
    - **It provides safe concurrent per-key operations, not transactions across entries.**
    - **Mechanism:** modern OpenJDK combines CAS, bin-level coordination, volatile publication, and cooperative resize; it is not the old fixed segment design.
    - **Failure:** slow mapping functions block colliding updates; recursive map mutation can fail. Iteration and aggregate size observations are not a globally consistent snapshot.
    - **Fix:** keep compute functions short and side-effect limited; avoid remote I/O inside them. Use a separate lock or immutable aggregate for multi-key invariants.
    - **Trade-off:** null exclusion distinguishes absence cleanly; contention still depends on key distribution. Read the API contract rather than relying on a particular lock implementation.
    - **Follow-up:** How would you deduplicate a slow load without holding map computation open?
    - **Takeaway:** Concurrent containers do not make workflows atomic.

### Q11. How does the initialization-on-demand holder implement a lazy singleton?

??? question "Reveal answer"
    - **Class initialization safely constructs the holder's instance on first active use.**
    - **Mechanism:** JVM initialization serializes initialization of the defining class; other threads observe its completed static initialization.
    - **Failure:** there is one instance per defining class loader, not one globally; mutable singleton behavior still races. Initialization failure can leave the class unusable.
    - **Fix:** use a holder for a truly process-local immutable utility; use container-managed lifecycle for external clients needing configuration, shutdown, or replacement.
    - **Trade-off:** static lifetime simplifies access but complicates testing and resource ownership; Spring singletons are per application context.
    - **Follow-up:** How would you test startup failure and close the client predictably?
    - **Takeaway:** Safe construction does not imply safe lifetime management.

### Q12. What makes a bounded producer–consumer pipeline production-safe?

??? question "Reveal answer"
    - **Bound admission, define rejection, and make shutdown and delivery guarantees explicit.**
    - **Mechanism:** timed `offer` limits producer waiting; consumers poll or block interruptibly. Capacity bounds queued entries, not their individual byte sizes.
    - **Failure:** memory queues lose accepted items on process death; an interrupt can abandon partially handled work; producers can race shutdown.
    - **Fix:** define when ownership transfers, handle failed offers, stop admission before draining, and use durable storage for business-critical accepted work.
    - **Verify:** test full queues, slow consumers, interruption, handler failure, and draining within a deadline. Record queue age, not only depth.
    - **Follow-up:** When is a poison-pill shutdown unsafe with several producers?
    - **Takeaway:** Backpressure and durability are separate properties.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Solutions](solutions.md)
- [Core Java practice](../core-java/practice.md)
- [JVM practice](../jvm/practice.md)
