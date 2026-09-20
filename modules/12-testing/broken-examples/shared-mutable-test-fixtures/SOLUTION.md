# Solution: Order totals test built on a shared fixture

## Annotated code

### `OrderFixture.java`

```java
package lab.testing.broken.sharedmutabletestfixtures;

import java.util.ArrayList;
import java.util.List;
import lab.testing.orders.Order;
import lab.testing.orders.OrderLine;
import lab.testing.pricing.Money;

/**
 * The orders the order tests work with.
 *
 * <p>Kept in one place so that a test does not have to describe a basket of its own: ORD-1001 is two
 * BOOK-001 at 1999 cents, ORD-1002 is three PEN-042 at 250 cents, and SHIPPING is the flat delivery
 * charge the order tests use.
 */
final class OrderFixture {

    // Testing issue: the fixture hands every test in the class the same mutable list, so one test's
    // output becomes another test's input. The suite's result then depends on what ran before it, a
    // failure is attributed to the wrong test, and a test can pass on data an earlier test left behind.
    // Design issue: the fixture has no ownership or lifecycle. A static collection lives as long as the
    // JVM, so whatever a test appends stays visible to every later test — including tests in other
    // classes of this package — and nothing resets it between runs.
    static final List<Order> ORDERS =
            new ArrayList<>(
                    List.of(
                            new Order("ORD-1001", List.of(new OrderLine("BOOK-001", 1999, 2))),
                            new Order("ORD-1002", List.of(new OrderLine("PEN-042", 250, 3)))));

    static final Money SHIPPING = new Money(499);

    private OrderFixture() {}

    static Order emptyOrder() {
        return new Order("ORD-1003", List.of());
    }

    static Order multiLineOrder() {
        return new Order(
                "ORD-1004",
                List.of(new OrderLine("BOOK-001", 1999, 2), new OrderLine("PEN-042", 250, 3)));
    }
}
```

### `OrderTotalsTest.java`

```java
package lab.testing.broken.sharedmutabletestfixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lab.testing.orders.OrderTotals;
import lab.testing.pricing.Money;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests for {@link OrderTotals}.
 *
 * <p>The orders the tests work with come from {@link OrderFixture}, and the methods run in the order
 * declared here so the suite is deterministic in CI.
 */
// Testing issue: @TestMethodOrder(OrderAnnotation.class) pins the execution order and hides the
// coupling below. The class is green only because @Order(4) always appends the third order before
// @Order(5) reads it; run that method alone, reorder the methods, or let a random orderer pick, and
// the suite fails for a reason that has nothing to do with OrderTotals.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderTotalsTest {

    private final OrderTotals totals = new OrderTotals();

    @Test
    @Order(1)
    void total_singleLineOrder_returnsLineTotal() {
        assertEquals(new Money(3998), totals.total(OrderFixture.ORDERS.get(0)));
    }

    @Test
    @Order(2)
    void total_emptyOrder_returnsZero() {
        assertEquals(Money.ZERO, totals.total(OrderFixture.emptyOrder()));
    }

    @Test
    @Order(3)
    void totalWithShipping_addsShippingToOrderTotal() {
        assertEquals(
                new Money(1249),
                totals.totalWithShipping(OrderFixture.ORDERS.get(1), OrderFixture.SHIPPING));
    }

    @Test
    @Order(4)
    void total_multiLineOrder_sumsEveryLine() {
        // Maintainability issue: with no builder to describe its own input, the test edits the shared
        // fixture and reads its own entry back. What the test depends on is implicit, the fixture is
        // changed by a test that does not own it, and every test that runs afterwards inherits the
        // change.
        OrderFixture.ORDERS.add(OrderFixture.multiLineOrder());

        assertEquals(
                new Money(4748), totals.total(OrderFixture.ORDERS.get(OrderFixture.ORDERS.size() - 1)));
    }

    @Test
    @Order(5)
    void totalWithShipping_largestOrder_usesFixtureShipping() {
        // The third order exists only because @Order(4) appended it, so this assertion measures the
        // fixture's accumulated state rather than the behaviour of OrderTotals.
        assertEquals(3, OrderFixture.ORDERS.size());

        assertEquals(
                new Money(5247),
                totals.totalWithShipping(OrderFixture.ORDERS.get(2), OrderFixture.SHIPPING));
    }
}
```

`Order`, `OrderLine` and `OrderTotals` (package `lab.testing.orders`) carry no issue comments: they are
correct and are the classes the correct implementation ships. The defect is entirely in the fixture and
the suite — which compiles and, executed as a whole, passes. That is why the coupling survived review.

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Testing issue | High | `OrderFixture.ORDERS` (used by every method of `OrderTotalsTest`) | All tests read and write one static mutable list of orders |
| 2 | Testing issue | High | `OrderTotalsTest` (`@TestMethodOrder`, `totalWithShipping_largestOrder_usesFixtureShipping()`) | The class only passes in the annotated order: `@Order(5)` depends on the order `@Order(4)` appended |
| 3 | Maintainability issue | Medium | `OrderTotalsTest.total_multiLineOrder_sumsEveryLine()` | No per-test data builder, so the test edits the shared fixture to obtain its input |
| 4 | Design issue | Medium | `OrderFixture` | A static fixture with no ownership or lifecycle leaks state between tests and test classes |

## Issue details

### Tests share a mutable static fixture

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5 · **Interview frequency:** High · **Production impact:** High

**Location:** `OrderFixture.ORDERS`, read by every method of `OrderTotalsTest`

#### Problem
`OrderFixture.ORDERS` is a single mutable `List<Order>` shared by the whole class. Tests are no longer
independent units: what one test asserts depends on what earlier tests did to that list. The list is
also the only description of the data a test uses, so the test method itself no longer states its own
input.

#### Why it happens
A shared fixture feels like DRY: the baskets are written once, and each test "just" picks one. Because
the collection is declared `static final`, it reads like a constant, and because the tests pass when the
class runs as a whole, nothing signals that the data is being mutated.

#### Production impact
```text
test selection or order changes (IDE rerun of one method, parallel execution, another orderer,
a JUnit upgrade, a new test class in the package)
→ @Order(5) reads a list that no longer contains the order @Order(4) appended
→ the suite fails for a reason unrelated to OrderTotals, or passes on data left behind by a
  previous test
→ developers lose trust in the suite, retry until green, and a real totals regression ships
```

#### Broken implementation
```java
static final List<Order> ORDERS = new ArrayList<>(List.of(/* ... */));

// in the test:
assertEquals(new Money(3998), totals.total(OrderFixture.ORDERS.get(0)));
```

#### Correct implementation
```java
Order order = OrderTestData.anOrder("ORD-1001").withLine("BOOK-001", 1999, 2).build();

assertThat(totals.total(order)).isEqualTo(new Money(3998));
```

#### Why the solution works
The data a test asserts on is created inside that test and is reachable from nowhere else, so the test
has no input other than its own literals and no output anyone else can observe. Any method can run
first, alone, repeatedly or in parallel with the same result.

#### Trade-offs
Duplicating literals per test looks less tidy than one shared list, and each test pays a few lines for
its own data. That is the price of independence; keep it cheap with a builder (issue 3) rather than with
shared mutable values, and share only immutable values (here `Money`) or genuinely expensive started
resources.

#### How to detect it
Look for `static` fields whose type is mutable (`List`, `Map`, `Set`, an array, a mutable POJO) in test
or fixture classes. Then run each test method on its own and run the class with
`MethodOrderer.Random` — order-sensitive suites fail immediately.

#### Interview follow-up
> Why is a shared immutable value safe while a shared mutable collection is not? How would you keep test
> data independent if constructing it were expensive (a started container, a seeded database)?

#### Related
- Test isolation · Test data builders · Deterministic tests · JUnit 5 lifecycle

### Test order dependence hidden by an explicit method order

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5 `@TestMethodOrder`, `@Order` · **Interview frequency:** High · **Production impact:** High

**Location:** `OrderTotalsTest` (`@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`,
`totalWithShipping_largestOrder_usesFixtureShipping()`)

#### Problem
`totalWithShipping_largestOrder_usesFixtureShipping` asserts that the fixture holds three orders and
totals `ORDERS.get(2)`. That third order exists only because `total_multiLineOrder_sumsEveryLine`
appended it. The dependency is invisible in the test and is made to work by
`@TestMethodOrder(OrderAnnotation.class)` plus `@Order(n)`.

#### Why it happens
The annotation was added to make CI deterministic — a reasonable goal — but it also freezes the order
the coupling needs. `@Order` documents a sequence instead of removing the need for one, so the suite
passes while the underlying state sharing stays.

#### Production impact
```text
@Order(4) runs before @Order(5)  → three orders in the fixture → assertion passes
@Order(5) run alone              → AssertionFailedError: expected: <3> but was: <2>
annotation replaced by a random orderer → the method fails whenever @Order(4) does not come first
```

#### Broken implementation
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderTotalsTest {
    @Test @Order(4) void total_multiLineOrder_sumsEveryLine() { OrderFixture.ORDERS.add(...); }
    @Test @Order(5) void totalWithShipping_largestOrder_usesFixtureShipping() {
        assertEquals(3, OrderFixture.ORDERS.size());
    }
}
```

#### Correct implementation
```java
class OrderTotalsTest {
    @Test
    void totalWithShipping_orderWithLines_addsShipping() {
        Order order = OrderTestData.anOrder("ORD-1005").withLine("PEN-042", 250, 3).build();
        assertThat(totals.totalWithShipping(order, new Money(499))).isEqualTo(new Money(1249));
    }
}
```

#### Why the solution works
With no shared state there is nothing for an order to protect: the class has no `@TestMethodOrder`, and
each method passes alone, in any order, and in parallel. The suite is deterministic because the tests
are independent, not because the runner was told what to do.

#### Trade-offs
Order annotations are legitimate when the sequence itself is the contract — a container started once, a
schema migrated before the tests that use it. Express that dependency with `@BeforeAll` or an extension
so it is visible in one place, rather than as an implicit `@Order` chain between test methods.

#### How to detect it
Any `@TestMethodOrder`/`@Order` pair in a class whose tests do not genuinely share a resource is a
candidate. Run each method individually (`--tests 'Class.method'`) and replace the orderer with
`MethodOrderer.Random` for a few runs; a suite that fails is order-dependent.

#### Interview follow-up
> When is it legitimate to fix the order of test methods, and how do you make that dependency explicit
> instead of encoding it in `@Order`?

#### Related
- Test isolation · Deterministic tests · JUnit 5 `@TestMethodOrder` · Flaky test diagnosis

### No per-test data builder; a test edits the shared fixture

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic
**Technology:** JUnit 5 test data builders · **Interview frequency:** Medium · **Production impact:** Medium

**Location:** `OrderTotalsTest.total_multiLineOrder_sumsEveryLine()`

#### Problem
There is no way to describe an order inline, so the test that needs a multi-line basket appends one to
`OrderFixture.ORDERS` and then reads it back with `get(size() - 1)`. The test's input is not visible in
the test, the fixture is modified by a test that does not own it, and every test that runs afterwards
inherits the change.

#### Why it happens
Without a builder, the cheapest way to get data is to reuse the shared collection, and adding a factory
method to the fixture (`multiLineOrder()`) looks like the fix — but it still publishes the value through
the shared list, so the coupling remains.

#### Production impact
```text
a new test asserts on the fixture (or on its size)
→ it must run after the mutating test to see the data it expects
→ the suite can no longer be split, reordered or parallelised
→ test files are edited to change other test files' data, and failures appear far from their cause
```

#### Broken implementation
```java
OrderFixture.ORDERS.add(OrderFixture.multiLineOrder());

assertEquals(
        new Money(4748), totals.total(OrderFixture.ORDERS.get(OrderFixture.ORDERS.size() - 1)));
```

#### Correct implementation
```java
Order order =
        OrderTestData.anOrder("ORD-1002")
                .withLine("BOOK-001", 1999, 2)
                .withLine("PEN-042", 250, 3)
                .build();

assertThat(totals.total(order)).isEqualTo(new Money(4748));
```

#### Why the solution works
The builder returns a fresh value per call, so the test states exactly the basket it needs and nothing is
published anywhere. Adding a line, changing a price or writing a new test has no effect on any other
test.

#### Trade-offs
A builder is code to maintain and can grow defaults that hide the values a test depends on. Keep it
dumb: no hidden defaults for data under assertion, package-private in the test source set, and with one
method per thing the domain actually has.

#### How to detect it
Search test methods for writes to fixture state: `fixture.add(`, `put(`, `remove(`, `clear()`, `set…(`
on a field the test did not create. Also search for `.get(size() - 1)` and index arithmetic, which is
usually a sign that a test is reading data another test published.

#### Interview follow-up
> How would you design test data builders so that a test's input is obvious from the test body alone,
> without hidden defaults?

#### Related
- Test data builders · Object Mother · Test isolation · Readability of tests

### Fixture has no ownership or lifecycle

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate
**Technology:** JUnit 5 static fixtures · **Interview frequency:** Medium · **Production impact:** Medium

**Location:** `OrderFixture`

#### Problem
`OrderFixture` is a package-private class of static state with no owner and no reset. `static final`
protects the reference, not the contents: any test in the package can add to or remove from `ORDERS`,
the list lives for the whole JVM, and nothing bounds its contents to a single test or restores them.

#### Why it happens
A "test data" class is usually written as a bag of constants, and a `static final` collection looks like
a constant. Because the fixture is a separate file that tests merely read from, the design question —
who owns this data, when is it valid, who resets it — is never asked.

#### Production impact
```text
a second test class in lab.testing.broken.sharedmutabletestfixtures uses OrderFixture
→ it observes the order appended by OrderTotalsTest (or its own writes break OrderTotalsTest)
→ adding one test file changes the outcome of another, and the failure surfaces far from its cause
```

#### Broken implementation
```java
final class OrderFixture {
    static final List<Order> ORDERS = new ArrayList<>(List.of(/* ... */));
}
```

#### Correct implementation
```java
// per-test factory in the test source set — no static state at all
Order order = OrderTestData.anOrder("ORD-1001").withLine("BOOK-001", 1999, 2).build();

// and the value type copies its lines, so even a shared order cannot be mutated
public record Order(String id, List<OrderLine> lines) {
    public Order {
        Objects.requireNonNull(id, "id must not be null");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines must not be null"));
    }
}
```

#### Why the solution works
The container that needed ownership is gone: data is created where it is used, and the value type is
immutable, so no test can change data another test asserts on. Lifecycle questions disappear because
there is nothing with a lifecycle.

#### Trade-offs
Some fixtures are genuinely expensive and must be shared — a started container, a migrated schema, a
seeded database. Share the *started resource* through `@BeforeAll`, a JUnit extension or a Testcontainers
singleton, keep it immutable or reset it between tests, and never let assertions depend on state that
one test wrote for another.

#### How to detect it
List every `static` mutable field in the test sources and answer "who resets this, and when?" If the
answer is "nobody" or "the next test does", it is leaked state. A fixture class with only static
mutable members and a private constructor is the pattern to look for.

#### Interview follow-up
> Which test fixtures are legitimately shared across a suite, and how do you keep a shared resource from
> leaking state into assertions?

#### Related
- Test isolation · Fixture lifecycle · JUnit 5 extensions · Testcontainers singletons

## Correct implementation

The production-ready counterpart lives in `lab.testing.orders`:

- [`OrderLine.java`](../../src/main/java/lab/testing/orders/OrderLine.java) — line record; non-null SKU
  and non-negative price and quantity are enforced in the compact constructor.
- [`Order.java`](../../src/main/java/lab/testing/orders/Order.java) — order record that takes a
  defensive `List.copyOf` of its lines, so an order value can be shared safely but never changed.
- [`OrderTotals.java`](../../src/main/java/lab/testing/orders/OrderTotals.java) — stateless `total` and
  `totalWithShipping` over [`Money`](../../src/main/java/lab/testing/pricing/Money.java), the immutable
  cents value object reused from the pricing example.
- [`OrderTestData.java`](../../src/test/java/lab/testing/orders/OrderTestData.java) — test-only fluent
  builder in the test source set; every call returns a fresh builder, so there is no static state and
  each test creates the data it asserts on.
- [`OrderTotalsTest.java`](../../src/test/java/lab/testing/orders/OrderTotalsTest.java) — nine tests
  covering a single-line total, several lines, an empty order, a zero-quantity line, shipping with and
  without lines, rejected null input, and the independence and immutability of built data. No
  `@TestMethodOrder`, no shared fixture: the class passes in any order and every method passes alone.

Walkthrough and trade-offs: [Testing — solutions](../../../../docs/topics/testing/solutions.md).
