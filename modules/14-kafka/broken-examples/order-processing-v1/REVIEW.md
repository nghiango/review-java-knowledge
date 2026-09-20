# Code Review — Order Processing Service v1

## Context

This pull request implements order creation and asynchronous fulfillment for an e-commerce platform. When a customer places an order, the system records the order in a database, broadcasts an `OrderPlacedEvent` to Kafka, and an asynchronous worker listener reserves warehouse inventory and marks the order fulfilled.

Review `InsecureOrderProcessor.java` across both publishing and consuming workflows for transaction consistency, partition ordering, idempotency, and error handling vulnerabilities.

## What to look for

- Interplay between Spring `@Transactional` database transactions and Kafka message publishing
- Partition routing and message key usage
- Idempotency guarantees when messages are redelivered by Kafka
- Exception handling, retry semantics, and Dead Letter Topic (DLT) strategy
