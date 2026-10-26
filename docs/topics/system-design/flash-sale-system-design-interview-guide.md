# Flash-Sale System Design Interview Guide

Compiled from [System Design Interview Prep](https://chatgpt.com/share/6adf1a6a-ca10-83ec-aa13-4cdfab6fd529), accessed 26 October 2026.

This is an edited, topic-organized study guide rather than a verbatim transcript. Repeated guidance is consolidated, interactive diagrams are expressed as text, and code is formatted for reuse. It covers the original interview rounds through distributed quota allocation, plus the subsequent failover, payments, recovery, migration, and multi-region material visible in the shared conversation. Examples are illustrative and some are adapted to make transaction boundaries and caveats explicit; they are not a complete production application.

## Contents

1. [Interview approach and scenario](#1-interview-approach-and-scenario)
2. [Requirements clarification](#2-requirements-clarification)
3. [Capacity estimation](#3-capacity-estimation)
4. [Recommended purchase architecture](#4-recommended-purchase-architecture)
5. [Atomic inventory reservation and Redis Lua](#5-atomic-inventory-reservation-and-redis-lua)
6. [Crash between Redis reservation and Kafka publication](#6-crash-between-redis-reservation-and-kafka-publication)
7. [Payment versus reservation expiry](#7-payment-versus-reservation-expiry)
8. [Kafka duplicates and effectively-once processing](#8-kafka-duplicates-and-effectively-once-processing)
9. [Distributed rate limiting and quota allocation](#9-distributed-rate-limiting-and-quota-allocation)
10. [Quota failover and durable grants](#10-quota-failover-and-durable-grants)
11. [End-to-end purchase and payment idempotency](#11-end-to-end-purchase-and-payment-idempotency)
12. [Transactional outbox at extreme throughput](#12-transactional-outbox-at-extreme-throughput)
13. [Debezium outage, WAL retention, and recovery](#13-debezium-outage-wal-retention-and-recovery)
14. [Zero-downtime schema migration](#14-zero-downtime-schema-migration)
15. [Mixed application versions and event compatibility](#15-mixed-application-versions-and-event-compatibility)
16. [Database connection pool exhaustion](#16-database-connection-pool-exhaustion)
17. [Payment retry storms and dependency protection](#17-payment-retry-storms-and-dependency-protection)
18. [One million attempts per second for 100 units](#18-one-million-attempts-per-second-for-100-units)
19. [Multi-region inventory ownership](#19-multi-region-inventory-ownership)
20. [Regional disaster recovery and split brain](#20-regional-disaster-recovery-and-split-brain)
21. [Interview answer and revision checklist](#21-interview-answer-and-revision-checklist)

## 1. Interview approach and scenario

The conversation begins with Senior Java system design preparation, progressing into Staff/Principal distributed-systems follow-ups. The preparation roadmap covers requirements, capacity, load balancing, caching, SQL/NoSQL, replication, partitioning, transactions, Saga, outbox, Kafka, idempotency, availability, retries, backpressure, observability, and disaster recovery.

The suggested 60-minute format is:

| Stage | Time | What to demonstrate |
|---|---:|---|
| Requirements | 5 minutes | Clarify business rules and guarantees |
| Capacity | 5 minutes | Quantify bursts and bottlenecks |
| High-level architecture | 10 minutes | Explain components and ownership |
| Detailed design | 15 minutes | APIs, schema, transactions, concurrency |
| Scaling and reliability | 15 minutes | Overload and partial failures |
| Follow-ups | 10 minutes | Defend trade-offs and recovery choices |

The original flash-sale scenario is:

| Metric | Value |
|---|---|
| Registered users | 10 million |
| Concurrent sale users | 500,000 |
| Campaign duration | 10 minutes |
| Products | 1,000 |
| Popular-product inventory | 100 units |
| Application | Java 21, Spring Boot |
| Infrastructure | AWS |

The two central requirements are no overselling and a responsive platform during extreme demand. Later questions deliberately change the workload and constraints; their numbers are separate scenarios, not one combined capacity estimate.

## 2. Requirements clarification

**Interview question:** Before drawing an architecture, what would you ask the business?

### Guided explanation

Separate functional requirements, non-functional requirements, and business constraints.

| Clarification | Design implication |
|---|---|
| Units per user and repeated purchases | Durable purchase-limit enforcement |
| Reserve before payment? | Reservation state and stock accounting |
| Payment deadline | Expiration and delayed-payment policy |
| Failed payment or cancellation | Idempotent stock release and compensation |
| Immediate or asynchronous confirmation | Response contract and queue design |
| Peak RPS and burst duration | Admission and infrastructure sizing |
| Latency target | Initial acknowledgment versus completed purchase |
| Availability target | Failure policy and redundancy |
| Strict no overselling | Authoritative inventory boundary |
| Fairness and bots | Waiting room, authentication, abuse protection |
| Existing order/payment systems | Integration and migration constraints |
| Budget and delivery constraints | Complexity appropriate to the team |

Distinguish **reserved** from **sold**. If all 100 units are reserved but unpaid, the item may be temporarily unavailable without having 100 completed sales.

Do not equate concurrent users with RPS. If 500,000 users submit once over five seconds, the initial estimate is 100,000 requests/second, before retries and other calls.

### Working assumptions established in the conversation

- One unit per user per product in the sale.
- Strictly no overselling.
- Inventory reserved before payment.
- Five-minute payment window.
- Release eligible reservations after confirmed failure or expiration.
- Asynchronous order confirmation acceptable.
- Initial API acknowledgment P95 below 200 ms.
- Availability target of 99.9% during the campaign.
- First-come, first-served as the stated business preference.
- Peak purchase traffic up to 100,000 RPS.

The latency target does not mean payment or final order confirmation completes within 200 ms. Clarify what first-come, first-served means across gateways and queues; local arrival order is not automatically a single global order.

### Interview-ready answer

> Before selecting components, I would clarify purchase limits, when stock becomes reserved, the payment deadline, and what happens after failure or cancellation. I would ask whether asynchronous confirmation is acceptable and distinguish the initial acknowledgment latency from final completion. I would quantify peak RPS and burst duration rather than infer them from concurrent users. Since overselling is prohibited, I would prioritize inventory correctness and clarify acceptable rejection or delay during failures. Finally, I would confirm fairness, bot controls, integration constraints, and budget.

## 3. Capacity estimation

**Interview question:** How do you estimate traffic, application capacity, and database work when 100,000 purchase requests arrive per second for a product with 100 units?

### Calculations

| Estimate | Calculation | Caveat |
|---|---|---|
| Requests if peak lasts the entire sale | 100,000 × 600 = 60 million | Upper-bound scenario, not a forecast |
| In-flight requests | 100,000/s × 0.2 s = 20,000 | Little's Law uses average latency, not P95 |
| Instances at measured 2,000 RPS each | 100,000 / 2,000 = 50 | Example load-test result, not a Spring Boot guarantee |
| Instances at 70% target utilization | ceil(100,000 / (2,000 × 0.7)) = 72 | Validate downstream limits and failure headroom |

Java 21 virtual threads or asynchronous handling mean in-flight requests need not correspond one-to-one with operating-system threads. They do not remove CPU, database, or downstream concurrency limits.

### The database bottleneck

This read-then-write sequence is unsafe:

```sql
SELECT stock FROM inventory WHERE product_id = 101;
-- Application checks the value, then writes later.
UPDATE inventory SET stock = stock - 1 WHERE product_id = 101;
```

An atomic conditional update provides a stock guard:

```sql
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 101 AND stock > 0;
```

But 100,000 competing updates to one inventory row still create hot-row contention. Correctness and throughput are different concerns.

The design should reject invalid, abusive, duplicate, and sold-out requests early. Only a bounded number of attempts should reach authoritative reservation processing. Across expiration and re-admission cycles, the total number of attempts over the campaign can exceed 100; the invariant concerns simultaneously allocated and completed stock, not lifetime request count.

### Interview-ready answer

> I would size for the launch burst, then validate with representative load tests. At 100,000 RPS and an illustrative average latency of 200 ms, the application tier has roughly 20,000 requests in flight. If an instance sustains 2,000 RPS, 50 instances is the theoretical minimum and around 72 gives 30% headroom. The main risk is still the shared inventory row. For 100 units, the goal is to bound database reservation work rather than send every incoming request to PostgreSQL. I would measure Redis hot-key throughput, Kafka capacity, connection limits, and queue lag as part of the same test.

## 4. Recommended purchase architecture

**Interview question:** Explain the flow from Buy Now to order creation and payment.

```text
Client: stable purchase idempotency key
  → CDN / WAF / waiting room
  → API gateway: authentication, abuse controls, rate limits
  → Flash Sale Service + Redis: provisional admission and deduplication
  → Durable reservation transaction in PostgreSQL
       conditional stock allocation + reservation + outbox
  → Outbox publisher / CDC → Kafka
  → Order Service: idempotent order creation
  → Payment workflow: stable payment operation identity
  → Confirm, reconcile, expire, or compensate
```

At higher scale, admitted commands can enter a bounded Kafka reservation workflow **before** inventory allocation. That is a different acknowledgment boundary: the response means the command is durably accepted, not that stock is already reserved. Do not mix these two contracts.

| Component | Responsibility | Limitation/trade-off |
|---|---|---|
| Gateway | Authentication, routing, coarse admission | Does not establish stock ownership |
| Redis | Fast filtering and provisional counters | Atomic primary operations do not guarantee failover durability |
| PostgreSQL | Authoritative stock, reservations, constraints | Hot rows and finite concurrency |
| Outbox | Durable publication intent with business commit | Storage, relay, retention, duplicates |
| Kafka | Decoupling and durable asynchronous work | Lag, redelivery, partition-order constraints |
| Order Service | Durable order state and idempotency | Eventual confirmation |
| Payment Service | Provider integration and reconciliation | External outcomes can be uncertain |

### Purchase sequence

1. Authenticate and validate campaign eligibility; reuse the client idempotency key on retries.
2. Apply admission controls and provisional Redis checks.
3. Durably record the accepted work. For a confirmed reservation, commit the stock decrement, reservation, and outbox together.
4. Deliver the event through Kafka. Create an order once per reservation, initially pending payment.
5. Track the five-minute deadline and a durable payment operation.
6. Confirm payment through conditional transitions. Release only eligible reservations, once. Reconcile unknown outcomes and compensate late successes as required.

### Interview-ready answer

> I would use Redis to reject unnecessary work quickly, PostgreSQL to enforce inventory correctness, and Kafka to decouple order creation. A reservation is confirmed only after its inventory allocation, reservation record, and outbox event commit together. Kafka consumers use unique business identifiers and transactional deduplication. Payment success and expiration compete through database state transitions, while uncertain provider outcomes go to reconciliation. This accepts eventual order confirmation and additional operational complexity in exchange for bounded downstream load and recoverable correctness.

## 5. Atomic inventory reservation and Redis Lua

### Authoritative SQL guard

```sql
UPDATE inventory
SET available = available - 1,
    reserved = reserved + 1
WHERE product_id = :productId
  AND available > 0;
```

Check the affected row count. Zero means the allocation did not occur. A successful decrement must share its transaction with the reservation and outbox insert.

Durable constraints must also enforce:

- One reservation per logical purchase attempt.
- The intended one-unit-per-user-per-product rule, scoped to the sale.
- One order per reservation.

Do not accidentally apply a campaign-wide `(campaign_id, user_id)` restriction if the business allows the customer to buy one unit of **each** product. The conversation's campaign/user index example explicitly depends on the campaign-level business rule.

### Redis Lua example from the high-scale discussion

```lua
local stock = tonumber(redis.call("GET", KEYS[1]) or "0")

if stock <= 0 then
    return 0
end

redis.call("DECR", KEYS[1])
return 1
```

This atomically checks and decrements one Redis counter on its primary. It does not include the user purchase-limit check, durable reservation identity, or safe compensation protocol. Those must be designed separately.

The architecture also discusses a Lua operation that checks for an existing user reservation, verifies stock, decrements it, and records a provisional reservation atomically. That remains provisional: Redis failover can lose acknowledged writes, and the script does not create an atomic transaction with PostgreSQL or Kafka. Multi-key scripts in a clustered deployment also need keys arranged for the applicable execution constraints; do not assume arbitrary cross-shard atomicity.

### Interview-ready answer

> I use a conditional SQL update as the final stock boundary and Redis Lua as a fast admission optimization. Lua prevents interleaving within one Redis primary, but I would not use that fact to claim durability across failover or atomicity with Kafka. Durable uniqueness constraints enforce the logical purchase and reservation rules. The database allocation, reservation, and outbox commit together.

## 6. Crash between Redis reservation and Kafka publication

**Interview question:** Redis decreases stock from 100 to 99, then the application crashes before durable recording or Kafka publication. The customer retries. How do you avoid lost inventory and duplicate orders?

### Why common answers are incomplete

| Proposal | Why it is insufficient alone |
|---|---|
| Add a Redis TTL | Expiration does not prove stock may safely be incremented |
| Retry Kafka publication | There is no retryable work after a crash unless intent was recorded durably |
| Lua + Kafka send | Separate systems still have a partial-failure gap |
| Saga compensation | Compensation must itself be durable, idempotent, and based on known state |
| Transactional outbox | Solves database-to-event handoff, but not a prior independent Redis mutation |

### Reservation and outbox transaction — Java

```java
@Transactional
public Reservation reserve(Long userId, Long productId, String requestId) {
    var existing = reservationRepository.findByRequestId(requestId);
    if (existing.isPresent()) {
        return existing.get();
    }

    int updated = inventoryRepository.reserveStock(productId);
    if (updated == 0) {
        throw new SoldOutException();
    }

    Reservation reservation = reservationRepository.save(
        new Reservation(requestId, userId, productId, "RESERVED")
    );
    outboxRepository.save(OutboxEvent.orderRequested(reservation.getId()));
    return reservation;
}
```

**Caveat:** The preliminary lookup is not concurrency protection. Two requests can both see no existing record. A unique request/reservation constraint must arbitrate the race; the losing transaction must roll back its inventory change and then retrieve the existing result in a separate transaction. Scope the idempotency key to the authenticated caller and validate the request fingerprint.

### Crash recovery

| Failure point | Recovery |
|---|---|
| Before database commit | All durable changes roll back; retry the logical attempt |
| After database commit, before HTTP response | Retry returns the existing reservation/result |
| After commit, before publication | Relay finds the durable outbox event |
| After publication, before relay checkpoint | Possible duplicate; downstream deduplicates |
| After provisional Redis decrement only | Reconciliation checks authoritative reservation state |

Each provisional Redis claim needs a unique identity and deadline. Expired claims must be checked against the database before release. Release must be idempotent; deleting a key or observing TTL expiry must not cause repeated stock increments or release an already committed allocation.

### Interview-ready answer

> Redis and Kafka do not share an ordinary ACID transaction. I would separate provisional admission from durable reservation, with PostgreSQL as the final inventory authority. The reservation, stock allocation, and outbox event commit in one transaction. Client retries reuse a stable key, and the order consumer enforces one order per reservation. A reconciliation worker safely releases provisional Redis claims that never became durable reservations. The outbox fixes publication recovery, while reconciliation closes the independent Redis gap.

## 7. Payment versus reservation expiry

**Interview question:** At the five-minute deadline, a successful payment callback and an expiration worker both read `PENDING_PAYMENT`. How do you prevent both payment and cancellation, and duplicate stock release?

### State machine and conditional transitions

```text
PENDING_PAYMENT → PAID
PENDING_PAYMENT → CANCELLED

For uncertain payment outcomes:
PENDING_PAYMENT → EXPIRING / PAYMENT_REVIEW → resolved outcome
```

`@Transactional` alone does not stop two transactions from making decisions based on the same earlier state.

```sql
-- Payment path; also validate expected payment identity and outcome.
UPDATE orders
SET status = 'PAID', updated_at = NOW()
WHERE id = :orderId AND status = 'PENDING_PAYMENT';

-- Simplified expiry path; production also checks the deadline
-- and follows the policy for uncertain payments.
UPDATE orders
SET status = 'CANCELLED', updated_at = NOW()
WHERE id = :orderId AND status = 'PENDING_PAYMENT';
```

For the PostgreSQL concurrency behavior discussed in the conversation, competing updates serialize on the row and re-evaluate the predicate after the winning commit under the relevant isolation behavior. At stronger isolation levels, be prepared to retry serialization failures. Always inspect the affected row count.

### Release a reservation once

```sql
UPDATE inventory_reservations
SET status = 'RELEASED'
WHERE order_id = :orderId AND status = 'RESERVED';

-- Execute only if the reservation transition affected one row:
UPDATE inventory
SET available = available + 1, reserved = reserved - 1
WHERE product_id = :productId;
```

The order transition, reservation transition, stock update, and outbox record belong in one transaction **when owned by the same database**. Across services/databases, use durable commands/events, idempotent state transitions, and reconciliation rather than claiming a single local transaction spans them.

### Java repository example

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Modifying
    @Query("""
        UPDATE Order o SET o.status = :newStatus
        WHERE o.id = :orderId AND o.status = :expectedStatus
        """)
    int transitionStatus(Long orderId,
                         OrderStatus expectedStatus,
                         OrderStatus newStatus);
}
```

Only the successful transition may write the corresponding business event or initiate a stock release. Payment observations must still be durably recorded if the order transition fails. Record reconciliation work transactionally; do not hold the database transaction open during a provider call.

### Delayed callback caveat

Payment may complete at 10:04:59 while its callback arrives at 10:05:02. Callback arrival time is not provider completion time. Define which trusted timestamp determines eligibility. Query/reconcile uncertain outcomes before releasing stock when policy requires it. If cancellation has already won and payment succeeded, create an idempotent refund or explicitly defined compensation; never silently discard the charge.

| Concurrency mechanism | Best fit | Trade-off |
|---|---|---|
| Conditional update | Simple state transition | Requires explicit predicates and row-count handling |
| Optimistic versioning | Lower-contention entity changes | Conflicts require retries |
| Pessimistic row locking | Complex serialized decisions | Lock duration and deadlock risk |

### Interview-ready answer

> I would enforce a state machine with conditional database updates so only one transition out of pending payment can succeed. Only the winning cancellation can conditionally release the reservation and increment stock, in the same transaction where possible. Callbacks and expiration work are idempotent. I would also distinguish a delayed callback from a late payment: uncertain outcomes need review, and a successful payment for an already cancelled order requires compensation. Transaction annotations alone do not solve that race.

## 8. Kafka duplicates and effectively-once processing

**Interview questions:** A consumer commits an order then crashes before committing its Kafka offset. How do you prevent two orders for one reservation? What if `PaymentSucceeded` or `OrderConfirmed` is delivered several times across different pods?

### Transactional inbox

```sql
CREATE TABLE processed_events (
    consumer_name VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (consumer_name, event_id)
);

INSERT INTO processed_events (consumer_name, event_id)
VALUES ('order-payment-handler', :eventId)
ON CONFLICT DO NOTHING
RETURNING event_id;
```

If no row is returned, this logical consumer has processed the event. The inbox claim and business change must commit together. If the transaction rolls back, the message remains eligible for processing.

```java
@Transactional
public void handle(ReservationCreated event) {
    boolean claimed = inbox.insertIfAbsent("order-service", event.eventId());
    if (!claimed) return;

    orderRepository.insertIfAbsent(
        event.reservationId(), event.userId(), event.productId()
    );
}
```

An order's `reservation_id` also needs a unique business constraint. Two distinct event IDs can describe the same reservation; event deduplication alone is insufficient.

### Payment confirmation and outbox

```text
BEGIN
  Claim inbox event
  Validate payment operation, order, amount, currency
  Conditionally transition pending order to confirmed
  If transition succeeds: insert OrderConfirmed into outbox
  Otherwise: classify duplicate or persist reconciliation work
COMMIT
Then acknowledge Kafka according to the listener configuration
```

```java
@Transactional
public void handle(PaymentSucceeded event) {
    if (!inbox.insertIfAbsent("order-payment-handler", event.eventId())) return;

    boolean confirmed = orders.confirmIfPending(
        event.orderId(), event.paymentOperationId()
    );
    if (confirmed) {
        outbox.insert(OrderConfirmed.from(event));
    } else {
        // Persist the observed payment and classify the current order state.
        // Conflicts/cancellation require durable reconciliation, not blind success.
        reconciliation.record(event);
    }
}
```

Repository logic must verify expected payment identity, amount, currency, and order reference. A zero-row update is not automatically a harmless duplicate.

Spring Kafka acknowledgment behavior depends on the configured listener container and error handler. Acknowledge only after successful database commit, and test the actual configuration under crash recovery. Do not infer this guarantee merely from the presence of `@KafkaListener` or `@Transactional`.

### Fulfillment boundary

```sql
CREATE TABLE fulfillments (
    fulfillment_id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    shipping_request_id UUID NOT NULL UNIQUE
);
```

This assumes one fulfillment per order. With split shipments, enforce uniqueness on the actual fulfillment unit or shipment group instead. Fulfillment needs its own inbox and a transaction combining fulfillment creation with a `ShipmentRequested` outbox event.

An external shipping provider must honor a stable request key:

```http
POST /shipments
Idempotency-Key: shipment-order-123
Content-Type: application/json
```

If the provider creates the shipment but the response is lost, retry with the same identity or query by a stable external reference. Without provider idempotency or reliable lookup, exactly-once shipment creation cannot be guaranteed across an ambiguous network outcome.

### Identity distinctions

| Identity | Meaning |
|---|---|
| `eventId` | One event envelope |
| `paymentOperationId` | One logical payment |
| `orderId` | One order |
| `reservationId` | One stock allocation |
| `fulfillmentId` | One logical fulfillment |
| `shippingRequestId` | One provider shipment operation |

Kafka transactions can atomically couple supported Kafka output records and consumed offsets. They do not automatically include an arbitrary PostgreSQL commit or external payment/shipping call.

### Interview-ready answer

> I design for effectively-once business effects with at-least-once delivery. Each consumer commits its inbox record, business transition, and outbox together. Unique business constraints protect against different events describing the same operation. Kafka offsets are acknowledged after the database commit. Downstream services enforce their own idempotency, and external providers receive stable operation keys. Kafka exactly-once features do not make all database and provider effects one distributed atomic transaction.

## 9. Distributed rate limiting and quota allocation

**Interview question:** There are 100 Spring Boot instances and a global admission budget of 5,000 requests/second. Giving each instance 50 RPS wastes capacity when traffic is uneven. How do you redistribute capacity without a Redis call for every request?

### Recommended architecture

```text
Global quota coordinator: bounded allocation per defined window
  ├─ Instance A: demand-based batch of permits
  ├─ Instance B: demand-based batch of permits
  └─ Instance N: demand-based batch of permits

Each instance atomically consumes its own valid local permits.
```

The issuance invariant is:

```text
sum(allocated permits for window W) <= 5,000
```

Instances request additional batches as needed. The coordinator rejects allocations when the global budget is exhausted. A request consumes local quota, reducing coordinator traffic.

Static `5,000 / 100 = 50` RPS per instance is safe only under its defined rate semantics, but inefficient: an instance with 200 RPS rejects requests while quieter instances leave quota unused.

### Lua teaching example and its limitations

The conversation uses this simplified fixed-window allocation script:

```lua
-- KEYS[1]: global quota key
-- ARGV[1]: requested permits
-- ARGV[2]: global limit
-- ARGV[3]: window TTL in milliseconds
local requested = tonumber(ARGV[1])
local globalLimit = tonumber(ARGV[2])
local remaining = tonumber(redis.call("GET", KEYS[1]))

if remaining == nil then
    remaining = globalLimit
end

local granted = math.min(requested, remaining)
if granted <= 0 then
    return 0
end

redis.call("SET", KEYS[1], remaining - granted, "PX", ARGV[3])
return granted
```

**Do not treat this as a production limiter.** Every successful allocation resets the TTL, potentially extending the effective window. The script also lacks a stable window identity and cannot distinguish safe initialization from missing state after a failure. It illustrates batch allocation, not a complete correctness protocol.

A production design needs:

- A coordinator/server-defined window ID.
- A bounded atomic allocation counter for that exact window.
- Expiry tied to the window, rather than sliding on each allocation.
- A unique allocation/grant ID and idempotent grant retries.
- Grant size, owner, validity interval, and expiry information.
- Local atomic consumption that cannot exceed the grant size.
- Conservative handling of network delay and clock uncertainty.
- Durable issuance if the quota is a hard failover-safe guarantee.

### Crashes and unused permits

If an instance receives 200 permits, consumes 50, and crashes, the simplest safe policy is to let the remaining 150 expire. Do not immediately redistribute them merely because a health check or lease timed out: the old holder might still be running.

Reclamation improves utilization but requires proof of non-consumption and a mechanism that rejects stale consumption. For the conversation's small-team/six-week constraint, non-reclamation during a short window is the preferred initial design.

### Clock and rate semantics

Use coordinator time, such as Redis server time for the advisory design, to define the window. Instances must stop consuming a grant before its authoritative expiry, with a conservative margin for uncertainty.

**A fixed-window limit is not a rolling-second limit.** Five thousand permits near the end of one calendar second and another 5,000 near the beginning of the next can admit 10,000 requests within a rolling second. Clarify whether the business wants:

- A fixed-window cap.
- A strict rolling-window cap.
- An average rate with bounded bursts.

These require different algorithms or more conservative policies. Merely changing a TTL does not make the guarantee equivalent.

### Failure policy and monitoring

If the coordinator is unavailable, stop issuing new grants. Existing permits may be used only while their validity and accounting remain safe under the chosen protocol. Monitor issued/used/wasted permits, rejection rate, allocator latency, grant retries, clock margins, and uneven utilization.

### Interview-ready answer

> I would allocate small batches of permits centrally and enforce them locally. The coordinator issues at most 5,000 permits for each explicitly defined window, while instances request batches according to demand. This avoids static per-instance limits and a Redis call on every request. Each grant has an identity, owner, window, and expiry. I would initially let unused permits expire after a crash rather than reclaim them unsafely. I would use coordinator-defined time and clarify fixed-window versus rolling-window semantics. If the cap must survive failover strictly, issuance must use a durable, strongly consistent ledger rather than asynchronous Redis replication alone.

## 10. Quota failover and durable grants

**Interview questions:** Redis primary A issues 3,000 permits before replication, then a stale replica issues another 5,000 in the same window. Or, for a finite stock admission budget, 80 permits were issued but the replica remembers only 50 and issues another 50. How do you prevent double issuance?

These produce 8,000 against a 5,000 window budget, or 130 against a 100-permit sale budget. Atomic Lua execution on each primary does not preserve the cross-failover invariant.

| Mechanism | What it provides | Why it is insufficient alone |
|---|---|---|
| Redis Lua | Atomic execution on one primary | Acknowledged issuance may be lost at failover |
| Redis `WAIT` | Replica acknowledgment improvement | Not a complete consensus/promotion guarantee |
| Redis distributed lock | Mutual exclusion under its assumptions | Lock state can also be lost; stale holders may continue |
| Fencing epoch | Rejectable stale authority | Only works if a trusted boundary enforces it |
| PostgreSQL transaction | Atomic durable ledger operation | Deployment must preserve acknowledged commits during failover |
| Consensus-backed allocator | Coordinated durable ownership | Latency, quorum, and operational complexity |

### Durable finite-quota schema

```sql
CREATE TABLE sale_quota (
    sale_id UUID PRIMARY KEY,
    total_quota INT NOT NULL CHECK (total_quota >= 0),
    issued_quota INT NOT NULL DEFAULT 0,
    CHECK (issued_quota >= 0 AND issued_quota <= total_quota)
);

CREATE TABLE quota_grants (
    grant_id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sale_quota(sale_id),
    owner_id VARCHAR(100) NOT NULL,
    grant_size INT NOT NULL CHECK (grant_size > 0),
    fencing_epoch BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Allocation conceptually combines these operations:

```sql
BEGIN;
-- First establish idempotent ownership of the grant request.
-- Return the existing matching grant on a retry; do not issue again.

UPDATE sale_quota
SET issued_quota = issued_quota + :requested
WHERE sale_id = :saleId
  AND issued_quota + :requested <= total_quota
RETURNING issued_quota;

-- Only after a successful allocation, within the same transaction:
INSERT INTO quota_grants
    (grant_id, sale_id, owner_id, grant_size, fencing_epoch, status)
VALUES
    (:grantId, :saleId, :ownerId, :requested, :epoch, 'ACTIVE');
COMMIT;
```

This is a transaction sketch, not a full retry-safe procedure. Grant-ID uniqueness and request ownership must prevent a repeated request from decrementing the budget twice. Any failed insert must roll back the allocation. Return the grant only after commit.

For a time-window budget, the analogous conditional update is:

```sql
UPDATE quota_window
SET allocated = allocated + :requested
WHERE product_id = :productId
  AND window_id = :windowId
  AND allocated + :requested <= 5000
RETURNING allocated;
```

Initialize the window row safely. A zero-row result may mean a missing row, an exhausted budget, or a batch larger than the remaining quota; handle these deliberately.

### Already issued means already accounted for

If 3,000 permits were durably granted, a replacement coordinator may issue only another 2,000 for that window. Treat issued grants as spent for issuance accounting even if a holder never uses them. Do not reset from a stale Redis counter.

A PostgreSQL primary with asynchronous failover can lose acknowledged ledger commits too. Strict claims require suitable synchronous durability and promotion policy, or a consensus-backed system.

### Fencing and redemption

Two distinct invariants are needed:

```text
sum(issued grants) <= quota
consumption of each grant <= grant size
```

An epoch such as 41 → 42 is useful only if the authoritative validator rejects stale authority. An epoch field ignored by the inventory service provides no protection.

For reclaimable finite permits, give each permit a stable ID and redeem it atomically/durably. Reclamation must revoke old redemption rights, determine which permits were already used, and reassign only proven-unused capacity. A local counter alone cannot provide that proof.

After Redis failover, pause issuance from unverified cache state, reconstruct grants from the durable ledger, invalidate stale cached authority, and resume with the actual remaining quota.

### Admission is not stock ownership

Issuing 130 admission permits does not necessarily oversell 100 units if PostgreSQL still allows only 100 authoritative reservations. It does violate a strict admission promise and creates rejected admitted customers. If a permit guarantees an item, its semantics must be much stronger than permission to attempt a reservation.

### Interview-ready answer

> I separate durable quota ownership from fast consumption. A strongly consistent allocator commits a unique bounded grant before returning it, and grant retries are idempotent. Redis or application memory can cache and consume valid grants, but failover recovery reconstructs state from the durable ledger. I count all issued permits against the budget and avoid immediate reclamation after a crash. Reclaimable grants require enforced fencing and durable redemption. If authoritative allocation is unavailable, I stop issuing new grants. PostgreSQL inventory allocation remains the final no-overselling guard even if advisory admission fails.

## 11. End-to-end purchase and payment idempotency

**Interview question:** An HTTP request times out and is retried three times, Kafka redelivers the reservation event, and the provider duplicates its callback. How do you obtain one reservation, one order, and at most one logical charge?

### HTTP boundary

```http
POST /api/flash-sales/101/purchase
Idempotency-Key: 2f1c5b8e-...
Authorization: Bearer <token>
```

```sql
CREATE TABLE purchase_requests (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reservation_id UUID,
    UNIQUE (user_id, idempotency_key)
);
```

| Request | Behavior |
|---|---|
| New key | Atomically claim the logical attempt |
| Same key, same payload | Return stored result or current status |
| Same key, different payload | Reject, for example with 409 Conflict |
| Existing request still running | Return processing state, for example 202 plus status endpoint |

The same logical operation must reuse its key. Enforce the purchase-limit business rule separately: a customer can generate several different idempotency keys. A lookup followed by insert is not sufficient concurrency protection.

### Payment boundary

Persist a stable `paymentOperationId` and provider idempotency key. Every retry of the same logical charge reuses that identity. A local unique constraint cannot prevent two external charges if the provider ignores the key.

```sql
CREATE TABLE payment_operations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    provider_payment_id VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Coordinate concurrent execution and make find-or-create atomic. Verify the provider's retention and retry semantics for the required idempotency window. A timeout is an unknown outcome, not proof of failure. Reconcile using the original identity; do not start a new logical charge because a response was lost.

### Interview-ready answer

> I enforce idempotency independently at HTTP, reservation, order, event-consumer, and provider boundaries. The HTTP key is bound to the user and request fingerprint. Database constraints and transactions protect the reservation and one order per reservation. Inbox/outbox processing tolerates Kafka redelivery. All retries of the same payment reuse a durable operation ID and provider key. Unknown outcomes are reconciled before retry or compensation. The objective is one business effect, not an impossible assumption that every network request executes once.

## 12. Transactional outbox at extreme throughput

**Interview question:** At 100,000 business events/second, a polling outbox grows to hundreds of millions of rows, `FOR UPDATE SKIP LOCKED` becomes expensive, and per-order ordering must survive. How would you redesign it?

### Scale and architecture

```text
100,000 events/s = 360 million/hour = 8.64 billion/day

Business transaction → PostgreSQL business data + append-only outbox
  → WAL / logical replication → Debezium + Outbox Event Router
  → Kafka keyed by aggregate ID → idempotent consumers
```

Polling repeatedly scans and locks rows, then often updates a publication-status field. This adds write amplification, index churn, dead tuples, and vacuum work. CDC removes the application polling and per-event published-state update, but not the database cost of writing 100,000 event rows/second.

```sql
CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Write an outbox event only when the business transition succeeds, and commit them together. Do not use this anti-pattern as if it were atomic across systems:

```java
@Transactional
public void reserve() {
    reservationRepository.save(/* ... */);
    kafkaTemplate.send("reservation-created", event);
}
```

### Per-order ordering

```java
ProducerRecord<String, String> record = new ProducerRecord<>(
    "order-events", event.orderId().toString(), event.payload()
);
```

Same-key partitioning is necessary but insufficient:

- Serialize changes at the authoritative aggregate owner with a row lock or version check.
- Preserve relevant source order through CDC and publication.
- Keep topic/key/partition assignment stable; partition-count changes can remap keys.
- Avoid consumer concurrency that reorders one partition's effects.
- Include aggregate versions and detect duplicates, stale events, and gaps.
- Separate databases/connectors cannot gain a global causal order simply by using the same Kafka key.

```sql
UPDATE order_projection
SET status = :newStatus, version = :incomingVersion
WHERE order_id = :orderId
  AND version = :incomingVersion - 1;
```

If version 3 arrives before version 2, defer, recover, or apply the defined gap-handling protocol rather than blindly overwriting state.

### Partitioning and retention

```sql
CREATE TABLE outbox_events (
    event_id UUID NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_version BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
) PARTITION BY RANGE (created_at);
```

For this PostgreSQL partitioning scheme, a parent primary/unique key must include the partition key. Do not copy the unpartitioned `event_id` primary key and assume it enforces uniqueness across all time partitions. Use an appropriate composite key and a separate durable identity mechanism if global uniqueness enforcement is required.

An hourly partition would contain 360 million events at the stated rate. Choose intervals based on measured row/index size and maintenance costs. Test partition publication/decoding and cleanup with the selected PostgreSQL/Debezium configuration.

Retention must account for verified CDC progress, durable Kafka delivery, replay needs, and recovery checkpoints. A time-only deletion policy can destroy the fallback event source while CDC lags.

### Interview-ready answer

> I would replace polling with an append-only transactional outbox captured through PostgreSQL WAL and Debezium. I would partition and retain outbox data according to verified delivery and recovery requirements. Events are keyed by order ID, but source serialization, aggregate versions, and consumer processing discipline also preserve ordering. Consumers remain idempotent because restart and checkpoint failures can redeliver events. I would benchmark the database, WAL, connector, brokers, and consumers; CDC removes polling overhead, not every throughput bottleneck.

## 13. Debezium outage, WAL retention, and recovery

**Interview question:** Debezium is down for 45 minutes while PostgreSQL processes 100,000 transactions/second. Disk usage reaches 95%; Kafka is healthy; the business requires zero lost events. What do you do?

### Quantify the incident

```text
100,000 × 45 × 60 = 270 million transactions
```

If each creates one outbox event, that is roughly 270 million pending events. It is not the number of WAL records or a WAL-byte estimate.

Logical replication slots retain the WAL required for the consumer. `restart_lsn` is the earliest position potentially required; `confirmed_flush_lsn` records acknowledged logical-consumer progress.

```sql
SELECT slot_name, active, restart_lsn, confirmed_flush_lsn,
       pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)
           AS retained_wal_bytes
FROM pg_replication_slots
WHERE slot_name = 'debezium_outbox_slot';
```

The LSN distance is useful but is not a direct physical disk-usage measurement. Inspect the storage volume and slot status too. Fields such as `wal_status`, `safe_wal_size`, and `invalidation_reason` depend on the deployed PostgreSQL version; adapt diagnostic queries accordingly.

### Recovery sequence

1. Reduce new event-producing writes: throttle flash-sale admission and pause noncritical jobs.
2. Safely expand storage if supported, buying recovery headroom.
3. Preserve the slot, WAL, offsets, and replayable outbox data.
4. Diagnose connector configuration, permissions, network, publication/schema changes, Kafka Connect workers, offsets, conflicts, and resource exhaustion.
5. If the slot is valid and required WAL exists, repair Debezium and resume from its durable position.
6. Verify delivery, per-aggregate ordering, idempotent replay, and stuck business workflows.
7. Drain the backlog with sustained capacity above new generation before reopening normal admission.

At an illustrative 150,000 events/second capture capacity and 100,000 new events/second, net drain is 50,000/second. Draining 270 million takes about 5,400 seconds, or 90 minutes. Real recovery depends on comparable measured rates, event sizes, WAL decoding, and downstream capacity.

At an illustrative 200 MB/second WAL retention rate, 30 minutes accumulates about 360 GB in decimal units. This is a capacity illustration, not a measured rate for the scenario.

### Invalid slot or missing WAL

A new slot does not reconstruct old missing events. Recovery requires a complete independent source: retained outbox rows, a durable event archive, or a verified backup/replay procedure.

```sql
-- Illustrative selection only; timestamp bounds do not prove completeness.
SELECT event_id, aggregate_id, aggregate_version, event_type, payload
FROM outbox_events
WHERE created_at >= :recoveryStart AND created_at < :recoveryEnd
ORDER BY created_at, event_id;
```

`created_at` is not PostgreSQL commit order, and timestamp boundaries alone can leave gaps. Use verified checkpoints/watermarks, stable event IDs, overlap that tolerates duplicates, and aggregate-version validation. If both WAL and complete replay data are gone, zero-loss recovery may be impossible.

For stuck orders and payments, inspect durable business state and query the provider before replaying uncertain charges. Replaying an event must not create a new payment identity.

### Actions to avoid

- Dropping or advancing the slot merely to free disk, without a proven recovery path.
- Manually deleting WAL files.
- Resetting connector offsets without accounting for gaps or duplicates.
- Deleting old outbox partitions while recovery may need them.
- Repeated database restarts as a substitute for capacity and connector diagnosis.
- Assuming storage expansion alone fixes a continuously growing backlog.

Dropping a slot does not necessarily erase WAL immediately; it removes its retention requirement and may allow recycling. `max_slot_wal_keep_size` can bound retention at the cost of invalidating the slot; it is not lossless backpressure.

Useful prevention metrics include retained WAL, slot lag, CDC catch-up capacity, free disk, time to disk exhaustion, outbox retention safety, connector errors, consumer lag, and the age of uncertain payments.

### Interview-ready answer

> I would protect PostgreSQL while preserving committed events. I would immediately reduce new writes, add safe storage headroom, and diagnose the connector without dropping or advancing its slot. If the slot and WAL are valid, resume from the stored position and keep backpressure until capture outpaces generation. If WAL is missing, recover through a complete retained outbox or verified replay source with stable IDs and gap detection. I would reconcile uncertain payments before replaying financial work. If zero loss and uninterrupted writes become incompatible, I would preserve committed data and temporarily restrict new writes.

## 14. Zero-downtime schema migration

**Interview questions:** Add mandatory `payment_status` to a 200-million-row orders table receiving 20,000 writes/second. A later challenge replaces `customer_id VARCHAR(50)` with `customer_uuid UUID NOT NULL` under mixed application versions.

### Expand–Migrate–Contract

1. **Expand:** add the new representation in a backward-compatible, initially nullable form.
2. **Compatibility deployment:** readers tolerate missing values; writers populate the new field while preserving old contracts.
3. **Migrate:** backfill in small, resumable batches using authoritative business data.
4. **Verify:** measure completeness and correctness; build needed indexes with appropriate concurrent procedures.
5. **Enforce:** validate constraints only after data and all active writers comply.
6. **Contract:** remove old compatibility paths only after the rollback window closes.

```sql
ALTER TABLE orders ADD COLUMN payment_status VARCHAR(30);

-- After a correct backfill and compatible writers:
ALTER TABLE orders
ADD CONSTRAINT orders_payment_status_present
CHECK (payment_status IS NOT NULL) NOT VALID;

ALTER TABLE orders
VALIDATE CONSTRAINT orders_payment_status_present;

-- Schedule the final metadata/constraint step with bounded lock waiting:
ALTER TABLE orders ALTER COLUMN payment_status SET NOT NULL;
```

Even a metadata-oriented DDL operation needs locks. Use a tested deployment procedure with short lock timeouts and retry policy. Exact lock and scan behavior depends on PostgreSQL version and migration details.

Modern PostgreSQL can add certain constant-default columns without rewriting every row, but that does not make any migration lock-free or make a default semantically correct. Setting every historical payment to `PENDING` can corrupt meaning.

### Backfill and indexing

- Use bounded batches, stable key ranges/checkpoints, short transactions, and restartability.
- Derive status from authoritative payment records; unresolved data needs an explicit review path.
- Avoid overwriting newer application writes. Use predicates/version checks and verify races.
- Monitor database CPU/I/O, replication lag, WAL, locks, and latency; throttle or pause as needed.
- Build appropriate indexes concurrently where supported; such operations have their own failure cleanup and transaction restrictions.
- Keep old fields/read paths available during rollback rather than dropping them in the same deployment.

For the customer-reference replacement, establish a validated mapping/conversion, dual-write or use a carefully scoped compatibility mechanism, batch backfill, compare references, and switch reads before enforcing the new non-null contract. A random UUID is not a valid replacement for an existing customer reference. Trigger versus application logic is a trade-off between coverage of legacy writers and hidden database complexity; test both mixed-version behavior and rollback.

### Interview-ready answer

> I would separate schema expansion, compatible application deployment, data backfill, validation, and final cleanup. I would add the field without forcing an immediate incompatible constraint, backfill in small resumable batches from authoritative data, and throttle against production health. I would validate constraints and build indexes with a tested low-disruption procedure. Only after all writers comply and rollback is no longer needed would I enforce the final contract and remove old paths. No downtime does not mean no locks or no operational risk.

## 15. Mixed application versions and event compatibility

**Interview question:** Thirty percent of pods still run old code that does not write `payment_status`; new code requires it, and old/new Kafka consumers coexist. Traffic cannot pause. What do you do?

Missing status is not equivalent to pending payment. An old order may already be paid, refunded, or failed.

- Keep the schema nullable and new readers tolerant during the overlap.
- Resolve absent values from authoritative payment state; use explicit unresolved/review handling when necessary.
- Use defaults only where their business meaning is valid for every affected writer.
- Evolve events compatibly: additive fields, explicit optionality/default semantics, tolerant readers, and schema compatibility checks.
- Understand consumer groups: same-group members share partitions, while different groups each receive events. Do not accidentally run two competing owners of the same business action.
- Conditional transitions/version checks protect against stale writers; compatibility readers alone do not.
- Sequence producer/consumer changes so every deployed combination can read and process safely.
- Observe old-writer traffic, null counts, reconciliation outcomes, and consumer errors before tightening constraints.

### Interview-ready answer

> I would keep the compatibility window open until every active writer can honor the new contract. New readers must handle nulls without inventing payment state, and events must remain readable by old consumers. I would explicitly control consumer ownership and protect business state with conditional/versioned writes. After all versions and historical data are verified, I would enforce the new constraint and later remove fallback logic. The rollout is complete only when the mixed-version behavior is safe, not simply when new pods are healthy.

## 16. Database connection pool exhaustion

**Interview question:** One hundred pods each have HikariCP maximum pool size 30 and connection timeout 30,000 ms. PostgreSQL permits 1,000 connections. Latency rises from 100 ms to four seconds, pool timeouts occur, and CPU reaches 90%. Should the team raise the pool size to 100?

### Guided diagnosis

```text
100 pods × 30 connections = 3,000 potential application connections
100 pods × 100 connections = 10,000 potential application connections
Database configured maximum = 1,000, also shared with other clients
```

Potential maxima are not proof every connection is currently open, but they reveal an incompatible aggregate budget. Increasing pools can amplify contention and overload an already CPU-bound database.

Diagnose active/idle/pending connections, pool acquisition time, transaction duration, slow queries, locks, hot rows, connection leaks, and dependency calls inside transactions. Include other services, jobs, replicas' roles, and operational connections in the budget.

Bound total database concurrency, shorten transactions, optimize query/index behavior, remove external calls from transaction scopes, fix leaks, and apply upstream backpressure. A connection proxy can improve connection management, but does not create more database execution capacity. Scaling application pods can make the aggregate connection problem worse.

### Interview-ready answer

> I would not approve a blind increase. The current configured pools can already demand 3,000 connections against a 1,000-connection database, and larger pools could multiply contention while CPU is near saturation. I would measure pool wait time, active transactions, locks, slow SQL, transaction duration, and leaks. Then I would set an aggregate connection/concurrency budget, shorten and optimize database work, and control admission. More connections are useful only if measurements show spare database capacity rather than an execution bottleneck.

## 17. Payment retry storms and dependency protection

**Interview question:** Payment takes eight seconds, the caller times out at three seconds, retries multiply, HPA scales from 30 to 100 pods, and the dependency returns 503. How do you restore availability without duplicate charges?

At 10,000 original requests/second and three additional attempts each, the dependency may see 40,000 attempts/second, before retries at gateways, meshes, and clients.

### Response

1. Stop aggressive retries and eliminate independent retries at multiple layers.
2. Bound new payment initiation while reserving capacity for callbacks, status queries, and reconciliation.
3. Use bulkheads to limit in-flight calls and circuit breakers to avoid repeated calls to a failing dependency.
4. Apply an end-to-end deadline, bounded retry budget, exponential backoff, and jitter.
5. Reuse the stable provider key for permitted retries.
6. Persist unknown outcomes and reconcile before a new logical payment or compensation.

```text
PAYMENT_PROCESSING
  ├─ verified success → PAYMENT_SUCCEEDED
  ├─ verified failure → PAYMENT_FAILED
  └─ timeout → PAYMENT_UNKNOWN → reconciliation
```

```java
@Service
public class PaymentGatewayClient {
    @CircuitBreaker(name = "paymentGateway")
    @Bulkhead(name = "paymentGateway")
    public PaymentResponse charge(PaymentRequest request) {
        return paymentHttpClient.charge(request);
    }
}
```

The conversation's illustrative Resilience4j starting configuration is:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        slidingWindowSize: 50
        minimumNumberOfCalls: 20
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  bulkhead:
    instances:
      paymentGateway:
        maxConcurrentCalls: 10
        maxWaitDuration: 0
```

Tune against measured workload. Proxy boundaries, annotation order, retries, and transaction propagation matter; do not blindly stack annotations or keep a database transaction open during a slow provider call.

| Failure | Handling |
|---|---|
| Validation error | Do not retry |
| Authentication/authorization failure | Do not retry blindly |
| 429 | Respect retry guidance where appropriate |
| 503 | Retry only under budget and safe operation semantics |
| Timeout after possible send | Unknown outcome; reconcile and preserve identity |
| Confirmed decline | Do not automatically create repeated charges |

A circuit breaker protects dependency load; it does not deduplicate charges. Increasing timeout alone can retain resources longer without fixing overload. More pods can increase pressure on the same downstream limit.

### Interview-ready answer

> I would first stop retry amplification and bound concurrent payment calls. Circuit breakers and bulkheads protect the dependency, while deadlines and retry budgets prevent unbounded work. A timeout means the outcome is unknown, so I persist that state and query the provider with the original payment identity. All permitted retries reuse the same provider key. If payment succeeded after reservation cancellation, I initiate idempotent compensation. I would monitor retry amplification, unknown-payment age, provider latency, and duplicate-charge indicators.

## 18. One million attempts per second for 100 units

**Interview question:** How do you preserve stock correctness without making PostgreSQL or a Redis hot key process every request?

At one million one-unit attempts for 100 initial units, at most 0.01% obtain an initial reservation. Sending all attempts into Kafka or PostgreSQL is unnecessary work.

```text
1,000,000 attempts/s
  → edge filtering / WAF / waiting room
  → per-user deduplication and distributed admission
  → small, bounded set of durable reservation commands
  → Inventory Service
  → atomic PostgreSQL allocation + reservation + inbox/outbox
```

Batch permits reduce central hot-key checks. Keep batches small for a tiny finite stock pool to avoid stranding most capacity on a failed gateway.

| Admission choice | Benefit | Trade-off |
|---|---|---|
| At most 100 permits for 100 units | Minimal downstream contention | Abandonment/crashes can strand stock |
| Controlled oversubscription, e.g. 150 attempts | Better use of stock when some attempts fail | Some admitted users are rejected by inventory allocation |

These figures illustrate strategy, not measured limits. A permit means permission to attempt a reservation unless the business explicitly promises stock ownership. That promise requires a stronger allocation protocol.

Before returning an accepted/pending response, durably record the work through command ingestion or an outbox. A Redis check followed by an unrecorded Kafka send has the same crash gap discussed earlier.

Fail closed for new admission when its correctness cannot be established, while continuing safe existing reservations, callbacks, and reconciliation. Reopen admission after eligible stock release in a controlled, idempotent way.

### Interview-ready answer

> I would optimize for successful business operations rather than raw reservation attempts. Edge controls and a waiting room absorb demand, batch grants avoid a central check for every request, and only a bounded admitted set enters durable processing. PostgreSQL remains the inventory authority with conditional allocation and unique reservations. I would choose strict permits or controlled oversubscription according to the customer promise, and I would never describe admission as confirmed stock unless that guarantee is actually enforced.

## 19. Multi-region inventory ownership

**Interview question:** Singapore and Tokyo each receive 500,000 requests/second. There are only 100 units globally, and the regions may be partitioned for minutes. How can both remain useful without overselling?

### Recommended design: exclusive regional quotas

```text
Strongly consistent global ownership allocator
  ├─ Singapore: 60 exclusive units → local admission + inventory DB
  └─ Tokyo:     40 exclusive units → local admission + inventory DB
```

The 60/40 split is an example based on expected demand, not a fixed rule. This is an escrow/bounded-rights approach: regions spend already-owned inventory rights locally.

```text
unallocated rights + sum(exclusive regional rights) = original stock

Within each region:
available + reserved + consumed + frozen-for-transfer
  = rights accounted for by that region at that protocol stage
```

Transfers change ownership, so the ledger and local accounting must define each transition without counting the same right twice. Every unit is accounted for exactly once across the ownership/state model.

Local reservation uses the familiar atomic transaction:

```sql
UPDATE regional_inventory
SET available_quantity = available_quantity - 1
WHERE sale_id = :saleId AND available_quantity >= 1
RETURNING available_quantity;
```

Commit the unique reservation and outbox with the allocation. Each region can continue within its remaining owned quota during a partition.

### Availability trade-off

If Singapore exhausts its 60 while Tokyo still has 20, Singapore must reject or queue until a safe transfer is possible. It cannot borrow the other region's rights unilaterally. Global stock may remain available while a region is locally sold out.

Global consensus on every purchase would simplify global ordering at cross-region latency and partition-availability cost. Independent writable inventory copies without exclusive rights are unsafe.

### Durable quota transfer

```text
REQUESTED
  → SOURCE_FROZEN
  → OWNERSHIP_COMMITTED
  → DESTINATION_ACTIVATED
  → COMPLETED
```

1. Source durably freezes the units, making them unavailable for local sale.
2. Global allocator commits the ownership change using a unique transfer ID/epoch.
3. Destination activates the transfer idempotently.
4. Recovery resumes incomplete transitions from durable state.

The source must lose the allocation right before the destination gains it. Temporarily unavailable units on both sides are safer than dual ownership. Duplicate delivery must not add quota twice.

Use a strongly consistent ownership ledger, not ordinary asynchronous writable cross-region replication. A third independent failure domain can participate in quorum: three voters need two votes. It does not make both sides of every partition capable of global writes. Consensus is for infrequent grants/transfers; regional purchases spend already-owned rights locally.

At 500,000 RPS per region, retain edge filtering, waiting rooms, local admission, bounded Kafka work, and authoritative local allocation. The product still has only 100 units globally; most traffic should not become database transactions.

### Interview-ready answer

> I would preallocate exclusive inventory rights to each region, for example 60 units to Singapore and 40 to Tokyo. Each region reserves from its own quota with local atomic transactions, allowing useful operation during a partition. A strongly consistent global ledger governs grants and transfers. Transfers first freeze source rights, then commit ownership, then activate the destination idempotently. A region that runs out cannot borrow unreachable stock. This accepts stranded capacity in exchange for low-latency local operation without global overselling.

## 20. Regional disaster recovery and split brain

**Interview question:** Singapore is unreachable from the coordinator but its pods and PostgreSQL may still be selling stock. Tokyo is sold out. Can Tokyo immediately take over Singapore's remaining inventory?

### Why not

Unreachable does not mean stopped. The last reported available count may also be stale. Transferring guessed remaining stock can create two active owners.

A lock expiring after 30 seconds proves only that the lease is no longer valid to the coordinator. It does not prove the old application or its isolated database stopped writing.

An epoch increment from 41 to 42 in the global database is insufficient if the old region can write to its local inventory database without checking enforceable authority. Traffic failover is not the same as safe stock-ownership failover.

### Safe choices

- Source durably freezes and relinquishes rights before transfer.
- An authoritative allocation/write boundary fences the old region so revoked rights cannot be spent.
- Otherwise leave those rights unavailable until safe ownership and remaining quantity can be established.

A failover runbook should identify the authoritative ownership ledger, verify the old writer is fenced or relinquished, recover/reconcile committed reservations, determine transferable stock from durable state, commit the transfer, activate the destination idempotently, and explicitly govern rejoining stale regions.

Quorum protects global ownership decisions only where the protocol enforces its authority. It cannot magically prevent independent writes to an isolated local store.

### Interview-ready answer

> I would not reallocate Singapore's inventory just because a health check fails. I must establish both the actual remaining rights and that Singapore can no longer spend them. A lease or lock timeout alone does not prove either. I would use enforced write fencing or a durable source relinquishment before committing the new ownership. If neither can be guaranteed, I would preserve correctness by leaving that quota unavailable. On rejoin, Singapore must reconcile and obtain current authority before serving inventory writes.

## 21. Interview answer and revision checklist

### Two-minute architecture answer

> I would clarify purchase limits, payment deadlines, fairness, and whether asynchronous confirmation is acceptable. I would distinguish concurrent users from peak RPS and initial acknowledgment from completed checkout.
>
> For the flash sale, I would reject unnecessary work at the edge and use bounded admission before expensive inventory operations. Redis helps with speed, but PostgreSQL is the final inventory authority. A confirmed reservation commits a conditional stock allocation, unique reservation, and transactional outbox together. Kafka decouples order processing, and consumers commit inbox deduplication with their business changes.
>
> Every retry reuses the logical purchase or payment identity. Database constraints protect business uniqueness; provider idempotency and reconciliation handle uncertain external outcomes. Payment confirmation and reservation expiry compete through conditional state transitions, and stock is released only by the successful reservation-release transition.
>
> For distributed admission, I would issue demand-based batches from a bounded allocator and consume them locally. Hard quotas require durable grant records and safe failover. I would not reclaim an unreachable holder's permits without preventing stale consumption, and I would distinguish fixed windows from rolling-rate guarantees.
>
> At higher throughput, I would use CDC for outbox transport, preserve per-aggregate ordering with versions, and monitor WAL retention and catch-up capacity. In multiple regions, exclusive local inventory rights permit local progress, while strongly consistent ownership transfers and enforced fencing prevent double allocation.
>
> The central trade-off is correctness versus utilization and availability. I would accept bounded rejection, delayed confirmation, or temporarily stranded quota before selling the same inventory twice.

### Failure-boundary checklist

| Boundary | Required protection |
|---|---|
| HTTP retry | Stable user-scoped key + request fingerprint + stored result |
| Duplicate purchase under different keys | Durable business purchase-limit constraint |
| Stock allocation | Conditional update + reservation in same transaction |
| Database → Kafka | Outbox / durable publication intent |
| Kafka → database | Transactional inbox + business uniqueness |
| Payment/expiry race | Conditional state transition + idempotent stock release |
| External payment/shipping | Stable provider identity, idempotency, lookup/reconciliation |
| Local grant use | Atomic consumption bounded by grant size |
| Coordinator retry | Stable grant ID + durable idempotent issuance |
| Quota failover | Durable committed ledger + safe promotion policy |
| Stale grant owner | Fencing enforced at an authoritative boundary |
| Permit reclamation | Revoke old use, establish consumed set, reassign only proven unused rights |
| Multi-region ownership | Exclusive quotas + freeze-before-transfer protocol |
| CDC recovery | Preserve WAL or complete replay source; tolerate duplicates, detect gaps |
| Schema rollout | Mixed-version compatibility before constraints/cleanup |
| Overload | Admission, bounded concurrency/queues, deadlines, retry budgets |

### Technical caveats to keep in every answer

- Atomicity on one Redis primary is not durable failover consistency.
- An outbox does not make an earlier Redis change part of the database transaction.
- `@Transactional` is not a complete concurrency or distributed-transaction solution.
- Event identity and business-operation identity are different.
- Kafka exactly-once semantics do not automatically include PostgreSQL or provider APIs.
- A timeout is an unknown outcome, especially for payments.
- A TTL, lock expiration, or failed health check is not proof an old owner stopped.
- Fencing works only if a trusted boundary rejects stale authority.
- Fixed-window and rolling-window RPS guarantees differ.
- Admission permits are not inventory ownership unless explicitly enforced as such.
- More pods, connections, retries, or queue capacity can worsen a shared bottleneck.
- CDC reduces polling; it does not eliminate database writes, ordering design, or recovery work.
- A new replication slot cannot restore changes from missing WAL.
- Timestamp-based replay alone does not prove event completeness or commit order.
- Strong SQL allocation still depends on the database's actual durability and failover policy.
- Zero downtime does not mean lock-free DDL or automatically correct historical defaults.

### What to test

Use representative load tests and targeted failure injection: concurrent identical requests, different event IDs for the same operation, crashes before/after commits and acknowledgments, lost provider responses, delayed callbacks, duplicate release, grant-response loss, stale Redis promotion, coordinator partitions, old-holder resumption, expired grant use, CDC lag/slot loss, partition cleanup, and mixed-version deployments. Verify business invariants and recovery completeness, not only HTTP latency.
