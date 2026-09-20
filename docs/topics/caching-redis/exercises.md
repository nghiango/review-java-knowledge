# Caching and Redis Exercises

Hands-on exercises to practice multi-tier cache invalidation and atomic distributed rate limiting with Lua.

---

## Exercise 1: Build a Two-Tier Cache Invalidation Manager via Redis Pub/Sub

Implement an invalidation listener that coordinates an in-process Caffeine L1 cache with a distributed Redis L2 cache. When an application node mutates a record, it publishes an invalidation topic message so that all peer application instances immediately clear their local L1 caches.

### Requirements
- Create `TwoTierCacheManager` containing a local `Cache<String, Object>` (Caffeine) and `RedisTemplate`.
- Implement `evict(String key)`: removes key from local L1, removes key from L2 Redis, and publishes `key` to Redis Pub/Sub channel `"cache:invalidations"`.
- Implement `MessageListener` that receives messages from `"cache:invalidations"` and invalidates the specified key from local L1.

??? question "Reveal solution"
    ```java
    @Component
    public class TwoTierCacheManager implements MessageListener {

        private final Cache<String, Object> l1Cache =
                Caffeine.newBuilder()
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .build();

        private final RedisTemplate<String, Object> redisTemplate;
        private static final String INVALIDATION_CHANNEL = "cache:invalidations";

        public TwoTierCacheManager(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        public Object get(String key, Function<String, Object> dbFallback) {
            // Check L1
            Object val = l1Cache.getIfPresent(key);
            if (val != null) {
                return val;
            }

            // Check L2 Redis
            val = redisTemplate.opsForValue().get(key);
            if (val != null) {
                l1Cache.put(key, val);
                return val;
            }

            // Fallback to Primary DB
            val = dbFallback.apply(key);
            if (val != null) {
                redisTemplate.opsForValue().set(key, val, Duration.ofMinutes(30));
                l1Cache.put(key, val);
            }
            return val;
        }

        public void evict(String key) {
            l1Cache.invalidate(key);
            redisTemplate.delete(key);
            redisTemplate.convertAndSend(INVALIDATION_CHANNEL, key);
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            String evictedKey = new String(message.getBody(), StandardCharsets.UTF_8);
            l1Cache.invalidate(evictedKey);
        }
    }
    ```

---

## Exercise 2: Implement an Atomic Sliding-Window Rate Limiter Using Redis Sorted Sets

Implement a distributed sliding-window rate limiter using Redis Sorted Sets (`ZADD`, `ZREMRANGEBYSCORE`, `ZCARD`) wrapped in an atomic Lua script to prevent race conditions.

### Requirements
- Execute within an atomic Lua script to ensure zero race conditions between sliding window evaluation and counter increment.
- Set member score = current timestamp in milliseconds.
- Remove elements older than `(currentTimeMs - windowSizeMs)`.
- Count remaining elements (`ZCARD`). If count `< limit`, add member (`ZADD`) and return `true`; else return `false`.
- Set TTL on the key to `windowSizeMs / 1000 + 1` to prevent storage leaks.

??? question "Reveal solution"
    ```java
    @Component
    public class RedisSlidingWindowRateLimiter {

        private final StringRedisTemplate redisTemplate;
        private final RedisScript<Long> rateLimitScript;

        public RedisSlidingWindowRateLimiter(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;

            String lua =
                    """
                    local key = KEYS[1]
                    local now = tonumber(ARGV[1])
                    local window = tonumber(ARGV[2])
                    local limit = tonumber(ARGV[3])
                    local clearBefore = now - window

                    redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore)
                    local currentRequests = redis.call('ZCARD', key)

                    if currentRequests < limit then
                        redis.call('ZADD', key, now, now .. '-' .. math.random(100000))
                        redis.call('EXPIRE', key, math.ceil(window / 1000) + 1)
                        return 1
                    else
                        return 0
                    end
                    """;
            this.rateLimitScript = RedisScript.of(lua, Long.class);
        }

        public boolean isAllowed(String clientKey, int limit, Duration window) {
            long now = System.currentTimeMillis();
            long windowMs = window.toMillis();

            Long result =
                    redisTemplate.execute(
                            rateLimitScript,
                            List.of("rate:limit:" + clientKey),
                            String.valueOf(now),
                            String.valueOf(windowMs),
                            String.valueOf(limit));

            return result != null && result == 1L;
        }
    }
    ```
