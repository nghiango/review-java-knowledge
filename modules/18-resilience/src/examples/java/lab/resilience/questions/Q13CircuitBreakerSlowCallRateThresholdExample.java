package lab.resilience.questions;

import java.time.Duration;

public class Q13CircuitBreakerSlowCallRateThresholdExample {

    record CallMeasurement(Duration duration, boolean isError) {
        boolean isSlow(Duration slowCallThreshold) {
            return duration.compareTo(slowCallThreshold) >= 0;
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        Duration slowCallThreshold = Duration.ofSeconds(2);

        // A call that returns 200 OK after 3.5s is technically successful, but considered a "slow
        // call"
        CallMeasurement slowSuccess = new CallMeasurement(Duration.ofMillis(3500), false);
        CallMeasurement fastSuccess = new CallMeasurement(Duration.ofMillis(300), false);

        boolean isSlowSuccessRecorded = slowSuccess.isSlow(slowCallThreshold); // true
        boolean isFastSuccessRecorded =
                slowSuccess.isSlow(slowCallThreshold)
                        && !fastSuccess.isSlow(slowCallThreshold); // true

        // CircuitBreaker can trip to OPEN purely based on slowCallRateThreshold >= 50%
        System.out.println("Slow call recorded: " + isSlowSuccessRecorded);
    }
}
