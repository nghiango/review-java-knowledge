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

        // Cache miss: recompute from expensive repository
        LeaderboardEntry fresh = analyticsRepository.computeTopPlayers(category);

        // Fixed TTL without jitter or distributed mutex protection
        redisTemplate.opsForValue().set(cacheKey, fresh, Duration.ofMinutes(10));

        return fresh;
    }
}
