# Code Review — Auto-Ack Before Processing Completion

## Context

An e-commerce order fulfillment service listens to RabbitMQ queue `order.confirmations`. When an order notification arrives, it validates authorization with an external payment gateway and records the confirmed state in a database.

Review `OrderConfirmationConsumer.java` for message loss risks, AMQP acknowledgment timing, and error handling vulnerabilities.

## What to look for

- Timing of `channel.basicAck()` relative to downstream network and database operations
- Worker crash and restart behavior while processing is in flight
- Delivery guarantees (at-most-once vs at-least-once)
- Exception handling and poison message mitigation
