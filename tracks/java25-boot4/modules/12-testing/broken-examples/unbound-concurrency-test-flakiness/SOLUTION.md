# Solution: Asynchronous Virtual Thread Test Flakiness & Shared State

## Annotated Code

```java
package lab.java25boot4.testing.broken.asyncflakiness;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AsyncOrderProcessingTest {

    // Concurrency issue: shared mutable static test state without thread synchronization leaks across concurrent test executions
    private static final List<String> NOTIFICATIONS_LOG = new ArrayList<>();

    @Test
    public void testOrderNotificationDispatched() throws InterruptedException {
        OrderNotificationService service = new OrderNotificationService();

        service.dispatchNotification("ORD-8899", status -> NOTIFICATIONS_LOG.add(status));

        // Reliability issue: Thread.sleep in asynchronous tests causes race conditions, non-deterministic execution, and flaky CI builds
        Thread.sleep(50);

        assertThat(NOTIFICATIONS_LOG).contains("NOTIFIED:ORD-8899");
    }
}
```

---

## Issues Identified

### 1. Non-Deterministic `Thread.sleep` in Asynchronous Tests
- **Category:** Reliability
- **Track:** `java25-boot4`
- **Severity:** Critical
- **Description:** Hardcoded `Thread.sleep(50)` makes assertions dependent on execution speed. Under CI CPU throttling or container noisy neighbor conditions, the 80ms worker task finishes after the 50ms sleep, resulting in intermittent false test failures. If the sleep is increased to compensate, the test suite wastes massive developer and CI time.
- **Remediation:** Replace `Thread.sleep` with poll-based waiting using Awaitility: `await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(log).contains(...))` or explicit `CountDownLatch` / `CompletableFuture` synchronizers.

### 2. Shared Mutable Static Test State
- **Category:** Concurrency
- **Track:** `java25-boot4`
- **Severity:** High
- **Description:** Storing test observations in a static `ArrayList` without synchronization causes `ConcurrentModificationException` when tests run concurrently, and leaks test state between executions, causing order-dependent test failures.
- **Remediation:** Use instance-scoped, thread-safe collections (`CopyOnWriteArrayList` or `ConcurrentLinkedQueue`) and reset state in `@BeforeEach`.
