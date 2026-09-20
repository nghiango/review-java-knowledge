package lab.concurrency.volatilecompound;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe inventory reservation service with lock-free atomic stock reduction and thread-safe
 * item registry.
 */
public class InventoryReservationService {
    private final Map<String, ItemStock> stockByItem = new ConcurrentHashMap<>();

    public void registerItem(String itemId, int initialStock) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        stockByItem.put(itemId, new ItemStock(itemId, initialStock));
    }

    public boolean reserve(String itemId, int requestedQuantity) {
        if (itemId == null) {
            return false;
        }
        ItemStock stock = stockByItem.get(itemId);
        if (stock == null) {
            return false;
        }
        return stock.reserveExact(requestedQuantity);
    }

    public int getAvailableStock(String itemId) {
        ItemStock stock = stockByItem.get(itemId);
        return stock == null ? 0 : stock.getAvailableQuantity();
    }
}
