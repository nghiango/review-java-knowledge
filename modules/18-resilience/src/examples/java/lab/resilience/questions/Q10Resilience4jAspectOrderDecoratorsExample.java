package lab.resilience.questions;

import java.util.List;

public class Q10Resilience4jAspectOrderDecoratorsExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Resilience4j default aspect evaluation order (outer to inner):
        // Retry -> CircuitBreaker -> RateLimiter -> TimeLimiter -> Bulkhead
        List<String> defaultOrder =
                List.of("Retry", "CircuitBreaker", "RateLimiter", "TimeLimiter", "Bulkhead");

        boolean retryIsOutermost = "Retry".equals(defaultOrder.get(0)); // true
        boolean bulkheadIsInnermost = "Bulkhead".equals(defaultOrder.get(4)); // true
        boolean circuitBreakerBeforeTimeLimiter =
                defaultOrder.indexOf("CircuitBreaker")
                        < defaultOrder.indexOf("TimeLimiter"); // true

        System.out.println(
                "Retry outermost: "
                        + retryIsOutermost
                        + ", Bulkhead innermost: "
                        + bulkheadIsInnermost
                        + ", CircuitBreaker wraps TimeLimiter: "
                        + circuitBreakerBeforeTimeLimiter);
    }
}
