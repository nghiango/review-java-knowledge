# Code Review — SQS Queue Without Dead Letter Queue (DLQ) Redrive Policy

## Context

A logistics shipment tracking microservice receives external carrier webhook status events into an SQS queue. A fleet of ECS tasks pulls messages from the queue, parses carrier JSON payloads, and updates shipment states in an Amazon Aurora PostgreSQL database.

Review `sqs-dlq-config.yaml` for error recovery mechanisms, poison message resilience, and queue degradation risks.

## What to look for

- Dead Letter Queue (DLQ) configuration and redrive policies
- Behavior when encountering malformed or corrupted webhook payloads
- Message retention period versus downstream service outage windows
- Worker CPU and network consumption under unrecoverable error conditions
