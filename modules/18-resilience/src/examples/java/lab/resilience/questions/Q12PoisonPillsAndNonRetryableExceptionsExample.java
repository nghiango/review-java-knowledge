package lab.resilience.questions;

import java.util.Set;

public class Q12PoisonPillsAndNonRetryableExceptionsExample {

    record ErrorClassification(int statusCode, boolean isRetryable) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Transient errors (retryable): 503, 504, 429
        Set<Integer> retryableCodes = Set.of(503, 504, 429);

        // Deterministic poison pills (non-retryable): 400, 401, 403, 422
        ErrorClassification malformedJson =
                new ErrorClassification(400, retryableCodes.contains(400));
        ErrorClassification gatewayTimeout =
                new ErrorClassification(504, retryableCodes.contains(504));

        boolean malformedIsPoisonPill = !malformedJson.isRetryable(); // true
        boolean timeoutIsRetryable = gatewayTimeout.isRetryable(); // true

        System.out.println(
                "400 is non-retryable poison pill: "
                        + malformedIsPoisonPill
                        + ", 504 is transient: "
                        + timeoutIsRetryable);
    }
}
