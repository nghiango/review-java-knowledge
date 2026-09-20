# Solution — SQS Queue Without Dead Letter Queue (DLQ) Redrive Policy

## Annotated code

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: Carrier Webhook Tracking Ingestion Queue

Resources:
  ShipmentTrackingQueue:
    Type: AWS::SQS::Queue
    Properties:
      QueueName: shipment-tracking-queue
      VisibilityTimeout: 60
      ReceiveMessageWaitTimeSeconds: 20
      # Reliability issue: Missing RedrivePolicy and Dead Letter Queue (DLQ).
      # If a carrier sends a malformed JSON body or unsupported schema (a "poison pill"),
      # the consumer fails parsing and does not call DeleteMessage. When the VisibilityTimeout
      # expires, SQS makes the message visible again. The consumer receives the failing message
      # repeatedly for the entire 24-hour retention period (up to 1,440 times per poison pill),
      # consuming AWS SQS API request charges, saturating worker CPU/network, and polluting logs.
      MessageRetentionPeriod: 86400
      # Observability issue: No RedriveAllowPolicy or DLQ CloudWatch alarm.
      # Without a DLQ, operators have zero visibility into unprocessable messages until end users
      # complain of missing shipments or data loss occurs when MessageRetentionPeriod expires.
      KmsMasterKeyId: alias/aws/sqs
```

## Issue list

### Reliability issue: Missing RedrivePolicy causes infinite poison pill retries

- **Location:** `sqs-dlq-config.yaml:7`
- **Description:** `RedrivePolicy` with a target DLQ is completely absent from the queue definition.
- **Impact:** Poison pills (messages causing deterministic runtime exceptions) cycle through consumer receive-fail-reappear loops continuously until `MessageRetentionPeriod` (86400s) elapses. With a 60s visibility timeout, a single corrupt message is reprocessed up to 1,440 times, saturating ECS worker capacity, inflating SQS API billing costs, and starving legitimate shipment messages. Once 24 hours pass, SQS deletes the message silently, causing irrecoverable data loss.
- **Remediation:** Declare a dedicated DLQ (`shipment-tracking-dlq`) and configure a `RedrivePolicy` with `maxReceiveCount: 5`.

### Observability issue: Absence of DLQ alarms prevents incident detection

- **Location:** `sqs-dlq-config.yaml:12`
- **Description:** No CloudWatch alarm monitors unprocessable or failed messages.
- **Impact:** Silent message degradation goes undetected by the engineering team until customer escalations occur.
- **Remediation:** Attach a CloudWatch Metric Alarm to the DLQ on metric `ApproximateNumberOfMessagesVisible > 0` to alert engineering immediately via SNS/PagerDuty when a poison message is sidelined.

## Correct implementation

See [`correct/sqs-dlq-config.yaml`](correct/sqs-dlq-config.yaml).

Detailed discussion in [Solutions](../../../docs/topics/aws-messaging/solutions.md#dead-letter-queue-redrive-and-poison-message-isolation).
