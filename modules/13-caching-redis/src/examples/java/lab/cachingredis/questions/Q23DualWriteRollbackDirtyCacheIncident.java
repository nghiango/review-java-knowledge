package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q23: Production Scenario: A failed bank payment rolled back in PostgreSQL, but customer wallets
 * retained phantom credits in Redis. How did the dual-write violation occur and how was strict
 * consistency restored?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q23DualWriteRollbackDirtyCacheIncident {

    public static void main(String[] args) {
        // Incident:
        // Service had @Transactional method:
        // 1. database.updateBalance(userId, +1000);
        // 2. redis.set("wallet:" + userId, newBalance);
        // 3. complianceCheck(); // Threw FraudException!
        // Result: PostgreSQL rolled back the +1000 credit. Redis retained the +1000 credit!
        // Customer immediately withdrew the phantom funds.

        Map<String, String> incidentReport =
                Map.of(
                        "Violation",
                                "Mutating non-transactional cache directly inside relational database transaction",
                        "Remediation Phase 1",
                                "Flush corrupted wallet keys from Redis and trigger re-read from PostgreSQL",
                        "Remediation Phase 2",
                                "Register cache eviction via TransactionSynchronization.afterCommit()",
                        "Permanent Architecture",
                                "Adopt Transactional Outbox + CDC (Debezium) for event-driven cache invalidation");

        boolean nonTransactionalCacheInsideDbTx =
                incidentReport.get("Violation").contains("Mutating non-transactional"); // true
        boolean afterCommitPreventsDirtyWrites =
                incidentReport.get("Remediation Phase 2").contains("afterCommit"); // true
    }
}
