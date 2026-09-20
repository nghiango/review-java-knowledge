# Solution: Non-Idempotent Saga Compensation

## Annotated code

```java
package lab.distributeddata.broken.compensation;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCompensationService {

    private final AccountRepository accountRepository;
    private final PaymentAuditRepository auditRepository;

    public PaymentCompensationService(AccountRepository accountRepository, PaymentAuditRepository auditRepository) {
        this.accountRepository = accountRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void compensateFailedOrder(String orderId, String accountId, BigDecimal amountToRefund) {
        // Concurrency issue: Unlocked findById followed by balance update is prone to lost updates.
        Account account = accountRepository.findById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }

        // Data consistency issue: Non-idempotent compensation.
        // Sagas rely on at-least-once message delivery. If the compensation event is redelivered (due to network timeout,
        // consumer rebalance, or orchestrator retry), compensateFailedOrder will execute multiple times, refunding the customer
        // 2x or 3x the original payment amount!
        // Reliability issue: Missing prior execution check for orderId before applying balance credit.
        account.setBalance(account.getBalance().add(amountToRefund));
        accountRepository.save(account);

        auditRepository.recordRefund(orderId, accountId, amountToRefund);
    }

    public static class Account {
        private final String accountId;
        private BigDecimal balance;

        public Account(String accountId, BigDecimal balance) {
            this.accountId = accountId;
            this.balance = balance;
        }

        public String getAccountId() { return accountId; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
    }

    public interface AccountRepository {
        Account findById(String accountId);
        void save(Account account);
    }

    public interface PaymentAuditRepository {
        void recordRefund(String orderId, String accountId, BigDecimal amount);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Data consistency issue | Critical | `PaymentCompensationService.compensateFailedOrder()` | Non-idempotent compensation issues multiple refunds on redelivery |
| 2 | Concurrency issue | High | `PaymentCompensationService.compensateFailedOrder()` | Unlocked check-then-act balance mutation causes lost updates |
| 3 | Reliability issue | High | `PaymentCompensationService.compensateFailedOrder()` | Missing deduplication check against historical refunds for `orderId` |

## Issue details

### Non-Idempotent Saga Compensation

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Intermediate
**Technology:** Distributed Sagas, Eventual Consistency · **Interview frequency:** High · **Production impact:** Critical

**Location:** `PaymentCompensationService.compensateFailedOrder()`

#### Problem
The compensation method unconditionally credits the customer account balance without checking whether a refund for that `orderId` has already been recorded.

#### Why it happens
Assuming that a Saga orchestrator or message broker delivers compensation events strictly once. In real-world distributed architectures, brokers guarantee only **at-least-once** delivery.

#### Production impact
When a network hiccup delays consumer offset commitment, Kafka redelivers the compensation event. The customer account is credited a second or third time, resulting in direct financial theft or loss for the company.

#### Correct implementation
See `lab.distributeddata.saga`:
Make compensation idempotent:
1. Maintain a `processed_compensations` table with a `UNIQUE(order_id, action)` constraint.
2. In the same transaction, insert the compensation record. If a unique key violation occurs, treat the call as a no-op success.

## Correct implementation

See `lab.distributeddata.saga` in `src/main/java/lab/distributeddata/saga/SagaCoordinator.java`.
Documentation: [Distributed Data Solutions](../../../docs/topics/distributed-data-patterns/solutions.md#idempotent-saga-compensation).
