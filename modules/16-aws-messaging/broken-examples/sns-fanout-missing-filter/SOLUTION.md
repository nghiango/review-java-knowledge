# Solution — SNS Topic Fanout Missing Subscription Filter Policies

## Annotated code

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: E-Commerce Order Lifecycle SNS to SQS Fanout Architecture

Resources:
  OrderEventsTopic:
    Type: AWS::SNS::Topic
    Properties:
      TopicName: order-events-topic

  FraudDetectionQueue:
    Type: AWS::SQS::Queue
    Properties:
      QueueName: fraud-detection-queue

  WarehouseShippingQueue:
    Type: AWS::SQS::Queue
    Properties:
      QueueName: warehouse-shipping-queue

  CustomerNotificationQueue:
    Type: AWS::SQS::Queue
    Properties:
      QueueName: customer-notification-queue

  FraudDetectionSubscription:
    Type: AWS::SNS::Subscription
    Properties:
      TopicArn: !Ref OrderEventsTopic
      Endpoint: !GetAtt FraudDetectionQueue.Arn
      Protocol: sqs
      RawMessageDelivery: true
      # Scalability issue: Missing SNS FilterPolicy forces ALL topic messages into the Fraud queue.
      # By default, SNS fanout delivers 100% of messages published to the topic to every subscription.
      # The fraud microservice only needs PaymentCompleted events, but receives OrderCreated,
      # AddressUpdated, ShipmentDispatched, and ItemReviewed, increasing queue volume by 10x-50x.
      # Performance issue: Consumer workers spend massive CPU and network bandwidth polling,
      # deserializing, inspecting, and discarding irrelevant messages, driving up AWS SQS API costs.

  WarehouseShippingSubscription:
    Type: AWS::SNS::Subscription
    Properties:
      TopicArn: !Ref OrderEventsTopic
      Endpoint: !GetAtt WarehouseShippingQueue.Arn
      Protocol: sqs
      RawMessageDelivery: true
      # Scalability issue: Warehouse shipping queue receives unneeded pre-payment and cancellation events.

  CustomerNotificationSubscription:
    Type: AWS::SNS::Subscription
    Properties:
      TopicArn: !Ref OrderEventsTopic
      Endpoint: !GetAtt CustomerNotificationQueue.Arn
      Protocol: sqs
      RawMessageDelivery: true
      # Scalability issue: Customer notification queue receives internal audit and warehouse telemetry events.
```

## Issue list

### Scalability issue: Unfiltered SNS fanout floods subscriber queues with irrelevant events

- **Location:** `sns-sqs-fanout-config.yaml:26`
- **Description:** Subscriptions omit `FilterPolicy` attributes, causing SNS to replicate 100% of topic events to every SQS subscriber queue.
- **Impact:** Downstream queues suffer from severe traffic amplification. For example, a system publishing 10,000 events/sec across 10 domain types delivers 10,000 msg/sec to queues that only require 500 msg/sec. Subscriber queues experience backlog buildup, delayed SLAs for critical events, and excessive SQS request billing costs.
- **Remediation:** Declare `FilterPolicy` on each `AWS::SNS::Subscription` filtering on message attributes (e.g. `eventType: ["PaymentCompleted"]`).

### Performance issue: Application workers waste compute cycles discarding noisy messages

- **Location:** `sns-sqs-fanout-config.yaml:26`
- **Description:** Workers poll messages from SQS, deserialize JSON payloads, evaluate if the message is relevant, and delete it without performing business logic.
- **Impact:** Up to 90% of worker CPU, thread pool capacity, and network bandwidth are consumed by discard operations rather than productive computation.
- **Remediation:** Offload event filtering entirely to the AWS SNS managed control plane using `FilterPolicy` and `FilterPolicyScope: MessageAttributes`.

## Correct implementation

See [`correct/sns-sqs-fanout-config.yaml`](correct/sns-sqs-fanout-config.yaml).

Detailed discussion in [Solutions](../../../docs/topics/aws-messaging/solutions.md#sns-subscription-filter-policies-and-fanout-isolation).
