# Solution: Inbox Without Deduplication Key

## Annotated code

```java
package lab.distributeddata.broken.inbox;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryAllocationConsumer {

    private final InventoryRepository inventoryRepository;

    public InventoryAllocationConsumer(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @KafkaListener(topics = "orders.events", groupId = "inventory-allocation-group")
    @Transactional
    public void onOrderPlaced(OrderPlacedEvent event) {
        // Data consistency issue: Non-idempotent consumer missing an Inbox table or deduplication check.
        // Kafka provides at-least-once delivery. On rebalances, broker crashes, or offset commit timeouts,
        // this event will be redelivered. Each redelivery decrements stock again, causing ghost inventory depletion.
        // Reliability issue: Processing mutations directly without recording eventId creates untraceable discrepancies.
        inventoryRepository.decrementStock(event.sku(), event.quantity());
    }

    public record OrderPlacedEvent(String eventId, String orderId, String sku, int quantity) {}

    public interface InventoryRepository {
        void decrementStock(String sku, int quantity);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Data consistency issue | Critical | `InventoryAllocationConsumer.onOrderPlaced()` | Non-idempotent consumer decrements stock multiple times on duplicate delivery |
| 2 | Reliability issue | High | `InventoryAllocationConsumer.onOrderPlaced()` | Missing Inbox deduplication table tracking processed `eventId` |

## Issue details

### Missing Consumer Deduplication (The Inbox Pattern)

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Intermediate
**Technology:** Kafka, Spring Kafka, Event-Driven Architecture · **Interview frequency:** High · **Production impact:** Critical

**Location:** `InventoryAllocationConsumer.onOrderPlaced()`

#### Problem
The consumer performs a state mutation (`decrementStock`) directly upon receiving an event, without checking whether `event.eventId()` has already been processed.

#### Why it happens
Assuming Kafka or messaging systems deliver messages exactly once. In reality, brokers guarantee only at-least-once delivery.

#### Production impact
When consumer group rebalances occur or worker nodes restart before committing offsets, hundreds of order events are redelivered. Inventory counts drop below physical warehouse availability, causing false "Out of Stock" alerts and lost sales.

#### Correct implementation
See `lab.distributeddata.inbox`:
Use the **Inbox Pattern**. In the same database transaction as the business mutation, insert `event.eventId()` into an `inbox` table with a primary key constraint. If insertion succeeds, decrement stock; if a unique key conflict occurs, acknowledge the message and discard the duplicate.

## Correct implementation

See `lab.distributeddata.inbox` in `src/main/java/lab/distributeddata/inbox/SafeInventoryConsumer.java`.
Documentation: [Distributed Data Solutions](../../../docs/topics/distributed-data-patterns/solutions.md#inbox-pattern-and-idempotent-consumer).
