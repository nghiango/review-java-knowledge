package lab.springtransactions.publicboundary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StockReservationCollaborator {

    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();

    // Transactional method declared public on a Spring managed bean to ensure AOP proxy wrapping
    @Transactional
    public void deductStockTransactional(String sku, int quantity) {
        if ("OUT_OF_STOCK".equals(sku)) {
            throw new IllegalStateException("Stock unavailable for SKU: " + sku);
        }
        inventory.merge(sku, quantity, Integer::sum);
    }

    public int getReservedStock(String sku) {
        return inventory.getOrDefault(sku, 0);
    }
}
