package lab.concurrency.broken.volatilecompound;

import java.util.HashMap;
import java.util.Map;

public class InventoryReservationService {
    private final Map<String, ItemStock> stockByItem = new HashMap<>();

    public void registerItem(String itemId, int initialStock) {
        stockByItem.put(itemId, new ItemStock(itemId, initialStock));
    }

    public boolean reserve(String itemId, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive");
        }

        ItemStock stock = stockByItem.get(itemId);
        if (stock == null) {
            return false;
        }

        // Check: reads volatile availableQuantity
        if (stock.getAvailableQuantity() >= requestedQuantity) {
            // Simulated scheduling pause between check and act
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
                // Swallowing interruption
            }

            // Act: reads volatile quantity again and sets new value
            int updated = stock.getAvailableQuantity() - requestedQuantity;
            stock.setAvailableQuantity(updated);
            return true;
        }

        return false;
    }

    public int getAvailableStock(String itemId) {
        ItemStock stock = stockByItem.get(itemId);
        return stock == null ? 0 : stock.getAvailableQuantity();
    }
}
