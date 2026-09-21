# Solution: Unbounded Retry Storm Without Jitter

## Categorized Issues

1. `// Reliability issue: Unbounded while(true) retry loop causes infinite task execution, thread exhaustion, and memory leaks.`
2. `// Concurrency issue: Fixed 100ms sleep without randomized jitter synchronizes retrying callers into thundering-herd retry storms against recovering downstream services.`
3. `// Reliability issue: Catching generic Exception retries non-retryable fatal errors (e.g. 400 Bad Request, validation errors, card declined) instead of failing fast.`

## Annotated Code

```java
package lab.java25boot4.resilience.broken.retrystorm;

import java.util.concurrent.atomic.AtomicInteger;

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
        // Reliability issue: Unbounded while(true) retry loop causes infinite task execution, thread exhaustion, and memory leaks.
        while (true) {
            try {
                attempts.incrementAndGet();
                return remotePaymentService.processPayment(transactionId, amountCents);
            // Reliability issue: Catching generic Exception retries non-retryable fatal errors (e.g. 400 Bad Request, validation errors, card declined) instead of failing fast.
            } catch (Exception ex) {
                try {
                    // Concurrency issue: Fixed 100ms sleep without randomized jitter synchronizes retrying callers into thundering-herd retry storms against recovering downstream services.
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
```

## Correct Implementation
The correct production implementation is provided in `ModernResilientExecutionEngine.java`, implementing bounded retries (e.g. max 3 attempts), exponential backoff with full randomized jitter (`interval = random(0, min(maxBackoff, base * 2^attempt))`), and selective predicate-based retry on transient failures only.
