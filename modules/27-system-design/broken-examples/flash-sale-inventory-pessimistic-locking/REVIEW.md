# System Design Review: Flash Sale Inventory System

## Context
This design proposal was submitted for an upcoming high-traffic flash sale event expected to attract 100,000 concurrent checkout attempts per second against a limited inventory of 10,000 items.

## Files Under Review
- `design.md` — Proposed architecture, transaction strategy, and database schema.

## Review Questions
1. What happens internally inside the PostgreSQL lock manager when 100,000 concurrent transactions execute `SELECT ... FOR UPDATE` against the exact same table row?
2. How does this design impact HikariCP connection pools, transaction queue latency, and non-flash-sale database queries?
3. What is the database CPU and memory behavior under this lock contention?
4. How would you redesign this architecture using a **Multi-Tier Reservation System**:
   - Tier 1: In-memory pre-allocation with atomic Redis `DECR` / Lua script.
   - Tier 2: Asynchronous message queue (Kafka / SQS) to buffer and smooth database persistence.
   - Tier 3: Database optimistic locking with temporary reservation hold timeouts.
