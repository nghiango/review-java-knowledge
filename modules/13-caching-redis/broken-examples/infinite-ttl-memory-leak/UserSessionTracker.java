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

    public void registerSession(String userId, String sessionToken) {
        // Storing high-cardinality user session keys in Redis
        String key = "session:" + userId + ":" + sessionToken;
        redisTemplate.opsForValue().set(key, "ACTIVE");
    }

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
