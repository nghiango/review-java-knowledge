package lab.springtransactions.selfinvocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPlacementCollaborator {

    private final Map<String, Double> orderStore = new ConcurrentHashMap<>();

    @Transactional
    public void placeOrder(String orderId, double amount) {
        insertOrderRecord(orderId, amount);
        deductInventory(orderId);
    }

    private void insertOrderRecord(String orderId, double amount) {
        orderStore.put(orderId, amount);
    }

    private void deductInventory(String orderId) {
        if ("INVALID".equals(orderId)) {
            orderStore.remove(orderId);
            throw new IllegalStateException("Inventory unavailable");
        }
    }

    public Double getOrder(String orderId) {
        return orderStore.get(orderId);
    }
}
