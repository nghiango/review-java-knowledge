# Code Review — Non-Idempotent Saga Compensation

## Context

In a distributed Saga orchestrating an order checkout, if inventory reservation fails downstream, the orchestrator triggers a compensating transaction to refund the customer's account balance.

Review `PaymentCompensationService.java` for idempotency, retry safety, duplicate message delivery, and race conditions.

## What to look for

- Idempotency guarantees when compensation events are redelivered by message brokers
- State verification: checking whether the refund was already applied for this `orderId`
- Concurrency and lost update issues on account balance modification
