package lab.rabbitmq.idempotent;

/** Atomic deduplication store for idempotent RabbitMQ consumer processing. */
public interface DeduplicationStore {

    /**
     * Atomically marks the command as processed if not previously seen.
     *
     * @param commandId unique message or command identifier
     * @return {@code true} if this is the first time the command has been processed, {@code false}
     *     if the command was already recorded (duplicate delivery)
     */
    boolean tryMarkProcessed(String commandId);

    /** Checks if the command was already recorded without altering state. */
    boolean isProcessed(String commandId);
}
