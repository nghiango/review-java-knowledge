# JVM Module and Collapsed Question Examples Design

**Date:** 2026-09-18
**Status:** Approved in chat; awaiting review of this written specification
**Scope:** Complete baseline Module 02 (`jvm`) and retrofit Module 01 questions with compiled,
collapsed code examples

## 1. Goal

Deliver a senior-level JVM learning module that connects runtime internals to reproducible failure
signals and diagnostic tools without relying on nondeterministic GC/JIT tests. Establish a
repository-wide convention that every interview answer is hidden behind a collapsed reveal and
contains separately collapsed example code, then bring the completed Core Java module into that
convention.

## 2. Repository-wide question reveal and example convention

Every question in `docs/topics/<slug>/questions.md` keeps only the question visible. All answer
sections are nested inside a collapsed `Reveal answer` admonition; its example is a second,
independently collapsed admonition:

```markdown
### Q: Why must equal objects have equal hash codes?

??? question "Reveal answer"

    **Short Answer:** Hash-based collections choose a bucket before checking equality. Equal
    objects with different hashes may never be compared.

    ??? example "Example"

        ```java
        --8&lt;-- "modules/NN-<slug>/src/examples/java/<package>/<Example>.java"
        ```
```

The initial page therefore reveals no answer text, hints, code or root-cause terminology beyond the
question itself. Opening the answer still leaves its example collapsed so the learner can reason
from prose before studying code.

Basic answers remain short inside the reveal; neither admonition changes the five-line Short Answer
limit. Intermediate answers place Short Answer, Internal Mechanism and Common Mistake inside the
same reveal. Senior and Scenario questions place all seven required answer sections inside it.

### 2.1 Source and compilation

Question/demo source lives under `src/examples/java`, not in Markdown and not in `src/main`:

```text
modules/NN-<slug>/src/examples/java/lab/<topic>/examples/
```

The Java convention plugin creates an `examples` source set and `compileExamples` lifecycle task.
`check` depends on `compileExamples`, so documentation code cannot silently rot. Example source may
use main output and module implementation dependencies but is not packaged into the module JAR.

Examples are educational artefacts, not production implementations corresponding to broken code;
they do not require one test per method. Any algorithm whose correctness is material still belongs
under `src/main` and is tested first.

Use a small cohesive example class for a group of related questions. Reusing one class in multiple
collapsed blocks is preferable to 23 trivial files. Do not introduce named snippet markers unless
the whole class is too noisy; use separate focused classes instead.

### 2.2 Required coverage

Every Basic, Intermediate, Senior and Scenario question has exactly one collapsed answer container,
and that answer has at least one nested collapsed example. Examples may be:

- compiled Java source from `src/examples/java`;
- tested correct source from `src/main/java`;
- a clean broken review target when the question asks the learner to diagnose it;
- a shell command block for JVM diagnostics, optionally paired with compiled Java source.

Source snippets always use `pymdownx.snippets`. Shell commands may be written directly in Markdown
because they are commands, not duplicated application source.

Module 01 receives five cohesive example classes (equality/immutability, collections, generics,
exceptions/resources, streams). All 23 existing questions are restructured so the full answer is
collapsed and each answer contains a separately collapsed compiled example.

## 3. Hard-to-test JVM behavior policy

GC, class unloading, JIT compilation, escape analysis, OOM and timing-sensitive allocation are not
asserted through brittle unit tests. They receive:

1. A precise conceptual explanation, including what is specification versus implementation detail.
2. A small compilable, opt-in program under `src/examples/java` or a clean broken review target.
3. An exact constrained command (`-Xmx`, `-Xss`, `-Xlog`, JFR, `jcmd`) and expected categories of
   output—not machine-specific counts or timings.
4. A warning when the command intentionally exhausts memory/stack or runs until interrupted.
5. Diagnostic interpretation: which signal supports or rejects a hypothesis.

Unit tests cover deterministic contracts around lifecycle, bounds and cleanup. A demonstration is
never executed by `build`, `test` or `verify-all.sh`; only its compilation is automatic.

## 4. Module 02 architecture

Module path: `modules/02-jvm`; package root: `lab.jvm`; type: code. It applies the existing
`lab.java-conventions` plugin and has no Spring or infrastructure dependencies.

### 4.1 Static listener leak

**Broken scenario:** a global static event registry retains listeners from completed application
contexts/deployments. Registration has no removal handle, accepts duplicate listeners and exposes no
bounded/lifecycle semantics.

**Correct package:** `lab.jvm.listenerregistry`.

**Design:** an instance-scoped `ListenerRegistry<T>` returns an idempotent `Registration`
(`AutoCloseable`) that removes exactly its listener. The registry exposes `size()` for diagnostics
but not its mutable backing collection. Tests prove removal, idempotent close and instance isolation.

**Trade-off:** explicit lifecycle management requires callers to close registrations; weak
references are not used because they make delivery depend on GC reachability.

### 4.2 ThreadLocal leak in a pooled thread

**Broken scenario:** request identity is set in a ThreadLocal and not removed when processing throws;
the next request on the same worker sees stale identity.

**Correct package:** `lab.jvm.requestcontext`.

**Design:** `RequestContext.open(RequestIdentity)` returns an `AutoCloseable` scope. Closing restores
the previous value, supporting nesting and cleanup in try-with-resources. Tests submit sequential
requests to the same single-thread executor, force a failure, and prove no cross-request leakage;
latches/futures are used, never sleeps.

**Trade-off:** callers must preserve lexical scope. Async hand-off requires explicit context
propagation; this module explains ScopedValue as a newer alternative but reserves structured
concurrency depth for Module 03.

### 4.3 Unbounded cache

**Broken scenario:** a static map caches every rendered template forever; user-controlled keys and
large values grow old-generation occupancy without a capacity, expiry or observability policy.

**Correct package:** `lab.jvm.boundedcache`.

**Design:** `BoundedLruCache<K,V>` validates positive capacity, uses access-order LinkedHashMap,
evicts eldest entries, and synchronizes compound operations. Tests cover capacity, access-order
eviction, replacement, invalid capacity and concurrent size bounds.

**Trade-off:** one lock limits throughput; production systems may use Caffeine for segmented/highly
concurrent policies, expiry, weighting and metrics. This JDK implementation exists to expose the
policy mechanics, not replace Caffeine.

### 4.4 Excessive allocation in a hot path

**Broken scenario:** a metrics formatter compiles a regex per event, creates split arrays and
intermediate lists, and uses `String.format` in a high-volume loop.

**Correct package:** `lab.jvm.allocation`.

**Design:** immutable `MetricEvent` plus `MetricLineEncoder` with a precompiled Pattern and one
StringBuilder per operation. Deterministic tests prove equivalent escaping/output and input
validation. Allocation improvement is demonstrated using JFR allocation events and optionally JMH
concepts; tests do not assert allocation counts or elapsed time.

**Trade-off:** precompilation and a direct parser improve a measured hot path but add specialized
code; optimize only after profiling.

### 4.5 Classloader leak guided experiment

Classloader retention is documented with a compilable `ClassLoaderLeakDemo` that creates an isolated
URLClassLoader and shows how a parent/static reference or ThreadLocal can retain it. The page uses a
WeakReference only as an observation aid and explicitly states that `System.gc()` is a request, not
a deterministic assertion.

This is not a fifth broken/correct exercise and is never part of the test suite. Commands demonstrate
class histograms and classloader statistics with `jcmd` and JFR.

## 5. Additional JVM examples

Focused compiled examples support questions and concepts:

- `BytecodeDemo` with `javap -c -v`
- `ClassInitializationDemo` showing initialization triggers/order
- `ClassLoaderDemo` printing bootstrap/platform/application loaders
- `StackDepthDemo` run with a constrained `-Xss`
- `HeapPressureDemo` run with constrained `-Xmx` and heap-dump flags
- `AllocationDemo` recorded with JFR
- `GcRootsDemo` illustrating static/thread/local reachability
- `ContainerMemoryDemo` printing effective heap and processor counts

Dangerous examples print a warning and require an explicit command-line confirmation argument before
allocating until failure or overflowing the stack.

## 6. Documentation

`docs/topics/jvm/` receives the standard nine pages:

- `index.md`: learning path and module navigation
- `concepts.md`: architecture, runtime areas, allocation/JIT/GC and error taxonomy
- `internals.md`: class lifecycle/delegation, bytecode execution, TLAB/escape analysis, tiered JIT,
  GC roots/safepoints and collector trade-offs
- `questions.md`: at least 8 Basic, 8 Intermediate, 5 Senior, 2 symptom-first Scenarios; every
  question keeps its complete answer collapsed, with a separately collapsed example nested inside
- `code-review.md`: four clean targets and complete collapsed reveals
- `solutions.md`: before/after snippets, issue-to-fix matrix and trade-offs
- `tests.md`: deterministic contract tests versus opt-in diagnostic experiments
- `production.md`: heap growth, metaspace/classloader growth, ThreadLocal cross-request leakage,
  GC pressure, OOM/StackOverflow and container sizing
- `exercises.md`: at least two collapsed solutions, including a heap-dump investigation and
  class-initialization/bytecode exercise

Global JVM questions are surfaced under `docs/questions/jvm.md` without duplicating authored text.
Issue catalogue entries link both ways. MkDocs navigation, progress and checklist are updated.

## 7. Interview depth

Required senior scenarios include:

- Heap climbs after redeploy while live business data is flat: distinguish static, cache and
  classloader retention using histogram, dominator tree and classloader statistics.
- A user sometimes inherits another request's identity after an exception: connect executor reuse,
  ThreadLocalMap lifecycle and lexical cleanup.
- High allocation rate and frequent young GC with stable live set: use JFR allocation profiling,
  avoid increasing heap as the first response.
- `OutOfMemoryError` with low Java-heap occupancy: distinguish metaspace, direct buffer, native
  thread and GC-overhead variants.
- Container is OOM-killed despite `-Xmx` looking safe: account for metaspace, code cache, thread
  stacks, direct/native memory and effective container limits.

## 8. Verification

Acceptance requires:

1. `./gradlew build`, `integrationTest`, `compileBrokenExamples` and new `compileExamples` pass.
2. `build` compiles examples but never compiles broken examples.
3. Four broken examples each have clean source, REVIEW.md, SOLUTION.md, corresponding tested correct
   package and registered issue backlinks.
4. JVM question counts meet 8/8/5/2; every question has one collapsed complete answer, and every
   answer has a nested collapsed example.
5. All 23 Core Java questions use the same collapsed-answer structure and examples sourced from
   compiled code.
6. No dangerous diagnostic demo runs from any lifecycle task.
7. `mkdocs build --strict` and `scripts/verify-all.sh` pass.
8. Module 01 remains complete; Module 02 is marked complete only after every code-module DoD item
   passes.

## 9. Out of scope

- Deterministic assertions that GC collects an object or JIT applies a specific optimization
- Performance thresholds or machine-dependent allocation counts
- A production cache competing with Caffeine
- Deep Java Memory Model/concurrency primitives (Module 03)
- Java 25 JVM deltas (the `java25-boot4` track after baseline Module 02 is complete)
- Running intentional OOM/StackOverflow examples in CI
