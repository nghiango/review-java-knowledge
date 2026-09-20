# AWS Messaging Solutions

Production-grade CloudFormation templates and architectural solutions corresponding to the code review exercises.

## Visibility timeout sizing and batch tuning

### Implementation

```yaml
--8<-- "modules/16-aws-messaging/broken-examples/visibility-timeout-too-short/correct/sqs-consumer-config.yaml"
```

### Why it works

1. **Lease Protection Rule of Thumb**: Setting SQS `VisibilityTimeout: 540` ($6 \times$ the 90-second Lambda timeout) ensures the message lease remains invisible during execution, preventing duplicate invocations.
2. **Constrained Batch Size**: For heavy computing tasks (45–60s per item), setting `BatchSize: 1` guarantees that each Lambda invocation handles exactly one job within its 90-second execution envelope.
3. **Dead-Letter Backup**: Adds a dedicated DLQ with `maxReceiveCount: 3` so unexpected crashes or unhandled runtime exceptions are moved to dead-letter storage after 3 attempts.

### Trade-offs

A longer `VisibilityTimeout` means that if a worker crashes abruptly without a chance to catch the failure, the message remains invisible for the full duration of the timeout before returning to the queue. For long-running tasks, combining a moderate timeout with dynamic heartbeat extensions (`ChangeMessageVisibility`) provides the optimal balance.

---

## SQS FIFO naming deduplication and high-throughput tuning

### Implementation

```yaml
--8<-- "modules/16-aws-messaging/broken-examples/fifo-missing-deduplication/correct/fifo-queue-config.yaml"
```

### Why it works

1. **Mandatory .fifo Suffix**: Adding `.fifo` to the queue and DLQ names satisfies AWS SQS API validation constraints.
2. **Content-Based Deduplication**: Enabling `ContentBasedDeduplication: true` generates SHA-256 hashes of message payloads across a 5-minute sliding window, eliminating duplicate enqueuing during producer retry storms.
3. **High-Throughput Partitioning**: `DeduplicationScope: messageGroup` and `FifoThroughputLimit: perMessageGroupId` enable SQS to scale up to 70,000 transactions/second partitioned by `MessageGroupId`.

### Trade-offs

Content-based deduplication hashes the entire message body. If two legitimate distinct business transactions have identical JSON payloads within 5 minutes, the second transaction is dropped as a duplicate. In high-frequency trading or recurring ledger environments, always supply an explicit unique business UUID via `MessageDeduplicationId`.

---

## Dead letter queue redrive and poison message isolation

### Implementation

```yaml
--8<-- "modules/16-aws-messaging/broken-examples/no-dlq-redrive-policy/correct/sqs-dlq-config.yaml"
```

### Why it works

1. **Poison Pill Containment**: `RedrivePolicy` with `maxReceiveCount: 5` automatically moves failing messages to `shipment-tracking-dlq` after 5 failed receive attempts, breaking the infinite retry cycle.
2. **Extended DLQ Retention**: DLQ retention is set to 14 days (`1209600` seconds), giving operations teams two full weeks to triage, fix bugs, and redrive messages.
3. **Automated Alarm Trigger**: CloudWatch alarm triggers on `ApproximateNumberOfMessagesVisible > 0`, notifying on-call engineers via SNS/PagerDuty immediately when a message lands in the DLQ.

### Trade-offs

Requires maintaining two queues per workload (main queue and DLQ) and monitoring DLQ backlog. Messages in the DLQ must eventually be triaged, redriven back to the source queue, or purged.

---

## SNS subscription filter policies and fanout isolation

### Implementation

```yaml
--8<-- "modules/16-aws-messaging/broken-examples/sns-fanout-missing-filter/correct/sns-sqs-fanout-config.yaml"
```

### Why it works

1. **Edge-Level Filtering**: `FilterPolicy` with `FilterPolicyScope: MessageAttributes` evaluates incoming event attributes directly at the SNS edge before message replication.
2. **Zero Waste Downstream**: Downstream queues only receive events relevant to their specific domain. For example, the `FraudDetectionQueue` only receives `PaymentCompleted` events with `amount >= 500`.
3. **Dramatic Cost Reduction**: Slashes SQS API request charges (`ReceiveMessage`, `DeleteMessage`) and worker CPU cycles spent polling and discarding irrelevant events.

### Trade-offs

Publishers must populate message attributes consistently with accurate naming and typing. If an event publisher forgets to include a filtered attribute, the message will not match subscription criteria and will not be delivered to that subscriber queue.

---

## Related

- [Code Review](code-review.md)
- [Concepts](concepts.md)
- [Internals](internals.md)
- [Production](production.md)
- [Interview Questions](questions.md)
