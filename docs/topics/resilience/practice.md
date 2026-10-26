# Resilience — Practice Q&A

Senior-depth practice from interview-prep §7, §15, and §17.7.

### Q1. How do fixed windows, sliding windows, token buckets, and leaky buckets differ?

Merged from: §7 rate limiting + §15 global limit + §17.7

??? question "Reveal answer"
    - **Choose a policy for fairness, burst tolerance, and enforcement scope.**
    - **Mechanism:** fixed windows permit boundary bursts; sliding logs are precise but costly; token buckets permit bounded bursts at a refill rate; leaky buckets smooth output.
    - **Failure:** per-pod limits multiply with replicas; spoofable headers undermine identity; non-atomic refill-and-decrement oversells permits.
    - **Fix:** use monotonic elapsed time locally, synchronize refill plus debit, validate parameters, and cap per-client state. For global enforcement, use an atomic shared-store operation with explicit outage behavior.
    - **Trade-off:** shared coordination adds latency and availability dependencies; local leases reduce calls but permit bounded overshoot.
    - **Follow-up:** Does your policy protect users behind NAT without allowing identity churn abuse?
    - **Takeaway:** State the rate, burst, identity, and consistency boundary.

### Q2. How is load shedding different from rate limiting?

??? question "Reveal answer"
    - **Rate limiting allocates client share; shedding protects service capacity.**
    - **Mechanism:** concurrency limits bound admitted work; adaptive limits adjust from queueing or latency signals. A static cap is not adaptive merely because a latency setting exists.
    - **Failure:** exempting every POST leaves the most expensive path unbounded. Increment/decrement races or missing finally cleanup permanently distort capacity.
    - **Fix:** define priorities plus a hard global safety ceiling; release permits on completion/cancellation; use bounded route labels. Usually distinguish 429 client quota from 503 service overload.
    - **Trade-off:** low limits waste capacity; high limits add queueing. Align Retry-After with jittered, bounded client retries.
    - **Follow-up:** How do you prevent a low-priority queue from consuming checkout connections?
    - **Takeaway:** Prioritize critical work without making it unlimited.

### Q3. How should timeouts, retries, breakers, bulkheads, and fallbacks compose?

??? question "Reveal answer"
    - **Protect one end-to-end deadline and avoid retry amplification.**
    - **Mechanism:** timeouts cap attempts, retries seek recovery, breakers avoid repeated failing calls, bulkheads bound concurrency, and fallbacks supply explicitly degraded behavior.
    - **Failure:** retries at three layers multiply traffic; a breaker observes too few failures if fallback masks them; a timeout may leave the underlying operation running.
    - **Fix:** assign retry ownership, retry only safe transient failures with capped jitter, pass remaining budgets downstream, and test decorator order and resource release.
    - **Trade-off:** fail-open is useful for optional caches; authorization and financial invariants generally require fail-closed behavior. Durable queues still need retention and disk limits.
    - **Follow-up:** Should an open breaker count as a retryable failure?
    - **Takeaway:** Resilience patterns must cooperate under overload, not multiply it.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Cache practice](../caching-redis/practice.md)
