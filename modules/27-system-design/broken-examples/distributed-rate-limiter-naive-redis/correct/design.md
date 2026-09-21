# Production System Design: Distributed Sliding-Window Rate Limiter

## 1. Architectural Highlights
1. **Atomic Evaluation via Redis Lua Scripting**: Enforces sliding-window rate limiting in a single atomic server-side script, eliminating network roundtrips and TOCTOU concurrency anomalies.
2. **Smooth Sliding Window Rate Limiting**: Employs Redis Sorted Sets (`ZADD`, `ZREMRANGEBYSCORE`, `ZCARD`) or Sliding Window Counter to completely eliminate the fixed-window boundary burst flaw.
3. **Resilience & Fail-Open Strategy**: Protects Redis calls with a strict 20ms timeout and Resilience4j circuit breaker. If Redis fails, the gateway gracefully fails open (logging an operational alert) or falls back to local in-memory token buckets (Caffeine).

## 2. Sliding Window Log Algorithm (Atomic Lua Script)
```lua
-- KEYS[1]: rate_limit:{apiKey}
-- ARGV[1]: current timestamp in milliseconds
-- ARGV[2]: window size in milliseconds (e.g. 60000 for 1 min)
-- ARGV[3]: max allowed requests (e.g. 100)

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local clearBefore = now - window

-- 1. Remove timestamps older than the sliding window
redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore)

-- 2. Count remaining timestamps in current sliding window
local currentRequests = redis.call('ZCARD', key)

if currentRequests < limit then
    -- 3. Add current timestamp as both score and member
    redis.call('ZADD', key, now, now .. '-' .. redis.call('INCR', key .. ':seq'))
    redis.call('PEXPIRE', key, window)
    return 1 -- Allowed
else
    return 0 -- Throttled (HTTP 429)
end
```

## 3. Spring Boot Gateway Filter Implementation
```java
@Component
public class ResilientRateLimiterGatewayFilter implements GlobalFilter {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> slidingWindowScript;
    private final CircuitBreaker circuitBreaker;

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String apiKey = extractApiKey(exchange);
        long now = System.currentTimeMillis();

        return Mono.fromCallable(() -> {
            return circuitBreaker.executeSupplier(() -> {
                Long allowed = redisTemplate.execute(
                    slidingWindowScript,
                    List.of("rate_limit:" + apiKey),
                    String.valueOf(now),
                    "60000",
                    "100"
                );
                return allowed != null && allowed == 1L;
            });
        })
        .onErrorReturn(true) // Fail-open on Redis timeout or circuit breaker trip
        .flatMap(allowed -> {
            if (allowed) {
                return chain.filter(exchange);
            }
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            return exchange.getResponse().setComplete();
        });
    }
}
```
