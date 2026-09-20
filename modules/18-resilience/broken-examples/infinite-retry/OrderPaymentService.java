package lab.resilience.broken.infinitetry;

import org.springframework.stereotype.Service;

@Service
public class OrderPaymentService {

    private final ExternalPaymentGateway paymentGateway;

    public OrderPaymentService(ExternalPaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public PaymentResult processPayment(String orderId, double amount) {
        while (true) {
            try {
                PaymentResponse response = paymentGateway.charge(orderId, amount);
                return new PaymentResult(response.transactionId(), true, "Success");
            } catch (Exception ex) {
                System.err.println("Payment failed for order " + orderId + ", retrying immediately: " + ex.getMessage());
            }
        }
    }

    public record PaymentResult(String transactionId, boolean success, String message) {}
    public record PaymentResponse(String transactionId, String status) {}

    public interface ExternalPaymentGateway {
        PaymentResponse charge(String orderId, double amount);
    }
}
