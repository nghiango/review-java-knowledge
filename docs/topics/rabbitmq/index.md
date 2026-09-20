# RabbitMQ

RabbitMQ is a widely deployed, open-source message broker implementing the Advanced Message Queuing
Protocol (AMQP 0-9-1) along with modern protocols like STOMP, MQTT, and AMQP 1.0. It provides flexible
routing, strong per-message delivery guarantees, and high-concurrency competing consumer patterns.

## Architectural Overview

RabbitMQ decouples publishers from consumers using an intermediary routing model composed of
**Exchanges**, **Bindings**, and **Queues**:

```mermaid
flowchart LR
    Publisher["Publisher<br/>(RabbitTemplate)"]
    subgraph Broker["RabbitMQ Broker / Virtual Host"]
        Exchange{"Exchange<br/>(Direct, Topic, Fanout)"}
        Queue1[("Queue: orders.processing<br/>Prefetch: 10")]
        Queue2[("Queue: orders.audit<br/>Prefetch: 50")]
        DLX{"Dead Letter Exchange<br/>(DLX)"}
        DLQ[("Dead Letter Queue<br/>(DLQ)")]
    end
    Consumer1["Worker Instance 1"]
    Consumer2["Worker Instance 2"]

    Publisher -->|publish(exchange, routingKey)| Exchange
    Exchange -->|Binding Key 1| Queue1
    Exchange -->|Binding Key 2| Queue2
    Queue1 -->|basic.deliver| Consumer1
    Queue2 -->|basic.deliver| Consumer2
    Queue1 -.->|Nack requeue=false / TTL expiry| DLX
    DLX --> DLQ
```

## Key Invariants

1. **Explicit Exchange Routing**: Producers never publish directly to queues; they publish to exchanges with a routing key. Bindings connect exchanges to queues according to routing patterns.
2. **Post-Processing Manual Acknowledgment**: Consumer acknowledgments (`channel.basicAck()`) must occur strictly **after** business logic and persistent state changes succeed. Acknowledging before processing completes risks message loss if the consumer crashes mid-flight.
3. **Poison Pill Non-Requeueing**: Fatal validation errors or deterministic deserialization failures must never be rejected with `requeue = true`. Requeueing poison pills causes an immediate 100% CPU infinite redelivery spin loop. Reject with `requeue = false` to route to a Dead Letter Exchange.
4. **Guaranteed Dead-Letter Retention**: Queues with TTL (`x-message-ttl`) or rejecting consumers must always be configured with `x-dead-letter-exchange` and `x-dead-letter-routing-key` to prevent expired or rejected messages from being dropped without audit records.
5. **Bounded Prefetch QoS**: Listeners must configure a bounded `prefetchCount` (e.g. 1–10 for heavy tasks). Leaving prefetch unbounded (`0`) flushes the entire queue backlog into consumer heap memory, causing `OutOfMemoryError` and starving competing workers.
6. **Publisher Confirms for Durability**: Critical messages must use Publisher Confirms (`confirm-type: correlated`) and mandatory flags (`mandatory = true`) to detect unroutable routing keys and verify broker disk fsync before acknowledging to upstream clients.

## Module Topics

| Section | Focus |
|---|---|
| [Concepts](concepts.md) | Exchanges, queues, bindings, acks/nacks, DLX/DLQ, prefetch, publisher confirms, Quorum queues |
| [Internals](internals.md) | Erlang actor model, Mnesia, Raft quorum consensus, AMQP protocol frames, flow control alarms |
| [Interview Questions](questions.md) | 23 questions spanning basic, intermediate, senior, and production incident scenarios |
| [Code Review](code-review.md) | 6 realistic broken examples to practice spotting RabbitMQ reliability and routing bugs in PRs |
| [Solutions](solutions.md) | Production-grade implementations with design rationale and architectural trade-offs |
| [Tests](tests.md) | Testcontainers RabbitMQ integration testing, publisher confirm assertions, and DLX routing suites |
| [Production](production.md) | Memory and disk alarm thresholds, Quorum queue sizing, prefetch guidelines, and production checklist |
| [Exercises](exercises.md) | Hands-on exercises: Custom Dead Letter Retry Strategy with Exponential Delays and Dynamic Topic Exchange Router |

## Related Modules

- [Kafka](../kafka/index.md) — Comparing append-only commit logs with RabbitMQ per-message queuing
- [Spring Transactions](../spring-transactions/index.md) — Coordinating database transaction commits with AMQP publisher confirms
- [Testing](../testing/index.md) — Testcontainers integration testing for messaging brokers
