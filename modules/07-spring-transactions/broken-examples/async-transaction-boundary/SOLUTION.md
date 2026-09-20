# Solution: Asynchronous Execution Across Transaction Boundaries

## Annotated Code

```java
package lab.springtransactions.broken.asynctransaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final NotificationService notificationService;

    public OrderService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Transactional
    public void createOrder(String orderId, double amount) {
        insertOrder(orderId, amount);

        // Reliability issue: @Async method dispatched before transaction commit; sends emails for rolled-back transactions
        notificationService.sendOrderConfirmation(orderId);

        if (amount > 10000) {
            throw new IllegalArgumentException("Amount exceeds credit limit - rolling back");
        }
    }

    private void insertOrder(String orderId, double amount) {
        // DB insert
    }
}
```

## Issues Identified

### Reliability issue: async task dispatched before transaction commit causes phantom notifications and race conditions
- **Location:** `OrderService.java#createOrder`
- **Explanation:** Spring's `@Transactional` context is stored in `ThreadLocal` (`TransactionSynchronizationManager`) and is not propagated to new asynchronous worker threads spawned by `@Async`. The async notification method executes immediately in parallel. If the outer transaction later encounters an exception and rolls back, the email has already been dispatched. Furthermore, if the async thread queries the database before the caller's transaction commits, it reads uncommitted/missing data. Use domain event publishing paired with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` to guarantee events fire only after successful database commits.

## Correct implementation

See `lab.springtransactions.asynctransaction.OrderService` and `lab.springtransactions.asynctransaction.OrderNotificationListener`.
