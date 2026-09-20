# Solution: Comprehensive Order Processing Workflow PR

## Annotated Code

```java
package lab.springtransactions.broken.orderprocessing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderProcessingWorkflow {

    public void processOrder(String orderId, String customerEmail, double amount)
            throws Exception {
        // Architecture issue: self-invocation bypasses Spring transactional proxy
        executeTransaction(orderId, amount);

        // Reliability issue: external notification dispatched directly without after-commit guarantee
        sendCustomerEmail(customerEmail, orderId);
    }

    // Reliability issue: missing rollbackFor = Exception.class on method throwing checked Exception
    @Transactional
    public void executeTransaction(String orderId, double amount) throws Exception {
        insertOrder(orderId, amount);

        // Performance issue: remote HTTP call inside @Transactional holds DB connection and exhausts pool
        chargePaymentGateway(orderId, amount);

        if (amount <= 0) {
            // Reliability issue: checked Exception commits transaction by default
            throw new Exception("Invalid amount for order " + orderId);
        }

        updateStatus(orderId, "COMPLETED");
    }

    private void insertOrder(String orderId, double amount) {
        // DB insert
    }

    private void chargePaymentGateway(String orderId, double amount) {
        // HTTP API call
    }

    private void updateStatus(String orderId, String status) {
        // DB update
    }

    private void sendCustomerEmail(String email, String orderId) {
        // SMTP email call
    }
}
```

## Issues Identified

### Architecture issue: self-invocation bypasses Spring transactional proxy
- **Location:** `OrderProcessingWorkflow.java#processOrder`
- **Explanation:** Calling `executeTransaction` from `processOrder` on `this` does not pass through the Spring proxy, so `@Transactional` advice is never executed.

### Performance issue: remote HTTP call inside @Transactional holds DB connection and exhausts pool
- **Location:** `OrderProcessingWorkflow.java#executeTransaction`
- **Explanation:** `chargePaymentGateway` is an external network call. Executing it while holding a database connection blocks connection pool slots, creating latency amplification and pool exhaustion.

### Reliability issue: missing rollbackFor on checked Exception commits transaction
- **Location:** `OrderProcessingWorkflow.java#executeTransaction`
- **Explanation:** Throwing checked `java.lang.Exception` does not trigger transaction rollback under default Spring rules.

### Reliability issue: external notification dispatched directly without after-commit guarantee
- **Location:** `OrderProcessingWorkflow.java#processOrder`
- **Explanation:** Sending emails before or outside transaction synchronization risks sending notifications for transactions that might fail during commit.

## Correct implementation

See `lab.springtransactions.orderworkflow.OrderProcessingCoordinator`, `lab.springtransactions.orderworkflow.OrderRepository`, `lab.springtransactions.orderworkflow.PaymentGatewayClient`, and `lab.springtransactions.orderworkflow.OrderNotificationListener`.
