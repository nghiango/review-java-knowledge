package lab.architecture.broken.crossmodule;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

    private final InventoryRepository inventoryRepository;
    private final ShippingRepository shippingRepository;

    public BillingService(InventoryRepository inventoryRepository, ShippingRepository shippingRepository) {
        this.inventoryRepository = inventoryRepository;
        this.shippingRepository = shippingRepository;
    }

    public BigDecimal processBilling(Long orderId, String customerId, BigDecimal baseAmount) {
        // Direct cross-module query into inventory internal data
        int stockCount = inventoryRepository.findStockCountByOrderId(orderId);
        if (stockCount <= 0) {
            throw new IllegalStateException("Cannot bill order with no allocated inventory");
        }

        // Direct cross-module query and mutation into shipping internal state
        Map<String, Object> shipmentData = shippingRepository.findShipmentRecord(orderId);
        BigDecimal shippingCost = (BigDecimal) shipmentData.getOrDefault("cost", BigDecimal.ZERO);

        BigDecimal finalTotal = baseAmount.add(shippingCost);

        // Direct cross-module write bypassing shipping module domain rules
        shippingRepository.updateShipmentStatus(orderId, "BILLING_COMPLETED");

        return finalTotal;
    }
}
