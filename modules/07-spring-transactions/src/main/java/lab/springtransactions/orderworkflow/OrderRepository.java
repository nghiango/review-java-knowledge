package lab.springtransactions.orderworkflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OrderRepository {

    record Entity(
            String orderId,
            String customerEmail,
            String accountId,
            double amount,
            String status,
            String paymentRef) {}

    private final Map<String, Entity> database = new ConcurrentHashMap<>();

    @Transactional
    public void createPendingOrder(
            String orderId, String customerEmail, String accountId, double amount) {
        database.put(
                orderId, new Entity(orderId, customerEmail, accountId, amount, "PENDING", null));
    }

    @Transactional
    public void markOrderCompleted(String orderId, String paymentRef) {
        Entity existing = database.get(orderId);
        if (existing != null) {
            database.put(
                    orderId,
                    new Entity(
                            existing.orderId(),
                            existing.customerEmail(),
                            existing.accountId(),
                            existing.amount(),
                            "COMPLETED",
                            paymentRef));
        }
    }

    @Transactional
    public void markOrderFailed(String orderId, String reason) {
        Entity existing = database.get(orderId);
        if (existing != null) {
            database.put(
                    orderId,
                    new Entity(
                            existing.orderId(),
                            existing.customerEmail(),
                            existing.accountId(),
                            existing.amount(),
                            "FAILED: " + reason,
                            null));
        }
    }

    public Entity findById(String orderId) {
        return database.get(orderId);
    }
}
