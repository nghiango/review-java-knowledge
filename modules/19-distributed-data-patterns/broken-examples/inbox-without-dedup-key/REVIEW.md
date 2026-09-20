# Code Review — Inbox Without Deduplication Key

## Context

An inventory microservice consumes `OrderPlacedEvent` messages from Kafka to allocate physical warehouse items for order fulfillment.

Review `InventoryAllocationConsumer.java` for consumer idempotency, at-least-once message delivery, and phantom inventory loss.

## What to look for

- Deduplication of incoming message IDs (`eventId` / `orderId`)
- Transactional state updates when Kafka redelivers messages
- Atomicity between inbox deduplication and business mutations
