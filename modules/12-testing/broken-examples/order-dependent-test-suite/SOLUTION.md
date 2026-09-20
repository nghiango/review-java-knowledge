# Solution: Sequence allocator suite that only passes in one order

## Annotated code

### `SequenceAllocator.java`

```java
package lab.testing.broken.orderdependenttestsuite;

/**
 * Issues monotonically increasing sequence numbers from a counter.
 *
 * <p>{@code start} is the value the counter holds before the first allocation, so {@code new
 * SequenceAllocator(0).next()} returns 1 and {@link #current()} returns 0 until then. Allocation
 * stops at {@link Long#MAX_VALUE} with an {@link ArithmeticException} instead of wrapping to a
 * negative value.
 */
public final class SequenceAllocator {

    private final long start;
    private long counter;

    public SequenceAllocator(long start) {
        this.start = start;
        this.counter = start;
    }

    public long next() {
        counter = Math.incrementExact(counter);
        return counter;
    }

    public long current() {
        return counter;
    }

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
// coupling below. The class is green only because @Order(1), @Order(2) and @Order(4) advance the
// shared counter exactly as many times as @Order(2), @Order(3) and @Order(5) assume; run a method
// alone, reorder them, or let a random orderer pick, and the suite fails for a reason that has
// nothing to do with SequenceAllocator.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SequenceAllocatorTest {

    // Testing issue: the allocator — and therefore its counter — is a static field shared by every
    // method in the class, so a test's result depends on how many allocations the methods that ran
    // before it made. The class is not a set of independent units but one sequence split across
    // methods, and the values asserted below are absolute positions in that sequence rather than
    // properties of next().
    // One allocator for the class, so a test does not have to build its own.
    private static final SequenceAllocator ALLOCATOR = new SequenceAllocator(100);

    @Test
    @Order(1)
    void next_fromStart_returnsFirstValue() {
        assertEquals(101, ALLOCATOR.next());
    }

    // Maintainability issue: because the expected values are absolute and the counter is shared, the
    // class cannot be split, reordered or executed in parallel. Any JUnit configuration that runs
    // methods concurrently, or a second test class that touches the allocator, produces failures
    // that look like allocator defects and cost the team a triage round.
    @Test
    @Order(2)
    void next_afterFirstAllocation_returnsSecondValue() {
        assertEquals(102, ALLOCATOR.next());
    }

    @Test
    @Order(3)
    void current_afterTwoAllocations_returnsLastValue() {
        assertEquals(102, ALLOCATOR.current());
    }

    @Test
    @Order(4)
    void next_afterTwoAllocations_returnsThirdValue() {
        assertEquals(103, ALLOCATOR.next());
    }

    @Test
    @Order(5)
    void reset_afterThreeAllocations_restartsAtStart() {
        ALLOCATOR.reset();

        assertEquals(100, ALLOCATOR.current());
    }
}
```

`SequenceAllocator` carries no issue comments: it is the class the correct implementation ships (only
the package differs). The defect is entirely in the test above — which compiles and, executed as a
whole in the declared order, passes. That is why the coupling survived review.

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Testing issue | High | `SequenceAllocatorTest` (`@TestMethodOrder`, the absolute-value assertions) | The class passes only when the methods run in the declared order |
| 2 | Testing issue | High | `SequenceAllocatorTest.ALLOCATOR` (static) | Every method advances and asserts on one shared static counter |
| 3 | Maintainability issue | Medium | `SequenceAllocatorTest` | The class cannot be split, reordered or executed in parallel |

## Issue details

### The class passes only in the declared order

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5 `@TestMethodOrder`, `@Order` · **Interview frequency:** High · **Production impact:** High

**Location:** `SequenceAllocatorTest` (`@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` and the
`assertEquals(102, …)` / `assertEquals(103, …)` methods)

#### Problem
Each method asserts the absolute number it expects to be handed out next: `@Order(1)` expects 101,
`@Order(2)` expects 102, `@Order(4)` expects 103. Those numbers are positions in one shared sequence,
not properties of `next()`. The suite therefore has exactly one passing execution order, and
`@TestMethodOrder(OrderAnnotation.class)` is what supplies it.

#### Why it happens
The author wanted each test to pin down the value the allocator returns, and the value the shared
allocator returns depends on how many times it has already been called. Writing the expected number
down is the shortest way to assert that, and adding `@Order` makes the numbers line up — so the suite
turns green and the coupling is never questioned.

#### Production impact
```text
run a method alone (@Order(2))  → AssertionFailedError: expected: <102> but was: <101>
run under MethodOrderer.Random  → whichever method is not first fails
run the class twice in one JVM  → the counter starts wherever the previous run left it
→ the suite is red for reasons unrelated to SequenceAllocator, developers retry until green,
  and a real regression in next()/reset() is indistinguishable from the noise
```

#### Broken implementation
```java
private static final SequenceAllocator ALLOCATOR = new SequenceAllocator(100);

@Test @Order(1) void next_fromStart_returnsFirstValue()               { assertEquals(101, ALLOCATOR.next()); }
@Test @Order(2) void next_afterFirstAllocation_returnsSecondValue()   { assertEquals(102, ALLOCATOR.next()); }
@Test @Order(4) void next_afterTwoAllocations_returnsThirdValue()     { assertEquals(103, ALLOCATOR.next()); }
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
Each test creates the allocator it asserts on, so the sequence it exercises starts at a value the test
itself chose and is reachable from nowhere else. The expected values are consequences of the calls the
test makes, not of the method's position, so every method passes alone, in any order, and in parallel.
The class needs no `@TestMethodOrder`.

#### Trade-offs
Writing several `next()` calls in one method rather than spreading them across methods looks less
granular, and a failure now points at a method that covers a small sequence instead of a single
call. That is the price of independence; keep the methods small and give each one a `@DisplayName`
that names the progression it covers.

#### How to detect it
Any `@TestMethodOrder`/`@Order` pair in a class whose tests do not genuinely share a started resource
is a candidate. Run each method individually (`--tests 'Class.method'`) and replace the orderer with
`MethodOrderer.Random` for a few runs; a suite that fails is order-dependent. A second signal is an
assertion whose expected value is a bare absolute number with no call in the test that produces it.

#### Interview follow-up
> When is fixing the order of test methods legitimate, and how do you make that dependency explicit
> instead of encoding it in `@Order`?

#### Related
- Test isolation · Deterministic tests · JUnit 5 `@TestMethodOrder` · Flaky test diagnosis

### Every method shares one static allocator

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5 static state · **Interview frequency:** High · **Production impact:** High

**Location:** `SequenceAllocatorTest.ALLOCATOR`

#### Problem
`ALLOCATOR` is a single `static final SequenceAllocator`, so all five methods advance and read the
same counter. The tests are not independent units: what one method asserts depends on how many times
the methods that ran before it called `next()`. `static final` protects the reference, not the counter
inside it, and the counter lives for the whole JVM — including into a second run of the class.

#### Why it happens
One shared allocator feels like DRY: it is built once, and each test "just" calls it. Because the
field is `static final` it reads like a constant, and because the class is green as a whole nothing
signals that the state is being mutated by every method that runs.

#### Production impact
```text
test selection or order changes (IDE rerun of one method, parallel execution, another orderer,
a JUnit upgrade, a new test class in the package)
→ the counter is not where the method assumes it is
→ the suite fails for a reason unrelated to SequenceAllocator, or passes on a counter state a
  previous test left behind
→ developers lose trust in the suite, retry until green, and a real allocator regression ships
```

#### Broken implementation
```java
// One allocator for the class, so a test does not have to build its own.
private static final SequenceAllocator ALLOCATOR = new SequenceAllocator(100);

assertEquals(102, ALLOCATOR.next());
```

#### Correct implementation
```java
@Test
void next_fromStart_returnsStartPlusOne() {
    assertThat(new SequenceAllocator(0).next()).isEqualTo(1);
}
```

#### Why the solution works
The allocator a test asserts on is created inside that test and is reachable from nowhere else, so the
test has no input other than its own literals and no output anyone else can observe. The value type
being instance-scoped is what makes this possible: `SequenceAllocator` keeps its counter in a field,
so two allocators with the same start value advance independently.

#### Trade-offs
Constructing an allocator per test repeats a line per method, and a genuinely expensive fixture (a
started container, a migrated schema) cannot be rebuilt per test. Share only the *started resource*,
through `@BeforeAll` or an extension with an explicit lifecycle, and never share mutable state that an
assertion depends on.

#### How to detect it
Look for `static` fields whose type is mutable in test classes — an allocator, a counter, a `List`, a
`Map`, a mutable POJO. Then run each method on its own and run the class twice in the same JVM; a
suite that fails is sharing state. The sibling exercise
`broken-examples/shared-mutable-test-fixtures` shows the same defect with a shared list instead of a
counter.

#### Interview follow-up
> Why is a shared immutable value safe while a shared counter is not? How would you keep tests
> independent if constructing the data under test were expensive?

#### Related
- Test isolation · Fixture lifecycle · JUnit 5 static state · Test data builders

### The suite cannot be split, reordered or run in parallel

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic
**Technology:** JUnit 5 parallel execution · **Interview frequency:** Medium · **Production impact:** Medium

**Location:** `SequenceAllocatorTest` (the class as a whole)

#### Problem
Because the counter is shared and every expected value is absolute, the class can only be executed as
one ordered run. It cannot be split into two classes, reordered by the IDE, run with JUnit's parallel
execution enabled, or selected method-by-method — all of which are normal ways to shorten a build.
Any of them turns the suite red with failures that look like allocator defects.

#### Why it happens
Order dependence and parallelism are invisible on a single-threaded, whole-class run, which is how the
suite is normally executed. The cost only appears when the build is optimised — enabling JUnit
parallelism, sharding tests across CI agents, or re-running one method in the IDE — and by then the
suite is treated as correct.

#### Production impact
```text
build time grows → the team enables JUnit parallel execution (junit.jupiter.execution.parallel.enabled)
→ methods of SequenceAllocatorTest run concurrently on one counter
→ interleaved next() calls make the absolute assertions fail nondeterministically
→ the feature is switched off again "because the suite is flaky", and the build stays slow
```

#### Broken implementation
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SequenceAllocatorTest {

    private static final SequenceAllocator ALLOCATOR = new SequenceAllocator(100);

    @Test @Order(1) void next_fromStart_returnsFirstValue() { assertEquals(101, ALLOCATOR.next()); }
    @Test @Order(2) void next_afterFirstAllocation_returnsSecondValue() { assertEquals(102, ALLOCATOR.next()); }
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
With no shared state there is nothing an order or a thread could interleave. The class has no
`@TestMethodOrder`, every method passes alone, the class can be split or sharded freely, and enabling
JUnit parallel execution changes nothing about the result.

#### Trade-offs
Independent tests construct more objects, which is negligible for a pure value type but real for an
expensive fixture. Where a resource genuinely must be shared, keep it read-only for the tests or give
each test its own slice of it (a distinct schema, a distinct key prefix) so parallel execution stays
safe.

#### How to detect it
Enable `junit.jupiter.execution.parallel.enabled=true` (or `MethodOrderer.Random`) on the suite and run
it a few times; an order-dependent class fails immediately. In CI, sharding or splitting a class across
agents surfaces the same defect. Grep for `@TestMethodOrder` and `static` fields in test sources as a
static check.

#### Interview follow-up
> Your suite is too slow, so you enable JUnit parallel execution. Which of your tests break first, and
> how do you make a suite parallel-safe without losing coverage?

#### Related
- Parallel test execution · Test isolation · CI sharding · Flaky test diagnosis

## Correct implementation

The production-ready counterpart lives in `lab.testing.sequence`:

- [`SequenceAllocator.java`](../../src/main/java/lab/testing/sequence/SequenceAllocator.java) — keeps
  the counter in an instance field, so two allocators with the same start value advance independently;
  `next()` fails with an `ArithmeticException` at `Long.MAX_VALUE` instead of wrapping to a negative
  value, and `reset()` restores the counter to `start`.
- [`SequenceAllocatorTest.java`](../../src/test/java/lab/testing/sequence/SequenceAllocatorTest.java) —
  eleven behaviour tests that each construct their own allocator: the first allocation, consecutive
  allocations, a negative start, `current()` before and after allocating, `reset()` before and after
  allocating, two allocators that do not share state, and the values adjacent to `Long.MAX_VALUE`. No
  `@TestMethodOrder`, no static field: the class passes in any order, every method passes alone, and it
  is safe under parallel execution.

Walkthrough and trade-offs: [Testing — solutions](../../../../docs/topics/testing/solutions.md).
