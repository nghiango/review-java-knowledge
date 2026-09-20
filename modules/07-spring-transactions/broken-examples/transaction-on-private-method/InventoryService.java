package lab.springtransactions.broken.privatemethod;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    public void reserveStock(String sku, int quantity) {
        checkAvailability(sku, quantity);
        // Invoking private @Transactional method
        deductStockTransactional(sku, quantity);
    }

    // @Transactional on private method is silently ignored by Spring AOP proxies
    @Transactional
    private void deductStockTransactional(String sku, int quantity) {
        updateInventoryTable(sku, quantity);
        insertAuditRecord(sku, quantity);
    }

    private void checkAvailability(String sku, int quantity) {
        // Check stock
    }

    private void updateInventoryTable(String sku, int quantity) {
        // DB update
    }

    private void insertAuditRecord(String sku, int quantity) {
        // DB insert
    }
}
