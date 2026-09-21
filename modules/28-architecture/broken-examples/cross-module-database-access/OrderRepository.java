package lab.architecture.broken.crossmodule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private final Map<Long, String> orders = new ConcurrentHashMap<>();

    public String getOrderSummary(Long orderId) {
        return orders.getOrDefault(orderId, "ORDER-" + orderId);
    }
}
