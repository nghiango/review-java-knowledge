# Code Review — Blocking Listener Thread Exceeding Max Poll Interval

## Context

A business analytics service listens to Kafka topic `report-generation-tasks`. It renders large multi-page PDF/Excel reports by querying extensive historical data, and subsequently uploads the rendered report to cloud object storage.

Review `ReportGenerationListener.java` for consumer group stability, heartbeat mechanics, poll timeout issues, and rebalance loop hazards.

## What to look for

- The relationship between task execution duration and Kafka consumer configuration (`max.poll.interval.ms`)
- What occurs inside the Kafka consumer coordinator when a listener method blocks for longer than `max.poll.interval.ms`
- What happens when the worker eventually finishes and invokes `ack.acknowledge()`
- Architectural approaches to handling long-running background workloads in message-driven systems
