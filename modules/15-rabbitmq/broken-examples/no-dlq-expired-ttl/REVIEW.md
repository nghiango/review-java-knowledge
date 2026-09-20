# Code Review — No Dead Letter Queue for Expired and Rejected Messages

## Context

An incident response service consumes real-time operational alerts from RabbitMQ queue `ops.alerts.incoming`. The queue defines a 60-second message TTL so stale alerts do not backlog. If notification dispatch fails, the consumer rejects the message without requeueing.

Review `QueueConfig.java` and `ExpiringAlertConsumer.java` for message loss hazards, dead-letter routing, and observability gaps.

## What to look for

- What happens when an alert message sits in the queue longer than `x-message-ttl` (60 seconds)
- What happens when `channel.basicReject(deliveryTag, false)` is executed on a failed dispatch
- Queue definition arguments related to dead-letter exchanges (`x-dead-letter-exchange`)
- Ability of operations teams to inspect or recover failed and expired alerts
