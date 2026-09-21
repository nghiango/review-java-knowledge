# Module 27 — System Design

This is a **doc module** covering large-scale distributed system design for Senior Java / Spring Boot Backend Engineers:
System design interviewing framework (Scope & FRs/NFRs, Back-of-the-envelope capacity estimations, High-Level Architecture, Deep Dives, Failure modes & Resiliency), end-to-end designs across 8 canonical systems (URL Shortener, Distributed Payment Processing, E-Commerce Order Management, Notification & Alerting Engine, Flash Sale Inventory Reservation, Distributed Rate Limiter, Large File Processing Pipeline, and Hotel/Ticket Booking System), data modeling & partitioning strategies (Sharding keys, Consistent Hashing, Primary-Replica, CQRS, Event Sourcing), distributed consistency models (ACID vs BASE, 2PC vs Saga choreography/orchestration, Transactional Outbox), caching topologies (Cache-aside, write-through, stampede prevention), and failure recovery (Idempotency keys, reconciliation loops, dead-letter redrive, circuit breaking).

The canonical prose, architecture blueprints, interview Q&A, and operational incident guides live in the documentation:

👉 **[System Design Documentation](../../docs/topics/system-design/index.md)**

## Broken Review Examples

This module provides 3 realistic broken system design proposals under `broken-examples/`:

1. `payment-system-two-phase-commit/` — Flawed design proposal relying on distributed synchronous REST 2PC across services, dual writes between the internal database and external PSP (Stripe/PayPal) without Transactional Outbox or reconciliation, and lack of idempotency keys.
2. `distributed-rate-limiter-naive-redis/` — Flawed design proposal using naive non-atomic `GET` then `INCR` Redis operations (Time-Of-Check to Time-Of-Use race condition), failing to handle Redis cluster partitions (fail-open vs fail-close stampedes), and unbounded key TTLs.
3. `flash-sale-inventory-pessimistic-locking/` — Flawed e-commerce design proposing database row-level pessimistic locking (`SELECT ... FOR UPDATE`) directly against a single PostgreSQL inventory row under 100,000 requests/sec, causing immediate lock queue starvation, connection pool exhaustion, and cascading database failure.
