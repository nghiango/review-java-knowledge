package lab.distributeddata.outbox;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository {
    void save(OutboxEvent event);

    List<OutboxEvent> lockPendingBatch(int limit);

    void markProcessed(String eventId);

    Optional<OutboxEvent> findById(String eventId);
}
