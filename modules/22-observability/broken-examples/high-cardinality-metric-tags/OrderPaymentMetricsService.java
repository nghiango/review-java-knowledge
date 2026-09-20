package lab.observability.broken.cardinalitytags;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class OrderPaymentMetricsService {

    private final MeterRegistry meterRegistry;

    public OrderPaymentMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPaymentAttempt(String orderId, String userId, String email, String method, boolean success) {
        // Tagging meters with unbounded high-cardinality values
        meterRegistry.counter(
                "payments.processed.total",
                "order_id", orderId,
                "user_id", userId,
                "customer_email", email,
                "payment_method", method,
                "status", success ? "SUCCESS" : "FAILED"
        ).increment();
    }
}
