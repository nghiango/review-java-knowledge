# Code Review — Infinite Retries Without Dead Letter Topic

## Context

A warehouse fulfillment listener processes physical order dispatch requests from the `order-fulfillments` Kafka topic. The container factory configures a custom error handler to handle processing errors.

Review `OrderFulfillmentConsumer.java` for poison pill hazards, error classification, retry exhaustion behavior, and partition starvation.

## What to look for

- How non-transient / deterministic errors (such as payload validation failures) are handled
- The retry policy configured on the error handler
- Absence of Dead Letter Topic (DLT) or recovery mechanism
- Impact of a single invalid message on subsequent messages in the partition
