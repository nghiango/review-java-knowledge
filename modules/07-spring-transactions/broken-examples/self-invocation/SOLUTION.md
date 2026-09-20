# Solution: Self-Invocation Transaction Bypass

## Annotated Code

```java
package lab.springtransactions.broken.selfinvocation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    public void processOrder(String orderId, double amount) {
        validateOrder(orderId, amount);
        // Architecture issue: self-invocation bypasses Spring AOP transactional proxy
        placeOrder(orderId, amount);
    }

    @Transactional
    public void placeOrder(String orderId, double amount) {
        insertOrderRecord(orderId, amount);
        deductInventory(orderId);
    }

    private void validateOrder(String orderId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void insertOrderRecord(String orderId, double amount) {
        // DB insert
    }

    private void deductInventory(String orderId) {
        // DB inventory update
    }
}
```

## Issues Identified

### Architecture issue: self-invocation bypasses Spring AOP transactional proxy
- **Location:** `OrderService.java#processOrder`
- **Explanation:** In Spring's proxy-based AOP architecture, transactional advice (`TransactionInterceptor`) is invoked only when calls enter through the Spring proxy. Calling `placeOrder()` from `processOrder()` executes directly on `this`, completely bypassing proxy interception. As a result, no database transaction is started, and failures in `deductInventory()` do not roll back `insertOrderRecord()`.

## Correct implementation

See `lab.springtransactions.selfinvocation.OrderService` and `lab.springtransactions.selfinvocation.OrderPlacementCollaborator`.
