package lab.cachingredis.questions;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

/**
 * Q03: How do Spring Cache annotations (@Cacheable, @CachePut, @CacheEvict, condition, unless)
 * operate?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q03SpringCacheAnnotations {

    static class UserService {

        // @Cacheable: If key exists, return from cache immediately without invoking method.
        // condition: Evaluated BEFORE method execution. If false, caching is skipped.
        // unless: Evaluated AFTER method execution. If true, result is NOT stored in cache.
        @Cacheable(
                value = "users",
                key = "#userId",
                condition = "#userId > 0",
                unless = "#result == null")
        public String getUser(Long userId) {
            return "User-" + userId;
        }

        // @CachePut: ALWAYS executes the method body and updates the cache with the returned
        // result.
        @CachePut(value = "users", key = "#userId")
        public String updateUser(Long userId, String name) {
            return name;
        }

        // @CacheEvict: Removes the entry from the cache.
        // beforeInvocation = false: Eviction happens only if method completes without exception.
        @CacheEvict(value = "users", key = "#userId", beforeInvocation = false)
        public void deleteUser(Long userId) {
            // delete logic
        }
    }

    public static void main(String[] args) {
        boolean cacheableSkipsMethodOnHit = true; // true
        boolean cachePutAlwaysExecutesMethod = true; // true
        boolean conditionEvaluatedBeforeInvocation = true; // true
        boolean unlessEvaluatedAfterInvocation = true; // true
    }
}
