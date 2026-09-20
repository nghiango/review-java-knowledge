package lab.distributeddata.outbox;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
            OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public int publishPendingEvents(int batchSize, String topicName) {
        List<OutboxEvent> pendingEvents = outboxRepository.lockPendingBatch(batchSize);
        int publishedCount = 0;

        for (OutboxEvent event : pendingEvents) {
            try {
                CompletableFuture<SendResult<String, String>> future =
                        kafkaTemplate.send(topicName, event.aggregateId(), event.payload());
                // Block synchronously to guarantee broker acknowledgment before marking as
                // processed
                future.join();
                outboxRepository.markProcessed(event.id());
                publishedCount++;
            } catch (Exception ex) {
                // If publish fails, abort processing for this batch so transaction rolls back
                throw new RuntimeException(
                        "Failed to publish outbox event " + event.id() + " to Kafka", ex);
            }
        }

        return publishedCount;
    }
}
