package lab.distributeddata.inbox;

public interface InboxRepository {
    boolean tryAcquireLease(String messageId, String consumerGroup);

    boolean isProcessed(String messageId, String consumerGroup);
}
