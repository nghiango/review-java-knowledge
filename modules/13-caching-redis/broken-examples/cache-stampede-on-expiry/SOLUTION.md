# Solution — Cache Stampede on Expiry (Thundering Herd)

## Annotated code

```java
package lab.cachingredis.broken.stampede;

import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HeavyAnalyticsRepository analyticsRepository;

    public LeaderboardService(
            RedisTemplate<String, Object> redisTemplate,
            HeavyAnalyticsRepository analyticsRepository) {
        this.redisTemplate = redisTemplate;
        this.analyticsRepository = analyticsRepository;
    }

    public LeaderboardEntry getLeaderboard(String category) {
        String cacheKey = "leaderboard:" + category;
        LeaderboardEntry cached = (LeaderboardEntry) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return cached;
        }

        // Concurrency issue: Unprotected cache miss allows hundreds of concurrent threads to
        // simultaneously execute expensive recomputations when the key expires (cache stampede).
        LeaderboardEntry fresh = analyticsRepository.computeTopPlayers(category);

        // Performance issue: Synchronous fixed TTL without random jitter causes keys across
        // categories to expire at identical moments, triggering simultaneous database spikes.
        redisTemplate.opsForValue().set(cacheKey, fresh, Duration.ofMinutes(10));

        return fresh;
    }
}
```

## Issues identified

### 1. Cache stampede (thundering herd) on key expiration
- **Category:** Concurrency issue
- **Severity:** Critical
- **Explanation:** When a hot cached item expires, all concurrent incoming requests observe `cached == null` simultaneously. Every requesting thread bypasses the cache and invokes `analyticsRepository.computeTopPlayers(category)`. Under high traffic (e.g. 1000 req/s), this causes sudden database CPU spikes, thread pool exhaustion, and cascading timeout failures.

### 2. Lack of distributed mutex or probabilistic early recomputation
- **Category:** Resilience issue
- **Severity:** High
- **Explanation:** Without a distributed lock (`SET lock:key token NX PX leaseTime`) or probabilistic early refresh (XFetch algorithm), there is no single-flight coordinator ensuring only one worker repopulates the cache while others await or read stale values.

### 3. Synchronized key expiration without jitter
- **Category:** Performance issue
- **Severity:** Medium
- **Explanation:** Using fixed round durations (`Duration.ofMinutes(10)`) causes all keys written in a batch to expire at the exact same second, generating periodic traffic spikes (cache avalanche). Adding random jitter spreads expiration over time.

## Correct implementation

See `lab.cachingredis.stampede.SafeLeaderboardService` under `src/main/java`.

## Trade-offs

- **Distributed Mutex:** Only 1 thread computes the value while other threads block/retry or read stale data. Eliminates database load, but slightly increases latency for waiting threads during cache recomputation.
- **Probabilistic Early Refresh (XFetch):** As the key approaches expiration, requests probabilistically recompute the key in background before it expires. Eliminates cache misses completely at the expense of slight background computation overhead.
