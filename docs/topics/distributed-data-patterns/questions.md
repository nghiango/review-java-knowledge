# Distributed Data Interview Questions

Answer each question before expanding its explanation.

<!-- --8<-- [start:basic] -->
## Basic Concepts

### Why does Two-Phase Commit (2PC) fail to scale in distributed microservice architectures?

Detail the blocking coordinator problem, network partition vulnerability, and latency amplification.

??? question "Reveal answer"
    - **The Blocking Problem**: In 2PC, all participants acquire row or table locks in Phase 1 (Prepare). If the coordinator crashes before issuing Commit or Abort in Phase 2, participants must hold locks indefinitely, freezing dependent business operations.
    - **Availability Penalty**: Under CAP theorem, 2PC prioritizes strict consistency ($C$) over availability ($A$). If any single participant or network connection drops, the entire transaction fails, reducing total availability exponentially ($A_{\text{total}} = A_1 \times A_2 \times \dots \times A_n$).
    - **Latency Amplification**: Coordinating locks across multiple network hops and remote data centers turns sub-millisecond local locks into multi-second distributed bottlenecks, causing connection pool starvation.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q01DistributedTransactionsTwoPhaseCommitPitfallsExample.java"
    ```

---

### What is the Dual Write problem, and why cannot `@Transactional` protect writes to both a database and a message broker?

Walk through the failure scenarios: broker crash after DB commit vs DB rollback after broker publish.

??? question "Reveal answer"
    - **No Shared Transaction Manager**: Spring's `@Transactional` manages a local database connection. Apache Kafka, RabbitMQ, and AWS SQS are separate distributed systems with their own network protocols that do not participate in Spring's relational transaction boundaries.
    - **Phantom Events**: If the application sends a message to Kafka first and the subsequent database commit fails (e.g. unique constraint violation), downstream services act upon an event that was rolled back and never existed.
    - **Silent Event Loss**: If the application commits the database transaction first and crashes before or during message broker transmission, the database mutation exists, but downstream services are never notified, corrupting distributed system consistency.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q02DualWriteProblemDbAndBrokerExample.java"
    ```

---

### How does the Transactional Outbox pattern guarantee atomic event publication?

Explain the role of the local database transaction, the outbox table, and the message relay worker.

??? question "Reveal answer"
    - **Local Transaction Atomicity**: Instead of publishing directly to Kafka, the service writes the domain state mutation and an event record into an `outbox_events` table within the **exact same local database transaction**.
    - **All-or-Nothing Guarantee**: If the business write succeeds, the outbox event is guaranteed to persist; if the business write rolls back, the outbox event is rolled back.
    - **Asynchronous Relay**: A separate background process (polling worker or CDC connector) reads the outbox table and publishes events to the broker, ensuring at-least-once delivery without distributed locks.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q03TransactionalOutboxPatternMechanicsExample.java"
    ```

---

### Compare Polling Outbox vs Change Data Capture (CDC / Debezium) for event relaying.

Analyze database query load, latency, table locking, and infrastructure complexity.

??? question "Reveal answer"
    - **Polling Outbox**: Queries the outbox table using `SELECT FOR UPDATE SKIP LOCKED` on a scheduled interval. Simple to implement with standard SQL, but adds recurring database query load, creates MVCC table bloat from frequent updates/deletes, and introduces polling latency ($100\text{ms} - 1\text{s}$).
    - **Change Data Capture (Debezium)**: Tails the database transaction log directly (PostgreSQL Write-Ahead Log or MySQL binlog). Incurs near-zero database CPU overhead, produces sub-millisecond publishing latency, and eliminates table polling, at the cost of deploying and managing Kafka Connect infrastructure.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q04ChangeDataCaptureDebeziumVsPollingOutboxExample.java"
    ```

---

### How does the Inbox Pattern achieve idempotent consumer processing?

Detail atomic insertion, unique constraints, and duplicate event discard semantics.

??? question "Reveal answer"
    - **The Problem**: Message brokers guarantee at-least-once delivery; consumer crashes or offset commit timeouts cause duplicate event delivery.
    - **Inbox Pattern**: The consumer writes the incoming event ID into an `inbox_messages` table with a primary key constraint (`PRIMARY KEY (message_id, consumer_group)`) within the **same local database transaction** that executes the business mutation.
    - **Atomic Deduplication**: If the event ID was already processed, the unique constraint causes `INSERT ... ON CONFLICT DO NOTHING` to return zero affected rows. The consumer safely acknowledges the message to Kafka and skips the business mutation.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q05IdempotentConsumerAndInboxPatternExample.java"
    ```

---

### Compare Saga Orchestration versus Saga Choreography.

Evaluate decoupling, visibility, testing complexity, and risk of cyclic dependencies.

??? question "Reveal answer"
    - **Saga Choreography**: Services listen to domain events and independently decide when to execute local transactions and emit subsequent events. Highly decoupled and decentralized, but difficult to monitor, hard to test end-to-end, and susceptible to cyclic event storms.
    - **Saga Orchestration**: A central orchestrator (state machine) explicitly sends command messages to participant services, waits for responses, and coordinates compensating workflows on failure. Provides centralized visibility, straightforward testing, and clear workflow tracking, with the trade-off of slight coupling to the central coordinator.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q06SagaPatternOrchestrationVsChoreographyExample.java"
    ```

---

### Why must Saga compensating transactions be strictly idempotent?

Analyze compensation redelivery and double-refund financial loss scenarios.

??? question "Reveal answer"
    - **At-Least-Once Delivery of Compensations**: If an orchestrator or broker triggers a compensating action (e.g. `refundPayment`), network timeouts on response receipt will cause the orchestrator to retry the compensation command.
    - **Non-Idempotent Hazard**: If the compensation method blindly credits an account balance without recording execution state, retried compensations will refund the customer multiple times.
    - **Remediation**: Compensating transactions must record a unique transaction reference (e.g. `refunded_orders` table) and ensure repeated executions produce no additional side effects.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q07IdempotentCompensatingTransactionsExample.java"
    ```

---

### What is a Pivot Transaction in a Saga, and how does it divide compensatable from retryable steps?

Explain the point of no return in distributed workflow execution.

??? question "Reveal answer"
    - **Compensatable Steps**: Initial transactions executed before the pivot that can be undone via compensating transactions if an error occurs.
    - **Pivot Transaction**: The definitive commitment step (e.g. charging an external credit card). Once the pivot transaction commits, the Saga **cannot be aborted or rolled back**.
    - **Retryable Steps**: Transactions executed after the pivot (e.g. provisioning cloud resources, sending user receipt). Because the pivot already succeeded, retryable steps must be guaranteed to succeed eventually via bounded retries, alerting, or manual operator queues.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q08SemanticLockingAndPivotTransactionsInSagasExample.java"
    ```
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Patterns

### How can an application provide Read-Your-Own-Writes consistency in eventually consistent multi-replica systems?

Detail version tracking, session affinity, and routing reads to primary nodes.

??? question "Reveal answer"
    - **The Problem**: In eventually consistent architectures (or CQRS read replicas), asynchronous replication lag causes users to perform a mutation (e.g. update profile) and immediately refresh, only to observe stale data from a lagging replica.
    - **Remediation Strategies**:
        1. **Version / Timestamp Token**: The mutation returns a monotonic revision or timestamp. The client sends this token in read requests; replicas compare their current sync version and stall or redirect to the primary if lagging.
        2. **Session Stickiness to Primary**: Route all read requests for a user to the primary database for a short window (e.g. 5 seconds) after any write operation.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q09EventualConsistencyReadYourOwnWritesExample.java"
    ```

---

### Why is end-to-end "Exactly-Once" delivery a fallacy, and why is it actually At-Least-Once + Idempotency?

Explain TCP packet retransmission, broker deduplication boundaries, and consumer side-effects.

??? question "Reveal answer"
    - **The Fallacy**: Over unreliable networks, no protocol can guarantee that a message is delivered across network hops exactly once. If a broker receives a message and acknowledges it, but the TCP ACK is lost in flight, the sender must retry, resulting in duplicate delivery.
    - **Kafka "Exactly-Once" (EOS)**: Kafka's transactional API provides exactly-once processing *strictly within Kafka's boundary* (Kafka-topic-to-Kafka-topic).
    - **End-to-End Reality**: As soon as processing involves external databases, third-party APIs, or email providers, exactly-once semantics collapse. True end-to-end exactly-once is achieved solely by: **At-Least-Once Delivery + Idempotent Consumer State Storage**.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q10ExactlyOnceDeliveryFallaciesAndAtLeastOnceRealityExample.java"
    ```

---

### How does PostgreSQL SELECT FOR UPDATE SKIP LOCKED prevent race conditions and lock contention in polling outbox publishers?

Contrast standard SELECT FOR UPDATE with SKIP LOCKED in horizontal scaling.

??? question "Reveal answer"
    - **Standard `FOR UPDATE` Flaw**: If Pod 1 executes `SELECT ... FOR UPDATE LIMIT 50`, Pod 2 executing the same query blocks waiting for Pod 1's locks to release, serializing workers and causing lock timeouts.
    - **`SKIP LOCKED` Advantage**: Instructs the PostgreSQL query planner to automatically bypass any rows currently locked by another active transaction.
    - **Horizontal Scalability**: Multiple outbox worker pods can poll the database simultaneously without blocking, lock collisions, or deadlocks, each claiming an independent batch of rows.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q11OutboxPollingPublisherSkipLockedQueryExample.java"
    ```

---

### How should Dead Letter Queues (DLQ) be integrated into distributed data pipelines to prevent partition blocking?

Examine poison pill quarantine, non-retryable exception routing, and manual replay tooling.

??? question "Reveal answer"
    - **Partition Blockage**: In partitioned message systems like Kafka, if a consumer fails repeatedly on a poison pill record, the consumer offset cannot advance, blocking all subsequent healthy messages in that partition.
    - **Dead Letter Isolation**: Bounded retries with exponential backoff should attempt recovery for transient errors. If failures persist or if a permanent deserialization error is encountered, the message must be routed to a `.DLT` (Dead Letter Topic).
    - **Audit & Replay**: The DLQ retains the original payload, failure timestamp, exception stack trace, and correlation ID, enabling alerting and administrative replay once bugs are patched.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q12DeadLetterQueuesInDistributedDataPipelinesExample.java"
    ```

---

### Contrast Event Sourcing with the Transactional Outbox pattern.

Analyze state reconstruction, audit history, schema evolution, and querying flexibility.

??? question "Reveal answer"
    - **Transactional Outbox**: Maintains current mutable entity state in relational tables for easy querying and indexing, and uses an outbox table purely as an egress buffer to publish domain events.
    - **Event Sourcing**: The immutable event append-only log IS the primary source of truth. Entity state is reconstructed on-the-fly by replaying historical events. Provides perfect auditability and temporal queries, but introduces high complexity for schema evolution, snapshotting, and ad-hoc SQL querying.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q13EventSourcingVsTransactionalOutboxExample.java"
    ```

---

### How does the Try-Confirm-Cancel (TCC) pattern differ from standard Saga compensation?

Explain two-phase reservation of business resources versus backward rollback.

??? question "Reveal answer"
    - **Standard Saga**: Directly executes mutations in Phase 1 (e.g. deduct money). If later steps fail, it issues an undo transaction (e.g. refund money). During execution, intermediate state is visible to other users (dirty read anomaly).
    - **TCC Pattern**:
        - **Try**: Reserves business resources explicitly (e.g. sets account balance = 80, reserved = 20).
        - **Confirm**: Finalizes the reservation permanently if all services succeed.
        - **Cancel**: Releases the reserved resources back to available balance if any service fails.
    - **Benefit**: Eliminates negative balance anomalies and semantic dirty reads by isolating reserved capacity.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q14TccTryConfirmCancelPatternExample.java"
    ```

---

### How do Debezium CDC connectors handle database entity deletions using Kafka tombstone records?

Explain Kafka log compaction, null payloads, and downstream consumer cache eviction.

??? question "Reveal answer"
    - **CDC Deletion Semantics**: When a row is deleted in PostgreSQL, Debezium captures the event from the WAL and produces two messages to Kafka:
        1. A deletion event containing the previous row state (`op: 'd'`).
        2. A **Tombstone Record**: a message with the entity ID as the key and a `null` value payload.
    - **Log Compaction**: When Kafka's background log cleaner processes a compacted topic, the tombstone record signals the broker to delete the key permanently from the segment after the tombstone retention period.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q15CdcTombstoneRecordsAndKafkaCompactionExample.java"
    ```

---

### What is the "Listen to Yourself" pattern, and when does it replace local database updates?

Analyze event publication priority, ordering guarantees, and command-query separation.

??? question "Reveal answer"
    - **Mechanism**: When an application receives a mutation command (e.g. `POST /orders`), it **does not** write to its local database immediately. Instead, it publishes an `OrderPlacementRequested` event to Kafka.
    - **Self-Consumption**: The same service (and all downstream services) listens to the Kafka topic. Local state is updated only when the service consumes its own event from the broker.
    - **Benefit**: Guarantees that the originating service and all replicas process state mutations in the exact total ordering established by the Kafka partition, completely eliminating Dual Write hazards.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q16ListenToYourselfPatternExample.java"
    ```
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Engineering

### How do you mitigate database table bloat and performance degradation in high-throughput Outbox tables?

Contrast single-row deletes, status updates, and PostgreSQL declarative table partitioning.

??? question "Reveal answer"
    - **The Table Bloat Hazard**: In PostgreSQL's MVCC architecture, deleting millions of outbox rows produces dead tuples. Continuous row-by-row `DELETE` queries cause index bloat, vacuum contention, and slow sequential scans.
    - **Production Architecture**:
        1. **Range Partitioning**: Partition `outbox_events` by date or hour (`PARTITION BY RANGE (created_at)`).
        2. **Status Updates**: Mark processed records via fast in-place status updates: `UPDATE outbox_events SET status = 'PROCESSED'`.
        3. **Partition Dropping**: Schedule a periodic maintenance task to drop expired partition tables (`DROP TABLE outbox_events_2026_09_15`). Dropping a partition is an instantaneous $O(1)$ metadata DDL operation that reclaims disk space immediately with zero vacuum overhead.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q17OutboxCleanupPartitioningAndRetentionExample.java"
    ```

---

### How do you propagate W3C Trace Context across asynchronous Outbox and Kafka boundaries for end-to-end distributed tracing?

Detail traceparent headers, OpenTelemetry injectors/extractors, and span linkage across asynchronous boundaries.

??? question "Reveal answer"
    - **The Challenge**: Because the Outbox Publisher runs asynchronously minutes or seconds after the original HTTP request completed, standard thread-local trace context (`Tracer.currentSpan()`) is lost.
    - **Context Capture**: During the initial business transaction, extract the active W3C `traceparent` and `tracestate` headers and store them as metadata columns in the `outbox_events` table.
    - **Header Injection**: When the outbox relay publishes to Kafka, it injects these stored trace headers into Kafka `RecordHeaders`.
    - **Context Extraction**: Downstream consumers extract the headers, linking the consumer execution span directly to the original client transaction span in Jaeger or OpenTelemetry.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q18DistributedTracingTraceIdPropagationInEventsExample.java"
    ```

---

### How must a Saga Coordinator recover and resume workflows if the coordinator node crashes mid-flight?

Analyze persistent Saga Execution Logs, leader election, and timeout-based reconciliation.

??? question "Reveal answer"
    - **Saga Execution Log**: The coordinator must persist every state transition (step started, step succeeded, step failed) to an ACID database or event log before dispatching commands.
    - **Reboot Recovery**: When a new coordinator boots or acquires the partition lease, it scans the persistent log for unfinished sagas:
        - If a step was in-flight, it queries the participant's status using the unique `saga_id`.
        - If a step failed, it resumes the compensation chain from the last completed step.
    - **Idempotency Requirement**: Because the coordinator cannot know if a command reached a participant before crashing, all commands and compensations must be idempotent.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q19SagaFailureRecoveryDuringCoordinatorRestartExample.java"
    ```

---

### Why can't Kafka Transactions replace the Transactional Outbox pattern when coordinating between an RDBMS and Kafka?

Explain the transaction coordinator scope, two-phase commit limitations, and external storage boundaries.

??? question "Reveal answer"
    - **Scope of Kafka Transactions**: Kafka transactions (`KafkaProducer.beginTransaction()`) provide atomic multi-topic writes and atomic consumer offset commits *strictly within the Kafka cluster*.
    - **No Cross-System Coordination**: Kafka cannot participate in an XA or 2PC transaction with PostgreSQL, MySQL, or Oracle. You cannot wrap `dataSource.getConnection()` and `kafkaProducer.send()` in an atomic transaction.
    - **Outbox Necessity**: The Transactional Outbox pattern remains the only robust pattern to bridge the gap between relational databases and Apache Kafka without distributed two-phase commit overhead.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q20KafkaTransactionsVsOutboxPatternExample.java"
    ```

---

### Compare Twitter Snowflake, UUIDv7, and standard UUIDv4 for distributed event and aggregate ID generation.

Analyze time-ordering, B-Tree index page splits, random I/O, and coordination requirements.

??? question "Reveal answer"
    - **UUIDv4**: Completely random 128-bit number. When used as a database primary key, random values cause severe B-Tree index fragmentation, frequent page splits, and excessive random disk I/O.
    - **UUIDv7**: Modern RFC 9562 standard. Encodes a 48-bit millisecond Unix timestamp followed by random bits. Monotonically increasing over time, maximizing B-Tree index locality and sequential write throughput with zero node coordination.
    - **Twitter Snowflake**: 64-bit integer encoding timestamp, worker machine ID, and sequence counter. Highly compact (8 bytes vs 16 bytes), but requires managing unique worker IDs across distributed nodes.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q21DistributedIdGenerationSnowflakeVsUuidV7Example.java"
    ```
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Production Incidents & Scenarios

### Production incident post-mortem: How a direct dual-write implementation caused 12,000 orders to be charged without ever being fulfilled during a Kafka broker restart.

Diagnose root cause, uncommitted Kafka events, and outbox recovery implementation.

??? question "Reveal answer"
    - **Incident Timeline**: During a scheduled rolling restart of the Kafka cluster, the broker returned intermittent connection timeouts. An order service used direct dual write (`orderRepository.save(order); kafkaTemplate.send("orders", event);`).
    - **Failure Chain**: Because the database transaction committed before `kafkaTemplate.send()` completed, the Kafka call failed with a timeout exception. The orders were committed in PostgreSQL, and customers' cards were charged. However, downstream fulfillment services never received the Kafka event. 12,000 orders were stuck in limbo with no warehouse processing.
    - **Remediation**:
        1. Implemented the Transactional Outbox pattern: orders and outbox records commit in the same local database transaction.
        2. Added an Outbox Publisher worker with retries and broker delivery confirmations.
        3. Backfilled missing events from historical database records to resume warehouse fulfillment.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q22IncidentDualWriteDataLossDuringBrokerOutageExample.java"
    ```

---

### Production incident post-mortem: A non-idempotent Saga compensation loop refunded \$450,000 of customer payments multiple times following consumer offset commit timeouts.

Analyze message redelivery, missing compensation ledger tables, and idempotent remediation.

??? question "Reveal answer"
    - **Incident Timeline**: A downstream inventory service failed, triggering a Saga compensation event `OrderCancelledEvent`. A payment microservice received the event and issued a customer refund. However, due to high database load, the consumer's Kafka offset commit timed out after applying the refund.
    - **Cascading Overpayment**: Kafka considered the message unacknowledged and redelivered the event across several consumer rebalances. Because the refund method lacked an idempotency check, each redelivery executed another credit card refund. Some customers received 5 to 8 refunds for a single cancelled order.
    - **Remediation**:
        1. Introduced an idempotent `processed_compensations` table with a `UNIQUE(order_id, action)` constraint.
        2. Wrapped balance refunds and compensation audit insertion in the same database transaction.
        3. Reclaimed overpaid funds through financial payment processor dispute workflows.

??? example "Example"
    ```java
    --8<-- "modules/19-distributed-data-patterns/src/examples/java/lab/distributeddata/questions/Q23IncidentSagaCompensationLoopDoubleRefundExample.java"
    ```
<!-- --8<-- [end:scenarios] -->
