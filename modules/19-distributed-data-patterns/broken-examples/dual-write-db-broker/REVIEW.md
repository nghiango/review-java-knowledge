# Code Review — Dual Write to Database and Message Broker

## Context

An e-commerce order service creates customer orders in an RDBMS database and broadcasts an `OrderCreated` event to Kafka so inventory, payment, and notification microservices can begin processing.

Review `OrderCreationService.java` for data consistency, partial failure vulnerabilities, transaction boundaries, and event loss risks.

## What to look for

- Consistency between database commit and message broker publication
- Failure scenarios when Kafka broker rejects writes or times out
- Failure scenarios when database commit fails after event publication
- Network I/O inside database transaction boundaries
