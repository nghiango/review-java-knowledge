# Solution: Obsolete Virtual Thread Pinning Refactor

## Annotated Code

```java
package lab.java25boot4.corejava.broken.pinning;

import java.util.concurrent.locks.ReentrantLock;

public class PaymentSequenceGenerator {

    private final ReentrantLock lock = new ReentrantLock();
    private long currentSequence = 1000L;

    public long nextSequence() {
        // Concurrency issue: Obsolete work-around; synchronized no longer pins virtual threads in Java 24/25
        // Reliability issue: Unprotected lock acquisition without try-finally causes permanent deadlock if an exception is thrown
        lock.lock();
        if (currentSequence >= 999999L) {
            throw new IllegalStateException("Sequence exhausted");
        }
        long next = ++currentSequence;
        lock.unlock();
        return next;
    }
}
```

## Issue Analysis

### 1. Obsolete Carrier Pinning Workaround (Concurrency / Java 25 Delta)
- **Problem**: In Java 21, Project Loom had a limitation where blocking on an internal `synchronized` block or method pinned the underlying OS carrier thread, preventing the virtual thread scheduler from unmounting it. Developers were advised to replace `synchronized` with `java.util.concurrent.locks.ReentrantLock`.
- **Java 25 Reality**: Java 24 and Java 25 resolved this long-standing limitation. The JVM runtime now fully supports unmounting virtual threads while blocking on `synchronized` (ObjectMonitor). Replacing clean `synchronized` with `ReentrantLock` introduces needless verbosity, extra heap allocation, and human error risk.

### 2. Missing `try-finally` Block Causing Permanent Deadlock (Reliability)
- **Problem**: When using `ReentrantLock`, failure to place `lock.unlock()` inside a `finally` block guarantees that if an exception is thrown (such as `IllegalStateException("Sequence exhausted")`), the lock is never released.
- **Consequence**: All subsequent threads calling `nextSequence()` block permanently, starving the thread pool and hanging payment requests.

---

## Correct Implementation

In Java 25, developers can confidently use standard `synchronized` blocks or methods without fear of carrier thread pinning:

```java
package lab.java25boot4.corejava;

public class SafePaymentSequenceGenerator {

    private long currentSequence = 1000L;

    public synchronized long nextSequence() {
        if (currentSequence >= 999999L) {
            throw new IllegalStateException("Sequence exhausted");
        }
        return ++currentSequence;
    }
}
```

Alternatively, for simple monotonic counters, prefer `AtomicLong` which requires zero locking:

```java
public class AtomicSequenceGenerator {
    private final AtomicLong sequence = new AtomicLong(1000L);

    public long nextSequence() {
        return sequence.updateAndGet(curr -> {
            if (curr >= 999999L) throw new IllegalStateException("Sequence exhausted");
            return curr + 1;
        });
    }
}
```

---

## Trade-offs

| Dimension | `synchronized` (Java 21) | `ReentrantLock` (Manual) | `synchronized` (Java 25) |
|---|---|---|---|
| **Carrier Pinning** | **Yes (Pins carrier thread)** | No | **No (Pinning eliminated!)** |
| **Deadlock Risk** | None (compiler managed release) | High (if missing `finally`) | None (compiler managed release) |
| **Code Simplicity** | Clean keyword | Verbose try/finally boilerplate | Clean keyword |
| **Fairness / Conditions**| No | Configurable | No |
