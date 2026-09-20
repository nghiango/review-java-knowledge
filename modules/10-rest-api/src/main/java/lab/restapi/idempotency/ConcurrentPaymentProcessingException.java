package lab.restapi.idempotency;

public class ConcurrentPaymentProcessingException extends RuntimeException {
    public ConcurrentPaymentProcessingException(String message) {
        super(message);
    }
}
