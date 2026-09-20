package lab.springtransactions.broken.remoteapi;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

    private final PaymentClient paymentClient;

    public CheckoutService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @Transactional
    public void checkout(String orderId, String accountId, double amount) {
        // Step 1: Create pending order in DB (holds DB connection)
        insertPendingOrder(orderId, amount);

        // Step 2: Remote HTTP call inside @Transactional
        boolean success = paymentClient.chargeCard(accountId, amount);
        if (!success) {
            throw new IllegalStateException("Payment failed");
        }

        // Step 3: Mark order paid in DB
        markOrderPaid(orderId);
    }

    private void insertPendingOrder(String orderId, double amount) {
        // DB insert
    }

    private void markOrderPaid(String orderId) {
        // DB update
    }
}
