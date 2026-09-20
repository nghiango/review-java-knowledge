# Solution: Catch and Ignore Failures

## Annotated code

```java
package lab.resilience.broken.swallowerror;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class CustomerRiskService {

    private final ExternalCreditBureauClient creditClient;

    public CustomerRiskService(ExternalCreditBureauClient creditClient) {
        this.creditClient = creditClient;
    }

    @CircuitBreaker(name = "creditBureau", fallbackMethod = "defaultRiskScore")
    public RiskAssessment evaluateRisk(String customerId) {
        try {
            int score = creditClient.fetchCreditScore(customerId);
            return new RiskAssessment(customerId, score, "ACCURATE");
        } catch (Exception ex) {
            // Resilience issue: Catching and swallowing exception inside the @CircuitBreaker method prevents the aspect from recording a failure!
            // The CircuitBreaker aspect only observes a normal method return, recording it as a SUCCESS.
            // As a result, failure rate stays 0%, the CircuitBreaker NEVER trips to OPEN state, and calls continue hammering the broken downstream bureau.
            // Observability issue: The true downstream failure is hidden from Micrometer/Prometheus failure metrics.
            // Design issue: Dead fallbackMethod code — defaultRiskScore is never triggered because the method never throws an exception out of the aspect.
            System.err.println("Warning: Credit bureau check threw exception: " + ex.getMessage());
            return new RiskAssessment(customerId, 650, "FALLBACK_CAUGHT");
        }
    }

    public RiskAssessment defaultRiskScore(String customerId, Throwable t) {
        return new RiskAssessment(customerId, 600, "CIRCUIT_FALLBACK");
    }

    public record RiskAssessment(String customerId, int score, String tier) {}

    public interface ExternalCreditBureauClient {
        int fetchCreditScore(String customerId);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Resilience issue | High | `CustomerRiskService.evaluateRisk()` | Internal `try-catch` hides failures from CircuitBreaker aspect, preventing OPEN state |
| 2 | Observability issue | High | `CustomerRiskService.evaluateRisk()` | Swallowed exception prevents Micrometer error metrics from incrementing |
| 3 | Design issue | Medium | `CustomerRiskService.evaluateRisk()` | Dead fallback method `defaultRiskScore` is never invoked |

## Issue details

### Swallowing exceptions blinds Circuit Breaker

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** Resilience4j, Spring AOP · **Interview frequency:** High · **Production impact:** High

**Location:** `CustomerRiskService.evaluateRisk()`

#### Problem
Resilience4j's `@CircuitBreaker` uses Spring AOP around advice. The aspect intercepts the method execution:
- If the method completes normally $\to$ records `onSuccess` in the sliding window.
- If the method throws an unhandled exception $\to$ records `onError` in the sliding window and evaluates if failure rate $\ge$ threshold to trip to `OPEN`.

Because the developer caught the exception inside the method body and returned a default value, the method completed normally from the viewpoint of Spring AOP! The CircuitBreaker records every downstream error as a successful call.

#### Why it happens
Defensive programming habits: developers write `try-catch` blocks inside methods out of habit, not realizing how AOP resilience annotations work.

#### Production impact
The circuit breaker **never opens**. During a major downstream bureau outage, all requests continue executing real network calls, waiting for timeouts, consuming socket connections, and hammering the failing dependency. Meanwhile, dashboards show a 100% success rate and green health checks ("zombie mode").

#### Broken implementation
```java
@CircuitBreaker(name = "creditBureau", fallbackMethod = "defaultRiskScore")
public RiskAssessment evaluateRisk(String customerId) {
    try {
        return creditClient.fetchCreditScore(customerId);
    } catch (Exception ex) {
        return new RiskAssessment(customerId, 650, "FALLBACK_CAUGHT");
    }
}
```

#### Correct implementation
See `lab.resilience.circuitbreaker.SafeCustomerRiskService`:
```java
@CircuitBreaker(name = "creditBureau", fallbackMethod = "defaultRiskScore")
public RiskAssessment evaluateRisk(String customerId) {
    // Let exceptions bubble up naturally so the CircuitBreaker aspect records onError
    int score = creditClient.fetchCreditScore(customerId);
    return new RiskAssessment(customerId, score, "ACCURATE");
}

public RiskAssessment defaultRiskScore(String customerId, Throwable t) {
    // Fallback is executed by the aspect when the circuit is OPEN or when an exception occurs
    return new RiskAssessment(customerId, 600, "DEGRADED");
}
```

#### Why the solution works
Exceptions escape the method boundary, allowing the Resilience4j aspect to record the failure in the sliding window, trip the circuit breaker to `OPEN` when the threshold is reached, and invoke the configured fallback method.

#### Related
- Resilience4j Spring Boot · Spring AOP · Circuit Breaker State Transitions

## Correct implementation

See `lab.resilience.circuitbreaker` in `src/main/java/lab/resilience/circuitbreaker/SafeCustomerRiskService.java`.
Documentation: [Resilience Solutions](../../../docs/topics/resilience/solutions.md#circuit-breaker-with-proper-aop-fallbacks).
