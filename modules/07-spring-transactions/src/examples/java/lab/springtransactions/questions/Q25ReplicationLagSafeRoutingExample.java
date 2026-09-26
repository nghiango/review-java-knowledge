package lab.springtransactions.questions;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SuppressWarnings("unused")
public final class Q25ReplicationLagSafeRoutingExample {
    private Q25ReplicationLagSafeRoutingExample() {}

    public enum DataSourceType {
        PRIMARY,
        REPLICA
    }

    private static final ThreadLocal<Long> LAST_WRITE_TIMESTAMP = new ThreadLocal<>();
    private static final long REPLICATION_LAG_WINDOW_MS = 2000L;

    public static class ReplicationLagRoutingDataSource extends AbstractRoutingDataSource {
        @Override
        protected Object determineCurrentLookupKey() {
            // If the transaction is read-write, always use PRIMARY:
            if (!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                LAST_WRITE_TIMESTAMP.set(System.currentTimeMillis());
                return DataSourceType.PRIMARY;
            }

            // Read-only transaction: check if current thread wrote recently within replication lag
            // window
            Long lastWrite = LAST_WRITE_TIMESTAMP.get();
            if (lastWrite != null
                    && (System.currentTimeMillis() - lastWrite) < REPLICATION_LAG_WINDOW_MS) {
                // Read-your-own-writes consistency: route read to PRIMARY to avoid reading stale
                // replica!
                return DataSourceType.PRIMARY;
            }

            return DataSourceType.REPLICA;
        }
    }

    public static void main(String[] args) {
        ReplicationLagRoutingDataSource router = new ReplicationLagRoutingDataSource();
        LAST_WRITE_TIMESTAMP.set(System.currentTimeMillis());
        Object key = router.determineCurrentLookupKey(); // DataSourceType.PRIMARY (guarantees
        // read-your-writes)
        boolean isPrimary = (key == DataSourceType.PRIMARY); // true
        LAST_WRITE_TIMESTAMP.remove();
    }
}
