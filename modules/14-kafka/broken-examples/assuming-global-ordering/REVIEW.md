# Code Review — Assuming Global Ordering Across Partitions

## Context

An identity and access management service publishes user lifecycle events (`USER_CREATED`, `USER_UPDATED`, `USER_DELETED`) to a multi-partition Kafka topic `user-lifecycle-events`. A downstream read-model consumer updates the localized user state view.

Review `UserEventProducer.java` and `UserActivityConsumer.java` for partitioning flaws, causality violations, concurrency issues, and ordering guarantees.

## What to look for

- How events are assigned to partitions at produce time
- The scope of Kafka ordering guarantees (partition vs topic)
- What happens when multiple consumer threads process different partitions concurrently
- The sequence of lifecycle events for an individual user
