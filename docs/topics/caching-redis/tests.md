# Caching and Redis Tests

## Caching Testing Strategy

Testing caching implementations requires validating both cache hits, cache misses, expiration, and multi-threaded concurrency safety:

1. **Cache Hit & Read-Through Verification**: Proves that the first read queries the persistent database, while subsequent reads return the cached object without incrementing database query counters.
2. **Cache Invalidation & Zombie Read Prevention**: Verifies that updating or deleting an entity immediately evicts the corresponding cache entry and forces fresh reads from the database.
3. **Concurrency & Stampede Defense**: Simulates dozens of concurrent threads querying an expired key simultaneously, asserting that heavy background computation executes strictly once under mutex protection.
4. **Cache Penetration Resilience**: Validates that repeated queries for non-existent IDs cache a sentinel null and do not repeatedly query the underlying storage.
5. **Multi-Tenant Key Segregation**: Asserts that two tenants requesting the same entity ID receive distinct data and never observe cross-tenant cache contamination.
6. **Transactional Dual-Write Safety**: Proves that a rolled-back transaction never leaves dirty uncommitted data in the cache, and a committed transaction safely evicts the cache via `afterCommit`.
7. **Real Testcontainers Redis Integration**: Validates actual Redis string operations, TTL expiration, and distributed lock acquisition (`SETNX PX`) against a live Redis container.

## Test Suite Overview

```bash
./gradlew :modules:13-caching-redis:test
./gradlew :modules:13-caching-redis:integrationTest
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `ProductCacheTest` | Entity mutations evict cache; reads bypass database on cache hit | Unit test with Spring `ConcurrentMapCacheManager` |
| `CacheStampedeProtectionTest` | 20 concurrent cache misses execute heavy calculation exactly once | Multi-threaded `CountDownLatch` + `ExecutorService` |
| `CachePenetrationTest` | Non-existent IDs cache sentinel and shield database from repeated scans | Query counter assertions |
| `MultiTenantCacheTest` | Different tenants querying the same ID get isolated cache entries | Compound SpEL key verification |
| `TtlPolicyTest` | Session keys have finite TTLs bounded within expected jitter ranges | Time calculations + assertion |
| `DualWriteConsistencyTest` | Rolled back transactions do not contaminate cache with phantom data | `TransactionSynchronizationManager` testing |
| `RedisIntegrationTest` | Real Redis container executes SET/GET with TTL and atomic SETNX locks | Lettuce + `SharedRedisContainer` (Docker) |

## Key Test Snippets

### Proving Cache Stampede Protection Under High Concurrency

```java
--8<-- "modules/13-caching-redis/src/test/java/lab/cachingredis/stampede/CacheStampedeProtectionTest.java"
```

### Proving Cache Penetration Shielding via Sentinels

```java
--8<-- "modules/13-caching-redis/src/test/java/lab/cachingredis/penetration/CachePenetrationTest.java"
```

### Proving Multi-Tenant Cache Isolation

```java
--8<-- "modules/13-caching-redis/src/test/java/lab/cachingredis/multitenant/MultiTenantCacheTest.java"
```

### Proving Dual-Write Transactional Rollback Cleanliness

```java
--8<-- "modules/13-caching-redis/src/test/java/lab/cachingredis/dualwrite/DualWriteConsistencyTest.java"
```

### Proving Real Redis Container Distributed Lock

```java
--8<-- "modules/13-caching-redis/src/integrationTest/java/lab/cachingredis/RedisIntegrationTest.java"
```

## Related

- [Solutions](solutions.md)
- [Code Review](code-review.md)
- [Production](production.md)
