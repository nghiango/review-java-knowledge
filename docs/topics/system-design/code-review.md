# System Design Code & Architecture Review Practice

Review the following architectural proposals, sequence flows, and distributed data designs. Identify distributed consensus pitfalls, hot-key lock manager bottlenecks, network partition vulnerabilities, and financial double-charge hazards before expanding the solution panels.

---

## 1. Synchronous Two-Phase Commit with External Payment Gateways

An engineering team submitted this design proposal for an enterprise e-commerce checkout flow processing high-value transactions across internal PostgreSQL databases and external third-party payment gateways (e.g., Stripe, Adyen).

### Broken Target: `design.md`

--8<-- "modules/27-system-design/broken-examples/payment-system-two-phase-commit/design.md"

??? question "Reveal issues"
    1. **Infeasible 2PC Across External SaaS Boundaries**:
       - External payment gateways (Stripe, Adyen, PayPal) communicate over standard REST/JSON APIs and do not implement XA or 2PC protocols (`prepare`, `commit`, `rollback`).
       - Third-party SaaS providers cannot hold distributed uncommitted locks or subordinate their internal transaction lifecycles to an external customer coordinator.
    2. **Database Connection Pool Exhaustion**:
       - Initiating an external HTTP call while holding an open PostgreSQL transaction keeps the database connection occupied for the full duration of external network round-trips (often 500ms to 3,000ms).
       - Under high checkout concurrency, this quickly exhausts the HikariCP connection pool, starving unrelated database operations and bringing down the entire microservice.
    3. **Silent Double-Charge Inconsistency**:
       - If the external payment processor successfully captures funds but a transient network partition or timeout occurs before the coordinator receives the response, the database transaction rolls back.
       - The customer's credit card is charged, but the internal database marks the payment as failed or missing.
    4. **Unbounded Client Retries Without Idempotency**:
       - Without an end-to-end unique idempotency key propagated from the client checkout session down to the external gateway, a frustrated customer refreshing or retrying causes duplicate charges.

---

## 2. Distributed Rate Limiter with Naive Redis INCR and Window Skew

This architectural design specifies a distributed rate limiter deployed across 30 microservice nodes to protect backend payment and inventory APIs from abusive traffic and credential stuffing.

### Broken Target: `design.md`

--8<-- "modules/27-system-design/broken-examples/distributed-rate-limiter-naive-redis/design.md"

??? question "Reveal issues"
    1. **Non-Atomic INCR and EXPIRE Race Condition (Permanent Key Leak)**:
       - The rate limiter executes `INCR` followed by a distinct `EXPIRE` command. If the application crashes, network disconnects, or Redis failover occurs between these two calls, the key is created without an expiration TTL.
       - The client is permanently blocked (HTTP 429) forever once the threshold is crossed, and Redis memory leaks indefinitely.
    2. **Fixed-Window Boundary Traffic Burst (2x Threshold Violation)**:
       - Fixed-window rate limiting resets counters at whole minute intervals (`rate:{userId}:12:00`, `rate:{userId}:12:01`).
       - A client sending 100 requests at 12:00:59 and another 100 requests at 12:01:01 transmits 200 requests within a 2-second window without triggering any rate limit, exceeding downstream capacity by 100%.
    3. **Fail-Closed Strategy on Redis Outage**:
       - Rejecting all incoming production traffic with HTTP 429 when Redis is unreachable converts a caching/rate-limiting outage into a catastrophic total platform blackout.
    4. **Network Multi-Round-Trip Latency Overhead**:
       - Executing separate `INCR` and `EXPIRE` round-trips across a 30-node cluster doubles Redis command latency and network utilization compared to an atomic Lua script.

---

## 3. Flash Sale Inventory System with Direct Pessimistic Row Locking

An engineering proposal for handling flash sale traffic (10,000 requests/second competing for 500 limited-edition stock units) using direct relational database pessimistic locking.

### Broken Target: `design.md`

--8<-- "modules/27-system-design/broken-examples/flash-sale-inventory-pessimistic-locking/design.md"

??? question "Reveal issues"
    1. **Extreme Lock Manager Contention on Hot Row**:
       - 10,000 concurrent threads issuing `SELECT ... FOR UPDATE` on a single inventory row serialize behind PostgreSQL's lock manager.
       - Lock queue traversal overhead spikes database CPU to 100%, while database connection pools (HikariCP) saturate within milliseconds, cascading into thread pool starvation and catastrophic timeouts across the entire platform.
    2. **Synchronous Payment Authorization Under Row Lock**:
       - The transaction holds the pessimistic database lock while making a synchronous outbound HTTP request to an external payment processor (taking 800ms–2,000ms).
       - This limits system-wide inventory decrement throughput to $\approx 1$ transaction per second on that SKU, rendering high-velocity flash sales impossible.
    3. **Missing Circuit Breaking and Queue Buffering**:
       - Incoming web traffic directly hits the relational database without a high-speed in-memory buffer (Redis Atomic Decrement / Lua) or an asynchronous decoupling queue (Kafka/RabbitMQ).
