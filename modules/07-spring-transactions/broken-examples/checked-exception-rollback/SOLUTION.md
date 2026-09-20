# Solution: Checked Exception Rollback Assumption

## Annotated Code

```java
package lab.springtransactions.broken.checkedexception;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPlacementService {

    // Reliability issue: @Transactional default rollback policy only rolls back unchecked exceptions (RuntimeException / Error)
    @Transactional
    public void placeOrder(String orderId, double amount) throws OrderValidationException {
        // Step 1: Save initial order record
        insertOrder(orderId, amount);

        // Step 2: Checked validation failure
        if (amount <= 0) {
            // Reliability issue: throwing checked Exception will commit the transaction by default
            throw new OrderValidationException("Invalid order amount: " + amount);
        }

        updateStatus(orderId, "CONFIRMED");
    }

    private void insertOrder(String orderId, double amount) {
        // DB insert
    }

    private void updateStatus(String orderId, String status) {
        // DB update
    }
}
```

## Issues Identified

### Reliability issue: default @Transactional rollback policy only rolls back unchecked exceptions
- **Location:** `OrderPlacementService.java#placeOrder`
- **Explanation:** By default in Spring Framework, a transaction is rolled back only on unchecked exceptions (`RuntimeException` and `Error`). Checked exceptions (subclasses of `java.lang.Exception`) do **not** trigger a rollback; instead, Spring commits the transaction. To ensure rollback on checked exceptions, declare `@Transactional(rollbackFor = {OrderValidationException.class, Exception.class})` or use domain `RuntimeException` types.

## Correct implementation

See `lab.springtransactions.rollbackrules.OrderPlacementService` and `lab.springtransactions.rollbackrules.OrderValidationException`.
