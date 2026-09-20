# Code Review — SNS Topic Fanout Missing Subscription Filter Policies

## Context

An e-commerce order lifecycle service publishes order events (e.g. `OrderCreated`, `PaymentCompleted`, `OrderCancelled`, `ShipmentDispatched`) to a central Amazon SNS topic. Multiple downstream service queues subscribe to the SNS topic via SQS subscriptions:
- `FraudDetectionQueue`: Only cares about `PaymentCompleted` events with transactions $> \$500$ or high-risk countries.
- `WarehouseShippingQueue`: Only cares about `PaymentCompleted` and `ShipmentDispatched` events.
- `CustomerNotificationQueue`: Cares about `OrderCreated` and `ShipmentDispatched` events.

Review `sns-sqs-fanout-config.yaml` for fanout traffic amplification, noisy subscriber queues, unnecessary AWS compute/storage billing, and lack of content-based filtering.

## What to look for

- Subscription filter policies (`FilterPolicy` and `FilterPolicyScope`)
- Message attribute vs message body filtering
- Cost and capacity impact on downstream subscriber queues
- Dead-letter configuration for failed SNS delivery to SQS
