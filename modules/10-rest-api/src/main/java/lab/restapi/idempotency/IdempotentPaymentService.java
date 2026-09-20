package lab.restapi.idempotency;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IdempotentPaymentService {

    private final IdempotencyStorage idempotencyStorage;

    public IdempotentPaymentService(IdempotencyStorage idempotencyStorage) {
        this.idempotencyStorage = idempotencyStorage;
    }

    public PaymentExecutionResult processPayment(String idempotencyKey, PaymentRequest request) {
        var cached = idempotencyStorage.findByKey(idempotencyKey);
        if (cached.isPresent()) {
            return new PaymentExecutionResult(cached.get(), true);
        }

        boolean acquired = idempotencyStorage.lock(idempotencyKey);
        if (!acquired) {
            // Concurrent in-flight request with same idempotency key
            throw new ConcurrentPaymentProcessingException(
                    "Payment with key " + idempotencyKey + " is currently in-flight.");
        }

        try {
            // Simulate payment processing
            PaymentResponse response =
                    new PaymentResponse(
                            UUID.randomUUID(),
                            request.accountId(),
                            request.amount(),
                            request.currency(),
                            "COMPLETED",
                            Instant.now());

            idempotencyStorage.save(idempotencyKey, response);
            return new PaymentExecutionResult(response, false);
        } catch (RuntimeException ex) {
            idempotencyStorage.release(idempotencyKey);
            throw ex;
        }
    }

    public record PaymentExecutionResult(PaymentResponse response, boolean wasReplayed) {}
}
