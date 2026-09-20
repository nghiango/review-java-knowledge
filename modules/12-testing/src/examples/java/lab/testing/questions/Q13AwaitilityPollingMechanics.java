package lab.testing.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lab.testing.async.AsyncReportJob;
import lab.testing.async.ReportStatus;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Q13: How Awaitility's polling loop actually works.
 *
 * <p>{@code await().atMost(2, SECONDS).pollInterval(100, MILLISECONDS).untilAsserted(...)} is a
 * bounded loop: wait the poll delay (100 ms by default), evaluate, wait one poll interval, evaluate
 * again, and stop the moment the condition holds or the {@code atMost} ceiling is reached. So
 * {@code atMost} is a ceiling, not a delay — the fast path costs a couple of poll intervals, which
 * is why an awaited test is both fast and correct. {@code untilAsserted} re-runs the whole
 * assertion chain and swallows {@code AssertionError} between polls; any other exception aborts the
 * wait unless {@code ignoreExceptions()} says otherwise. The nested class is the shape the module's
 * {@code AsyncReportJobTest} uses; {@code main} builds the same configuration and does the
 * arithmetic without polling anything.
 */
public class Q13AwaitilityPollingMechanics {

    /** The awaited assertion, in the shape this module's async test uses. */
    static class AwaitedReportTest {

        private ExecutorService executor;
        private AsyncReportJob job;

        @BeforeEach
        void setUp() {
            executor = Executors.newSingleThreadExecutor();
            job = new AsyncReportJob(executor, Duration.ofMillis(20));
        }

        @AfterEach
        void tearDown() {
            job.close(); // shuts the executor down, so no worker thread survives the test
        }

        @Test
        @DisplayName("a submitted report reaches COMPLETED")
        void submit_report_reachesCompleted() {
            job.submit("RPT-1");

            await().atMost(Duration.ofSeconds(2))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(
                            () ->
                                    assertThat(job.status("RPT-1"))
                                            .isEqualTo(ReportStatus.COMPLETED));
        }
    }

    public static void main(String[] args) {
        Duration atMost = Duration.ofSeconds(2);
        Duration pollInterval = Duration.ofMillis(100);
        Duration pollDelay = Duration.ZERO;

        // await() only builds a ConditionFactory: nothing is polled until a condition is given.
        ConditionFactory configured =
                await().atMost(atMost).pollDelay(pollDelay).pollInterval(pollInterval);
        String factory = configured.getClass().getSimpleName(); // "ConditionFactory"

        long ceilingMillis = atMost.toMillis(); // 2000
        long intervalMillis = pollInterval.toMillis(); // 100
        long evaluationsWithinTheCeiling = ceilingMillis / intervalMillis + 1; // 21
        long fastPathMillis = 2 * intervalMillis; // the condition held on the third evaluation
        long sleepWasteMillis = ceilingMillis - fastPathMillis; // 1800
        boolean atMostIsACeiling = fastPathMillis < ceilingMillis; // true

        System.out.println("Factory: " + factory); // Factory: ConditionFactory
        System.out.println("Ceiling: " + ceilingMillis + " ms"); // Ceiling: 2000 ms
        System.out.println("Interval: " + intervalMillis + " ms"); // Interval: 100 ms
        System.out.println("Evaluations: " + evaluationsWithinTheCeiling); // Evaluations: 21
        System.out.println("Fast path: " + fastPathMillis + " ms"); // Fast path: 200 ms
        System.out.println("Sleep waste: " + sleepWasteMillis + " ms"); // Sleep waste: 1800 ms
        System.out.println("Is a ceiling: " + atMostIsACeiling); // Is a ceiling: true
    }
}
