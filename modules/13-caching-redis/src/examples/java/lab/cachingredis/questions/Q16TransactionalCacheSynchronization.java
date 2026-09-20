package lab.cachingredis.questions;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Q16: Why should cache evictions be deferred until afterCommit in Spring @Transactional methods?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q16TransactionalCacheSynchronization {

    static class OrderService {

        public void cancelOrder(Long orderId) {
            // Update order status in relational database
            // database.updateStatus(orderId, "CANCELLED");

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                // Register cache eviction hook to run ONLY AFTER the physical DB transaction has
                // committed
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                // redisTemplate.delete("order:" + orderId);
                            }
                        });
            }
        }
    }

    public static void main(String[] args) {
        // If we evict cache INSIDE the transaction before commit:
        // A concurrent reader experiences a cache miss, reads the DB BEFORE our commit finishes,
        // and repopulates the cache with the OLD uncommitted data!
        boolean evictBeforeCommitCausesDirtyCacheRefill = true; // true

        // If the transaction throws an exception and rolls back:
        // afterCommit() is NEVER executed, protecting the cache from spurious evictions.
        boolean rollbackSkipsAfterCommit = true; // true
    }
}
