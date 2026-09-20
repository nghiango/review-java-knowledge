package lab.kafka.idempotent;

/**
 * Deduplication storage abstraction for idempotent consumer processing.
 *
 * <p>In production, this is backed by an RDBMS unique constraint table (e.g. {@code INSERT INTO
 * processed_messages ...}) or Redis atomic {@code SETNX} with TTL.
 */
public interface DeduplicationStore {

    /**
     * Atomically checks if the transaction was already processed, and marks it as processed if not.
     *
     * @param transactionId unique transaction identifier
     * @return {@code true} if this is a newly recorded transaction (first time seen), {@code false}
     *     if already processed (duplicate delivery)
     */
    boolean tryMarkProcessed(String transactionId);

    /** Checks if the transaction was already processed without altering state. */
    boolean isProcessed(String transactionId);
}
