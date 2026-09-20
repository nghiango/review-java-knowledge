# Solution: Blocking network calls and thread spawning in @PostConstruct

## Annotated code

```java
@Service
public class ExchangeRateService {

    private final RemoteRateClient rateClient;

    // Concurrency issue: Plain HashMap accessed by reader threads and modified by background refresh thread.
    private final Map<String, Double> rates = new HashMap<>();

    public ExchangeRateService(RemoteRateClient rateClient) {
        this.rateClient = rateClient;
    }

    @PostConstruct
    public void init() {
        // Reliability / Performance issue: Executing blocking remote HTTP calls inside @PostConstruct stalls the
        // Spring startup thread. If the remote service is slow or down, the ApplicationContext crashes or
        // breaches Kubernetes startup/liveness probe deadlines.
        rates.putAll(rateClient.fetchRates());

        // Resource management / Observability issue: Spawning an unmanaged background thread directly via new Thread().start()
        // inside bean initialization escapes Spring lifecycle management. The thread lacks error handling, metrics,
        // and prevents graceful JVM shutdown.
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60_000);
                    // Concurrency issue: Concurrent write to unsynchronized HashMap while getRate() reads from it.
                    rates.putAll(rateClient.fetchRates());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public double getRate(String currency) {
        return rates.getOrDefault(currency, 1.0);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Reliability issue | Critical | `ExchangeRateService.init()` | Blocking network I/O in `@PostConstruct` delays/crashes container startup |
| 2 | Resource management issue | High | `ExchangeRateService.init()` | Unmanaged `new Thread()` inside bean lifecycle leaks resources |
| 3 | Concurrency issue | High | `ExchangeRateService.rates` | Unsynchronized `HashMap` updated concurrently in background |
| 4 | Reliability issue | High | Startup error handling | Remote API failure during `@PostConstruct` aborts entire service startup |

## Issue details

### Blocking operations during Spring bean initialization

**Type:** Reliability issue · **Severity:** Critical · **Difficulty:** Intermediate

`@PostConstruct` and `InitializingBean.afterPropertiesSet()` are intended strictly for lightweight in-memory setup (validating configured parameters, initializing internal data structures). Executing blocking network calls or database operations here:
1. Blocks the main application startup thread synchronously.
2. If the external dependency fails, the `ApplicationContext` fails to start, preventing the application from starting even if the feature could have degraded gracefully.
3. Bean proxies (such as `@Transactional` or `@Async`) are not yet fully active during constructor/early initialization phases.

### Solutions:
1. **Application Lifecycle Events**: Move initialization to an `@EventListener(ApplicationReadyEvent.class)` listener or `SmartInitializingSingleton`. This ensures the full context is started before background warmup begins.
2. **Scheduled Task Execution**: Use Spring's managed `TaskScheduler` or `@Scheduled` executor rather than ad-hoc `new Thread()` loops.
3. **Resilient Thread-Safe Caching**: Use `ConcurrentHashMap` with atomic updates and fallback cached values to handle network glitches.

## Correct implementation

The production-ready fix lives in `lab.springcore.heavyinit`:
- `ExchangeRateService.java` with thread-safe `ConcurrentHashMap` caching and graceful degradation.
- `ExchangeRateWarmupListener.java` asynchronously loading initial rates on `ApplicationReadyEvent` without blocking container startup.
