# Solution: Decorator Order Bug

## Annotated code

```java
package lab.designpatterns.broken.decoratororder;

public class OrderQueryServiceFactory {

    // Security issue: Inverted decorator order; caching wraps authorizing decorator, allowing unauthorized users to retrieve cached sensitive data
    // Reliability issue: Unpredictable composition order; without explicit pipeline governance or Spring @Order, decorators can be accidentally reordered
    public static OrderQueryService createService() {
        OrderQueryService target = new DefaultOrderQueryService();
        OrderQueryService authorizing = new AuthorizingOrderQueryDecorator(target);
        
        // Security issue: Cache hits return OrderSummary directly without invoking AuthorizingOrderQueryDecorator!
        return new CachingOrderQueryDecorator(authorizing);
    }
}
```

And in `CachingOrderQueryDecorator.java`:

```java
package lab.designpatterns.broken.decoratororder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachingOrderQueryDecorator implements OrderQueryService {

    private final OrderQueryService delegate;
    // Reliability issue: Unbounded in-memory map without TTL or eviction leads to memory leak
    private final Map<String, OrderSummary> cache = new ConcurrentHashMap<>();

    public CachingOrderQueryDecorator(OrderQueryService delegate) {
        this.delegate = delegate;
    }

    @Override
    public OrderSummary getOrderSummary(String orderId) {
        // Security issue: Query result is cached globally across all users regardless of caller role or tenant isolation
        return cache.computeIfAbsent(orderId, delegate::getOrderSummary);
    }

    public void clear() {
        cache.clear();
    }
}
```

## Issue analysis

### 1. Critical Authorization Bypass via Cache Hit (Security)
- **Vulnerability**: 
  The factory composes decorators as:
  $$\text{Client} \longrightarrow \text{CachingDecorator} \longrightarrow \text{AuthorizingDecorator} \longrightarrow \text{DefaultService}$$
  1. An `ADMIN` queries `ORD-CONFIDENTIAL-99`. The cache misses, calls `AuthorizingDecorator` (which allows it because role is `ADMIN`), reads from `DefaultService`, and caches the result.
  2. A malicious user with `GUEST` or null role requests `ORD-CONFIDENTIAL-99`.
  3. `CachingDecorator` sees a cache hit for `ORD-CONFIDENTIAL-99` and returns the confidential order immediately!
  4. The inner `AuthorizingDecorator` is **never executed**!
- **Consequence**: Full privilege escalation and data breach of confidential orders.

### 2. Inverted Decorator Onion & Ordering Ambiguity (Reliability)
- **Problem**: When multiple cross-cutting concerns (authentication, caching, metrics, transactions, retries) are stacked using the Decorator or Proxy pattern, execution order is critical.
- **Rule of Thumb**:
  - **Outer layer**: Authentication / Authorization & Rate Limiting.
  - **Middle layer**: Metrics / Tracing & Retries.
  - **Inner layer**: Caching $\to$ Target Service.
- An authorization check must *always* execute before a cache lookup, or the cache key must explicitly incorporate the security identity/role.

---

## Correct implementation

The production solution enforces the correct decorator composition order:
- Package: `lab.designpatterns.decorator`
- Composition order:
  $$\text{Client} \longrightarrow \text{AuthorizingDecorator} \longrightarrow \text{CachingDecorator} \longrightarrow \text{TargetService}$$
  Every single request passes through authorization *first*. If unauthorized, a `SecurityException` is thrown before the cache is ever consulted.

---

## Trade-offs

| Decorator Order | Security | Cache Hit Rate |
|---|---|---|
| **Cache outside Auth (Broken)** | **Vulnerable**: Anyone can read cached secrets | Higher (no auth overhead on cache hit) |
| **Auth outside Cache (Correct)** | **Secure**: Auth check verified on 100% of requests | Slightly lower (must verify auth before cache hit) |
| **Role-Partitioned Cache Key** | **Secure**: Cache key contains `(userId, orderId)` | Lower hit rate across users, higher memory |
