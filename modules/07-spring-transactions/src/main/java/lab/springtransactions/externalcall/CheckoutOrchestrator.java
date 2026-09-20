package lab.springtransactions.externalcall;

import org.springframework.stereotype.Service;

@Service
public class CheckoutOrchestrator {

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;

    public CheckoutOrchestrator(OrderRepository orderRepository, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
    }

    // Orchestrator method is NOT annotated with @Transactional.
    // Database modifications run in isolated short transactions, and remote network I/O executes
    // outside DB transactions.
    public void checkout(String orderId, String accountId, double amount) {
        // Step 1: Short atomic transaction to create pending order
        orderRepository.savePendingOrder(orderId, accountId, amount);

        // Step 2: Remote payment call executed WITHOUT holding database connection
        boolean paymentSuccess = paymentClient.chargeCard(accountId, amount);

        // Step 3: Short atomic transaction to update final status
        if (paymentSuccess) {
            orderRepository.updateStatus(orderId, "PAID");
        } else {
            orderRepository.updateStatus(orderId, "PAYMENT_FAILED");
            throw new IllegalStateException("Payment processing failed");
        }
    }
}
