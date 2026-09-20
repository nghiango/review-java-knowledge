# Review: Asynchronous report test that waits with Thread.sleep

## Context

A team added `AsyncReportJob`, which runs report generation on a background thread and exposes the
status of each report, together with a unit test for the asynchronous transition. The test passes in
CI and is treated as the safety net for the job's lifecycle — but it waits for the work with fixed
delays instead of for the condition it asserts, and it never exercises a report whose generation
fails.

`AsyncReportJob` is the production class as it stands *before* the test seam is added: it accepts only
an executor and a simulated delay, so no test can make its work fail and its `FAILED` branch is
unreachable. Both the test and the seam that makes the failure path testable are part of the review
target and of the fix.

## Target Files

- [`AsyncReportJob.java`](AsyncReportJob.java)
- [`AsyncReportJobTest.java`](AsyncReportJobTest.java)

## Task

Review the two files as if they were a pull request. Identify every problem with the test and with
the way it synchronises on asynchronous work. Consider:

- how the test decides that the background work has finished, and what happens when the work is
  slower or faster than the delay it waits for
- what happens when a report never finishes, or finishes in a state other than the one the test
  waits for
- whether the test can tell "not finished yet" apart from "failed"
- which transitions of the job's lifecycle are exercised, and which are never reached
- whether the test would still pass if `submit` ran the work on the calling thread
- what each test costs in wall-clock time, and whether that cost depends on the machine
- what the suite would look like if the report generation were replaced by a slower or a failing
  generator

Write your findings down before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:12-testing:compileBrokenExamples
```
