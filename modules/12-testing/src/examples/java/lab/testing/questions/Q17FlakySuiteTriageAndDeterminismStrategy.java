package lab.testing.questions;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Q17: Triaging a flaky suite, and the determinism strategy that follows from it.
 *
 * <p>Triage is a ranking exercise: measure each test's failure rate over many runs, order by rate,
 * and group by cause. The cause decides the fix — a shared port needs a dynamic port, a sleep needs
 * an awaited condition, an assumed row order needs an explicit {@code Sort} — and a test with no
 * failure in the window stays untouched. The blast radius matters more than the rate: one test that
 * fails 1% of the time makes a 500-test suite red in almost every run, so "rare" flakes are not
 * rare at suite level.
 */
public class Q17FlakySuiteTriageAndDeterminismStrategy {

    /** One test's history: how often it ran, how often it failed, and the recorded cause. */
    record TestHistory(String test, int runs, int failures, String cause) {

        int flakeRatePercent() {
            return failures * 100 / runs;
        }

        String simpleClassName() {
            return test.substring(0, test.indexOf('#'));
        }
    }

    public static void main(String[] args) {
        List<TestHistory> history =
                List.of(
                        new TestHistory(
                                "OrderFulfilmentWireMockIT#canFulfil",
                                200,
                                37,
                                "fixed port already in use"),
                        new TestHistory(
                                "AsyncReportJobTest#submit_reportWithGatedWork",
                                200,
                                12,
                                "sleep shorter than the work"),
                        new TestHistory(
                                "AccountRepositoryIT#findAll_sorted",
                                200,
                                4,
                                "row order assumed without Sort"),
                        new TestHistory("OrderTotalsTest#total_multipleLines", 200, 0, "none"));

        List<TestHistory> ranked =
                history.stream()
                        .sorted(Comparator.comparingInt(TestHistory::flakeRatePercent).reversed())
                        .toList();

        Map<String, String> actionPerCause =
                Map.of(
                        "fixed port already in use",
                        "give the stub server a dynamic port and share one container per JVM",
                        "sleep shorter than the work",
                        "replace the sleep with a bounded awaited condition",
                        "row order assumed without Sort",
                        "request the order explicitly with Sort");

        TestHistory worst = ranked.get(0);
        int worstRate = worst.flakeRatePercent(); // 18
        String worstCause = worst.cause(); // "fixed port already in use"
        String secondWorst = ranked.get(1).simpleClassName(); // "AsyncReportJobTest"
        long flakyTests = history.stream().filter(test -> test.failures() > 0).count(); // 3
        long healthyTests = history.size() - flakyTests; // 1
        boolean worstIsASharedResource = worstCause.contains("port"); // true
        boolean actionKnown = actionPerCause.containsKey(worstCause); // true

        // Blast radius: a 500-test suite with one test failing 1% of the time.
        double passProbability = Math.pow(1 - 0.01, 500);
        long suiteFailurePermille = Math.round((1 - passProbability) * 1_000); // 993

        System.out.println("Worst rate: " + worstRate + "%"); // Worst rate: 18%
        System.out.println("Cause: " + worstCause); // Cause: fixed port already in use
        System.out.println("Second worst: " + secondWorst); // Second worst: AsyncReportJobTest
        System.out.println("Flaky tests: " + flakyTests); // Flaky tests: 3
        System.out.println("Healthy: " + healthyTests); // Healthy: 1
        System.out.println("Shared resource: " + worstIsASharedResource); // Shared resource: true
        System.out.println("Action known: " + actionKnown); // Action known: true
        System.out.println("Suite fail/1000: " + suiteFailurePermille); // Suite fail/1000: 993
    }
}
