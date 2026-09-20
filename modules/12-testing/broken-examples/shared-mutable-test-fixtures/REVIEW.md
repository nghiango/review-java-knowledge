# Review: Order totals test built on a shared fixture

## Context

A team added `OrderTotals`, which sums the lines of an order and can add a shipping charge, together
with a unit test covering totals, shipping and empty orders. The test suite is green in CI and is
treated as the safety net for the totals — but the data every test works with lives in one static
`OrderFixture`, and one of the tests changes it.

The production classes (`Order`, `OrderLine`, `OrderTotals` in `lab.testing.orders`) are not the target
of this review: they are correct and are the same ones the correct implementation ships. The review
target is the fixture and the suite built on it.

## Target Files

- [`OrderFixture.java`](OrderFixture.java)
- [`OrderTotalsTest.java`](OrderTotalsTest.java)

## Task

Review the two files as if they were a pull request. Identify every problem with the fixture and with
the tests that use it. Consider:

- where the data a test asserts on comes from, and who else can see or change it
- what each test's input actually is, and whether reading it requires reading the other tests first
- what happens when the methods run in a different order, when one method runs alone, when the class
  runs twice in the same JVM, or when the suite is executed in parallel
- which test mutates shared state, and which test silently depends on that mutation
- whether `@TestMethodOrder(OrderAnnotation.class)` documents a real sequence or hides a coupling
- what a second test class in the same package would observe if it used the same fixture
- how much of each assertion is about `OrderTotals` and how much is about the fixture itself

Write your findings down before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:12-testing:compileBrokenExamples
```
