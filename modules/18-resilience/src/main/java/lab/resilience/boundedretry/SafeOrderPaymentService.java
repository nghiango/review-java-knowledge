package lab.resilience.boundedretry;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class SafeOrderPaymentService {

    private final OrderPaymentClient paymentClient;
    private final Retry retry;

    public SafeOrderPaymentService(OrderPaymentClient paymentClient) {
        this(paymentClient, createDefaultRetry());
    }

    public SafeOrderPaymentService(OrderPaymentClient paymentClient, Retry retry) {
        this.paymentClient = paymentClient;
        this.retry = retry;
    }

    public static Retry createDefaultRetry() {
        RetryConfig config =
                RetryConfig.custom()
                        .maxAttempts(3)
                        .intervalFunction(
                                IntervalFunction.ofExponentialRandomBackoff(
                                        Duration.ofMillis(50), 2.0, 0.5))
                        .retryExceptions(TransientGatewayException.class)
                        .ignoreExceptions(InvalidPaymentException.class)
                        .build();
        return Retry.of("orderPaymentRetry", config);
    }

    public PaymentResult processPayment(String orderId, double amount) {
        Supplier<PaymentResult> supplier =
                Retry.decorateSupplier(
                        retry,
                        () -> {
                            OrderPaymentClient.PaymentResponse response =
                                    paymentClient.charge(orderId, amount);
                            return new PaymentResult(response.transactionId(), true, "Success");
                        });

        return supplier.get();
    }

    public record PaymentResult(String transactionId, boolean success, String message) {}

    public static class TransientGatewayException extends RuntimeException {
        public TransientGatewayException(String message) {
            super(message);
        }
    }

    public static class InvalidPaymentException extends RuntimeException {
        public InvalidPaymentException(String message) {
            super(message);
        }
    }
}
