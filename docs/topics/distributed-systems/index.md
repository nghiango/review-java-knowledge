# Distributed Systems

A distributed system consists of autonomous computing nodes communicating over a shared network to coordinate actions and share state. While distributed architectures unlock horizontal scalability and fault tolerance, they introduce fundamental physics and computer science constraints: network partitions, independent clock drift, asynchronous message delivery, and partial failure.

This topic is a **doc module** exploring foundational distributed systems theory, consistency models, consensus mechanics, distributed locking hazards, and operational incident triage.

---

## The Eight Fallacies of Distributed Computing

Coined by L. Peter Deutsch and James Gosling at Sun Microsystems, these eight false assumptions lead to catastrophic software failures when architects treat remote network calls as local in-memory method invocations:

```mermaid
flowchart LR
    subgraph Fallacies["The 8 Fallacies"]
        F1["1. The network is reliable"]
        F2["2. Latency is zero"]
        F3["3. Bandwidth is infinite"]
        F4["4. The network is secure"]
        F5["5. Topology doesn't change"]
        F6["6. There is one administrator"]
        F7["7. Transport cost is zero"]
        F8["8. The network is homogeneous"]
    end
```

---

## The Consistency Spectrum

Consistency in distributed systems is not a binary switch; it is a spectrum of guarantees trading latency and availability for stronger coordination:

```mermaid
flowchart TD
    subgraph Strong["Strong Coordination (High Latency, Reduced Availability on Partition)"]
        Lin["Linearizability (External Real-Time Consistency)"]
        Seq["Sequential Consistency (Total Program Order)"]
    end

    subgraph Causal["Causal Consistency (No Coordination for Concurrent Events)"]
        Caus["Causal Consistency (Happens-Before Preserved)"]
        RYW["Read-Your-Writes Consistency"]
        MR["Monotonic Reads / Monotonic Writes"]
    end

    subgraph Weak["High Availability (Zero Coordination, Highest Performance)"]
        Eventual["Eventual Consistency (Convergence Over Time)"]
    end

    Lin --> Seq --> Caus --> RYW --> MR --> Eventual
```

---

## Module Overview

| Resource | Purpose |
|---|---|
| [Concepts](concepts.md) | Fallacies, Partial Failure, CAP & PACELC, Consistency Models, Quorums, Clocks & Distributed Locking |
| [Internals](internals.md) | Quorum overlap proofs ($R + W > N$), Vector Clocks, Fencing Token validation, and 2PC blocking mechanics |
| [Interview Questions](questions.md) | 23 questions across Basic, Intermediate, Senior, and Incident Scenarios |
| [Code Review](code-review.md) | 3 architectural ADR & Design Document review targets with collapsed issue explanations |
| [Solutions](solutions.md) | Corrected architecture designs, fencing tokens, Hybrid Logical Clocks, and Saga orchestration |
| [Production](production.md) | Incident walkthroughs (The Fencing Token Incident), distributed telemetry, and production checklist |
| [Exercises](exercises.md) | Hands-on challenges: Vector Clock Causality Engine and Storage Fencing Token Validator |

---

## Related

- [AWS Messaging](../aws-messaging/index.md)
- [Kafka](../kafka/index.md)
- [RabbitMQ](../rabbitmq/index.md)
- [Architecture spec](../../spec/curriculum.md)
