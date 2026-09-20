# Kafka Concepts & Architecture

Kafka departs fundamentally from traditional message brokers (such as RabbitMQ or ActiveMQ). Instead of maintaining transient queues where messages are discarded once acknowledged, Kafka organizes data into distributed, append-only, durable **commit logs**.

---

## 1. The Distributed Commit Log

In Kafka, a **topic** is a logical stream of events. Each topic is partitioned into one or more **partitions**. A partition is an immutable, ordered sequence of records continuously appended to disk:

```mermaid
flowchart LR
    subgraph Partition["Topic Partition Log"]
        direction LR
        O0["Offset 0<br/>Msg A"] --> O1["Offset 1<br/>Msg B"]
        O1 --> O2["Offset 2<br/>Msg C"]
        O2 --> O3["Offset 3<br/>Msg D"]
        O3 --> O4["Offset 4<br/>Msg E"]
        O4 --> LEO["Log End Offset (LEO)"]
    end
    ConsumerOffset["Committed Offset = 2<br/>(Consumer Group A)"] -.-> O2
    NewWrite["Producer Write"] --> LEO
```

### Characteristics

- **Append-Only Immutability**: Records cannot be updated or deleted in place. Every write appends to the log end.
- **Sequential Disk I/O**: Sequential writes eliminate disk head seek times, allowing Kafka to achieve gigabyte-per-second write throughput even on rotational storage and cloud block storage.
- **Deterministic Addressing**: Every record within a partition is uniquely identified by a 64-bit integer called its **offset**.
- **Retention Without Deletion**: Records persist based on time (`log.retention.hours`) or size (`log.retention.bytes`), independent of whether they have been read by consumers.

---

## 2. Topics, Partitions, and Key Hashing

Partitions serve as the atomic unit of scalability and parallelism in Kafka.

```mermaid
flowchart TD
    Record["ProducerRecord(topic, key, value)"]
    Decision{"Is Key null?"}
    Murmur["Murmur2(keyBytes) % numPartitions"]
    Sticky["StickyPartitioner (Batch to current active partition)"]
    P0["Partition 0"]
    P1["Partition 1"]
    P2["Partition 2"]

    Record --> Decision
    Decision -- "Key != null" --> Murmur
    Decision -- "Key == null" --> Sticky
    Murmur --> P0
    Murmur --> P1
    Murmur --> P2
```

- **Keyed Records**: When a key is supplied, Kafka computes `Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions`. The identical key is guaranteed to map to the identical partition as long as partition count remains constant.
- **Null Keys**: Since Kafka 2.4, the default partitioner uses sticky batching. It routes unkeyed records to a chosen partition until the batch size or `linger.ms` is reached, then rotates to another partition, dramatically improving throughput.
- **The Ordering Invariant**: Kafka guarantees message ordering **only within a single partition**. If an application requires chronological event processing for a customer or order, that entity identifier **must** be provided as the message key.

---

## 3. Producer Durability and Acks

The producer configuration `acks` determines how many partition replicas must acknowledge receipt before a publish operation is considered successful:

| Setting | Durability Guarantee | Latency | Data Loss Risk |
|---|---|---|---|
| `acks=0` | None (fire-and-forget) | Lowest | Extreme: dropped silently if broker drops packet or crashes |
| `acks=1` | Partition leader writes to local disk | Low | Moderate: leader crash before replication causes silent data loss |
| `acks=all` (`-1`) | Leader and all `min.insync.replicas` commit | Moderate | Zero data loss as long as `min.insync.replicas` is satisfied |

### Producer Idempotence (`enable.idempotence=true`)

When network hiccups occur while the broker is returning an acknowledgment, the producer cannot tell if the message was written or lost. If the producer retries, a non-idempotent producer writes duplicate messages to the log.

When `enable.idempotence=true` (the default since Kafka 3.0):
1. The broker assigns each producer a unique 64-bit **Producer ID (PID)** via `InitProducerId`.
2. Each record sent to a partition carries a monotonically increasing **Sequence Number**.
3. The broker checks the incoming sequence number against its in-memory table. If `incoming_seq <= last_committed_seq`, the broker silently ignores the duplicate record while returning success to the producer.

---

## 4. Consumer Groups and Partition Balancing

A **consumer group** coordinates a set of consumers to process records from a topic in parallel:

```mermaid
flowchart TD
    subgraph Topic["Topic (4 Partitions)"]
        P0["Partition 0"]
        P1["Partition 1"]
        P2["Partition 2"]
        P3["Partition 3"]
    end
    subgraph ConsumerGroup["Consumer Group (2 Instances)"]
        C1["Consumer 1<br/>Assigned: P0, P1"]
        C2["Consumer 2<br/>Assigned: P2, P3"]
    end

    P0 --> C1
    P1 --> C1
    P2 --> C2
    P3 --> C2
```

- **1:1 Assignment Rule**: A single partition can be consumed by at most **one** consumer instance within the same consumer group at any time.
- **Over-Provisioning Limit**: If consumer group instances exceed the number of partitions in a topic, surplus instances remain idle with zero assigned partitions.
- **Independent Groups**: Multiple distinct consumer groups read the exact same topic independently at their own reading pace with distinct committed offsets.

---

## 5. Offset Commit Semantics

Consumers maintain a committed offset representing the highest point in the log they have successfully processed:

```mermaid
sequenceDiagram
    participant Broker as Kafka Broker
    participant Consumer as Spring Kafka Consumer
    participant DB as Business Database

    Broker->>Consumer: poll() delivers records [offset 100..105]
    Consumer->>DB: Process business logic & write to DB
    Note over Consumer,DB: Business transaction committed
    Consumer->>Broker: commitSync() / ack.acknowledge() [offset 106]
    Note over Broker: Offset advanced to 106
```

- **At-Most-Once Delivery**: Committing the offset *before* processing completes. If the consumer crashes during processing, the record is skipped on restart.
- **At-Least-Once Delivery**: Committing the offset *after* processing completes. If the consumer crashes before committing the offset, redelivery occurs.
- **Idempotent Consumers**: Because at-least-once is the industry standard, consumers must guard against duplicate deliveries using an idempotency store (e.g. unique constraint on event ID or atomic Redis `SETNX`).

---

## 6. Error Handling and Dead Letter Topics (DLT)

Deterministic failures (such as schema validation errors or missing required fields) will fail on every subsequent attempt:

```mermaid
flowchart TD
    Record["Incoming Record"] --> Listener["@KafkaListener"]
    Listener -->|Success| Ack["Commit Offset"]
    Listener -->|Exception| ErrorHandler["DefaultErrorHandler"]
    ErrorHandler --> Decision{"Is Exception Fatal / Non-Retryable?"}
    Decision -- Yes (IllegalArgumentException) --> DLT["Publish directly to topic.DLT"]
    Decision -- No (Network / Lock Timeout) --> Retry{"Retry Count &lt; Max?"}
    Retry -- Yes --> Backoff["Exponential Backoff Sleep"] --> Listener
    Retry -- No --> DLT
    DLT --> Ack
```

1. **Dead Letter Publishing Recoverer**: Moves exhausted or fatal records to `${topic}.DLT` with error metadata headers (`kafka_exception-message`, `kafka_original-topic`).
2. **Non-Retryable Exceptions**: Fatal business errors skip retries immediately, avoiding pointless CPU cycles and latency delays.
3. **Partition Liveness**: Routing poison pills to DLT allows the consumer offset to advance, preventing a single corrupted record from stalling an entire partition.

---

## 7. Dual-Write Hazards and the Transactional Outbox Pattern

Attempting to update a database and publish to Kafka inside an application method introduces a classic dual-write race condition:

```mermaid
flowchart TD
    subgraph Broken["The Dual-Write Anti-Pattern"]
        direction TB
        B1["1. Open DB Transaction"] --> B2["2. Update Account Balance"]
        B2 --> B3["3. Publish to Kafka (kafkaTemplate.send)"]
        B3 --> B4["4. DB Commit Fails (Constraint Error / Timeout)"]
        B4 --> B5["Result: Phantom Kafka Event Dispatched!"]
    end
    subgraph Outbox["The Transactional Outbox Solution"]
        direction TB
        O1["1. Open DB Transaction"] --> O2["2. Update Account Balance"]
        O2 --> O3["3. INSERT INTO outbox_table"]
        O3 --> O4["4. DB Commit (Atomic ACID Boundary)"]
        O4 --> O5["5. Debezium CDC / Poller reads outbox"]
        O5 --> O6["6. Publish to Kafka with Guaranteed Delivery"]
    end
```

By storing outbox records in the database within the primary business transaction, state changes and event declarations commit atomically. Downstream publication occurs safely via Change Data Capture (CDC) or reliable post-commit event listeners (`@TransactionalEventListener(phase = AFTER_COMMIT)`).
