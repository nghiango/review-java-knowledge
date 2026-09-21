package lab.architecture.modularmonolith.inventory.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lab.architecture.modularmonolith.inventory.InventoryApi;
import org.springframework.stereotype.Service;

/**
 * Internal implementation of the Inventory API. Encapsulated within internal package to prevent
 * direct cross-module coupling.
 */
@Service
public class InventoryServiceImpl implements InventoryApi {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();

    public InventoryServiceImpl() {
        stock.put("PROD-1", 100);
        stock.put("PROD-2", 50);
    }

    @Override
    public boolean isAvailable(String productId, int quantity) {
        return stock.getOrDefault(productId, 0) >= quantity;
    }

    @Override
    public synchronized boolean reserve(String productId, int quantity) {
        int current = stock.getOrDefault(productId, 0);
        if (current >= quantity) {
            stock.put(productId, current - quantity);
            return true;
        }
        return false;
    }
}
