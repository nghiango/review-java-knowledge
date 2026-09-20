# Solution: Retrying Non-Idempotent Call

## Annotated code

```java
package lab.resilience.broken.nonidempotent;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class BillingService {

    private final RestClient restClient;

    public BillingService(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = restClientBuilder
                .requestFactory(factory)
                .baseUrl("https://billing.internal.company.com")
                .build();
    }

    public ChargeConfirmation chargeCustomer(String customerId, double amount, String currency) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Data consistency issue: Retrying a non-idempotent HTTP POST without an Idempotency-Key.
                // If a read timeout occurs after the downstream server committed the transaction but before returning the HTTP 200 response,
                // the subsequent retry will execute a second charge, charging the customer twice.
                return restClient.post()
                        .uri("/api/v1/charges")
                        .body(new ChargePayload(customerId, amount, currency))
                        .retrieve()
                        .body(ChargeConfirmation.class);
            } catch (ResourceAccessException ex) {
                // Reliability issue: Blindly catching ResourceAccessException cannot distinguish between connection failure (safe to retry)
                // and socket read timeout (unsafe without idempotency guarantee).
                if (attempt == maxAttempts) {
                    throw new RuntimeException("Charge failed after " + maxAttempts + " attempts", ex);
                }
                try {
                    // Performance issue: Linear backoff without jitter.
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry backoff", ie);
                }
            }
        }
        throw new IllegalStateException("Unreachable code reached in chargeCustomer");
    }

    public record ChargePayload(String customerId, double amount, String currency) {}
    public record ChargeConfirmation(String chargeId, String status) {}
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Data consistency issue | High | `BillingService.chargeCustomer()` | Retrying non-idempotent HTTP `POST` without `Idempotency-Key` |
| 2 | Reliability issue | High | `BillingService.chargeCustomer()` | Read timeout treated as safe to retry on mutating endpoint |
| 3 | Performance issue | Medium | `BillingService.chargeCustomer()` | Missing randomized jitter in backoff |

## Issue details

### Retrying non-idempotent HTTP POST

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** HTTP, REST, Distributed Systems · **Interview frequency:** High · **Production impact:** High

**Location:** `BillingService.chargeCustomer()`

#### Problem
The service retries `POST /api/v1/charges` when a `ResourceAccessException` is caught. A `ResourceAccessException` occurs on both `ConnectException` (the request never reached the server) and `SocketTimeoutException` (the server received the request, may have executed the database mutation, but response packets were delayed or lost).

#### Why it happens
Assuming network errors mean the remote operation was not performed. In distributed systems, network timeouts represent an **unknown state**, not a failure state.

#### Production impact
Customers are charged two or three times for a single order whenever downstream response latency exceeds 2 seconds. This results in customer churn, chargebacks, and manual financial reconciliation nightmares.

#### Broken implementation
```java
return restClient.post()
        .uri("/api/v1/charges")
        .body(new ChargePayload(customerId, amount, currency))
        .retrieve()
        .body(ChargeConfirmation.class);
```

#### Correct implementation
See `lab.resilience.idempotentretry.SafeBillingService`:
```java
// Pass an Idempotency-Key header generated per business intent (e.g. orderId / paymentId)
String idempotencyKey = UUID.randomUUID().toString();
return restClient.post()
        .uri("/api/v1/charges")
        .header("Idempotency-Key", idempotencyKey)
        .body(payload)
        .retrieve()
        .body(ChargeConfirmation.class);
```

#### Why the solution works
Downstream services check the `Idempotency-Key` against a transactional dedup store. If the key was already executed, they return the cached original response rather than executing a duplicate charge.

#### Trade-offs
Downstream services must implement idempotency key storage and transaction coordination (e.g., Redis or RDBMS unique constraint).

#### Related
- Idempotency Keys · At-Least-Once Delivery · Distributed Transactions

## Correct implementation

See `lab.resilience.idempotentretry` in `src/main/java/lab/resilience/idempotentretry/SafeBillingService.java`.
Documentation: [Resilience Solutions](../../../docs/topics/resilience/solutions.md#idempotent-retries-with-deduplication-keys).
