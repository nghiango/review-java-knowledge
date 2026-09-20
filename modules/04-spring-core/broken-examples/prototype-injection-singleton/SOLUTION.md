# Solution: Prototype bean injected directly into a singleton

## Annotated code

```java
@Component
@Scope("prototype")
public class ExecutionContext {
    private String requestId;
    private Instant startTime;

    public void initialize(String requestId) {
        this.requestId = requestId;
        this.startTime = Instant.now();
    }

    public String getRequestId() {
        return requestId;
    }

    public Instant getStartTime() {
        return startTime;
    }
}

@Service
public class ReportGenerator {

    // Scope / Concurrency issue: Injecting a prototype bean directly into a singleton bean binds
    // the prototype instance to the singleton's lifecycle. Spring resolves and instantiates
    // ExecutionContext exactly once at container startup.
    // Multiple concurrent requests overwrite the shared state in executionContext, leading to race conditions
    // and multi-tenant request ID leakage.
    private final ExecutionContext executionContext;

    public ReportGenerator(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public String generateReport(String requestId) {
        // Concurrency issue: Shared mutable context modified concurrently across threads
        executionContext.initialize(requestId);
        return "Report for request: " + executionContext.getRequestId() + " started at: "
                + executionContext.getStartTime();
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Scope issue | Critical | `ReportGenerator.executionContext` | Prototype bean injected into singleton is never re-created |
| 2 | Concurrency issue | Critical | `ReportGenerator.generateReport()` | Concurrent requests overwrite shared mutable state |
| 3 | Design issue | High | `ExecutionContext` | Mutable state stored in Spring bean instead of method-scoped value object |

## Issue details

### The Singleton-Prototype scoping mismatch

**Type:** Scope issue · **Severity:** Critical · **Difficulty:** Intermediate

When Spring constructs a singleton bean (such as `ReportGenerator`), it resolves all constructor arguments **once**. If one of those dependencies is annotated `@Scope("prototype")`, Spring instantiates a single instance of that prototype and injects it. Subsequent calls to `generateReport()` on the singleton bean always use that exact same instance. The prototype is never instantiated again for subsequent method invocations, completely defeating the purpose of `@Scope("prototype")`.

### Solutions:
1. **`ObjectProvider<T>` (Recommended)**: Inject `ObjectProvider<ExecutionContext>` into `ReportGenerator`, and call `provider.getObject()` on every invocation to obtain a fresh prototype instance.
2. **Method Injection (`@Lookup`)**: Declare an abstract or stub method `@Lookup public abstract ExecutionContext getExecutionContext();` which Spring overrides using CGLIB bytecode generation.
3. **Scoped Proxy (`proxyMode = ScopedProxyMode.TARGET_CLASS`)**: Spring injects a CGLIB proxy that dynamically fetches the scoped target behind the scenes.
4. **Pass as Method Argument / Value Object**: Eliminate the prototype bean entirely and pass a pure immutable `ExecutionContext` record as a parameter to `generateReport(ExecutionContext context)`.

## Correct implementation

The production-ready fix lives in `lab.springcore.prototypescope`:
- `ExecutionContext.java` immutable context model.
- `ReportGenerator.java` using `ObjectProvider<ExecutionContext>` to create dedicated per-execution instances on demand.
- `ExecutionContextFactory.java` factory provider.
