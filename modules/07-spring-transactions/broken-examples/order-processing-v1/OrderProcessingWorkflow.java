package lab.springtransactions.broken.orderprocessing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderProcessingWorkflow {

    public void processOrder(String orderId, String customerEmail, double amount)
            throws Exception {
        // Self-invocation of transactional method
        executeTransaction(orderId, amount);

        // Sending email directly after method returns, but before outer callers finish
        sendCustomerEmail(customerEmail, orderId);
    }

    @Transactional
    public void executeTransaction(String orderId, double amount) throws Exception {
        insertOrder(orderId, amount);

        // Long-running remote payment gateway call inside @Transactional
        chargePaymentGateway(orderId, amount);

        if (amount <= 0) {
            // Checked exception: default @Transactional does not roll back on checked exceptions
            throw new Exception("Invalid amount for order " + orderId);
        }

        updateStatus(orderId, "COMPLETED");
    }

    private void insertOrder(String orderId, double amount) {
        // DB insert
    }

    private void chargePaymentGateway(String orderId, double amount) {
        // HTTP API call
    }

    private void updateStatus(String orderId, String status) {
        // DB update
    }

    private void sendCustomerEmail(String email, String orderId) {
        // SMTP email call
    }
}
