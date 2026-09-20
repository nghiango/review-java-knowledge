# Kafka Interview Questions

Answer each question before expanding its explanation.

<!-- --8<-- [start:basic] -->
## Basic Concepts

### How does Kafka achieve high write and read throughput through its commit log architecture and sequential disk I/O?

Explain append-only logs, sequential disk access, and OS page cache utilization.

??? question "Reveal answer"
    - **Append-Only Immutability**: All records are sequentially appended to the end of a partition log on disk. Appends do not require random disk seeks, allowing rotational HDDs and NVMe SSDs to perform at theoretical bus limits.
    - **OS Page Cache Utilization**: Rather than caching records in JVM heap memory (which induces heavy garbage collection overhead), Kafka delegates caching entirely to the Linux OS Page Cache. Read requests for recently written records are served directly from page cache RAM without touching physical disks.
    - **Partition Scalability**: A topic is split into multiple independent partitions. Each partition can be written and read concurrently by separate threads and consumer instances.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q01KafkaArchitectureBrokersPartitions.java"
    ```

---

### What is the role of the In-Sync Replicas (ISR) list, and how does min.insync.replicas prevent data loss?

Detail leader-follower synchronization, the ISR contract, and how broker dropouts affect write acknowledgments.

??? question "Reveal answer"
    - **In-Sync Replicas (ISR)**: The set of partition replicas (including the leader) that are actively caught up with the leader's Log End Offset within `replica.lag.time.max.ms`.
    - **`min.insync.replicas`**: The minimum number of replicas in the ISR that must acknowledge a write when a producer sends with `acks=all`.
    - **Data Loss Prevention**: If a cluster has a replication factor of 3 and `min.insync.replicas=2`, the leader will acknowledge writes as long as at least one follower is caught up. If two followers crash (leaving only the leader in the ISR), the leader immediately rejects further writes with `NotEnoughReplicasException`, preventing un-replicated commits from being acknowledged before a potential leader crash.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q02ReplicationAndIsrMechanics.java"
    ```

---

### What are the differences between acks=0, acks=1, and acks=all (-1), and what does enable.idempotence provide?

Compare producer acknowledgment modes, durability risks, and sequence number deduplication.

??? question "Reveal answer"
    - **`acks=0`**: Fire-and-forget. The producer considers the record sent as soon as it is written to the local network socket. Provides lowest latency, but messages are silently lost on broker crashes or network drops.
    - **`acks=1`**: The leader writes the record to its local log and acknowledges immediately without waiting for followers. Moderate durability, but if the leader crashes before followers replicate, the acknowledged record is lost.
    - **`acks=all` (`-1`)**: The leader waits until the record is committed by all current In-Sync Replicas (governed by `min.insync.replicas`). Guarantees zero data loss as long as at least `min.insync.replicas` stay healthy.
    - **`enable.idempotence=true`**: The broker tracks a Producer ID (PID) and sequence number for each partition. If a producer retries due to a network timeout where the record was actually written, the broker detects the duplicate sequence number and discards it without writing duplicate log entries.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q03ProducerAcksAndDurability.java"
    ```

---

### How does Kafka map message keys to topic partitions, and what happens when the key is null?

Explain the DefaultPartitioner, Murmur2 hashing, sticky batching, and partition count mutability.

??? question "Reveal answer"
    - **Key Hashing**: For non-null keys, Kafka computes `Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions`. This guarantees that records with identical keys always land on the identical partition, preserving strict causal sequence.
    - **Null Keys**: With null keys, modern Kafka clients utilize `StickyPartitioner`. Instead of round-robining individual records (which creates tiny, inefficient network packets), it batches consecutive unkeyed records to a chosen partition until the batch is full or `linger.ms` expires, then selects another partition.
    - **Partition Count Changes**: If the number of partitions on an existing topic is increased, the modulus denominator changes. Subsequent records with the same key will hash to different partitions, breaking global causal ordering for historical keys.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q04PartitionKeysAndHashing.java"
    ```

---

### How do consumer groups enable horizontal scaling, and how do partition assignors divide work?

Describe consumer group parallelism limits, idle instances, and assignment strategies.

??? question "Reveal answer"
    - **Scalability Limit**: Within a consumer group, a partition can be consumed by at most one consumer instance at any time. Maximum active parallelism is strictly bounded by the number of partitions in the topic.
    - **Idle Consumers**: If a consumer group has 10 consumer instances subscribed to a topic with 6 partitions, 4 consumers will sit completely idle with 0 assigned partitions, acting only as hot standby instances.
    - **Assignment Strategies**:
        - **RangeAssignor**: Assigns contiguous partition ranges per topic (e.g. C1 gets P0-P1, C2 gets P2-P3). Can lead to partition skew across multiple co-partitioned topics.
        - **RoundRobinAssignor**: Interleaves partitions evenly across all consumers.
        - **CooperativeStickyAssignor**: Retains existing assignments across rebalances and only migrates reassigned partitions, eliminating global processing pauses.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q05ConsumerGroupsAndRebalancing.java"
    ```

---

### What are the differences between auto-commit and manual offset commit, and what are the delivery semantics?

Contrast enable.auto.commit with manual acknowledgment and at-most-once vs at-least-once hazards.

??? question "Reveal answer"
    - **Auto-Commit (`enable.auto.commit=true`)**: The consumer background poll loop commits the latest received offset every `auto.commit.interval.ms`. If a consumer crashes midway through processing a batch of records, the uncommitted records are re-delivered on restart (at-least-once). However, if an offset commits *before* processing finishes and the application encounters a fatal error, records are permanently skipped (at-most-once).
    - **Manual Offset Commit (`AckMode.MANUAL_IMMEDIATE`)**: The application code controls the commit timing via `Acknowledgment.acknowledge()`. Committing strictly after business logic completes guarantees at-least-once delivery: on crash, unacknowledged records will be redelivered.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q06OffsetCommitSemantics.java"
    ```

---

### Is "Exactly-Once Processing" achievable across end-to-end distributed systems?

Clarify Kafka's Exactly-Once Semantics (EOS) boundaries versus external side-effects in third-party services.

??? question "Reveal answer"
    - **Within Kafka Boundaries**: Kafka's transactional API (`initTransactions`, `sendOffsetsToTransaction`) guarantees atomic read-process-write loops between Kafka topics. Records published to output topics and consumer offsets committed on input topics commit together atomically.
    - **Across External Databases & APIs**: Kafka transactions **cannot** span external systems (databases, HTTP webhooks, email providers) without a distributed two-phase commit (XA) coordinator. If a consumer writes to PostgreSQL and then crashes before committing its Kafka offset, Kafka redelivers the message on restart.
    - **Production Reality**: End-to-end exactly-once is achieved in practice via **at-least-once delivery paired with idempotent consumers** using a deduplication store.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q07ExactlyOnceSemanticsMyths.java"
    ```

---

### How do session.timeout.ms, heartbeat.interval.ms, and max.poll.interval.ms govern consumer health?

Detail consumer group coordinator heartbeats and foreground processing timeouts.

??? question "Reveal answer"
    - **`heartbeat.interval.ms`**: Frequency with which the consumer's dedicated background heartbeat thread sends heartbeats to the Group Coordinator broker (typically 3,000ms).
    - **`session.timeout.ms`**: Maximum duration the broker coordinator waits without receiving a heartbeat before considering the consumer dead and triggering a group rebalance (typically 45,000ms; must be >= 3x heartbeat interval).
    - **`max.poll.interval.ms`**: Maximum allowed delay between consecutive calls to `poll()` by the application thread (default 300,000ms / 5 minutes). If processing a batch of records exceeds this deadline, the consumer group coordinator assumes the thread is locked up and revokes its partitions.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q08ConsumerRebalanceHeartbeat.java"
    ```
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Concepts

### What key settings on ConcurrentKafkaListenerContainerFactory tune concurrency, error handling, and ack modes?

Detail Spring Kafka listener container configuration options.

??? question "Reveal answer"
    - **`setConcurrency(int)`**: Spawns multiple concurrent `KafkaMessageListenerContainer` instances. Each container thread connects to Kafka and receives a subset of topic partitions. Concurrency should not exceed total topic partitions.
    - **`setAckMode(AckMode)`**: Controls commit behavior:
        - `RECORD`: Commits offset after each individual record listener completes.
        - `BATCH`: Commits offset after all records returned by the poll are processed.
        - `MANUAL_IMMEDIATE`: Commits offset immediately when `Acknowledgment.acknowledge()` is called.
    - **`setCommonErrorHandler(CommonErrorHandler)`**: Replaces legacy error handlers with `DefaultErrorHandler`, supporting exponential backoff, retry filters, and Dead Letter Topic recoverers.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q09SpringKafkaListenerConfiguration.java"
    ```

---

### How does Spring Kafka's DefaultErrorHandler combined with DeadLetterPublishingRecoverer route exhausted records to a Dead Letter Topic?

Explain poison pill prevention, retry backoff, and DLT naming conventions.

??? question "Reveal answer"
    - **`DeadLetterPublishingRecoverer`**: A recoverer callback that publishes failing consumer records to a Dead Letter Topic (by default `<original-topic>.DLT` with the same partition index).
    - **Backoff & Bounded Retries**: Configured with `FixedBackOff` or `ExponentialBackOff` to retry transient failures (e.g. database deadlocks or network glitches).
    - **Non-Retryable Exceptions**: By adding deterministic exceptions (e.g. `IllegalArgumentException`, `DeserializationException`) via `errorHandler.addNotRetryableExceptions(...)`, the handler routes corrupted messages directly to the DLT without wasting retry attempts.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q10DeadLetterTopicErrorHandling.java"
    ```

---

### What is consumer lag, why is it a critical SLO metric, and how is it calculated?

Discuss Log End Offset (LEO), committed offset, and consumer lag monitoring.

??? question "Reveal answer"
    - **Calculation**: Consumer Lag is the delta between the highest offset written by producers to a partition (Log End Offset, LEO) and the highest offset committed by the consumer group (`Lag = LEO - CurrentOffset`).
    - **SLO Significance**: Consumer lag directly measures processing latency and message backlog. Persistent or growing lag indicates that downstream consumer capacity is insufficient to keep pace with incoming producer throughput.
    - **Monitoring**: Monitored cluster-wide via Burrow, Kafka Exporter, or Micrometer metric `kafka.consumer.fetch.manager.records.lag`.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q11ConsumerLagMonitoring.java"
    ```

---

### How does the Transactional Outbox pattern solve the dual-write consistency hazard between SQL DB and Kafka?

Explain the failure modes of publishing directly inside database transactions and the outbox remediation.

??? question "Reveal answer"
    - **The Dual-Write Hazard**: Publishing to Kafka inside an open Spring `@Transactional` method creates a race condition. If the database commit fails afterwards, the message has already been published to Kafka and cannot be recalled, causing downstream consumers to process phantom state. Conversely, publishing after commit risks lost messages if the JVM crashes before `send()` executes.
    - **Outbox Pattern**: Both the business entity change and an outbox event record are written into the same relational database in a single atomic ACID transaction.
    - **Reliable Dispatch**: An asynchronous poller or Change Data Capture (CDC) engine (e.g. Debezium reading PostgreSQL WAL) reads the outbox table and publishes events to Kafka with guaranteed at-least-once delivery.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q12TransactionalOutboxPattern.java"
    ```

---

### How do Kafka transactions (transactional.id, Transaction Coordinator, 2PC marker) work under the hood?

Detail the two-phase commit protocol implemented by Kafka brokers.

??? question "Reveal answer"
    - **Transaction Coordinator**: A dedicated broker managing an internal `__transaction_state` log.
    - **`transactional.id`**: Identifies a transactional producer across restarts, allowing the coordinator to fence zombie producer instances by incrementing the epoch.
    - **Two-Phase Protocol**:
        1. Producer calls `initTransactions()` and registers partitions via `AddPartitionsToTxnRequest`.
        2. Producer sends messages to topic partitions and sends consumer offsets via `sendOffsetsToTransaction()`.
        3. Producer calls `commitTransaction()`. Coordinator writes a `PREPARE_COMMIT` marker to its log.
        4. Coordinator writes a `COMMIT` control marker to every participating topic partition.
    - **Consumer Isolation**: Consumers configured with `isolation.level=read_committed` buffer incoming records in memory and only emit records whose partition log contains a `COMMIT` control marker, filtering out aborted records.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q13KafkaTransactionsTwoPhaseCommit.java"
    ```

---

### Why is a Schema Registry needed with Apache Avro/Protobuf, and how does schema compatibility work?

Discuss binary serialization overhead, wire format, and schema evolution rules.

??? question "Reveal answer"
    - **Payload Optimization**: Serializing full JSON schemas with every message wastes network bandwidth. With Avro, the schema is registered once in a Schema Registry.
    - **Wire Format**: Messages are prefixed with 5 bytes: a 1-byte magic marker (`0x00`) followed by a 4-byte Schema ID. The serialized binary Avro payload follows. Consumers fetch the schema by ID from the registry and cache it locally.
    - **Compatibility Modes**:
        - `BACKWARD` (default): New schema can read data written by old schema. (Fields can be deleted; optional fields with defaults can be added).
        - `FORWARD`: Old schema can read data written by new schema.
        - `FULL`: Both backward and forward compatible.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q14SchemaRegistryAndAvroEvolution.java"
    ```

---

### How does Kafka log compaction retain only the latest state per key, and what are tombstone records?

Explain cleanup.policy=compact, state store changelogs, and message deletion.

??? question "Reveal answer"
    - **Log Compaction**: When `cleanup.policy=compact` is enabled, Kafka's background cleaner thread scans inactive log segments and purges older records that share the same message key, retaining only the latest record per key.
    - **Changelog Pattern**: Compaction turns a Kafka topic into an append-only snapshot database (e.g. latest user address or current product price).
    - **Tombstones**: To delete a key, a producer sends a record with the key and a `null` value. The cleaner thread retains this tombstone for `delete.retention.ms` so downstream consumers observe the deletion before the key is permanently removed.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q15KafkaLogCompactionMechanics.java"
    ```

---

### When should you choose Kafka Streams over standard Spring Kafka consumers, and how do KTable / RocksDB work?

Contrast microservice event handling with stateful stream processing.

??? question "Reveal answer"
    - **Standard Spring Kafka Consumer**: Best suited for stateless event routing, point-to-point dispatching, and triggering external microservice operations (e.g. database updates, REST calls).
    - **Kafka Streams**: Purpose-built Java client library for stateful streaming computations: continuous windowed aggregations, stream-stream joins, and stream-table lookups.
    - **KTable & RocksDB**: A `KTable` represents an up-to-date state view changelog. State is maintained in embedded RocksDB key-value stores on local disk for sub-millisecond query access, backed up automatically to internal Kafka changelog topics.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q16KafkaStreamsVsStandardConsumer.java"
    ```
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Architectural & Deep Dive

### How do linger.ms, batch.size, and compression.type drastically increase producer throughput?

Detail batch accumulator mechanics and compression trade-offs.

??? question "Reveal answer"
    - **Batch Accumulator**: Producers do not send records immediately. Instead, records are appended to in-memory byte buffers partitioned by topic-partition.
    - **`batch.size` & `linger.ms`**: A batch is dispatched when either the accumulated memory reaches `batch.size` (e.g. 64 KB) or the time delay `linger.ms` (e.g. 10–20 ms) expires. Setting `linger.ms > 0` allows small delays to group hundreds of individual records into large batches.
    - **Batch Compression**: Whole batches are compressed together with `compression.type=lz4` or `snappy`. Compressing whole batches yields vastly superior compression ratios than compressing individual records, minimizing broker network I/O and disk write amplification.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q17KafkaBatchingOptimization.java"
    ```

---

### What is the difference between Eager Rebalancing and Cooperative Sticky Rebalancing?

Analyze stop-the-world partition revocations versus incremental non-blocking reassignments.

??? question "Reveal answer"
    - **Eager Rebalancing (`RangeAssignor`, `RoundRobinAssignor`)**: Implements a "stop-the-world" protocol. When any consumer joins or leaves, all consumers in the group immediately revoke all their assigned partitions and stop processing. A temporary slow consumer or heartbeat hiccup halts processing for the entire consumer group.
    - **Cooperative Sticky Rebalancing (`CooperativeStickyAssignor`)**: Partitions are reassigned incrementally over two phases. Consumers retain healthy partitions and continue consuming without interruption. Only partitions that need to be migrated from one consumer to another are paused and transferred, virtually eliminating group-wide downtime.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q18CooperativeStickyAssignor.java"
    ```

---

### How do Java 21 Virtual Threads integrate with Spring Kafka listeners, and what are the concurrency benefits?

Evaluate virtual thread adoption in Kafka consumers and partition-level parallelism boundaries.

??? question "Reveal answer"
    - **Partition Concurrency Boundary**: Virtual threads do not alter Kafka's fundamental rule: a single partition can only be consumed by at most one consumer thread per group. Setting 1,000 virtual threads for a topic with 10 partitions will leave 990 threads completely idle.
    - **Downstream Async Offloading**: Virtual threads provide huge advantages when listeners perform blocking I/O (e.g. database calls or external HTTP requests). By configuring Spring Boot 3.2+ with `spring.threads.virtual.enabled=true`, task execution and asynchronous listener callbacks run on virtual threads, eliminating thread starvation and memory overhead without complex reactive programming.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q19VirtualThreadsWithKafkaListener.java"
    ```

---

### How does Kafka utilize OS page cache zero-copy data transfer (FileChannel.transferTo / sendfile)?

Walk through Linux kernel socket buffers, DMA transfers, and user space bypass.

??? question "Reveal answer"
    - **Standard I/O Overhead**: In standard file transfers, data is read from disk into OS page cache, copied to JVM user space, copied back to OS socket buffer, and copied to NIC buffer (4 context switches, 2 CPU copies).
    - **Zero-Copy Optimization**: Via `FileChannel.transferTo()` / `sendfile()`, the kernel transfers bytes directly from OS Page Cache to the NIC buffer via DMA gather descriptors.
    - **Performance Impact**: Bypasses JVM user space completely. Brokers consume negligible CPU during high-volume reads and generate zero JVM heap allocations, achieving near-line-rate network saturation.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q20KafkaZeroCopyTransferTo.java"
    ```

---

### What are the architectural trade-offs among RDBMS unique constraints, Redis SETNX, and Bloom filters for idempotent consumer deduplication?

Compare consistency, throughput, and operational complexity across deduplication backends.

??? question "Reveal answer"
    - **RDBMS Unique Constraint**: Insert event ID into a `processed_events` table in the same ACID transaction as the business mutation. Provides absolute consistency and atomic rollbacks, but increases database write IOPS.
    - **Redis `SETNX` with TTL**: Ultra-low latency (~1ms) and automatic memory reclamation via TTL. However, if the subsequent database write fails after the Redis key is set, the event will not be reprocessed on redelivery unless explicitly compensated.
    - **Sliding Window Bloom Filter**: Negligible memory footprint capable of checking billions of IDs in RAM. However, probabilistic false positives can cause rare valid messages to be falsely discarded as duplicates unless backed by a secondary store.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q21MessageDeduplicationStoreDesigns.java"
    ```
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Production Incidents & Scenarios

### Production incident post-mortem: How an unmonitored Stop-The-World JVM GC pause triggered a cascading consumer rebalance storm across a 40-node Kafka consumer group.

Analyze root cause, coordinator timeout mechanics, and remediation strategy.

??? question "Reveal answer"
    - **Incident Timeline**: A memory leak caused a 45-second Full GC pause on Consumer Node 1. The JVM froze both the application poll loop and the heartbeat thread. The Group Coordinator exceeded `session.timeout.ms` (45s), marked Node 1 dead, and triggered a rebalance.
    - **Cascading Failure**: Under eager rebalancing, all 40 consumers revoked all their partitions simultaneously. When Node 2 inherited Node 1's partitions, it faced double the processing queue, experienced elevated GC pressure, and also missed its poll timeout, triggering an endless cascading rebalance loop with zero aggregate throughput for 2 hours.
    - **Remediation**:
        1. Migrated partition assignment to `CooperativeStickyAssignor` to eliminate global partition revocation.
        2. Adopted Java 21 Generational ZGC, bringing GC pause times from 45 seconds down to under 1 millisecond.
        3. Configured Prometheus alerts on `jvm.gc.pause` and `kafka.consumer.coordinator.rebalance.latency.avg`.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q22RebalanceStormOutageIncident.java"
    ```

---

### Production incident post-mortem: How a malformed JSON payload stalled a financial clearing topic for 6 hours due to missing Dead Letter Topic (DLT) error handling.

Diagnose poison pill behavior, partition starvation, and Spring Kafka resilience configuration.

??? question "Reveal answer"
    - **Incident Timeline**: An upstream partner published records with unexpected null fields. The downstream consumer's Jackson deserializer threw an unhandled exception. The listener error handler retried indefinitely with fixed backoff.
    - **Partition Starvation**: Because Kafka offsets cannot advance past an uncommitted or continuously failing offset, Partition 3 was completely blocked. 250,000 valid settlement instructions queued behind the malformed record were delayed for 6 hours.
    - **Remediation**:
        1. Configured Spring Kafka `ErrorHandlingDeserializer` to trap deserialization errors and delegate them to the error handler.
        2. Configured `DefaultErrorHandler` with a `DeadLetterPublishingRecoverer` to publish unrecoverable records directly to `financial-clearing.DLT`.
        3. Configured alerts on per-partition consumer lag to trigger PagerDuty when lag exceeds 500 records on any single partition.

??? example "Example"
    ```java
    --8<-- "modules/14-kafka/src/examples/java/lab/kafka/questions/Q23PoisonPillInfiniteLoopIncident.java"
    ```
<!-- --8<-- [end:scenarios] -->
