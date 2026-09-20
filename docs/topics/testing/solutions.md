# Testing Solutions

The production-grade counterpart of each code review exercise: what the correct suite does
differently, why that fixes each documented issue, and the trade-off it introduces. Every snippet is
included from `modules/12-testing`, so the walkthrough cannot drift from the code.

## Issue-to-fix matrix

| Exercise | What the broken suite proves instead | Correct implementation | Main trade-off |
|---|---|---|---|
| Asserting implementation not behaviour | That `CheckoutService` calls a mocked calculator in order — never that the customer is charged the right amount | Real `PricingCalculator`, injectable `PaymentGateway`, `checkout` returns the charged amount | Behaviour assertions need an observable result; the boundary had to change to provide one |
| Mocking away the integration | That a mocked `InventoryClient` returns a hand-written map — never the request path, the `Accept` header or the JSON field names | Typed `InventoryResponse` + `InventoryClient` over an injected `RestClient`, covered by `OrderFulfilmentWireMockIT` | A stub server is slower and needs a port and lifecycle; keep the client thin and fake it for domain unit tests |
| Shared mutable test fixtures | That one static fixture happens to hold what each test expects after the previous test mutated it | Immutable `Order`/`OrderLine`/`OrderTotals` with per-test `OrderTestData` builders | Each test pays a few lines for its own data instead of sharing one list |
| Sleep-based async assertions | That the background work finished within a fixed delay on this machine — never that a failure is distinguishable from a slow run | `AsyncReportJob` with an injectable work seam and a `QUEUED`/`RUNNING`/`COMPLETED`/`FAILED` lifecycle, asserted with bounded Awaitility waits | Waiting is bounded by a chosen budget; a gate couples the test to the seam that exposes it |
| Embedded substitute hides Postgres semantics | That a case-insensitive map behaves like a case-sensitive unique index on PostgreSQL | `AccountService` normalises the email; `AccountRepositoryIT` asserts the semantics against a real PostgreSQL container | Docker-backed tests are slower and need the container infrastructure |
| Order-dependent test suite | That the method's position in the class matches the absolute value it asserts | `SequenceAllocator` with instance state, and a suite that builds the allocator it asserts on | More objects per test; a genuinely process-wide sequence must be modelled explicitly |

---

## Asserting implementation not behaviour

=== "Broken"

    ```java
    --8<-- "modules/12-testing/broken-examples/asserting-implementation-not-behaviour/CheckoutService.java"
    ```

    ```java
    --8<-- "modules/12-testing/broken-examples/asserting-implementation-not-behaviour/CheckoutServiceTest.java"
    ```

=== "Correct"

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/pricing/Money.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/pricing/LineItem.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/pricing/PricingCalculator.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/pricing/PaymentGateway.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/pricing/CheckoutService.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/test/java/lab/testing/pricing/CheckoutServiceBehaviourTest.java"
    ```

### Why it works

1. **The rule under test runs for real.** `PricingCalculator` is pure and in-process, so the test uses
   the real instance; only `PaymentGateway` — the remote boundary — is doubled. A broken percentage or
   rounding rule changes the asserted amount and fails the test.
2. **The boundary has an observable result.** `checkout` returns the amount actually charged, so the
   test can assert money instead of inspecting internal calls, and the wrong-charge defect is caught by
   `checkout_mixedQuantities_chargesDiscountedSubtotal`.
3. **Assertions describe the contract, not the control flow.** No `verify(times(...))` and no `InOrder`
   remain, so an extract-method or reorder refactor cannot fail the test.
4. **The uncovered branches are covered.** Empty basket, full discount, negative quantity and a
   discount outside 0..100 are each asserted, and the gateway is proved untouched
   (`verifyNoInteractions`) where there is nothing to charge.

### Trade-offs

Returning the charged amount couples the caller to it — usually desirable for a checkout, but it has
to stay consistent with the payment provider's asynchronous settlement. A zero total is deliberately
not sent to the gateway: there is nothing to charge, and a zero-value payment would only add noise.
Where a boundary genuinely has no return value, assert a captured side effect instead of an internal
call.

---

## Mocking away the integration

=== "Broken"

    ```java
    --8<-- "modules/12-testing/broken-examples/mocking-away-the-integration/InventoryClient.java"
    ```

    ```java
    --8<-- "modules/12-testing/broken-examples/mocking-away-the-integration/OrderFulfilmentService.java"
    ```

    ```java
    --8<-- "modules/12-testing/broken-examples/mocking-away-the-integration/OrderFulfilmentServiceTest.java"
    ```

=== "Correct"

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/fulfilment/InventoryResponse.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/fulfilment/InventoryClient.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/fulfilment/OrderFulfilmentService.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/integrationTest/java/lab/testing/fulfilment/OrderFulfilmentWireMockIT.java"
    ```

### Why it works

1. **A stub server speaks the real protocol.** `OrderFulfilmentWireMockIT` runs the service against a
   WireMock instance, so the client's request path (`/inventory/{sku}`), its `Accept` header and the
   JSON body Jackson binds are all exercised; a renamed endpoint or field fails the test.
2. **The request itself is asserted.** `verify(getRequestedFor(urlEqualTo("/inventory/A-1"))
   .withHeader("Accept", equalTo(MediaType.APPLICATION_JSON_VALUE)))` turns the wire contract into an
   assertion rather than an implicit assumption.
3. **The transport carries a timeout policy.** The `RestClient` is built with a
   `SimpleClientHttpRequestFactory` with explicit connect and read timeouts, so a stalled inventory
   service fails fast instead of holding the request thread.
4. **The wire format is encapsulated.** `InventoryClient.lookup` returns a typed `InventoryResponse`;
   the domain is written against `response.available()`, so a field rename is a single-file change and
   a type mismatch fails at the boundary that owns the contract.

### Trade-offs

WireMock is a real network hop: it is slower and heavier than a mock and needs a port and lifecycle
management, so it is reserved for the boundary. Keep the client thin (build the request, bind the
response) and unit-test the surrounding domain logic with a fake client. Asserting the exact path
couples the test to the API version — which is the point for a contract test, but assert only the
fields genuinely part of the contract. Timeouts must come from the dependency's SLO, with a bounded
retry rather than a longer timeout.

---

## Shared mutable test fixtures

=== "Broken"

    ```java
    --8<-- "modules/12-testing/broken-examples/shared-mutable-test-fixtures/OrderFixture.java"
    ```

    ```java
    --8<-- "modules/12-testing/broken-examples/shared-mutable-test-fixtures/OrderTotalsTest.java"
    ```

=== "Correct"

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/orders/OrderLine.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/orders/Order.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/orders/OrderTotals.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/test/java/lab/testing/orders/OrderTestData.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/test/java/lab/testing/orders/OrderTotalsTest.java"
    ```

### Why it works

1. **Each test creates the data it asserts on.** `OrderTestData.anOrder(...).withLine(...).build()`
   returns a fresh value per call, so a test's input is visible in the test and reachable from nowhere
   else — no test can observe or change another test's data.
2. **There is no shared mutable state to order around.** The class has no `@TestMethodOrder`, so every
   method passes alone, in any order, when the class runs twice in the same JVM and under parallel
   execution.
3. **Values are immutable.** `Order` takes a defensive `List.copyOf` of its lines and `OrderLine`
   validates in its compact constructor, so even a shared order cannot be mutated; only immutable
   values (`Money`) are shared.
4. **The suite now asserts independence.** `testData_builtTwice_producesEqualIndependentOrders` and
   `order_linesAreImmutable` pin the property the fixture was silently violating.

### Trade-offs

Duplicating data per test looks less tidy than one shared list, and each test pays a few lines for its
own input. Keep that cheap with a builder, and keep the builder dumb: no hidden defaults for data
under assertion, package-private in the test source set, one method per thing the domain actually has.
Some fixtures are genuinely expensive — a started container, a migrated schema, a seeded database;
share the *started resource* with an explicit lifecycle and never let an assertion depend on state one
test wrote for another.

---

## Sleep-based async assertions

=== "Broken"

    ```java
    --8<-- "modules/12-testing/broken-examples/sleep-based-async-assertions/AsyncReportJob.java"
    ```

    ```java
    --8<-- "modules/12-testing/broken-examples/sleep-based-async-assertions/AsyncReportJobTest.java"
    ```

=== "Correct"

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/async/ReportStatus.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/async/AsyncReportJob.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/test/java/lab/testing/async/AsyncReportJobTest.java"
    ```

### Why it works

1. **The test waits on the condition, not on time.** Every assertion polls the status it cares about
   with `await().atMost(WORK_BUDGET).pollInterval(POLL_INTERVAL).untilAsserted(...)`, so the test is
   fast when the work is fast and still correct when the work is slow, and it fails with the status it
   actually observed.
2. **The transition is driven by a signal.** Injecting work gated by a `CountDownLatch` lets the test
   assert `RUNNING` before it releases the work and `COMPLETED` afterwards, so it proves the work ran
   asynchronously instead of guessing from a delay.
3. **The failure path is exercised.** Work that throws reaches `FAILED` instead of leaving the report
   `RUNNING`, which is the branch a caller polling the status depends on.
4. **The bound turns a hang into a failure.** `atMost` makes a report that never finishes an ordinary
   `ConditionTimeoutException` instead of an infinite loop the CI job timeout has to kill.

### Trade-offs

Awaitility hides a polling loop, so a condition that never holds still costs the full `atMost` budget;
keep the bound short (2 s here) and poll the observable state rather than a proxy. A gate couples the
test to the seam that exposes it — where there is no natural seam (a real HTTP call, a database write),
Awaitility polling is the right tool and the suite costs whatever the boundary costs. Never "fix" a
slow test by raising `atMost`: if 2 s is not enough, the work or the assertion is wrong.

---

## Embedded substitute hides Postgres semantics

=== "Broken"

    ```java
    --8<-- "modules/12-testing/broken-examples/embedded-substitute-hides-postgres-semantics/InMemoryAccountRepository.java"
    ```

    ```java
    --8<-- "modules/12-testing/broken-examples/embedded-substitute-hides-postgres-semantics/AccountService.java"
    ```

    ```java
    --8<-- "modules/12-testing/broken-examples/embedded-substitute-hides-postgres-semantics/AccountServiceTest.java"
    ```

=== "Correct"

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/accounts/Account.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/accounts/AccountRepository.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/accounts/AccountService.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/integrationTest/java/lab/testing/accounts/TestingJpaConfiguration.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/integrationTest/java/lab/testing/accounts/AccountRepositoryIT.java"
    ```

### Why it works

1. **The application owns case-insensitive uniqueness.** `AccountService` normalises the email (trim +
   lower-case) before storing or querying it, so the rule is explicit and portable instead of a
   property of whichever store is plugged in.
2. **The constraint is the arbiter.** `Account` declares `uk_accounts_email`, and
   `save_identicalEmailTwice_isRejectedByTheUniqueIndex` proves the database rejects a duplicate with a
   `DataIntegrityViolationException` — the failure the in-memory map could never produce.
3. **The semantics are asserted against the engine that will run them.** `AccountRepositoryIT` proves
   the index is case-sensitive (`save_emailsDifferingOnlyByCase_bothPersist`), that `findByEmail` is
   case-sensitive, and that row order is only guaranteed when requested with `Sort`.
4. **There is only one implementation of the contract.** The hand-written map is deleted, so nothing
   has to be kept in step with the repository interface.

### Trade-offs

Docker-backed tests are slower and need the container infrastructure, and the JPA slice must be wired
with `@AutoConfigureTestDatabase(replace = Replace.NONE)` so Boot cannot swap the container DataSource
for an embedded one. A case-insensitive rule enforced by normalising in the application is portable but
must be applied at every entry point; a `citext` column or a `lower(email)` functional index pushes the
rule into the database — stronger, but PostgreSQL-specific and needing a migration. Relying on the
constraint means the application must translate a persistence exception into its own error contract.

---

## Order-dependent test suite

=== "Broken"

    ```java
    --8<-- "modules/12-testing/broken-examples/order-dependent-test-suite/SequenceAllocator.java"
    ```

    ```java
    --8<-- "modules/12-testing/broken-examples/order-dependent-test-suite/SequenceAllocatorTest.java"
    ```

=== "Correct"

    ```java
    --8<-- "modules/12-testing/src/main/java/lab/testing/sequence/SequenceAllocator.java"
    ```

    ```java
    --8<-- "modules/12-testing/src/test/java/lab/testing/sequence/SequenceAllocatorTest.java"
    ```

### Why it works

1. **The counter is instance state seeded from `start`.** Two allocators created with the same value
   advance independently, which is the property the test relies on when it builds its own allocator.
2. **Expected values are consequences of the calls the test makes.** The test that allocates
   repeatedly from a chosen start asserts the progression those calls produce, instead of asserting
   the method's position in the class.
3. **The class needs no `@TestMethodOrder`.** With no shared state there is nothing for an order to
   protect: every method passes alone, in any order, twice in the same JVM, and under parallel
   execution.
4. **The boundary behaviour is tested too.** `next_twoAllocatorsWithSameStart_doNotShareState` pins the
   independence, and the tests adjacent to `Long.MAX_VALUE` prove the counter fails instead of wrapping
   to a negative value.

### Trade-offs

Independent tests construct more objects, which is negligible for a pure value type but real for an
expensive fixture. Where a genuinely process-wide sequence is required, model it explicitly (a
singleton or a dedicated `AtomicLong` service) so the shared state is visible at the call site. Where a
resource must be shared between tests, keep it read-only or give each test its own slice (a distinct
schema, a distinct key prefix) so parallel execution stays safe.

## Related

- [Code review](code-review.md)
- [Tests](tests.md)
- [Production](production.md)
- [Exercises](exercises.md)
- [Reliability issues](../../issues/reliability.md)
- [Database and JPA issues](../../issues/database.md)
- [Maintainability issues](../../issues/maintainability.md)
