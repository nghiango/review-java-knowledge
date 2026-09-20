# Solution: Remote API Call Inside Transaction

## Annotated Code

```java
package lab.springtransactions.broken.remoteapi;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

    private final PaymentClient paymentClient;

    public CheckoutService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    // Performance issue: remote HTTP call inside @Transactional holds DB connection and exhausts pool
    @Transactional
    public void checkout(String orderId, String accountId, double amount) {
        // Step 1: Create pending order in DB (holds DB connection)
        insertPendingOrder(orderId, amount);

        // Performance issue: slow downstream network I/O executed while holding database connection
        boolean success = paymentClient.chargeCard(accountId, amount);
        if (!success) {
            throw new IllegalStateException("Payment failed");
        }

        // Step 3: Mark order paid in DB
        markOrderPaid(orderId);
    }

    private void insertPendingOrder(String orderId, double amount) {
        // DB insert
    }

    private void markOrderPaid(String orderId) {
        // DB update
    }
}
```

## Issues Identified

### Performance issue: remote HTTP call inside @Transactional holds DB connection and exhausts pool
- **Location:** `CheckoutService.java#checkout`
- **Explanation:** When entering `@Transactional`, Spring's `TransactionInterceptor` acquires a database `Connection` from HikariCP and binds it to `TransactionSynchronizationManager`. Executing a remote HTTP API call while holding this connection blocks the connection for the entire duration of the HTTP network latency (hundreds of ms or seconds). Under concurrent load, all connection pool threads become saturated, leading to `HikariPool ConnectionTimeoutException` across unrelated services.

## Correct implementation

See `lab.springtransactions.externalcall.CheckoutOrchestrator` and `lab.springtransactions.externalcall.OrderRepository`.
