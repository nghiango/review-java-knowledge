# Code Review — Acknowledging Offset Before Processing Completion

## Context

A payment service listens to Kafka topic `payment-notifications`. Upon receiving a notification, it confirms settlement with a third-party payment gateway over HTTP and persists the final settled status in a database.

Review `PaymentNotificationConsumer.java` for message loss hazards, offset delivery semantics, error handling, and reliability flaws.

## What to look for

- When the Kafka offset acknowledgment occurs relative to business operations
- Behavior when the external gateway call or database persistence throws an exception
- Consequences of a process crash or restart mid-flight
- Delivery semantics (at-most-once vs at-least-once)
