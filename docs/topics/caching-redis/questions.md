# Caching and Redis Interview Questions

Answer each question before expanding its explanation.

<!-- --8<-- [start:basic] -->
## Basic Concepts

### What are the fundamental caching patterns, and when should each be used?

Explain Cache-Aside (Lazy Loading), Read-Through, Write-Through, and Write-Behind (Write-Back).

??? question "Reveal answer"
    - **Cache-Aside (Lazy Loading)**: The application directly coordinates between cache and persistent storage. On reads, the application checks the cache; on a miss, it queries the database and populates the cache. On writes, the application updates the database and deletes/evicts the cache entry. Highly resilient because cache failures do not crash the primary datastore.
    - **Read-Through**: The application interacts exclusively with the caching layer as the primary datastore. On a cache miss, the cache provider itself fetches the entity from the database and returns it.
    - **Write-Through**: The application writes to the cache, and the cache provider synchronously writes to the primary database before acknowledging success. Guarantees cache-DB consistency on writes, but incurs higher write latency.
    - **Write-Behind (Write-Back)**: The application writes to the cache, which acknowledges immediately and asynchronously flushes dirty writes to the database in background batches. Delivers extreme write throughput, but risks data loss if the cache node crashes before pending writes flush.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q01CachePatternsComparison.java"
    ```

---

### What are the primary trade-offs between local in-memory caches (Caffeine) and distributed caches (Redis)?

Compare access latency, memory constraints, cluster consistency, and resilience to node crashes.

??? question "Reveal answer"
    - **Local In-Memory Cache (Caffeine)**: Lives directly inside the JVM heap. Delivers sub-microsecond access times (nanoseconds) with zero network overhead, zero serialization costs, and zero external infrastructure. However, data is bounded by JVM heap limits and is lost when the JVM restarts. In multi-instance deployments, local caches diverge, leading to inconsistent state across nodes unless synchronized via an invalidation message bus.
    - **Distributed Cache (Redis)**: Out-of-process in-memory store accessed over the network. Incurs ~0.5ms–2ms network latency and socket serialization overhead. However, it provides a centralized, authoritative cache shared by all application replicas, survives application restarts, and offers rich data structures and replication primitives.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q02LocalVsDistributedCache.java"
    ```

---

### How do Spring Cache annotations (@Cacheable, @CachePut, @CacheEvict, condition, unless) operate?

Detail the lifecycle and conditional evaluation rules of Spring's declarative caching annotations.

??? question "Reveal answer"
    - **`@Cacheable`**: Intercepts method invocation. If the key exists in the target cache, the cached value is returned immediately without executing the method body. If absent, the method executes and the returned result is stored in the cache.
    - **`@CachePut`**: Always executes the underlying method body and updates the cache with the returned result. Used for resource creation or updates.
    - **`@CacheEvict`**: Removes the specified key (or all entries with `allEntries = true`) from the cache. `beforeInvocation = false` (default) ensures eviction occurs only if the method completes successfully without throwing an exception.
    - **`condition`**: SpEL expression evaluated **before** method invocation. If `false`, caching is completely skipped.
    - **`unless`**: SpEL expression evaluated **after** method invocation with access to `#result`. If `true`, the returned result is not cached (e.g. `unless = "#result == null"`).

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q03SpringCacheAnnotations.java"
    ```

---

### How are cache keys generated in Spring Cache via SpEL and custom KeyGenerator beans?

Contrast default `SimpleKeyGenerator` behavior with custom SpEL expressions and dedicated `KeyGenerator` components.

??? question "Reveal answer"
    - **`SimpleKeyGenerator` (Default)**: If a method takes 0 parameters, it returns `SimpleKey.EMPTY`. If 1 parameter, it uses that parameter directly. If multiple parameters, it wraps them into a composite `SimpleKey`. This default is hazardous for multi-tenant applications or when parameters lack proper `equals()` and `hashCode()` implementations.
    - **SpEL Expressions**: Allows fine-grained key construction using parameter references (`key = "#userId"`), nested properties (`key = "#order.id"`), or composite expressions (`key = "#tenantId + ':' + #reportId"`).
    - **Custom `KeyGenerator`**: Implementing `org.springframework.cache.interceptor.KeyGenerator` allows injecting global key formatting rules, automatic tenant prefixes, or hashing strategies across all cached methods.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q04CacheKeyGenerationSpEL.java"
    ```

---

### What core data structures does Redis provide, and what is the optimal use case for each?

List Redis primitives from strings to streams and identify their primary production applications.

??? question "Reveal answer"
    - **String**: Binary-safe byte sequences up to 512MB. Used for serialized JSON objects, session tokens, string values, and atomic counters (`INCR`, `DECR`).
    - **Hash**: Field-value mapping. Efficient for modeling domain entities (user profiles, shopping carts) while saving memory via internal ziplist encoding.
    - **List**: Linked list of strings. Used for message queues, task buffers, and timeline pagination (`LPUSH`, `RPOP`, `BRPOP`).
    - **Set**: Unordered collection of unique strings. Used for tagging, mutual connections, IP blocklists, and set algebra (`SADD`, `SINTER`, `SUNION`).
    - **Sorted Set (ZSet)**: Sets where each element is mapped to a floating-point score. Used for real-time leaderboards, priority queues, and sliding-window rate limiters.
    - **Bitmaps & HyperLogLog**: Bitmaps track boolean states (daily active users); HyperLogLog provides approximate unique cardinality counts using fixed 12KB memory.
    - **Streams**: Append-only log with persistent consumer groups, message IDs, and acknowledgments (`XADD`, `XREADGROUP`).

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q05RedisDataStructuresOverview.java"
    ```

---

### What serialization mechanisms are available for Redis in Spring Data Redis, and why is JDK serialization avoided?

Explain `JdkSerializationRedisSerializer`, `StringRedisSerializer`, `GenericJackson2JsonRedisSerializer`, and typed JSON serializers.

??? question "Reveal answer"
    - **`JdkSerializationRedisSerializer` (Legacy default)**: Serializes objects into Java binary format. Strongly discouraged in modern architectures:
      1. Massive binary size overhead compared to JSON.
      2. Brittle against class changes, package moves, or `serialVersionUID` mismatches.
      3. Critical security vulnerability: Deserialization of untrusted payloads enables Remote Code Execution (RCE) via gadget chains.
    - **`StringRedisSerializer`**: Encodes simple strings as UTF-8 bytes. Lightweight, human-readable, and cross-platform.
    - **`GenericJackson2JsonRedisSerializer`**: Embeds `@class` type metadata in JSON payloads for polymorphic deserialization. Can leak Java package structures and introduces deserialization vulnerability risks if typing is unconstrained.
    - **`Jackson2JsonRedisSerializer<T>`**: Serializes specific Java record/DTO types to clean, schema-less JSON without class metadata. Best practice for microservices.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q06RedisSerializationStrategies.java"
    ```

---

### How do Redis memory eviction policies (maxmemory-policy) operate when memory limits are reached?

Contrast `noeviction`, `allkeys-lru`, `volatile-lru`, `allkeys-lfu`, and `volatile-ttl`.

??? question "Reveal answer"
    - **`noeviction`**: Default policy in standalone Redis. When `maxmemory` is reached, Redis rejects all mutating write commands (`SET`, `HSET`, `LPUSH`) with an OOM error, but continues serving read commands.
    - **`allkeys-lru`**: Evicts the least-recently-used keys among all keys in the database. Recommended configuration for dedicated caching clusters.
    - **`volatile-lru`**: Evicts least-recently-used keys only among keys that have an explicit expiration TTL configured. Keys without TTL are never evicted.
    - **`allkeys-lfu`**: Evicts least-frequently-used keys across all keys using a 24-bit logarithmic frequency counter. Ideal when popularity distribution is skewed.
    - **`volatile-ttl`**: Evicts keys with the shortest remaining TTL first.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q07CacheEvictionPolicies.java"
    ```

---

### How does Redis reclaim expired keys (passive vs active expiration), and why is TTL jitter critical?

Explain Redis expiration algorithms and the danger of simultaneous key expiration.

??? question "Reveal answer"
    - **Passive Expiration (Lazy)**: When a client attempts to read a key (`GET key`), Redis checks its expiration timestamp. If expired, Redis deletes the key and returns `nil`.
    - **Active Expiration (Periodic)**: 10 times per second, Redis randomly samples 20 keys with an expiration. It deletes all expired keys found. If more than 25% of sampled keys are expired, it repeats the sampling loop immediately until the expired ratio drops below 25%.
    - **TTL Jitter**: If thousands of keys are written at the same time with identical round TTLs (e.g. 1 hour), all keys expire at the exact same second. Active expiration locks Redis CPU in sampling loops, while incoming user requests miss the cache and hammer the database (Cache Avalanche). Adding random jitter (+/- 10–20%) distributes expirations evenly across time.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q08RedisTtlAndKeyExpiration.java"
    ```
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Architecture

### What is a Cache Stampede (Thundering Herd), and what strategies effectively prevent it?

Explain how hot key expiration can take down primary database clusters, and detail mitigation techniques.

??? question "Reveal answer"
    A **Cache Stampede** occurs when a high-traffic cached resource expires. Hundreds or thousands of concurrent threads observe a cache miss simultaneously, and all proceed to execute the expensive database query or aggregation at the same time.
    
    **Prevention Strategies:**
    1. **Distributed Mutex (Lock-Aside)**: On cache miss, threads attempt to acquire a short-lived distributed lock (`SET lock:key token NX PX 5000`). Only the thread that acquires the lock recomputes the resource and updates the cache. Other threads await completion or fall back to stale data.
    2. **Probabilistic Early Recomputation (XFetch)**: Calculates early refresh probability based on remaining TTL, computation duration, and an aggressive factor $\beta$. Keys are refreshed asynchronously in the background before they expire.
    3. **Background Pre-Warming**: Critical hot keys are assigned permanent TTLs and refreshed periodically by scheduled background worker tasks.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q09CacheStampedeThunderingHerd.java"
    ```

---

### What is Cache Penetration, and how do Bloom Filters and Sentinel Null Objects mitigate it?

Explain the security and resilience hazards of querying non-existent IDs.

??? question "Reveal answer"
    **Cache Penetration** occurs when incoming queries request keys that exist neither in the cache nor in the primary database (e.g. non-existent user IDs or malicious automated scans). Because the data does not exist, standard caching logic stores nothing, causing every request to bypass the cache and query the database directly.
    
    **Mitigations:**
    1. **Sentinel Null Object Caching**: When the database returns `null` or empty, cache a sentinel object with a short TTL (e.g. 30–60 seconds). Subsequent requests hit the sentinel in cache and return `null` immediately without querying the database.
    2. **Bloom Filter Screening**: A space-efficient probabilistic data structure placed ahead of the cache. If the Bloom filter reports an ID does not exist, the ID is guaranteed not to exist, rejecting the query immediately with zero database operations.
    3. **API Gateway Input Validation**: Reject malformed IDs, negative numbers, or invalid formats before queries reach backend microservices.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q10CachePenetrationMitigation.java"
    ```

---

### What is a Cache Avalanche, and how do clustered architecture and jittered TTL prevent cascading failures?

Explain how mass key expiration or cache cluster failure cascades into database outages.

??? question "Reveal answer"
    A **Cache Avalanche** occurs when a large proportion of cache entries expire at the same time, or when the entire caching cluster becomes unavailable. The sudden torrent of read traffic overwhelms the primary database, exhausting connection pools and causing cascading system failure.
    
    **Prevention Techniques:**
    1. **TTL Random Jitter**: Add randomized variation (e.g. `baseTtl + random(-300, 300)`) to ensure keys expire in a smooth, continuous distribution rather than synchronized cliffs.
    2. **High-Availability Cluster Architecture**: Deploy Redis Sentinel or Redis Cluster with automatic master-replica failover to eliminate single points of failure.
    3. **Multi-Tier Caching (L1/L2)**: Maintain an in-memory L1 Caffeine cache that continues serving traffic during transient Redis network partitions.
    4. **Circuit Breaking & Rate Limiting**: Configure Resilience4j circuit breakers to fail fast or return fallback responses when database latency spikes.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q11CacheAvalanchePrevention.java"
    ```

---

### How are Hot Keys detected in Redis, and what architectural designs protect single Redis nodes from hot-key saturation?

Explain how single-key read traffic can saturate Redis network interfaces, and how to distribute hot load.

??? question "Reveal answer"
    Because Redis shards keys by hash slots, an ultra-popular hot key (e.g. a flash sale item or viral tweet) resides on a single Redis node. Thousands of requests per second can saturate that single node's CPU or network bandwidth while other cluster nodes remain idle.
    
    **Detection:**
    - `redis-cli --hotkeys`: Scans the keyspace using LFU counters to identify hot keys.
    - Client-side metrics and Micrometer request counters tracking key frequency at application boundaries.
    
    **Mitigation:**
    1. **Two-Tier Caching (L1 Local Cache)**: Buffer hot keys in local Caffeine memory on application instances for 5–10 seconds, reducing Redis requests by >99%.
    2. **Key Sharding / Salted Replication**: Duplicate the hot key across multiple slots by appending random suffixes (e.g. `item:1001_1`, `item:1001_2`, `item:1001_N`), scattering reads across cluster nodes.
    3. **Read Replicas**: Direct read traffic across Redis read replicas via a load balancer.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q12HotKeyDetectionAndHandling.java"
    ```

---

### How is an atomic distributed lock implemented in Redis using SET key value NX PX, and why is Lua required for unlocking?

Detail the two-phase lock acquisition and atomic release mechanics.

??? question "Reveal answer"
    1. **Lock Acquisition**: Must be executed in a single atomic command:
       `SET lock:resource <unique_random_token> NX PX <lease_time_ms>`
       - `NX`: Ensures the key is written only if it does not already exist (mutual exclusion).
       - `PX <ms>`: Assigns an auto-release TTL to prevent deadlocks if the lock holder crashes.
       - `<unique_random_token>`: UUID identifying the lock holder so one client cannot release another's lock.
    2. **Lock Release via Lua Script**: A non-atomic `GET` followed by `DEL` introduces a race condition: if the lock expires after `GET` but before `DEL`, another worker acquires the lock, and the delayed `DEL` deletes the new worker's lock! An atomic Lua script verifies that the token matches before deleting:
       ```lua
       if redis.call('get', KEYS[1]) == ARGV[1] then
           return redis.call('del', KEYS[1])
       else
           return 0
       end
       ```

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q13DistributedLockSetnx.java"
    ```

---

### What are the fundamental pitfalls of Redis distributed locks (GC pauses, clock drift, Redlock controversy, fencing tokens)?

Explain why simple distributed locks cannot guarantee safety in storage systems without fencing tokens.

??? question "Reveal answer"
    - **Stop-the-World GC Pauses**: Worker A acquires a lock with a 5-second lease. A JVM GC pause halts Worker A for 7 seconds. The lock expires in Redis. Worker B acquires the lock. Worker A resumes and executes its write concurrently with Worker B, violating mutual exclusion.
    - **Clock Drift**: Systems relying on wall clocks across nodes can experience premature expiration if NTP shifts local time forward.
    - **Asynchronous Replication Hazard**: Worker A acquires a lock on the master. The master crashes before replicating to its replica. The replica is promoted to master, and Worker B acquires the exact same lock.
    - **Fencing Tokens**: To guarantee safety in storage layers, the lock manager must issue a monotonically increasing fencing token (e.g. 31, 32, 33). The database rejects any write carrying a token lower than the latest committed token.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q14DistributedLockPitfalls.java"
    ```

---

### In Cache-Aside architecture, why is "Write to DB, then Delete Cache" preferred over "Write to DB, then Update Cache"?

Analyze the race conditions between concurrent database writers and cache mutators.

??? question "Reveal answer"
    If we choose **Update DB, then Update Cache**, concurrent writes trigger race conditions:
    1. Thread 1 updates DB (value = A).
    2. Thread 2 updates DB (value = B).
    3. Thread 2 updates Cache (value = B).
    4. Thread 1 updates Cache (value = A) due to network delays.
    Now the database holds value B, but the cache permanently holds stale value A!
    
    If we choose **Update DB, then Delete Cache (Eviction)**:
    Deleting the cache forces the subsequent read to fetch the latest committed DB value. The probability of an out-of-order race during read-repopulation is orders of magnitude lower because database reads and cache writes are much faster than database writes.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q15DualWriteConsistencyTradeoffs.java"
    ```

---

### Why should cache evictions be deferred until afterCommit in Spring @Transactional methods?

Explain how transactional dirty writes and phantom cache state occur when evicting inside open transactions.

??? question "Reveal answer"
    If cache eviction is performed **inside** an open `@Transactional` method:
    1. The method executes an update and evicts the cache entry.
    2. A concurrent reading thread misses the cache and reads the database **before the active transaction has committed**.
    3. The reading thread fetches the OLD uncommitted database record and repopulates the cache with stale data.
    4. Alternatively, if the database transaction encounters an error and rolls back, the cache eviction or write cannot be rolled back, leaving inconsistent state.
    
    **Remediation**: Use `TransactionSynchronizationManager.registerSynchronization(afterCommit)` or `@TransactionalEventListener(phase = AFTER_COMMIT)` to ensure cache invalidation runs only after the relational database transaction physically commits.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q16TransactionalCacheSynchronization.java"
    ```
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Engineering & Production

### How is a Two-Tier Cache (L1 Caffeine + L2 Redis) architected, and how is cross-node invalidation handled?

Explain the motivation for two-tier caching and how Redis Pub/Sub coordinates multi-instance synchronization.

??? question "Reveal answer"
    A Two-Tier cache combines an in-process L1 Caffeine cache (<100ns latency) with a centralized L2 Redis cluster (~1ms latency). L1 absorbs massive read spikes and protects Redis network interfaces, while L2 maintains cluster consistency.
    
    **Cross-Node Invalidation:**
    When Node A updates or deletes an entity:
    1. Node A writes to the primary database and commits.
    2. Node A evicts its local L1 cache and the centralized L2 Redis cache.
    3. Node A publishes an invalidation message (e.g. `cache:invalidation:products:101`) to a Redis Pub/Sub topic or Redis Stream.
    4. All other application nodes subscribed to the topic receive the message and evict the specified key from their local L1 caches within milliseconds.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q17TwoTierCachingL1L2.java"
    ```

---

### What is the difference between Redis Pipelines, MULTI/EXEC Transactions, and Lua Scripts?

Contrast execution atomicity, network roundtrip reduction, and rollback semantics.

??? question "Reveal answer"
    - **Redis Pipeline**: Batches multiple commands into a single TCP socket packet, reducing network roundtrip latency (RTT). However, operations are **not atomic**—other clients' commands can interleave between pipelined commands on the Redis server.
    - **MULTI/EXEC Transactions**: Commands are queued on the server and executed sequentially in a single atomic block without interleaving. However, Redis transactions **do not support rollback**—if one command fails with a runtime error, subsequent commands still execute.
    - **Lua Scripts (`EVAL` / `EVALSHA`)**: Atomic server-side execution. The entire Lua script executes uninterrupted on Redis's single-threaded engine, allowing developers to implement conditional logic (`if-else`), inspect intermediate results, and perform atomic multi-step mutations.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q18RedisPipelineAndTransactions.java"
    ```

---

### How does Redis Cluster partition data using Hash Slots, and how do Hash Tags ({...}) enable multi-key operations?

Explain cluster slot assignment, cross-slot limitations, and hash tag syntax.

??? question "Reveal answer"
    Redis Cluster partitions the keyspace across **16,384 logical Hash Slots**:

    $\displaystyle \text{Slot} = \text{CRC16}(\text{key}) \pmod{16384}$
    Multi-key operations (MGET, MSET, transactions, Lua scripts) are rejected with a `CROSSSLOT` error if the involved keys belong to different hash slots residing on different physical cluster nodes.
    
    **Hash Tags (`{...}`)**:
    When a key contains `{...}`, Redis computes the CRC16 hash slot using **only the substring within the braces**. For example, `{user:1001}:profile` and `{user:1001}:orders` will always hash to the exact same slot, allowing atomic multi-key operations and transactions across them.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q19RedisClusterPartitioning.java"
    ```

---

### What are the differences and operational trade-offs between RDB snapshots and AOF (Append-Only File) in Redis?

Compare disaster recovery, write throughput, fsync policies, and recovery duration.

??? question "Reveal answer"
    - **RDB (Redis Database Snapshot)**: Point-in-time binary snapshot created via background `BGSAVE` fork.
      - *Pros*: Extremely compact; minimal impact on disk I/O; fast server restart and replication initialization.
      - *Cons*: Higher data loss window (minutes of writes lost if crash occurs between scheduled snapshots); `fork()` memory copy-on-write overhead on large heaps.
    - **AOF (Append-Only File)**: Transaction log recording every write command.
      - *Fsync Policies*: `appendfsync always` (slowest, max safety), `appendfsync everysec` (standard baseline: at most 1s of data lost), `appendfsync no` (OS flushes buffer).
      - *Pros*: Minimal data loss; human-readable append log.
      - *Cons*: Larger file size; slower restart recovery time (must replay write log).
    - **Hybrid Persistence (Redis 4.0+)**: Combines RDB snapshot as the preamble with recent AOF write logs at the tail, achieving fast restarts and low data loss.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q20RedisPersistenceRdbAof.java"
    ```

---

### How do Lettuce and Jedis Redis drivers interact with Project Loom Java 21 Virtual Threads?

Analyze thread pinning, connection pooling, and socket multiplexing under high virtual thread concurrency.

??? question "Reveal answer"
    - **Jedis**: Connection-per-thread architecture relying on `JedisPool`. Under Project Loom with 10,000+ virtual threads, a connection pool of 50–100 sockets quickly becomes a severe bottleneck. Thousands of virtual threads block waiting for pooled connections.
    - **Lettuce**: Netty-based asynchronous driver that multiplexes requests from thousands of concurrent threads across a single shared TCP socket connection using pipelining. Under Java 21, virtual threads invoking synchronous Lettuce operations unmount cleanly from OS carrier threads during socket I/O without pinning, making Lettuce the superior driver for Virtual Thread architectures.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q21RedisVirtualThreadsReentrancy.java"
    ```
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Production Scenarios

### Incident Walkthrough: Database pool exhausted after midnight cache expiration

A high-traffic e-commerce portal crashed at 00:00:00 when all promotional product caches expired simultaneously. Walk through root cause analysis and immediate remediation.

??? question "Reveal answer"
    - **Incident Timeline**: At 00:00:00, the daily deals cache key expired. Over 15,000 concurrent requests missed the cache simultaneously. All 15,000 threads queried PostgreSQL with heavy multi-table joins, exhausting HikariCP within 300ms. The entire web application began failing health checks and returning HTTP 504 Gateway Timeout.
    - **Immediate Mitigation**: An engineer used `redis-cli` to manually set the hot deal key with a temporary 24-hour TTL, instantly dropping database load and allowing HikariCP connections to recover.
    - **Root Cause**: The caching implementation used fixed-duration TTLs without single-flight locking (distributed mutex) or probabilistic early expiration.
    - **Permanent Fix**: Introduced single-flight mutex locking via Redis `SETNX` so that only 1 worker recomputes the deal payload on expiration, and added randomized TTL jitter (+/- 15%) across all product caches.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q22CacheStampedeDowntimeIncident.java"
    ```

---

### Incident Walkthrough: Payment rollback left phantom credits in Redis cache

A financial wallet transfer failed and rolled back in PostgreSQL, but customer accounts retained cached balances in Redis. Walk through the dual-write violation and fix.

??? question "Reveal answer"
    - **Incident Timeline**: A wallet transfer service executed inside a Spring `@Transactional` method: it credited the receiver's account in PostgreSQL, immediately updated Redis with the new receiver balance, and then performed a secondary compliance check. The compliance check failed and threw an exception. Spring rolled back the PostgreSQL database transaction. However, the Redis write remained committed, displaying phantom funds that the user immediately withdrew.
    - **Root Cause**: Mutating an external non-transactional cache directly inside a relational database transaction violates two-phase consistency. External cache mutations cannot be rolled back by Spring's `PlatformTransactionManager`.
    - **Permanent Fix**: Refactored to Cache-Aside with Post-Commit Eviction. Instead of updating the cache inside the transaction, the service registers a cache eviction hook via `TransactionSynchronizationManager.registerSynchronization(afterCommit)`. If the transaction rolls back, the hook never executes, leaving the cache clean.

??? example "Example"
    ```java
    --8<-- "modules/13-caching-redis/src/examples/java/lab/cachingredis/questions/Q23DualWriteRollbackDirtyCacheIncident.java"
    ```
<!-- --8<-- [end:scenarios] -->

## Related

- [Caching and Redis Concepts](concepts.md)
- [Caching and Redis Internals](internals.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
