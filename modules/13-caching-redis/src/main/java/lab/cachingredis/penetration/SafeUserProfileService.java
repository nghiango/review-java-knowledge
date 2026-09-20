package lab.cachingredis.penetration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class SafeUserProfileService {

    // Sentinel object representing a confirmed non-existent record in the database
    public static final UserProfile NOT_FOUND_SENTINEL = new UserProfile(-1L, "", "");

    private final Map<Long, UserProfile> userDatabase = new ConcurrentHashMap<>();
    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();
    private final AtomicInteger databaseQueryCount = new AtomicInteger();

    public record CacheEntry(UserProfile profile, long expiresAtEpochMs) {
        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAtEpochMs;
        }
    }

    public SafeUserProfileService() {
        userDatabase.put(1001L, new UserProfile(1001L, "alice@example.com", "Alice Smith"));
        userDatabase.put(1002L, new UserProfile(1002L, "bob@example.com", "Bob Jones"));
    }

    // Cache Penetration Defense:
    // When a requested key is absent from the database, we cache a sentinel object with a short
    // TTL.
    // Subsequent requests for non-existent IDs hit the sentinel in cache and return null without
    // touching the DB.
    public UserProfile getUserProfile(Long userId) {
        CacheEntry cached = cache.get(userId);
        if (cached != null && !cached.isExpired()) {
            if (NOT_FOUND_SENTINEL.equals(cached.profile())) {
                return null;
            }
            return cached.profile();
        }

        databaseQueryCount.incrementAndGet();
        UserProfile fromDb = userDatabase.get(userId);

        if (fromDb == null) {
            // Cache sentinel with short TTL (e.g. 30 seconds) to prevent DB penetration
            long sentinelTtlMs = Duration.ofSeconds(30).toMillis();
            cache.put(
                    userId,
                    new CacheEntry(NOT_FOUND_SENTINEL, System.currentTimeMillis() + sentinelTtlMs));
            return null;
        }

        // Cache real object with standard TTL (e.g. 10 minutes)
        long normalTtlMs = Duration.ofMinutes(10).toMillis();
        cache.put(userId, new CacheEntry(fromDb, System.currentTimeMillis() + normalTtlMs));
        return fromDb;
    }

    // If a new user is created, evict any sentinel to ensure immediate availability
    public void createUserProfile(UserProfile profile) {
        userDatabase.put(profile.userId(), profile);
        cache.remove(profile.userId());
    }

    public int getDatabaseQueryCount() {
        return databaseQueryCount.get();
    }
}
