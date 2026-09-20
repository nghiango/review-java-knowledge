package lab.resilience.questions;

import java.time.Duration;

public class Q20HedgedRequestsAndTailLatencyTolerantExample {

    record HedgedConfig(Duration hedgeDelay, int maxHedges) {
        boolean shouldSendSecondCopy(long elapsedMs) {
            return elapsedMs >= hedgeDelay.toMillis();
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Jeff Dean's "The Tail at Scale": Send hedge request if initial copy exceeds p95 latency
        // (150ms)
        HedgedConfig config = new HedgedConfig(Duration.ofMillis(150), 2);

        boolean hedgeTriggeredAt200ms = config.shouldSendSecondCopy(200); // true
        boolean hedgeSkippedAt50ms = !config.shouldSendSecondCopy(50); // true

        System.out.println(
                "Hedge triggered at p95: "
                        + hedgeTriggeredAt200ms
                        + ", Skipped for fast responses: "
                        + hedgeSkippedAt50ms);
    }
}
