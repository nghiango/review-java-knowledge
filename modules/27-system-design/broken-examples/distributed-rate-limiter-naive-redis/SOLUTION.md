# Solution: Distributed API Rate Limiter

## Annotated Target Review

```markdown
# System Design Proposal: Distributed API Rate Limiter

## 1. System Overview
The API Gateway needs to enforce a rate limit of 100 requests per minute per API key across a horizontally scaled cluster of 20 Spring Boot gateway instances.

## 2. Proposed Architecture & Algorithm
```
[Client] ---> [API Gateway Node 1..20] ---> [Central Redis Cache]
```

### Rate Limiting Logic in Spring Gateway Filter
```java
public boolean isAllowed(String apiKey) {
    # Scalability issue: Fixed-window key design allows 2x traffic bursts at window boundaries (100 reqs at 10:00:59 + 100 reqs at 10:01:01).
    String redisKey = "rate_limit:" + apiKey + ":" + (System.currentTimeMillis() / 60000);
    # Concurrency issue: Non-atomic GET and subsequent operation creates a classic Time-Of-Check to Time-Of-Use (TOCTOU) race condition.
    String value = redisTemplate.opsForValue().get(redisKey);
    
    if (value == null) {
        # Concurrency issue: Multiple concurrent gateway threads seeing value == null will all execute SET and reset the counter.
        redisTemplate.opsForValue().set(redisKey, "1", 60, TimeUnit.SECONDS);
        return true;
    }
    
    int count = Integer.parseInt(value);
    if (count < 100) {
        # Concurrency issue: 50 concurrent requests reading count == 99 will all evaluate count < 100 as true, allowing 149 total requests.
        redisTemplate.opsForValue().increment(redisKey);
        return true;
    }
    
    return false; // Return HTTP 429 Too Many Requests
}
```

## 3. Failure Behavior
# Resilience issue: Hard-failing with HTTP 500 when Redis fails (fail-closed without circuit breaking) turns a caching dependency failure into a total gateway outage.
If the Redis cluster is unreachable or network times out, the method throws a `RedisConnectionException` and returns HTTP 500 Internal Server Error to the caller.
```

---

## Discovered Issues

### 1. TOCTOU Race Condition on Non-Atomic Redis Operations
`get()` followed by application-side evaluation and a subsequent `increment()` or `set()` is not atomic. When 50 concurrent requests arrive at 20 gateway instances:
- All 50 threads execute `get()` and receive `count = 99`.
- All 50 threads evaluate `99 < 100` as `true`.
- All 50 threads call `increment()`, allowing 149 requests to pass to backend services, defeating the purpose of the rate limiter.

### 2. Window Boundary Bursting (Fixed Window Flaw)
Using a 1-minute fixed window bucket (`System.currentTimeMillis() / 60000`):
- A client can send 100 requests at 10:00:59 (the end of window 1).
- The client can send another 100 requests at 10:01:01 (the beginning of window 2).
- The client successfully sends **200 requests within a 2-second interval** without being throttled, potentially taking down backend microservices.

### 3. Fail-Closed Catastrophe on Redis Outage
Throwing HTTP 500 upon Redis timeout or connection failure creates an unmitigated single point of failure. If Redis experiences a node failover or network partition, 100% of API traffic fails. Enterprise rate limiters must implement a **Fail-Open Policy** with a local in-memory fallback (Caffeine) or circuit breaker to keep core business traffic flowing.

---

## Correct Implementation

See [`correct/design.md`](correct/design.md) for the production-grade rate limiter implementing the **Sliding Window Counter / Token Bucket** algorithm executed atomically via **Redis Lua scripts** with Resilience4j circuit-breaking and local fail-open semantics.
