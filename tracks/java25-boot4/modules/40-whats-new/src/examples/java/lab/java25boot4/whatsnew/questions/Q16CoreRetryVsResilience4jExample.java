package lab.java25boot4.whatsnew.questions;

/** Q16: what is core {@code @Retryable} and how does it relate to Resilience4j? */
public class Q16CoreRetryVsResilience4jExample {

    public static void main(String[] args) {
        int maxAttempts = 3;
        int attempts = 0;

        while (attempts < maxAttempts) {
            attempts++;
            if (attempts == 3) {
                break; // the call succeeds on the third attempt
            }
        }

        System.out.println(attempts); // 3 — bounded retry, never infinite
        // Boot 4 moves simple retry into core (@Retryable / RetryTemplate).
        // Resilience4j remains the choice for circuit breaker, bulkhead and rate limiter.
    }
}
