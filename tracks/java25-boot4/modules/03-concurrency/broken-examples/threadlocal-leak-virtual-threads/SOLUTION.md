# Solution: Virtual Thread Context Propagation & Leaks

## Issues Identified

```java
package lab.java25boot4.concurrency.broken.threadlocalleak;

public final class TenantContextHolder {

    // Memory issue: InheritableThreadLocal creates a deep map copy for every child virtual thread, causing severe heap bloat under million-thread workloads
    private static final ThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();

    private TenantContextHolder() {}

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void processTenantRequest(String tenantId, Runnable task) {
        setTenantId(tenantId);
        // Error-handling issue: Missing try-finally block to remove the ThreadLocal value, causing context pollution and memory leaks across thread pools or virtual carriers
        task.run();
    }
}
```

### 1. Memory issue (`InheritableThreadLocal` on Virtual Threads)
`InheritableThreadLocal` forces child threads to clone the parent thread's map. When spawning thousands of short-lived virtual threads per request, each thread allocates a `ThreadLocalMap` table, quickly exhausting heap space and destroying the lightweight advantages of virtual threads.

### 2. Error-handling issue (Unbounded Context Lifetime & Missing Cleanup)
There is no `try-finally` cleanup via `CURRENT_TENANT.remove()`. If `task.run()` fails with an exception, or if virtual thread pools/carriers reuse threads in executors, tenant data persists, bleeding into subsequent unrelated requests.

## Refactored Solution (Java 25 `ScopedValue`)
In Java 25, `ScopedValue` solves both issues:
1. **Immutable & Lightweight**: Values are stored on the execution frame / thread scope table rather than copying hash maps into every thread.
2. **Guaranteed Dynamic Scope**: Value is only bound for the duration of the lambda or runnable invocation and automatically unbound when the scope completes.
