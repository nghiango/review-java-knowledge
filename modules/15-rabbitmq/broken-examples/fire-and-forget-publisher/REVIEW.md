# Code Review — Fire-and-Forget Publishing

## Context

A compliance microservice publishes mandatory security audit trail events (e.g. login attempts, privilege escalation, data export) to RabbitMQ exchange `audit.events.exchange`.

Review `AuditLogPublisher.java` for message delivery guarantees, broker acknowledgment, unroutable message handling, and silent message loss.

## What to look for

- How `RabbitTemplate.convertAndSend()` behaves under network timeouts or broker failure
- What happens if the exchange name is misspelled or the routing key matches zero queues
- Configuration of publisher confirms (`confirm-type`) and return callbacks (`mandatory`)
- Verification that published compliance records actually reached durable storage on the broker
