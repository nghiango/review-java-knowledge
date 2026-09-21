# Solution: InheritableThreadLocal Security Context Pollution

## Annotated Code

```java
package lab.java25boot4.springsecurity.broken.contextleak;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncAuditSecurityService {

    private final ExecutorService workerPool = Executors.newFixedThreadPool(2);

    public void dispatchPrivilegedAudit(String auditAction) {
        workerPool.submit(() -> {
            // Concurrency issue: InheritableThreadLocal leaks SecurityContext across pooled worker tasks and virtual thread carrier resumptions
            String currentUser = TenantSecurityContextHolder.getUsername();
            String currentRole = TenantSecurityContextHolder.getRole();

            // Security issue: uncleared SecurityContext permits unauthenticated worker tasks to execute privileged audit mutations
            if ("ROLE_ADMIN".equals(currentRole)) {
                executePrivilegedAuditMutation(auditAction, currentUser);
            }
            // Missing finally block to clear context allows bleeding to subsequent tasks
        });
    }

    private void executePrivilegedAuditMutation(String action, String executedBy) {
        System.out.println("AUDIT_MUTATION: action=" + action + ", by=" + executedBy);
    }
}
```

---

## Issues Identified

### 1. Context Retention and Bleed Across Pooled Worker Threads
- **Category:** Concurrency
- **Track:** `java25-boot4`
- **Severity:** Critical
- **Description:** `InheritableThreadLocal` only copies context when a new thread is spawned. When submitted to a reusable thread pool, threads never re-inherit from the submitter; instead, they retain whatever context was set during thread creation or left behind by a previous task. When running on virtual threads, `InheritableThreadLocal` also prevents garbage collection of carrier-bound data structures and accumulates unbounded memory.
- **Remediation:** Replace `InheritableThreadLocal` with Java 25 `ScopedValue` or wrap task execution using `SecurityContextRunnable` / `DelegatingSecurityContextExecutorService`, guaranteeing strict lexical lifetime and automatic cleanup.

### 2. Privilege Escalation via Uncleared Security Context
- **Category:** Security
- **Track:** `java25-boot4`
- **Severity:** Critical
- **Description:** Because the worker task does not clear `TenantSecurityContextHolder` inside a `finally` block, subsequent unprivileged or unauthenticated tasks running on the same worker thread inherit the admin credentials and execute privileged audit actions under false identity.
- **Remediation:** Enforce immutable scoped bindings with `ScopedValue.where(SECURITY_CONTEXT, ctx).run(...)` where context cannot leak beyond the lexical block.
