# Code Review — Infinite Requeue Poison Pill Loop

## Context

A finance microservice consumes invoice generation requests from RabbitMQ queue `invoices.incoming`. It validates European VAT numbers against a validation service and writes ledger entries.

Review `InvoiceProcessingConsumer.java` for poison pill behavior, requeue semantics, CPU exhaustion, and queue starvation.

## What to look for

- The effect of `basicNack(deliveryTag, false, true)` with `requeue = true`
- What happens when a message contains deterministic validation errors (e.g. invalid VAT format)
- How quickly the broker redelivers a requeued message to available consumers
- Queue head blocking and CPU utilization patterns under failure
