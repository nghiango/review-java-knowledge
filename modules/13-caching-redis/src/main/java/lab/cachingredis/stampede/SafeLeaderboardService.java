package lab.cachingredis.stampede;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;

@Service
public class SafeLeaderboardService {

    private final HeavyAnalyticsService analyticsService;
    private final Map<String, CacheItem> cache = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> keyLocks = new ConcurrentHashMap<>();

    public record CacheItem(LeaderboardEntry data, long expireAtEpochMs) {
        public boolean isExpired() {
            return System.currentTimeMillis() >= expireAtEpochMs;
        }
    }

    public SafeLeaderboardService(HeavyAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // Cache-Stampede Protection via Distributed/Single-Flight Mutex:
    // Only one thread is allowed to recompute the expensive resource on cache expiration.
    // Concurrent threads block and wait for the single computation or read stale data.
    public LeaderboardEntry getLeaderboard(String category) {
        String cacheKey = "leaderboard:" + category;
        CacheItem cached = cache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            return cached.data();
        }

        ReentrantLock lock = keyLocks.computeIfAbsent(cacheKey, k -> new ReentrantLock());
        lock.lock();
        try {
            // Double-checked locking: verify if another thread just finished recomputing while we
            // were waiting for the lock
            cached = cache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return cached.data();
            }

            // Exactly one thread executes the heavy analytics query
            LeaderboardEntry fresh = analyticsService.computeTopPlayers(category);

            // Add random jitter (500 to 1500 ms) to TTL to prevent simultaneous cache avalanche
            long jitterMs = ThreadLocalRandom.current().nextLong(500, 1500);
            long ttlMs = Duration.ofSeconds(60).toMillis() + jitterMs;
            cache.put(cacheKey, new CacheItem(fresh, System.currentTimeMillis() + ttlMs));

            return fresh;
        } finally {
            lock.unlock();
        }
    }

    public void clearCache() {
        cache.clear();
    }
}
