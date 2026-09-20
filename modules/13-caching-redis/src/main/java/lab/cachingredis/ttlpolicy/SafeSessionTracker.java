package lab.cachingredis.ttlpolicy;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class SafeSessionTracker {

    public static final Duration DEFAULT_SESSION_TTL = Duration.ofHours(2);
    public static final Duration DEFAULT_SEARCH_TTL = Duration.ofMinutes(5);

    public record CachedItem(String payload, long expiresAtEpochMs) {
        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAtEpochMs;
        }
    }

    private final Map<String, CachedItem> inMemoryRedis = new ConcurrentHashMap<>();

    // Strict TTL Policy with Jitter:
    // Every key stored in Redis MUST declare an explicit finite TTL.
    // Jitter (e.g. +/- 10% of duration) prevents cascading key expirations (Cache Avalanche).
    public void registerSession(String userId, String sessionToken) {
        String key = "session:" + userId + ":" + sessionToken;
        long jitterMs = ThreadLocalRandom.current().nextLong(-60_000, 60_000);
        long ttlMs = DEFAULT_SESSION_TTL.toMillis() + jitterMs;
        inMemoryRedis.put(key, new CachedItem("ACTIVE", System.currentTimeMillis() + ttlMs));
    }

    public void cacheSearchResult(String query, String jsonResults) {
        String key =
                "search:results:"
                        + UUID.nameUUIDFromBytes(
                                query.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long jitterMs = ThreadLocalRandom.current().nextLong(-10_000, 10_000);
        long ttlMs = DEFAULT_SEARCH_TTL.toMillis() + jitterMs;
        inMemoryRedis.put(key, new CachedItem(jsonResults, System.currentTimeMillis() + ttlMs));
    }

    public boolean isSessionActive(String userId, String sessionToken) {
        String key = "session:" + userId + ":" + sessionToken;
        CachedItem item = inMemoryRedis.get(key);
        if (item == null || item.isExpired()) {
            inMemoryRedis.remove(key);
            return false;
        }
        return "ACTIVE".equals(item.payload());
    }

    public CachedItem getItem(String key) {
        return inMemoryRedis.get(key);
    }
}
