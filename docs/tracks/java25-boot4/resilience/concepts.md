# Modern Resilience Concepts: Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline resilience concepts in [`modules/18-resilience`](../../../topics/resilience/concepts.md) focus on Resilience4j annotations (`@Retry`, `@CircuitBreaker`, `@RateLimiter`) and thread pool bulkheads.
    This page covers modern retry mechanics, carrier thread safety, and jittered backoffs under Spring Boot 4 and Java 25.

---

## 1. Native Functional Resilience vs Heavy Decorator AOP

In traditional Spring Boot 3 architectures, resilience was predominantly added via external libraries such as Resilience4j:
- Required heavy annotations (`@Retry(name = "backendA")`, `@CircuitBreaker(name = "backendA")`).
- Relied on Spring AOP proxy interception, creating multiple nested proxy layers around every bean invocation.
- Required external configuration hierarchies in `application.yml` coupled to third-party metric binders.

In Spring Boot 4 and Spring Framework 7, resilience is increasingly embraced as a composable functional pipeline:
- Lightweight programmatic engines can wrap `Supplier<T>` or `Runnable` actions directly.
- Avoids bytecode manipulation and proxy overhead.
- Integrates seamlessly with Virtual Threads without context-loss across proxy boundaries.

---

## 2. Virtual Threads and Bulkheads: Semaphore vs Thread Pools

In Java 21+ and Java 25, Virtual Threads dramatically alter the economics of concurrency:
- **Legacy ThreadPool Bulkheads**: Dedicated `ExecutorService` pools with 20 platform threads were historically used to isolate slow downstreams. On Virtual Threads, maintaining fixed platform thread pools re-introduces thread exhaustion bottlenecks and adds context-switch overhead between virtual and platform threads.
- **Semaphore Bulkheads**: A simple `Semaphore` or atomic counter acts as a non-blocking gate. When thousands of virtual threads invoke a downstream service, the semaphore limits concurrent in-flight calls to the external resource (e.g. 50 calls) while remaining threads park cleanly without consuming OS carrier threads.

```text
[10,000 Virtual Threads]
          │
          ▼
   [Semaphore(50)]  <--- Parks virtual threads cheaply when exhausted
          │
          ▼
 [Downstream Resource]
```

---

## 3. Exponential Backoff and Full Jitter

When a downstream service experiences an outage or temporary latency spike, naive retry algorithms with fixed intervals (e.g. retry every 500ms) produce synchronized retry waves:
- All failed clients retry at the exact same moment.
- The recovering downstream service is hit by a massive thundering herd spike, causing it to crash immediately again.

The solution is **Full Jitter**:
$$\text{delay} = \text{random}(0, \min(\text{maxBackoff}, \text{baseBackoff} \times 2^{\text{attempt}-1}))$$

```java
public static long calculateJitteredBackoff(int attempt, Duration baseBackoff, Duration maxBackoff) {
    long baseMillis = baseBackoff.toMillis();
    long maxMillis = maxBackoff.toMillis();
    long exponential = baseMillis * (1L << Math.min(attempt - 1, 10));
    long capped = Math.min(exponential, maxMillis);
    return ThreadLocalRandom.current().nextLong(capped + 1);
}
```

Full Jitter guarantees that retries are uniformly distributed across the interval, flattening the request rate and allowing the downstream service to recover gracefully.

---

## 4. Carrier Thread Safety in Resilience Decorators

When a virtual thread executes inside a `synchronized` block and encounters blocking I/O or a sleeping backoff (`Thread.sleep`), the virtual thread **pins** its underlying OS carrier thread:
- Other virtual threads waiting to run on that carrier thread are starved.
- If all carrier threads in the ForkJoinPool (typically equal to the number of CPU cores) are pinned, the entire JVM stalls.

To ensure carrier thread safety:
- Never use `synchronized` methods or monitors in circuit breakers, rate limiters, or retry loops.
- Use lock-free primitives: `AtomicReference`, `AtomicInteger`, `LongAdder`.
- When mutual exclusion is necessary, use `java.util.concurrent.locks.ReentrantLock`, which parks virtual threads without pinning carrier threads.
