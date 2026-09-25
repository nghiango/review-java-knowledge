# Interview Questions: Java 25 & Spring Boot 4 What's New

!!! info "Delta from baseline"
    Baseline modules test core Java 21 semantics and Spring Boot 3.5 architectures.
    These 23 questions evaluate your mastery of **Java 22 → 25** runtime capabilities, **Spring Boot 4.0 / Spring Framework 7** architectural changes, and real-world upgrade migration failure modes.

---

### 1. What does `Stream::gather` add that `map` and `filter` cannot do?

??? question "Reveal answer"

    **Short Answer:** `Stream::gather` introduces intermediate, stateful stream transformations with arbitrary cardinality ($M \to N$), whereas `map` is strictly $1 \to 1$ and `filter` is $1 \to \{0, 1\}$. It allows windowing, chunking, and cumulative reductions while maintaining stream pipeline laziness.

    **Internal Mechanism:** A `Gatherer` operates with an initializer (creates intermediate accumulator state), an integrator (evaluates elements and decides whether to push to downstream), a combiner (merging states for parallel pipelines), and a finisher (emitting remaining buffered elements upon stream completion).

    **Common Mistake:** Attempting to implement batching or moving averages in Java 21 by accumulating elements into an external mutable list inside `map()` or `forEach()`, which causes catastrophic race conditions and corrupted batches in parallel streams.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q01StreamGatherersWhatExample.java"
        ```

---

### 2. What is the purpose of unnamed variables and patterns (`_`) in Java 25?

??? question "Reveal answer"

    **Short Answer:** Unnamed variables (`_`) explicitly signal to both the compiler and human reviewers that a variable, exception parameter, or pattern component is intentionally unused. This suppresses unused-variable compiler warnings and prevents accidental usage.

    **Internal Mechanism:** The identifier `_` is a keyword representing an unnamed variable. The compiler verifies that `_` cannot be read or referenced in subsequent statements, and emits bytecode that elides local variable table slot naming where appropriate.

    **Common Mistake:** Naming unused variables `ignored` or `dummy`, which fails to enforce at compile time that the value is not accidentally accessed later in the block.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q02UnnamedVariablesExample.java"
        ```

---

### 3. What does the Foreign Function & Memory (FFM) API replace in backend architectures?

??? question "Reveal answer"

    **Short Answer:** The FFM API (`java.lang.foreign`) safely replaces `sun.misc.Unsafe` off-heap memory management and complex JNI native bindings. It provides deterministic deallocation via `Arena` and memory bounds checking via `MemorySegment`.

    **Internal Mechanism:** Native memory allocations are backed by `Arena` scopes. Accesses through `MemorySegment` execute intrinsic JVM bounds and liveness checks, throwing `IndexOutOfBoundsException` or `IllegalStateException` rather than causing fatal segmentation faults or memory corruption.

    **Common Mistake:** Relying on `sun.misc.Unsafe.allocateMemory()` without bounds checking, which causes silent heap corruption and SIGSEGV process crashes when indices exceed buffer limits.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q03ForeignMemoryArenaExample.java"
        ```

---

### 4. What happens to `System.getSecurityManager()` on Java 25?

??? question "Reveal answer"

    **Short Answer:** On Java 25, `SecurityManager` is permanently disabled and cannot be enabled (JEP 486). `System.getSecurityManager()` always returns `null`, and `AccessController.checkPermission()` is a no-op.

    **Internal Mechanism:** HotSpot removed all internal hooks and permissions verification code. Any JVM launched with `-Djava.security.manager` logs an error or refuses startup, and all standard library checks bypass permission lookups completely.

    **Common Mistake:** Writing fallback authorization logic that assumes `if (System.getSecurityManager() == null) return true;`, creating a total fail-open security bypass on Java 25.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q04SecurityManagerDisabledExample.java"
        ```

---

### 5. What are Scoped Values and how do they improve on `ThreadLocal`?

??? question "Reveal answer"

    **Short Answer:** Scoped Values (`ScopedValue`) provide immutable, stack-confined data sharing with child tasks and virtual threads. They eliminate the memory leaks, mutable state hazards, and heavy map-cloning overhead of `ThreadLocal`.

    **Internal Mechanism:** Bindings established via `ScopedValue.where(KEY, value).run(...)` are stored in an internal one-way linked list on the execution stack. Virtual threads forked within structured scopes inherit these bindings in $O(1)$ time with zero heap cloning.

    **Common Mistake:** Using `InheritableThreadLocal` across millions of virtual threads, which creates millions of heap map instances and triggers premature OutOfMemoryErrors.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q05ScopedValuesExample.java"
        ```

---

### 6. What do flexible constructor bodies allow in Java 25?

??? question "Reveal answer"

    **Short Answer:** Flexible constructor bodies (JEP 482) permit statements to appear before an explicit `super(...)` or `this(...)` constructor call, provided those statements do not reference the uninitialized instance (`this`).

    **Internal Mechanism:** The Java compiler divides constructor execution into a pre-construction prologue and the post-construction body. The prologue allows defensive parameter validation, argument normalization, and static utility invocation before passing arguments to the parent constructor.

    **Common Mistake:** Spilling pre-construction validation logic into static helper methods or awkward ternary expressions within the `super(...)` call arguments because of the historical "call must be first" restriction.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q06FlexibleConstructorBodiesExample.java"
        ```

---

### 7. Why do `sun.misc.Unsafe` memory methods emit warnings in Java 25?

??? question "Reveal answer"

    **Short Answer:** Under JEP 498, all off-heap memory-access methods in `sun.misc.Unsafe` (`allocateMemory`, `putLong`, `getLong`, `freeMemory`) are deprecated for removal to force migration to the safe Foreign Function & Memory API.

    **Internal Mechanism:** The JVM prints warnings to standard error whenever an `Unsafe` memory-access method is invoked. A command-line option (`--sun-misc-unsafe-memory-access=deny`) allows teams to simulate complete removal and catch offending libraries before production upgrades.

    **Common Mistake:** Suppressing the JVM warnings without planning a migration to `java.lang.foreign.Arena`, leaving services vulnerable to immediate startup failures when a future JDK turns warnings into hard exceptions.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q07UnsafeMemoryDeprecationExample.java"
        ```

---

### 8. What is a compact source file and instance `main` method?

??? question "Reveal answer"

    **Short Answer:** JEP 512 enables instance main methods (`void main()`) and implicitly declared classes, allowing runnable Java programs and utilities without requiring `public static void main(String[] args)` or explicit enclosing class declarations.

    **Internal Mechanism:** The `java` launcher detects classes lacking `public static void main` and searches for instance-level `main()` or `main(String[])` methods, automatically invoking a default zero-argument constructor to execute the entry point.

    **Common Mistake:** Assuming instance `main` is only for beginners; it is widely utilized for lightweight scripting, CLI utilities, and single-file integration test harnesses in modern builds.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q08CompactSourceMainExample.java"
        ```

---

### 9. How do `windowFixed`, `windowSliding`, `scan`, and `fold` differ in Stream Gatherers?

??? question "Reveal answer"

    **Short Answer:** `windowFixed` produces non-overlapping batches; `windowSliding` produces overlapping windows advancing one element at a time; `scan` emits running cumulative aggregates at each step; `fold` produces a single aggregated value as an intermediate stream element.

    **Internal Mechanism:** Each gatherer uses an internal state accumulator: `windowFixed` clears its buffer upon reaching capacity; `windowSliding` evicts the oldest element while retaining subsequent items; `scan` updates an accumulator and immediately emits it downstream on each step.

    **Common Mistake:** Using `windowSliding` when batching database inserts, which mistakenly generates redundant overlapping mutations instead of partitioned discrete batches.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q09StreamGatherersWindowingExample.java"
        ```

---

### 10. How does an `Arena` bound the lifetime of native off-heap memory?

??? question "Reveal answer"

    **Short Answer:** An `Arena` acts as a deterministic lifetime scope for native memory. When `arena.close()` is called (typically via try-with-resources), all native allocations allocated within that arena are released immediately in one bulk operation.

    **Internal Mechanism:** The `Arena` tracks all allocated segments under a shared `MemoryScope`. Upon closing, the scope transitions to `CLOSED` and calls the underlying native `free()`. Any subsequent dereference through a previously allocated `MemorySegment` throws `IllegalStateException`.

    **Common Mistake:** Sharing a single unbounded `Arena.ofAuto()` across the application lifetime for temporary high-throughput buffers, effectively re-introducing garbage collection dependency for native memory reclamation.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q10ArenaLifetimeExample.java"
        ```

---

### 11. Does `synchronized` still pin virtual threads to carrier threads on Java 25?

??? question "Reveal answer"

    **Short Answer:** No. In Java 25, HotSpot's `ObjectMonitor` implementation unmounts virtual threads when they block on monitor contention or wait/notify, freeing the carrier platform thread to execute other virtual tasks.

    **Internal Mechanism:** When a virtual thread encounters contention in `synchronized`, the JVM parks the Loom continuation and detaches the virtual thread from the `ForkJoinPool` worker. Once unlocked, the virtual thread is placed back onto the carrier run queue.

    **Common Mistake:** Continuing to replace simple `synchronized` blocks with `ReentrantLock` under the obsolete Java 21 assumption that `synchronized` causes carrier starvation.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q11SynchronizedUnpinningExample.java"
        ```

---

### 12. What changed regarding Garbage Collection defaults in Java 25?

??? question "Reveal answer"

    **Short Answer:** Generational ZGC is now the default mode when `-XX:+UseZGC` is specified (JEP 474). Generational collection separates young and old objects, dramatically reducing memory allocation stall risks without sacrificing sub-millisecond pause times.

    **Internal Mechanism:** HotSpot allocates young and old generations into separate colored-pointer page regions. Generational barrier checks intercept thread mutations with minimal CPU overhead, allowing concurrent young-gen evacuation while mature objects remain untouched.

    **Common Mistake:** Passing `-XX:+ZGenerational` on modern JDKs where the non-generational ZGC has been deprecated or removed, leading to startup warnings or configuration conflicts.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q12GcDefaultsExample.java"
        ```

---

### 13. How does JSpecify `@NullMarked` change a public API contract in Spring Boot 4?

??? question "Reveal answer"

    **Short Answer:** `@NullMarked` sets a package- or class-wide contract where all method parameters, return values, and type arguments are non-null by default. Nullable fields or parameters must be explicitly opted into using `@Nullable`.

    **Internal Mechanism:** Static analysis checkers (e.g. NullAway, ErrorProne, IDE linters) inspect the bytecode annotations. Any unannotated parameter receiving a potentially null value is flagged as a compile-time or build-time error.

    **Common Mistake:** Mixing legacy JSR-305 annotations (`@Nonnull`) with JSpecify annotations, which leads to split analyzer behavior and inconsistent nullness enforcement across modules.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q13JspecifyNullMarkedExample.java"
        ```

---

### 14. How does first-class API versioning work in Spring MVC and WebFlux 7?

??? question "Reveal answer"

    **Short Answer:** Spring Framework 7 introduces declarative API versioning natively in handler mappings (`@RequestMapping(version = "1.0")`). Version negotiation can resolve versions from URL paths, query parameters, request headers, or custom media types.

    **Internal Mechanism:** A configured `ApiVersionResolver` extracts the requested version token from the `HttpServletRequest` or ServerWebExchange and evaluates it against registered `RequestMappingHandlerMapping` version metadata.

    **Common Mistake:** Hand-rolling version routing via fragile `HandlerInterceptor` URL rewrites or custom regex filters that bypass Spring MVC argument resolution and OpenAPI documentation generation.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q14ApiVersionNegotiationExample.java"
        ```

---

### 15. What is `RestTestClient` and how does it compare to `MockMvc` and `WebTestClient`?

??? question "Reveal answer"

    **Short Answer:** `RestTestClient` provides a unified, fluent HTTP test client across all Spring Web environments. It allows identical assertion syntax whether running against mock servlet environments, reactive WebFlux, or real HTTP server sockets.

    **Internal Mechanism:** `RestTestClient` decouples request specification and JSON response assertions from the underlying transport mechanism, bridging MockMvc servlet dispatchers and real HTTP network clients into a single API surface.

    **Common Mistake:** Maintaining two separate test suites—one using `MockMvc` for slice tests and another using `RestClient` or `TestRestTemplate` for integration tests—duplicating test logic across different assertion syntaxes.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q15RestTestClientShapeExample.java"
        ```

---

### 16. How does Spring Framework 7 core retry compare to Resilience4j?

??? question "Reveal answer"

    **Short Answer:** Spring Framework 7 incorporates lightweight retry abstractions (`@Retryable`) directly into core framework components without requiring external dependencies, while Resilience4j remains the choice for complex resilience topologies (circuit breakers, rate limiters, bulkheads).

    **Internal Mechanism:** Core retry uses a lightweight AOP interceptor or functional wrapper with bounded backoff and jitter algorithms tailored specifically for virtual thread non-blocking sleeps.

    **Common Mistake:** Using heavyweight circuit breaker libraries for simple transient network call retries, or nesting core retry inside an unconfigured Resilience4j retry decorator creating retry amplification storms.

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q16CoreRetryVsResilience4jExample.java"
        ```

---

### 17. In what order should a senior engineer plan a Java 21 / Boot 3.5 to Java 25 / Boot 4 migration?

??? question "Reveal answer"

    **Short Answer:** Migrate in two strictly isolated phases: Phase 1 upgrades the runtime to Java 25 while keeping Spring Boot 3.5; Phase 2 upgrades Spring Boot 3.5 to Spring Boot 4.0. Upgrading both simultaneously conflates JDK runtime bugs with framework breaking changes.

    **Deep Explanation:** A combined upgrade makes regression attribution near-impossible. If latency increases by 20%, is it the new Generational ZGC, unpinned virtual thread scheduler shifts, Tomcat 11 request handling, or Spring Boot 4 auto-configuration changes? Isolating the upgrade into two stages ensures that each layer is benchmarked, tested, and validated in production before moving to the next.

    **Internal Mechanism:** Phase 1 exposes compiler warnings (`sun.misc.Unsafe` usage, SecurityManager references, preview language flags). Phase 2 executes Jakarta EE 11 namespace migration, Spring Security 7 DSL refactoring, and `@ConfigurationProperties` binding updates.

    **Common Mistake:** Changing `sourceCompatibility = 25` and `id("org.springframework.boot") version "4.0.0"` in a single commit, resulting in broken CI builds and untraceable test failures.

    **Production Consideration:** Deploy Phase 1 as a separate canary in production for at least one full business cycle to monitor GC throughput, CPU utilization, and virtual thread carrier metrics before starting Phase 2.

    **Follow-up Questions:** How do you handle third-party libraries (e.g. ByteBuddy, CGLIB, Lombok) that do not yet officially support the Java 25 bytecode version during Phase 1?

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q17MigrationPhaseOrderExample.java"
        ```

---

### 18. A feature silently stops working after upgrading to Spring Boot 4. How do you diagnose the cause?

??? question "Reveal answer"

    **Short Answer:** Verify if configuration properties were renamed or deprecated. Spring Boot's relaxed binder ignores unrecognized property keys by default, leaving fields at their Java defaults (`null` or `0`) without logging errors unless explicit validation is configured.

    **Deep Explanation:** Between major releases, property prefixes and keys are frequently renamed (e.g. `notifications.outbound.topic` to `notifications.outbound.destination`). In an unvalidated JavaBean configuration class, the new application configuration YAML is ignored, the bean is instantiated with null fields, and consumers silently skip message publication or use default fallback endpoints.

    **Internal Mechanism:** Spring Boot relaxed binding queries `ConfigurationPropertyName` tokens. If a token does not match any property setter or record component, it is discarded. Running with `--debug` or inspecting `/actuator/configprops` exposes the discrepancy between environment sources and bound values.

    **Common Mistake:** Assuming that if `application.yml` contains a typo or renamed key, Spring Boot will refuse to start. Without `@Validated`, startup succeeds unconditionally.

    **Production Consideration:** Always migrate configuration beans to Java records annotated with `@Validated` and Jakarta Validation constraints (`@NotBlank`, `@NotNull`), forcing the application to fail fast at the deploy stage rather than failing silently in production.

    **Follow-up Questions:** How does Spring Boot's `spring-boot-properties-migrator` starter aid in detecting renamed properties during migration?

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q18RemovedPropertyDiagnosisExample.java"
        ```

---

### 19. How do you classify upgrade changes across compile-time, startup-time, and production-traffic failure stages?

??? question "Reveal answer"

    **Short Answer:** Categorize breaking changes into three risk tiers: Compile-time (APIs removed/changed), Startup-time (auto-configuration failures, missing beans, property validation errors), and Production-traffic only (semantic changes, silent property drops, lock leaks).

    **Deep Explanation:**
    - **Compile-time failures** are the cheapest to fix: removed methods (e.g. Spring Security 7's `.and()`), altered method signatures, or JSpecify nullness violations.
    - **Startup-time failures** stop deployment immediately: circular dependencies, missing `@ConfigurationProperties` fields under `@Validated`, or failing database migrations.
    - **Production-traffic failures** are the most hazardous: unhandled exceptions escaping `ReentrantLock` blocks causing deadlocks, fail-open authorization gates allowing unauthenticated data deletion, or carrier thread pinning on native JNI calls under load.

    **Internal Mechanism:** The three stages reflect Java's phased lifecycle: `javac` AST checking $\to$ Spring ApplicationContext dependency wiring and validation $\to$ multithreaded runtime execution under real concurrent workload.

    **Common Mistake:** Trusting that a clean `./gradlew build` means the upgrade is successful, omitting production canary testing where traffic-only bugs manifest.

    **Production Consideration:** Implement automated smoke-test suites in staging that trigger every critical business pathway with synthetic traffic to force traffic-stage bugs to surface before customer rollout.

    **Follow-up Questions:** Why are security matchers that silently change evaluation order considered traffic-stage failures rather than compile-time failures?

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q19FailureStageClassificationExample.java"
        ```

---

### 20. A teammate insists on keeping every `ReentrantLock` rewrite made for Java 21 virtual threads. How do you evaluate this decision?

??? question "Reveal answer"

    **Short Answer:** Reject blanket retention of `ReentrantLock`. In Java 25, `synchronized` unpins cleanly. Keeping manual locks introduces heap object allocation overhead and massive deadlock risks if any developer omits the mandatory `finally { lock.unlock(); }` block on an abnormal path.

    **Deep Explanation:** Java 21 forced developers to rewrite `synchronized` blocks with `ReentrantLock` to avoid carrier thread pinning. In Java 25, HotSpot's ObjectMonitor redesign eliminated this constraint. A `synchronized` block is built into the JVM language specification, guarantees unlocking on all normal and abnormal paths via exception table bytecode (`athrow`), and avoids heap-allocating `ReentrantLock` instances.

    **Internal Mechanism:** Bytecode for `synchronized` emits `monitorenter` and `monitorexit` instructions paired with synthetic exception handler entries covering the entire instruction block. `ReentrantLock` relies entirely on correct programmer discipline; missing a `finally` block creates a permanent lock leak that halts all subsequent callers.

    **Common Mistake:** Assuming `ReentrantLock` is "always better and more modern" than `synchronized`, ignoring its verbosity, memory cost, and leak surface.

    **Production Consideration:** Where atomic variables (`AtomicLong`, `AtomicReference`) or `synchronized` suffice, revert the code. Reserve `ReentrantLock` strictly for advanced requirements: timed lock acquisition (`tryLock(timeout)`), interruptible acquisition, or fairness policies.

    **Follow-up Questions:** When is `StampedLock` still preferable to both `synchronized` and `ReentrantLock`?

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q20LockRevertDecisionExample.java"
        ```

---

### 21. What replaces `SecurityManager`-based authorization in modern Java 25 microservices?

??? question "Reveal answer"

    **Short Answer:** Explicit, fail-closed application-level authorization based on authenticated callers and authorities (e.g. Spring Security's `AuthorizationManager` or custom domain guards), paired with OS/container-level process isolation.

    **Deep Explanation:** The JVM `SecurityManager` checked whether code was authorized to execute privileged operations based on code source certificates. Modern backends execute trusted code servicing untrusted users. Authorization must therefore inspect the authenticated caller's identity, roles, and resource permissions on a per-request basis, defaulting to deny-all.

    **Internal Mechanism:** Modern authorization uses domain invariants or method security (`@PreAuthorize("hasAuthority('admin.operations')")`). A request context or `ScopedValue` holds the authenticated `Principal`, and the domain guard verifies that `caller.authorities().contains(requiredAuthority)`. If the caller is null or lacks the authority, an `AccessDeniedException` is thrown immediately.

    **Common Mistake:** Attempting to build custom JVM-wide security managers or relying on ambient static state to decide authorization, which breaks across concurrent virtual threads.

    **Production Consideration:** Defense in depth: enforce authorization at the API gateway, in Spring Security filter chains, and within the domain model itself.

    **Follow-up Questions:** How do you propagate authenticated security context across asynchronous boundaries when using Java 25 virtual threads?

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q21SecurityManagerReplacementExample.java"
        ```

---

### 22. After a Java 25 upgrade, a high-throughput sequence generator hangs under load. Thread dumps show dozens of virtual threads parked on a `ReentrantLock`. What happened?

??? question "Reveal answer"

    **Short Answer:** A `ReentrantLock` was acquired without enclosing the critical section in a `try-finally` block. An unexpected runtime exception was thrown, escaping without calling `lock.unlock()`, leaving the lock permanently held and deadlocking all subsequent callers.

    **Deep Explanation:** During the Java 21 migration, the team converted a synchronized sequence generator to `ReentrantLock` to prevent carrier thread pinning. However, an exception path (such as a sequence boundary check or logging failure) was introduced without a `finally` block. Once the exception was thrown, the thread exited without unlocking. Every subsequent virtual thread that called the method parked indefinitely on `ReentrantLock.lock()`.

    **Internal Mechanism:** `ReentrantLock` synchronizer state relies on AQS (`AbstractQueuedSynchronizer`). If `unlock()` is not invoked, the internal state remains locked (`state = 1`). Competing threads calling `lock()` are enqueued onto the AQS node queue and parked via `LockSupport.park()`. Because the owner thread has completed, no thread will ever unpark them.

    **Common Mistake:** Writing `lock.lock(); doSomething(); lock.unlock();` on adjacent lines without `try-finally`, believing that "this code cannot throw".

    **Production Consideration:** In Java 25, revert such methods back to `synchronized` or use `AtomicLong.incrementAndGet()`. Built-in language synchronization guarantees release on all exceptions, eliminating lock leaks entirely.

    **Follow-up Questions:** How do thread dumps generated via `jcmd <pid> Thread.dump_to_file` represent parked virtual threads versus pinned platform carrier threads?

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q22LockLeakScenarioExample.java"
        ```

---

### 23. Following a Spring Boot 4 canary deployment, messages are not reaching downstream queues, but health checks remain green and zero error logs are generated. How do you triage and resolve this?

??? question "Reveal answer"

    **Short Answer:** A configuration property key was renamed in Boot 4, causing relaxed binding to leave configuration fields as `null`. The notifier's publish method treated a null topic as an unconfigured optional feature and returned silently, causing silent data drops.

    **Deep Explanation:** The configuration bean was implemented as a mutable JavaBean with `@ConfigurationProperties(prefix = "notifications.outbound")` but lacked `@Validated`. In Boot 3.5, the property `notifications.outbound.topic: orders` bound correctly. In Boot 4, the property was renamed to `notifications.outbound.destination`. The application started cleanly because Spring Boot does not treat missing properties as errors. When `publish()` was invoked, `settings.getTopic()` returned `null`, triggering a defensive `return "skipped"` check.

    **Internal Mechanism:** Relaxed binding assigns null to unmatched reference fields. Because no `@NotNull` or `@NotBlank` validation was present on the configuration class, `ConfigurationPropertiesValidator` did not run, and the application completed startup successfully.

    **Common Mistake:** Treating missing configuration as an ignorable optional state without logging a warning, emitting a metric, or failing startup.

    **Production Consideration:**
    1. Refactor configuration properties to immutable Java records annotated with `@Validated` and Jakarta Validation constraints (`@NotBlank`, `@Positive`).
    2. Add startup integration tests asserting that all required property beans are fully populated.
    3. Ensure business logic throws an `IllegalStateException` or logs at `ERROR` level with alert metrics if an essential destination is absent.

    **Follow-up Questions:** How do you configure Spring Boot 4 to fail startup if unknown or unmapped configuration keys are detected in `application.yml`?

    ??? example "Example"

        ```java
        --8<-- "tracks/java25-boot4/modules/40-whats-new/src/examples/java/lab/java25boot4/whatsnew/questions/Q23SilentPropertyDropScenarioExample.java"
        ```
