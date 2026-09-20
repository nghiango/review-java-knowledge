# Solution: Outbox Processing PR (Premature Deletion & Unbounded Batches)

## Annotated code

```java
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
        // Concurrency issue: Unlocked fetchPending() causes multiple polling worker instances to fetch and process identical rows.
        List<OutboxEntry> pending = outboxTable.fetchPending();

        for (OutboxEntry entry : pending) {
            // Reliability issue: Premature deletion before broker acknowledgment.
            // If the broker rejects the message, times out, or the process crashes during kafkaTemplate.send(), the event is lost forever!
            outboxTable.delete(entry.id());

            // Reliability issue: Fire-and-forget asynchronous send without blocking on RecordMetadata future or attaching callbacks.
            // Performance issue: Hard deletion in a tight loop generates excessive single-row database round-trips and table fragmentation.
            kafkaTemplate.send("fulfillment.events", entry.aggregateId(), entry.payload());
        }
    }

    public record OutboxEntry(String id, String aggregateId, String payload) {}

    public interface OutboxTable {
        List<OutboxEntry> fetchPending();
        void delete(String id);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Reliability issue | Critical | `OrderFulfillmentCoordinator.processOutboxBatch()` | Deleting outbox record before broker acknowledges message loss |
| 2 | Reliability issue | High | `OrderFulfillmentCoordinator.processOutboxBatch()` | Asynchronous fire-and-forget send without checking `CompletableFuture<SendResult>` |
| 3 | Concurrency issue | High | `OrderFulfillmentCoordinator.processOutboxBatch()` | Unlocked polling without `SELECT FOR UPDATE SKIP LOCKED` |
| 4 | Performance issue | Medium | `OrderFulfillmentCoordinator.processOutboxBatch()` | Row-by-row deletions rather than status updates or batched removal |

## Issue details

### Premature Outbox Deletion Before Broker Confirmation

**Type:** Reliability issue · **Severity:** Critical · **Difficulty:** Intermediate
**Technology:** Transactional Outbox, Kafka · **Interview frequency:** High · **Production impact:** Critical

**Location:** `OrderFulfillmentCoordinator.processOutboxBatch()`

#### Problem
The publisher deletes the outbox entry from the database *prior* to sending the message to Kafka or waiting for the broker to acknowledge disk persistence.

#### Why it happens
Mistakenly assuming that initiating a send operation guarantees successful broker delivery.

#### Production impact
If the Kafka broker is temporarily offline, if authentication fails, or if the server crashes before network packets leave the socket buffer, the database record has already been deleted. The event is permanently destroyed with zero recovery mechanism.

#### Correct implementation
See `lab.distributeddata.outbox`:
Only update outbox status to `PROCESSED` (or delete) **after** the `CompletableFuture<SendResult>` returns a successful `RecordMetadata` response from Kafka. Use `SELECT FOR UPDATE SKIP LOCKED` to allow multiple concurrent pollers without collisions.

## Correct implementation

See `lab.distributeddata.outbox` in `src/main/java/lab/distributeddata/outbox/OutboxPublisher.java`.
Documentation: [Distributed Data Solutions](../../../docs/topics/distributed-data-patterns/solutions.md#transactional-outbox-pattern).
