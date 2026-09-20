# Testing Exercises

Hands-on rewrites of the module's broken suites. Each exercise starts from a real review target and
asks for the production-grade replacement; attempt it before expanding the solution. Every solution is
the module's own checked code, so you can run it with the commands in [Tests](tests.md).

## Exercise 1: Turn the mocked inventory client into a WireMock contract test

### Task

`OrderFulfilmentServiceTest` mocks `InventoryClient`, so nothing checks the request the client sends or
the payload it binds. Rewrite the coverage so that a change to the inventory API's URL, its `Accept`
header or its JSON field names fails the suite.

### Requirements

- Exercise the service against a stub HTTP server, not a mocked client.
- Assert the recorded request path and the `Accept` header.
- Bind a real JSON body into a typed response instead of a hand-built map.
- Configure explicit connect and read timeouts on the real `RestClient`.
- Cover both the "enough stock" and "not enough stock" outcomes.

### Hints

- `WireMockExtension` with a dynamic port gives you a base URL; point the real `RestClient` at it.
- `verify(getRequestedFor(urlEqualTo(...)).withHeader(...))` asserts the request the client actually
  sent.
- The field the service reads must be the one the API returns, bound by Jackson — not a key the test
  invented.

??? success "Reveal solution"

    **Why this works.** A stub server speaks the real protocol, so the client's path, header and body
    binding are all exercised: a renamed endpoint or field fails the contract test instead of the mocks
    staying green. The service is written against `InventoryResponse.available()`, so the wire field
    name lives only in the client, and the timeout policy is part of the tested configuration because
    the test uses a real `RestClient`.

    The review target to replace:

    ??? example "The mocked test"

        ```java
        --8<-- "modules/12-testing/broken-examples/mocking-away-the-integration/OrderFulfilmentServiceTest.java"
        ```

    The reference contract test and the typed client it exercises:

    ??? example "The contract test"

        ```java
        --8<-- "modules/12-testing/src/integrationTest/java/lab/testing/fulfilment/OrderFulfilmentWireMockIT.java"
        ```

    ??? example "The typed client"

        ```java
        --8<-- "modules/12-testing/src/main/java/lab/testing/fulfilment/InventoryClient.java"
        ```

        ```java
        --8<-- "modules/12-testing/src/main/java/lab/testing/fulfilment/InventoryResponse.java"
        ```

    **Extension.** Add a case that stubs a delayed or 5xx response and asserts the failure is bounded by
    the configured timeout rather than hanging the caller. The stub shape is the same; only the
    `willReturn(aResponse().withStatus(503))` changes.

## Exercise 2: Write a deterministic Awaitility test for the async report job

### Task

`AsyncReportJobTest` waits for a background report with `Thread.sleep(500)` and an unbounded polling
loop, and never exercises a report whose generation fails. Rewrite it so it is fast, bounded and proves
the `RUNNING` → `COMPLETED` transition and the failure path.

### Requirements

- Never sleep for a fixed delay; wait on the status the test asserts on.
- Bound every wait with `atMost` so a hang fails with the state it observed instead of killing the build.
- Prove the work ran asynchronously (the report is `RUNNING` before the work is released).
- Make the work throw through a seam and assert the report reaches `FAILED`.

### Hints

- The job already accepts an injectable work function; gate it with a `CountDownLatch` the test
  releases.
- `await().atMost(...).pollInterval(...).untilAsserted(...)` returns as soon as the condition holds.
- A single-thread executor lets you control exactly when the worker can pick up a report.

??? success "Reveal solution"

    **Why this works.** The test waits on the condition it cares about, so it is fast when the work is
    fast and still correct when the work is slow; the bound turns a hang into an ordinary
    `ConditionTimeoutException` that reports the last observed status. Releasing a latch makes the
    `RUNNING` → `COMPLETED` transition deterministic instead of a race, and the throwing-work seam
    executes the production `catch` so the `FAILED` branch is verified.

    The review target to replace:

    ??? example "The sleep-based test"

        ```java
        --8<-- "modules/12-testing/broken-examples/sleep-based-async-assertions/AsyncReportJobTest.java"
        ```

    The reference test and the lifecycle it asserts:

    ??? example "The deterministic test"

        ```java
        --8<-- "modules/12-testing/src/test/java/lab/testing/async/AsyncReportJobTest.java"
        ```

    ??? example "The lifecycle"

        ```java
        --8<-- "modules/12-testing/src/main/java/lab/testing/async/ReportStatus.java"
        ```

## Exercise 3: Replace the shared fixture with per-test builders

### Task

`OrderTotalsTest` works with a single static `OrderFixture.ORDERS`, one test appends to it and another
asserts the accumulated size, and `@TestMethodOrder` is what keeps the class green. Rewrite the suite so
every test builds its own data and the class passes alone, in any order and in parallel.

### Requirements

- Remove the shared mutable fixture entirely; no `static` mutable field may remain.
- Give each test a fluent builder that returns a fresh order per call.
- Delete `@TestMethodOrder`/`@Order` and prove each method passes alone (`--tests 'Class.method'`).
- Keep the value types immutable so a shared order still cannot be mutated.

### Hints

- `OrderTestData.anOrder(id).withLine(sku, price, qty).build()` is the whole fixture.
- `Order` already takes a defensive `List.copyOf` of its lines.
- A class with no shared state needs no order annotation; run it with `MethodOrderer.Random` to confirm.

??? success "Reveal solution"

    **Why this works.** The data a test asserts on is created inside that test and is reachable from
    nowhere else, so the test has no input other than its own literals and no output anyone else can
    observe. With no shared state there is nothing for an order to protect: the class has no
    `@TestMethodOrder` and each method passes alone, in any order and in parallel.

    The review target to replace:

    ??? example "The shared fixture and its suite"

        ```java
        --8<-- "modules/12-testing/broken-examples/shared-mutable-test-fixtures/OrderFixture.java"
        ```

        ```java
        --8<-- "modules/12-testing/broken-examples/shared-mutable-test-fixtures/OrderTotalsTest.java"
        ```

    The reference builder and suite:

    ??? example "The builder and the independent suite"

        ```java
        --8<-- "modules/12-testing/src/test/java/lab/testing/orders/OrderTestData.java"
        ```

        ```java
        --8<-- "modules/12-testing/src/test/java/lab/testing/orders/OrderTotalsTest.java"
        ```

    ??? example "The immutable value types it builds"

        ```java
        --8<-- "modules/12-testing/src/main/java/lab/testing/orders/Order.java"
        ```

        ```java
        --8<-- "modules/12-testing/src/main/java/lab/testing/orders/OrderLine.java"
        ```

## Exercise 4: Make the sequence allocator suite order-independent

### Task

`SequenceAllocator` keeps its counter in a static field and the suite asserts absolute values that only
line up in the declared method order. Move the counter into the instance and rewrite the suite so it is
order-independent and parallel-safe.

### Requirements

- Keep the counter in an instance field seeded from the constructor's `start` argument.
- Assert consequences of the calls each test makes, not the method's position in the class.
- Prove two allocators with the same start value advance independently.
- Remove `@TestMethodOrder` and confirm the class passes with `MethodOrderer.Random` and with parallel
  execution enabled.

### Hints

- The constructor already receives `start`; the fix is to seed the instance counter from it.
- `next_twoAllocatorsWithSameStart_doNotShareState` is the test that pins the property.
- An exhausted counter should fail, not wrap to a negative value.

??? success "Reveal solution"

    **Why this works.** The counter is instance state, so two allocators created with the same value
    advance independently and nothing one instance does is observable by another. Expected values are
    consequences of the calls the test itself makes, so every method passes alone, in any order, twice
    in the same JVM and under parallel execution — and the class needs no `@TestMethodOrder`.

    The review target to replace:

    ??? example "The static counter and the ordered suite"

        ```java
        --8<-- "modules/12-testing/broken-examples/order-dependent-test-suite/SequenceAllocator.java"
        ```

        ```java
        --8<-- "modules/12-testing/broken-examples/order-dependent-test-suite/SequenceAllocatorTest.java"
        ```

    The reference allocator and suite:

    ??? example "The instance-state allocator and the independent suite"

        ```java
        --8<-- "modules/12-testing/src/main/java/lab/testing/sequence/SequenceAllocator.java"
        ```

        ```java
        --8<-- "modules/12-testing/src/test/java/lab/testing/sequence/SequenceAllocatorTest.java"
        ```

## Related

- [Solutions](solutions.md)
- [Tests](tests.md)
- [Code review](code-review.md)
- [Production](production.md)
- [Concepts](concepts.md)
