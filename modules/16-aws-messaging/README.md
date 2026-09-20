# Module 16 — AWS Messaging

This is a **doc module** covering AWS messaging architectures:
Amazon Simple Queue Service (SQS Standard and FIFO), Amazon Simple Notification Service (SNS pub/sub and fanout), and Amazon EventBridge (event bus, rules, pattern filtering, and scheduling).

The canonical prose, comparison matrix, interview Q&A, and architectural guides live in the documentation:

👉 **[AWS Messaging Documentation](../../docs/topics/aws-messaging/index.md)**

## Broken Review Examples

This module provides 4 infrastructure-as-code review targets under `broken-examples/`:

1. `visibility-timeout-too-short/` — SQS queue visibility timeout shorter than downstream processing duration causing duplicate message execution.
2. `fifo-missing-deduplication/` — SQS FIFO queue configuration missing deduplication ID / content-based deduplication leading to ordering drift and duplicate delivery.
3. `no-dlq-redrive-policy/` — SQS queue without a Dead Letter Queue (DLQ) redrive policy, causing poison pills to block queue workers indefinitely.
4. `sns-fanout-missing-filter/` — SNS topic fanout to multiple SQS queues without subscription filter policies, causing subscriber noise and unnecessary billing.
