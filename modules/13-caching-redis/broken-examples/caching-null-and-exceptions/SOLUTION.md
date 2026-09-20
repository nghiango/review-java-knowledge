# Solution — Cache Penetration on Missing Entities

## Annotated code

```java
package lab.cachingredis.broken.penetration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final Map<Long, UserProfile> userDatabase = new ConcurrentHashMap<>();
    private final AtomicInteger databaseQueryCount = new AtomicInteger();

    public UserProfileService() {
        userDatabase.put(1001L, new UserProfile(1001L, "alice@example.com", "Alice Smith"));
        userDatabase.put(1002L, new UserProfile(1002L, "bob@example.com", "Bob Jones"));
    }

    // Resilience issue: Cache penetration vulnerability. When a requested user ID does not exist,
    // returning null without caching a sentinel value (or without Bloom filter screening) causes
    // repeated requests for non-existent IDs to bypass the cache and hit the database on every query.
    //
    // Performance issue: An attacker scanning random or non-existent IDs can overwhelm the database
    // with 100% cache-miss traffic (CWE-400 / Denial of Service).
    @Cacheable(value = "userProfiles", key = "#userId")
    public UserProfile getUserProfile(Long userId) {
        databaseQueryCount.incrementAndGet();
        // If the user does not exist, returns null.
        // Spring Cache does not cache nulls unless explicitly configured,
        // or caches null indefinitely depending on RedisCacheConfiguration.
        return userDatabase.get(userId);
    }

    public int getDatabaseQueryCount() {
        return databaseQueryCount.get();
    }
}
```

## Issues identified

### 1. Cache penetration vulnerability (bypassing cache for non-existent keys)
- **Category:** Resilience issue
- **Severity:** High
- **Explanation:** When an entity does not exist in the database, `getUserProfile` returns `null`. By default in many caching configurations (or with `unless = "#result == null"`), `null` values are not cached. Consequently, repeated requests for invalid or malicious IDs bypass the cache completely and execute database queries every single time.

### 2. Database Denial of Service (DoS) via ID enumeration
- **Category:** Performance issue
- **Severity:** High
- **Explanation:** Malicious clients can easily exhaust database connection pools and CPU by firing thousands of requests for random negative or non-existent user IDs. Because none of these keys exist in the cache, the caching layer provides zero protection.

## Correct implementation

See `lab.cachingredis.penetration.SafeUserProfileService` under `src/main/java`.

## Trade-offs

- **Short-Lived Sentinel Nulls:** Caching a sentinel `NULL` object with a short TTL (e.g. 30–60 seconds) stops penetration queries from reaching the database. Trade-off: Consumes small memory in Redis for non-existent keys; newly created users within that 60s window must explicitly evict the sentinel key on creation.
- **Bloom Filters:** A probabilistic Bloom filter can screen incoming IDs in $O(1)$ time with zero database queries. Trade-off: Bloom filters cannot delete keys easily and require synchronization when new entities are added to the database.
