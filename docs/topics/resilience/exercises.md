# Resilience Exercises

Hands-on exercises to master custom jitter algorithms, stochastic backoff tuning, and adaptive concurrency control.

## Exercise 1: Custom Full Jitter Backoff Calculator

### Problem

In distributed systems under heavy load, deterministic exponential backoff synchronizes retries, generating destructive thundering herds. Implement an algorithmic Full Jitter calculator:
$$\text{Delay} = \text{random}(0, \min(\text{maxDelay}, \text{baseDelay} \times \text{multiplier}^{\text{attempt}-1}))$$

### Requirements

1. Encapsulate base interval, max interval ceiling, and exponential multiplier.
2. Ensure delay is non-negative and strictly bounded by the ceiling.
3. Validate uniform distribution across the range $[0, \text{ceiling}]$.

??? question "Reveal solution"
    ```java
    --8<-- "modules/18-resilience/src/main/java/lab/resilience/FullJitterBackoffCalculator.java"
    ```

---

## Exercise 2: Dynamic Adaptive Concurrency Limiter (TCP Vegas)

### Problem

Static thread pool and bulkhead sizes cannot adapt to downstream service degradation. When a downstream database slows down, static limits cause requests to queue up and exhaust memory. Implement a dynamic concurrency limiter inspired by TCP Vegas:
$$\text{gradient} = \frac{\text{RTT}_{\text{no\_load}}}{\text{RTT}_{\text{observed}}}$$

### Requirements

1. Initialize with a baseline no-load round-trip time ($\text{RTT}_{\text{no\_load}}$) and an initial concurrency limit.
2. When observed $\text{RTT}$ spikes ($\text{gradient} < 0.8$), throttle concurrency proportionally to prevent bufferbloat.
3. When observed $\text{RTT}$ is nominal, increment concurrency limit by 1 to probe for additional available throughput.

??? question "Reveal solution"
    ```java
    --8<-- "modules/18-resilience/src/main/java/lab/resilience/VegasAdaptiveConcurrencyLimiter.java"
    ```

---

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Solutions](solutions.md)
- [Tests](tests.md)
- [Production](production.md)
