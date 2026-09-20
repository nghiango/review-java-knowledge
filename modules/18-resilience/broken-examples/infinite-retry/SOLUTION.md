# Solution: Infinite Retry Loop

## Annotated code

```java
package lab.resilience.broken.infinitetry;

import org.springframework.stereotype.Service;

@Service
public class OrderPaymentService {

    private final ExternalPaymentGateway paymentGateway;

    public OrderPaymentService(ExternalPaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public PaymentResult processPayment(String orderId, double amount) {
        // Resilience issue: Unbounded while(true) loop causes infinite retries on downstream outage.
        // If the payment provider is down, worker threads spin forever and can never terminate.
        while (true) {
            try {
                PaymentResponse response = paymentGateway.charge(orderId, amount);
                return new PaymentResult(response.transactionId(), true, "Success");
            } catch (Exception ex) {
                // Performance issue: Immediate tight-loop retry with zero backoff spins CPU and floods downstream gateway.
                // Reliability issue: Swallowing all exceptions treats permanent errors (e.g. 400 Bad Request, 402 Card Declined) as retryable.
                // Observability issue: System.err.println logs without structured context, MDC, or correlation IDs.
                System.err.println("Payment failed for order " + orderId + ", retrying immediately: " + ex.getMessage());
            }
        }
    }

    public record PaymentResult(String transactionId, boolean success, String message) {}
    public record PaymentResponse(String transactionId, String status) {}

    public interface ExternalPaymentGateway {
        PaymentResponse charge(String orderId, double amount);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Resilience issue | High | `OrderPaymentService.processPayment()` | Unbounded `while(true)` retry loop never terminates |
| 2 | Performance issue | High | `OrderPaymentService.processPayment()` | Tight loop retry with zero delay spins CPU and generates flood traffic |
| 3 | Reliability issue | High | `OrderPaymentService.processPayment()` | Catching generic `Exception` retries permanent non-retryable errors |
| 4 | Observability issue | Low | `OrderPaymentService.processPayment()` | Logging to standard error instead of structured logger |

## Issue details

### Unbounded retry loop

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Basic
**Technology:** Core Java, Microservice Resilience · **Interview frequency:** High · **Production impact:** High

**Location:** `OrderPaymentService.processPayment()`

#### Problem
The loop condition is hardcoded to `while(true)` without an attempt counter or timeout boundary.

#### Why it happens
Developers often write naive loops assuming external services recover within a few seconds and wanting to "guarantee" delivery.

#### Production impact
When the downstream payment provider experiences a 10-minute outage, incoming worker threads enter the infinite loop and never return. In a Tomcat container with 200 worker threads, 200 concurrent payment requests completely exhaust the container thread pool in seconds, causing total cascading failure across the entire application.

#### Broken implementation
```java
while (true) {
    try {
        return paymentGateway.charge(orderId, amount);
    } catch (Exception ex) { ... }
}
```

#### Correct implementation
See `lab.resilience.boundedretry.SafeOrderPaymentService`:
```java
Retry retry = Retry.of("paymentService", RetryConfig.custom()
    .maxAttempts(3)
    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(Duration.ofMillis(200), 2.0, 0.5))
    .retryExceptions(TransientGatewayException.class)
    .ignoreExceptions(InvalidPaymentException.class)
    .build());
```

#### Why the solution works
Bounded retries limit attempts to a fixed ceiling (e.g. 3 attempts). If all attempts fail, the failure is propagated or handled by a graceful fallback, releasing worker threads.

#### Trade-offs
Failed calls will bubble up and must be handled by callers or dead-letter queues.

#### How to detect it
Worker thread pool saturation (`server.tomcat.threads.busy` = `server.tomcat.threads.max`), thread dumps showing threads blocked in `processPayment`, massive CPU utilization.

#### Interview follow-up
> How do you distinguish between transient network blips and deterministic client/business errors in retry configurations?

#### Related
- Resilience4j Retry · Exponential Backoff · Cascading Failures

---

### Zero-delay tight loop retry

**Type:** Performance issue · **Severity:** High · **Difficulty:** Basic
**Technology:** Resilience4j, Concurrency · **Interview frequency:** High · **Production impact:** High

**Location:** `OrderPaymentService.processPayment()`

#### Problem
Failed attempts are immediately retried with 0ms delay.

#### Why it happens
Omission of sleep or backoff algorithm.

#### Production impact
Hundreds of requests per second spinning on a single thread peg CPU cores at 100% and generate millions of unwanted HTTP requests towards an already struggling downstream service.

#### Broken implementation
```java
} catch (Exception ex) {
    System.err.println("... retrying immediately: " + ex.getMessage());
}
```

#### Correct implementation
Use exponential backoff with randomized jitter (`IntervalFunction.ofExponentialRandomBackoff(...)`).

#### Why the solution works
Spreads retry attempts over time, providing downstream services breathing room to recover.

#### Trade-offs
Slightly increases overall call latency on transient failures.

#### How to detect it
Spike in CPU utilization without increased incoming traffic; spike in outbound HTTP request rate.

#### Related
- Full Jitter · Exponential Backoff · Thundering Herd

---

### Retrying non-retryable exceptions

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** HTTP, Resilience4j · **Interview frequency:** High · **Production impact:** High

**Location:** `OrderPaymentService.processPayment()`

#### Problem
Catching `Exception` catches 4xx client errors, invalid card details, insufficient funds, and business validation errors, and repeatedly retries them.

#### Why it happens
Catching generic `Exception` instead of filtering on transient network timeouts (503 Service Unavailable, 504 Gateway Timeout, `ConnectException`).

#### Production impact
Invalid credit card requests hammer the payment provider indefinitely and lock user transactions rather than immediately returning a clear decline error to the customer.

#### Broken implementation
```java
catch (Exception ex) {
    ...
}
```

#### Correct implementation
Explicitly declare retryable exception types (`retryExceptions`) and non-retryable types (`ignoreExceptions`).

#### Why the solution works
Permanent business and client errors fail fast immediately.

#### Trade-offs
Requires careful classification of upstream HTTP status codes and exception types.

#### Related
- Fail Fast · Poison Messages

## Correct implementation

See `lab.resilience.boundedretry` in `src/main/java/lab/resilience/boundedretry/SafeOrderPaymentService.java`.
Documentation: [Resilience Solutions](../../../docs/topics/resilience/solutions.md#bounded-retries-with-exponential-backoff).
