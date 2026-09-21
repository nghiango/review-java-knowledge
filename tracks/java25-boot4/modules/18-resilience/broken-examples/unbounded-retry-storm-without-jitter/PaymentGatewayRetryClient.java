package lab.java25boot4.resilience.broken.retrystorm;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Review target: Payment client retrying external API calls with unbounded attempts,
 * fixed interval delays, and zero randomized jitter.
 */
public class PaymentGatewayRetryClient {

    public interface RemotePaymentService {
        String processPayment(String transactionId, long amountCents);
    }

    private final RemotePaymentService remotePaymentService;
    private final AtomicInteger attempts = new AtomicInteger(0);

    public PaymentGatewayRetryClient(RemotePaymentService remotePaymentService) {
        this.remotePaymentService = remotePaymentService;
    }

    public String executePaymentWithRetry(String transactionId, long amountCents) {
        while (true) {
            try {
                attempts.incrementAndGet();
                return remotePaymentService.processPayment(transactionId, amountCents);
            } catch (Exception ex) {
                try {
                    // Fixed sleep without maximum backoff ceiling and without jitter
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
    }

    public int getAttempts() {
        return attempts.get();
    }
}
