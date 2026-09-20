# Solution: Dual Write to Database and Message Broker

## Annotated code

```java
package lab.distributeddata.broken.dualwrite;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCreationService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderCreationService(OrderRepository orderRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public OrderRecord createOrder(String orderId, String customerId, double totalAmount) {
        OrderRecord order = new OrderRecord(orderId, customerId, totalAmount, "CREATED");
        orderRepository.save(order);

        // Transaction issue: Remote Kafka network I/O executed inside active database transaction.
        // If Kafka broker is slow or times out, the database connection is held open, causing HikariCP pool exhaustion.
        // Data consistency issue: Dual Write problem. If Kafka publish fails, DB rolls back. But if DB commit fails AFTER kafkaTemplate.send()
        // (or if kafkaTemplate.send() is asynchronous without blocking), the message is published to the broker for an uncommitted order!
        // Reliability issue: If the JVM crashes after DB commit but before or during broker communication, the event is permanently lost.
        kafkaTemplate.send("orders.events", orderId, "{\"orderId\":\"" + orderId + "\",\"status\":\"CREATED\"}");

        return order;
    }

    public record OrderRecord(String orderId, String customerId, double totalAmount, String status) {}

    public interface OrderRepository {
        void save(OrderRecord order);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Data consistency issue | Critical | `OrderCreationService.createOrder()` | Dual write: DB and Kafka cannot participate in an atomic transaction |
| 2 | Transaction issue | High | `OrderCreationService.createOrder()` | Remote network I/O (`kafkaTemplate.send`) inside open DB transaction |
| 3 | Reliability issue | High | `OrderCreationService.createOrder()` | Asynchronous `kafkaTemplate.send()` fire-and-forget without waiting for broker ACK |

## Issue details

### Dual Write Problem

**Type:** Data consistency issue · **Severity:** Critical · **Difficulty:** Intermediate
**Technology:** PostgreSQL, Kafka, Spring Transactions · **Interview frequency:** High · **Production impact:** Critical

**Location:** `OrderCreationService.createOrder()`

#### Problem
The service writes to two separate storage systems (PostgreSQL and Apache Kafka) within a single method execution. Distributed systems cannot provide atomic two-phase commit between an RDBMS and a message broker without massive performance and availability degradation.

#### Why it happens
Assuming `@Transactional` protects non-database systems like message brokers, or assuming network calls never fail.

#### Production impact
1. **Phantom Events**: `kafkaTemplate.send()` pushes a message to Kafka. When the method returns, Spring issues `COMMIT` to PostgreSQL. If the DB commit fails (e.g. unique constraint violation, DB connection lost, disk full), downstream services consume the event and fulfill an order that **does not exist in the database**.
2. **Silent Event Loss**: If the order is committed to PostgreSQL, but the application crashes before the broker acknowledges the message, the order exists in the DB, but downstream payment and fulfillment services never receive the event. The order is permanently stuck.

#### Correct implementation
See `lab.distributeddata.outbox`:
Use the **Transactional Outbox Pattern**. Write the domain entity (`Order`) and the outbox event (`OutboxEvent`) into the same database transaction. A separate relay worker (or CDC tool like Debezium) reads the outbox table and publishes to Kafka with at-least-once delivery guarantees.

#### Trade-offs
Introduces eventual consistency and requires downstream consumers to be idempotent.

## Correct implementation

See `lab.distributeddata.outbox` in `src/main/java/lab/distributeddata/outbox/OutboxPublisher.java`.
Documentation: [Distributed Data Solutions](../../../docs/topics/distributed-data-patterns/solutions.md#transactional-outbox-pattern).
