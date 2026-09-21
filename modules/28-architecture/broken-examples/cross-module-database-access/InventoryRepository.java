package lab.architecture.broken.crossmodule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryRepository {

    private final Map<Long, Integer> stockByOrder = new ConcurrentHashMap<>();

    public int findStockCountByOrderId(Long orderId) {
        return stockByOrder.getOrDefault(orderId, 1);
    }

    public void allocateStock(Long orderId, int count) {
        stockByOrder.put(orderId, count);
    }
}
