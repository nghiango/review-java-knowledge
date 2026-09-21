# Testing Interview Questions: Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline questions in [`modules/12-testing`](../../../topics/testing/questions.md) cover standard test slice annotations, Mockito matchers, and Testcontainers lifecycle.
    These 13 questions focus on testing virtual threads, structured concurrency scopes, RFC 9457 ProblemDetail assertions, and ArchUnit architecture verification under Spring Boot 4 and Java 25.

---

### 1. How does modern Spring testing unify REST endpoint integration assertions?

??? question "Reveal answer"
    In Spring Boot 4 and Spring Framework 7, endpoint testing eliminates the historical dichotomy between `MockMvc` (MVC) and `WebTestClient` (reactive) by standardizing on unified fluent assertion models. Both mock dispatching and live HTTP servers share consistent request specifications and JSON assertion mechanisms, particularly around RFC 9457 error contracts.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q01RestTestClientBasicsExample.java"
        ```

---

### 2. How do virtual threads affect test design and thread lifecycle tracking in Java 25?

??? question "Reveal answer"
    Virtual threads are lightweight, cheap to spawn, and multiplexed over carrier threads. In unit and integration tests, testing code cannot rely on traditional thread identification, fixed-size thread pools, or thread names. Tests must verify observable outcomes, task boundaries, and asynchronous completion rather than thread identities.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q02VirtualThreadTestingBasicsExample.java"
        ```

---

### 3. Why is Thread.sleep considered an anti-pattern in asynchronous and virtual thread test suites?

??? question "Reveal answer"
    `Thread.sleep` induces artificial latency and non-deterministic behavior. Under heavy CI load, fixed sleep durations expire before background threads complete, causing intermittent test failures. In fast environments, sleep unnecessarily delays execution. Tests should instead use poll-based synchronizers like Awaitility that assert conditions immediately upon state transition.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q03ThreadSleepAntiPatternExample.java"
        ```

---

### 4. How do you verify RFC 9457 ProblemDetail payloads in modern integration tests?

??? question "Reveal answer"
    RFC 9457 defines standard fields (`status`, `title`, `detail`, `type`, `instance`) along with custom extension properties. Tests verify both the HTTP status code and the presence and values of these fields in the returned JSON, preventing client-breaking regressions in API error contracts.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q04ProblemDetailTestingAssertionsExample.java"
        ```

---

### 5. How should StructuredTaskScope concurrency workflows be tested deterministically?

??? question "Reveal answer"
    Workflows using `StructuredTaskScope` confine subtasks to a lexical block. When testing, engineers verify that `scope.join()` cleanly coordinates subtasks, that subtask states reflect `SUCCESS` or `FAILED`, and that exceptions thrown by subtasks are properly unhandled or wrapped in `FailedException` to trigger fast failure.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q05StructuredTaskScopeTestingExample.java"
        ```

---

### 6. How does Awaitility facilitate poll-based asynchronous state verification without flaky timeouts?

??? question "Reveal answer"
    Awaitility continuously polls a supplier, lambda, or AssertJ assertion at configured intervals (e.g. every 5ms) until a maximum timeout is reached. As soon as the condition holds true, execution resumes immediately, making tests fast and resilient to CI execution variance.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q06AwaitilityVirtualThreadVerificationExample.java"
        ```

---

### 7. How do JSpecify nullness annotations impact unit and integration test assertions?

??? question "Reveal answer"
    JSpecify `@NullMarked` makes all reference types non-null by default within a package or module. In test code, passing an unexpected null triggers static analysis or IDE warnings, and tests must explicitly test `@Nullable` boundary conditions to verify that null arguments are rejected or handled gracefully.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q07JSpecifyNullnessTestVerificationExample.java"
        ```

---

### 8. How do you unit test declarative @HttpExchange client interfaces in Spring Boot 4?

??? question "Reveal answer"
    Declarative `@HttpExchange` interfaces define contracts without implementation code. Testing them involves either mocking the underlying HTTP client adapter (e.g. Mockito mock or WireMock test server) or registering a test proxy bean that simulates downstream REST responses.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q08HttpExchangeClientTestingMockServerExample.java"
        ```

---

### 9. How do you migrate legacy MockMvc or WebTestClient suites towards modern Spring unified testing patterns?

??? question "Reveal answer"
    Migration begins by identifying legacy tests that hardcode JSON string comparisons or rely on deprecated matchers. Teams convert assertions to use `jsonPath` against standardized RFC 9457 fields, ensure content types use `application/problem+json` for errors, and unify test setups across MVC and reactive controllers.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q09MigrationMockMvcToRestTestClientExample.java"
        ```

---

### 10. How do you refactor brittle legacy test suites that rely on Thread.sleep() for asynchronous verification?

??? question "Reveal answer"
    Refactoring replaces hardcoded sleep intervals with `Awaitility.await().atMost(...).until(...)`. This guarantees that if a background worker completes in 10ms, the test finishes in 10ms instead of waiting for a 500ms sleep, while still permitting generous timeouts (e.g. 2-5 seconds) to survive heavily loaded CI runners.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q10MigrationFlakySleepToAwaitilityExample.java"
        ```

---

### 11. How do you design high-concurrency stress tests for virtual threads without pinning carrier threads?

??? question "Reveal answer"
    Stress tests for virtual threads should spawn thousands of concurrent virtual tasks using `Executors.newVirtualThreadPerTaskExecutor()`. The test must ensure that tasks avoid synchronized blocks containing I/O operations or native calls that could pin the underlying ForkJoinPool carrier threads, and instead use ReentrantLocks or non-blocking primitives.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q11VirtualThreadHighConcurrencyLoadTestExample.java"
        ```

---

### 12. How can ArchUnit enforce Java 25 architectural boundaries and package-info nullness invariants?

??? question "Reveal answer"
    ArchUnit scans compiled bytecode and enforces structural rules, such as verifying that production classes reside in designated packages, do not access test packages, comply with layering conventions, and adhere to JSpecify package-level nullness rules.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q12ArchUnitJava25RulesExample.java"
        ```

---

### 13. Incident scenario: Flaky CI test suites timing out after virtual thread migration.

??? question "Reveal answer"
    Following a migration to virtual threads, a CI build pipeline experienced intermittent failures due to tests using `Thread.sleep` assuming synchronous execution on fixed worker pools. The resolution required replacing `Thread.sleep` with countdown latches and `Awaitility` assertions, and eliminating carrier-thread pinning synchronized methods in test fixtures.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/12-testing/src/examples/java/lab/java25boot4/testing/questions/Q13ScenarioFlakyCiVirtualThreadOutageExample.java"
        ```
