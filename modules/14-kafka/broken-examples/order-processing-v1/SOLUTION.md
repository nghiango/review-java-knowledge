# Solution — Order Processing Service v1

## Annotated code

```java
package lab.kafka.broken.orderprocessing;

import java.math.BigDecimal;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsecureOrderProcessor {

    public static final String TOPIC = "orders.v1";

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public InsecureOrderProcessor(
            OrderRepository orderRepository,
            InventoryClient inventoryClient,
            KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public OrderEntity placeOrder(String orderId, String customerId, BigDecimal amount) {
        OrderEntity order = new OrderEntity(orderId, customerId, amount, "PENDING");
        orderRepository.save(order);

        OrderPlacedEvent event = new OrderPlacedEvent(orderId, customerId, amount, java.time.Instant.now());
        // Data consistency issue: Dual-write race condition. Publishing to Kafka inside an active database
        // transaction before DB commit. If the subsequent updateStatus() fails or the database transaction
        // rolls back, the Kafka message cannot be recalled; downstream workers fulfill a phantom order.
        // Data consistency issue: Sending without a partition key (null key). Order lifecycle events for the same
        // order are distributed across partitions, breaking causal ordering.
        kafkaTemplate.send(TOPIC, event);

        orderRepository.updateStatus(orderId, "CONFIRMED");
        return order;
    }

    @KafkaListener(topics = TOPIC, groupId = "order-fulfillment-group")
    public void onOrderPlaced(OrderPlacedEvent event) {
        // Data consistency issue: Non-idempotent consumer. At-least-once delivery duplicates redelivered events,
        // causing double reservation of stock without checking if orderId was already fulfilled.
        try {
            inventoryClient.reserveStock(event.orderId());
            orderRepository.updateStatus(event.orderId(), "FULFILLED");
        } catch (Exception e) {
            // Error handling issue: Catching and swallowing generic Exception without DLT routing
            // or propagating to a Spring Kafka error handler silently drops unfulfilled orders.
            System.err.println("Fulfillment failed for order: " + event.orderId());
        }
    }

    public interface OrderRepository {
        void save(OrderEntity order);

        void updateStatus(String orderId, String status);
    }

    public interface InventoryClient {
        void reserveStock(String orderId);
    }
}
```

## Issue list

### Data consistency issue: Dual-write hazard publishing to Kafka inside uncommitted database transaction

- **Location:** `InsecureOrderProcessor.java:32-34`
- **Description:** `kafkaTemplate.send(TOPIC, event)` is called directly inside an open Spring `@Transactional` method.
- **Impact:** If `orderRepository.updateStatus()` throws an exception, or the database connection drops during commit, the database rolls back the order creation. However, Kafka has already accepted the event. Downstream inventory workers consume the event and reserve warehouse stock for an order that does not exist in the database (phantom order).
- **Remediation:** Decouple publishing from the local transaction using the Transactional Outbox pattern (saving an outbox record in the same DB transaction) or defer sending until after transaction commit using Spring's `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.

### Data consistency issue: Null partition key breaks per-order event ordering

- **Location:** `InsecureOrderProcessor.java:34`
- **Description:** Messages are published without specifying the `orderId` or `customerId` as the Kafka message key.
- **Impact:** Events are distributed across multiple topic partitions. If an order is canceled or modified shortly after creation, downstream consumers listening to different partitions can process the cancellation before the creation, causing invalid state transitions.
- **Remediation:** Pass `orderId` as the record key: `kafkaTemplate.send(TOPIC, orderId, event)` to guarantee partition affinity and total sequential ordering per order.

### Data consistency issue: Consumer lacks idempotency guards against duplicate redeliveries

- **Location:** `InsecureOrderProcessor.java:42-45`
- **Description:** `onOrderPlaced` receives events and calls `inventoryClient.reserveStock(event.orderId())` without checking if stock was already reserved for this `orderId`.
- **Impact:** In at-least-once delivery, consumer crashes, rebalances, and retries cause duplicate processing. Inventory will be reserved multiple times for the same order, artificially depleting stock.
- **Remediation:** Implement an idempotency store check (e.g. tracking processed `orderId`s with a unique database constraint or distributed lock/cache). If already processed, return immediately.

### Error handling issue: Swallowing exceptions hides fulfillment failures and drops Dead Letter routing

- **Location:** `InsecureOrderProcessor.java:46-48`
- **Description:** Swallows `Exception` and logs to stdout without rethrowing or routing to a Dead Letter Topic.
- **Impact:** Any transient inventory outage or permanent data error silently disappears; the order remains stuck unfulfilled forever without operator alerting.
- **Remediation:** Propagate exceptions to a configured `DefaultErrorHandler` with bounded retries and publish exhausted messages to `orders.DLT`.
