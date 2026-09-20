# AWS Messaging Code Review

Review each clean infrastructure-as-code template before expanding its answer.

## Visibility timeout shorter than processing duration

A video rendering pipeline offloads transcode requests to an AWS Lambda function triggered by an SQS queue. The Lambda function takes 45 to 60 seconds to process heavy video frames.

```yaml
--8<-- "modules/16-aws-messaging/broken-examples/visibility-timeout-too-short/sqs-consumer-config.yaml"
```

Consider SQS visibility timeout mechanics, Lambda execution timeout limits, message batch sizes, and duplicate processing risks.

??? warning "Reveal issues"
    **Reliability issue — VisibilityTimeout shorter than Lambda execution timeout:** `VisibilityTimeout` is set to 30 seconds while the processing Lambda `Timeout` is set to 90 seconds. When video rendering takes longer than 30 seconds, SQS makes the in-flight message visible again. Another Lambda worker polls and starts rendering the same video concurrently, causing duplicate processing, wasted compute, and race conditions. AWS best practice mandates setting `VisibilityTimeout >= 6 * Lambda Timeout` (e.g. 540 seconds).

    **Scalability issue — large batch size for heavy compute jobs:** `BatchSize: 10` combined with 45–60s tasks guarantees function timeouts when multiple items arrive together. For heavy compute jobs, configure `BatchSize: 1` or enable `ReportBatchItemFailures`.

[Correct implementation](solutions.md#visibility-timeout-sizing-and-batch-tuning)

---

## FIFO queue missing deduplication and partitioning strategy

A core banking payment gateway must process ledger balance adjustments in strict chronological sequence per customer account.

```yaml
--8<-- "modules/16-aws-messaging/broken-examples/fifo-missing-deduplication/fifo-queue-config.yaml"
```

Consider FIFO queue naming rules, message deduplication configurations, and high-throughput scaling modes.

??? warning "Reveal issues"
    **Configuration issue — FIFO queue name missing mandatory .fifo suffix:** SQS FIFO queue names must end with `.fifo`. Declaring `FifoQueue: true` with `QueueName: payment-ledger-queue` causes CloudFormation stack creation failure.

    **Reliability issue — missing ContentBasedDeduplication or explicit deduplication strategy:** Omitting `ContentBasedDeduplication` (default false) causes SQS to reject producer calls that omit `MessageDeduplicationId` with an API error. Furthermore, without deduplication, transient network retries risk duplicate balance adjustments.

    **Scalability issue — standard FIFO throughput bottleneck:** Omitting `DeduplicationScope: messageGroup` and `FifoThroughputLimit: perMessageGroupId` bottlenecks queue throughput to 300 transactions/second across all customers.

[Correct implementation](solutions.md#sqs-fifo-naming-deduplication-and-high-throughput-tuning)

---

## SQS queue without dead letter queue redrive policy

A logistics shipment tracking microservice receives external carrier webhook status events into an SQS queue. A fleet of ECS tasks pulls messages from the queue, parses carrier JSON payloads, and updates shipment states in an Aurora database.

```yaml
--8<-- "modules/16-aws-messaging/broken-examples/no-dlq-redrive-policy/sqs-dlq-config.yaml"
```

Consider error recovery mechanics, poison message handling, queue worker resource consumption, and incident observability.

??? warning "Reveal issues"
    **Reliability issue — missing RedrivePolicy causes infinite poison pill retries:** Without a DLQ and `RedrivePolicy`, a malformed or corrupted carrier webhook payload will be received, fail processing, and reappear in the queue every 60 seconds for the entire 24-hour retention period (up to 1,440 times per poison pill). This saturates ECS worker capacity, inflates SQS API billing costs, and floods logging pipelines until SQS silently purges the message after 24 hours.

    **Observability issue — absence of DLQ alarms prevents incident detection:** Without a dedicated DLQ and CloudWatch metric alarm on `ApproximateNumberOfMessagesVisible`, silent message loss occurs without triggering engineering alerts.

[Correct implementation](solutions.md#dead-letter-queue-redrive-and-poison-message-isolation)

---

## SNS topic fanout missing subscription filter policies

An e-commerce order lifecycle service publishes order events (`OrderCreated`, `PaymentCompleted`, `OrderCancelled`, `ShipmentDispatched`) to a central Amazon SNS topic that fans out to multiple downstream SQS queues.

```yaml
--8<-- "modules/16-aws-messaging/broken-examples/sns-fanout-missing-filter/sns-sqs-fanout-config.yaml"
```

Consider fanout traffic amplification, noisy subscriber queues, unnecessary AWS compute/storage billing, and lack of content-based filtering.

??? warning "Reveal issues"
    **Scalability issue — unfiltered SNS fanout floods subscriber queues with irrelevant events:** Without `FilterPolicy`, SNS replicates 100% of topic events to every SQS subscriber queue. Queues that only require a small subset of events (e.g. fraud detection only needing high-value `PaymentCompleted` events) receive massive volumes of irrelevant traffic, driving up queue storage and API costs.

    **Performance issue — application workers waste compute cycles discarding noisy messages:** Consumer workers must poll, deserialize, inspect, and discard thousands of unneeded messages per minute, exhausting worker CPU and network bandwidth.

[Correct implementation](solutions.md#sns-subscription-filter-policies-and-fanout-isolation)

---

## Related

- [Solutions](solutions.md)
- [Concepts](concepts.md)
- [Internals](internals.md)
- [Production](production.md)
- [Interview Questions](questions.md)
