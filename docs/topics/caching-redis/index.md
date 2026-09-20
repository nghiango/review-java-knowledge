# Caching and Redis

High-throughput systems require fast, low-latency data access to shield relational databases,
minimize computation overhead, and maintain predictable SLA latencies under heavy traffic.

## Architectural Overview

Modern microservices architectures utilize multi-tier caching combining in-process memory with
distributed key-value stores:

```mermaid
flowchart TD
    Client["HTTP / gRPC Client"]
    AppNode["Spring Boot Application Instance"]
    L1Cache["L1 Local Cache (Caffeine)<br/>Heap memory &lt; 100ns"]
    L2Cache["L2 Distributed Cache (Redis Cluster)<br/>In-memory network ~1ms"]
    DB[("Relational Database (PostgreSQL)<br/>Disk / SSD storage ~5-50ms")]

    Client --> AppNode
    AppNode -->|1. Check L1| L1Cache
    L1Cache -- Miss -->|2. Check L2| L2Cache
    L2Cache -- Miss -->|3. Query Primary| DB
    DB -.->|4. Populate L2 and L1| AppNode
```

## Key Invariants

1. **Cache-Aside with Post-Commit Eviction**: In mutating operations, relational database state must be committed before invalidating or deleting the corresponding cache entry (`afterCommit`). Writing to cache directly inside open transactions risks dirty uncommitted reads on rollback.
2. **Deterministic Time-To-Live (TTL)**: Every dynamically created key in an in-memory database must declare an explicit finite expiration TTL. Unbounded key accumulation causes memory exhaustion and crashes under Redis `noeviction` policies.
3. **Entropy & Expiration Jitter**: High-cardinality keys must apply random expiration jitter (+/- 10–20%) to spread TTL expiry over time, preventing synchronized cache avalanches and database connection pool starvation.
4. **Single-Flight Concurrency (Anti-Stampede)**: When hot keys expire, single-flight locking (distributed mutex via Redis `SETNX` or probabilistic early refresh) must ensure exactly one thread recomputes the resource while concurrent requests await or read stale data.
5. **Multi-Tenant Key Scoping**: Cache keys must always be strictly namespaced by tenant identifier (`tenant:{id}:{resource}`) to prevent cross-tenant information leakage and security violations (CWE-639).
6. **Sentinel Nulls Against Cache Penetration**: Repeated queries for non-existent database identifiers must be shielded by caching short-lived sentinel null objects or screening with Bloom filters.

## Module Topics

| Section | Focus |
|---|---|
| [Concepts](concepts.md) | Cache patterns, local vs distributed caching, Spring Cache, Redis data structures, eviction policies |
| [Internals](internals.md) | Spring `CacheInterceptor`, Redis single-threaded event loop, Lettuce Netty multiplexing, Redlock consensus |
| [Interview Questions](questions.md) | 23 questions spanning basic, intermediate, senior, and production outage scenarios |
| [Code Review](code-review.md) | 6 realistic broken examples to practice spotting caching bugs in PRs |
| [Solutions](solutions.md) | Production-grade implementations with design rationale and architectural trade-offs |
| [Tests](tests.md) | Testcontainers Redis testing, concurrency stampede assertions, and tenant isolation suites |
| [Production](production.md) | Redis memory sizing (`maxmemory`), latency monitoring (`SLOWLOG`), eviction tuning, and production checklist |
| [Exercises](exercises.md) | Hands-on exercises: Two-Tier L1/L2 Cache with Pub/Sub invalidation and Atomic Lua Rate Limiter |

## Related Modules

- [Spring Transactions](../spring-transactions/index.md) — Coordinating transaction commits with after-commit cache invalidation
- [Spring Boot](../spring-boot/index.md) — Spring Cache auto-configuration and Lettuce connection pooling
- [REST API](../rest-api/index.md) — HTTP caching headers (`ETag`, `Cache-Control`) and idempotent mutation contracts
