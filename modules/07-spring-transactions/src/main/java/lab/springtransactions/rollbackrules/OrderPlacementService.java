package lab.springtransactions.rollbackrules;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPlacementService {

    private final Map<String, String> orderStore = new ConcurrentHashMap<>();

    // Explicit rollbackFor ensures checked exceptions trigger automatic transaction rollback
    @Transactional(rollbackFor = {OrderValidationException.class, Exception.class})
    public void placeOrder(String orderId, double amount) throws OrderValidationException {
        orderStore.put(orderId, "PENDING");

        if (amount <= 0) {
            orderStore.remove(orderId);
            throw new OrderValidationException("Invalid order amount: " + amount);
        }

        orderStore.put(orderId, "CONFIRMED");
    }

    public String getOrderStatus(String orderId) {
        return orderStore.get(orderId);
    }
}
