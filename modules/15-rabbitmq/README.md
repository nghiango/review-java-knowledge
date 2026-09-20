# Module 15 — RabbitMQ

This module contains practical implementations, unit tests, and broken code review exercises for
RabbitMQ and Spring AMQP: exchanges, queues, bindings, acknowledgment modes, publisher confirms,
Dead Letter Exchanges (DLX), prefetch QoS, fair dispatching, consumer idempotency, and Quorum queues.

Full theory, concepts, internals diagrams, interview questions, and deep walkthroughs live in the documentation:

👉 **[RabbitMQ Documentation](../../docs/topics/rabbitmq/index.md)**

## Structure

- `broken-examples/`: Intentionally flawed RabbitMQ implementations for code review practice
  - `auto-ack-before-processing/`: Premature auto-ack losing messages on consumer crash
  - `infinite-requeue-loop/`: Continuous nacking with `requeue = true` spinning at 100% CPU on poison pills
  - `no-dlq-expired-ttl/`: Messages expiring or rejected without DLX, causing silent message loss
  - `unbounded-prefetch-oom/`: Unbounded prefetch buffer exhausting JVM heap on slow consumer workloads
  - `non-idempotent-consumer/`: Redelivered messages causing duplicate business mutations
  - `fire-and-forget-publisher/`: Publishing without publisher confirms or mandatory return callbacks
- `src/main/java/lab/rabbitmq/`: Production-grade correct implementations
- `src/test/java/lab/rabbitmq/`: Fast unit tests
- `src/integrationTest/java/lab/rabbitmq/`: Real RabbitMQ Testcontainers integration tests
- `src/examples/java/lab/rabbitmq/questions/`: Standalone compilable question verification classes
