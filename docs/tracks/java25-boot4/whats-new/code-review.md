# Code Review: Java 25 & Spring Boot 4 Upgrade Pull Requests

!!! info "Delta from baseline"
    Baseline modules evaluate core Java, concurrency, database, and Spring MVC pull requests.
    This page presents three real-world code review scenarios specifically encountered when upgrading services to **Java 25 LTS** and **Spring Boot 4.0**. Each example compiles cleanly, but contains fatal defects in security, reliability, or memory safety.

---

## Review Target 1: Unsafe Off-Heap Buffer

### Context
A platform engineering team is optimizing off-heap message serialization for a high-throughput messaging gateway. The author submits an off-heap buffer implementation using `sun.misc.Unsafe`, claiming low-latency performance with zero garbage collection overhead.

### Code under review

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/broken-examples/unsafe-offheap-buffer/UnsafeOffHeapBuffer.java"
```

### Review prompt
1. What will happen to `sun.misc.Unsafe` memory-access methods in Java 25 and future releases?
2. What happens if an exception is thrown after memory allocation, or if the caller fails to invoke `close()`?
3. How are memory read/write boundaries validated? What happens if an index is negative or exceeds capacity?
4. How should modern Java 25 code access off-heap native memory safely?

??? warning "Reveal issues"

    ### Annotated code

    ```java
    package lab.java25boot4.whatsnew.broken.unsafebuffer;

    import java.lang.reflect.Field;
    import sun.misc.Unsafe;

    public class UnsafeOffHeapBuffer {

        // Deployment issue: sun.misc.Unsafe memory access is deprecated for removal (JEP 498).
        // On Java 25 every call below warns; a future release will remove it entirely.
        // Maintainability issue: Unsafe is reachable only through the private `theUnsafe` field
        // via reflection, defeating module encapsulation and breaking when internals move.
        private static final Unsafe UNSAFE = loadUnsafe();

        private final long address;
        private final int capacity;

        public UnsafeOffHeapBuffer(int capacity) {
            this.capacity = capacity;
            // Resource leak issue: raw native memory is allocated with no owner. If the constructor
            // succeeds but the caller never calls close(), or an exception escapes between here and
            // close(), the memory is leaked for the lifetime of the process.
            this.address = UNSAFE.allocateMemory((long) capacity * Long.BYTES);
        }

        // Memory issue: index is not bounds-checked, so set(capacity, v) or set(-1, v) writes
        // outside the allocation and corrupts unrelated native memory.
        public void set(int index, long value) {
            UNSAFE.putLong(address + (long) index * Long.BYTES, value);
        }

        // Memory issue: get() has the same missing bounds check, so a read can return data from
        // outside the buffer instead of failing.
        public long get(int index) {
            return UNSAFE.getLong(address + (long) index * Long.BYTES);
        }

        public int capacity() {
            return capacity;
        }

        // Resource leak issue: close() is not idempotent and is not tied to a try-with-resources
        // scope — the class does not implement AutoCloseable, so the compiler cannot enforce release.
        public void close() {
            UNSAFE.freeMemory(address);
        }

        private static Unsafe loadUnsafe() {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return (Unsafe) field.get(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("sun.misc.Unsafe is not available", e);
            }
        }
    }
    ```

    ### Issues summary

    | # | Category | Severity | Location | Summary |
    |---|---|---|---|---|
    | 1 | Deployment issue | High | `UnsafeOffHeapBuffer` field | `sun.misc.Unsafe` memory access is deprecated for removal (JEP 498) |
    | 2 | Resource leak issue | Critical | `UnsafeOffHeapBuffer.close()` | Native memory has no `try-with-resources` owner; leaked on any abnormal path |
    | 3 | Memory issue | Critical | `set()` / `get()` | No bounds check — writes and reads outside the allocation corrupt process memory |
    | 4 | Maintainability issue | Medium | `loadUnsafe()` | JDK-internal field reached by reflection; fragile and encapsulation-breaking |

    ### Root Cause Analysis & Fix
    - **Deprecation Warning:** Under JEP 498, calling `allocateMemory` emits runtime warnings. Future Java releases remove these entry points entirely.
    - **Memory Safety:** Replace raw pointers with the Foreign Function & Memory API (`java.lang.foreign.Arena` and `MemorySegment`).
    - **Deterministic Release:** Implement `AutoCloseable` backed by `Arena.ofConfined()`, guaranteeing that `close()` immediately releases native pages.
    - See the tested fix in [`ArenaMemoryBuffer.java`](solutions.md#1-safe-off-heap-buffer-arenamemorybuffer).

---

## Review Target 2: SecurityManager-Based Authorization

### Context
During a Java 25 migration, an internal administrative operation service is reviewed. The gate checks authorization before executing tenant data purges. The author notes that "the legacy code compiled with zero warnings and continues to pass existing tests."

### Code under review

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/broken-examples/security-manager-authorization/SecurityManagerGate.java"
```

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/broken-examples/security-manager-authorization/AdminOperationService.java"
```

### Review prompt
1. What does `System.getSecurityManager()` return in Java 25 (JEP 486)?
2. What is the execution flow of `isAuthorized(userId)` when `securityManager == null`?
3. Does `AccessController.checkPermission` throw security exceptions on Java 25?
4. How is caller identity evaluated in this gate?

??? warning "Reveal issues"

    ### Annotated code

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

    ### Issues summary

    | # | Category | Severity | Location | Summary |
    |---|---|---|---|---|
    | 1 | Security issue | Critical | `SecurityManagerGate.isAuthorized()` | `SecurityManager` is permanently disabled on Java 25 — the gate is a no-op |
    | 2 | Security issue | Critical | `SecurityManagerGate.isAuthorized()` | Fail-open default: a missing security mechanism is treated as "allowed" |
    | 3 | Reliability issue | Medium | `SecurityManagerGate.isAuthorized()` | `AccessController.checkPermission` is deprecated and never throws — dead protection |
    | 4 | Maintainability issue | High | `SecurityManagerGate` / `AdminOperationService` | Authorization ignores caller identity; policy is JVM-global instead of per-request |

    ### Root Cause Analysis & Fix
    - **Total Authorization Bypass:** On Java 25, `System.getSecurityManager()` is hardcoded to return `null`. The `if (securityManager == null) return true;` branch executes for 100% of requests, granting full admin access to unauthenticated callers.
    - **Dead Code:** `AccessController.checkPermission` does nothing in Java 25.
    - **Fail-Closed Principal Design:** Authorization must inspect the caller's authenticated identity and assigned authorities, defaulting to deny.
    - See the tested fix in [`ResourceAuthorizationGuard.java`](solutions.md#2-fail-closed-authorization-resourceauthorizationguard).

---

## Review Target 3: Silently Renamed Configuration Property

### Context
A microservice upgrading to Spring Boot 4 publishes outbound events to a messaging broker. In `application.yml`, the team updated other properties but left `notifications.outbound` intact. The service starts cleanly in canary, with health checks passing.

### Code under review

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/broken-examples/boot4-renamed-property-silent/LegacyMessagingSettings.java"
```

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/broken-examples/boot4-renamed-property-silent/OutboundNotifier.java"
```

### Review prompt
1. If a configuration property was renamed in Spring Boot 4, what happens during relaxed binding?
2. Why did the application start cleanly without throwing an exception or logging an error?
3. What happens in `OutboundNotifier.publish()` when `settings.getTopic()` returns `null`?
4. How do you design configuration properties classes to guarantee fail-fast startup behavior?

??? warning "Reveal issues"

    ### Annotated code

    ```java
    package lab.java25boot4.whatsnew.broken.propertybinding;

    import org.springframework.boot.context.properties.ConfigurationProperties;

    // Configuration issue: the prefix and key names were inherited from Boot 3.5. Where a key was
    // renamed or removed in Boot 4, binding simply finds nothing and leaves the field at its default
    // (null / 0). Startup succeeds — there is no error to notice.
    @ConfigurationProperties(prefix = "notifications.outbound")
    public class LegacyMessagingSettings {

        private String endpoint;
        private String topic;
        private int batchSize;

        // Reliability issue: mutable JavaBean setters mean the bean exists in a half-bound state and
        // is shared before binding completes. Nothing rejects an instance that was never populated.
        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
    ```

    ```java
    package lab.java25boot4.whatsnew.broken.propertybinding;

    public class OutboundNotifier {

        private final LegacyMessagingSettings settings;

        public OutboundNotifier(LegacyMessagingSettings settings) {
            this.settings = settings;
        }

        // Reliability issue: a missing topic is treated as "nothing to do" rather than a
        // configuration failure, so the pipeline drops every notification.
        // Observability issue: the drop is silent — no log, no metric, no health signal. The canary
        // looks healthy while messages disappear.
        public String publish(String payload) {
            if (settings.getTopic() == null) {
                return "skipped";
            }
            return "published "
                    + payload
                    + " to "
                    + settings.getTopic()
                    + " (batch="
                    + settings.getBatchSize()
                    + ")";
        }
    }
    ```

    ### Issues summary

    | # | Category | Severity | Location | Summary |
    |---|---|---|---|---|
    | 1 | Configuration issue | High | `LegacyMessagingSettings` | Boot 4 binding keys no longer match; fields stay null/0 with no error |
    | 2 | Reliability issue | High | `LegacyMessagingSettings` | No `@Validated` / `@NotNull` — a misconfigured app starts successfully |
    | 3 | Observability issue | High | `OutboundNotifier.publish()` | Missing configuration drops messages silently — no log, metric or health signal |
    | 4 | Reliability issue | Medium | `LegacyMessagingSettings` | Mutable JavaBean binding exposes a half-bound object; a record is validated at construction |

    ### Root Cause Analysis & Fix
    - **Silent Data Loss:** Because `topic` is `null`, `publish()` drops notifications without error or logging.
    - **Unenforced Configuration Invariants:** Without `@Validated` and Jakarta Validation annotations, Spring Boot relaxed binding does not check for mandatory keys.
    - **Immutable Record Binding:** Replace mutable JavaBeans with Java records annotated with `@Validated`, `@NotBlank`, and `@Positive`.
    - See the tested fix in [`MessagingSettings.java`](solutions.md#3-validated-immutable-configuration-messagingsettings).
