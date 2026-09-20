# Testing Code Review

Six review targets, each a test that *lies about what it proves*. Read the clean sources, write your
findings down, then expand the reveal and compare them with the documented issues. The correct
implementation for each is linked at the end of its section.

Validate that any review target still compiles with:

```bash
./gradlew :modules:12-testing:compileBrokenExamples
```

---

## Asserting implementation not behaviour

A checkout service prices a basket and charges the customer through `PaymentGateway`. Its unit test
passes in CI and is treated as the safety net for checkout, yet a pricing defect has already shipped
without the suite noticing.

```java
--8<-- "modules/12-testing/broken-examples/asserting-implementation-not-behaviour/CheckoutService.java"
```

```java
--8<-- "modules/12-testing/broken-examples/asserting-implementation-not-behaviour/CheckoutServiceTest.java"
```

Consider: what the assertions actually prove about the amount the customer is charged; which
collaborator is replaced by a double and whether that removes the rule under test; how tightly the
test is coupled to the service's internal call structure; whether it survives a behaviour-preserving
refactor; the behaviour it never reaches (empty basket, full discount, invalid input); and whether
both observable outcomes of `checkout` — the returned amount and the charged amount — are verified.

??? warning "Reveal issues"

    ### Testing issue — Assertions coupled to implementation instead of behaviour

    **Problem.** The test asserts *how* `CheckoutService` talks to its collaborators — exact argument
    lists, the number of `subtotal` calls and the call order — rather than *what* the checkout produces.

    **Why it happens.** Mockito makes interaction verification easy, so a test written to "cover" a
    method ends up mirroring its body; a passing suite then feels like proof even though no business
    result is checked.

    **Production impact.** A behaviour-preserving refactor turns the test red, developers weaken or
    delete the assertions, and the regressions the test was meant to catch (for example the wrong
    amount charged) stay invisible.

    **How to detect it.** Look for `verify`, `times`, `InOrder` and argument-matcher imports in a test
    that never asserts a return value or a captured argument; mutate a business rule and see whether
    the test still passes.

    **The fix.** Assert the contract a caller depends on: the amount returned and the amount charged
    (`assertThat(charged).isEqualTo(new Money(3798))`, `verify(gateway).charge(new Money(3798))`).

    **Trade-off.** Behaviour assertions need a well-defined observable result, which forced `checkout`
    to return the amount charged; where a boundary has no return value, capture the recorded side effect
    instead of asserting on internal calls.

    ### Testing issue — Mocking a value collaborator proves nothing

    **Problem.** `PricingCalculator` is a pure, in-process value collaborator, but the test replaces it
    with a mock and stubs its result, so the percentage and rounding rules are never executed.

    **Why it happens.** Mocking is the default reflex for any injected dependency, so a collaborator
    that performs no I/O is doubled anyway — which silently deletes the code path under test.

    **Production impact.** The discount rule can be broken or changed and `CheckoutServiceTest` stays
    green because the mock returns whatever the test told it to; wrong prices reach production through a
    green pipeline.

    **How to detect it.** Every `@Mock` whose class does no I/O and holds no external state is a
    candidate: ask "if I break this class, does the test still pass?" If yes, the mock removed the
    subject of the test.

    **The fix.** Use the real calculator (`private final PricingCalculator calculator = new
    PricingCalculator();`) and double only the remote `PaymentGateway` boundary.

    **Trade-off.** Real collaborators make a test depend on more code, so a failure can originate in the
    collaborator; keep the collaborator pure and fast, and reserve doubles for I/O, time and randomness.

    ### Design issue — No observable behaviour at the service boundary

    **Problem.** `checkout` has two observable outcomes — the amount it returns and the amount it
    charges — and the test verifies neither precisely: the charge is matched with `any(Money.class)`.

    **Why it happens.** The test was written around the implementation's call sequence rather than the
    service's contract, so when the only "result" it knows is a method invocation, the real outcome
    (money leaving the customer's account) is never pinned down.

    **Production impact.** A basket of 4748 cents with a 20% discount is charged as 4748 instead of
    3798 — 970 cents overcharged per order — and the defect ships with the suite green.

    **How to detect it.** Search tests for `any(`, `anyList()` or `anyString()` on values that represent
    the outcome; a matcher that accepts "anything of the right type" marks a boundary whose behaviour is
    unspecified.

    **The fix.** Return the amount charged from `checkout`, charge only a positive total, and assert both
    the returned value and the recorded charge in money.

    **Trade-off.** Returning the charged amount couples the caller to it — usually desirable for a
    checkout, but it must stay consistent with the payment provider's asynchronous settlement.

    ### Maintainability issue — Test breaks on every behaviour-preserving refactor

    **Problem.** Verifying call order (`InOrder`) and the exact number of internal calls (`times(1)`)
    makes the test fail for changes that do not alter what the customer is charged.

    **Why it happens.** `InOrder` and `times(n)` encode the current control flow; they were added to "be
    thorough" but assert an implementation detail no caller can observe.

    **Production impact.** A refactor inside `checkout` produces a red test, the team spends time fixing
    the test rather than the code, and the test is eventually deleted — taking the flow's only (weak)
    safety net with it.

    **How to detect it.** Any test that fails after an IDE "extract method" or "inline variable" refactor
    without a behaviour change is over-coupled; run the refactor and observe.

    **The fix.** Drop the order and call-count verification and assert only the observable contract, so
    the test survives any refactor that keeps the charged amount correct.

    **Trade-off.** Call-order verification is legitimate when the order itself is the contract — for
    example "authenticate before authorise" or "commit before acknowledging a message".

[Correct implementation](solutions.md#asserting-implementation-not-behaviour)

---

## Mocking away the integration

An order fulfilment service asks the inventory service whether a requested quantity is in stock,
through `InventoryClient`. The unit test mocks the client and passes in CI, yet the inventory API's
endpoint and response field names have both changed since the test was written.

```java
--8<-- "modules/12-testing/broken-examples/mocking-away-the-integration/InventoryClient.java"
```

```java
--8<-- "modules/12-testing/broken-examples/mocking-away-the-integration/OrderFulfilmentService.java"
```

```java
--8<-- "modules/12-testing/broken-examples/mocking-away-the-integration/OrderFulfilmentServiceTest.java"
```

Consider: which collaborator is replaced by a double and what contract that removes; whether the
request path and the `Accept` header are ever checked; whether the JSON field names the client and
service rely on are verified; what happens when the inventory service is slow, returns a 5xx or sends
an unexpected body; the type the client returns and what each caller must know to use it; and how an
API change would surface in the suite.

??? warning "Reveal issues"

    ### Testing issue — Mocking the HTTP boundary hides contract drift

    **Problem.** The test replaces `InventoryClient` — the outbound HTTP boundary itself — with a mock,
    so no request is ever built or sent. The wrong path (`/stock/{sku}` instead of `/inventory/{sku}`)
    and the wrong JSON field (`"quantity"` instead of `"available"`) cannot fail the suite.

    **Why it happens.** Mocking the collaborator that performs the I/O is the fastest way to make a
    service method testable, but the one thing that can drift silently — the wire contract — is exactly
    what the mock removes.

    **Production impact.** The client calls `GET /stock/A-1`, the inventory API answers 404,
    `RestClient` throws, and every `canFulfil` call fails — while the mocked suite stayed green through
    the whole rollout.

    **How to detect it.** Look for a `@Mock` on a class whose name ends in `Client`, `Gateway`,
    `Repository` or `Api` and whose method performs I/O; ask "if the remote contract changed, which test
    would fail?"

    **The fix.** Replace the boundary with a stub server (`WireMock`) that speaks the real protocol, so
    the request path, the header and the body binding are all exercised.

    **Trade-off.** A stub server is slower and heavier than a mock and needs a port and lifecycle
    management, so it is reserved for the boundary; unit-test the surrounding domain logic with a fake
    client.

    ### Testing issue — No assertion on the wire contract

    **Problem.** Even with a real client, this test asserts nothing about the request it produces — not
    the URL path, not the `Accept` header, not that the body deserializes into a typed object. The stub
    hands the service a `Map` the test itself built.

    **Why it happens.** The test was written to check a branch (`available >= quantity`) and stopped
    there; verifying the outbound request feels like testing the framework, so the HTTP-level contract
    is left implicit.

    **Production impact.** An `Accept` header dropped or set to `text/plain` makes the API answer 406 or
    a different representation, and the client's deserialization fails in production with no test
    covering the request shape.

    **How to detect it.** Search a mocked-client test for any assertion on the request object (URL,
    headers, body); if there is none, the wire contract is untested however many branches are covered.

    **The fix.** Use `WireMock`'s recorded requests — `verify(getRequestedFor(urlEqualTo(...))
    .withHeader("Accept", ...))` — and let Jackson bind the stubbed JSON into a typed response.

    **Trade-off.** Asserting the exact path couples the test to the API version, which is the point for
    a contract test — but assert only the fields genuinely part of the contract and avoid pinning
    headers the API ignores.

    ### Reliability issue — No timeout or error-path coverage on an outbound call

    **Problem.** The outbound call is built with no connect or read timeout, and the suite covers only
    the happy path: a slow or failing inventory service is never exercised, and a 5xx or unparseable
    body surfaces as a raw `RestClientException` (or `NullPointerException`).

    **Why it happens.** The default `RestClient` inherits the underlying HTTP client's defaults, which
    are effectively unbounded for a synchronous call; because the client is mocked, the test never
    touches the transport and the missing policy is invisible.

    **Production impact.** When the inventory service degrades, every fulfilment request holds its
    thread waiting on the socket, the Tomcat thread pool saturates, and the whole application stops
    serving — not just fulfilment.

    **How to detect it.** Grep for `RestClient`, `WebClient` or `HttpClient` construction without a
    request factory or timeout setting; in tests, check that at least one case covers a timeout, a 5xx
    and a malformed body.

    **The fix.** Set explicit connect and read timeouts on the injected request factory and cover the
    failure paths, so the timeout policy is part of the tested configuration.

    **Trade-off.** A timeout that is too short turns a slow-but-correct dependency into spurious
    failures; choose it from the dependency's SLO and add a bounded retry with jitter rather than
    lengthening the timeout.

    ### Design issue — HTTP client interface leaks the wire format

    **Problem.** `get` returns `Map<String, Object>`, so the caller must know the JSON field names and
    cast the values itself; a renamed or retyped field ripples through the domain layer and fails as a
    `NullPointerException` far from the client.

    **Why it happens.** Returning a `Map` avoids writing a DTO and "just works" while the payload is
    small — and it moves the deserialization decision out of the one place that should own it.

    **Production impact.** If the API changes `available` from a number to a string, the cast throws a
    `ClassCastException` attributed to `OrderFulfilmentService` rather than to the client that should
    adapt.

    **How to detect it.** Any client or repository method whose return type is `Map<String, Object>`,
    `JsonNode` or a raw `String` body is leaking the wire format; ask "how many call sites would change
    if a field is renamed?"

    **The fix.** Return a typed record (`InventoryResponse`) that Jackson binds once, inside the client,
    and write the domain against `response.available()`.

    **Trade-off.** A DTO per response is more code and couples the domain to the API's shape; where the
    payload is genuinely dynamic, return a documented type rather than a raw `Map`, and consider an
    anti-corruption layer.

[Correct implementation](solutions.md#mocking-away-the-integration)

---

## Shared mutable test fixtures

An order totals component sums the lines of an order and can add shipping. The suite is green in CI,
but the data every test works with lives in one static `OrderFixture` — and one of the tests changes
it.

```java
--8<-- "modules/12-testing/broken-examples/shared-mutable-test-fixtures/OrderFixture.java"
```

```java
--8<-- "modules/12-testing/broken-examples/shared-mutable-test-fixtures/OrderTotalsTest.java"
```

Consider: where the data a test asserts on comes from and who else can change it; what each test's
input actually is; what happens when the methods run in a different order, one method runs alone, the
class runs twice in the same JVM, or the suite runs in parallel; which test mutates shared state and
which silently depends on that mutation; whether `@TestMethodOrder` documents a real sequence or hides
a coupling; what a second test class in the package would observe; and how much of each assertion is
about `OrderTotals` versus the fixture.

??? warning "Reveal issues"

    ### Testing issue — Tests share a mutable static fixture

    **Problem.** `OrderFixture.ORDERS` is a single mutable `List<Order>` shared by the whole class, so
    what one test asserts depends on what earlier tests did to that list — and the list is the only
    description of the data a test uses.

    **Why it happens.** A shared fixture feels like DRY, and because the collection is `static final` it
    reads like a constant; the tests pass when the class runs as a whole, so nothing signals the
    mutation.

    **Production impact.** A test-selection or ordering change (IDE rerun of one method, parallel
    execution, another orderer, a new test class in the package) makes `@Order(5)` read a list that no
    longer holds the order `@Order(4)` appended — the suite fails for a reason unrelated to
    `OrderTotals`, or passes on data left behind by a previous test.

    **How to detect it.** Look for `static` fields of a mutable type (`List`, `Map`, `Set`, an array, a
    mutable POJO) in test or fixture classes, then run each method alone and the class with
    `MethodOrderer.Random`.

    **The fix.** Create the data a test asserts on inside that test (`OrderTestData.anOrder(...)
    .withLine(...).build()`), so it is reachable from nowhere else.

    **Trade-off.** Duplicating literals per test looks less tidy than one shared list and each test pays
    a few lines; keep that cheap with a builder and share only immutable values or genuinely expensive
    started resources.

    ### Testing issue — Test order dependence hidden by an explicit method order

    **Problem.** `totalWithShipping_largestOrder_usesFixtureShipping` asserts the fixture holds three
    orders and totals `ORDERS.get(2)`; that third order exists only because
    `total_multiLineOrder_sumsEveryLine` appended it, and `@TestMethodOrder(OrderAnnotation.class)` plus
    `@Order(n)` is what makes the dependency work.

    **Why it happens.** The annotation was added to make CI deterministic — a reasonable goal — but it
    also freezes the order the coupling needs; `@Order` documents a sequence instead of removing the
    need for one.

    **Production impact.** Run `@Order(5)` alone and it fails with `expected: <3> but was: <2>`; replace
    the orderer with a random one and the method fails whenever `@Order(4)` does not come first.

    **How to detect it.** Any `@TestMethodOrder`/`@Order` pair in a class whose tests do not genuinely
    share a resource; run each method individually (`--tests 'Class.method'`) and swap in
    `MethodOrderer.Random` for a few runs.

    **The fix.** Remove the shared state, so the class needs no `@TestMethodOrder` and every method
    passes alone, in any order and in parallel.

    **Trade-off.** Order annotations are legitimate when the sequence itself is the contract — a
    container started once, a schema migrated before the tests that use it; express that with
    `@BeforeAll` or an extension so it is visible in one place.

    ### Maintainability issue — No per-test data builder; a test edits the shared fixture

    **Problem.** There is no way to describe an order inline, so the test that needs a multi-line basket
    appends one to `OrderFixture.ORDERS` and reads it back with `get(size() - 1)`; its input is not
    visible in the test and the fixture is modified by a test that does not own it.

    **Why it happens.** Without a builder, the cheapest way to get data is to reuse the shared
    collection, and adding a factory method to the fixture still publishes the value through the shared
    list, so the coupling remains.

    **Production impact.** A new test must run after the mutating test to see the data it expects, the
    suite can no longer be split, reordered or parallelised, and failures appear far from their cause.

    **How to detect it.** Search test methods for writes to fixture state (`add(`, `put(`, `remove(`,
    `clear()`) on a field the test did not create, and for `.get(size() - 1)` index arithmetic.

    **The fix.** Give each test a fluent builder that returns a fresh value stating exactly the basket
    it needs, so nothing is published anywhere.

    **Trade-off.** A builder is code to maintain and can grow defaults that hide the values a test
    depends on; keep it dumb, package-private in the test source set, and with no hidden defaults for
    data under assertion.

    ### Design issue — Fixture has no ownership or lifecycle

    **Problem.** `OrderFixture` is a package-private class of static state with no owner and no reset:
    `static final` protects the reference, not the contents, and the list lives for the whole JVM.

    **Why it happens.** A "test data" class is usually written as a bag of constants, and a `static
    final` collection looks like a constant; because the fixture is a separate file tests merely read
    from, the questions — who owns this data, when is it valid, who resets it — are never asked.

    **Production impact.** A second test class in the package observes the order appended by
    `OrderTotalsTest` (or its own writes break `OrderTotalsTest`), so adding one test file changes
    another's outcome.

    **How to detect it.** List every `static` mutable field in the test sources and answer "who resets
    this, and when?"; a fixture class with only static mutable members and a private constructor is the
    pattern to look for.

    **The fix.** Model fixtures as factories that return fresh immutable values, and make the value
    types copy defensively (`List.copyOf` in the record constructor), so lifecycle questions disappear.

    **Trade-off.** Some fixtures are genuinely expensive and must be shared — a started container, a
    migrated schema, a seeded database; share the *started resource* with an explicit lifecycle and keep
    it immutable or reset it between tests.

[Correct implementation](solutions.md#shared-mutable-test-fixtures)

---

## Sleep-based async assertions

An asynchronous report job runs generation on a background thread and exposes each report's status.
The test passes in CI and is treated as the safety net for the job's lifecycle — but it waits with
fixed delays instead of for the condition it asserts, and it never exercises a report whose generation
fails.

```java
--8<-- "modules/12-testing/broken-examples/sleep-based-async-assertions/AsyncReportJob.java"
```

```java
--8<-- "modules/12-testing/broken-examples/sleep-based-async-assertions/AsyncReportJobTest.java"
```

Consider: how the test decides the background work has finished and what happens when the work is
slower or faster than the delay; what happens when a report never finishes or finishes in another
state; whether the test can tell "not finished yet" from "failed"; which transitions of the lifecycle
are exercised; whether the test would still pass if `submit` ran the work on the calling thread; and
what each test costs in wall-clock time.

??? warning "Reveal issues"

    ### Testing issue — Sleep-based waiting for asynchronous work

    **Problem.** The test submits a report and then calls `Thread.sleep(500)` before asserting the
    status; the sleep is a guess that the background work finishes within 500 ms, not a
    synchronisation mechanism, so the assertion races the worker thread.

    **Why it happens.** Waiting for a background thread is awkward without a tool for it, and a sleep is
    the first thing that works on a developer's machine — `REPORT_GENERATION` and the sleep are both
    500 ms, so the worker usually wins locally.

    **Production impact.** On a loaded CI box the assertion runs while the status is still `RUNNING`
    (`expected: <COMPLETED> but was: <RUNNING>`); on a fast machine the test still sleeps 500 ms and
    would pass even if `submit()` blocked the caller, so the asynchronous contract is never verified.

    **How to detect it.** Search test sources for `Thread.sleep`, `TimeUnit.SECONDS.sleep`,
    `LockSupport.park*` and `Thread.yield`; then run the class with the worker artificially delayed — a
    suite that fails is waiting on time instead of on state.

    **The fix.** Poll the state under test with Awaitility — `await().atMost(Duration.ofSeconds(2))
    .pollInterval(...).untilAsserted(...)` — and release a latch where the transition itself is what the
    test is about.

    **Trade-off.** Awaitility hides a polling loop, so a condition that never holds still costs the full
    `atMost` budget; keep the bound short and poll the observable state rather than a proxy.

    ### Testing issue — Unbounded polling loop masks a hang

    **Problem.** The second test polls with `while (job.status("RPT-2") != ReportStatus.COMPLETED)`; the
    loop has no timeout and `COMPLETED` is the only state that ends it, so a report that fails or hangs
    keeps it spinning for the life of the build.

    **Why it happens.** An unbounded `while` around a sleep looks like it handles "eventually", and it
    does terminate on the happy path — the author reasoned about the case that works, not the state the
    loop can never leave.

    **Production impact.** A `FAILED` status leaves the condition true, the loop sleeps 50 ms for ever,
    the test method never returns, and the JVM is killed by the CI job timeout — no assertion message,
    no observed status, and the run is written off as infrastructure flake.

    **How to detect it.** Look for `while` loops in tests that poll a status or a collection with no
    counter, deadline or `atMost` around them, and for `Thread.sleep` inside a loop; a JUnit `@Timeout`
    turns any such loop into a failure.

    **The fix.** Replace the loop with a bounded `await().atMost(...).untilAsserted(...)`, so a hang
    becomes an ordinary `ConditionTimeoutException` that reports the status it observed.

    **Trade-off.** A bound has to be chosen and too generous a bound still hides slowness; set it from
    the contract ("this must complete in two seconds"), not from the current machine's speed.

    ### Reliability issue — No failure-path assertion on an asynchronous job

    **Problem.** Both tests only ever expect `COMPLETED`; nothing submits a report whose generation
    throws, so the `FAILED` transition is never executed and the assertion that distinguishes "still
    running" from "will never finish" is never made.

    **Why it happens.** The happy path is what the feature was written for and the test was written
    alongside it, so the `catch` branch in `generate` exists but is free to be wrong.

    **Production impact.** If `generate()` swallows the exception and forgets to set `FAILED`, the report
    stays `RUNNING` for ever; the caller polls, the operator sees "in progress", nothing is retried or
    alerted, and the suite stays green because it never made the work throw.

    **How to detect it.** For every asynchronous state machine, list its terminal states and check that
    a test reaches each one; coverage points at the `catch` block and mutation testing proves the
    assertion is real.

    **The fix.** Inject work that throws through a seam and assert the job reaches `FAILED` instead of
    staying `RUNNING`.

    **Trade-off.** Testing the failure path needs a seam that can make the work fail; it must be the
    same code path production uses, not a test-only branch inside `generate`.

    ### Maintainability issue — Fixed delays slow the suite and encode machine speed

    **Problem.** Every test waits a hard-coded delay — `Thread.sleep(500)` in the first and a 50 ms poll
    in the second — that must track the production simulation, is copied rather than derived, and is
    paid in full on every run.

    **Why it happens.** The delay is the mechanism the tests use to wait, so its value looks like a
    detail of the test; in fact it is a second, invisible copy of the timing contract.

    **Production impact.** Twenty tests that each sleep 500 ms add ten seconds of dead wall-clock time
    per run, so the suite is run less often or parallelised (making the timing races worse), and a
    developer "fixes" a failure by raising the sleep.

    **How to detect it.** Sum the `Thread.sleep` arguments across the test sources and compare that with
    the suite's runtime; grep for a sleep literal that also appears as a production constant.

    **The fix.** Wait on the condition with a bound (`atMost` + `pollInterval`) and release the work
    where the transition matters, so a test costs the time the work actually takes.

    **Trade-off.** A gate couples the test to the seam that exposes it; where there is no natural seam —
    a real HTTP call, a database write — Awaitility polling is the right tool and the suite costs
    whatever the boundary costs.

[Correct implementation](solutions.md#sleep-based-async-assertions)

---

## Embedded substitute hides Postgres semantics

An account service registers accounts and reads their balance, with an `InMemoryAccountRepository` so
the tests run without Docker. The suite is fast and green, but the repository the tests exercise is a
map keyed by a lower-cased email, and the service leans on that key for both uniqueness and lookup.
The store it ships against is PostgreSQL.

```java
--8<-- "modules/12-testing/broken-examples/embedded-substitute-hides-postgres-semantics/InMemoryAccountRepository.java"
```

```java
--8<-- "modules/12-testing/broken-examples/embedded-substitute-hides-postgres-semantics/AccountService.java"
```

```java
--8<-- "modules/12-testing/broken-examples/embedded-substitute-hides-postgres-semantics/AccountServiceTest.java"
```

Consider: which semantics of the real store the map reproduces and which it silently replaces; what the
`email` unique index and the `findByEmail` query do on PostgreSQL; where the case-insensitive matching
comes from and whether the application states it; what `findAll` guarantees about row order; which
layer is supposed to reject two accounts sharing an address; and how much of each assertion is about
`AccountService` versus the map.

??? warning "Reveal issues"

    ### Testing issue — Substitute diverges from production semantics

    **Problem.** Every test runs against `InMemoryAccountRepository`, so the suite asserts what the map
    does — case-insensitive matching, insertion order, silent overwrite — rather than what PostgreSQL
    does; PostgreSQL compares `email` case-sensitively and returns rows in no particular order without a
    `Sort`.

    **Why it happens.** Replacing the repository is the fastest way to keep a service test Docker-free,
    and a map is the smallest thing that satisfies the methods the service calls; the substitute is
    written to make the service testable, not to reproduce the store.

    **Production impact.** `register("ada@example.com")` followed by `register("Ada@Example.com")` is
    rejected by the fake but accepted by PostgreSQL, so two accounts exist for one address and the
    user's balance is split across them — the suite never noticed because it never spoke to PostgreSQL.

    **How to detect it.** For every hand-written repository double, list the guarantees of the real
    repository (types, collation, constraints, ordering, isolation) and ask which of them the double
    reproduces "for free".

    **The fix.** Normalise the email explicitly in `AccountService` so case-insensitive uniqueness is an
    application rule, and test the persistence semantics against a real PostgreSQL container.

    **Trade-off.** A Docker-backed test is slower than a map and needs the container infrastructure;
    keep fast unit tests for pure logic, but test matching, constraints, ordering and transaction
    boundaries against the real engine.

    ### Database issue — Constraint and collation assumptions are unverified

    **Problem.** The substitute assumes three things PostgreSQL does not do: that the unique index on
    `email` is case-insensitive, that a duplicate insert fails, and that `findAll()` returns insertion
    order — none verified, all load-bearing for the service's uniqueness rule.

    **Why it happens.** An in-memory store has no schema, so there is nothing to enforce a constraint
    and no collation to disagree with; `LinkedHashMap` ordering and `put()` overwriting look like
    incidental details rather than database semantics being quietly redefined.

    **Production impact.** Because the index is case-sensitive, `"Ada@Example.com"` inserts a second row
    for an existing customer, so a duplicate-email cleanup migration is needed and lookups return one of
    two rows until it runs.

    **How to detect it.** Compare every assumption a test double makes about matching, ordering and
    constraints with the schema and the query; inspect the index definition and assert the constraint by
    attempting the conflicting write against the real engine.

    **The fix.** Declare the unique constraint on the mapping (`uk_accounts_email`) so the database is
    the arbiter, and request row order explicitly with `Sort` instead of assuming it.

    **Trade-off.** Enforcing case-insensitive uniqueness by normalising in the application is portable
    but must be applied at every entry point; a `citext` column or a `lower(email)` functional index
    pushes the rule into the database — stronger but PostgreSQL-specific and needing a migration.

    ### Reliability issue — Defects reach production undetected

    **Problem.** `register` reads for an existing address and then writes, with nothing but the
    substitute's map to back the check; the map cannot raise a constraint violation, so the suite never
    executes the failure the database produces — a second insert of the same address.

    **Why it happens.** The check-then-act shape reads naturally and passes every test the fake can run,
    because the fake always answers the lookup — so the branch where the database rejects the write is
    unreachable.

    **Production impact.** Two concurrent registrations of the same address both pass the check and both
    save; the unique index rejects the loser with a `DataIntegrityViolationException` that nothing maps
    to a 409, so the caller sees a 500. No concurrency is even required: inserting the same address
    twice on one connection is enough.

    **How to detect it.** Look for read-then-write uniqueness checks whose only test double cannot fail,
    then check that the column carries a unique constraint and that a test attempts the conflicting
    write against the real database.

    **The fix.** Let the unique index be the arbiter and keep the pre-check only to turn the common case
    into a typed `DuplicateEmailException`; assert the violation in `AccountRepositoryIT`.

    **Trade-off.** Relying on the constraint means the application must translate a persistence
    exception into its own error contract (a 409), which couples the service to the exception type —
    cheaper than a correct distributed lock, and the constraint has to exist regardless.

    ### Maintainability issue — The fake duplicates repository logic

    **Problem.** `InMemoryAccountRepository` is a second implementation of the repository contract, kept
    in step with `AccountRepository` by hand; change a query, a matching rule or a constraint and only
    the fake — which the tests read — is now wrong, and nothing in the build links the two.

    **Why it happens.** The map is written next to the test that needs it, so it looks like test
    scaffolding rather than a parallel implementation of persistence, and because it compiles and the
    tests pass there is no signal that it is a liability.

    **Production impact.** When `AccountRepository` gains a query the fake does not, the tests written
    against it still pass, developers reason about persistence from the fake and ship queries that do
    not exist, and the fake becomes the de facto (wrong) specification of the repository.

    **How to detect it.** Search test sources for classes whose name mirrors a repository
    (`InMemory…Repository`, `Fake…Repository`, `Stub…Repository`) and whose body re-implements queries.

    **The fix.** Delete the repository double: exercise the persistence layer against PostgreSQL
    (`@DataJpaTest` with `Replace.NONE` and a `@ServiceConnection` container), so there is only one
    implementation of the contract.

    **Trade-off.** Giving up the map means the tests need a container and are slower; reserve test
    doubles for collaborators where the interaction, not the semantics, is the subject.

[Correct implementation](solutions.md#embedded-substitute-hides-postgres-semantics)

---

## Order-dependent test suite

A sequence allocator hands out monotonically increasing numbers. The suite is green in CI and treated
as the safety net for the allocator — but each test builds a fresh allocator and then asserts the
absolute number it expects next, and those numbers only line up when the methods run in the order
declared in the class.

```java
--8<-- "modules/12-testing/broken-examples/order-dependent-test-suite/SequenceAllocator.java"
```

```java
--8<-- "modules/12-testing/broken-examples/order-dependent-test-suite/SequenceAllocatorTest.java"
```

Consider: where the counter a test calls `next()` on actually lives and who else can advance it;
whether a newly constructed allocator starts from the `start` value it was given and what the Javadoc
promises; what each expected value describes — a property of `next()` or the method's position in the
class; what happens when methods run in a different order, one runs alone, the class runs twice in the
same JVM, or the suite runs in parallel; whether `@TestMethodOrder` documents a real sequence or hides
a coupling; and what a second allocator or a second test class would observe.

??? warning "Reveal issues"

    ### Testing issue — Absolute assertions pinned to the declaration order

    **Problem.** Each method builds a fresh allocator but asserts the absolute number it expects the
    process to hand out next (`@Order(1)` expects 1, `@Order(2)` expects 2, `@Order(4)` expects 3);
    those numbers are positions in one shared sequence, not properties of a fresh allocator.

    **Why it happens.** Because the allocator's counter is shared, the value a fresh allocator returns
    depends on how many allocations earlier methods made; writing the observed number down is the
    shortest way to assert it, and adding `@Order` makes the numbers line up.

    **Production impact.** Run `next_afterFirstAllocation_returnsSecondValue` alone and it fails with
    `expected: <2> but was: <1>`; replace `@TestMethodOrder` with `MethodOrderer.Random` and almost
    every run is red — the suite is red for reasons unrelated to `SequenceAllocator`, developers retry
    until green, and a real regression is indistinguishable from the noise.

    **How to detect it.** Any `@TestMethodOrder`/`@Order` pair in a class whose tests do not genuinely
    share a started resource; run each method individually and swap in `MethodOrderer.Random` for a few
    runs. A second signal is an assertion whose expected value is a bare absolute number no call in the
    test produces.

    **The fix.** Create the allocator the test asserts on and assert the consequences of the calls the
    test itself makes (`next()` twice from a chosen start), so every method passes alone and in any
    order.

    **Trade-off.** Writing several `next()` calls in one method looks less granular and a failure now
    points at a method covering a small sequence instead of a single call; keep the methods small and
    name the progression with `@DisplayName`.

    ### Testing issue — Static counter shared across allocators

    **Problem.** The counter is declared `private static long`, so it belongs to the class, not to an
    allocator: two allocators created with the same `start` share one sequence, and because the
    constructor assigns only `start`, a newly constructed allocator does not begin at its own start
    value at all.

    **Why it happens.** The field was meant to be the instance counter the constructor seeds from
    `start`; marking it `static` (and dropping the seeding) moves the state to the class, where it lives
    for the whole JVM — and because the class still compiles, has a `start` parameter and passes its
    suite, the mistake looks deliberate.

    **Production impact.** A second allocator's first `next()` returns whatever the first has already
    reached, so identifiers a caller expects to be independent interleave and restarting one sequence
    moves the other; a test class that builds a fresh allocator per method still shares the counter with
    every other class and previous run.

    **How to detect it.** Look for `static` fields of a mutable type in production classes constructed
    with a per-instance value; construct two instances with the same argument and check whether they are
    independent. A field a constructor parameter appears to initialise but does not is the specific
    shape here.

    **The fix.** Keep the counter in an instance field seeded from `start` in the constructor, so two
    allocators advance independently.

    **Trade-off.** Instance state means each allocator holds its own counter, so a caller that wants one
    process-wide sequence must share the instance deliberately (a singleton or a dedicated `AtomicLong`
    service) instead of relying on an accident.

    ### Maintainability issue — Order-dependent test class cannot run in parallel

    **Problem.** Because the counter is shared and every expected value is absolute, the class can only
    be executed as one ordered run: it cannot be split, reordered by the IDE, run with JUnit's parallel
    execution enabled, or selected method-by-method.

    **Why it happens.** Order dependence and parallelism are invisible on a single-threaded,
    whole-class run — which is how the suite is normally executed — and the cost only appears when the
    build is optimised.

    **Production impact.** When the team enables JUnit parallel execution to shorten the build, methods
    run concurrently against one static counter, interleaved `next()` calls make the absolute assertions
    fail nondeterministically, and the feature is switched off again "because the suite is flaky" — the
    build stays slow.

    **How to detect it.** Enable `junit.jupiter.execution.parallel.enabled=true` (or
    `MethodOrderer.Random`) and run the suite a few times; sharding a class across CI agents surfaces
    the same defect. Grep for `@TestMethodOrder` and for `static` mutable fields as a static check.

    **The fix.** Remove the shared state so there is nothing an order or a thread could interleave; the
    class then needs no `@TestMethodOrder` and is safe to split, shard or parallelise.

    **Trade-off.** Independent tests construct more objects, which is negligible for a pure value type
    but real for an expensive fixture; where a resource must be shared, keep it read-only or give each
    test its own slice (a distinct schema, a distinct key prefix).

[Correct implementation](solutions.md#order-dependent-test-suite)

---

## Related

- [Testing concepts](concepts.md)
- [Testing internals](internals.md)
- [Solutions](solutions.md)
- [Tests](tests.md)
- [Production](production.md)
- [Exercises](exercises.md)
- [Reliability issues](../../issues/reliability.md)
- [Database and JPA issues](../../issues/database.md)
- [Maintainability issues](../../issues/maintainability.md)
