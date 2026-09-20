# RabbitMQ Interview Questions

Answer each question before expanding its explanation.

<!-- --8<-- [start:basic] -->
## Basic Concepts

### What is the core AMQP 0-9-1 architecture (Brokers, Virtual Hosts, Exchanges, Queues, Bindings)?

Explain the decoupling between publishers and queues, and the role of bindings and routing keys.

??? question "Reveal answer"
    - **Decoupled Topology**: In AMQP 0-9-1, producers never publish directly to queues; they publish to Exchanges with a routing key. Exchanges inspect message attributes and route copies to bound queues based on binding keys.
    - **Virtual Hosts (`vhosts`)**: Logical namespaces within a RabbitMQ cluster providing security boundaries, permission isolation, and independent exchange/queue definitions.
    - **Queues**: In-memory and on-disk FIFO buffers managed by dedicated Erlang actor processes. Messages are held until consumed and acknowledged by clients.
    - **Bindings**: The routing relationship between an exchange and a queue, optionally defining binding keys or header matching rules.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q01RabbitMqArchitectureExchangesQueues.java"
    ```

---

### What are the routing semantics of Direct, Fanout, Topic, and Headers exchanges?

Compare exact matching, broadcasting, pattern matching with wildcards (* and #), and metadata matching.

??? question "Reveal answer"
    - **Direct Exchange**: Routes messages based on an exact string match between the message routing key and the queue binding key.
    - **Fanout Exchange**: Broadcasts messages to all bound queues unconditionally, completely ignoring the routing key. Provides highest routing throughput.
    - **Topic Exchange**: Pattern-based routing supporting dot-delimited words and wildcards:
        - `*` (star) matches exactly one word (e.g. `audit.*.login`).
        - `#` (hash) matches zero or more words (e.g. `audit.#`).
    - **Headers Exchange**: Routes based on message header key-value pairs (`x-match: all` or `x-match: any`), completely ignoring the routing key string.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q02ExchangeTypesRoutingSemantics.java"
    ```

---

### What are the differences among durable queues, durable exchanges, and persistent messages (delivery_mode=2)?

Detail broker reboot survival, transient messages, and fsync disk persistence.

??? question "Reveal answer"
    - **Durable Queue**: The queue's metadata survives broker restarts. However, any transient messages buffered inside it are permanently lost on reboot.
    - **Durable Exchange**: The exchange definition survives broker restarts.
    - **Persistent Message (`delivery_mode = 2`)**: The message body and properties are written to an on-disk commit log and flushed via `fsync`. Persistent messages inside a durable queue survive broker crashes and reboots.
    - **Full Durability**: True zero-loss resilience requires all three settings: a durable exchange routing persistent messages (`delivery_mode = 2`) into durable queues.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q03MessageDurabilityAndPersistence.java"
    ```

---

### How do basic.ack, basic.nack, basic.reject, and the requeue flag control message lifecycle in RabbitMQ?

Explain positive acknowledgment, single rejection, batch rejection, and the poison pill requeue hazard.

??? question "Reveal answer"
    - **`basic.ack(deliveryTag, multiple)`**: Confirms successful processing. RabbitMQ purges the message from queue memory. `multiple = true` acknowledges all unacknowledged messages up to that delivery tag.
    - **`basic.reject(deliveryTag, requeue)`**: Negatively acknowledges a single message. If `requeue = true`, RabbitMQ reinserts the message at the head of the queue. If `requeue = false`, it drops the message or routes to a DLX.
    - **`basic.nack(deliveryTag, multiple, requeue)`**: Same as `basic.reject`, but supports acknowledging multiple messages simultaneously via `multiple = true`.
    - **Poison Pill Trap**: Requeueing a message (`requeue = true`) that fails due to deterministic validation or deserialization errors creates an infinite 100% CPU crash loop, redelivering the failing message indefinitely.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q04AckNackRejectRequeueMechanics.java"
    ```

---

### How do x-dead-letter-exchange, x-dead-letter-routing-key, and x-message-ttl route dead letters?

Detail dead-letter triggers, TTL expiration, and queue arguments.

??? question "Reveal answer"
    - **Dead-Letter Conditions**: A message is forwarded to a Dead Letter Exchange (DLX) when:
        1. A consumer rejects or nacks with `requeue = false`.
        2. A message expires due to per-message or per-queue TTL (`x-message-ttl`).
        3. A queue exceeds its length limit (`x-max-length` or `x-max-length-bytes`).
    - **Queue Arguments**:
        - `x-dead-letter-exchange`: The target exchange to publish dead-lettered messages.
        - `x-dead-letter-routing-key`: Optional replacement routing key for dead letters.
    - **Data Loss Warning**: If a message expires or is rejected with `requeue = false` on a queue that has no DLX configured, RabbitMQ silently and permanently drops the message.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q05DeadLetterExchangeAndTtl.java"
    ```

---

### How does basic.qos prefetch count enforce fair dispatch across competing consumers?

Contrast round-robin push with fair dispatching and worker starvation prevention.

??? question "Reveal answer"
    - **Unbounded Prefetch (`prefetch = 0`)**: RabbitMQ pushes all available queue messages across the network to connected consumers in a round-robin loop. If one worker receives a batch of heavy tasks, it buffers all of them in memory while other workers sit idle, causing worker starvation and potential `OutOfMemoryError`.
    - **Fair Dispatch (`prefetch = 1`)**: The broker delivers at most 1 unacknowledged message to a worker. RabbitMQ will not send another task until the worker returns `basic.ack`, dynamically balancing load according to individual worker processing speeds.
    - **Throughput Tuning**: For lightweight, high-volume messages, setting `prefetch = 20–100` saturates the network socket pipeline without overwhelming client memory.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q06PrefetchAndQosFairDispatch.java"
    ```

---

### How do publisher confirms and return callbacks guarantee message arrival and routing in RabbitMQ?

Detail publisher confirm acknowledgments, CorrelationData, and mandatory routing callbacks.

??? question "Reveal answer"
    - **Publisher Confirms (`confirm-type: correlated`)**: An asynchronous AMQP extension. When enabled, the broker assigns a sequence number to each published message and returns `basic.ack` once the message is committed to disk log or quorum replicas. Spring AMQP pairs this with `CorrelationData` CompletableFutures.
    - **Returns Callback (`mandatory = true`)**: By default, if a published message reaches an exchange but cannot be routed to any queue, RabbitMQ silently drops it. Setting `mandatory = true` instructs the broker to return unroutable messages back to the publisher via `basic.return` and Spring's `ReturnsCallback`.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q07PublisherConfirmsAndReturns.java"
    ```

---

### What are the differences between SimpleMessageListenerContainer and DirectMessageListenerContainer?

Compare internal queue buffers, thread dispatching, dynamic scaling, and acknowledgment modes.

??? question "Reveal answer"
    - **`SimpleMessageListenerContainer` (SMLC)**: Spawns independent listener threads managed by Spring's task executor. Incoming messages from RabbitMQ client channels are queued in an internal Java `BlockingQueue` before being handed to listener methods. Supports dynamic concurrency scaling and batch listeners.
    - **`DirectMessageListenerContainer` (DMLC)**: Eliminates the internal Java queue. Listener methods execute directly on RabbitMQ client connection/channel I/O threads. Yields lower CPU overhead and higher raw throughput, but requires careful thread sizing.
    - **Acknowledgment Modes**:
        - `NONE`: Auto-ack as soon as message is transmitted over TCP (at-most-once).
        - `AUTO`: Container acks on normal return, nacks/requeues on exception.
        - `MANUAL`: Application explicitly calls `channel.basicAck()` / `basicNack()`.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q08SpringRabbitListenerContainer.java"
    ```
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Concepts

### How should consumers detect redelivered messages and guarantee idempotent processing?

Evaluate the amqp_redelivered header and atomic deduplication store implementations.

??? question "Reveal answer"
    - **The `amqp_redelivered` Flag**: RabbitMQ sets `redelivered = true` on messages that were previously dispatched to a consumer that crashed, disconnected, or timed out before sending `basic.ack`.
    - **Insufficiency for Idempotency**: A flag of `redelivered = true` tells you the message was previously delivered, but cannot tell you whether the previous worker succeeded in updating the database before dying.
    - **Production Pattern**: Applications must enforce idempotency using an atomic deduplication store (e.g. database table with unique constraint on `message_id` or Redis `SETNX`). If the ID already exists, the consumer acknowledges the message immediately and skips state mutation.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q09ConsumerIdempotencyPatterns.java"
    ```

---

### How do the rabbitmq_delayed_message_exchange plugin and DLX TTL queues implement exponential retry backoff?

Contrast plugin-based delayed exchanges with dead-letter TTL queue topologies.

??? question "Reveal answer"
    - **Plugin Approach (`x-delayed-message`)**: The `rabbitmq_delayed_message_exchange` plugin stores delayed messages in an internal Erlang Mnesia timer table on the broker and routes them to target queues only after the specified `x-delay` (in ms) expires. Supports arbitrary per-message backoff intervals.
    - **Native AMQP DLX TTL Approach**: Without plugins, declare a chain of retry queues with fixed TTLs (e.g. `retry.10s` with `x-message-ttl = 10000`, `retry.60s` with `x-message-ttl = 60000`). Each retry queue has its `x-dead-letter-exchange` pointing back to the main work exchange. When a message's TTL expires, it automatically dead-letters back to the main work queue.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q10DelayedMessageExchangeRetry.java"
    ```

---

### How do priority queues work in RabbitMQ, and what are the trade-offs regarding consumer starvation?

Detail x-max-priority queue arguments, broker memory overhead, and starvation prevention.

??? question "Reveal answer"
    - **Declaration**: Declared with argument `x-max-priority = N` (typically between 1 and 10).
    - **Broker Overhead**: Each priority level allocates an internal Erlang priority sub-queue, consuming additional broker memory. Setting priority > 10 is strongly discouraged by RabbitMQ documentation.
    - **Consumer Starvation**: If a continuous stream of priority 9 messages arrives, priority 1 messages can remain stuck at the tail of the queue indefinitely (starvation). Systems should implement aging mechanisms or separate high-priority queues with dedicated workers.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q11PriorityQueuesAndFairness.java"
    ```

---

### Why are Quorum Queues (Raft consensus) replacing Classic Mirrored Queues (HA queues)?

Analyze network partition resilience, replication protocols, and x-delivery-count tracking.

??? question "Reveal answer"
    - **Classic Mirrored Queues (HA) Deprecation**: Classic mirroring used custom synchronization protocols that suffered from message loss during network partitions, synchronization pauses on node restarts, and cluster-wide split-brain. Deprecated in RabbitMQ 3.8 and removed in 4.0.
    - **Quorum Queues (`x-queue-type: quorum`)**: Built on the formal Raft consensus algorithm. Writes are replicated to an on-disk write-ahead log across a cluster quorum ($N/2 + 1$), eliminating data loss and split-brain states during network partitions.
    - **Poison Pill Counter**: Quorum queues automatically append an `x-delivery-count` header tracking how many times a message has been redelivered, allowing simple poison pill quarantine via `delivery-limit = 3`.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q12QuorumQueuesVsClassicMirrored.java"
    ```

---

### How do RabbitMQ Memory and Disk alarms enforce backpressure on publishing clients?

Explain vm_memory_high_watermark, disk_free_limit, TCP zero-window freezing, and consumer draining.

??? question "Reveal answer"
    - **Alarm Triggers**:
        - `vm_memory_high_watermark`: Trips when broker RAM exceeds threshold (default 40% of host RAM).
        - `disk_free_limit`: Trips when free disk drops below threshold (default 50MB or 1.5x RAM).
    - **Backpressure Mechanism**: When an alarm trips, RabbitMQ immediately stops reading bytes from publisher TCP sockets (TCP zero-window). Publishing threads in client applications block on socket write buffers without dropping messages.
    - **Consumer Drain**: Crucially, consumer connections are **never** blocked during alarms. Consumers continue pulling and acknowledging messages, draining queue memory so the alarm clears automatically.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q13RabbitMqFlowControlAlarms.java"
    ```

---

### Compare the architectural trade-offs: RabbitMQ (smart broker / dumb consumer) vs Kafka (dumb broker / smart consumer).

Analyze message state tracking, retention, consumer replaying, routing complexity, and throughput ceilings.

??? question "Reveal answer"
    - **RabbitMQ (Smart Broker / Dumb Consumer)**:
        - The broker tracks per-message delivery state, ack statuses, dead-letter routing, and deletes messages immediately upon acknowledgment.
        - Rich routing topologies (wildcards, headers, fanout) managed entirely inside the broker.
        - Best for: Task worker queues, complex routing, per-message TTL/priority, sub-millisecond push latency.
    - **Kafka (Dumb Broker / Smart Consumer)**:
        - The broker is an append-only commit log; does not track individual message acks. Consumers manage their own offset positions.
        - Messages persist based on retention time/size, allowing consumers to replay historical streams.
        - Best for: High-throughput event streaming (millions/sec), event sourcing, stream processing, total ordering per partition.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q14RabbitMqVsKafkaArchitecture.java"
    ```

---

### How does Jackson2JsonMessageConverter serialize AMQP messages, and how does __TypeId__ mapping work?

Detail JSON standardization, class mapping security, and cross-service interoperability.

??? question "Reveal answer"
    - **Standardization**: Replaces default Java binary serialization with clean UTF-8 JSON payloads.
    - **`__TypeId__` Header**: By default, Spring AMQP attaches a `__TypeId__` header containing the publisher's fully qualified Java class name. If a consuming microservice lives in a different package or repository, Jackson deserialization fails with `ClassNotFoundException`.
    - **Remediation**: Configure `DefaultClassMapper` with `setTypePrecedence(TYPE_ID)` and custom `idClassMapping` maps, or configure the listener to receive untyped `Message` or generic DTO classes without relying on publisher class paths.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q15SpringAmqpMessageConverters.java"
    ```

---

### How does CachingConnectionFactory manage AMQP connections and channels, and what is channel churn?

Explain connection pooling, Channel CacheMode, channelCacheSize, and thread synchronization.

??? question "Reveal answer"
    - **Connection Multiplexing**: TCP connections in AMQP are expensive (TLS handshakes, auth). `CachingConnectionFactory` maintains a long-lived shared Connection, pooling and multiplexing lightweight Channels across threads.
    - **Cache Modes**:
        - `CHANNEL` (default): Single shared connection with a pool of cached channels up to `channelCacheSize`.
        - `CONNECTION`: Multiple connections cached, each with its own channel pool.
    - **Channel Churn**: If `channelCacheSize` is left at the default (25) while consumer/producer concurrency is high (e.g. 100), channels that exceed the cache limit are continuously opened and closed on every message. This creates heavy Erlang process creation overhead and network latency. Set `channelCacheSize` equal to or greater than peak concurrency.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q16ConnectionAndChannelManagement.java"
    ```
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Architectural & Deep Dive

### How does the rabbitmq_consistent_hash_exchange enable horizontal partition-like queue sharding?

Detail Erlang queue concurrency limits and partition-like ordering with consistent hashing.

??? question "Reveal answer"
    - **Queue Concurrency Ceiling**: Because a single RabbitMQ queue is backed by exactly one Erlang process running on a single CPU core, maximum throughput per queue tops out at ~30,000–50,000 msg/sec.
    - **Consistent Hash Sharding (`x-consistent-hash`)**: Binds $N$ distinct queues to a single consistent hash exchange. Publishers provide a partition key (e.g. `userId` or `orderId`) in the routing key or header. The exchange hashes the key and routes records to one of the $N$ queues.
    - **Causal Ordering**: Messages sharing the same key always route to the same shard queue, delivering parallel multi-core scalability while preserving per-entity causal ordering.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q17ConsistentHashExchangeSharding.java"
    ```

---

### How do Java 21 Virtual Threads scale Spring AMQP SimpleMessageListenerContainer?

Evaluate virtual thread adoption in AMQP consumers and blocking I/O throughput.

??? question "Reveal answer"
    - **TaskExecutor Offload**: `SimpleMessageListenerContainer` uses a Spring `TaskExecutor` to dispatch consumer messages to `@RabbitListener` methods.
    - **Virtual Thread Scaling**: Under platform threads, scaling listener concurrency to 500 workers consumes ~500MB of stack memory and causes kernel thread scheduling contention during blocking HTTP/database calls. By assigning `Executors.newVirtualThreadPerTaskExecutor()` as the container factory's task executor, thousands of concurrent listener tasks execute with near-zero memory footprint, unblocking carrier threads during I/O waits.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q18VirtualThreadsWithRabbitListener.java"
    ```

---

### What are the primary RabbitMQ Prometheus metrics, and what do they reveal about cluster health?

Analyze messages_ready, messages_unacked, consumer_capacity, and consumer_utilisation.

??? question "Reveal answer"
    - **`rabbitmq_queue_messages_ready`**: Count of messages waiting in queue to be dispatched. Growth indicates producer throughput exceeds consumer capacity.
    - **`rabbitmq_queue_messages_unacked`**: Count of messages dispatched to consumers awaiting acknowledgment. High values suggest long processing times, unbounded prefetch buffers, or stalled listener threads.
    - **`rabbitmq_queue_consumer_capacity`**: Percentage of time a queue can immediately deliver messages to consumers (1.0 = optimal). Values < 1.0 indicate consumers are busy and messages must wait.
    - **`rabbitmq_queue_consumer_utilisation`**: Percentage of time consumers are ready to receive messages. Low utilisation with non-empty queues points to network latency or prefetch bottlenecks.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q19RabbitMqMetricsAndMonitoring.java"
    ```

---

### How does RabbitMQ handle network partitions, and what are the partition handling modes?

Compare ignore, pause_minority, and autoheal cluster split-brain strategies.

??? question "Reveal answer"
    - **`ignore`**: Does not intervene during a network split. Nodes on both sides continue accepting reads and writes independently, leading to catastrophic split-brain state and divergent queues upon reconnection. Never use in production.
    - **`pause_minority`**: Nodes that detect they are in a minority partition ($\le N/2$ nodes) immediately pause themselves, closing client sockets and rejecting traffic. The majority partition continues serving clients safely without split-brain. When the partition heals, minority nodes resume.
    - **`autoheal`**: When a partition split occurs, the cluster elects a winning partition (the one with the most clients) and restarts all nodes in other partitions, causing temporary downtime on restarted nodes.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q20RabbitMqClusteringNetworkPartitions.java"
    ```

---

### Compare AMQP transactions (tx_select / tx_commit) versus asynchronous publisher confirms.

Detail protocol synchronization overhead and throughput differences.

??? question "Reveal answer"
    - **AMQP Transactions (`txSelect` / `txCommit`)**: Fully synchronous blocking protocol. The publisher issues `txCommit` and blocks until the broker writes the transaction to disk and returns `txCommit-Ok`. Drops publishing throughput by up to 250x, severely crippling broker performance.
    - **Publisher Confirms (`confirmSelect`)**: Asynchronous pipelining. The publisher sends continuous streams of messages with monotonically increasing sequence numbers without blocking. The broker asynchronously sends `basic.ack` frames back to confirm disk persistence. Provides identical durability guarantees with 100x–200x higher throughput.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q21TransactionalPublishingVsConfirms.java"
    ```
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Production Incidents & Scenarios

### Production incident post-mortem: How an unhandled NullPointerException caused 100% CPU lockup and queue stall due to infinite requeue.

Analyze root cause, broker spin loops, and poison pill remediation.

??? question "Reveal answer"
    - **Incident Timeline**: An upstream change produced invoices with missing VAT fields. Downstream listeners threw `NullPointerException`. The error handler invoked `basicNack` with `requeue = true`. RabbitMQ immediately placed the message back at the head of the queue. The consumer re-received the message 0.1ms later and failed again, spinning at 10,000 attempts per second.
    - **Impact**: Worker CPU hit 100%, disk filled with gigabytes of identical stack traces, and 50,000 valid invoices queued behind the poison pill were stalled for 4 hours.
    - **Remediation**:
        1. Configured Quorum Queues with `delivery-limit = 3` to automatically quarantine poison messages.
        2. Configured Spring AMQP `RepublishMessageRecoverer` to send failed records to an error exchange.
        3. Rejected deterministic business exceptions with `requeue = false` immediately.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q22PoisonPillInfiniteRequeueIncident.java"
    ```

---

### Production incident post-mortem: How an unbounded prefetch configuration caused an OutOfMemoryError crash loop across worker pods during a flash sale.

Diagnose push-based message distribution hazards, heap exhaustion, and cascading thundering herds.

??? question "Reveal answer"
    - **Incident Timeline**: A flash sale generated 20,000 PDF invoice rendering tasks. Worker pods had `prefetchCount = 0` (unbounded). The first pod to boot was flooded with 8,000 tasks pushed simultaneously by RabbitMQ. Each task required 2MB of memory; heap consumption surged to 16GB, triggering an OOM kill by the Kubernetes kernel.
    - **Cascading Failure**: When Pod 1 crashed, RabbitMQ requeued all 8,000 unacknowledged tasks and pushed them to Pod 2, immediately crashing Pod 2 in a cascading domino failure across the entire cluster.
    - **Remediation**:
        1. Configured `prefetchCount = 5` on `SimpleRabbitListenerContainerFactory`.
        2. Set Kubernetes resource requests and limits accurately with Prometheus alerts on `rabbitmq_queue_messages_unacked`.
        3. Enforced fair dispatching so each worker only buffers what it can immediately process.

??? example "Example"
    ```java
    --8<-- "modules/15-rabbitmq/src/examples/java/lab/rabbitmq/questions/Q23UnboundedPrefetchOomIncident.java"
    ```
<!-- --8<-- [end:scenarios] -->
