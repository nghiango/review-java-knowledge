package lab.springtransactions.selfinvocation;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderPlacementCollaborator orderPlacementCollaborator;

    public OrderService(OrderPlacementCollaborator orderPlacementCollaborator) {
        this.orderPlacementCollaborator = orderPlacementCollaborator;
    }

    public void processOrder(String orderId, double amount) {
        validateOrder(orderId, amount);
        // Invocations across collaborator bean boundaries pass through Spring AOP transactional
        // proxy
        orderPlacementCollaborator.placeOrder(orderId, amount);
    }

    private void validateOrder(String orderId, double amount) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
