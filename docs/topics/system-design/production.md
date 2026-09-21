# System Design Production Operations & Incident Guide

Operational postmortems, telemetry benchmarks, capacity planning models, and production readiness checklists for distributed enterprise architectures.

---

## 1. Production Incident Postmortems

### Incident 1: The Hot-Row Lock Manager Crash

#### Incident Timeline
- **08:59 UTC**: A tier-1 consumer brand launches a limited-edition sneaker release (1,000 units in stock). 50,000 concurrent mobile and web users flood the checkout gateway.
- **09:00:02 UTC**: All 40 application pods execute `SELECT stock FROM inventory WHERE item_id = 42 FOR UPDATE`.
- **09:00:05 UTC**: PostgreSQL's lock manager queues 12,000 concurrent transactions waiting on the identical tuple lock (`Item #42`). Lock table memory consumption spikes, and CPU on the primary database hits 100% in kernel spinlocks traversing the lock wait graph.
- **09:00:15 UTC**: HikariCP connection pools on all 40 application pods reach 100% saturation (`ConnectionTimeoutException: Connection is not available, request timed out after 30000ms`).
- **09:00:30 UTC**: Because the database connection pool is completely exhausted, health check probes (`/actuator/health/readiness`) fail. Kubernetes liveness and readiness probes mark pods as unready and begin terminating pods.
- **09:01:00 UTC**: Cascading collapse: remaining pods receive redirected traffic, instantly saturate their connection pools, and crash. Total platform outage for all product categories, not just the sneaker sale.
- **09:18 UTC**: Engineers kill all incoming checkout traffic at the Cloudflare edge, restart PostgreSQL, and flush hung client transactions.
- **Total Customer Outage**: **18 minutes of complete platform downtime, estimated $450K lost GMV across unrelated product lines**.

#### Root Cause Analysis
1. Direct relational row locking (`SELECT ... FOR UPDATE`) on a single hot record cannot scale beyond dozens of concurrent transactions.
2. Shared database connection pool: checkout queries shared the same HikariCP datasource as read queries and general catalog browsing, allowing one contended query to exhaust all database access across the service.
3. Lack of admission control or queue buffering at the application edge.

#### Permanent Remediation
1. **Separation of Concerns**: Flash sale inventory decrements moved entirely to Redis Lua atomic reservations (`DECRBY`).
2. **Bulkheading & Pool Isolation**: Dedicated, isolated database connection pool for checkout transactions with strict maximum execution timeouts (`statement_timeout = '2s'`, `lock_timeout = '500ms'`).
3. **Edge Rate Limiting & Virtual Waiting Rooms**: Cloudflare Waiting Room activated for extreme traffic spikes to throttle traffic entering the origin infrastructure.

---

### Incident 2: The Double-Charge Outage

#### Incident Timeline
- **18:00 UTC**: Black Friday peak shopping hours begin. Payment gateway traffic surges to 800 transactions/second.
- **18:14 UTC**: Transient packet loss between AWS us-east-1 and Stripe's payment ingress edge causes outbound HTTP requests to hang beyond the 5-second socket timeout configured in the Payment Service.
- **18:14:15 UTC**: Spring `RestClient` throws `ResourceAccessException: Read timed out`. The local checkout database transaction rolls back, marking the order as `FAILED_PAYMENT`.
- **18:14:20 UTC**: The e-commerce frontend automatically prompts users: *"Payment timed out. Please click retry to complete your purchase."*
- **18:14:30 UTC**: Frustrated users repeatedly click the "Pay Now" button 3 to 5 times. Each retry generates a new internal order UUID and makes a new payment request to Stripe without an `Idempotency-Key` header.
- **18:25 UTC**: Customer support receives hundreds of panicked messages alerting that users have been debited 3 to 6 times on their banking apps for a single shopping cart.
- **18:45 UTC**: Stripe's delayed webhook callbacks begin arriving, successfully capturing funds for both the timed-out requests and the retry requests.
- **Total Impact**: **4,200 customers billed multiple times, requiring $820,000 in emergency refunds and $35,000 in payment gateway dispute/chargeback fees**.

#### Root Cause Analysis
1. Payment service did not propagate a persistent, client-generated idempotency key to the external payment processor.
2. Synchronous timeout handling assumed network timeout implied payment failure; in distributed systems, a timeout represents an *unknown* state (the remote operation may have succeeded).
3. Client UI did not disable the submission button upon initiating the charge request.

#### Permanent Remediation
1. **Mandatory Idempotency Keys**: Generated at cart checkout creation time (e.g., UUIDv7) and forwarded in all downstream API calls (`Idempotency-Key` header).
2. **Transactional Outbox & Two-Phase Ingestion**: Payment requests are recorded locally as `PENDING` before external calls; external timeouts trigger background reconciliation rather than customer-facing failure.
3. **Automated Gateway Reconciliation**: Hourly reconciliation workers compare local database transaction IDs against payment provider settlement logs, automatically initiating refunds for any duplicate captures.

---

## 2. Key System Telemetry & Metrics

| Telemetry Metric | Production Target | Diagnostic Significance |
|---|---|---|
| **P99 API Latency** | $< 250\text{ ms}$ | High latency indicates upstream queuing, thread pool starvation, or lock contention. |
| **DB Lock Wait Time** | $< 10\text{ ms}$ | Spikes in lock wait time indicate row-level or table-level serialization bottlenecks. |
| **HikariCP Pending Threads** | $0$ | Non-zero values signal that application threads are waiting for free database connections. |
| **Idempotency Cache Hit Rate** | $> 2\%$ | Identifies frequency of network retries, client double-submits, and broker duplicate deliveries. |
| **Distributed Cache Hit Ratio** | $> 95\%$ | Cache hit drop shifts read traffic directly to the database, risking database collapse. |
| **Kafka / Queue Consumer Lag** | $< 1,000\text{ messages}$ | Rising lag indicates worker thread exhaustion, slow downstream dependencies, or poisoned messages. |

---

## 3. Capacity Planning: Back-of-the-Envelope Estimation

When designing distributed systems for high-scale applications, always validate your design with capacity estimation:

### Storage Estimation (E-Commerce Order System)
- **Daily Order Volume**: $10\text{ million orders/day}$.
- **Average Record Size**: $2\text{ KB}$ per order (metadata, items, customer info).
- **Daily Data Ingestion**:
  $$10\text{M} \times 2\text{ KB} = 20\text{ GB/day}$$
- **5-Year Data Growth (including indexes $\times 2$)**:
  $$20\text{ GB/day} \times 365 \times 5 \times 2 \approx 73\text{ TB}$$
- **Architectural Conclusion**: Single PostgreSQL instance cannot support 5 years of hot data. Implement horizontal sharding by `customer_id` or date-based table partitioning with archival to Amazon S3.

### Throughput Estimation (Write QPS)
- **Average Write QPS**:
  $$\frac{10\text{M orders}}{86,400\text{ seconds}} \approx 116\text{ writes/sec}$$
- **Peak Write QPS ($5\times$ average multiplier)**:
  $$116 \times 5 \approx 580\text{ writes/sec}$$
- **Flash Sale Spike QPS**: $10,000\text{ to }50,000\text{ writes/sec}$.
- **Architectural Conclusion**: Standard relational databases comfortably handle $580\text{ writes/sec}$. However, flash sale spikes ($10,000+\text{ QPS}$) require memory buffering (Redis) and asynchronous messaging (Kafka).

---

## 4. Production Readiness Checklist

- [ ] **End-to-End Idempotency**: Every state-altering API accepts a unique idempotency key persisted before execution.
- [ ] **Bounded Connection Pools**: Database, Redis, and HTTP client connection pools are explicitly sized and bounded.
- [ ] **Strict Timeouts Everywhere**: All external RPCs, HTTP calls, and database statements specify explicit connect and socket timeouts.
- [ ] **Bulkheading & Graceful Degradation**: Critical paths (order placement) are physically or logically decoupled from non-critical paths (recommendations, reviews).
- [ ] **Circuit Breaking & Fallbacks**: Resilience4j or Envoy circuit breakers prevent cascading failures when dependencies fail.
- [ ] **Zero Unbounded Queries**: All database queries enforce pagination (`LIMIT` / `OFFSET` or keyset pagination).
- [ ] **Chaos & Disaster Recovery Tested**: Regularly simulate node failure, network latency, and regional database failover in staging environments.
