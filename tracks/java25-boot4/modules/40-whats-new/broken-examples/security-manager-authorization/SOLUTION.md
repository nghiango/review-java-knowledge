# Solution: SecurityManager-Based Authorization

## Annotated code

```java
package lab.java25boot4.whatsnew.broken.securitygate;

import java.security.AccessController;
import java.security.Permission;

public class SecurityManagerGate {

    private static final Permission ADMIN_PERMISSION = new RuntimePermission("admin.operations");

    // Maintainability issue: userId is accepted but never consulted. Authorization cannot depend
    // on the caller's identity because the check is JVM-global, so any authenticated (or even
    // unauthenticated) caller is indistinguishable here.
    public boolean isAuthorized(String userId) {
        // Security issue: System.getSecurityManager() is permanently null on Java 25 (JEP 486),
        // so this branch is always taken.
        // Security issue: returning true when no SecurityManager is installed is fail-open — the
        // absence of a security mechanism is treated as "allowed", which is a full bypass.
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager == null) {
            return true;
        }
        try {
            // Reliability issue: AccessController.checkPermission is deprecated for removal and
            // no longer throws on Java 25. The try/catch below is dead code that implies a
            // protection which does not exist.
            AccessController.checkPermission(ADMIN_PERMISSION);
            return true;
        } catch (SecurityException denied) {
            return false;
        }
    }
}
```

```java
package lab.java25boot4.whatsnew.broken.securitygate;

public class AdminOperationService {

    private final SecurityManagerGate gate;

    public AdminOperationService(SecurityManagerGate gate) {
        this.gate = gate;
    }

    // Security issue: because the gate always allows on Java 25, every caller can purge tenant
    // data — a broken object-level authorization check.
    public String purgeTenantData(String userId) {
        if (!gate.isAuthorized(userId)) {
            return "denied";
        }
        return "purged tenant data for " + userId;
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Security issue | Critical | `SecurityManagerGate.isAuthorized()` | `SecurityManager` is permanently disabled on Java 25 — the gate is a no-op |
| 2 | Security issue | Critical | `SecurityManagerGate.isAuthorized()` | Fail-open default: a missing security mechanism is treated as "allowed" |
| 3 | Reliability issue | Medium | `SecurityManagerGate.isAuthorized()` | `AccessController.checkPermission` is deprecated and never throws — dead protection |
| 4 | Maintainability issue | High | `SecurityManagerGate` / `AdminOperationService` | Authorization ignores caller identity; policy is JVM-global instead of per-request |

## Issue details

## Authorization depends on the disabled SecurityManager

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate
**Track:** `java25-boot4` · **Technology:** `SecurityManager`, `AccessController`, JEP 486
**Interview frequency:** High · **Production impact:** Critical

**Location:** `SecurityManagerGate.isAuthorized()`

### Problem
The entire authorization decision rests on `System.getSecurityManager()`. On Java 25 the
`SecurityManager` is permanently disabled (JEP 486): it can no longer be installed, and
`System.getSecurityManager()` always returns `null`. The gate therefore never performs a check.

### Why it happens
On Java 17 the `SecurityManager` was still installable, so this code worked in a JVM started with
`-Djava.security.manager`. The upgrade silently removed the enforcement while leaving the code — and
the reviewer's confidence — intact.

### Production impact
```text
caller → isAuthorized(userId) → getSecurityManager() == null → return true
→ every caller purges tenant data
```
An authorization bypass with no error, no log and no failed request — the worst possible signature.

### Broken implementation
```java
SecurityManager securityManager = System.getSecurityManager();
if (securityManager == null) {
    return true;
}
```

### Correct implementation
```java
public boolean isAuthorized(Principal caller, String requiredAuthority) {
    if (caller == null) {
        return false; // fail closed
    }
    return caller.authorities().contains(requiredAuthority);
}
```

### Why the solution works
The decision is based on the authenticated caller's authorities, not on a JVM-global mechanism that
no longer exists. It has no dependency on a deprecated platform feature, so it survives the upgrade.

### Trade-offs
Authorization logic now lives in application code and must be maintained and tested there. In a
Spring service you would delegate to Spring Security's `AuthorizationManager` rather than write a
custom guard; the principle — decide from the principal, fail closed — is identical.

### How to detect it
```bash
grep -rn "getSecurityManager\|AccessController" src/main
```
Any surviving call is a defect on Java 25. Confirm at runtime that `System.getSecurityManager()`
returns `null`; there is no flag that can restore it.

### Interview follow-up
> The `SecurityManager` was removed rather than fixed. What did it protect, and where does each of
> those responsibilities belong now?

### Related
- Security issue · JEP 486 · Authorization · Fail-closed design

## Fail-open authorization default

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Basic
**Track:** `java25-boot4` · **Technology:** Authorization design
**Interview frequency:** High · **Production impact:** Critical

**Location:** `SecurityManagerGate.isAuthorized()`

### Problem
When no `SecurityManager` is present the method returns `true`. The absence of a security mechanism
is interpreted as permission rather than as an error. This is the classic fail-open bug: any
misconfiguration grants access instead of denying it.

### Why it happens
The author assumed the `SecurityManager` would always be installed in production, so the `null`
branch was treated as a defensive fallback rather than the primary path. On Java 25 it *is* the
primary path.

### Production impact
Any environment that forgets the security configuration — or runs on a JDK where it is impossible —
silently exposes destructive operations.

### Broken implementation
```java
if (securityManager == null) {
    return true;
}
```

### Correct implementation
```java
if (caller == null) {
    return false;
}
```

### Why the solution works
Authorization defaults to deny. A missing or unknown principal is denied, so a misconfiguration
fails safely and loudly (the operation is refused) instead of silently allowing access.

### Trade-offs
Fail-closed can cause an outage if a legitimate caller is misconfigured. That is the intended
trade-off: a refused admin operation is recoverable, a data purge is not.

### How to detect it
Grep for authorization helpers whose default branch returns `true` or does not throw. Test with a
`null`/anonymous principal and assert denial.

### Interview follow-up
> Give an example where fail-open is the right choice, and one where it never is.

### Related
- Security issue · Fail-closed design · Object-level authorization

## Deprecated AccessController check that never throws

**Type:** Reliability issue · **Severity:** Medium · **Difficulty:** Intermediate
**Track:** `java25-boot4` · **Technology:** `AccessController`, JEP 486
**Interview frequency:** Medium · **Production impact:** Medium

**Location:** `SecurityManagerGate.isAuthorized()`

### Problem
`AccessController.checkPermission(ADMIN_PERMISSION)` is deprecated for removal and, on Java 25, never
throws. The `catch (SecurityException denied)` block can never execute, so the code reads as if it
denies unauthorized callers when it does not.

### Why it happens
`AccessController` was part of the `SecurityManager` stack. With the stack disabled, the check became
a no-op. Deprecation warnings are easy to ignore during an upgrade.

### Production impact
Dead protection is worse than no protection: a reviewer or auditor reading the method believes
permission is enforced, so the gap is not investigated.

### Broken implementation
```java
try {
    AccessController.checkPermission(ADMIN_PERMISSION);
    return true;
} catch (SecurityException denied) {
    return false; // unreachable on Java 25
}
```

### Correct implementation
Delete the call. There is no drop-in replacement: authorization moves to the application, as in the
correct implementation above.

### Why the solution works
Removing dead code removes the false impression of protection. The replacement makes the real policy
visible and testable.

### Trade-offs
None. A deprecated no-op has no value.

### How to detect it
```bash
./gradlew :modules:40-whats-new:compileJava --warning-mode all | grep -i "AccessController\|SecurityManager"
```
Any deprecation warning on a security API during an upgrade is a migration task, not noise.

### Interview follow-up
> Why is a no-op security check more dangerous than a missing one?

### Related
- Reliability issue · Deprecation · JEP 486

## Authorization ignores caller identity

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Intermediate
**Track:** `java25-boot4` · **Technology:** Authorization design, principal model
**Interview frequency:** High · **Production impact:** High

**Location:** `SecurityManagerGate` / `AdminOperationService.purgeTenantData()`

### Problem
`isAuthorized(String userId)` accepts the caller's id and never uses it. The method signature
suggests per-caller authorization, but the implementation is a JVM-global permission check, so it
cannot distinguish one caller from another.

### Why it happens
The `SecurityManager` model grants permissions to *code*, not to *users*. Mapping a user-level
authorization requirement onto it was never correct; it only appeared to work because the
`SecurityManager` was enforced.

### Production impact
Once the mechanism is replaced, a naive fix that keeps the `userId` parameter unused still grants
every authenticated caller the same rights — a broken object-level authorization (IDOR-style) gap.

### Broken implementation
```java
public boolean isAuthorized(String userId) { ... } // userId unused
```

### Correct implementation
```java
public boolean isAuthorized(Principal caller, String requiredAuthority) {
    return caller != null && caller.authorities().contains(requiredAuthority);
}
```

### Why the solution works
The decision is a pure function of the caller's identity and authorities, so it can be tested per
role and cannot be bypassed by omitting a JVM flag.

### Trade-offs
The application now owns the authority model and must keep it consistent with the identity provider.
That is normal, and it is where the requirement actually lives.

### How to detect it
Look for authorization methods whose identity parameter is unused; static analysis flags unused
parameters. Test with two principals that differ only by role and assert different outcomes.

### Interview follow-up
> Why is "the code has permission" not the same as "the user has permission"?

### Related
- Maintainability issue · Object-level authorization · Principal model

## Correct implementation

Package `lab.java25boot4.whatsnew.securitygate`, source
`src/main/java/lab/java25boot4/whatsnew/securitygate/ResourceAuthorizationGuard.java`, tests in
`ResourceAuthorizationGuardTest`.

Walkthrough and trade-offs: [What's New — Solutions](../../../../../docs/tracks/java25-boot4/whats-new/solutions.md#securitymanager-based-authorization).
