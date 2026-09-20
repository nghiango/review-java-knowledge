# Solution — SQS FIFO Queue Missing Deduplication and Partitioning Strategy

## Annotated code

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: Banking Payment Ledger FIFO Queue Configuration

Resources:
  PaymentLedgerQueue:
    Type: AWS::SQS::Queue
    Properties:
      # Configuration issue: SQS FIFO queue names MUST end with the literal suffix '.fifo'.
      # Declaring 'FifoQueue: true' without the '.fifo' suffix triggers an immediate CloudFormation
      # validation exception: "The name of a FIFO queue can only include alphanumeric characters,
      # hyphens, or underscores, and must end with .fifo".
      QueueName: payment-ledger-queue
      FifoQueue: true
      # Reliability issue: Missing ContentBasedDeduplication or explicit deduplication strategy.
      # When ContentBasedDeduplication is omitted (default false), every producer SendMessage call
      # MUST supply a MessageDeduplicationId. If producers omit this parameter, SQS rejects the message
      # with InvalidParameterValueException. Furthermore, if ContentBasedDeduplication is enabled blindly
      # on transactions with identical amounts within 5 minutes, legitimate subsequent payments are discarded.
      VisibilityTimeout: 120
      ReceiveMessageWaitTimeSeconds: 20
      MessageRetentionPeriod: 345600
      # Scalability issue: Missing high-throughput FIFO configuration.
      # By default, SQS FIFO queues limit throughput to 300 transactions/sec (or 3,000 with batching)
      # across the entire queue. High-throughput FIFO requires DeduplicationScope: messageGroup and
      # FifoThroughputLimit: perMessageGroupId to scale up to 70,000 transactions/sec.
```

## Issue list

### Configuration issue: FIFO queue name missing mandatory `.fifo` suffix

- **Location:** `fifo-queue-config.yaml:7`
- **Description:** `QueueName: payment-ledger-queue` lacks the required `.fifo` suffix.
- **Impact:** CloudFormation stack deployment fails immediately during resource provisioning with `InvalidParameterValue: The name of a FIFO queue must end with the .fifo suffix`.
- **Remediation:** Update `QueueName` to `payment-ledger-queue.fifo`.

### Reliability issue: Missing content-based deduplication or explicit deduplication strategy

- **Location:** `fifo-queue-config.yaml:8`
- **Description:** `ContentBasedDeduplication` is omitted and defaults to `false`.
- **Impact:** Producers calling `SendMessage` without `MessageDeduplicationId` fail with runtime API exceptions. If publishers provide arbitrary random deduplication IDs, duplicate network retries from producers will result in duplicated ledger balance mutations.
- **Remediation:** Configure `ContentBasedDeduplication: true` for automatic SHA-256 body hashing, or mandate business transaction IDs as explicit `MessageDeduplicationId` parameters on the producer client.

### Scalability issue: Standard FIFO throughput bottleneck across message groups

- **Location:** `fifo-queue-config.yaml:8`
- **Description:** Omission of `DeduplicationScope` and `FifoThroughputLimit` locks the FIFO queue to legacy limits (300 msg/sec without batching).
- **Impact:** High-volume banking ledger transactions experience severe throttling (`QueueThrottle` / `OverLimitException`).
- **Remediation:** Configure `DeduplicationScope: messageGroup` and `FifoThroughputLimit: perMessageGroupId` to unlock up to 70,000 transactions per second distributed across independent customer account message groups.

## Correct implementation

See [`correct/fifo-queue-config.yaml`](correct/fifo-queue-config.yaml).

Detailed discussion in [Solutions](../../../docs/topics/aws-messaging/solutions.md#sqs-fifo-naming-deduplication-and-high-throughput-tuning).
