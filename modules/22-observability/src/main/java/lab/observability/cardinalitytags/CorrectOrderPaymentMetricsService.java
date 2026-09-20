package lab.observability.cardinalitytags;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CorrectOrderPaymentMetricsService {

    private static final Logger log =
            LoggerFactory.getLogger(CorrectOrderPaymentMetricsService.class);

    private final MeterRegistry meterRegistry;

    public CorrectOrderPaymentMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPaymentAttempt(
            String orderId, String userId, String email, String method, boolean success) {
        // Safe, bounded low-cardinality tags: method and status.
        // Bounded combinations prevent memory exhaustion in MeterRegistry and keep Prometheus
        // scrapes fast.
        String sanitizedMethod = normalizeMethod(method);
        String status = success ? "SUCCESS" : "FAILED";

        meterRegistry
                .counter(
                        "payments.processed.total",
                        "payment_method",
                        sanitizedMethod,
                        "status",
                        status)
                .increment();

        // High-cardinality attributes (orderId, userId, email) are recorded in structured logs and
        // traces
        log.info(
                "Payment attempt evaluated orderId={} userId={} emailHash={} method={} status={}",
                orderId,
                userId,
                hashEmail(email),
                sanitizedMethod,
                status);
    }

    private String normalizeMethod(String method) {
        if (method == null) {
            return "UNKNOWN";
        }
        String upper = method.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "CARD", "CREDIT_CARD" -> "CARD";
            case "PAYPAL" -> "PAYPAL";
            case "BANK_TRANSFER" -> "BANK_TRANSFER";
            default -> "OTHER";
        };
    }

    private String hashEmail(String email) {
        if (email == null) {
            return "none";
        }
        return Integer.toHexString(email.toLowerCase(Locale.ROOT).trim().hashCode());
    }
}
