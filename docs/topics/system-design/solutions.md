# System Design Solutions & Architectural Patterns

Detailed engineering walkthroughs and production-grade architectures correcting the broken design targets identified in [Code Review](code-review.md).

---

## 1. Event-Driven Payments: Transactional Outbox, Idempotency, and Reconciliation

### Correct Implementation: `design.md`

--8<-- "modules/27-system-design/broken-examples/payment-system-two-phase-commit/correct/design.md"

### Architectural Rationale & Trade-offs

1. **Decoupling Database State from External Latency**:
   - The client checkout HTTP request only inserts an `orders` record and an `outbox` event within a single local PostgreSQL ACID transaction (typically $< 5\text{ ms}$).
   - The database transaction commits immediately without waiting on external third-party HTTP round-trips. Database connection pools remain available and unsaturated.
2. **Deterministic Idempotency Key Propagation**:
   - The client provides or receives a deterministic idempotency key (e.g., `checkout_token` / UUIDv7) persisted in the database.
   - The Outbox dispatcher forwards this exact key in the HTTP header `Idempotency-Key` to Stripe. Any duplicate message delivery from Kafka or outbox polling is recognized by Stripe as an identical intent, returning the cached charge response without double-billing.
3. **Eventual Consistency and Asynchronous Reconciliation**:
   - A dedicated reconciliation worker runs periodically (every 5 minutes) querying payment gateway APIs for pending or dangling transactions.
   - In the event of gateway timeouts or message broker crashes, the reconciliation loop heals state drift, either transitioning orders to `PAID` or marking them `FAILED` and issuing customer notifications.
4. **Trade-offs**:
   - **Increased Latency for Final Status**: Checkouts become asynchronous. The UI must either poll a status endpoint (`GET /orders/{id}`) or subscribe to a WebSocket / SSE feed to confirm final payment settlement.
   - **Infrastructure Overhead**: Requires Kafka/RabbitMQ or an outbox CDC poller (Debezium) alongside an automated reconciliation scheduler.

---

## 2. Distributed Rate Limiter: Atomic Redis Lua Scripting and Sliding Window

### Correct Implementation: `design.md`

--8<-- "modules/27-system-design/broken-examples/distributed-rate-limiter-naive-redis/correct/design.md"

### Architectural Rationale & Trade-offs

1. **Atomicity via Redis Lua Scripting**:
   - Redis evaluates Lua scripts in a single atomic execution context on its single-threaded event loop.
   - Counter increments and TTL assignments happen atomically:
     ```lua
     local current = redis.call('INCR', key)
     if current == 1 then
         redis.call('EXPIRE', key, window_seconds)
     end
     ```
   - This completely eliminates orphaned, immortal keys and prevents persistent HTTP 429 lockouts.
2. **Boundary Burst Elimination with Sliding Window Counter**:
   - Using a sliding window counter (weighted average of previous and current window counters) or sliding window log smooths out traffic spikes across window transitions.
   - A client cannot burst $2\times$ the rate limit at boundary minutes ($12:00:59$ and $12:01:01$).
3. **Resilient Fail-Open Strategy**:
   - When Redis experiences high latency ($> 50\text{ ms}$) or connectivity drops, the application rate limiter falls open: traffic is allowed through to upstream services while an alert is dispatched to operations.
   - Rate limiting is an advisory protection layer; it should degrade gracefully during infrastructure distress rather than shutting down the business.
4. **Trade-offs**:
   - **Fail-Open Downstream Risk**: During a Redis failure, downstream services become vulnerable to spikes. Mitigate with local in-memory fallback limiters (e.g., Bucket4j/Caffeine) per node.
   - **Script Execution Overhead**: Lua scripts running on Redis must be kept minimal ($O(1)$) to avoid blocking other commands.

---

## 3. High-Velocity Flash Sale: Two-Stage Reservation & Asynchronous Fulfillment

### Correct Implementation: `design.md`

--8<-- "modules/27-system-design/broken-examples/flash-sale-inventory-pessimistic-locking/correct/design.md"

### Architectural Rationale & Trade-offs

1. **Protecting Relational Database via In-Memory Ingress**:
   - High-concurrency inventory reservation is shifted entirely to Redis in-memory atomic operations (Lua script with `DECRBY`).
   - Redis handles tens of thousands of ops/second at sub-millisecond latencies, absorbing the flash sale burst and instantly rejecting excess requests (`Out of Stock`) without touching PostgreSQL.
2. **Asynchronous Order Processing Queue**:
   - Successful reservations publish an order event to a durable Kafka/RabbitMQ queue.
   - Worker pools consume messages at a controlled, steady pace that PostgreSQL can comfortably handle (e.g., 200 writes/sec), eliminating connection pool starvation and lock manager thrashing.
3. **Hold Timer with Automated Stock Reclamation**:
   - Reservations are created with a time-to-live (e.g., 10 minutes) to allow customer payment completion.
   - If payment is not completed before expiration, an asynchronous scheduler or Redis key expiration event releases the reserved quantity back into the available stock pool.
4. **Trade-offs**:
   - **Eventual Consistency Complexity**: Stock levels in Redis and PostgreSQL may differ momentarily while messages are in flight.
   - **Ghost Reservation Risk**: Requires robust reconciliation to ensure expired uncompleted checkouts reliably replenish the Redis stock counter.
