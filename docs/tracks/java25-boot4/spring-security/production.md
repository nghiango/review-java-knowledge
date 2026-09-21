# Production Incidents & Runbooks — Spring Security 7

!!! info "Delta from baseline"
    Baseline production scenarios in [`docs/topics/spring-security/production.md`](../../../topics/spring-security/production.md) cover JWT signing key compromise, CORS wildcard exploitation, and session fixation.
    This page covers **production scenarios specific to Spring Security 7 and Java 25 concurrency**:
    
    1. **The InheritableThreadLocal Identity Spoofing Incident**: Context bleeding across background audit worker pools.
    2. **The Breaking Chaining DSL Upgrade Outage**: Hidden matcher bypasses caused by ambiguous builder returns.

---

## Incident 1: InheritableThreadLocal Identity Spoofing Incident

### Incident Timeline & Symptoms
- **11:00 UTC**: Compliance auditors discover that an unauthenticated external batch webhook triggered a sensitive financial ledger adjustment.
- **11:15 UTC**: The audit log records the mutation as performed by `admin_ops_user`, even though the operator was offline.
- **11:45 UTC**: Investigation reveals that an asynchronous worker thread pool executed the webhook task immediately after processing an administrative maintenance job.

### Root Cause
The application used `InheritableThreadLocal` to propagate security credentials to worker threads. When the administrative job completed, the thread was returned to the pool without calling `clearContext()`. When the unauthenticated webhook task was scheduled on that thread, it inherited the admin credentials and bypassed authorization checks.

### Remediation & Runbook
1. **Adopt Java 25 `ScopedValue`**:
   Replace all thread-local storage with `ScopedValue`. Because `ScopedValue` is bound strictly to the execution block, credentials automatically disappear upon block exit.
2. **Strict Thread Pool Hygiene**:
   Wrap thread pool executors with `DelegatingSecurityContextExecutorService` to ensure automatic cleanup of `SecurityContext`.
3. **Security Audit Metric**:
   Add telemetry monitoring mismatch between incoming request headers and resolved execution principal.

---

## Incident 2: Hidden Matcher Bypass from Ambiguous Builder Chaining

### Incident Timeline & Symptoms
- **16:00 UTC**: Following a security refactoring, external penetration testers discover that `/admin/users/export` is accessible without authentication.
- **16:20 UTC**: Emergency hotfix deployed.

### Root Cause
The legacy security configuration chained `.requestMatchers("/admin/**").hasRole("ADMIN").and().anyRequest().permitAll()`. Due to a misinterpretation of which builder was active after a custom filter `.and()`, the `/admin/**` rule was attached to an ignored sub-registry while `anyRequest().permitAll()` matched all traffic.

### Prevention
Spring Security 7's removal of `.and()` and mandatory use of the lambda DSL makes this bug structurally impossible:
```java
http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/public/**").permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated());
```
