# Solution: Retry Without Timeout

## Annotated code

```java
package lab.resilience.broken.notimeout;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class InventorySyncService {

    private final RestClient restClient;

    public InventorySyncService(RestClient.Builder restClientBuilder) {
        // Resilience issue: RestClient constructed with default ClientHttpRequestFactory has NO read timeout (infinite timeout).
        // If the warehouse server accepts TCP connection but hangs on reading or writing, the calling thread blocks forever.
        // Configuration issue: Connection timeout is not configured, risking indefinite stalls during SYN-packet packet loss.
        this.restClient = restClientBuilder
                .baseUrl("https://warehouse.internal.company.com")
                .build();
    }

    public InventoryStatus checkStock(String sku) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Reliability issue: Retrying an operation with unbounded execution/socket read time multiplies thread lockup duration.
                return restClient.get()
                        .uri("/api/v1/inventory/{sku}", sku)
                        .retrieve()
                        .body(InventoryStatus.class);
            } catch (Exception ex) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("Failed to sync inventory after " + maxRetries + " attempts", ex);
                }
                try {
                    // Performance issue: Linear backoff without jitter causes synchronized retries if warehouse briefly recovers.
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry backoff", ie);
                }
            }
        }
        return new InventoryStatus(sku, 0, "UNAVAILABLE");
    }

    public record InventoryStatus(String sku, int availableQuantity, String status) {}
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Resilience issue | High | `InventorySyncService.<init>` | Default `RestClient` has infinite socket read timeout |
| 2 | Configuration issue | High | `InventorySyncService.<init>` | Connect timeout is unset, relying on OS TCP handshake timeout |
| 3 | Reliability issue | High | `InventorySyncService.checkStock()` | Retrying unbounded calls amplifies thread blockage |
| 4 | Performance issue | Medium | `InventorySyncService.checkStock()` | Linear backoff lacks random jitter |

## Issue details

### Infinite socket read timeout

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** Spring RestClient, HTTP Client · **Interview frequency:** High · **Production impact:** High

**Location:** `InventorySyncService.<init>`

#### Problem
By default, the JDK `HttpURLConnection` and many HTTP client factories default read timeout to `0`, which means infinite timeout.

#### Why it happens
Developers assume modern HTTP clients come with safe out-of-the-box timeouts.

#### Production impact
If downstream services suffer a deadlock, slow query, or network black hole after connection establishment, incoming request threads hang in socket read forever. Since threads are locked, thread pools become exhausted and whole service instances freeze without any error thrown.

#### Broken implementation
```java
this.restClient = restClientBuilder
        .baseUrl("https://warehouse.internal.company.com")
        .build();
```

#### Correct implementation
See `lab.resilience.timeout.SafeInventorySyncService`:
```java
SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
requestFactory.setConnectTimeout(Duration.ofSeconds(1));
requestFactory.setReadTimeout(Duration.ofSeconds(2));

this.restClient = restClientBuilder
        .requestFactory(requestFactory)
        .baseUrl(warehouseBaseUrl)
        .build();
```

#### Why the solution works
Explicit connect and read timeouts guarantee that no HTTP invocation can monopolize a thread beyond the defined deadline.

#### Trade-offs
Legitimate slow queries will be interrupted with a `ResourceAccessException` / `SocketTimeoutException` if the timeout threshold is set too aggressively.

#### How to detect it
Thread dumps showing worker threads blocked in `java.net.SocketInputStream.read()`.

#### Related
- Connect Timeout · Read Timeout · Resilience4j TimeLimiter

---

### Retrying unbounded calls

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** Resilience, Threading · **Interview frequency:** Medium · **Production impact:** High

**Location:** `InventorySyncService.checkStock()`

#### Problem
Combining a retry loop with an external call that has no timeout creates catastrophic latency amplification. If a call takes 30 seconds to fail (e.g. at TCP keepalive), 3 attempts will lock the calling thread for 90 seconds.

#### Why it happens
Applying retry patterns blindly without first establishing strict timeout contracts.

#### Remediation
Always configure a strict timeout hierarchy: `Execution Timeout` $\ge$ `Retry Count` $\times$ (`Read Timeout` + `Backoff Delay`).

## Correct implementation

See `lab.resilience.timeout` in `src/main/java/lab/resilience/timeout/SafeInventorySyncService.java`.
Documentation: [Resilience Solutions](../../../docs/topics/resilience/solutions.md#strict-timeouts-and-timelimiter).
