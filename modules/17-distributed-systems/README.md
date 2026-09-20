# Module 17 — Distributed Systems

This is a **doc module** covering foundational principles, failure modes, consistency trade-offs, and architectural patterns of distributed systems:
Fallacies of distributed computing, partial failure, CAP and PACELC theorems, consistency models (from Linearizability to Eventual Consistency), replication quorums ($R + W > N$), physical vs logical clocks (NTP drift, Lamport, Vector, HLC, TrueTime), distributed locking pitfalls (GC pause lease expiration, Kleppmann-Sanfilippo debate, fencing tokens), load balancing, and backpressure.

The canonical prose, comparison matrices, interview Q&A, and architectural guides live in the documentation:

👉 **[Distributed Systems Documentation](../../docs/topics/distributed-systems/index.md)**

## Broken Review Examples

This module provides 3 realistic system design and architecture decision record (ADR) review targets under `broken-examples/`:

1. `distributed-lock-no-fencing/` — Architecture Decision Record specifying a Redis `SET NX PX` distributed lock lacking fencing tokens, vulnerable to client GC pauses and shared storage corruption.
2. `wall-clock-order-assumption/` — Design document relying on server physical wall clocks (`System.currentTimeMillis()`) for multi-region ledger conflict resolution (LWW), vulnerable to NTP clock drift and lost updates.
3. `two-phase-commit-coordinator/` — Design document introducing synchronous Two-Phase Commit (2PC) across microservices, resulting in coordinator blocking, cascading lock timeouts, and availability collapse during network partitions.
