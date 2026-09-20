package lab.springtransactions.publicboundary;

import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final StockReservationCollaborator stockReservationCollaborator;

    public InventoryService(StockReservationCollaborator stockReservationCollaborator) {
        this.stockReservationCollaborator = stockReservationCollaborator;
    }

    public void reserveStock(String sku, int quantity) {
        checkAvailability(sku, quantity);
        // Cross-bean method invocation ensures Spring proxy intercepts the transactional boundary
        stockReservationCollaborator.deductStockTransactional(sku, quantity);
    }

    private void checkAvailability(String sku, int quantity) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
