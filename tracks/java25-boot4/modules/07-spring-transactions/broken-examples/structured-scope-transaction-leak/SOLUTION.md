# Solution: StructuredTaskScope Subtasks Inside @Transactional

## Issues Identified

```java
package lab.java25boot4.springtransactions.broken.structuredscope;

import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchOrderService {

    private final OrderItemRepository itemRepository;

    public BatchOrderService(OrderItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // Transaction issue: Spring @Transactional relies on ThreadLocal TransactionSynchronizationManager. Forking child threads breaks the atomic boundary
    @Transactional
    public void processBatchOrders(List<String> orderIds) throws Exception {
        // Concurrency issue: Spawning StructuredTaskScope inside a database transaction causes child virtual threads to acquire separate connections or run in auto-commit mode
        try (var scope = StructuredTaskScope.open()) {
            for (String orderId : orderIds) {
                scope.fork(() -> {
                    // Each child virtual thread runs outside the parent transaction. Changes are committed immediately or fail due to unmanaged connection acquisition
                    itemRepository.insertItem(orderId, "ITEM-DATA");
                    return null;
                });
            }
            scope.join();
        }

        // Transaction issue: Rolling back the parent transaction does NOT roll back items inserted by child threads, creating orphaned/inconsistent data
        if (orderIds.contains("fail-batch")) {
            throw new IllegalStateException("Simulated batch failure");
        }
    }

    public interface OrderItemRepository {
        void insertItem(String orderId, String itemData);
    }
}
```

### 1. Transaction issue (Thread-Bound Transaction Context)
Spring transactions bind the active `ConnectionHolder` and transaction status to `TransactionSynchronizationManager` via a `ThreadLocal`. When `scope.fork(...)` spawns child virtual threads, the parent thread's transactional context is NOT inherited. Each child thread either opens a new separate connection or operates in non-transactional auto-commit mode.

### 2. Concurrency & Consistency issue (Partial Writes & Rollback Inefficacy)
If the parent method throws an exception after `scope.join()`, Spring attempts to roll back the parent transaction. However, the items inserted by the child threads were already committed to the database on distinct connections. This results in corrupt partial writes and violates ACID atomicity.

## Refactored Solution
1. **Never fork child threads to perform transactional writes inside a parent `@Transactional` boundary**.
2. Perform batch database operations sequentially or in a single JDBC batch inside the transaction.
3. If parallel execution is required across external systems, separate the workflow into an **orchestrator**: perform parallel I/O outside DB transactions, then record state updates in short atomic transactions.
