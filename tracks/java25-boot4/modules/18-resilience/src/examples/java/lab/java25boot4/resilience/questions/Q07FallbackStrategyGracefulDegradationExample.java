package lab.java25boot4.resilience.questions;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Q07: How should fallback strategies provide deterministic degraded contracts when upstream
 * dependencies fail?
 */
public class Q07FallbackStrategyGracefulDegradationExample {

    public static <T> T executeWithFallback(Supplier<T> primary, Function<Throwable, T> fallback) {
        try {
            return primary.get();
        } catch (Throwable t) {
            return fallback.apply(t);
        }
    }

    public static void main(String[] args) {
        String primaryResult =
                executeWithFallback(
                        () -> "LIVE_PAYMENT_CONFIRMATION", t -> "DEGRADED_PENDING_RECONCILIATION");

        String fallbackResult =
                executeWithFallback(
                        () -> {
                            throw new RuntimeException("Downstream timeout");
                        },
                        t -> "DEGRADED_PENDING_RECONCILIATION");

        System.out.println(
                "Primary execution: "
                        + primaryResult); // Primary execution: LIVE_PAYMENT_CONFIRMATION
        System.out.println(
                "Fallback execution: "
                        + fallbackResult); // Fallback execution: DEGRADED_PENDING_RECONCILIATION
    }
}
