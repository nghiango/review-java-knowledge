# Design Patterns in Production: Incidents & Diagnostics

Production incident post-mortems, telemetry, memory leak diagnostics, and engineering checklists for pattern implementations.

---

## 1. Incident Post-Mortems

### Incident 1: The Cache-Leaked Confidential Document Outage
- **Severity**: P1 Security Incident
- **Impact**: High-net-worth customer portfolio data was accessible to unauthenticated guest accounts for 4 hours following a deployment.
- **Root Cause**:
  A developer added a Redis/Caffeine caching decorator around the document query repository. They composed the beans in the configuration class as:
  $$\text{Client} \longrightarrow \text{CachingDecorator} \longrightarrow \text{AuthorizingDecorator} \longrightarrow \text{DocRepository}$$
  When an account executive opened a confidential document, the serialized payload was stored in cache under key `doc-9942`. When a guest user guessed or obtained the document URL, the caching decorator served the cached payload directly without invoking the inner authorization check.
- **Remediation**:
  1. Immediately flushed all caches and inverted decorator ordering:
     $$\text{Client} \longrightarrow \text{AuthorizingDecorator} \longrightarrow \text{CachingDecorator} \longrightarrow \text{DocRepository}$$
  2. Partitioned cache keys by user identity / role: `(userId, docId)`.
  3. Added an automated security integration test preventing deployment if an unauthorized user receives data from a pre-warmed cache.

---

### Incident 2: The Black Friday Monolithic Switch Outage
- **Severity**: P1 Availability Outage
- **Impact**: 100% of European credit card transactions failed for 90 minutes during Black Friday peak traffic.
- **Root Cause**:
  To support a new regional Buy-Now-Pay-Later payment method (`KLARNA`), an engineer added a new `case KLARNA:` to a 1,500-line `PaymentProcessor` class. A copy-paste error in the fee calculation block accidentally modified the shared variable used by the credit card authorization branch, causing all credit card charges to fail validation.
- **Remediation**:
  1. Rolled back the release immediately to restore credit card revenue.
  2. Refactored the monolithic `PaymentProcessor` into the **Strategy Pattern**. Each payment rail was moved into an isolated class with dedicated unit tests.
  3. Codified an ArchUnit fitness rule banning switch statements over extensible business enums.

---

### Incident 3: The Lapsed Listener Memory Leak
- **Severity**: P2 Performance Degradation
- **Impact**: Production containers experienced hourly OutOfMemoryErrors (`java.lang.OutOfMemoryError: Java heap space`) despite low CPU load.
- **Root Cause**:
  A global singleton `MarketDataFeed` (Observer subject) allowed user session beans to register for real-time stock price updates via `feed.addListener(this)`. When user sessions expired and were destroyed by the servlet container, the listeners were **never deregistered**. The global singleton retained strong references to millions of dead session objects, preventing Garbage Collection.
- **Remediation**:
  1. Refactored listener registration to use `WeakReference<MarketDataListener>` so that dead sessions are collected automatically.
  2. Implemented an explicit `DisposableBean` / `SessionDestroyedEvent` listener to clean up observer subscriptions upon session termination.

---

## 2. Production Telemetry & Observability for Patterns

When deploying design patterns to production, monitor these operational signals:

| Pattern | Metric / Tag | Production Danger Signal |
|---|---|---|
| **Strategy** | `payment.strategy.duration{type="card"}` | Latency spike or error spike isolated to a single rail. |
| **Strategy Registry** | `strategy.registry.unresolved.count` | Non-zero value indicates client requesting an unmapped strategy. |
| **Decorator (Cache)** | `decorator.cache.hit_ratio` | Low hit ratio indicates bad key generation or cache thrashing. |
| **Chain of Responsibility** | `validation.chain.drop{handler="fraud"}` | Sudden surge indicates credential stuffing or bad client payloads. |
| **Observer** | `observer.active.subscribers` | Monotonically increasing count signals a lapsed listener memory leak! |

---

## 3. Production Readiness Checklist

Before approving any design pattern implementation for production:

- [ ] **Open/Closed Compliance**: Is the Strategy pattern backed by a Spring factory registry (`Map<Type, Strategy>`) rather than an expanding switch statement?
- [ ] **Decorator Security Order**: Is Authorization / Authentication wrapping the **outermost** layer of the decorator pipeline, preceding any caching layers?
- [ ] **Cache Key Partitioning**: If cached data is subject to access control, does the cache key incorporate tenant and role context (`tenant:role:resourceId`)?
- [ ] **YAGNI / KISS Verification**: Does this pattern solve real operational friction, or is it an over-engineered layer (Visitor/Bridge) for a trivial operation?
- [ ] **Observer Leak Prevention**: Are event listeners either managed by Spring lifecycle or stored via `WeakReference` to prevent lapsed listener memory leaks?
- [ ] **State Machine Fast-Failure**: Do invalid state transitions throw explicit, typed domain exceptions (`IllegalStateException`) rather than silently returning null?
- [ ] **Unit Test Isolation**: Can each Strategy and Handler be tested in under 5ms without needing a full Spring context or Docker container?
