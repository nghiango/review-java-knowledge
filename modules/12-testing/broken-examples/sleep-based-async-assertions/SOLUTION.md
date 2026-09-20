# Solution: Asynchronous report test that waits with Thread.sleep

## Annotated code

### `AsyncReportJob.java`

```java
package lab.testing.broken.sleepbasedasyncassertions;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lab.testing.async.ReportStatus;

/**
 * Runs report generation on a background thread and tracks the status of each report.
 *
 * <p>Kept deliberately small: this class accepts a report, runs the simulated generation on the
 * injected executor and records the outcome, so the companion test can observe the transition.
 */
public final class AsyncReportJob implements AutoCloseable {

    private final ExecutorService executor;
    private final Duration simulatedWork;
    private final Map<String, ReportStatus> statuses = new ConcurrentHashMap<>();

    public AsyncReportJob(ExecutorService executor, Duration simulatedWork) {
        this.executor = executor;
        this.simulatedWork = simulatedWork;
    }

    public ReportStatus submit(String reportId) {
        statuses.put(reportId, ReportStatus.QUEUED);
        executor.execute(() -> generate(reportId));
        return ReportStatus.QUEUED;
    }

    public ReportStatus status(String reportId) {
        return statuses.get(reportId);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private void generate(String reportId) {
        statuses.put(reportId, ReportStatus.RUNNING);
        try {
            CountDownLatch workGate = new CountDownLatch(1);
            workGate.await(simulatedWork.toMillis(), TimeUnit.MILLISECONDS);
            statuses.put(reportId, ReportStatus.COMPLETED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            statuses.put(reportId, ReportStatus.FAILED);
        }
    }
}
```

`AsyncReportJob` carries no issue comments: it is the class the correct implementation ships. The
defect is entirely in the test below, which is why the exercise is a review of *how* the test
synchronises rather than of the production code.

### `AsyncReportJobTest.java`

```java
package lab.testing.broken.sleepbasedasyncassertions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lab.testing.async.ReportStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AsyncReportJob}.
 *
 * <p>Generating a report is simulated to take half a second, so each test gives the background
 * thread time to finish before it checks the status it expects.
 */
// Reliability issue: the class only ever drives a report to COMPLETED. No test makes the work
// throw, so the FAILED transition is never executed: a job that swallowed the exception and left
// the report RUNNING, or marked it COMPLETED, would pass this suite unchanged.
class AsyncReportJobTest {

    // Maintainability issue: the delay the tests wait for is a magic number that has to track the
    // production simulation. Every run of the class pays it in full, whatever the machine, and a
    // change to REPORT_GENERATION silently turns both tests into timing guesses.
    private static final Duration REPORT_GENERATION = Duration.ofMillis(500);

    private ExecutorService executor;
    private AsyncReportJob job;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        job = new AsyncReportJob(executor, REPORT_GENERATION);
    }

    @AfterEach
    void tearDown() {
        job.close();
    }

    @Test
    void submit_thenWait_statusIsCompleted() throws InterruptedException {
        job.submit("RPT-1");

        // Testing issue: Thread.sleep(500) guesses how long the work takes instead of waiting for
        // the condition under test. It is exactly the simulated duration, so on a slower machine or
        // a loaded CI box the assertion runs before the worker has finished and fails for a reason
        // that has nothing to do with the job; on a fast machine it still passes if submit() ran the
        // work synchronously, so the test does not prove the work was asynchronous at all.
        Thread.sleep(500);

        assertEquals(ReportStatus.COMPLETED, job.status("RPT-1"));
    }

    @Test
    void submit_secondReport_statusIsCompleted() throws InterruptedException {
        job.submit("RPT-2");

        // Testing issue: the loop has no timeout and no exit for a terminal state other than
        // COMPLETED. If generation fails the status becomes FAILED, this condition stays true
        // forever and the loop spins for the life of the build; if it hangs, so does the test. The
        // build is killed by the CI timeout instead of failing with the status it observed.
        while (job.status("RPT-2") != ReportStatus.COMPLETED) {
            Thread.sleep(50);
        }

        assertEquals(ReportStatus.COMPLETED, job.status("RPT-2"));
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Testing issue | High | `AsyncReportJobTest.submit_thenWait_statusIsCompleted()` | `Thread.sleep(500)` guesses the duration instead of waiting for the asserted condition |
| 2 | Testing issue | High | `AsyncReportJobTest.submit_secondReport_statusIsCompleted()` | The polling loop has no timeout and never exits on `FAILED`, so a stuck report hangs the build |
| 3 | Reliability issue | High | `AsyncReportJobTest` | No test drives a report to `FAILED`, so the failure path is unverified |
| 4 | Maintainability issue | Medium | `AsyncReportJobTest` (`REPORT_GENERATION` and the fixed delays) | Hard-coded delays make every run pay the full simulated duration |

## Issue details

### Sleep-based waiting for asynchronous work

**Type:** Testing issue · **Severity:** High · **Difficulty:** Basic
**Technology:** JUnit 5, Awaitility · **Interview frequency:** High · **Production impact:** High

**Location:** `AsyncReportJobTest.submit_thenWait_statusIsCompleted()`

#### Problem
The test submits a report and then calls `Thread.sleep(500)` before asserting the status. The sleep
is not a synchronisation mechanism: it is a guess that the background work finishes within 500 ms.
Nothing in the test observes the work finishing, so the assertion races the worker thread.

#### Why it happens
Waiting for a background thread is awkward without a tool for it, and a sleep is the first thing that
works on a developer's machine. Because `REPORT_GENERATION` is 500 ms and the sleep is also 500 ms,
the test passes whenever the worker wins the race — which it usually does locally.

#### Production impact
```text
CI box under load, worker scheduled late
→ the assertion runs while the status is still RUNNING
→ AssertionFailedError: expected: <COMPLETED> but was: <RUNNING>
→ a green suite on the laptop, a red one on CI, and a retry that fixes it

work finishes in 10 ms
→ the test still sleeps 500 ms, and would pass even if submit() blocked the caller
→ the asynchronous contract is never actually verified
```

#### Broken implementation
```java
job.submit("RPT-1");

Thread.sleep(500);

assertEquals(ReportStatus.COMPLETED, job.status("RPT-1"));
```

#### Correct implementation
```java
job.submit("RPT-1");

await()
        .atMost(Duration.ofSeconds(2))
        .pollInterval(Duration.ofMillis(25))
        .untilAsserted(() -> assertThat(job.status("RPT-1")).isEqualTo(ReportStatus.COMPLETED));
```

Where the test wants to pin down a *transition* rather than just the end state, it releases the work
itself instead of estimating how long it takes:

```java
CountDownLatch release = new CountDownLatch(1);
job = new AsyncReportJob(executor, reportId -> release.await());

job.submit("RPT-1");

await()
        .atMost(Duration.ofSeconds(2))
        .pollInterval(Duration.ofMillis(25))
        .untilAsserted(() -> assertThat(job.status("RPT-1")).isEqualTo(ReportStatus.RUNNING));

release.countDown();

await()
        .atMost(Duration.ofSeconds(2))
        .pollInterval(Duration.ofMillis(25))
        .untilAsserted(() -> assertThat(job.status("RPT-1")).isEqualTo(ReportStatus.COMPLETED));
```

#### Why the solution works
`untilAsserted` polls the condition the test actually cares about and returns as soon as it holds, so
the test is fast when the work is fast and still correct when the work is slow. The bound (`atMost`)
turns a real hang into a failure with the last observed value instead of an infinite wait, and the
gate makes the RUNNING → COMPLETED transition deterministic rather than a race.

#### Trade-offs
Awaitility hides a polling loop, so a condition that never holds still costs the full `atMost`
budget. Keep the bound short (here 2 s), poll on the observable state (the status) rather than on a
proxy, and prefer releasing a gate where the transition itself is what the test is about. Do not
"fix" a slow test by raising `atMost`: if 2 s is not enough, the work or the assertion is wrong.

#### How to detect it
Search test sources for `Thread.sleep`, `TimeUnit.SECONDS.sleep`, `LockSupport.park*` and
`Thread.yield`; every hit is a synchronisation guess. Then run the class with the worker artificially
delayed (a slow executor, a debugger pause, a busy machine) — a suite that fails is waiting on time
instead of on state.

#### Interview follow-up
> Why is a fixed sleep both flaky and unable to prove that work was asynchronous? What would you
> assert instead, and what upper bound would you choose?

#### Related
- Awaitility · Deterministic tests · Flaky test diagnosis · Asynchronous assertions

### Unbounded polling loop masks a hang

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5, `Thread.sleep` polling · **Interview frequency:** High · **Production impact:** High

**Location:** `AsyncReportJobTest.submit_secondReport_statusIsCompleted()`

#### Problem
The second test polls with `while (job.status("RPT-2") != ReportStatus.COMPLETED)`. The loop has no
timeout, and `COMPLETED` is the only state that ends it. A report that never reaches `COMPLETED` —
because generation threw and the status is `FAILED`, or because the worker is stuck — keeps the loop
spinning forever.

#### Why it happens
An unbounded `while` around a sleep looks like it handles "eventually", and it does terminate on the
happy path. The author reasoned about the case that works and not about the state the loop can never
leave, which is exactly the case a safety net exists to catch.

#### Production impact
```text
generation throws → status becomes FAILED → the condition is still true
→ the loop sleeps 50 ms forever, the test method never returns
→ the JVM/build does not fail, it hangs until the CI job timeout kills it
→ no assertion message, no status reported: the failure is masked, and the run is charged as
  "infrastructure flake"
```

#### Broken implementation
```java
while (job.status("RPT-2") != ReportStatus.COMPLETED) {
    Thread.sleep(50);
}

assertEquals(ReportStatus.COMPLETED, job.status("RPT-2"));
```

#### Correct implementation
```java
await()
        .atMost(Duration.ofSeconds(2))
        .pollInterval(Duration.ofMillis(25))
        .untilAsserted(() -> assertThat(job.status("RPT-2")).isEqualTo(ReportStatus.COMPLETED));
```

#### Why the solution works
`atMost` makes the wait bounded: when the condition does not hold in time, Awaitility throws a
`ConditionTimeoutException` that reports the last observed status, so a hang becomes an ordinary,
diagnosable test failure. The loop cannot outlive the assertion it serves.

#### Trade-offs
A bound has to be chosen, and too generous a bound still hides slowness. Set it from the contract
("this must complete in two seconds"), not from the current machine's speed, and treat the first
`ConditionTimeoutException` in CI as a real defect rather than raising the number.

#### How to detect it
Look for `while` loops in tests that poll a status or a collection with no counter, deadline or
`atMost` around them, and for `Thread.sleep` inside a loop. A JUnit timeout (`@Timeout`) on the class
turns any such loop into a failure, but replacing the loop with a bounded await is the real fix.

#### Interview follow-up
> How does an unbounded poll turn a product bug into an infrastructure flake, and how would you make
> the failure report the state the test observed?

#### Related
- Awaitility · Bounded waits · `@Timeout` · CI flake triage

### No failure-path assertion on an asynchronous job

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5, asynchronous job lifecycle · **Interview frequency:** High · **Production impact:** High

**Location:** `AsyncReportJobTest` (the class as a whole)

#### Problem
Both tests only ever expect `COMPLETED`. Nothing submits a report whose generation throws, so the
`FAILED` transition is never executed and the assertion that distinguishes "still running" from
"will never finish" is never made.

#### Why it happens
The happy path is what the feature was written for, and the test was written alongside it. The
failure branch in `AsyncReportJob.generate` exists but is untested, so it is free to be wrong — for
example by leaving the report `RUNNING` after swallowing the exception, which is the exact failure a
caller polling the status would wait on forever.

#### Production impact
```text
generate() catches the exception and forgets to set FAILED (a refactor, a merge conflict)
→ a report stays RUNNING for ever
→ the caller polls the status, the operator sees "in progress", nothing is retried or alerted
→ the suite stays green, because it never made the work throw
```

#### Broken implementation
```java
// the whole suite, reduced to its two assertions
assertEquals(ReportStatus.COMPLETED, job.status("RPT-1"));
assertEquals(ReportStatus.COMPLETED, job.status("RPT-2"));
```

#### Correct implementation
```java
@Test
void submit_failingWork_reachesFailed() {
    job =
            new AsyncReportJob(
                    executor,
                    reportId -> {
                        throw new IllegalStateException("generator unavailable");
                    });

    job.submit("RPT-2");

    await()
            .atMost(Duration.ofSeconds(2))
            .pollInterval(Duration.ofMillis(25))
            .untilAsserted(() -> assertThat(job.status("RPT-2")).isEqualTo(ReportStatus.FAILED));
}
```

#### Why the solution works
The failing work throws inside the worker, so the production `catch` in `AsyncReportJob.generate` is
executed and the report must land in `FAILED`. The assertion now fails if the exception is swallowed,
if the status stays `RUNNING`, or if the wrong terminal state is recorded — the three ways this
branch can be wrong.

#### Trade-offs
Testing the failure path needs a seam that can make the work fail, here an injectable work function.
That seam has to stay honest: it must be the same code path production uses, not a test-only branch
inside `generate`. If the production work cannot be made to throw from a test, that is a design
signal about how the generator is wired, not a reason to skip the assertion.

#### How to detect it
For every asynchronous state machine, list its terminal states and check that a test reaches each
one. Coverage tools point at the `catch` block; mutation testing (flipping the status assignment)
proves the assertion is real. A `FAILED` (or `DEAD_LETTER`, `ABORTED`) constant with no test naming
it is the smell to look for.

#### Interview follow-up
> How would you test that an asynchronous job reports failure rather than hanging, without making
> the test slow or dependent on the machine?

#### Related
- Failure-path testing · Awaitility · Injectable seams · Mutation testing

### Fixed delays slow the suite and encode machine speed

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic
**Technology:** JUnit 5, `Duration` constants · **Interview frequency:** Medium · **Production impact:** Medium

**Location:** `AsyncReportJobTest` (`REPORT_GENERATION = Duration.ofMillis(500)` and both delays)

#### Problem
Every test waits a hard-coded delay: `Thread.sleep(500)` in the first and a 50 ms poll in the second.
The numbers must track the production simulation (`REPORT_GENERATION`), they are copied rather than
derived, and they are paid in full on every run regardless of how fast the work actually is.

#### Why it happens
The delay is the mechanism the tests use to wait, so its value looks like a detail of the test. In
fact it is a second, invisible copy of the timing contract: change the simulation to 800 ms and both
tests fail; shorten it to 50 ms and both tests pass while still sleeping half a second.

#### Production impact
```text
20 tests that each sleep 500 ms → +10 s of dead wall-clock time per run
→ the suite is slow, so it is run less often or parallelised, which makes the timing races worse
→ a developer "fixes" a failure by raising the sleep, and the suite gets slower and no more correct
```

#### Broken implementation
```java
private static final Duration REPORT_GENERATION = Duration.ofMillis(500);

Thread.sleep(500);          // first test
Thread.sleep(50);           // second test's poll interval
```

#### Correct implementation
```java
private static final Duration WORK_BUDGET = Duration.ofSeconds(2);
private static final Duration POLL_INTERVAL = Duration.ofMillis(25);

CountDownLatch release = new CountDownLatch(1);
job = new AsyncReportJob(executor, reportId -> release.await());

job.submit("RPT-1");

await()
        .atMost(WORK_BUDGET)
        .pollInterval(POLL_INTERVAL)
        .untilAsserted(() -> assertThat(job.status("RPT-1")).isEqualTo(ReportStatus.RUNNING));

release.countDown();
```

#### Why the solution works
The test no longer waits for the simulation to elapse; it releases the work and observes the
transition, so a test costs the time the work actually takes. `atMost` is an upper bound that is only
reached when something is wrong, and `pollInterval` is a bound on how quickly the test notices, not a
delay the happy path pays.

#### Trade-offs
A gate couples the test to the seam that exposes it. When there is no natural seam — a real HTTP call,
a database write — Awaitility polling is the right tool, and the suite then costs whatever the
boundary costs. The rule is not "never wait": it is "wait on the condition, with a bound, and never
on a number that has to be kept in sync with the implementation".

#### How to detect it
Sum the `Thread.sleep` arguments across the test sources and compare that with the suite's runtime;
a suite whose wall-clock time is dominated by sleeps has this problem. Grep for `Thread.sleep` with a
literal that also appears as a production constant, which is the copied-timing-contract smell.

#### Interview follow-up
> How do you keep a test that covers asynchronous behaviour fast without making it depend on the
> machine it runs on?

#### Related
- Awaitility · Test suite runtime · Deterministic tests · Test seams

## Correct implementation

The production-ready counterpart lives in `lab.testing.async`:

- [`ReportStatus.java`](../../src/main/java/lab/testing/async/ReportStatus.java) — the `QUEUED`,
  `RUNNING`, `COMPLETED`, `FAILED` lifecycle, with `COMPLETED` and `FAILED` documented as terminal so
  a caller can tell "still working" from "will not change again".
- [`AsyncReportJob.java`](../../src/main/java/lab/testing/async/AsyncReportJob.java) — runs generation
  on the injected executor, records `QUEUED`/`RUNNING`/`COMPLETED`/`FAILED` in a `ConcurrentHashMap`,
  records `FAILED` when the work throws instead of leaving the report `RUNNING`, and shuts the
  executor down in `close()`. The simulated work is a bounded, interruptible wait rather than a
  `Thread.sleep`, and a package-private work seam lets a test release the work (or make it throw).
- [`AsyncReportJobTest.java`](../../src/test/java/lab/testing/async/AsyncReportJobTest.java) — six
  tests: `submit` returns `QUEUED`, a gated report is observed `RUNNING` and then `COMPLETED`, failing
  work reaches `FAILED`, the default simulated work completes, a null report id is rejected, and
  `close()` shuts the worker down. Every asynchronous assertion is an
  `await().atMost(2 s).pollInterval(25 ms).untilAsserted(...)`; the class contains no `Thread.sleep`
  and passes repeatedly when run on its own.

Walkthrough and trade-offs: [Testing — solutions](../../../../docs/topics/testing/solutions.md).
