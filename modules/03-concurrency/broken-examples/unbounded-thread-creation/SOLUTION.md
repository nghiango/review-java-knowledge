# Solution: Unbounded thread creation and unmanaged executors

## Annotated code

```java
public class BackgroundJobProcessor {
    private final ReportGenerationWorker worker;

    // Resource management issue: Executors.newCachedThreadPool() configures maximumPoolSize = Integer.MAX_VALUE.
    // Under traffic surges or slow downstream dependencies, it spawns thousands of OS threads, exhausting
    // OS memory and crashing the JVM with "java.lang.OutOfMemoryError: unable to create native thread".
    private final ExecutorService cachedPool = Executors.newCachedThreadPool();

    public BackgroundJobProcessor(ReportGenerationWorker worker) {
        this.worker = worker;
    }

    public void processStandardJob(String reportId) {
        cachedPool.submit(() -> worker.generateReport(reportId));
    }

    public void processUrgentJob(String reportId) {
        // Resource management issue: Creating unmanaged OS threads via new Thread().start() bypasses all
        // rate-limiting, pool boundaries, metrics, and lifecycle management.
        // Observability issue: Unnamed thread with default UncaughtExceptionHandler silently swallows runtime crashes.
        new Thread(() -> worker.generateReport(reportId)).start();
    }

    public void stopImmediately() {
        // Reliability issue: Invoking shutdownNow() without a graceful 2-phase awaitTermination() drops queued
        // work and interrupts in-flight transactions abruptly.
        cachedPool.shutdownNow();
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Resource management issue | Critical | `BackgroundJobProcessor.cachedPool` | Unbounded `cachedThreadPool` causes native thread OOM |
| 2 | Resource management issue | Critical | `BackgroundJobProcessor.processUrgentJob()` | Direct `new Thread()` creation bypasses resource controls |
| 3 | Observability issue | Medium | `BackgroundJobProcessor.processUrgentJob()` | Threads lack descriptive names and exception handlers |
| 4 | Reliability issue | High | `BackgroundJobProcessor.stopImmediately()` | Abrupt termination without graceful shutdown drain |

## Issue details

### Unbounded thread pools in production

**Type:** Resource management issue · **Severity:** Critical · **Difficulty:** Intermediate

Each Java platform thread requires its own OS thread and allocates native stack memory (typically 1MB via `-Xss`). `Executors.newCachedThreadPool()` uses a `SynchronousQueue` with `maximumPoolSize = Integer.MAX_VALUE`, creating a new thread for every task if all existing threads are busy. If tasks take 1 second and the system receives 5,000 requests/sec, the JVM attempts to spawn 5,000 threads, quickly hitting OS PID / `max_user_processes` or virtual memory limits and crashing.

Fix: Use an explicit `ThreadPoolExecutor` with a fixed core/maximum pool size, a bounded `ArrayBlockingQueue` or `LinkedBlockingQueue` with fixed capacity, a named `ThreadFactory`, and an explicit `RejectedExecutionHandler` (e.g. `CallerRunsPolicy` or custom metric-emitting rejector).

### Graceful two-phase executor shutdown

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

Calling `shutdownNow()` abruptly interrupts running threads and discards waiting tasks. Production executors should follow the standard Oracle two-phase shutdown idiom:
1. Call `pool.shutdown()` to stop accepting new tasks while letting running tasks complete.
2. Call `pool.awaitTermination(timeout, unit)` to wait for completion.
3. If timeout expires, call `pool.shutdownNow()` to interrupt stubborn tasks.
4. Call `pool.awaitTermination()` again to ensure threads exit.

## Correct implementation

The production-ready fix lives in `lab.concurrency.unboundedthreads`:
- `CustomThreadPoolFactory.java` configuring bounded `ThreadPoolExecutor`, custom `ThreadFactory` with thread names and uncaught exception handlers, and backpressure policies.
- `BoundedJobProcessor.java` managing task submission, priority queues with bounded capacity, and clean two-phase graceful shutdown hooks.
