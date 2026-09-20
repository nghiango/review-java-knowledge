# Architecture Review — Distributed Lock Without Fencing Tokens

## Context

An engineering team authored Architecture Decision Record `ADR-004-distributed-locking.md` proposing a distributed locking mechanism using Redis (`SET resource_id token NX PX 10000`) to coordinate batch mutations against a shared cloud object storage service (Amazon S3 / Azure Blob).

Review `ADR-004-distributed-locking.md` for client pause hazards, lease expiration race conditions, and shared storage corruption risks.

## What to look for

- Client-side pause hazards (JVM Stop-the-World GC, hypervisor stalls, network packet delays)
- Timeouts and lease validity boundaries relative to I/O execution
- Protection against stale lock holders at the storage layer
- Distributed locking invariants and Martin Kleppmann's fencing token critique
