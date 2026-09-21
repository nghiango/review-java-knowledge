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
    String redisKey = "rate_limit:" + apiKey + ":" + (System.currentTimeMillis() / 60000);
    String value = redisTemplate.opsForValue().get(redisKey);
    
    if (value == null) {
        redisTemplate.opsForValue().set(redisKey, "1", 60, TimeUnit.SECONDS);
        return true;
    }
    
    int count = Integer.parseInt(value);
    if (count < 100) {
        redisTemplate.opsForValue().increment(redisKey);
        return true;
    }
    
    return false; // Return HTTP 429 Too Many Requests
}
```

## 3. Failure Behavior
If the Redis cluster is unreachable or network times out, the method throws a `RedisConnectionException` and returns HTTP 500 Internal Server Error to the caller.
