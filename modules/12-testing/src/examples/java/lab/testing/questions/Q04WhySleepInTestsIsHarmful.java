package lab.testing.questions;

/**
 * Q04: Why a fixed sleep in a test is harmful.
 *
 * <p>A sleep is a guess about how long work takes. Guess low and the assertion runs against
 * unfinished work — the test is flaky on a loaded CI agent. Guess high and every run of the suite
 * pays the guess. The numbers below are the two failure modes of the same guess; a wait that is
 * released by the work itself is neither too short nor wasteful.
 */
public class Q04WhySleepInTestsIsHarmful {

    /** How long a test waits for async work when it uses a fixed sleep. */
    private static final long SLEEP_MILLIS = 500;

    /** What a wait observed: when it stopped, and whether the work was finished by then. */
    record WaitOutcome(long waitedMillis, long observedAtMillis, boolean workFinished) {}

    /** Sleeps a fixed time, then asserts — the classic flake. */
    static WaitOutcome sleepThenAssert(long sleepMillis, long workMillis) {
        return new WaitOutcome(
                sleepMillis, Math.min(sleepMillis, workMillis), sleepMillis >= workMillis);
    }

    /** Waits for the work's own signal, then asserts. */
    static WaitOutcome signalThenAssert(long workMillis) {
        return new WaitOutcome(workMillis, workMillis, true);
    }

    public static void main(String[] args) {
        long fastWorkMillis = 200; // a warm developer laptop
        long slowWorkMillis = 800; // a loaded CI agent

        WaitOutcome fastRun = sleepThenAssert(SLEEP_MILLIS, fastWorkMillis);
        WaitOutcome slowRun = sleepThenAssert(SLEEP_MILLIS, slowWorkMillis);
        WaitOutcome signalledRun = signalThenAssert(fastWorkMillis);
        WaitOutcome bumpedRun = sleepThenAssert(2_000, fastWorkMillis); // "just sleep longer"

        long wastedPerTestMillis = fastRun.waitedMillis() - fastRun.observedAtMillis(); // 300
        int sleepsPerSuite = 500; // async assertions across the suite
        long perSuiteSeconds = sleepsPerSuite * wastedPerTestMillis / 1_000; // 150
        long signalWastedMillis =
                signalledRun.waitedMillis() - signalledRun.observedAtMillis(); // 0
        long bumpedWastedMillis = bumpedRun.waitedMillis() - bumpedRun.observedAtMillis(); // 1800
        long bumpedSuiteSeconds = sleepsPerSuite * bumpedWastedMillis / 1_000; // 900

        System.out.println(
                "Sleep passes fast: " + fastRun.workFinished()); // Sleep passes fast: true
        System.out.println(
                "Sleep passes slow: " + slowRun.workFinished()); // Sleep passes slow: false
        System.out.println(
                "Wasted per test: " + wastedPerTestMillis + " ms"); // Wasted per test: 300 ms
        System.out.println(
                "Wasted per suite: " + perSuiteSeconds + " s"); // Wasted per suite: 150 s
        System.out.println("Signal wasted: " + signalWastedMillis + " ms"); // Signal wasted: 0 ms
        System.out.println("Bumped suite: " + bumpedSuiteSeconds + " s"); // Bumped suite: 900 s
    }
}
