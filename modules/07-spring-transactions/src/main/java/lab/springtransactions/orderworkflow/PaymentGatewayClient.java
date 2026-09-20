package lab.springtransactions.orderworkflow;

import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayClient {

    public record PaymentAuth(boolean successful, String transactionRef, String message) {}

    public PaymentAuth processPayment(String accountId, double amount) {
        if (amount <= 0) {
            return new PaymentAuth(false, null, "Invalid transaction amount");
        }
        if ("declined-card".equals(accountId)) {
            return new PaymentAuth(false, null, "Card declined by issuing bank");
        }
        return new PaymentAuth(true, "tx-auth-" + System.currentTimeMillis(), "Authorized");
    }
}
