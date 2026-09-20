package lab.springtransactions.broken.selfinvocation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    public void processOrder(String orderId, double amount) {
        validateOrder(orderId, amount);
        // Direct method call on this bypasses Spring AOP proxy interceptor
        placeOrder(orderId, amount);
    }

    @Transactional
    public void placeOrder(String orderId, double amount) {
        insertOrderRecord(orderId, amount);
        deductInventory(orderId);
    }

    private void validateOrder(String orderId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void insertOrderRecord(String orderId, double amount) {
        // DB insert
    }

    private void deductInventory(String orderId) {
        // DB inventory update
    }
}
