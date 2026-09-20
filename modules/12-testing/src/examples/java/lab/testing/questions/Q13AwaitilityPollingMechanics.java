package lab.testing.questions;

import java.util.function.BooleanSupplier;

/**
 * Q13: How Awaitility's polling loop actually works.
 *
 * <p>{@code await().atMost(2, SECONDS).pollInterval(100, MILLISECONDS).until(condition)} is a
 * bounded loop: evaluate the condition, wait one interval, evaluate again, and stop the moment the
 * condition holds or the ceiling is reached. {@code atMost} is a ceiling, not a delay — the fast
 * path costs one poll interval, which is why an awaited test is fast *and* correct. {@code
 * untilAsserted} re-runs the whole assertion chain and swallows {@code AssertionError} between
 * polls; any other exception aborts the wait immediately. The loop below is the same shape with
 * virtual time, so the example never sleeps.
 */
public class Q13AwaitilityPollingMechanics {

    /**
     * What a poll loop observed: evaluations made, virtual time passed, and whether it succeeded.
     */
    record PollResult(int evaluations, long elapsedMillis, boolean satisfied) {}

    /** The loop Awaitility runs, with a virtual clock instead of a real one. */
    static PollResult poll(BooleanSupplier condition, long atMostMillis, long pollIntervalMillis) {
        long elapsed = 0;
        int evaluations = 0;
        while (elapsed <= atMostMillis) {
            evaluations++;
            if (condition.getAsBoolean()) {
                return new PollResult(evaluations, elapsed, true);
            }
            elapsed += pollIntervalMillis;
        }
        return new PollResult(evaluations, elapsed, false);
    }

    public static void main(String[] args) {
        // The condition holds on its third evaluation: the loop returns as soon as it does.
        int[] evaluations = {0};
        PollResult released = poll(() -> ++evaluations[0] >= 3, 2_000, 100);

        // A condition that never holds runs out the ceiling and fails instead of hanging.
        PollResult exhausted = poll(() -> false, 500, 100);

        // untilAsserted: the assertion throws AssertionError until the data arrives.
        int[] attempts = {0};
        PollResult asserted = poll(() -> ++attempts[0] > 2, 2_000, 100);
        int swallowedAssertionErrors = asserted.evaluations() - 1; // 2

        int releasedEvaluations = released.evaluations(); // 3
        long fastPathMillis = released.elapsedMillis(); // 200
        long ceilingMillis = 2_000; // what the author wrote in atMost()
        long sleepWasteMillis = ceilingMillis - fastPathMillis; // 1800
        int exhaustedEvaluations = exhausted.evaluations(); // 6
        long exhaustedMillis = exhausted.elapsedMillis(); // 600
        boolean timedOut = !exhausted.satisfied(); // true
        boolean atMostIsACeiling = released.satisfied() && fastPathMillis < ceilingMillis; // true

        System.out.println("Evaluations: " + releasedEvaluations); // Evaluations: 3
        System.out.println("Elapsed: " + fastPathMillis + " ms"); // Elapsed: 200 ms
        System.out.println("Satisfied: " + released.satisfied()); // Satisfied: true
        System.out.println("Timeout polls: " + exhaustedEvaluations); // Timeout polls: 6
        System.out.println(
                "Timeout elapsed: " + exhaustedMillis + " ms"); // Timeout elapsed: 600 ms
        System.out.println("Timed out: " + timedOut); // Timed out: true
        System.out.println("Swallowed errors: " + swallowedAssertionErrors); // Swallowed errors: 2
        System.out.println("Ceiling: " + atMostIsACeiling); // Ceiling: true
        System.out.println("Sleep waste: " + sleepWasteMillis + " ms"); // Sleep waste: 1800 ms
    }
}
