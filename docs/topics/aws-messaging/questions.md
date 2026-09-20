# AWS Messaging Interview Questions

Comprehensive interview questions covering Amazon SQS, Amazon SNS, Amazon EventBridge, queue internals, and cloud-native messaging architectures.

<!-- --8<-- [start:basic] -->
## Basic

### What are the key differences between Amazon SQS Standard and SQS FIFO queues?

??? question "Reveal answer"
    **Short Answer:** SQS Standard provides unlimited throughput and at-least-once delivery with best-effort ordering. SQS FIFO guarantees strict First-In, First-Out ordering and exactly-once processing (within a 5-minute deduplication window) for messages within the same `MessageGroupId`, with a baseline throughput limit of 300 msg/sec (3,000 with batching) or up to 70,000 msg/sec in high-throughput mode.

    ??? example "Example"
        ```yaml
        # Standard Queue vs FIFO Queue declaration
        StandardQueue:
          Type: AWS::SQS::Queue
          Properties:
            QueueName: standard-events-queue

        FifoQueue:
          Type: AWS::SQS::Queue
          Properties:
            QueueName: banking-ledger.fifo
            FifoQueue: true
            ContentBasedDeduplication: true
        ```

### How does the SQS Visibility Timeout work, and what happens when it expires?

??? question "Reveal answer"
    **Short Answer:** When a consumer receives a message, SQS does not delete it; instead, it starts a visibility timeout lease (default 30 seconds) during which the message is invisible to other consumers. If the consumer calls `DeleteMessage` before the timeout expires, the message is permanently removed. If the timeout expires before deletion (due to worker crash or slow execution), SQS makes the message visible again, allowing another worker to poll and process it.

    ??? example "Example"
        ```bash
        # Polling message and extending visibility timeout on slow processing
        RECEIPT_HANDLE=$(aws sqs receive-message --queue-url $QUEUE_URL \
          --query 'Messages[0].ReceiptHandle' --output text)

        # Extend lease by another 60 seconds
        aws sqs change-message-visibility --queue-url $QUEUE_URL \
          --receipt-handle "$RECEIPT_HANDLE" --visibility-timeout 60
        ```

### What is the difference between SQS Short Polling and Long Polling, and why is Long Polling preferred?

??? question "Reveal answer"
    **Short Answer:** Short polling (`WaitTimeSeconds = 0`) samples a subset of SQS distributed storage servers and returns immediately even if no messages are found, causing empty responses and high API billing costs. Long polling (`WaitTimeSeconds = 1-20`) waits up to 20 seconds for messages to arrive on any server before responding, eliminating empty responses, slashing SQS request costs by up to 90%, and reducing delivery latency.

    ??? example "Example"
        ```yaml
        # Enabling queue-level long polling
        TaskQueue:
          Type: AWS::SQS::Queue
          Properties:
            QueueName: worker-task-queue
            ReceiveMessageWaitTimeSeconds: 20 # Enables 20-second long polling
        ```

### What is the maximum message size and retention period supported by Amazon SQS?

??? question "Reveal answer"
    **Short Answer:** Amazon SQS supports a maximum payload size of 256 KB (for text, JSON, XML, or binary data). Messages exceeding 256 KB can be stored using the Amazon SQS Extended Client Library for Java, which stores the payload in Amazon S3 and sends a pointer payload via SQS. The message retention period ranges from 60 seconds to 14 days, with a default of 4 days.

    ??? example "Example"
        ```yaml
        Properties:
          MaximumMessageSize: 262144        # 256 KB max payload
          MessageRetentionPeriod: 1209600   # 14 days maximum retention
        ```

### How does a Dead Letter Queue (DLQ) work in Amazon SQS, and what is maxReceiveCount?

??? question "Reveal answer"
    **Short Answer:** A Dead Letter Queue (DLQ) is an auxiliary SQS queue used to isolate poison pill messages that fail processing repeatedly. When a consumer receives a message, SQS increments `ApproximateReceiveCount`. If this count reaches the configured `maxReceiveCount` in the queue's `RedrivePolicy`, SQS automatically moves the message to the deadLetterTargetArn on the next receive attempt, protecting main queue workers from infinite crash loops.

    ??? example "Example"
        ```json
        {
          "deadLetterTargetArn": "arn:aws:sqs:us-east-1:123456789012:orders-dlq",
          "maxReceiveCount": 5
        }
        ```

### What is Amazon SNS, and how does the SNS-to-SQS fanout pattern work?

??? question "Reveal answer"
    **Short Answer:** Amazon SNS is a fully managed pub/sub messaging service that pushes messages to multiple subscribers. In the SNS-to-SQS fanout pattern, a publisher sends a single event to an SNS topic, and SNS replicates that message asynchronously to multiple SQS queues (e.g. billing, shipping, analytics) subscribed to the topic. This decouples the producer and allows each consumer fleet to process events at its own independent rate.

    ??? example "Example"
        ```mermaid
        flowchart LR
            P["Order Service"] -->|Publish| SNS["SNS: order-events"]
            SNS -->|Fanout| Q1["SQS: Billing Queue"]
            SNS -->|Fanout| Q2["SQS: Inventory Queue"]
            SNS -->|Fanout| Q3["SQS: Notification Queue"]
        ```

### How does Amazon EventBridge differ from Amazon SNS?

??? question "Reveal answer"
    **Short Answer:** SNS is a lightweight, ultra-high-throughput pub/sub service optimized for rapid push fanout (to SQS, HTTP endpoints, SMS, email) with basic attribute filtering. EventBridge is a schema-aware serverless event bus with advanced declarative JSON pattern matching on event bodies, integration with over 30 AWS services, SaaS partner event ingestion (e.g. Datadog, Stripe), event archiving and replay, and native scheduling (EventBridge Scheduler).

    ??? example "Example"
        ```json
        // EventBridge Rule matching specific JSON payload fields
        {
          "source": ["ecommerce.checkout"],
          "detail-type": ["PaymentSuccess"],
          "detail": {
            "amount": [{"numeric": [">=", 500]}]
          }
        }
        ```

### What is an SQS ReceiptHandle, and why is it required to delete a message?

??? question "Reveal answer"
    **Short Answer:** A `ReceiptHandle` is an opaque, ephemeral token generated by SQS every time a message is received via `ReceiveMessage`. It represents the specific delivery lease rather than the permanent message identity (`MessageId`). SQS requires the `ReceiptHandle` for `DeleteMessage` and `ChangeMessageVisibility` to verify that the consumer deleting the message is the one that received that specific active lease, preventing stale workers from deleting redelivered messages.

    ??? example "Example"
        ```bash
        # Deleting message with receipt handle
        aws sqs delete-message \
          --queue-url "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue" \
          --receipt-handle "$RECEIPT_HANDLE"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### How does SQS FIFO message deduplication work using MessageDeduplicationId versus Content-Based Deduplication?

??? question "Reveal answer"
    **Short Answer:** SQS FIFO maintains a 5-minute sliding window of deduplication tokens. When `ContentBasedDeduplication` is enabled, SQS generates a SHA-256 hash of the message body as the deduplication token. Alternatively, producers can explicitly provide a business unique ID via `MessageDeduplicationId`. If a message with the same deduplication token arrives within 5 minutes, SQS accepts it (`HTTP 200`) but does not enqueue a duplicate.

    ??? example "Example"
        ```java
        // AWS SDK v2 Java explicit MessageDeduplicationId
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(fifoQueueUrl)
                .messageBody(payloadJson)
                .messageGroupId(accountId)
                .messageDeduplicationId("tx-uuid-987654321") // Business unique ID
                .build();
        sqsClient.sendMessage(request);
        ```

### How does MessageGroupId enable concurrent processing in an SQS FIFO queue without violating ordering?

??? question "Reveal answer"
    **Short Answer:** `MessageGroupId` acts as a partition key in SQS FIFO. SQS guarantees strict FIFO ordering only *within* the same `MessageGroupId`. When a consumer receives an in-flight message for a specific `MessageGroupId`, SQS locks that group and will not deliver subsequent messages of that same group to any worker until the in-flight message is deleted. However, messages with different `MessageGroupId` values are processed concurrently by other workers in parallel.

    ??? example "Example"
        ```mermaid
        flowchart TD
            subgraph FIFO["SQS FIFO Queue"]
                G1["GroupId: User#101 (Locked to Worker 1)"]
                G2["GroupId: User#202 (Available for Worker 2)"]
                G3["GroupId: User#303 (Available for Worker 3)"]
            end
        ```

### How do SNS Subscription Filter Policies work, and how do they reduce downstream consumer load?

??? question "Reveal answer"
    **Short Answer:** By default, SNS fanout delivers 100% of topic messages to all subscribers. A `FilterPolicy` defined on an SNS subscription instructs the SNS control plane to evaluate message attributes or body JSON against criteria (string matching, prefix, numeric range). Messages that do not match are discarded at the SNS boundary, preventing downstream SQS queues from receiving irrelevant messages and eliminating wasted worker compute.

    ??? example "Example"
        ```yaml
        # SNS Subscription with filter policy
        HighValueOrderSubscription:
          Type: AWS::SNS::Subscription
          Properties:
            TopicArn: !Ref OrderTopic
            Endpoint: !GetAtt FraudQueue.Arn
            Protocol: sqs
            FilterPolicyScope: MessageAttributes
            FilterPolicy:
              eventType: ["OrderPlaced"]
              amount: [{"numeric": [">=", 1000]}]
        ```

### What is the difference between SQS message delay (DelaySeconds) and Visibility Timeout?

??? question "Reveal answer"
    **Short Answer:** `DelaySeconds` (0 to 900 seconds / 15 minutes) postpones the *initial* visibility of a message when it is first published to the queue; no consumer can see or poll the message until the delay elapses. `VisibilityTimeout` (0 to 12 hours) applies *after* a message has been polled by a consumer, hiding it from other workers while it is being processed.

    ??? example "Example"
        ```bash
        # Publishing a message with a 10-minute initial delay
        aws sqs send-message \
          --queue-url $QUEUE_URL \
          --message-body '{"action": "send_abandoned_cart_reminder"}' \
          --delay-seconds 600
        ```

### How does SQS handle batch operations, and how should partial failures be handled?

??? question "Reveal answer"
    **Short Answer:** SQS supports batching up to 10 messages (or 256 KB total) via `SendMessageBatch`, `DeleteMessageBatch`, and `ChangeMessageVisibilityBatch`. Batch operations are not atomic transactions; individual messages can succeed while others fail (e.g. due to throttling). Consumers and producers must inspect the `BatchResultErrorEntry` list in the response and retry failed items individually with exponential backoff.

    ??? example "Example"
        ```java
        // Handling SQS partial batch deletion
        DeleteMessageBatchResponse response = sqsClient.deleteMessageBatch(batchRequest);
        if (!response.failed().isEmpty()) {
            for (BatchResultErrorEntry failure : response.failed()) {
                logger.warn("Failed to delete id: {}, code: {}", failure.id(), failure.code());
                // Retry individual deletion or alert
            }
        }
        ```

### How do you implement dynamic message visibility extension for long-running worker tasks?

??? question "Reveal answer"
    **Short Answer:** For tasks with variable execution times (e.g. 2 to 30 minutes), set an initial conservative `VisibilityTimeout` (e.g. 60 seconds) and run a background heartbeat thread or scheduled task. Every 30 seconds, if the worker thread is still actively processing the job, call `changeMessageVisibility` to extend the lease by another 60 seconds. If the worker crashes, heartbeating stops and the message returns to the queue promptly.

    ??? example "Example"
        ```java
        ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();
        heartbeat.scheduleAtFixedRate(() -> {
            sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .visibilityTimeout(60)
                    .build());
        }, 30, 30, TimeUnit.SECONDS);
        ```

### How does Amazon EventBridge schema discovery and schema registry facilitate event-driven contracts?

??? question "Reveal answer"
    **Short Answer:** EventBridge Schema Discovery automatically inspects events sent to an event bus, detects their structure, and generates OpenAPI 3.0 or JSONSchema specs in the EventBridge Schema Registry. It provides downloadable code bindings (Java, TypeScript, Python) to generate strongly-typed model classes, catching contract drift between producers and consumers at compile time.

    ??? example "Example"
        ```bash
        # Enable schema discovery on custom event bus
        aws schemas create-discoverer --source-arn "arn:aws:events:us-east-1:123456789012:event-bus/orders-bus"
        ```

### How should Amazon CloudWatch metrics be used to auto-scale SQS consumer worker fleets?

??? question "Reveal answer"
    **Short Answer:** SQS consumer fleets should not scale on CPU utilization because I/O-bound workers waiting on downstream APIs may have low CPU while queues build massive backlogs. Instead, scale on **backlog per worker**: divide `ApproximateNumberOfMessagesVisible` by the number of active consumer instances. If backlog per instance exceeds a target SLA threshold, trigger EC2 Auto Scaling or ECS Service Auto Scaling.

    ??? example "Example"
        ```yaml
        # CloudWatch Metric Math for Backlog Per Instance
        Metrics:
          - Id: backlogPerInstance
            Expression: "visibleMessages / activeTasks"
          - Id: visibleMessages
            MetricStat:
              Metric:
                Namespace: AWS/SQS
                MetricName: ApproximateNumberOfMessagesVisible
          - Id: activeTasks
            MetricStat:
              Metric:
                Namespace: ECS/ContainerInsights
                MetricName: RunningTaskCount
        ```
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### How would you design a mission-critical transactional outbox with DynamoDB and EventBridge without dual-write hazards?

??? question "Reveal answer"
    **Short Answer:** Write the business entity and an outbox event record atomically within a single DynamoDB `TransactWriteItems` call. Enable DynamoDB Streams with `NEW_IMAGE`. Deploy an AWS Lambda or AWS EventBridge Pipes consumer that reads the stream and publishes events to the EventBridge bus. If EventBridge is temporarily unavailable, DynamoDB Streams retains events for 24 hours with automatic retries, guaranteeing zero data loss and eliminating dual-write race conditions.

    ??? example "Example"
        ```mermaid
        sequenceDiagram
            participant App as Application Service
            participant DDB as DynamoDB (Entity + Outbox)
            participant Stream as DynamoDB Stream
            participant Pipe as EventBridge Pipes / Lambda
            participant Bus as EventBridge Bus

            App->>DDB: TransactWriteItems(Order + OutboxEvent)
            Note over DDB: Atomic ACID write
            DDB-->>App: OK
            DDB->>Stream: CDC Stream (New Outbox Record)
            Stream->>Pipe: Poll Stream Batch
            Pipe->>Bus: PutEvents(OrderPlaced)
            Bus-->>Pipe: 200 OK
        ```

### How do you scale SQS FIFO throughput beyond 300 messages per second up to 70,000 messages per second?

??? question "Reveal answer"
    **Short Answer:** Enable SQS High-Throughput FIFO mode by configuring `DeduplicationScope: messageGroup` and `FifoThroughputLimit: perMessageGroupId` on the queue. Distribute traffic across high-cardinality `MessageGroupId` keys (e.g. `userId` or `orderId` rather than a static group). Each distinct message group can then process up to 300 msg/sec (or 3,000 with batching), aggregating up to 70,000 transactions per second across the queue.

    ??? example "Example"
        ```yaml
        Properties:
          QueueName: high-throughput.fifo
          FifoQueue: true
          DeduplicationScope: messageGroup
          FifoThroughputLimit: perMessageGroupId
        ```

### How do you design an enterprise cross-account event mesh using Amazon EventBridge event buses?

??? question "Reveal answer"
    **Short Answer:** Create a central event bus in a dedicated Shared Services AWS account. Configure resource-based policies on the central bus granting `events:PutEvents` permission to spoke accounts. Spoke accounts publish events to the central bus. The central bus defines routing rules that target spoke event buses in consumer accounts. Cross-account IAM roles authorize target invocations, creating a decoupled, governed organizational event mesh.

    ??? example "Example"
        ```json
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Sid": "AllowSpokeAccountsToPublish",
              "Effect": "Allow",
              "Principal": {
                "AWS": [
                  "arn:aws:iam::111122223333:root",
                  "arn:aws:iam::444455556666:root"
                ]
              },
              "Action": "events:PutEvents",
              "Resource": "arn:aws:events:us-east-1:123456789012:event-bus/central-mesh"
            }
          ]
        }
        ```

### In an active-active multi-region architecture, how do you handle regional disaster recovery and cross-region message replication with SQS and SNS?

??? question "Reveal answer"
    **Short Answer:** SQS and SNS are regional services and do not support automatic cross-region replication natively. To achieve multi-region resiliency: use Amazon Route 53 latency-based or failover routing for API endpoints; deploy producers with dual-write or fallback client logic; or use EventBridge Global Endpoints with Route 53 health checks to automatically route events to the secondary region if primary event ingestion fails, paired with consumer fleets deployed active-active across both regions.

    ??? example "Example"
        ```yaml
        # EventBridge Global Endpoint with Route 53 Health Check
        GlobalEndpoint:
          Type: AWS::Events::Endpoint
          Properties:
            Name: global-order-endpoint
            RoutingConfig:
              FailoverConfig:
                Primary:
                  HealthCheck: !Ref PrimaryRegionHealthCheck
                Secondary:
                  Route: us-west-2
            EventBuses:
              - EventBusArn: !GetAtt UsEast1Bus.Arn
              - EventBusArn: !GetAtt UsWest2Bus.Arn
        ```

### How do you implement end-to-end distributed tracing across SNS, SQS, and EventBridge using AWS X-Ray and W3C Trace Context?

??? question "Reveal answer"
    **Short Answer:** Enable AWS X-Ray active tracing on Lambda and ECS containers. AWS SNS and EventBridge automatically propagate the `X-Amzn-Trace-Id` header across message boundaries. For SQS consumers written in Java with Spring Boot or AWS SDK v2, extract `AWSTraceHeader` from message system attributes and inject it into the OpenTelemetry or X-Ray `TraceContext` before invoking service methods, ensuring a continuous trace graph across asynchronous queues.

    ??? example "Example"
        ```java
        // Propagating AWS X-Ray Trace Context from SQS message
        String traceHeader = message.attributes().get(MessageSystemAttributeName.AWS_TRACE_HEADER);
        if (traceHeader != null) {
            AWSXRay.getGlobalRecorder().beginSubsegment("process-sqs-message");
            TraceHeader xRayTrace = TraceHeader.fromString(traceHeader);
            AWSXRay.getGlobalRecorder().getTraceEntity().setSampled(xRayTrace.getSampled());
        }
        ```
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenarios

### Incident: Downstream database latency spike causes SQS consumer workers to process messages repeatedly, triggering massive duplicate writes. Diagnose and resolve.

??? question "Reveal answer"
    **Short Answer:** Downstream database contention caused transaction processing time to jump from 5 seconds to 45 seconds. Because the queue's `VisibilityTimeout` was set to 30 seconds, SQS assumed in-flight workers had died and redelivered the same messages to available workers every 30 seconds. Multiple workers processed identical records concurrently.
    
    **Remediation:**
    1. Immediately increase SQS `VisibilityTimeout` to 300 seconds using `aws sqs set-queue-attributes`.
    2. Implement dynamic heartbeat visibility extensions via `changeMessageVisibility` in worker threads.
    3. Ensure downstream database writes are strictly idempotent using conditional `INSERT ... ON CONFLICT DO NOTHING` or unique transaction idempotency keys.

    ??? example "Example"
        ```bash
        # Emergency mitigation: Increase visibility timeout to 300s
        aws sqs set-queue-attributes \
          --queue-url "https://sqs.us-east-1.amazonaws.com/123456789012/orders-queue" \
          --attributes VisibilityTimeout=300
        ```

### Incident: An unprocessable malformed message in an SQS FIFO queue halts all subsequent message processing for a major customer account. Diagnose and resolve.

??? question "Reveal answer"
    **Short Answer:** A malformed payload arrived with `MessageGroupId = "EnterpriseCorp"`. The worker failed parsing and crashed without deleting the message. Because SQS FIFO guarantees strict ordering within a message group, SQS locks the message group head, blocking all subsequent healthy messages for EnterpriseCorp.
    
    **Remediation:**
    1. Configure a Dead Letter Queue with `maxReceiveCount = 3` so poison pills automatically move to the DLQ after 3 failures.
    2. Catch unrecoverable deserialization exceptions in the consumer and delete the message immediately after logging to DLQ or archive.
    3. Use SQS Redrive to replay fixed payloads after bug resolution.

    ??? example "Example"
        ```yaml
        # Attaching DLQ redrive policy to SQS FIFO queue
        RedrivePolicy:
          deadLetterTargetArn: !GetAtt EnterpriseDeadLetterQueue.Arn
          maxReceiveCount: 3 # Automatically unblocks MessageGroupId on 3rd failure
        ```
<!-- --8<-- [end:scenarios] -->

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Production](production.md)
