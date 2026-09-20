# Reliability Issues

Failure handling, configuration, deployment and testing weaknesses that reduce dependable service.

## Entries

### Unchecked Optional access

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Basic

Calling `Optional.get()` without proving presence converts expected absence into a context-free
exception. Map absence explicitly with `orElseThrow` or transform it with `map`/`flatMap`.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Broad exception catch hides an outage

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

Catching `Exception` around repository access merges absence, invalid input, defects and outages.
Handle only the failure the layer owns; preserve operational failures and their causes.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Structural mutation invalidates iteration

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Basic

Removing through a collection while an enhanced-for Iterator is active changes its modification
count and fails fast. Use iterator removal, `removeIf`, or derive a new immutable result.

### Abrupt JVM Termination Drops In-Flight Tasks

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

Default `server.shutdown: immediate` and unmanaged `ExecutorService` instances forcefully terminate
running worker threads and sever active HTTP connections upon receiving `SIGTERM`. Configure
`server.shutdown: graceful` and manage worker threads via `ThreadPoolTaskExecutor` with
`setWaitForTasksToCompleteOnShutdown(true)` and `setAwaitTerminationSeconds(30)`.

**Appears in:** `modules/05-spring-boot/broken-examples/no-graceful-shutdown`

### Assertions coupled to implementation details

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** JUnit 5, Mockito · **Interview frequency:** High · **Production impact:** High

Verifying call order, call counts and exact argument lists restates the implementation instead of the
observable outcome. The test then fails on behaviour-preserving refactors and still passes when the
business result is wrong. Assert the returned value or a captured side effect.

**Appears in:** `modules/12-testing/broken-examples/asserting-implementation-not-behaviour`

### Mocking a value collaborator removes the rule under test

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Mockito · **Interview frequency:** High · **Production impact:** High

Doubling a pure, in-process collaborator and stubbing its result deletes the code path the test
claims to verify; the test can only prove that the stub was called. Use the real collaborator and
reserve test doubles for I/O boundaries, time and randomness.

**Appears in:** `modules/12-testing/broken-examples/asserting-implementation-not-behaviour`

### Mocking the HTTP boundary hides contract drift

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Mockito, WireMock · **Interview frequency:** High · **Production impact:** High

Doubling the outbound HTTP client makes the suite green while the real request path and JSON field
names are never exercised, so a renamed endpoint or field ships undetected. Replace the boundary with
a stub server (WireMock) and assert the recorded request and the deserialized response.

**Appears in:** `modules/12-testing/broken-examples/mocking-away-the-integration`

### No assertion on the wire contract

**Type:** Testing issue · **Severity:** Medium · **Difficulty:** Intermediate

**Technology:** WireMock, Spring `RestClient` · **Interview frequency:** Medium · **Production impact:** Medium

A test that stubs the client cannot verify the URL path, the `Accept` header or that the response body
deserializes into the expected type. Against a stub server, assert `verify(getRequestedFor(...))` with
the header and let Jackson bind the real payload.

**Appears in:** `modules/12-testing/broken-examples/mocking-away-the-integration`

### No timeout or error-path coverage on an outbound call

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Spring `RestClient` · **Interview frequency:** High · **Production impact:** High

An outbound HTTP client built without connect or read timeouts, tested only on the happy path, blocks a
request thread for the container's default and surfaces a 5xx or malformed body as a raw exception. Set
explicit timeouts and test the failure paths (timeout, 5xx, unparseable body).

**Appears in:** `modules/12-testing/broken-examples/mocking-away-the-integration`

## Related

- [Issue catalogue](index.md)
