# Module 13 — Caching / Redis

This module contains practical implementations, unit tests, and broken code review exercises for
caching architectures, local vs distributed caching, the Spring Cache abstraction, Redis data structures,
cache stampede prevention, cache penetration safeguards, multi-tenant key namespacing, and dual-write consistency.

Full theory, concepts, internals diagrams, interview questions, and deep walkthroughs live in the documentation:

👉 **[Caching & Redis Documentation](../../docs/topics/caching-redis/index.md)**

## Structure

- `broken-examples/`: Intentionally flawed caching implementations for code review practice
  - `stale-cache-after-update/`: Modifying database entity without invalidating cache
  - `cache-stampede-on-expiry/`: Hot key expiration causing database thundering herd
  - `caching-null-and-exceptions/`: Cache penetration on missing IDs and error caching
  - `missing-tenant-in-cache-key/`: Cross-tenant data leakage due to un-namespaced cache keys
  - `infinite-ttl-memory-leak/`: Unbounded dynamic keys without expiration leading to Redis OOM
  - `dual-write-consistency-ordering/`: Dirty cache writes on transaction rollback
- `src/main/java/lab/cachingredis/`: Production-grade correct implementations
- `src/test/java/lab/cachingredis/`: Fast unit tests
- `src/integrationTest/java/lab/cachingredis/`: Real Redis Testcontainers integration tests
- `src/examples/java/lab/cachingredis/questions/`: Standalone compilable question verification classes
