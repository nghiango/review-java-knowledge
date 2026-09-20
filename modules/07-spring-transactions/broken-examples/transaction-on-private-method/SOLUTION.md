# Solution: Transaction on Private Method

## Annotated Code

```java
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

    // Architecture issue: @Transactional on private method is silently ignored by Spring AOP proxies
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
```

## Issues Identified

### Architecture issue: @Transactional on private method is silently ignored by Spring AOP proxies
- **Location:** `InventoryService.java#deductStockTransactional`
- **Explanation:** In standard Spring AOP (using JDK dynamic proxies or CGLIB subclassing), generated proxies can only override and intercept **`public`** methods. Annotating `private` (or package-private/protected) methods with `@Transactional` does not generate interceptor advice. Spring ignores the annotation without throwing an exception, leading to silent non-transactional execution.

## Correct implementation

See `lab.springtransactions.publicboundary.InventoryService` and `lab.springtransactions.publicboundary.StockReservationCollaborator`.
