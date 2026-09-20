# Solution: Retry Storm Missing Jitter

## Annotated code

```java
package lab.resilience.broken.nojitter;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class CurrencyRateService {

    private final ExternalForexProvider forexProvider;

    public CurrencyRateService(ExternalForexProvider forexProvider) {
        this.forexProvider = forexProvider;
    }

    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        int maxRetries = 5;
        long fixedBackoffMs = 500L;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return forexProvider.fetchRate(fromCurrency, toCurrency);
            } catch (Exception ex) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("Forex provider unavailable after " + maxRetries + " attempts", ex);
                }
                try {
                    // Resilience issue: Deterministic exponential backoff without randomized jitter creates a synchronized retry storm.
                    // All concurrent failed threads wait exactly the same duration (500ms, 1000ms, 2000ms, 4000ms) and retry in lockstep,
                    // hammering the recovering downstream service with massive traffic waves (thundering herd).
                    // Performance issue: High max retries (5) without circuit breaker compounds traffic amplification by a factor of 6x.
                    Thread.sleep(fixedBackoffMs * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during backoff", ie);
                }
            }
        }
        throw new IllegalStateException("Unreachable state in getExchangeRate");
    }

    public interface ExternalForexProvider {
        BigDecimal fetchRate(String from, String to);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Resilience issue | High | `CurrencyRateService.getExchangeRate()` | Deterministic exponential backoff without jitter causes synchronized retry storms |
| 2 | Performance issue | High | `CurrencyRateService.getExchangeRate()` | 5 retries without circuit breaking amplifies traffic by 600% |
| 3 | Reliability issue | Medium | `CurrencyRateService.getExchangeRate()` | No circuit breaker to fast-fail callers during prolonged downstream outages |

## Issue details

### Synchronized retry storm (Missing Jitter)

**Type:** Resilience issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** Distributed Systems, Resilience4j · **Interview frequency:** High · **Production impact:** High

**Location:** `CurrencyRateService.getExchangeRate()`

#### Problem
The retry interval is strictly deterministic: $t = 500\text{ms} \times 2^{\text{attempt}-1}$. If a transient blip causes 1,000 requests to fail at $T=0$, all 1,000 threads sleep until exactly $T=500\text{ms}$, at which point all 1,000 requests hit the downstream service in the exact same millisecond. If that fails, all 1,000 hit again at $T=1500\text{ms}$, and again at $T=3500\text{ms}$.

#### Why it happens
Developers learn exponential backoff formulas ($2^n$) from academic texts without realizing that in multi-client distributed systems, deterministic backoff synchronizes clients into periodic pulses.

#### Production impact
The recovering downstream dependency gets knocked offline repeatedly by periodic tsunami waves of traffic, prolonging outages from seconds into hours.

#### Broken implementation
```java
Thread.sleep(fixedBackoffMs * (1L << (attempt - 1)));
```

#### Correct implementation
See `lab.resilience.jitter.SafeCurrencyRateService`:
```java
// Full Jitter: sleep = random_between(0, min(cap, base * 2^attempt))
IntervalFunction intervalFn = IntervalFunction.ofExponentialRandomBackoff(
        Duration.ofMillis(500), 2.0, 0.5);
```

#### Why the solution works
Random jitter spreads the 1,000 requests uniformly across the time horizon, transforming a spiky pulse into smooth, constant-rate background traffic that allows downstream services to warm caches and recover gracefully.

#### Trade-offs
Individual requests experience non-deterministic latency.

#### Related
- Full Jitter · Equal Jitter · Decorrelated Jitter · Thundering Herd

## Correct implementation

See `lab.resilience.jitter` in `src/main/java/lab/resilience/jitter/SafeCurrencyRateService.java`.
Documentation: [Resilience Solutions](../../../docs/topics/resilience/solutions.md#randomized-jitter-strategies).
