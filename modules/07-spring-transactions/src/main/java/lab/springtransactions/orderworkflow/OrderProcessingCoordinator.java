package lab.springtransactions.orderworkflow;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessingCoordinator {

    private final OrderRepository orderRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final ApplicationEventPublisher eventPublisher;

    public OrderProcessingCoordinator(
            OrderRepository orderRepository,
            PaymentGatewayClient paymentGatewayClient,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.paymentGatewayClient = paymentGatewayClient;
        this.eventPublisher = eventPublisher;
    }

    public OrderWorkflowResult processOrder(OrderWorkflowCommand command) {
        if (command.amount() <= 0) {
            throw new IllegalArgumentException("Order amount must be positive");
        }

        // Step 1: Atomic short transaction to persist pending order
        orderRepository.createPendingOrder(
                command.orderId(), command.customerEmail(), command.accountId(), command.amount());

        // Step 2: Remote payment processing executed outside any database transaction
        PaymentGatewayClient.PaymentAuth auth =
                paymentGatewayClient.processPayment(command.accountId(), command.amount());

        if (!auth.successful()) {
            orderRepository.markOrderFailed(command.orderId(), auth.message());
            return new OrderWorkflowResult(
                    command.orderId(), "FAILED", null, "Payment declined: " + auth.message());
        }

        // Step 3: Atomic short transaction to mark order completed
        orderRepository.markOrderCompleted(command.orderId(), auth.transactionRef());

        // Step 4: Publish domain event for decoupled downstream processing (e.g. notifications)
        eventPublisher.publishEvent(
                new OrderCompletedEvent(
                        command.orderId(),
                        command.customerEmail(),
                        command.amount(),
                        auth.transactionRef()));

        return new OrderWorkflowResult(
                command.orderId(), "COMPLETED", auth.transactionRef(), "Order placed successfully");
    }
}
