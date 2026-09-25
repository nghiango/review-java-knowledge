# Curriculum Question Expansion & Cross-Linking Design

**Date:** 2026-09-25  
**Status:** Approved in chat; awaiting user review of this written specification  
**Scope:** Architecture and roadmap for expanding all 30 baseline modules from 23 to 30 canonical interview questions with enriched cross-module linking, beginning with Batch 1 (Modules 01–05).

---

## 1. Executive Summary & Goals

The Senior Java / Spring Backend Engineer Interview Lab currently contains 23 questions per baseline module (8 Basic, 8 Intermediate, 5 Senior, 2 Scenarios), fulfilling the minimum specification defined in `docs/spec/module-conventions.md`.

To provide comprehensive, end-to-end depth and reflect real-world staff/principal backend engineering interview expectations, this initiative:
1. Expands every baseline module from **23 to 30 questions** (+2 Intermediate, +3 Senior, +2 Scenarios per module, totaling 210 new questions across all 30 modules).
2. Establishes a systematic **cross-module linking network** via hyperlinked `**Follow-up Questions:**` that connect related foundational, operational, data-tier, and architectural concepts.
3. Implements dedicated, compilable Java example classes under `modules/<NN-slug>/src/examples/java/lab/<topic>/questions/` with trailing evaluation comments for every new question.
4. Executes the expansion in 6 sequential batches of 5 modules, starting with **Batch 1 (Modules 01–05: Core Java, JVM, Concurrency, Spring Core, Spring Boot)**.
5. Preserves strict verification gates: zero compiler errors in `./gradlew compileExamples` and zero broken links in `mkdocs build --strict`.

---

## 2. Question Distribution & Formatting Standards

### 2.1 Per-Module Target Distribution (Total: 30 Questions)

| Level | Existing Count | Added Count | New Total | Required Format |
|---|---|---|---|---|
| **Basic** | 8 | 0 | 8 | Concise (≤ 5 lines), direct answer, link to concept, compilable example class. |
| **Intermediate** | 8 | +2 | 10 | Short Answer, Internal Mechanism, Common Mistake, link to concept, compilable example class. |
| **Senior** | 5 | +3 | 8 | Full 7-part format: Short Answer, Deep Explanation, Internal Mechanism, Example reference, Common Mistake, Production Consideration, **Enriched Follow-up Questions with cross-links**. |
| **Scenario** | 2 | +2 | 4 | Incident/symptom-driven 7-part format starting from metrics/logs/alerts, root cause analysis, mitigation, trade-offs, and **Enriched Follow-up Questions with cross-links**. |

### 2.2 Cross-Module Follow-up Linking Standard

In all Senior and Scenario questions, the `**Follow-up Questions:**` section is enriched with explicit, clickable cross-module links. Links must adhere to standard relative Markdown paths resolving to the respective module's `questions.md` anchor or `concepts.md` anchor:

```markdown
**Follow-up Questions:**
- How does thread switching interact with this mechanism under virtual threads? See [Concurrency: Virtual Thread Pinning & ThreadLocals](../concurrency/questions.md#q20)
- How does the database engine handle lock escalation for this query pattern? See [Database / SQL: Lock Contention](../database-sql/questions.md#q15)
- What alerting metrics reveal pool starvation before timeout errors occur? See [Observability: Micrometer Pool Metrics](../observability/questions.md#q12)
```

### 2.3 Compilable Example Code Contract

Every new question receives a dedicated, compilable Java example class under:
`modules/<NN-slug>/src/examples/java/lab/<topic>/questions/` (for code modules) or inlined/referenced doc examples (for doc modules).

All code examples must:
- Compile under Java 21 without warnings.
- Demonstrate runtime or evaluation results via trailing inline comments:
  ```java
  List<String> immutable = List.of("a", "b");
  // immutable.add("c"); // throws UnsupportedOperationException
  ```
- Be embedded into `docs/topics/<slug>/questions.md` using the PyMdown snippets syntax:
  ```markdown
  ??? example "Example"
      ```java
      --8<-- "modules/NN-slug/src/examples/java/lab/topic/questions/QxxExample.java"
      ```
  ```

---

## 3. Sequential 6-Batch Roadmap

| Batch | Modules Included | Core Focus |
|---|---|---|
| **Batch 1** | **01 Core Java, 02 JVM, 03 Concurrency, 04 Spring Core, 05 Spring Boot** | Foundations of execution, memory, threading, and framework container lifecycle. |
| **Batch 2** | **06 Spring MVC, 07 Spring Transactions, 08 JPA/Hibernate, 09 Database/SQL, 10 REST API** | Web request lifecycle, transaction management, ORM internals (including `temporary-practise.md` questions), SQL optimization, and API design. |
| **Batch 3** | **11 Spring Security, 12 Testing, 13 Caching/Redis, 14 Kafka, 15 RabbitMQ** | Security boundaries, testing strategies, distributed caching, and event-driven messaging. |
| **Batch 4** | **16 AWS Messaging, 17 Distributed Systems, 18 Resilience, 19 Distributed Data Patterns, 20 Spring Cloud** | Cloud messaging, distributed consensus, fault tolerance, dual-write consistency, and microservice infrastructure. |
| **Batch 5** | **21 WebClient/WebFlux, 22 Observability, 23 Performance, 24 Docker, 25 AWS** | Reactive pipelines, telemetry (metrics/traces/logs), profiling, containerization, and cloud deployment. |
| **Batch 6** | **26 CI/CD, 27 System Design, 28 Architecture, 29 Design Patterns, 30 Senior Engineering** | Delivery pipelines, high-level system architecture, design patterns, and engineering leadership. |

---

## 4. Batch 1 Detailed Specification (Modules 01–05)

### 4.1 Module 01: Core Java (`modules/01-core-java`, `docs/topics/core-java/`)
* **New Questions (7 total):**
  - **Intermediate Q9:** Pattern matching for `switch` and record patterns: exhaustiveness, guarded patterns (`when`), and null handling.
    - Example: `Q09PatternMatchingSwitchExample.java`
  - **Intermediate Q10:** Sequenced Collections (`SequencedCollection`, `SequencedSet`, `SequencedMap`) contract, reversed views, and encounter order.
    - Example: `Q10SequencedCollectionsExample.java`
  - **Senior Q19:** Object serialization vulnerabilities and migration to records, Jackson, or Protobuf without object graph deserialization hazards.
    - Example: `Q19SerializationSafetyExample.java`
    - Cross-links: [JVM Metaspace & Bytecode](../jvm/questions.md), [Security Serialization](../spring-security/questions.md)
  - **Senior Q20:** Custom ClassLoaders, class identity (`ClassCastException` across classloaders), and preventing ClassLoader memory leaks on dynamic unloading.
    - Example: `Q20ClassLoaderIsolationExample.java`
    - Cross-links: [JVM Class Loading Lifecycle](../jvm/questions.md)
  - **Senior Q21:** Cache-line false sharing, padding, and data locality in Java data structures (`@Contended` vs field padding).
    - Example: `Q21FalseSharingPaddingExample.java`
    - Cross-links: [Concurrency JMM Safe Publication](../concurrency/questions.md), [Performance Hardware Architecture](../performance/questions.md)
  - **Scenario 3:** Native off-heap DirectByteBuffer leak when handling high-throughput network buffers leading to OS kill without heap memory alert.
    - Example: `Q22DirectMemoryLeakScenarioExample.java`
    - Cross-links: [JVM Off-Heap Diagnostics](../jvm/questions.md), [WebFlux Netty Buffer Leaks](../webclient-webflux/questions.md)
  - **Scenario 4:** Uncaught `RuntimeException` in scheduled executor thread terminates periodic execution silently.
    - Example: `Q23ScheduledExecutorFailureScenarioExample.java`
    - Cross-links: [Concurrency ThreadPoolExecutor Policies](../concurrency/questions.md), [Spring Core Async Exception Handling](../spring-core/questions.md)

### 4.2 Module 02: JVM (`modules/02-jvm`, `docs/topics/jvm/`)
* **New Questions (7 total):**
  - **Intermediate Q9:** Class Data Sharing (CDS) and Dynamic AppCDS: mechanisms for reducing startup time and resident memory footprint in microservices.
    - Example: `Q09ClassDataSharingExample.java`
  - **Intermediate Q10:** Generational ZGC vs G1 GC: colored pointers, load barriers, concurrent compaction tradeoffs, and latency guarantees.
    - Example: `Q10GenerationalZgcExample.java`
  - **Senior Q19:** JIT compiler inlining budgets, megamorphic call sites (`invokevirtual` devirtualization), and performance cliff identification using JFR.
    - Example: `Q19JitInliningDevirtualizationExample.java`
    - Cross-links: [Core Java Polymorphism](../core-java/questions.md), [Performance Profiling](../performance/questions.md)
  - **Senior Q20:** Metaspace exhaustion from runtime bytecode generation proxies (CGLIB, ByteBuddy) and class unloading constraints.
    - Example: `Q20MetaspaceBytecodeLeakExample.java`
    - Cross-links: [Spring Core CGLIB Proxies](../spring-core/questions.md), [Core Java ClassLoaders](../core-java/questions.md)
  - **Senior Q21:** Safepoints, thread-local handshakes, and uncounted loop safepoint polling overhead affecting p99.9 latency SLAs.
    - Example: `Q21SafepointPollingHandshakeExample.java`
    - Cross-links: [Concurrency Thread States](../concurrency/questions.md), [Observability Latency Tracing](../observability/questions.md)
  - **Scenario 3:** Latency spikes at p99 caused by safepoint stalls in long-running `int` counted loops without loop strip mining.
    - Example: `Q22SafepointLoopStallScenarioExample.java`
    - Cross-links: [Performance JFR Analysis](../performance/questions.md)
  - **Scenario 4:** Native memory exhaustion caused by unclosed `Deflater`/`Inflater` or JNI bindings exceeding cgroup limits.
    - Example: `Q23NativeMemoryExhaustionScenarioExample.java`
    - Cross-links: [Docker Container Limits](../docker/questions.md), [JVM Container Memory Limits](../jvm/questions.md)

### 4.3 Module 03: Concurrency (`modules/03-concurrency`, `docs/topics/concurrency/`)
* **New Questions (7 total):**
  - **Intermediate Q9:** Structured Concurrency via `StructuredTaskScope` (`ShutdownOnFailure`, `ShutdownOnSuccess`), subtask cancellation, and scope nesting.
    - Example: `Q09StructuredConcurrencyExample.java`
  - **Intermediate Q10:** `VarHandle` versus `AtomicReferenceFieldUpdater`: memory access modes (`getAcquire`, `setRelease`, `opaque`, `volatile`) and throughput.
    - Example: `Q10VarHandleAccessModesExample.java`
  - **Senior Q19:** Lock-free stack/queue implementation and ABA problem remediation using `AtomicStampedReference`.
    - Example: `Q19LockFreeStackAbaExample.java`
    - Cross-links: [Core Java Data Structures](../core-java/questions.md), [Database Optimistic Versioning](../database-sql/questions.md)
  - **Senior Q20:** Virtual thread carrier pinning hazards: `synchronized` blocks vs `ReentrantLock`, native methods, and filesystem I/O.
    - Example: `Q20VirtualThreadCarrierPinningExample.java`
    - Cross-links: [Spring Transactions Connection Holding](../spring-transactions/questions.md), [Database Connection Pools](../database-sql/questions.md)
  - **Senior Q21:** Scalable high-contention counters: `LongAdder` cell striped contention vs CAS spin loops (`AtomicLong`).
    - Example: `Q21LongAdderCellContentionExample.java`
    - Cross-links: [Core Java False Sharing](../core-java/questions.md), [Observability Metric Counters](../observability/questions.md)
  - **Scenario 3:** Thread pool starvation deadlock caused by submitting dependent child tasks into the same bounded worker pool.
    - Example: `Q22ThreadPoolStarvationDeadlockScenarioExample.java`
    - Cross-links: [Spring Core Async Executors](../spring-core/questions.md), [Resilience Bulkhead Isolation](../resilience/questions.md)
  - **Scenario 4:** Silent loss of `SecurityContext` and MDC correlation IDs across reactive/asynchronous `CompletableFuture` stages.
    - Example: `Q23AsyncContextPropagationScenarioExample.java`
    - Cross-links: [Spring Security Context Propagation](../spring-security/questions.md), [Observability Distributed Tracing](../observability/questions.md)

### 4.4 Module 04: Spring Core (`modules/04-spring-core`, `docs/topics/spring-core/`)
* **New Questions (7 total):**
  - **Intermediate Q9:** `BeanPostProcessor` vs `BeanFactoryPostProcessor`: execution ordering, lifecycle hooks, and early bean initialization risks.
    - Example: `Q09BeanPostProcessorLifecycleExample.java`
  - **Intermediate Q10:** Conditional configuration evaluation: `@Conditional`, `ConditionContext`, phase ordering (`REGISTER_BEAN` vs `CONFIGURATION_PHASE`).
    - Example: `Q10ConditionalPhaseEvaluationExample.java`
  - **Senior Q19:** Circular dependencies resolution internals in Spring (three-level cache in `DefaultSingletonBeanRegistry`) and why constructor injection cleanly breaks cycles.
    - Example: `Q19CircularDependencyResolutionExample.java`
    - Cross-links: [Design Patterns Dependency Inversion](../design-patterns/questions.md)
  - **Senior Q20:** CGLIB vs JDK dynamic proxies: final class/method interception limitations, `equals`/`hashCode` proxy bypassing, and self-invocation traps.
    - Example: `Q20ProxyMechanismsAndBypassingExample.java`
    - Cross-links: [Spring Transactions Self-Invocation](../spring-transactions/questions.md), [Spring Security Method Security](../spring-security/questions.md)
  - **Senior Q21:** Ahead-Of-Time (AOT) engine and GraalVM Native Image compilation boundaries in Spring Framework 6.
    - Example: `Q21SpringAotReflectionHintsExample.java`
    - Cross-links: [JVM Class Loading](../jvm/questions.md), [Performance Startup Optimization](../performance/questions.md)
  - **Scenario 3:** Concurrency race condition and state contamination caused by injecting a prototype bean into a singleton bean without `ObjectProvider` or `@Lookup`.
    - Example: `Q22PrototypeInSingletonLeakScenarioExample.java`
    - Cross-links: [Concurrency Safe Publication](../concurrency/questions.md)
  - **Scenario 4:** Silent bypass of `@PreAuthorize` security checks caused by intra-class method calls within a proxy-based service.
    - Example: `Q23ProxySecurityBypassScenarioExample.java`
    - Cross-links: [Spring Security Method Security](../spring-security/questions.md), [Spring Transactions Proxy Bypass](../spring-transactions/questions.md)

### 4.5 Module 05: Spring Boot (`modules/05-spring-boot`, `docs/topics/spring-boot/`)
* **New Questions (7 total):**
  - **Intermediate Q9:** Type-safe configuration binding via `@ConfigurationProperties`: relaxed binding rules, JSR-380 validation, and immutable constructor binding (`@ConstructorBinding`).
    - Example: `Q09ConfigurationPropertiesBindingExample.java`
  - **Intermediate Q10:** Custom auto-configuration ordering: `@AutoConfiguration`, `@AutoConfigureBefore`, `@AutoConfigureAfter`, and conditional registration.
    - Example: `Q10AutoConfigurationOrderingExample.java`
  - **Senior Q19:** Spring Boot application startup phases: `ApplicationStartingEvent` through `ApplicationReadyEvent`, and lazy initialization (`spring.main.lazy-initialization`) risks in production.
    - Example: `Q19StartupPhasesAndLazyInitExample.java`
    - Cross-links: [Observability Startup Metrics](../observability/questions.md), [Testing Context Caching](../testing/questions.md)
  - **Senior Q20:** Production Actuator hardening: role-based endpoint exposure, sensitive data sanitization (`SanitizingFunction`), and custom composite `HealthIndicator`s.
    - Example: `Q20ActuatorHardeningAndHealthExample.java`
    - Cross-links: [Spring Security Actuator Authorization](../spring-security/questions.md), [Observability Health Probes](../observability/questions.md)
  - **Senior Q21:** Reducing classpath scanning overhead in large microservices using `spring-context-indexer` and modular component scanning.
    - Example: `Q21ContextIndexingOptimizationExample.java`
    - Cross-links: [Architecture Modular Monoliths](../architecture/questions.md), [Spring Core Bean Discovery](../spring-core/questions.md)
  - **Scenario 3:** Kubernetes rolling deployment drops in-flight HTTP requests during SIGTERM because graceful shutdown was unconfigured or premature connection reset occurred.
    - Example: `Q22KubernetesGracefulShutdownScenarioExample.java`
    - Cross-links: [Docker Container Lifecycles](../docker/questions.md), [Spring MVC Request Processing](../spring-mvc/questions.md)
  - **Scenario 4:** Stale configuration and memory leakage caused by improper `@RefreshScope` usage during Spring Cloud Config dynamic refresh.
    - Example: `Q23RefreshScopeDynamicConfigScenarioExample.java`
    - Cross-links: [Spring Cloud Configuration](../spring-cloud/questions.md), [Concurrency Safe Publication](../concurrency/questions.md)

---

## 5. Verification & Quality Gates

Each batch must satisfy the following automated verification suite before being considered complete:
1. `./gradlew compileExamples` — compiles all new example classes with zero errors.
2. `mkdocs build --strict` — validates all markdown files, anchors, and `--8<--` snippet inclusions with zero broken links.
3. Git history commits accurately describing the additions following repository commit conventions.
4. `docs/progress.md` updated reflecting the new 30-question count.
