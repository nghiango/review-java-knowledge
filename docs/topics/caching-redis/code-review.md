# Caching and Redis Code Review

Review each clean source before expanding its answer.

## Stale cache after entity mutation

A product catalog service caches product lookups in Redis using `@Cacheable`. The service also provides mutation operations to update prices and delete products.

```java
--8<-- "modules/13-caching-redis/broken-examples/stale-cache-after-update/Product.java"
```

```java
--8<-- "modules/13-caching-redis/broken-examples/stale-cache-after-update/ProductService.java"
```

Consider data consistency, cache synchronization on write operations, zombie reads after deletion, and `Optional` return value caching.

??? warning "Reveal issues"
    **Data consistency issue — Missing cache invalidation on update:** `updatePrice` updates the database record but omits `@CacheEvict` or `@CachePut`, leaving stale pricing in the cache until TTL expiration.

    **Data consistency issue — Zombie reads after entity deletion:** `deleteProduct` deletes the record from the database but fails to evict the cache, allowing deleted products to be retrieved as if they still exist.

    **Design issue — Caching Optional return types:** Caching `Optional<Product>` directly can fail during serialization or cache `Optional.empty` indefinitely without explicit sentinel handling.

[Correct implementation](solutions.md#stale-cache-after-entity-mutation)

---

## Cache stampede on expiry (thundering herd)

A gaming analytics service calculates category leaderboards. Recomputing the leaderboard is computationally expensive. Results are cached in Redis with a 10-minute TTL.

```java
--8<-- "modules/13-caching-redis/broken-examples/cache-stampede-on-expiry/LeaderboardEntry.java"
```

```java
--8<-- "modules/13-caching-redis/broken-examples/cache-stampede-on-expiry/HeavyAnalyticsRepository.java"
```

```java
--8<-- "modules/13-caching-redis/broken-examples/cache-stampede-on-expiry/LeaderboardService.java"
```

Consider concurrency bottlenecks, cache expiration handling under high load, and database exhaustion risks.

??? warning "Reveal issues"
    **Concurrency issue — Unprotected cache stampede (thundering herd):** When the key expires, all concurrent requests observe `cached == null` simultaneously. Hundreds of threads bypass the cache and run the expensive aggregation simultaneously, exhausting database connection pools and CPU.

    **Resilience issue — Lack of single-flight mutex coordination:** Missing distributed lock or probabilistic early expiration to ensure only one worker recomputes the resource while others await completion or read stale data.

    **Performance issue — Fixed round TTL without jitter:** Fixed 10-minute TTL across multiple categories causes all category keys to expire at the exact same moment, triggering periodic database load spikes.

[Correct implementation](solutions.md#cache-stampede-on-expiry-thundering-herd)

---

## Cache penetration on missing entities

A user profile service caches user profile lookups by user ID using `@Cacheable(value = "userProfiles", key = "#userId")`. When a requested user ID does not exist, the method returns `null`.

```java
--8<-- "modules/13-caching-redis/broken-examples/caching-null-and-exceptions/UserProfile.java"
```

```java
--8<-- "modules/13-caching-redis/broken-examples/caching-null-and-exceptions/UserProfileService.java"
```

Consider resilience vulnerabilities, denial of service risks, and cache penetration defects.

??? warning "Reveal issues"
    **Resilience issue — Cache penetration vulnerability:** When an entity does not exist, returning `null` without caching a sentinel object causes repeated requests for invalid IDs to bypass the cache and hit the database on every single query.

    **Performance issue — Database Denial of Service (DoS):** An attacker scanning non-existent IDs can overwhelm database connection pools and CPU because the caching layer provides zero protection for missing keys (CWE-400).

[Correct implementation](solutions.md#cache-penetration-on-missing-entities)

---

## Cross-tenant cache leakage

A multi-tenant SaaS accounting application provides financial reporting endpoints, caching report objects in Redis.

```java
--8<-- "modules/13-caching-redis/broken-examples/missing-tenant-in-cache-key/AccountReport.java"
```

```java
--8<-- "modules/13-caching-redis/broken-examples/missing-tenant-in-cache-key/TenantReportService.java"
```

Consider multi-tenant data segregation, security implications of cache key collisions, and privacy regulations.

??? warning "Reveal issues"
    **Security issue — Cross-tenant data leakage (IDOR / BOLA):** Cache key is declared as `key = "#reportId"` without scoping by `#tenantId`. When Tenant A accesses report `rep-01`, it is cached. When Tenant B subsequently requests `rep-01`, the cache returns Tenant A's confidential financial report to Tenant B (CWE-639).

    **Design issue — Missing compound key generator:** Multi-tenant cache keys must always include the tenant namespace (`#tenantId + ':' + #reportId`).

[Correct implementation](solutions.md#cross-tenant-cache-leakage)

---

## Infinite TTL and memory exhaustion in Redis

A security session and catalog search service tracks user login sessions and caches search result payloads in Redis using `StringRedisTemplate`.

```java
--8<-- "modules/13-caching-redis/broken-examples/infinite-ttl-memory-leak/UserSessionTracker.java"
```

Consider resource leaks, memory exhaustion risks, and cache retention policy flaws.

??? warning "Reveal issues"
    **Reliability issue — Infinite TTL on dynamic keys:** Calling `opsForValue().set(key, val)` without an explicit TTL creates immortal keys in Redis that survive indefinitely. Over time, millions of inactive session keys accumulate, consuming all available RAM.

    **Performance issue — Unbounded dynamic search query caching:** Caching arbitrary search query strings without expiration rapidly exhausts Redis memory.

    **Reliability issue — Redis OOM command failure:** Under the default `noeviction` policy, hitting `maxmemory` causes Redis to reject all subsequent mutating commands with OOM errors. Under `volatile-lru`, immortal keys are never evicted, forcing the eviction of critical TTL-protected caches.

[Correct implementation](solutions.md#infinite-ttl-and-memory-exhaustion-in-redis)

---

## Dual-write consistency and transaction ordering

A digital wallet and payment processing service transfers funds between customer accounts, mutating database accounts and updating cached balances in Redis.

```java
--8<-- "modules/13-caching-redis/broken-examples/dual-write-consistency-ordering/WalletBalance.java"
```

```java
--8<-- "modules/13-caching-redis/broken-examples/dual-write-consistency-ordering/WalletTransferService.java"
```

Consider transaction atomicity violations, cache-database consistency defects, and rollback failure modes.

??? warning "Reveal issues"
    **Data consistency issue — Dirty cache writes on database transaction rollback:** Mutating external Redis cache inside an active database transaction leaves phantom balances in Redis if the database transaction rolls back, because Redis writes cannot be rolled back by Spring's `PlatformTransactionManager`.

    **Concurrency issue — Dual-write race conditions:** Updating the cache directly instead of evicting creates race conditions where concurrent writers commit to DB and cache in differing orders, resulting in permanently stale cache data.

    **Transaction issue — Missing after-commit synchronization:** Cache evictions must be deferred to `afterCommit` to guarantee invalidation occurs only after physical database commit.

[Correct implementation](solutions.md#dual-write-consistency-and-transaction-ordering)
