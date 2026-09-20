# Solution — Infinite TTL & Memory Exhaustion in Redis

## Annotated code

```java
package lab.cachingredis.broken.ttlpolicy;

import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserSessionTracker {

    private final StringRedisTemplate redisTemplate;

    public UserSessionTracker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Reliability issue: Infinite TTL on high-cardinality session keys. Calling set() without a TTL
    // creates persistent keys in Redis that survive indefinitely. Over time, millions of inactive
    // user sessions accumulate, consuming all available RAM.
    public void registerSession(String userId, String sessionToken) {
        // Storing high-cardinality user session keys in Redis
        String key = "session:" + userId + ":" + sessionToken;
        redisTemplate.opsForValue().set(key, "ACTIVE");
    }

    // Performance issue: Dynamic unbounded key generation for search queries without expiration.
    // Unique search query phrases will quickly saturate Redis memory with stale search results.
    //
    // Reliability issue: When Redis maxmemory is reached under default "noeviction" policy, all write
    // operations across the entire application fail with OOM errors. Under "volatile-lru", keys
    // without TTL are never evicted, causing eviction of unrelated TTL-enabled cache entries.
    public void cacheSearchResult(String query, String jsonResults) {
        // Caching arbitrary free-text search queries
        String key = "search:results:" + UUID.nameUUIDFromBytes(query.getBytes());
        redisTemplate.opsForValue().set(key, jsonResults);
    }

    public boolean isSessionActive(String userId, String sessionToken) {
        String key = "session:" + userId + ":" + sessionToken;
        String status = redisTemplate.opsForValue().get(key);
        return "ACTIVE".equals(status);
    }
}
```

## Issues identified

### 1. Persistent memory leak via keys without TTL
- **Category:** Reliability issue
- **Severity:** Critical
- **Explanation:** In Redis, keys written via `opsForValue().set(key, val)` have a TTL of `-1` (infinite). Storing ephemeral domain objects such as user sessions or search queries without a finite TTL causes cumulative, irreversible in-memory growth.

### 2. Redis OOM (Out Of Memory) command failure
- **Category:** Reliability issue
- **Severity:** Critical
- **Explanation:** When Redis hits `maxmemory`:
  - If `maxmemory-policy` is `noeviction` (Redis default), Redis immediately rejects all mutating commands (`SET`, `HSET`, `LPUSH`) with `OOM command not allowed when used memory > 'maxmemory'`, taking down core user workflows.
  - If `maxmemory-policy` is `volatile-lru`, Redis will only evict keys that *have* an expiration set. Keys written without TTL are exempt from eviction, resulting in the eviction of critical TTL-protected caches while infinite-TTL keys remain forever.

### 3. Missing TTL jitter on search cache entries
- **Category:** Performance issue
- **Severity:** Medium
- **Explanation:** Even when TTL is introduced, applying uniform static durations across high volumes of search queries causes cascading cache expirations and periodic database query spikes.

## Correct implementation

See `lab.cachingredis.ttlpolicy.SafeSessionTracker` under `src/main/java`.

## Trade-offs

- **Strict TTL Enforcement:** Guarantees deterministic memory reclamation and prevents Redis OOM, but requires application architects to define appropriate session timeouts and refresh strategies (e.g. rolling TTL on user activity).
- **Eviction Policies (`allkeys-lru` vs `volatile-lru`):** `allkeys-lru` protects Redis from OOM by evicting least-recently-used keys regardless of TTL, but can accidentally discard critical unexpired session tokens if cache memory is shared with bulk query caches.
