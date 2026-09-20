# AWS Messaging

Amazon Web Services offers cloud-native messaging primitives designed for serverless, microservice, and distributed enterprise architectures: **Amazon SQS** (point-to-point queuing), **Amazon SNS** (publish/subscribe push notifications and fanout), and **Amazon EventBridge** (serverless event bus with advanced schema routing).

This topic is a **doc module** focusing on AWS messaging mechanics, queue semantics, architectural trade-offs, operational incident triage, and comprehensive comparison across modern messaging backbones (Kafka, RabbitMQ, SQS, SNS, and EventBridge).

---

## The AWS Messaging Landscape

```mermaid
flowchart TD
    subgraph Producers["Event Producers"]
        P1["Microservice Apps<br/>(Spring Boot / Java)"]
        P2["AWS Services<br/>(S3, DynamoDB, IAM)"]
        P3["SaaS Partners<br/>(Stripe, Zendesk, Auth0)"]
    end

    subgraph Routers["Routing & Fanout"]
        SNS["Amazon SNS<br/>High-throughput Pub/Sub<br/>Push Fanout & SMS/Email"]
        EB["Amazon EventBridge<br/>Schema-aware Event Bus<br/>Content Filtering & SaaS Integrations"]
    end

    subgraph Queues["Buffering & Decoupling"]
        SQS_STD["Amazon SQS Standard<br/>Unlimited Throughput<br/>At-least-once, Best-effort order"]
        SQS_FIFO["Amazon SQS FIFO<br/>Strict Ordering & Deduplication<br/>Partitioned by MessageGroupId"]
    end

    subgraph Consumers["Downstream Consumers"]
        C1["Lambda Serverless"]
        C2["ECS / EKS Container Fleet"]
        C3["External Webhook Endpoints"]
    end

    P1 --> SNS
    P1 --> EB
    P1 --> SQS_STD
    P1 --> SQS_FIFO
    P2 --> EB
    P3 --> EB

    SNS --> SQS_STD
    SNS --> SQS_FIFO
    SNS --> C3

    EB --> SQS_STD
    EB --> SQS_FIFO
    EB --> C1
    EB --> C2

    SQS_STD --> C1
    SQS_STD --> C2
    SQS_FIFO --> C2
```

---

## Architectural Decision Matrix

When should an architect select SQS, SNS, EventBridge, Kafka, or RabbitMQ?

```mermaid
graph TD
    Start["Need asynchronous messaging?"] --> Q1{"Pub/Sub Fanout vs<br/>Queue Buffering?"}
    Q1 -->|Queue Buffering| Q2{"Strict Order vs<br/>Massive Throughput?"}
    Q2 -->|Strict Order & Deduplication| SQS_F["Amazon SQS FIFO<br/>(Financial/Ledger)"]
    Q2 -->|Massive Throughput| SQS_S["Amazon SQS Standard<br/>(Work queues, Batch processing)"]

    Q1 -->|Pub/Sub Fanout| Q3{"High Throughput (<30ms)<br/>vs Complex Event Routing?"}
    Q3 -->|High Throughput / Push Fanout| SNS_D["Amazon SNS<br/>(Millions msg/sec, SQS fanout)"]
    Q3 -->|Complex JSON Routing / SaaS| EB_D["Amazon EventBridge<br/>(Event mesh, Schema registry)"]

    Start --> Q4{"Need Event Sourcing,<br/>Replay, or AMQP Routing?"}
    Q4 -->|Replay / Stream Processing / Commit Log| KAFKA["Apache Kafka<br/>(Real-time streams, CDC)"]
    Q4 -->|Complex AMQP Exchanges / Low Latency| RABBIT["RabbitMQ<br/>(RPC, Topic routing, In-broker state)"]
```

---

## Module Overview

| Resource | Purpose |
|---|---|
| [Concepts](concepts.md) | SQS Standard vs FIFO, Visibility Timeout, Long Polling, DLQ, SNS Fanout, EventBridge Bus & Comprehensive Comparison Matrix |
| [Internals](internals.md) | SQS distributed multi-AZ architecture, lease state machine, receipt handle lifecycle, FIFO partition lanes |
| [Interview Questions](questions.md) | 23 questions across Basic, Intermediate, Senior, and Incident Scenarios |
| [Code Review](code-review.md) | 4 CloudFormation review targets with collapsed issue explanations |
| [Solutions](solutions.md) | Corrected infrastructure templates, design rationales, and trade-offs |
| [Production](production.md) | Incident walkthroughs, CloudWatch metrics, alarms, and production readiness checklist |
| [Exercises](exercises.md) | Hands-on architectural challenges: EventBridge content routing and SQS visibility heartbeat worker |

---

## Related

- [Kafka topic](../kafka/index.md)
- [RabbitMQ topic](../rabbitmq/index.md)
- [Architecture spec](../../spec/curriculum.md)
