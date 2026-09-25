# Solutions: Java 25 & Spring Boot 4 Production Implementations

!!! info "Delta from baseline"
    Baseline modules provide production solutions on Java 21 and Spring Boot 3.5.
    This page details the correct implementations resolving the review targets in this module, leveraging **Java 25 LTS** language/runtime capabilities and **Spring Boot 4.0** configuration contracts.

---

## 1. Safe Off-Heap Buffer: `ArenaMemoryBuffer`

To eliminate deprecated `sun.misc.Unsafe` methods and off-heap memory corruption risks, `ArenaMemoryBuffer` utilizes the Foreign Function & Memory API (`java.lang.foreign`).

### Implementation

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/src/main/java/lab/java25boot4/whatsnew/unsafebuffer/ArenaMemoryBuffer.java"
```

### Why this fixes the issues

1. **JEP 498 Deprecation Removed (Issue 1):** Replaces private reflective access to `theUnsafe` with standard, supported `java.lang.foreign` types (`Arena`, `MemorySegment`, `ValueLayout.JAVA_LONG`). Zero compilation warnings are generated on Java 25.
2. **Deterministic Resource Release (Issue 2):** Implements `AutoCloseable`. The buffer owns an `Arena.ofConfined()` instance. Closing the arena immediately releases all allocated native memory pages.
3. **Hardware Bounds Checking (Issue 3):** Spatial bounds checking is enforced:
   ```java
   private void checkIndex(int index) {
       if (index < 0 || index >= capacity) {
           throw new IndexOutOfBoundsException("index " + index + " out of bounds for capacity " + capacity);
       }
   }
   ```
   Even if omitted, `MemorySegment.get()` and `set()` execute intrinsic JVM bounds checking against segment byte length, preventing buffer overruns.
4. **Encapsulation Preserved (Issue 4):** No reflective access to JVM-internal fields is required. The code conforms to Java strong encapsulation.

### Trade-offs
- **Allocation Cost:** Allocating native memory via `Arena.allocate()` incurs an OS page allocation cost. For ultra-short-lived buffers within a single method, allocate once per task or use a bounded arena buffer pool.
- **Thread Confinement:** `Arena.ofConfined()` restricts buffer reads/writes to the thread that created the arena. If buffer instances must cross virtual thread boundaries, use `Arena.ofShared()` with appropriate synchronization.

---

## 2. Fail-Closed Authorization: `ResourceAuthorizationGuard`

To replace the removed `SecurityManager` (JEP 486), authorization logic is refactored into an explicit, domain-level principal guard adhering to the principle of least privilege and fail-closed design.

### Implementation

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/src/main/java/lab/java25boot4/whatsnew/securitygate/Caller.java"
```

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/src/main/java/lab/java25boot4/whatsnew/securitygate/ResourceAuthorizationGuard.java"
```

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/src/main/java/lab/java25boot4/whatsnew/securitygate/AccessDeniedException.java"
```

### Why this fixes the issues

1. **Zero Dependency on Disabled SecurityManager (Issue 1):** Removes all calls to `System.getSecurityManager()`. Authorization does not rely on a JVM-wide state that returns `null` on Java 25.
2. **Fail-Closed Default (Issue 2):** If `caller == null`, `isAuthorized()` immediately returns `false`:
   ```java
   if (caller == null) {
       return false;
   }
   ```
   Missing or anonymous callers are denied by default.
3. **Elimination of Dead AccessController Calls (Issue 3):** Eliminates deprecated no-op `AccessController.checkPermission()` calls, making authorization behavior transparent and deterministic.
4. **Per-Caller Identity & Authorities (Issue 4):** Authorization inspects the authenticated `Caller` instance and its explicit authorities set. In production, this integrates with Spring Security's `Authentication` or `@PreAuthorize`.

### Trade-offs
- **Context Passing:** The `Caller` principal must be explicitly passed into method calls or stored in a `ScopedValue` context. In Spring services, Spring Security automatically provides this via `SecurityContextHolder` or `AuthorizationManager`.

---

## 3. Validated Immutable Configuration: `MessagingSettings`

To prevent silent configuration drops during Spring Boot 4 upgrades, configuration properties are modeled as immutable records backed by Jakarta Validation.

### Implementation

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/src/main/java/lab/java25boot4/whatsnew/propertybinding/MessagingSettings.java"
```

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/src/main/java/lab/java25boot4/whatsnew/propertybinding/OutboundPublisher.java"
```

### Why this fixes the issues

1. **Fail-Fast on Missing / Renamed Keys (Issue 1 & 2):** Adding `@Validated`, `@NotBlank`, and `@Positive` ensures that if a property name changes in Boot 4, the application fails immediately during startup with a `ConfigurationPropertiesBindException`. No unconfigured bean enters the runtime container.
2. **No Silent Notification Drops (Issue 3):** `OutboundPublisher` receives fully populated, non-null settings:
   ```java
   if (payload == null || payload.isBlank()) {
       throw new IllegalArgumentException("payload must not be blank");
   }
   ```
   Notifications are published reliably to the verified destination.
3. **Immutable Constructor Binding (Issue 4):** Using a Java record eliminates mutable JavaBean setters. Properties are immutable once bound, eliminating partial-state concurrency hazards.

### Trade-offs
- **Strict Startup Requirements:** If a secondary feature is truly optional in certain environments (e.g. local vs production), it must be explicitly declared as `Optional<String>` or provided with a default value. Required properties cannot be omitted.

---

## 4. Feature Tour Harness: `FeatureTour`

The `FeatureTour` class serves as an executable entry point demonstrating Stream Gatherers, Foreign Memory segments, Scoped Values, and the migration checklist.

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/src/main/java/lab/java25boot4/whatsnew/FeatureTour.java"
```

### Key Demonstrations
- **Stream Gatherers:** Employs `Gatherers.windowFixed(3)` to batch numbers into lists of 3, and `Gatherers.scan()` to compute cumulative running sums.
- **Foreign Memory Arena:** Demonstrates safe native page allocation, value assignment via `ValueLayout.JAVA_LONG`, and auto-closing.
- **Scoped Values:** Binds an immutable `REQUEST_ID` and accesses it within a runnable closure without `ThreadLocal` cleanup boilerplate.
