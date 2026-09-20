# Code Review — Uncontrolled flatMap Concurrency

## Context

A notification aggregation service batches alerts for hundreds of thousands of users. An engineer authored `NotificationBatchSender.java` to blast notifications via `Flux.fromIterable(users).flatMap(user -> sendNotification(user))`.

Review `NotificationBatchSender.java` for unbounded concurrency, downstream service saturation, memory pressure, and backpressure collapse.

## What to look for

- Default concurrency parameter in `flatMap`
- Outbound socket/connection pool exhaustion
- Downstream rate limit triggering (HTTP 429)
- Memory accumulation under large stream inputs
