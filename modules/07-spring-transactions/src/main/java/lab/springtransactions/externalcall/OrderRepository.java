package lab.springtransactions.externalcall;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OrderRepository {

    private final Map<String, OrderRecord> storage = new ConcurrentHashMap<>();

    @Transactional
    public void savePendingOrder(String orderId, String accountId, double amount) {
        storage.put(orderId, new OrderRecord(orderId, accountId, amount, "PENDING"));
    }

    @Transactional
    public void updateStatus(String orderId, String status) {
        OrderRecord existing = storage.get(orderId);
        if (existing != null) {
            storage.put(
                    orderId,
                    new OrderRecord(
                            existing.orderId(), existing.accountId(), existing.amount(), status));
        }
    }

    public OrderRecord findById(String orderId) {
        return storage.get(orderId);
    }
}
