# Solution: ThreadLocal Context Loss in Async Virtual Workers

## Issues Identified

```java
package lab.java25boot4.springtransactions.broken.virtualthreadcontext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentOrchestrator {

    // Resource management issue: ThreadLocal lacks automatic lifecycle bounding and leaks state across thread executions
    private static final ThreadLocal<String> CURRENT_TX_CORRELATION = new ThreadLocal<>();
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Transactional
    public void executePayment(String paymentId, double amount, Runnable onCompleteCallback) {
        CURRENT_TX_CORRELATION.set(paymentId);

        // Concurrency issue: Plain ThreadLocal is NOT inherited by virtual threads spawned in Executors. Workers observe null context
        virtualExecutor.submit(() -> {
            String correlation = CURRENT_TX_CORRELATION.get();
            if (correlation == null) {
                System.err.println("Lost transaction correlation in virtual worker!");
            }
            onCompleteCallback.run();
        });

        // Error-handling issue: Missing try-finally cleanup leaks ThreadLocal value on caller thread
    }
}
```

### 1. Concurrency issue (Context Loss across Thread Boundary)
A standard `ThreadLocal` is bound strictly to the thread that invoked `set()`. Virtual threads spawned in an executor run on fresh virtual thread instances and observe `null`, causing transaction tracing and audit logging to fail silently.

### 2. Resource management & Memory issue (Missing Cleanup)
Without `try ... finally { CURRENT_TX_CORRELATION.remove(); }`, the correlation string remains pinned to the request platform or virtual thread.

## Refactored Solution (ScopedValue & Transaction Coordination)
Use Java 25 `ScopedValue` with `StructuredTaskScope` or pass immutable transaction context explicitly into virtual workers:
1. `ScopedValue` scopes are automatically bounded with zero cleanup boilerplate.
2. If tasks are forked inside a structured scope, the scoped context is inherited with zero-copy overhead.
