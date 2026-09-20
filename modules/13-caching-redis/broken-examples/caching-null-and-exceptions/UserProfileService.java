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
