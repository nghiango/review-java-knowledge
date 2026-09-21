package lab.architecture.cleanarchitecture.domain.port.out;

import lab.architecture.cleanarchitecture.domain.model.Money;
import lab.architecture.cleanarchitecture.domain.model.OrderId;

/**
 * Outgoing port for external payment processing. Isolates the domain from external gateway APIs
 * (e.g. Stripe, PayPal).
 */
public interface PaymentPort {

    PaymentResult processPayment(OrderId orderId, Money amount, String paymentToken);

    record PaymentResult(boolean successful, String transactionId, String errorMessage) {

        public static PaymentResult success(String transactionId) {
            return new PaymentResult(true, transactionId, null);
        }

        public static PaymentResult failure(String errorMessage) {
            return new PaymentResult(false, null, errorMessage);
        }
    }
}
