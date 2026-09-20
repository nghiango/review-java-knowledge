package lab.distributeddata.broken.combinedpr;

import java.util.List;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderFulfillmentCoordinator {

    private final OutboxTable outboxTable;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderFulfillmentCoordinator(OutboxTable outboxTable, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxTable = outboxTable;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void processOutboxBatch() {
        List<OutboxEntry> pending = outboxTable.fetchPending();

        for (OutboxEntry entry : pending) {
            // Delete record from outbox table BEFORE sending to Kafka
            outboxTable.delete(entry.id());

            // Fire and forget send without waiting for broker acknowledgment
            kafkaTemplate.send("fulfillment.events", entry.aggregateId(), entry.payload());
        }
    }

    public record OutboxEntry(String id, String aggregateId, String payload) {}

    public interface OutboxTable {
        List<OutboxEntry> fetchPending();
        void delete(String id);
    }
}
