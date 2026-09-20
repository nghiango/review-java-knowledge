# Solution: No Graceful Shutdown with In-Flight Work

## Annotated Code

### `BatchTaskProcessor.java`

```java
package lab.springboot.broken.gracefulshutdown;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class BatchTaskProcessor {

    // Concurrency issue: Raw unmanaged ExecutorService is not registered as a Spring bean lifecycle participant
    // Reliability issue: Executor is not gracefully drained or awaited on container shutdown, dropping in-flight batch jobs
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public void submitJob(Runnable task) {
        executor.submit(
                () -> {
                    task.run();
                    processedCount.incrementAndGet();
                });
    }

    public int getProcessedCount() {
        return processedCount.get();
    }
}
```

### `application.yml`

```yaml
server:
  # Reliability issue: 'immediate' shutdown aborts all active HTTP requests and socket connections instantly upon SIGTERM
  shutdown: immediate
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `BatchTaskProcessor.java:13` | `Concurrency` | Raw unmanaged `ExecutorService` | Does not participate in Spring `SmartLifecycle` or `DisposableBean` shutdown hooks; threads are forcefully terminated. |
| `BatchTaskProcessor.java:16` | `Reliability` | Missing shutdown await and queue draining | In-flight jobs are abandoned in the middle of processing when the JVM process exits. |
| `application.yml:2` | `Reliability` | `server.shutdown: immediate` | Refuses to let in-flight HTTP connections complete, resulting in 502/504 Bad Gateway errors on upstream load balancers during rolling deploys. |

## Correct implementation

- Package: `lab.springboot.gracefulshutdown`
- Production reference: `GracefulTaskProcessor.java`, `ShutdownConfig.java`
- Fix: Configure `server.shutdown: graceful` and `spring.lifecycle.timeout-per-shutdown-phase: 30s` in `application.yml`. Use Spring's `ThreadPoolTaskExecutor` with `setWaitForTasksToCompleteOnShutdown(true)` and `setAwaitTerminationSeconds(30)` so worker threads are allowed to finish their current work before the JVM terminates.
