# Review: Sequence allocator suite that only passes in one order

## Context

A team added `SequenceAllocator`, which hands out monotonically increasing numbers, together with a
unit test covering allocation, the current value and reset. The suite is green in CI and is treated
as the safety net for the allocator — but every test works on one `static` allocator, and each test
asserts the absolute number it expects to be handed out next.

`SequenceAllocator` itself is the class the correct implementation ships; the review target is the
test built around it.

## Target Files

- [`SequenceAllocator.java`](SequenceAllocator.java)
- [`SequenceAllocatorTest.java`](SequenceAllocatorTest.java)

## Task

Review the two files as if they were a pull request. Identify every problem with the suite and with
the way it shares the allocator. Consider:

- where the allocator a test calls `next()` on comes from, and who else can advance it
- what each expected value actually describes — a property of `next()`, or the position the method
  happens to occupy in the class
- what happens when the methods run in a different order, when one method runs alone, when the class
  runs twice in the same JVM, or when the suite is executed in parallel
- whether `@TestMethodOrder(OrderAnnotation.class)` documents a real sequence or hides a coupling
- whether the suite would still pass if each method built its own allocator, and what that says
  about the assertions
- what a second test class in the same package would observe if it used the same allocator
- how much of each assertion is about `SequenceAllocator` and how much about the shared counter

Write your findings down before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:12-testing:compileBrokenExamples
```
