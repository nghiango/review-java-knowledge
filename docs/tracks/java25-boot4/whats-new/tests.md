# Tests: Java 25 & Spring Boot 4 Verification Suite

!!! info "Delta from baseline"
    Baseline modules verify Java 21 functionality and Spring Boot 3.5 slices.
    This page explains the automated test suite in Module 40, verifying memory safety invariants under the **Foreign Function & Memory API**, fail-closed behavior for **modern authorization guards**, and startup validation for **Spring Boot 4 configuration properties**.

---

## 1. Test Architecture

The module 40 test suite is located in `src/test/java/lab/java25boot4/whatsnew/`:

```text
src/test/java/lab/java25boot4/whatsnew/
├── unsafebuffer/
│   └── ArenaMemoryBufferTest.java          # Memory bounds, liveness, and auto-close
├── securitygate/
│   └── ResourceAuthorizationGuardTest.java # Fail-closed security and authority checks
└── propertybinding/
    └── MessagingSettingsTest.java          # Jakarta validation and property invariants
```

---

## 2. Tested Invariants

### 1. Memory Safety & AutoCloseable Liveness (`ArenaMemoryBufferTest`)

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/src/test/java/lab/java25boot4/whatsnew/unsafebuffer/ArenaMemoryBufferTest.java"
```

**What it proves:**
- **Value Round-Tripping:** Long values written at valid indices are retrieved accurately without bit corruption.
- **Strict Bounds Checking:** Accessing indices outside $[0, \text{capacity} - 1]$ throws `IndexOutOfBoundsException` rather than crashing the JVM or reading process memory.
- **Immediate Resource Reclamation:** Closing the buffer transitions the owning arena scope to `CLOSED`; subsequent accesses throw `IllegalStateException`.
- **Pre-Condition Validation:** Attempting to construct a buffer with zero or negative capacity throws `IllegalArgumentException`.

---

### 2. Fail-Closed Authorization Guard (`ResourceAuthorizationGuardTest`)

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/src/test/java/lab/java25boot4/whatsnew/securitygate/ResourceAuthorizationGuardTest.java"
```

**What it proves:**
- **Authority Enforcement:** Callers possessing the required permission string are granted access; callers lacking the authority are denied.
- **Fail-Closed on Missing Caller:** Passing a `null` caller returns `false` and `checkAuthorized()` throws `AccessDeniedException`, preventing fail-open bypasses.
- **Defensive Principal Invariants:** Callers constructed with blank usernames are rejected at instantiation time; null authority collections default safely to empty sets.

---

### 3. Configuration Property Validation (`MessagingSettingsTest`)

```java
--8<-- "tracks/java25-boot4/modules/40-whats-new/src/test/java/lab/java25boot4/whatsnew/propertybinding/MessagingSettingsTest.java"
```

**What it proves:**
- **Validation Acceptance:** Records with valid endpoints, topics, and positive batch sizes validate cleanly with zero constraint violations.
- **Fail-Fast on Missing Topic:** If the topic is blank or missing, Jakarta Validation registers a violation on the `topic` field.
- **Batch Size Bounds:** Non-positive batch sizes ($\le 0$) fail validation, preventing downstream messaging loops from hanging or failing to flush.

---

## 3. Running the Tests

Execute the unit tests using the Gradle toolchain:

```bash
cd tracks/java25-boot4
../../gradlew :modules:40-whats-new:test
```

Run test suite along with broken examples compilation:

```bash
cd tracks/java25-boot4
../../gradlew :modules:40-whats-new:test :modules:40-whats-new:compileBrokenExamples :modules:40-whats-new:compileExamples
```
