# Review: Sequence allocator suite that only passes in one order

## Context

A team added `SequenceAllocator`, which hands out monotonically increasing numbers, together with a
unit test covering allocation, the current value and reset. The suite is green in CI and is treated as
the safety net for the allocator — but each test builds a fresh allocator and then asserts the absolute
number it expects to be handed out next, and those numbers only line up when the methods run in the
order declared in the class.

Both files are the review target: the allocator and the suite that covers it.

## Target Files

- [`SequenceAllocator.java`](SequenceAllocator.java)
- [`SequenceAllocatorTest.java`](SequenceAllocatorTest.java)

## Task

Review the two files as if they were a pull request. Identify every problem with the allocator and with
the suite. Consider:

- where the counter a test calls `next()` on actually lives, and who else can advance it
- whether a newly constructed allocator starts from the `start` value it was given, and what the class
  promises in its Javadoc
- what each expected value describes — a property of `next()`, or the position the method happens to
  occupy in the class
- what happens when the methods run in a different order, when one method runs alone, when the class
  runs twice in the same JVM, or when the suite is executed in parallel
- whether `@TestMethodOrder(OrderAnnotation.class)` documents a real sequence or hides a coupling
- what a second allocator — or a second test class in the same package — would observe
- how much of each assertion is about `SequenceAllocator` and how much about shared state

Write your findings down before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:12-testing:compileBrokenExamples
```
