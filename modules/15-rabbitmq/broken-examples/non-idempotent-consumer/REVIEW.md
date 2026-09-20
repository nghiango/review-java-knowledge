# Code Review — Non-Idempotent Consumer Processing

## Context

A customer loyalty reward microservice consumes point grant commands from RabbitMQ queue `loyalty.points.award`. For each message received, it awards points to the user's account and saves an audit log entry.

Review `LoyaltyPointConsumer.java` for duplicate redelivery issues, at-least-once delivery implications, and data consistency risks.

## What to look for

- Consequences of RabbitMQ redelivering an unacknowledged message (`redelivered == true`)
- Impact of worker crashes or network interruptions before `channel.basicAck()` reaches the broker
- Deduplication mechanisms and idempotency keys
- Atomicity between the business mutation and deduplication state
