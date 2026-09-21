# Resilience Exercises: Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline exercises in [`modules/18-resilience`](../../../topics/resilience/exercises.md) cover configuring Resilience4j annotations and custom event consumers.
    These exercises focus on implementing carrier-safe resilience algorithms and lock-free rate limiters in Java 25.

---

## Exercise 1: Implement Adaptive Equal Jitter

### Objective
Extend `ModernResilientExecutionEngine` to support **Equal Jitter**, which splits the delay into a deterministic half and a randomized half:
$$\text{delay} = \frac{\text{temp}}{2} + \text{random}\left(0, \frac{\text{temp}}{2}\right)$$
where $\text{temp} = \min(\text{maxBackoff}, \text{baseBackoff} \times 2^{\text{attempt}-1})$.

### Requirements
1. Implement `calculateEqualJitterBackoff(int attempt, Duration baseBackoff, Duration maxBackoff)`.
2. Guarantee that the delay is never less than $\frac{\text{temp}}{2}$ and never exceeds $\text{temp}$.
3. Write a unit test asserting minimum and maximum bounds over 100 trials.

---

## Exercise 2: Carrier-Safe Token Bucket Rate Limiter

### Objective
Implement a high-throughput, lock-free rate limiter suitable for millions of virtual threads.

### Requirements
1. Create `CarrierSafeTokenBucket` with parameters `int capacity` and `Duration refillPeriod`.
2. Use `AtomicReference` or `AtomicLong` to track last refill timestamp and available tokens without any `synchronized` blocks.
3. Verify that 100 concurrent virtual threads can consume tokens safely without carrier thread pinning.
