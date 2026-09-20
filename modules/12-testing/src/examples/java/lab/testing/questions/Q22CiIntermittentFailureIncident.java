package lab.testing.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Q22: CI intermittent failure incident — the suite passes locally and fails in CI.
 *
 * <p>Symptom first: the same class is green on a laptop and red in roughly one CI run in seven,
 * always with "Address already in use" in the log rather than an assertion message. The triage is
 * to find the attribute that separates the red runs from the green ones — and, importantly, to test
 * the single attributes before the combination, because a hypothesis that only holds for a pair of
 * attributes is a different (and much more useful) finding than "CI is flaky". The fix keeps the
 * parallel reproduction in place, so the verification is meaningful.
 */
public class Q22CiIntermittentFailureIncident {

    /** One CI run of the same test class, with the attributes that could explain a failure. */
    record Run(
            int number,
            boolean parallel,
            int forks,
            boolean sharedAgent,
            long durationMillis,
            boolean failed) {}

    public static void main(String[] args) {
        List<Run> runs = new ArrayList<>();
        for (int run = 1; run <= 20; run++) {
            boolean parallel = run % 2 == 0; // the CI matrix shard that runs with forks = 4
            boolean sharedAgent = run % 6 == 0 || run == 3; // a second job landed on the same agent
            boolean failed = parallel && sharedAgent; // the fixed stub port was taken
            runs.add(
                    new Run(
                            run,
                            parallel,
                            parallel ? 4 : 1,
                            sharedAgent,
                            parallel ? 9_800 : 4_100,
                            failed));
        }

        long failedRuns = runs.stream().filter(Run::failed).count(); // 3
        long totalRuns = runs.size(); // 20
        int flakeRatePercent = (int) (failedRuns * 100 / totalRuns); // 15

        // Test each attribute on its own before believing the combination.
        boolean parallelAloneFits =
                runs.stream().allMatch(run -> run.failed() == run.parallel()); // false
        boolean agentAloneFits =
                runs.stream().allMatch(run -> run.failed() == run.sharedAgent()); // false
        boolean theCombinationFits =
                runs.stream()
                        .allMatch(
                                run ->
                                        run.failed()
                                                == (run.parallel() && run.sharedAgent())); // true

        String symptom = "Address already in use"; // the log line, not an assertion message
        boolean theSymptomIsResourceContention = symptom.contains("already in use"); // true
        int verificationRuns = 200; // after the fix: dynamic port + a fresh fixture per test
        int verificationFailures = 0; // with forks = 4 still enabled
        boolean fixed = verificationFailures == 0; // true
        int forksAfterTheFix = 4; // the reproduction is kept, so the fix stays honest

        System.out.println("Failed: " + failedRuns); // Failed: 3
        System.out.println("Runs: " + totalRuns); // Runs: 20
        System.out.println("Flake rate: " + flakeRatePercent + "%"); // Flake rate: 15%
        System.out.println("Parallel alone: " + parallelAloneFits); // Parallel alone: false
        System.out.println("Agent alone: " + agentAloneFits); // Agent alone: false
        System.out.println("Combination: " + theCombinationFits); // Combination: true
        System.out.println("Symptom: " + theSymptomIsResourceContention); // Symptom: true
        System.out.println("Verification runs: " + verificationRuns); // Verification runs: 200
        System.out.println("Fixed: " + fixed); // Fixed: true
        System.out.println("Forks after fix: " + forksAfterTheFix); // Forks after fix: 4
    }
}
