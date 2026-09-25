# Batch 1 Curriculum Question Expansion (Modules 01–05) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand baseline modules 01 to 05 (Core Java, JVM, Concurrency, Spring Core, Spring Boot) from 23 to 30 canonical interview questions each (total 35 new questions) with dedicated compilable Java example classes and enriched cross-module follow-up links.

**Architecture:** Each module receives +2 Intermediate, +3 Senior, and +2 Scenario questions according to the curriculum convention. Every question is backed by a compilable Java class under `modules/<NN-slug>/src/examples/java/lab/<topic>/questions/` (named `Q24...` through `Q30...` to preserve existing references) with trailing evaluation comments and is referenced via PyMdown snippets in `docs/topics/<slug>/questions.md`. Senior and Scenario questions are enriched with bidirectional cross-links to sibling modules in their `**Follow-up Questions:**` sections.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Gradle Kotlin DSL, MkDocs Material.

**Spec:** `docs/superpowers/specs/2026-09-25-curriculum-question-expansion-design.md`

## Global Constraints
- Every question must compile with zero errors via `./gradlew compileExamples`.
- All snippet references (`--8<--`) must resolve and `mkdocs build --strict` must pass with zero broken links.
- Follow the 7-part format for Senior and Scenario questions verbatim: Short Answer, Deep Explanation, Internal Mechanism, Example, Common Mistake, Production Consideration, Follow-up Questions.
- Follow-up questions must include relative markdown links to target module anchors.
- Trailing comments in example classes must explicitly state evaluation outcomes.

## Review Focus
1. Link Anchor Validity: Relative paths and anchor names in `**Follow-up Questions:**` must point to existing heading slugs in target topic `questions.md` files so `mkdocs build --strict` will not fail.
2. Snippet Inclusion Accuracy: PyMdown snippet paths `--8<-- "modules/..."` must match the exact file name and path of newly created example classes.
3. Example Compilation Under Java 21: New example classes must not rely on preview features without flags and must compile under standard javac.
4. Formatting Integrity: Reveal admonitions (`??? question "Reveal answer"` and nested `??? example "Example"`) must preserve exact 4-space indentation.
5. Scenario Realistic Root Causes: Scenario questions must start from real production symptoms/metrics without leaking the diagnosis in the question title.

---

### Task 1: Module 01 — Core Java Question Expansion (Q24–Q30)

**Files:**
- Create:
  - `modules/01-core-java/src/examples/java/lab/corejava/questions/Q24PatternMatchingSwitchExample.java`
  - `modules/01-core-java/src/examples/java/lab/corejava/questions/Q25SequencedCollectionsExample.java`
  - `modules/01-core-java/src/examples/java/lab/corejava/questions/Q26SerializationSafetyExample.java`
  - `modules/01-core-java/src/examples/java/lab/corejava/questions/Q27ClassLoaderIsolationExample.java`
  - `modules/01-core-java/src/examples/java/lab/corejava/questions/Q28FalseSharingPaddingExample.java`
  - `modules/01-core-java/src/examples/java/lab/corejava/questions/Q29DirectMemoryLeakScenarioExample.java`
  - `modules/01-core-java/src/examples/java/lab/corejava/questions/Q30ScheduledExecutorFailureScenarioExample.java`
- Modify:
  - `docs/topics/core-java/questions.md`

**Interfaces:**
- Produces: 7 new compilable examples under `lab.corejava.questions.*` and 7 new questions in `docs/topics/core-java/questions.md`.

- [ ] **Step 1: Create the 7 compilable Java example classes for Module 01**
  Create `Q24PatternMatchingSwitchExample.java` (switch pattern matching, when guards, null handling), `Q25SequencedCollectionsExample.java` (SequencedCollection first/last reversed views), `Q26SerializationSafetyExample.java` (record serialization vs Java native serialization), `Q27ClassLoaderIsolationExample.java` (custom URLClassLoader and ClassCastException across loaders), `Q28FalseSharingPaddingExample.java` (cache line contention and padding), `Q29DirectMemoryLeakScenarioExample.java` (ByteBuffer.allocateDirect cleanup tracking), and `Q30ScheduledExecutorFailureScenarioExample.java` (ScheduledExecutorService swallows unhandled exceptions).

- [ ] **Step 2: Append Intermediate Q9 and Q10 to `docs/topics/core-java/questions.md`**
  Add Q9 (Pattern matching switch exhaustiveness) and Q10 (Sequenced collections contract) with snippet inclusions and concept links.

- [ ] **Step 3: Append Senior Q6, Q7, Q8 to `docs/topics/core-java/questions.md`**
  Add Q6 (Serialization security & migration), Q7 (ClassLoader isolation & memory leaks), Q8 (False sharing & cache-line padding) using the 7-part format with enriched cross-module follow-ups.

- [ ] **Step 4: Append Scenario 3 and Scenario 4 to `docs/topics/core-java/questions.md`**
  Add Scenario 3 (Native DirectByteBuffer leak in network parsing) and Scenario 4 (Silent task disappearance in scheduled executor) using the symptom-first 7-part format.

- [ ] **Step 5: Verify Module 01 examples and links**
  Run `mkdocs build --strict` and verify no broken anchors or missing snippet references.

- [ ] **Step 6: Commit Module 01 expansion**
  ```bash
  git add modules/01-core-java/ docs/topics/core-java/
  git commit -m "$(cat <<'EOF'
  feat(core-java): expand question bank to 30 questions with cross-links

  Generated with [Devin](https://devin.ai)

  Co-Authored-By: Devin <158243242+devin-ai-integration[bot]@users.noreply.github.com>
  EOF
  )"
  ```

---

### Task 2: Module 02 — JVM Question Expansion (Q24–Q30)

**Files:**
- Create:
  - `modules/02-jvm/src/examples/java/lab/jvm/questions/Q24ClassDataSharingExample.java`
  - `modules/02-jvm/src/examples/java/lab/jvm/questions/Q25GenerationalZgcExample.java`
  - `modules/02-jvm/src/examples/java/lab/jvm/questions/Q26JitInliningDevirtualizationExample.java`
  - `modules/02-jvm/src/examples/java/lab/jvm/questions/Q27MetaspaceBytecodeLeakExample.java`
  - `modules/02-jvm/src/examples/java/lab/jvm/questions/Q28SafepointPollingHandshakeExample.java`
  - `modules/02-jvm/src/examples/java/lab/jvm/questions/Q29SafepointLoopStallScenarioExample.java`
  - `modules/02-jvm/src/examples/java/lab/jvm/questions/Q30NativeMemoryExhaustionScenarioExample.java`
- Modify:
  - `docs/topics/jvm/questions.md`

**Interfaces:**
- Produces: 7 new compilable examples under `lab.jvm.questions.*` and 7 new questions in `docs/topics/jvm/questions.md`.

- [ ] **Step 1: Create the 7 compilable Java example classes for Module 02**
  Create `Q24ClassDataSharingExample.java` (AppCDS shared archive benefits), `Q25GenerationalZgcExample.java` (colored pointers & load barriers), `Q26JitInliningDevirtualizationExample.java` (monomorphic vs megamorphic call sites), `Q27MetaspaceBytecodeLeakExample.java` (runtime proxy generation without unloading), `Q28SafepointPollingHandshakeExample.java` (safepoint check frequency and thread handshakes), `Q29SafepointLoopStallScenarioExample.java` (counted int loop safepoint elimination), and `Q30NativeMemoryExhaustionScenarioExample.java` (Deflater/Inflater native resource leak).

- [ ] **Step 2: Append Intermediate Q9 and Q10 to `docs/topics/jvm/questions.md`**
  Add Q9 (Class Data Sharing AppCDS) and Q10 (Generational ZGC vs G1) with snippet inclusions and concept links.

- [ ] **Step 3: Append Senior Q6, Q7, Q8 to `docs/topics/jvm/questions.md`**
  Add Q6 (JIT inlining & devirtualization), Q7 (Metaspace proxy exhaustion), Q8 (Safepoint polling & thread handshakes) using the 7-part format with cross-links to Core Java and Concurrency.

- [ ] **Step 4: Append Scenario 3 and Scenario 4 to `docs/topics/jvm/questions.md`**
  Add Scenario 3 (P99 latency spikes from loop safepoint stalls) and Scenario 4 (Container killed by OOMKiller from unclosed native Deflater) using symptom-first 7-part format.

- [ ] **Step 5: Verify Module 02 examples and links**
  Run `mkdocs build --strict`.

- [ ] **Step 6: Commit Module 02 expansion**
  ```bash
  git add modules/02-jvm/ docs/topics/jvm/
  git commit -m "$(cat <<'EOF'
  feat(jvm): expand question bank to 30 questions with cross-links

  Generated with [Devin](https://devin.ai)

  Co-Authored-By: Devin <158243242+devin-ai-integration[bot]@users.noreply.github.com>
  EOF
  )"
  ```

---

### Task 3: Module 03 — Concurrency Question Expansion (Q24–Q30)

**Files:**
- Create:
  - `modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q24StructuredConcurrencyExample.java`
  - `modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q25VarHandleAccessModesExample.java`
  - `modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q26LockFreeStackAbaExample.java`
  - `modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q27VirtualThreadCarrierPinningExample.java`
  - `modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q28LongAdderCellContentionExample.java`
  - `modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q29ThreadPoolStarvationDeadlockScenarioExample.java`
  - `modules/03-concurrency/src/examples/java/lab/concurrency/questions/Q30AsyncContextPropagationScenarioExample.java`
- Modify:
  - `docs/topics/concurrency/questions.md`

**Interfaces:**
- Produces: 7 new compilable examples under `lab.concurrency.questions.*` and 7 new questions in `docs/topics/concurrency/questions.md`.

- [ ] **Step 1: Create the 7 compilable Java example classes for Module 03**
  Create `Q24StructuredConcurrencyExample.java` (StructuredTaskScope fork, join, shutdown), `Q25VarHandleAccessModesExample.java` (VarHandle getAcquire/setRelease vs volatile), `Q26LockFreeStackAbaExample.java` (Treiber stack with AtomicStampedReference), `Q27VirtualThreadCarrierPinningExample.java` (synchronized block carrier pinning vs ReentrantLock), `Q28LongAdderCellContentionExample.java` (LongAdder Striped64 cells under high thread contention), `Q29ThreadPoolStarvationDeadlockScenarioExample.java` (parent task waiting on child task in same pool), and `Q30AsyncContextPropagationScenarioExample.java` (MDC/SecurityContext loss across CompletableFuture stages).

- [ ] **Step 2: Append Intermediate Q9 and Q10 to `docs/topics/concurrency/questions.md`**
  Add Q9 (StructuredTaskScope error propagation) and Q10 (VarHandle memory access modes) with snippet inclusions and concept links.

- [ ] **Step 3: Append Senior Q6, Q7, Q8 to `docs/topics/concurrency/questions.md`**
  Add Q6 (Lock-free Treiber stack & ABA mitigation), Q7 (Virtual thread carrier pinning on synchronized), Q8 (LongAdder cell contention vs CAS loops) using 7-part format with cross-links.

- [ ] **Step 4: Append Scenario 3 and Scenario 4 to `docs/topics/concurrency/questions.md`**
  Add Scenario 3 (Self-deadlock in single-pool task hierarchy) and Scenario 4 (Missing tenant MDC logs in asynchronous pipeline) using symptom-first 7-part format.

- [ ] **Step 5: Verify Module 03 examples and links**
  Run `mkdocs build --strict`.

- [ ] **Step 6: Commit Module 03 expansion**
  ```bash
  git add modules/03-concurrency/ docs/topics/concurrency/
  git commit -m "$(cat <<'EOF'
  feat(concurrency): expand question bank to 30 questions with cross-links

  Generated with [Devin](https://devin.ai)

  Co-Authored-By: Devin <158243242+devin-ai-integration[bot]@users.noreply.github.com>
  EOF
  )"
  ```

---

### Task 4: Module 04 — Spring Core Question Expansion (Q24–Q30)

**Files:**
- Create:
  - `modules/04-spring-core/src/examples/java/lab/springcore/questions/Q24BeanPostProcessorLifecycleExample.java`
  - `modules/04-spring-core/src/examples/java/lab/springcore/questions/Q25ConditionalPhaseEvaluationExample.java`
  - `modules/04-spring-core/src/examples/java/lab/springcore/questions/Q26CircularDependencyResolutionExample.java`
  - `modules/04-spring-core/src/examples/java/lab/springcore/questions/Q27ProxyMechanismsAndBypassingExample.java`
  - `modules/04-spring-core/src/examples/java/lab/springcore/questions/Q28SpringAotReflectionHintsExample.java`
  - `modules/04-spring-core/src/examples/java/lab/springcore/questions/Q29PrototypeInSingletonLeakScenarioExample.java`
  - `modules/04-spring-core/src/examples/java/lab/springcore/questions/Q30ProxySecurityBypassScenarioExample.java`
- Modify:
  - `docs/topics/spring-core/questions.md`

**Interfaces:**
- Produces: 7 new compilable examples under `lab.springcore.questions.*` and 7 new questions in `docs/topics/spring-core/questions.md`.

- [ ] **Step 1: Create the 7 compilable Java example classes for Module 04**
  Create `Q24BeanPostProcessorLifecycleExample.java` (BPP before/after initialization ordering), `Q25ConditionalPhaseEvaluationExample.java` (ConfigurationCondition.ConfigurationPhase evaluation), `Q26CircularDependencyResolutionExample.java` (three-level singleton cache in DefaultSingletonBeanRegistry), `Q27ProxyMechanismsAndBypassingExample.java` (JDK dynamic proxy vs CGLIB final methods and identity), `Q28SpringAotReflectionHintsExample.java` (RuntimeHintsRegistrar reflection registration), `Q29PrototypeInSingletonLeakScenarioExample.java` (prototype bean cached in singleton vs ObjectProvider.getObject()), and `Q30ProxySecurityBypassScenarioExample.java` (self-invocation bypassing Spring method interceptor).

- [ ] **Step 2: Append Intermediate Q9 and Q10 to `docs/topics/spring-core/questions.md`**
  Add Q9 (BeanPostProcessor lifecycle) and Q10 (Conditional phase evaluation) with snippet inclusions and concept links.

- [ ] **Step 3: Append Senior Q6, Q7, Q8 to `docs/topics/spring-core/questions.md`**
  Add Q6 (Circular dependency 3-level cache), Q7 (CGLIB vs JDK proxy limitations), Q8 (Spring 6 AOT runtime hints) using 7-part format with cross-links.

- [ ] **Step 4: Append Scenario 3 and Scenario 4 to `docs/topics/spring-core/questions.md`**
  Add Scenario 3 (Prototype state contamination in singleton service) and Scenario 4 (Security check bypassed via internal method call) using symptom-first 7-part format.

- [ ] **Step 5: Verify Module 04 examples and links**
  Run `mkdocs build --strict`.

- [ ] **Step 6: Commit Module 04 expansion**
  ```bash
  git add modules/04-spring-core/ docs/topics/spring-core/
  git commit -m "$(cat <<'EOF'
  feat(spring-core): expand question bank to 30 questions with cross-links

  Generated with [Devin](https://devin.ai)

  Co-Authored-By: Devin <158243242+devin-ai-integration[bot]@users.noreply.github.com>
  EOF
  )"
  ```

---

### Task 5: Module 05 — Spring Boot Question Expansion (Q24–Q30)

**Files:**
- Create:
  - `modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q24ConfigurationPropertiesBindingExample.java`
  - `modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q25AutoConfigurationOrderingExample.java`
  - `modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q26StartupPhasesAndLazyInitExample.java`
  - `modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q27ActuatorHardeningAndHealthExample.java`
  - `modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q28ContextIndexingOptimizationExample.java`
  - `modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q29KubernetesGracefulShutdownScenarioExample.java`
  - `modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q30RefreshScopeDynamicConfigScenarioExample.java`
- Modify:
  - `docs/topics/spring-boot/questions.md`

**Interfaces:**
- Produces: 7 new compilable examples under `lab.springboot.questions.*` and 7 new questions in `docs/topics/spring-boot/questions.md`.

- [ ] **Step 1: Create the 7 compilable Java example classes for Module 05**
  Create `Q24ConfigurationPropertiesBindingExample.java` (immutable constructor binding and JSR-380 validation), `Q25AutoConfigurationOrderingExample.java` (@AutoConfigureAfter and conditional checks), `Q26StartupPhasesAndLazyInitExample.java` (ApplicationReadyEvent and lazy init risks), `Q27ActuatorHardeningAndHealthExample.java` (CompositeHealthContributor and SanitizingFunction), `Q28ContextIndexingOptimizationExample.java` (spring.components index generation), `Q29KubernetesGracefulShutdownScenarioExample.java` (server.shutdown=graceful and preStop lifecycle hook), and `Q30RefreshScopeDynamicConfigScenarioExample.java` (@RefreshScope proxy re-creation).

- [ ] **Step 2: Append Intermediate Q9 and Q10 to `docs/topics/spring-boot/questions.md`**
  Add Q9 (ConfigurationProperties binding) and Q10 (Auto-configuration ordering) with snippet inclusions and concept links.

- [ ] **Step 3: Append Senior Q6, Q7, Q8 to `docs/topics/spring-boot/questions.md`**
  Add Q6 (Startup phases & lazy init risks), Q7 (Actuator hardening & custom health), Q8 (Context indexing optimization) using 7-part format with cross-links.

- [ ] **Step 4: Append Scenario 3 and Scenario 4 to `docs/topics/spring-boot/questions.md`**
  Add Scenario 3 (Dropped in-flight requests during rolling restart) and Scenario 4 (Transient 500 errors on configuration refresh) using symptom-first 7-part format.

- [ ] **Step 5: Verify Module 05 examples and links**
  Run `mkdocs build --strict`.

- [ ] **Step 6: Commit Module 05 expansion**
  ```bash
  git add modules/05-spring-boot/ docs/topics/spring-boot/
  git commit -m "$(cat <<'EOF'
  feat(spring-boot): expand question bank to 30 questions with cross-links

  Generated with [Devin](https://devin.ai)

  Co-Authored-By: Devin <158243242+devin-ai-integration[bot]@users.noreply.github.com>
  EOF
  )"
  ```

---

### Task 6: Progress Tracking & Final Batch 1 Verification

**Files:**
- Modify:
  - `docs/progress.md`

- [ ] **Step 1: Update `docs/progress.md`**
  Reflect the new 30-question milestone for baseline modules 01 through 05 in `docs/progress.md`.

- [ ] **Step 2: Run full documentation verification**
  Run `mkdocs build --strict` to ensure 100% clean builds across the entire documentation site.

- [ ] **Step 3: Commit Batch 1 completion**
  ```bash
  git add docs/progress.md
  git commit -m "$(cat <<'EOF'
  docs(progress): update baseline question counts for batch 1 (modules 01-05)

  Generated with [Devin](https://devin.ai)

  Co-Authored-By: Devin <158243242+devin-ai-integration[bot]@users.noreply.github.com>
  EOF
  )"
  ```
