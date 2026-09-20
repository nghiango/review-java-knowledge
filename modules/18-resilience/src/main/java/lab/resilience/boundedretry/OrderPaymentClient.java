package lab.resilience.boundedretry;

public interface OrderPaymentClient {
    PaymentResponse charge(String orderId, double amount);

    record PaymentResponse(String transactionId, String status) {}
}
