# Spring Cloud Exercises

Practical hands-on challenges to reinforce edge gateway filtering, rate limiting, and resilient declarative HTTP client engineering.

---

## Exercise 1: Custom Multi-Tenant Gateway Rate Limiter Filter

### Problem Statement

Design a custom Spring Cloud Gateway `GatewayFilterFactory` that enforces dynamic rate limiting based on the caller's subscription tier:
- **Free tier** (`tier: free` in JWT): 10 requests per second, burst 20.
- **Enterprise tier** (`tier: enterprise` in JWT): 1,000 requests per second, burst 2,000.
- **Missing or invalid token**: Reject immediately with HTTP `401 Unauthorized`.

The rate limiter must run asynchronously on Netty without blocking and must attach standard rate limiting response headers:
- `X-RateLimit-Remaining`
- `X-RateLimit-Burst-Capacity`

### Requirements
1. Implement a `AbstractGatewayFilterFactory<MultiTenantRateLimiterConfig>`.
2. Extract the tenant identifier and tier from `ServerWebExchange`.
3. Integrate with reactive Redis token bucket scripts (`ReactiveRedisTemplate`).
4. Return HTTP `429 Too Many Requests` when quota is exceeded.

??? tip "Reveal Solution"
    ```java
    package lab.springcloud.gateway.exercise;

    import java.util.Collections;
    import java.util.List;
    import org.springframework.cloud.gateway.filter.GatewayFilter;
    import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
    import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
    import org.springframework.data.redis.core.script.RedisScript;
    import org.springframework.http.HttpStatus;
    import org.springframework.stereotype.Component;
    import org.springframework.web.server.ServerWebExchange;
    import reactor.core.publisher.Mono;

    @Component
    public class MultiTenantRateLimiterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<MultiTenantRateLimiterGatewayFilterFactory.Config> {

      private final ReactiveStringRedisTemplate redisTemplate;
      private final RedisScript<List<Long>> tokenBucketScript;

      public MultiTenantRateLimiterGatewayFilterFactory(
          ReactiveStringRedisTemplate redisTemplate,
          RedisScript<List<Long>> tokenBucketScript
      ) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = tokenBucketScript;
      }

      @Override
      public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
          String tier = exchange.getRequest().getHeaders().getFirst("X-Tenant-Tier");
          String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");

          if (tenantId == null || tier == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
          }

          int replenishRate = "enterprise".equalsIgnoreCase(tier) ? 1000 : 10;
          int burstCapacity = "enterprise".equalsIgnoreCase(tier) ? 2000 : 20;

          String rateLimitKey = "ratelimit:" + tenantId;
          List<String> keys = Collections.singletonList(rateLimitKey);

          return redisTemplate.execute(
                  tokenBucketScript,
                  keys,
                  List.of(String.valueOf(replenishRate), String.valueOf(burstCapacity))
              )
              .next()
              .flatMap(results -> {
                long allowed = results.get(0);
                long tokensRemaining = results.get(1);

                exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(tokensRemaining));
                exchange.getResponse().getHeaders().add("X-RateLimit-Burst-Capacity", String.valueOf(burstCapacity));

                if (allowed == 1L) {
                  return chain.filter(exchange);
                } else {
                  exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                  return exchange.getResponse().setComplete();
                }
              });
        };
      }

      public static class Config {}
    }
    ```

---

## Exercise 2: Resilient Feign Client with RFC 9457 Problem Details Decoder

### Problem Statement

Implement a production-grade Feign `ErrorDecoder` that integrates with modern Spring Boot 3 RFC 9457 `ProblemDetail` responses.

When a downstream service returns an error:
1. Parse the JSON body into a Spring `ProblemDetail` object.
2. If status is `400 Bad Request` or `422 Unprocessable Entity`: throw `DomainValidationException` containing the problem details `detail` and `properties`.
3. If status is `404 Not Found`: throw `DomainNotFoundException`.
4. If status is `503 Service Unavailable` or `504 Gateway Timeout`: throw Feign's `RetryableException` with an exponential backoff retry-after timestamp.
5. If the error body cannot be parsed or is unreadable: fall back cleanly without swallowing the root cause.

??? tip "Reveal Solution"
    ```java
    package lab.springcloud.feign.exercise;

    import com.fasterxml.jackson.databind.ObjectMapper;
    import feign.Response;
    import feign.RetryableException;
    import feign.codec.ErrorDecoder;
    import java.io.InputStream;
    import java.nio.charset.StandardCharsets;
    import java.util.Date;
    import org.springframework.http.ProblemDetail;

    public class ProblemDetailErrorDecoder implements ErrorDecoder {

      private final ObjectMapper objectMapper = new ObjectMapper();
      private final ErrorDecoder defaultDecoder = new Default();

      @Override
      public Exception decode(String methodKey, Response response) {
        ProblemDetail problem = extractProblemDetail(response);
        String detail = problem != null ? problem.getDetail() : "Downstream error";

        return switch (response.status()) {
          case 400, 422 -> new DomainValidationException(detail, problem);
          case 404 -> new DomainNotFoundException(detail);
          case 503, 504 -> new RetryableException(
              response.status(),
              "Transient downstream failure: " + detail,
              response.request().httpMethod(),
              new Date(System.currentTimeMillis() + 1000), // Retry after 1s
              response.request()
          );
          default -> defaultDecoder.decode(methodKey, response);
        };
      }

      private ProblemDetail extractProblemDetail(Response response) {
        if (response.body() == null) {
          return null;
        }
        try (InputStream is = response.body().asInputStream()) {
          return objectMapper.readValue(is, ProblemDetail.class);
        } catch (Exception ignored) {
          return null;
        }
      }

      public static class DomainValidationException extends RuntimeException {
        private final ProblemDetail problemDetail;
        public DomainValidationException(String message, ProblemDetail problemDetail) {
          super(message);
          this.problemDetail = problemDetail;
        }
        public ProblemDetail getProblemDetail() { return problemDetail; }
      }

      public static class DomainNotFoundException extends RuntimeException {
        public DomainNotFoundException(String message) { super(message); }
      }
    }
    ```
