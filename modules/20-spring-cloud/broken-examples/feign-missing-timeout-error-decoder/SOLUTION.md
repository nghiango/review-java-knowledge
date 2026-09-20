# Solution — Feign Missing Timeout and Error Decoder

## Annotated code

```java
package lab.springcloud.broken.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import feign.Retryer;

// Resilience issue: Feign client lacks fallback or fallbackFactory.
// When inventory-service is unreachable or times out, calls fail immediately with raw FeignException
// instead of triggering a graceful degradation path or cached fallback.
@FeignClient(
    name = "inventory-service",
    url = "${inventory.service.url}",
    configuration = InventoryClient.InventoryConfig.class
)
public interface InventoryClient {

  @GetMapping("/api/inventory/{sku}")
  InventoryDto getStock(@PathVariable("sku") String sku);

  // Resilience issue: Mutating POST endpoint is subject to blind client-side retries without idempotency keys.
  // If reserveStock times out on read after the inventory was already reserved remotely, retrying causes
  // duplicate reservations or capacity exhaustion.
  @PostMapping("/api/inventory/{sku}/reserve")
  ReservationResponse reserveStock(
      @PathVariable("sku") String sku,
      @RequestBody ReservationRequest request
  );

  class InventoryConfig {
    // Resilience issue: Missing Request.Options configuring explicit connect and read timeouts.
    // By default or without explicit options, OpenFeign can block on TCP socket reads indefinitely or for 60+ seconds,
    // tying up Tomcat request threads and causing thread pool starvation.

    // Reliability issue: Missing custom ErrorDecoder.
    // OpenFeign's default ErrorDecoder converts all 4xx and 5xx responses into generic FeignException.
    // Permanent domain errors (e.g. 400 Bad Request, 404 Not Found, 422 Unprocessable) are not translated into
    // domain exceptions, and transient 503/504 errors cannot be cleanly distinguished for retry purposes.

    // Resilience issue: Aggressive retryer (10 attempts) applied indiscriminately to all HTTP methods.
    // Default feign Retryer retries on RetryableException; retrying up to 10 times with a 100ms interval
    // creates a retry storm against an already struggling downstream service.
    @Bean
    public Retryer retryer() {
      return new Retryer.Default(100, 1000, 10);
    }
  }

  record InventoryDto(String sku, int availableQuantity) {}
  record ReservationRequest(String orderId, int quantity) {}
  record ReservationResponse(String reservationId, boolean confirmed) {}
}
```

## Issue list

### Resilience issue: Missing explicit connect and read timeouts on Feign client

- **Location:** `InventoryClient.java:28`
- **Description:** No `Request.Options` bean or property (`spring.cloud.openfeign.client.config.default.connectTimeout` / `readTimeout`) is provided.
- **Impact:** Downstream latency spikes or TCP half-open states cause calling threads to block, quickly exhausting web server worker threads and cascading failures upstream.
- **Remediation:** Configure `Request.Options` with tight connect (e.g. 500ms) and read (e.g. 2000ms) timeouts.

### Reliability issue: Missing custom ErrorDecoder to classify 4xx and 5xx responses

- **Location:** `InventoryClient.java:31`
- **Description:** Default error decoding throws generic `FeignException.NotFound`, `FeignException.BadRequest`, or `FeignException.InternalServerError` without domain mapping.
- **Impact:** Upstream callers cannot distinguish between recoverable transient errors (503 Service Unavailable) and non-recoverable business errors (404 Out of Stock). 4xx errors bubble up as internal server errors (500) to clients.
- **Remediation:** Implement an `ErrorDecoder` that maps 404 to `ItemNotFoundException`, 409/422 to `InsufficientStockException`, and 503 to `RetryableException`.

### Resilience issue: Blind retries (10 attempts) on mutating POST requests

- **Location:** `InventoryClient.java:37`
- **Description:** A retryer with 10 attempts is configured across all endpoints, including non-idempotent `POST /reserve`.
- **Impact:** If a read timeout occurs after inventory service decrements stock, the retryer fires up to 10 more times, depleting stock multiple times for a single order.
- **Remediation:** Disable retries (`Retryer.NEVER_RETRY`) in Feign and manage retries explicitly at the service layer using Resilience4j with idempotency keys and exponential backoff.

### Resilience issue: Missing FallbackFactory for circuit breaking and degradation

- **Location:** `InventoryClient.java:13`
- **Description:** `@FeignClient` does not define `fallback` or `fallbackFactory`.
- **Impact:** When downstream inventory fails or the circuit breaker opens, calls immediately abort with exceptions rather than returning degraded cached stock numbers.
- **Remediation:** Define a `FallbackFactory<InventoryClient>` that logs the root cause and returns a safe fallback response or throws a typed domain exception.

## Correct implementation

See [`correct/InventoryClient.java`](correct/InventoryClient.java).

Detailed discussion in [Solutions](../../../docs/topics/spring-cloud/solutions.md#feign-timeouts-custom-errordecoder-and-resilience).
