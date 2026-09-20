# Solution: Sequence allocator suite that only passes in one order

## Annotated code

### `SequenceAllocator.java`

```java
package lab.testing.broken.orderdependenttestsuite;

/**
 * Issues monotonically increasing sequence numbers.
 *
 * <p>{@code start} is the value the counter holds before the first allocation, so {@code new
 * SequenceAllocator(0).next()} returns 1 and {@link #current()} returns 0 until then. Allocation
 * stops at {@link Long#MAX_VALUE} with an {@link ArithmeticException} instead of wrapping to a
 * negative value.
 */
public final class SequenceAllocator {

    // Testing issue: the counter is static, so it belongs to the class rather than to an allocator.
    // Every instance shares one sequence and a newly constructed allocator continues wherever the
    // previous one stopped instead of starting from its own start value — the constructor assigns
    // only start, and nothing in the API says the sequence is process-wide. The Javadoc above
    // describes the per-instance behaviour the class was meant to have, which is why the defect reads
    // as correct.
    private static long counter;

    private final long start;

    public SequenceAllocator(long start) {
        this.start = start;
    }

    /**
     * Advances the counter and returns the newly allocated value.
     *
     * @throws ArithmeticException if the counter has already reached {@link Long#MAX_VALUE}
     */
    public long next() {
        counter = Math.incrementExact(counter);
        return counter;
    }

    /** Returns the last allocated value, or {@code start} if nothing has been allocated yet. */
    public long current() {
        return counter;
    }

    /** Restores the counter to {@code start}, so the next allocation is {@code start + 1}. */
    public void reset() {
        counter = start;
    }
}
```

### `SequenceAllocatorTest.java`

```java
package lab.testing.broken.orderdependenttestsuite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests for {@link SequenceAllocator}.
 *
 * <p>The methods run in the order declared here so the suite is deterministic in CI.
 */
// Testing issue: @TestMethodOrder(OrderAnnotation.class) pins the execution order and hides the
// coupling below. Each method builds its own allocator, so the tests look independent, but every
// expected value is the next absolute number of the process-wide sequence: the class is green only
// because the methods advance the static counter exactly as many times as the later methods assume.
// Run a method alone, reorder them, or let a random orderer pick, and the suite fails for a reason
// that has nothing to do with the allocator's contract.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SequenceAllocatorTest {

    @Test
    @Order(1)
    void next_fromStart_returnsFirstValue() {
        SequenceAllocator allocator = new SequenceAllocator(0);

        assertEquals(1, allocator.next());
    }

    // Maintainability issue: because the counter lives in production static state and the expected
    // values are absolute, the class cannot be split, reordered or executed in parallel. Any JUnit
    // configuration that runs methods concurrently, or a second test class that touches the
    // allocator, produces failures that look like allocator defects and cost the team a triage round.
    @Test
    @Order(2)
    void next_afterFirstAllocation_returnsSecondValue() {
        SequenceAllocator allocator = new SequenceAllocator(0);

        assertEquals(2, allocator.next());
    }

    @Test
    @Order(3)
    void current_afterTwoAllocations_returnsLastValue() {
        SequenceAllocator allocator = new SequenceAllocator(0);

        assertEquals(2, allocator.current());
    }

    @Test
    @Order(4)
    void next_afterTwoAllocations_returnsThirdValue() {
        SequenceAllocator allocator = new SequenceAllocator(0);

        assertEquals(3, allocator.next());
    }

    @Test
    @Order(5)
    void reset_afterThreeAllocations_restartsAtStart() {
        SequenceAllocator allocator = new SequenceAllocator(0);

        allocator.reset();

        assertEquals(0, allocator.current());
    }
}
```

Both files are the review target: the static counter in `SequenceAllocator` is the root cause, and the
test's absolute-value assertions (kept passing by `@TestMethodOrder`) are what make the class green
anyway. Executed as a whole in the declared order the suite passes — which is why the coupling survived
review.

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Testing issue | High | `SequenceAllocatorTest` (`@TestMethodOrder`, the absolute-value assertions) | The class passes only when the methods run in the declared order |
| 2 | Testing issue | High | `SequenceAllocator.counter` (static) | The counter is class-wide, so a newly constructed allocator does not start from its own `start` value |
| 3 | Maintainability issue | Medium | `SequenceAllocatorTest` | The class cannot be split, reordered or executed in parallel |

## Issue details

### Absolute assertions pinned to the declaration order

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5 `@TestMethodOrder`, `@Order` · **Interview frequency:** High · **Production impact:** High

**Location:** `SequenceAllocatorTest` (`@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` and the
`assertEquals(2, …)` / `assertEquals(3, …)` methods)

#### Problem
Each method builds a fresh allocator but asserts the absolute number it expects the process to hand out
next: `@Order(1)` expects 1, `@Order(2)` expects 2, `@Order(4)` expects 3. Those numbers are positions
in one shared sequence, not properties of a fresh allocator. The suite therefore has very few passing
execution orders, and `@TestMethodOrder(OrderAnnotation.class)` is what supplies one of them.

#### Why it happens
The author wanted each test to pin down the value the allocator returns. Because the allocator's counter
is shared (issue 2), the value a fresh allocator returns depends on how many allocations earlier methods
made; writing the observed number down is the shortest way to assert it, and adding `@Order` makes the
numbers line up. The suite turns green, and the coupling is never questioned.

#### Production impact
```text
run a method alone (next_afterFirstAllocation_returnsSecondValue)
→ org.opentest4j.AssertionFailedError: expected: <2> but was: <1>
replace @TestMethodOrder with MethodOrderer.Random
→ 10 of 10 runs failed (1 to 4 of the 5 methods red), because only a handful of the 120
  permutations start with @Order(1), @Order(2), @Order(3)/@Order(4) and end with @Order(5)
→ the suite is red for reasons unrelated to SequenceAllocator, developers retry until green, and a
  real regression in next()/reset() is indistinguishable from the noise
```

#### Broken implementation
```java
@Test @Order(1) void next_fromStart_returnsFirstValue() {
    SequenceAllocator allocator = new SequenceAllocator(0);
    assertEquals(1, allocator.next());   // holds only when this method runs first
}

@Test @Order(2) void next_afterFirstAllocation_returnsSecondValue() {
    SequenceAllocator allocator = new SequenceAllocator(0);
    assertEquals(2, allocator.next());   // fails alone: expected: <2> but was: <1>
}
```

#### Correct implementation
```java
@Test
void next_calledRepeatedly_returnsConsecutiveValues() {
    SequenceAllocator allocator = new SequenceAllocator(10);

    assertThat(allocator.next()).isEqualTo(11);
    assertThat(allocator.next()).isEqualTo(12);
}
```

#### Why the solution works
Each test creates the allocator it asserts on and the counter is instance state (issue 2), so the
sequence a test exercises starts at a value the test itself chose and is reachable from nowhere else.
The expected values are consequences of the calls the test makes, not of the method's position, so
every method passes alone, in any order, and in parallel. The class needs no `@TestMethodOrder`.

#### Trade-offs
Writing several `next()` calls in one method rather than spreading them across methods looks less
granular, and a failure now points at a method that covers a small sequence instead of a single call.
That is the price of independence; keep the methods small and give each one a `@DisplayName` that names
the progression it covers.

#### How to detect it
Any `@TestMethodOrder`/`@Order` pair in a class whose tests do not genuinely share a started resource
is a candidate. Run each method individually (`--tests 'Class.method'`) and replace the orderer with
`MethodOrderer.Random` for a few runs; a suite that fails is order-dependent. A second signal is an
assertion whose expected value is a bare absolute number that no call in the test produces.

#### Interview follow-up
> When is fixing the order of test methods legitimate, and how do you make that dependency explicit
> instead of encoding it in `@Order`?

#### Related
- Test isolation · Deterministic tests · JUnit 5 `@TestMethodOrder` · Flaky test diagnosis

### Static counter shared across allocators

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5 static state · **Interview frequency:** High · **Production impact:** High

**Location:** `SequenceAllocator.counter`

#### Problem
The counter is declared `private static long`, so it belongs to the class, not to an allocator. Two
allocators created with the same `start` value share one sequence, and because the constructor assigns
only `start`, a newly constructed allocator does not begin at its own start value at all — it continues
wherever the process is. Nothing in the constructor, the API or the Javadoc says the sequence is
process-wide; the Javadoc describes the per-instance behaviour the class was meant to have.

#### Why it happens
The field was meant to be the instance counter the constructor seeds from `start`. Marking it `static`
(and dropping the seeding) moves the state to the class, where it lives for the whole JVM. Because the
class still compiles, has a `start` parameter and passes its test suite, the mistake looks like a
deliberate process-wide sequence.

#### Production impact
```text
two allocators created in the same process
→ the second one's first next() returns whatever the first has already reached, not start + 1
→ identifiers that a caller expects to be independent interleave, and a restart of one sequence
  moves the other
a test class that builds a fresh allocator per method
→ still shares the counter with every other class and with every previous run in the same JVM
→ the suite is order-dependent and the failure appears far from the class that caused it
```

#### Broken implementation
```java
private static long counter;   // class-wide, never seeded from start

private final long start;

public SequenceAllocator(long start) {
    this.start = start;
}
```

#### Correct implementation
```java
private final long start;
private long counter;

public SequenceAllocator(long start) {
    this.start = start;
    this.counter = start;
}
```

#### Why the solution works
The counter is instance state seeded from `start`, so two allocators created with the same value
advance independently and nothing one instance does is observable by another. That is the property the
test relies on when it builds its own allocator, and it is what makes the class safe to use for several
sequences in one process — one per tenant, per partition, per stream.

#### Trade-offs
Instance state means each allocator holds its own counter, so a caller that wants one process-wide
sequence must share the instance deliberately instead of relying on an accident. Where a genuinely
global sequence is required, model it explicitly (a singleton or a dedicated `AtomicLong` service) so
the shared state is visible at the call site.

#### How to detect it
Look for `static` fields of a mutable type — a counter, a collection, a mutable POJO — in production
classes that are constructed with a per-instance value. Construct two instances with the same argument
and check whether they are independent; a shared field makes them interact. A field that a constructor
parameter appears to initialise but does not is the specific shape here.

#### Interview follow-up
> Which production state is legitimately static, and how do you keep static state from making a test
> suite order-dependent without moving the problem into the tests?

#### Related
- Hidden global state · Test isolation · Instance vs class state · JUnit 5 static state

### Order-dependent test class cannot run in parallel

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic
**Technology:** JUnit 5 parallel execution · **Interview frequency:** Medium · **Production impact:** Medium

**Location:** `SequenceAllocatorTest` (the class as a whole)

#### Problem
Because the counter is shared and every expected value is absolute, the class can only be executed as
one ordered run. It cannot be split into two classes, reordered by the IDE, run with JUnit's parallel
execution enabled, or selected method-by-method — all of which are normal ways to shorten a build. Any
of them turns the suite red with failures that look like allocator defects.

#### Why it happens
Order dependence and parallelism are invisible on a single-threaded, whole-class run, which is how the
suite is normally executed. The cost only appears when the build is optimised — enabling JUnit
parallelism, sharding tests across CI agents, or re-running one method in the IDE — and by then the
suite is treated as correct.

#### Production impact
```text
build time grows → the team enables JUnit parallel execution (junit.jupiter.execution.parallel.enabled)
→ methods of SequenceAllocatorTest run concurrently against one static counter
→ interleaved next() calls make the absolute assertions fail nondeterministically
→ the feature is switched off again "because the suite is flaky", and the build stays slow
```

#### Broken implementation
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SequenceAllocatorTest {

    @Test @Order(1) void next_fromStart_returnsFirstValue() {
        assertEquals(1, new SequenceAllocator(0).next());
    }

    @Test @Order(2) void next_afterFirstAllocation_returnsSecondValue() {
        assertEquals(2, new SequenceAllocator(0).next());
    }
}
```

#### Correct implementation
```java
class SequenceAllocatorTest {

    @Test
    void next_fromStart_returnsStartPlusOne() {
        assertThat(new SequenceAllocator(0).next()).isEqualTo(1);
    }

    @Test
    void next_twoAllocatorsWithSameStart_doNotShareState() {
        SequenceAllocator first = new SequenceAllocator(0);
        SequenceAllocator second = new SequenceAllocator(0);

        assertThat(first.next()).isEqualTo(1);
        assertThat(second.current()).isEqualTo(0);
    }
}
```

#### Why the solution works
With no shared state there is nothing an order or a thread could interleave: the counter belongs to the
allocator each test builds. The class has no `@TestMethodOrder`, every method passes alone, it can be
split or sharded freely, and enabling JUnit parallel execution changes nothing about the result.

#### Trade-offs
Independent tests construct more objects, which is negligible for a pure value type but real for an
expensive fixture. Where a resource genuinely must be shared, keep it read-only for the tests or give
each test its own slice of it (a distinct schema, a distinct key prefix) so parallel execution stays
safe.

#### How to detect it
Enable `junit.jupiter.execution.parallel.enabled=true` (or `MethodOrderer.Random`) on the suite and run
it a few times; an order-dependent class fails immediately. In CI, sharding or splitting a class across
agents surfaces the same defect. Grep for `@TestMethodOrder` and for `static` mutable fields in
production and test sources as a static check.

#### Interview follow-up
> Your suite is too slow, so you enable JUnit parallel execution. Which of your tests break first, and
> how do you make a suite parallel-safe without losing coverage?

#### Related
- Parallel test execution · Test isolation · CI sharding · Flaky test diagnosis

## Correct implementation

The production-ready counterpart lives in `lab.testing.sequence`:

- [`SequenceAllocator.java`](../../src/main/java/lab/testing/sequence/SequenceAllocator.java) — keeps
  the counter in an instance field seeded from `start`, so two allocators with the same start value
  advance independently; `next()` fails with an `ArithmeticException` at `Long.MAX_VALUE` instead of
  wrapping to a negative value, and `reset()` restores the counter to `start`.
- [`SequenceAllocatorTest.java`](../../src/test/java/lab/testing/sequence/SequenceAllocatorTest.java) —
  eleven behaviour tests that each construct their own allocator: the first allocation, consecutive
  allocations, a negative start, `current()` before and after allocating, `reset()` before and after
  allocating, two allocators with the same start that do not share state, and the values adjacent to
  `Long.MAX_VALUE`. No `@TestMethodOrder`, no static state: the class passes in any order, every method
  passes alone, and it is safe under parallel execution.

Walkthrough and trade-offs: [Testing — solutions](../../../../docs/topics/testing/solutions.md).
