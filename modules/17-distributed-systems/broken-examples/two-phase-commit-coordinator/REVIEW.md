# Architecture Review — Synchronous Two-Phase Commit Across Microservices

## Context

An e-commerce engineering organization designed a distributed transaction protocol to coordinate order placement across three separate microservices: `OrderService`, `PaymentService`, and `InventoryService`. The proposed architecture `DESIGN-distributed-checkout-2pc.md` adopts a synchronous Two-Phase Commit (2PC) coordinator.

Review `DESIGN-distributed-checkout-2pc.md` for coordinator single point of failure (SPOF) risks, lock holding during network partitions, blocking failure modes, and microservice coupling.

## What to look for

- Coordinator failure modes and blocking states in Phase 2
- Distributed lock contention across network boundaries
- Availability impact under CAP theorem (trade-off between C and A)
- Alternatives for eventual consistency and decoupled compensating workflows (Saga pattern)
