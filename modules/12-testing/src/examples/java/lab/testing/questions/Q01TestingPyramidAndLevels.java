package lab.testing.questions;

import java.util.List;

/**
 * Q01: What the testing pyramid actually prescribes, and what each level costs.
 *
 * <p>The pyramid is a statement about *distribution and cost*, not about frameworks: many fast
 * in-process tests, fewer tests that cross a process boundary, very few end-to-end tests. The same
 * 486 tests arranged as an inverted pyramid take almost four times as long to give an answer, which
 * is the whole reason the shape is worth defending.
 */
public class Q01TestingPyramidAndLevels {

    /** One level of the pyramid: how many tests it holds and what each of them costs to run. */
    record TestLevel(
            String name,
            int tests,
            long startupMillis,
            long perTestMillis,
            boolean crossesProcessBoundary) {}

    public static void main(String[] args) {
        List<TestLevel> pyramid =
                List.of(
                        new TestLevel("unit", 420, 0, 1, false),
                        new TestLevel("integration", 60, 8_000, 60, true),
                        new TestLevel("end-to-end", 6, 45_000, 400, true));

        // The same 486 tests, redistributed towards the top of the pyramid.
        List<TestLevel> inverted =
                List.of(
                        new TestLevel("unit", 60, 0, 1, false),
                        new TestLevel("integration", 6, 8_000, 60, true),
                        new TestLevel("end-to-end", 420, 45_000, 400, true));

        int totalTests = pyramid.stream().mapToInt(TestLevel::tests).sum(); // 486
        int unitSharePercent = pyramid.get(0).tests() * 100 / totalTests; // 86
        long boundaryLevels =
                pyramid.stream().filter(TestLevel::crossesProcessBoundary).count(); // 2

        long pyramidMillis = estimatedSuiteMillis(pyramid); // 420 + 11_600 + 47_400
        long invertedMillis = estimatedSuiteMillis(inverted); // 60 + 8_360 + 213_000
        long extraMillis = invertedMillis - pyramidMillis; // 162_000

        System.out.println("Suite tests: " + totalTests); // Suite tests: 486
        System.out.println("Unit share: " + unitSharePercent + "%"); // Unit share: 86%
        System.out.println("Boundary levels: " + boundaryLevels); // Boundary levels: 2
        System.out.println("Pyramid suite: " + pyramidMillis + " ms"); // Pyramid suite: 59420 ms
        System.out.println(
                "Inverted suite: " + invertedMillis + " ms"); // Inverted suite: 221420 ms
        System.out.println("Time lost: " + extraMillis + " ms"); // Time lost: 162000 ms
    }

    /**
     * Estimates the wall clock of a suite: each level pays its startup once, then every test pays
     * its own cost.
     */
    private static long estimatedSuiteMillis(List<TestLevel> levels) {
        return levels.stream()
                .mapToLong(level -> level.startupMillis() + level.tests() * level.perTestMillis())
                .sum();
    }
}
