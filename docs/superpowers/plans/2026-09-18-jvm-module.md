# JVM Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Module 02 JVM with four production-realistic failure labs, safe diagnostic demos, full documentation and hidden Q&A answers, while retrofitting Module 01 to the same compiled-example convention.

**Architecture:** A new `examples` source set compiles educational and opt-in diagnostic programs without packaging or executing them. Deterministic lifecycle/bounds behavior lives in tested `src/main`; GC/JIT/OOM behavior is explained and observed through constrained commands. Every question displays only its prompt; the complete answer and nested code example are collapsed.

**Tech Stack:** Java 21 toolchain, Gradle 9.7.1 Kotlin DSL, JUnit Jupiter 5.14.4, AssertJ 3.27.7, Error Prone 2.50.0, Spotless 8.10.2, MkDocs Material 9.7.7, JDK tools (`javap`, `jcmd`, JFR).

**Spec:** `docs/superpowers/specs/2026-09-18-jvm-module-design.md`

## Global Constraints

- Preserve Java 21 / Spring Boot 3.5.16 baseline and all Module 01 behavior.
- No deterministic GC/JIT timing assertions; use compiled opt-in demos plus diagnostic commands.
- Dangerous OOM/StackOverflow demos require an explicit `--i-understand` argument and never run from lifecycle tasks.
- Every question has exactly one collapsed `??? question "Reveal answer"`; each answer has a nested collapsed `??? example "Example"`.
- Java shown in docs comes from `src/examples`, `src/main` or clean broken targets via checked snippets.
- Review targets contain no categorized issue comments; `SOLUTION.md` contains exact categories.
- Production implementation is test-first; examples are compiled but not packaged and need tests only when correctness is material.
- Module 02 needs four broken examples, 8/8/5/2 questions, nine topic pages and issue-catalogue backlinks.

---

### Task 1: Make compiled collapsed examples a repository convention

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/spec/module-conventions.md`
- Modify: `docs/spec/documentation-conventions.md`
- Modify: `.devin/skills/writing-interview-questions/SKILL.md`
- Modify: `.devin/skills/documenting-topic/SKILL.md`
- Modify: `.devin/skills/verifying-module/SKILL.md`
- Modify: `build-logic/src/main/kotlin/lab.java-conventions.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `scripts/verify-all.sh`

**Produces:** source set `examples`, task `compileExamples`, `check -> compileExamples`, root aggregator `compileExamples`.

- [ ] Add `src/examples/java` and resources to an `examples` source set. Its compile classpath is main output + implementation; runtime is output + compile classpath. Register `compileExamples` depending on `compileExamplesJava`; make module `check` depend on it. Disable Error Prone only if a diagnostic demo intentionally uses a discouraged API, and document each disabled check locally—do not disable it globally.
- [ ] Add root `compileExamples` aggregation using `subprojects.mapNotNull { findByName(...) }`. Extend `verify-all.sh` to execute it.
- [ ] Add the exact reveal contract from the design to both specs and skills. Verification counts question headings, `??? question "Reveal answer"`, and nested `??? example "Example"`; all three counts must match per module.
- [ ] Run `./gradlew compileExamples build` with Module 01 currently `NO-SOURCE`; run `mkdocs build --strict`.
- [ ] Commit: `Establish compiled question examples`.

---

### Task 2: Retrofit Module 01 Q&A with hidden answers and compiled examples

**Files:**
- Create: `modules/01-core-java/src/examples/java/lab/corejava/examples/{EqualityExamples,CollectionExamples,GenericExamples,ResourceExamples,StreamExamples}.java`
- Modify: `docs/topics/core-java/questions.md`
- Test: existing Module 01 tests + `compileExamples`

**Interfaces:** each example class is final, has a private constructor, and exposes small static methods used only for readable examples.

- [ ] Implement five focused, compilable classes: equality/hash map lookup and immutable record; ArrayList/Map/Comparator operations; `copy(List<? extends T>, List<? super T>)`; try-with-resources/Optional mapping; lazy stream and ordered executor examples. Avoid external I/O in examples.
- [ ] Restructure all 23 Core Java questions. Keep each `###` question visible; indent every answer field inside `??? question "Reveal answer"`; append nested `??? example "Example"` with one checked source snippet. Senior/scenario reveals retain all seven fields.
- [ ] Run structural counts: 23 questions, 23 answer reveals, 23 nested examples. Run `./gradlew :modules:01-core-java:compileExamples :modules:01-core-java:test` and strict MkDocs.
- [ ] Commit: `Hide Core Java answers behind compiled examples`.

---

### Task 3: Scaffold Module 02 and safe JVM diagnostic demos

**Files:**
- Modify: `settings.gradle.kts`
- Create: `modules/02-jvm/{README.md,build.gradle.kts}`
- Create under `src/examples/java/lab/jvm/examples/`: `BytecodeDemo`, `ClassInitializationDemo`, `ClassLoaderDemo`, `StackDepthDemo`, `HeapPressureDemo`, `AllocationDemo`, `GcRootsDemo`, `ContainerMemoryDemo`, `ClassLoaderLeakDemo`

**Interfaces:** dangerous demos accept `--i-understand`; absent confirmation prints usage and returns without pressure.

- [ ] Include `modules:02-jvm`; apply only `lab.java-conventions`. README links docs and lists `test`, `compileExamples`, `compileBrokenExamples` commands.
- [ ] Add focused demos: arithmetic/branch/try-catch for `javap`; static initialization order; loader chain including null bootstrap; recursive stack and retained-byte heap pressure behind confirmation; allocation loop with finite default iteration count; root reachability; effective `Runtime.maxMemory/availableProcessors`; URLClassLoader retention observation with WeakReference and explicit nondeterminism warning.
- [ ] Compile demos. Manually run only safe demos (`BytecodeDemo`, class initialization/loaders, container memory). Verify dangerous demos without confirmation exit normally.
- [ ] Commit: `Scaffold safe JVM diagnostic demos`.

---

### Task 4: Implement lifecycle-aware listener registration with TDD and review exercise

**Files:**
- Create: `src/main/java/lab/jvm/listenerregistry/{ListenerRegistry,Registration}.java`
- Create: `src/test/java/lab/jvm/listenerregistry/ListenerRegistryTest.java`
- Create: `broken-examples/static-listener-leak/{GlobalEventRegistry,REVIEW,SOLUTION}.java|md`
- Modify: `docs/issues/performance.md`, `docs/issues/maintainability.md`

**Interfaces:**

```java
public final class ListenerRegistry<T> {
    public Registration register(T listener);
    public List<T> snapshot();
    public int size();
}
public interface Registration extends AutoCloseable { @Override void close(); }
```

- [ ] RED tests: registration appears in immutable snapshot; close removes only its listener; repeated close is harmless; two registry instances are isolated; null rejected.
- [ ] Run targeted tests and observe missing types. Implement instance-owned list and an idempotent registration handle (`AtomicBoolean.compareAndSet`); synchronize compound list access. Run green and full module tests.
- [ ] Broken static registry stores listeners forever, permits duplicate registration, exposes backing list and has no lifecycle handle. `SOLUTION.md` annotates Memory, Resource leak, Maintainability and Design issues (3–8 total), names `lab.jvm.listenerregistry`; catalogue each.
- [ ] Run `compileBrokenExamples`; commit: `Make listener retention lifecycle scoped`.

---

### Task 5: Implement scoped ThreadLocal context with TDD and review exercise

**Files:**
- Create: `src/main/java/lab/jvm/requestcontext/{RequestIdentity,RequestContext,ContextScope}.java`
- Create: `src/test/java/lab/jvm/requestcontext/RequestContextTest.java`
- Create: `broken-examples/threadlocal-pool-leak/{RequestContext,RequestHandler,REVIEW.md,SOLUTION.md}`
- Modify: `docs/issues/{data-consistency,security,reliability,maintainability}.md`

**Interfaces:**

```java
public record RequestIdentity(String requestId, String userId) {}
public final class RequestContext {
    public ContextScope open(RequestIdentity identity);
    public Optional<RequestIdentity> current();
}
public interface ContextScope extends AutoCloseable { @Override void close(); }
```

- [ ] RED tests: open exposes current; close clears; nested close restores parent; repeated close harmless; exception inside try-with-resources clears; two tasks on the same single-thread executor do not share identity.
- [ ] Implement per-instance ThreadLocal. `open` captures previous, sets identity, and returns idempotent scope restoring previous or removing. Run green without sleeps.
- [ ] Broken handler sets static ThreadLocal and neither finally-removes nor restores nesting; stale user data leaks to reused pooled thread. Annotate Data consistency, Security, Resource leak and Design issues; catalogue and compile.
- [ ] Commit: `Scope request context to lexical lifetime`.

---

### Task 6: Implement bounded LRU cache with TDD and review exercise

**Files:**
- Create: `src/main/java/lab/jvm/boundedcache/BoundedLruCache.java`
- Create: `src/test/java/lab/jvm/boundedcache/BoundedLruCacheTest.java`
- Create: `broken-examples/unbounded-cache/{TemplateCache,REVIEW.md,SOLUTION.md}`
- Modify: `docs/issues/{performance,observability,maintainability}.md`

**Interface:** `BoundedLruCache(int maximumSize)`, synchronized `get`, `put`, `computeIfAbsent`, `size`, `snapshot`; access-order LinkedHashMap evicts eldest when `size > maximumSize`.

- [ ] RED tests: non-positive capacity rejected; never exceeds capacity; least-recently-accessed entry evicted; get refreshes order; replacement does not grow; immutable snapshot; concurrent puts leave size bounded.
- [ ] Implement minimal synchronized JDK cache; run green and Error Prone with no warnings.
- [ ] Broken static cache accepts user-controlled keys, stores large rendered values forever, has no capacity/expiry/weight or metrics. Annotate Memory, Scalability, Observability and Design issues; explain Caffeine trade-off; catalogue and compile.
- [ ] Commit: `Bound retained cache state`.

---

### Task 7: Implement allocation-conscious metric encoding with TDD and review exercise

**Files:**
- Create: `src/main/java/lab/jvm/allocation/{MetricEvent,MetricLineEncoder}.java`
- Create: `src/test/java/lab/jvm/allocation/MetricLineEncoderTest.java`
- Create: `broken-examples/excessive-hot-path-allocation/{MetricLineEncoder,REVIEW.md,SOLUTION.md}`
- Modify: `docs/issues/{performance,maintainability,observability}.md`

**Interfaces:** `MetricEvent(String name, Map<String,String> tags, long value)` defensively copies tags; `MetricLineEncoder.encode(MetricEvent)` returns deterministic `name{key=value,...}=value`, sorting tags by key and escaping `\`, `,`, `=`.

- [ ] RED parameterized tests for deterministic tag order, escaping, empty tags, invalid blank name/null input and immutable event tags.
- [ ] Implement one operation-local StringBuilder and static precompiled validation Pattern; no shared mutable builder. Run green.
- [ ] Broken encoder compiles regex per event, uses streams/intermediate maps and `String.format` in the loop, and logs every encoded value. Annotate Performance (multiple allocation sites), Observability (high-volume logging) and Maintainability issues; catalogue.
- [ ] Add an `AllocationDemo` comparison command using finite iterations and JFR instructions—never assert timing/allocation in tests. Compile and commit: `Reduce measured hot-path allocation`.

---

### Task 8: Write JVM concepts and internals

**Files:** `docs/topics/jvm/{index,concepts,internals}.md`; modify `mkdocs.yml`, checklist/progress (Module 02 🟨).

- [ ] Concepts covers full curriculum: JVM/JRE/JDK, bytecode, runtime areas, allocation/TLAB/escape analysis, tiered JIT/CDS/AOT, generational hypothesis and G1/ZGC/Shenandoah, roots/safepoints/STW, OOM variants/StackOverflow, diagnostics and container memory.
- [ ] Internals traces load→verify→prepare→resolve→initialize; parent delegation; interpreter→C1→C2/deoptimization; allocation fast path; root marking/remembered sets; collector trade-offs. Distinguish spec guarantees from HotSpot details. Add Mermaid diagrams.
- [ ] Embed demos and exact `javap`, `java -Xmx/-Xss/-Xlog`, `jcmd`, `-XX:StartFlightRecording` commands, with danger admonitions.
- [ ] Add nav and JVM checklist; strict build; commit: `Document JVM runtime internals`.

---

### Task 9: Write hidden JVM interview answers with nested examples

**Files:** `docs/topics/jvm/questions.md`, `docs/questions/jvm.md`, `mkdocs.yml`.

- [ ] Write 8 Basic: JDK/JRE/JVM; bytecode; heap vs stack; metaspace; classloader hierarchy; JIT; GC root; STW.
- [ ] Write 8 Intermediate: loading phases; initialization triggers; TLAB/escape analysis; tiered compilation/deoptimization; generational hypothesis; G1 regions; heap dump vs thread dump vs JFR; OOM variants.
- [ ] Write 5 Senior using design scenarios: redeploy heap growth; pooled-thread identity leak; stable live set/high allocation; low-heap OOM; container OOMKill.
- [ ] Write 2 symptom-first scenarios: heap climbs after redeploy; cross-user identity after exceptional requests.
- [ ] Every question: visible heading only; one collapsed full answer; nested collapsed snippet/command example. Surface globally using snippets with docs-root-relative links.
- [ ] Verify counts 23/23/23 and strict build; commit: `Add hidden JVM interview answers`.

---

### Task 10: Complete JVM review, solution, tests, production and exercises

**Files:** `docs/topics/jvm/{code-review,solutions,tests,production,exercises}.md`; catalogue files; `mkdocs.yml`.

- [ ] Code review page embeds four clean targets, dimensions and complete collapsed reveals (problem, mechanism, impact, detection, fix, trade-off).
- [ ] Solutions provides issue-to-fix matrix and before/after checked snippets for all four exercises.
- [ ] Tests separates deterministic tests from demos and lists exact commands. Production covers histograms/dominator paths, ThreadLocal retention, allocation profiling, classloader/metaspace, OOM taxonomy and container native-memory budget.
- [ ] Exercises: heap-dump investigation and bytecode/class-initialization experiment, both with collapsed complete solutions.
- [ ] Reconcile every SOLUTION issue with exactly one fixed catalogue file and backlink. Run strict docs and commit: `Complete the JVM learning loop`.

---

### Task 11: Update repository workflow and progress

**Files:** `README.md`, `docs/progress.md`, `scripts/verify-all.sh`, `mkdocs.yml`.

- [ ] Ensure verifier runs `compileExamples` and structural checks enforce question/answer/example count equality for completed modules.
- [ ] Keep Module 02 🟨 until final verification. Update README study instructions to explain question→answer reveal→example reveal.
- [ ] Run `scripts/run-module.sh jvm`; expect library/demo exit 2 with test guidance.
- [ ] Commit: `Verify compiled learning examples`.

---

### Task 12: Final verification, review and completion

- [ ] Run `./gradlew spotlessApply build integrationTest compileExamples compileBrokenExamples`.
- [ ] Run static checks: exactly four Module 02 broken examples; REVIEW/SOLUTION/source present; no ` issue:` in clean Java; recorded correct packages/tests exist; 23 JVM and 23 Core Java questions each match answer/example counts; no dangerous demo lifecycle dependency.
- [ ] Run `.venv/bin/mkdocs build --strict` and `scripts/verify-all.sh`.
- [ ] Invoke `/reviewing-lab-change` and `/verifying-module 02-jvm`; fix every Blocking/Should-fix finding and rerun all checks.
- [ ] Set Module 02 progress cells ✅ and README status to Module 03 next; strict-build again.
- [ ] Commit: `Complete the JVM interview lab`.
