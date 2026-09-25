package lab.springtransactions.questions;

import java.time.Instant;
import java.util.UUID;

@SuppressWarnings("unused")
public final class Q26CdcOutboxWalVsPollerExample {
    private Q26CdcOutboxWalVsPollerExample() {}

    // Outbox event record inserted atomically within the business transaction:
    public record OutboxRecord(
        UUID id,
        String aggregateType,
        String aggregateId,
        String payloadJson,
        Instant createdAt
    ) {}

    public static class OutboxComparison {
        // Approach A: Polling Publisher
        // Queries outbox table with SELECT ... FOR UPDATE SKIP LOCKED.
        // Advantage: Simple, database-agnostic, zero external infrastructure.
        // Drawback: Constant polling load on database, table bloat from frequent inserts/deletes, latency delay.
        public static String describePollingPublisher() {
            return "SELECT * FROM outbox_table FOR UPDATE SKIP LOCKED LIMIT 100";
        }

        // Approach B: Change Data Capture (CDC via Debezium)
        // Reads database transaction log (PostgreSQL WAL) directly via logical decoding replication slots.
        // Advantage: Zero polling overhead, sub-second latency, near-zero impact on application database.
        // Drawback: Requires Kafka Connect cluster and PostgreSQL logical replication slot monitoring.
        public static String describeCdcPublisher() {
            return "Debezium pgoutput plugin reading pg_wal directly";
        }
    }

    public static void main(String[] args) {
        OutboxRecord record = new OutboxRecord(
            UUID.randomUUID(), "Order", "ord-123", "{\"status\":\"PLACED\"}", Instant.now()
        );
        String sql = OutboxComparison.describePollingPublisher();
        boolean usesSkipLocked = sql.contains("SKIP LOCKED"); // true
    }
}
