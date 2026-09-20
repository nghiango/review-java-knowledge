package lab.resilience.questions;

public class Q05SlidingWindowCountVsTimeBasedExample {

    record WindowMetrics(int windowSize, int failures, int totalCalls) {
        float failureRate() {
            return (float) failures / totalCalls * 100.0f;
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // COUNT_BASED: fixed N calls (e.g. last 100 calls)
        WindowMetrics countBased = new WindowMetrics(100, 60, 100);
        float countFailureRate = countBased.failureRate(); // 60.0f

        // TIME_BASED: last N seconds (e.g. 10 seconds), calls bucketed per epoch second
        WindowMetrics timeBased = new WindowMetrics(10, 25, 50);
        float timeFailureRate = timeBased.failureRate(); // 50.0f

        boolean countTripped = countFailureRate >= 50.0f; // true
        boolean timeTripped = timeFailureRate >= 50.0f; // true

        System.out.println("Count tripped: " + countTripped + ", Time tripped: " + timeTripped);
    }
}
