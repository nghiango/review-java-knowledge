# Caching and Redis Solutions

Production-grade implementations corresponding to the code review exercises.

## Stale cache after entity mutation

### Implementation

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/stalecache/Product.java"
```

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/stalecache/ProductRepository.java"
```

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/stalecache/SafeProductService.java"
```

### Why it works

1. **Explicit Cache Eviction on Update**: `@CacheEvict(value = "products", key = "#id", beforeInvocation = false)` evicts the cache entry when a product price is updated, forcing the next read to fetch the fresh database state.
2. **Post-Invocation Safety**: `beforeInvocation = false` guarantees that cache eviction occurs only if the database write operation succeeds without throwing an exception.
3. **Zombie Read Elimination on Delete**: `@CacheEvict` on `deleteProduct` immediately purges the deleted record from cache.
4. **Conditional Cache Exclusion**: `unless = "#result == null"` ensures that non-existent product queries do not populate cache with null objects as permanent hits.

### Trade-offs

- **Evict vs Put**: Evicting cache forces the next reader to pay a slight latency penalty to query the database and reload the cache, but eliminates race conditions between concurrent updates.

---

## Cache stampede on expiry (thundering herd)

### Implementation

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/stampede/LeaderboardEntry.java"
```

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/stampede/HeavyAnalyticsService.java"
```

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/stampede/SafeLeaderboardService.java"
```

### Why it works

1. **Single-Flight Distributed Mutex**: When a cache miss occurs, threads acquire a lock for that specific cache key. Only the thread that acquires the lock executes the heavy analytics query.
2. **Double-Checked Locking**: Inside the lock boundary, the thread re-checks the cache. If another thread just finished recomputing and populated the cache, the second thread reads from cache immediately without running the heavy query.
3. **TTL Jitter**: Adding randomized jitter (+/- 10%) to the cache expiration prevents multiple categories from expiring simultaneously (Cache Avalanche).

### Trade-offs

- Threads unable to acquire the lock must block briefly or fall back to stale data. For read-heavy analytics where slightly stale data is acceptable, returning the last known cached value while an asynchronous worker recomputes the fresh value yields lower latency.

---

## Cache penetration on missing entities

### Implementation

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/penetration/UserProfile.java"
```

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/penetration/SafeUserProfileService.java"
```

### Why it works

1. **Sentinel Null Caching**: When a user ID is not found in the database, the service writes a `NOT_FOUND_SENTINEL` object to cache with a short TTL (e.g. 30 seconds).
2. **Shielded Database Queries**: Repeated queries for invalid or malicious IDs hit the sentinel in cache and return `null` immediately without touching the database.
3. **Immediate Invalidation on Registration**: When a new user profile is created, any existing sentinel for that ID is explicitly removed from cache.

### Trade-offs

- Consumes minor memory in Redis for non-existent IDs. If an attacker queries billions of completely random non-existent IDs, in-memory Bloom filter screening at the API gateway layer is preferable to prevent Redis memory saturation.

---

## Cross-tenant cache leakage

### Implementation

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/multitenant/AccountReport.java"
```

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/multitenant/SafeTenantReportService.java"
```

### Why it works

1. **Composite Tenant Keyspace**: Every `@Cacheable` and `@CacheEvict` key incorporates the tenant identifier: `key = "#tenantId + ':' + #reportId"`.
2. **Guaranteed Tenant Segregation**: Even if Tenant Alpha and Tenant Beta request reports with identical local identifiers (`rep-01`), their cache entries are isolated into `tenant-alpha:rep-01` and `tenant-beta:rep-01`.
3. **Targeted Invalidation**: Invalidating Tenant Alpha's report does not touch Tenant Beta's cached data.

### Trade-offs

- Requires developers to consistently declare `#tenantId` in every SpEL key, or configure a centralized `TenantAwareKeyGenerator` bean that automatically extracts tenant context from security context.

---

## Infinite TTL and memory exhaustion in Redis

### Implementation

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/ttlpolicy/SafeSessionTracker.java"
```

### Why it works

1. **Strict Mandatory TTL**: Every cached item is written with an explicit expiration duration (`Duration.ofHours(2)` for sessions, `Duration.ofMinutes(5)` for searches).
2. **Entropy Jitter**: Adds randomized jitter (+/- 10%) to prevent simultaneous batch expirations.
3. **Deterministic Memory Reclamation**: Expired keys are purged passively on read or actively by Redis periodic sampling, preventing memory growth from hitting `maxmemory`.

### Trade-offs

- Active sessions require rolling expiration renewals on user activity (heartbeats) to prevent active sessions from abruptly expiring.

---

## Dual-write consistency and transaction ordering

### Implementation

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/dualwrite/WalletBalance.java"
```

```java
--8<-- "modules/13-caching-redis/src/main/java/lab/cachingredis/dualwrite/SafeWalletService.java"
```

### Why it works

1. **Zero Cache Writes Inside Transaction**: The service mutates the relational database inside `@Transactional`, but does NOT write to Redis inside the open transaction.
2. **After-Commit Eviction**: Eviction is deferred to `TransactionSynchronizationManager.registerSynchronization(afterCommit)`.
3. **Rollback Immunity**: If the database transaction throws an exception or fails commit, `afterCommit` never executes. The cache remains completely clean of phantom uncommitted data.
4. **Race Condition Prevention**: Deleting the cache after commit eliminates concurrent writer race conditions where update order in DB differs from update order in cache.

### Trade-offs

- In high-throughput systems where database commit is followed by immediate service crash, cache eviction could be lost. For mission-critical systems, publishing cache eviction events via Transactional Outbox or CDC (Debezium) guarantees at-least-once eviction.
