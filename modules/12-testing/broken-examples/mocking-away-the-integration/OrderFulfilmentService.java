package lab.testing.broken.mockingawaytheintegration;

import java.util.Map;

/** Decides whether an order can be fulfilled from current inventory. */
public final class OrderFulfilmentService {

    private final InventoryClient inventoryClient;

    public OrderFulfilmentService(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    public boolean canFulfil(String sku, int quantity) {
        Map<String, Object> body = inventoryClient.get(sku);
        int available = ((Number) body.get("quantity")).intValue();
        return available >= quantity;
    }
}
