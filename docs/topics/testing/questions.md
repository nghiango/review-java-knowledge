# Testing Interview Questions

Four levels of interview questions covering the testing pyramid and test levels, behaviour versus implementation assertions, test doubles and Mockito strictness, JUnit 5 extensions and parameterized tests, Spring test slices, Testcontainers, WireMock, Awaitility, fixtures and builders, ArchUnit, mutation testing with PIT, consumer-driven contracts, and the triage of a flaky suite and of a post-upgrade production failure.

<!-- --8<-- [start:basic] -->
## Basic

### 1. What is the testing pyramid actually prescribing?

??? question "Reveal answer"
    **Short Answer:** A distribution of cost, not a choice of framework: many fast in-process tests, fewer tests that cross a process boundary, very few end-to-end tests. The same 486 tests arranged as an inverted pyramid take almost four times as long to answer. [Concept](/topics/testing/concepts.md#1-the-testing-pyramid-is-a-statement-about-cost-not-frameworks)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q01TestingPyramidAndLevels.java"
        ```

### 2. Unit, component, integration or end-to-end — what does each level observe?

??? question "Reveal answer"
    **Short Answer:** The levels differ by how much real machinery is in the test, not by the framework used: unit tests one class or pure function, component tests real service logic with the boundary stubbed, integration tests the service plus real PostgreSQL or a broker, end-to-end tests the deployed system through its public API. A defect should be owned by the cheapest level that can observe it. [Concept](/topics/testing/concepts.md#2-test-levels-unit-component-contract-e2e)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q02UnitVsIntegrationVsComponentVsE2e.java"
        ```

### 3. Why should a test assert behaviour instead of implementation?

??? question "Reveal answer"
    **Short Answer:** A behaviour assertion describes what a caller can observe — the amount returned, the amount the customer is charged. An implementation assertion describes how the result was produced — the step count, which collaborator ran. Both pass today; only the behaviour assertion survives a behaviour-preserving refactor. [Concept](/topics/testing/concepts.md#3-assert-behaviour-not-implementation) · [Review](/topics/testing/code-review.md#asserting-implementation-not-behaviour)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q03BehaviourVsImplementationAssertions.java"
        ```

### 4. Why is `Thread.sleep` banned in a test?

??? question "Reveal answer"
    **Short Answer:** A sleep is a guess about how long work takes. Guess low and the assertion runs against unfinished work, so the test flakes on a loaded CI agent; guess high and every run pays the guess. Five hundred async assertions wasting 300 ms each is 150 seconds of suite time, and "just sleep longer" turns it into 900. [Concept](/topics/testing/concepts.md#11-awaitility-bounded-polling-instead-of-sleeps) · [Review](/topics/testing/code-review.md#sleep-based-async-assertions)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q04WhySleepInTestsIsHarmful.java"
        ```

### 5. What are the test doubles — dummy, stub, spy, mock and fake?

??? question "Reveal answer"
    **Short Answer:** The names describe roles in a test, not framework types: a dummy fills a parameter, a stub answers with canned data, a spy wraps a real object and records calls, a mock is a stub whose interactions the test also verifies, and a fake is a hand-written implementation with real behaviour. Only a fake has behaviour the production code actually exercises. [Concept](/topics/testing/concepts.md#4-test-doubles-dummy-stub-spy-mock-fake)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q05TestDoublesTaxonomy.java"
        ```

### 6. What does Mockito strict stubs actually check?

??? question "Reveal answer"
    **Short Answer:** `MockitoExtension` opens a `MockitoSession` with `STRICT_STUBS` before the test and calls `finishMocking()` after it, so a stub that nothing used fails the test with `UnnecessaryStubbingException` even though every assertion passed. An argument mismatch is reported immediately instead of quietly returning `null`. [Concept](/topics/testing/concepts.md#5-mockito-strictness-and-argument-captors) · [Internals](/topics/testing/internals.md#2-mockito-bytecode-proxies-and-strict-stub-detection)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q06MockitoStrictStubs.java"
        ```

### 7. `@SpringBootTest` versus a test slice — what does each load?

??? question "Reveal answer"
    **Short Answer:** `@SpringBootTest` loads the whole application: every bean, a web server and a real DataSource. A slice loads a curated auto-configuration — `@WebMvcTest` the web layer with the service replaced by a mock bean, `@DataJpaTest` JPA plus a DataSource, `@JsonTest` only the Jackson testers. Spring caches one context per context key, so slices are cheap only when the test classes share a key. [Concept](/topics/testing/concepts.md#8-spring-test-slices-webmvctest-datajpatest-springboottest)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q07SpringBootTestVsSliceTests.java"
        ```

### 8. What makes a test deterministic, and what makes it flaky?

??? question "Reveal answer"
    **Short Answer:** Every input the test depends on must be a constant or injected. The clock, generated identifiers, iteration order and parallelism are the four usual leaks: inject a `Clock` instead of reading the wall clock, assert a property of an id (distinct) instead of its value, request ordering explicitly instead of assuming it. [Concept](/topics/testing/concepts.md#14-determinism-and-flaky-tests)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q08DeterministicAndFlakyTests.java"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 9. When do you need an argument captor rather than a plain verification?

??? question "Reveal answer"
    **Short Answer:** `verify(mock).charge(new Money(3798))` proves the argument equals an expected value. An `ArgumentCaptor` hands the argument to the test when the expected value is not known up front — a generated id, a timestamp, a computed amount — and `getAllValues()` returns every captured argument in invocation order.

    **Internal Mechanism:** A mock records its invocations in an `InvocationContainer`; a verification mode reads that record and matches it against a `Verification` with argument matchers. A captor is a matcher whose `capture()` appends the actual value to a list and returns a placeholder, so `verify(gateway, times(2)).charge(captor.capture())` both asserts the cardinality and collects the arguments.

    **Common Mistake:** Capturing an argument the test could have asserted directly, or verifying interactions on every collaborator. A captor on each dependency turns the test into a description of the implementation instead of a statement about the outcome. [Concept](/topics/testing/concepts.md#5-mockito-strictness-and-argument-captors) · [Internals](/topics/testing/internals.md#2-mockito-bytecode-proxies-and-strict-stub-detection)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q09MockitoArgumentCaptorsAndVerification.java"
        ```

### 10. How do JUnit 5 extensions and parameterized tests work?

??? question "Reveal answer"
    **Short Answer:** `@ParameterizedTest` is not a kind of `@Test`: it is a `@TestTemplate` extended by `ParameterizedTestExtension`, and each row of its source becomes one test invocation with its own lifecycle. An extension is a class the engine calls back at whichever lifecycle points it implements.

    **Internal Mechanism:** The engine discovers a template once, then asks every registered `TestTemplateInvocationContextProvider` for invocation contexts — that is why a four-row `@CsvSource` is four tests, each with its own `@BeforeEach`. Extensions registered by `@ExtendWith` or `@RegisterExtension` are composed into a chain and invoked around each invocation, which is how Mockito, Spring and Testcontainers hook in without the test doing anything.

    **Common Mistake:** Writing the rows as a loop inside a single `@Test`. A failure then names the method rather than the failing row, and state from one row leaks into the next because there is only one invocation. [Concept](/topics/testing/concepts.md#7-junit-5-parameterized-tests-nesting-and-extensions) · [Internals](/topics/testing/internals.md#1-junit-platform-engines-discovery-and-the-extension-callback-flow)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q10Junit5ExtensionsAndParameterizedTests.java"
        ```

### 11. What does a Spring test slice contain, and how is that decided?

??? question "Reveal answer"
    **Short Answer:** A slice is `@OverrideAutoConfiguration(enabled = false)` plus a curated list of auto-configurations, so it is defined as much by what it leaves out as by what it loads. `@WebMvcTest` brings controllers, filters and advice with the service replaced by `@MockitoBean`; `@DataJpaTest` brings JPA plus a DataSource and is transactional.

    **Internal Mechanism:** Each slice annotation carries `@ImportAutoConfiguration` naming the auto-configurations to apply; the import selector filters them and disables the rest, and the resulting context is cached under the context key — the configuration classes, properties and active profile. `@AutoConfigureTestDatabase(replace = NONE)` keeps the container-backed DataSource instead of swapping in an embedded one.

    **Common Mistake:** Assuming a slice is "the web layer" without checking what it left out. A missing bean inside a slice is the slice boundary, not a broken application — and a `@DataJpaTest` that keeps its default `replace = NON_TEST` silently tests a different database. [Concept](/topics/testing/concepts.md#8-spring-test-slices-webmvctest-datajpatest-springboottest) · [Internals](/topics/testing/internals.md#4-slice-auto-configuration-how-a-slice-decides-what-to-load)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q11SliceTestContentsAndAutoConfiguration.java"
        ```

### 12. How does the Testcontainers lifecycle work, and what does `@ServiceConnection` do?

??? question "Reveal answer"
    **Short Answer:** The lifecycle question is how many containers the suite starts: `@Testcontainers` with an instance `@Container` field starts one around every test, a static field starts one per class, and a hand-started singleton published as a bean starts one per JVM. `@ServiceConnection` lets Boot derive the DataSource URL, credentials and driver from the running container, so no test hand-writes `@DynamicPropertySource`.

    **Internal Mechanism:** `TestcontainersExtension` starts and stops each `@Container` field at the lifecycle point its modifier implies — `beforeAll` for a static field, `beforeEach` for an instance field. `@ServiceConnection` is resolved by a `ConnectionDetailsFactory` that matches the container type and publishes connection details as properties before the DataSource bean is created, which is why the two mechanisms are alternatives rather than complements.

    **Common Mistake:** Constructing a container and assuming it started. Construction and image-name parsing are pure string work; only `start()` touches Docker. The other mistake is a container per test method, which multiplies the startup and migration cost by the number of tests. [Concept](/topics/testing/concepts.md#9-testcontainers-real-infrastructure-with-a-managed-lifecycle) · [Internals](/topics/testing/internals.md#6-testcontainers-lifecycle-and-ryuk) · [Internals](/topics/testing/internals.md#5-property-resolution-dynamicpropertysource-vs-serviceconnection)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q12TestcontainersLifecycleAndServiceConnection.java"
        ```

### 13. How does Awaitility's polling loop actually work?

??? question "Reveal answer"
    **Short Answer:** `await().atMost(2, SECONDS).pollInterval(100, MILLISECONDS).untilAsserted(...)` is a bounded loop: wait the poll delay, evaluate, wait one poll interval, evaluate again, and stop the moment the condition holds or the ceiling is reached. `atMost` is a ceiling, not a delay, so the fast path costs a couple of poll intervals.

    **Internal Mechanism:** `await()` only builds a `ConditionFactory`; nothing is polled until a condition is supplied. The condition then runs a `ConditionAwaiter` that polls on a scheduled executor and re-runs the whole assertion chain, swallowing `AssertionError` between polls. Any other exception aborts the wait unless `ignoreExceptions()` says otherwise.

    **Common Mistake:** Reading `atMost` as "the wait" and pairing it with a sleep, or awaiting a condition that cannot distinguish "not yet" from "never" — a permanently broken path then simply consumes the whole ceiling before failing. [Concept](/topics/testing/concepts.md#11-awaitility-bounded-polling-instead-of-sleeps) · [Internals](/topics/testing/internals.md#8-awaitility-the-polling-loop-and-condition-evaluation) · [Review](/topics/testing/code-review.md#sleep-based-async-assertions)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q13AwaitilityPollingMechanics.java"
        ```

### 14. How does WireMock match stubs and verify requests?

??? question "Reveal answer"
    **Short Answer:** A stub is a matching rule — method, URL pattern, headers, body — and a request that matches no stub gets `404 Request was not matched`, so a near miss fails loudly instead of returning an empty body. Verification is the mirror image: it counts the recorded requests that match a request pattern.

    **Internal Mechanism:** Every stub is a `StubMapping` carrying a `RequestPattern`; the server matches an incoming request against the registered stubs and returns the first match's response, so the URL matcher decides what "the same path" means. Verification builds the same `RequestPattern` type and counts the journal entries it matches, which is why `getRequestedFor(urlEqualTo(...)).withHeader("Accept", equalTo(...))` proves the client sent what the contract describes.

    **Common Mistake:** Stubbing a URL and calling it a contract test while the client's path, `Accept` header or JSON field names are never checked. A near miss such as `/inventory/a-1` is a different matcher result, and a mocked client never sends a request at all. [Concept](/topics/testing/concepts.md#10-contract-tests-with-wiremock) · [Internals](/topics/testing/internals.md#7-wiremock-stub-matching-and-verification) · [Review](/topics/testing/code-review.md#mocking-away-the-integration) · [Solution](/topics/testing/solutions.md#mocking-away-the-integration)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q14WiremockStubbingAndVerification.java"
        ```

### 15. How do test data builders keep fixtures isolated?

??? question "Reveal answer"
    **Short Answer:** The builder is the whole fixture: `anOrder("ORD-1001").withLine(...).build()` hands each test its own data, so no test can observe or change another test's input and no execution order is required. An immutable value can safely be shared where a shared builder cannot.

    **Internal Mechanism:** A builder owns a mutable list until `build()` hands a copy to an immutable value. Reusing one builder instance across two tests appends to the same list, so the second test silently builds on the first test's lines — the leak only becomes visible when the tests run in a different order. A static fixture field shared by a class has exactly the same failure at class scope.

    **Common Mistake:** A `@BeforeAll` or static fixture that tests mutate, or a builder stored in a field because it "reads nicely". Both leak state between tests and make the suite order-dependent. [Concept](/topics/testing/concepts.md#15-fixtures-and-test-data-builders) · [Review](/topics/testing/code-review.md#shared-mutable-test-fixtures) · [Solution](/topics/testing/solutions.md#shared-mutable-test-fixtures)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q15TestDataBuildersAndFixtureIsolation.java"
        ```

### 16. How do ArchUnit rules express package dependencies?

??? question "Reveal answer"
    **Short Answer:** ArchUnit imports the compiled classes and evaluates rules over the resulting graph, so a rule reads as a sentence: `noClasses().that().resideInAPackage("..pricing..").should().dependOnClassesThat().resideInAPackage("..accounts..")`. A layered rule states the allowed direction once instead of one rule per package pair.

    **Internal Mechanism:** The import reads each class file's constant pool with ASM, so every referenced type becomes a dependency edge — including a type used only in a field signature. `@AnalyzeClasses` declares the import and each `@ArchTest` field is evaluated against it. `FreezingArchRule` records today's violations as a baseline so a legacy codebase can adopt a rule and fail only on new ones.

    **Common Mistake:** Writing a rule nothing can violate (it passes and teaches nothing), or freezing a rule before fixing the violations it was written to catch — the baseline then silently blesses the coupling. [Concept](/topics/testing/concepts.md#16-archunit-architecture-as-executable-rules) · [Internals](/topics/testing/internals.md#9-archunit-bytecode-import-and-rule-evaluation)

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q16ArchunitRulesAndPackageDependencies.java"
        ```
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 17. A suite is green on a laptop and red in roughly one CI run in seven. How do you triage it?

??? question "Reveal answer"
    **Short Answer:** Rank the tests by measured failure rate, group them by cause, and fix the cause rather than the symptom.

    **Deep Explanation:** The cause decides the fix: a fixed port needs a dynamic port and a container shared per JVM, a sleep needs a bounded awaited condition, an assumed row order needs an explicit `Sort`. A test with no failure in the measurement window stays untouched — triage is a ranking exercise, not a sweep. Blast radius matters more than rate: one test failing 1% of the time makes a 500-test suite red in roughly 99% of runs, so "rare" flakes are not rare at suite level.

    **Internal Mechanism:** Rerun the failing class many times with the same forks and parallelism, then record the attribute that separates red runs from green ones — parallel execution, a co-tenant job on the agent, ordering, time of day. Test each attribute on its own before believing the combination, because "fails when parallel and on a shared agent" is a far more useful finding than "CI is flaky": it names the resource and therefore the fix.

    **Example:** [Review](/topics/testing/code-review.md#sleep-based-async-assertions) — a wait that is a guess about how long work takes.

    **Common Mistake:** Marking the test `@Disabled`, adding a retry, or reducing parallelism, all of which hide the defect the suite was supposed to catch and leave the rate in the suite for the next agent to rediscover.

    **Production Consideration:** Track flake rate as a metric with an owner and a budget, quarantine with an expiry date rather than silently, and keep the parallel reproduction in CI after the fix so the verification is meaningful. [Concept](/topics/testing/concepts.md#14-determinism-and-flaky-tests)

    **Follow-up Questions:** How do you attribute a failure to infrastructure rather than to the test? What is the smallest reproduction you can keep in CI without slowing every build?

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q17FlakySuiteTriageAndDeterminismStrategy.java"
        ```

### 18. Where do you draw the line between mocking, integration and contract tests?

??? question "Reveal answer"
    **Short Answer:** Double only at the boundary, and verify the boundary against something that speaks the protocol.

    **Deep Explanation:** A mock answers whatever the test told it to answer, so it can observe defects only in the code the test itself drives — never the request the client builds or the response it binds. A stub server can: the client really opens a connection, so a renamed path, a missing `Accept` header or a renamed JSON field fails the test. A container-backed test adds the store's semantics, and an end-to-end test adds the deployment.

    **Internal Mechanism:** A mocked `InventoryClient` returns the test's own assumption, so it catches none of the three wire-level defects — endpoint rename, field rename, status-code change. A WireMock server catches all three because the real client serialises and sends the request. Nothing about the mock is wrong; it is simply not a protocol, so it cannot be evidence about one.

    **Example:** [Review](/topics/testing/code-review.md#mocking-away-the-integration) and the corrected client in [Solution](/topics/testing/solutions.md#mocking-away-the-integration).

    **Common Mistake:** Reading the rule as "mock less" and replacing every unit test with a container test, which buys the same confidence at many times the cost and hides the pricing rule behind infrastructure.

    **Production Consideration:** Contract tests do not replace integration tests: they say nothing about the store's semantics or the deployment. Keep the pyramid and let each defect be owned by the cheapest level that can observe it — unit for a rounding rule, contract for a wire rename, integration for a dropped index, end-to-end for a missing bean in a profile. [Concept](/topics/testing/concepts.md#10-contract-tests-with-wiremock) · [Concept](/topics/testing/concepts.md#18-consumer-driven-contracts)

    **Follow-up Questions:** Which defects can only an end-to-end test observe? When is a mocked collaborator still the right double?

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q18MockingVsIntegrationAndContractTesting.java"
        ```

### 19. How do you run Testcontainers in CI at scale?

??? question "Reveal answer"
    **Short Answer:** Decide how many containers the suite starts, then how the work is sharded; those two numbers set the CI bill.

    **Deep Explanation:** A container per test class on a 24-class suite starts 24 PostgreSQL instances in one job — about 12 GB of memory on a 4 GB runner, which is an OOM kill rather than a slow build. One singleton container per job plus eight shards starts eight containers in total and cuts the wall clock from about five minutes to under half a minute, because the shards run in parallel and each pays for exactly one container.

    **Internal Mechanism:** A hand-started singleton published as a `@ServiceConnection` bean is started once per JVM; `TestcontainersExtension` starts a static `@Container` once per class and an instance `@Container` once per test. Ryuk reaps leftovers, but it needs the Docker socket, so an agent that hides the socket must disable it and clean up per job instead.

    **Example:** [Internals](/topics/testing/internals.md#6-testcontainers-lifecycle-and-ryuk).

    **Common Mistake:** Adding shards without sharing the container, which multiplies container startups by the shard count, or disabling Ryuk without a per-job cleanup and leaking containers until the agent runs out of disk.

    **Production Consideration:** Budget container memory and startup as first-class CI resources, pin the image tag so an upstream change cannot silently alter test semantics, and treat a warm agent with a reused container as the target state rather than the exception. [Concept](/topics/testing/concepts.md#9-testcontainers-real-infrastructure-with-a-managed-lifecycle) · [Concept](/topics/testing/concepts.md#12-database-integration-tests)

    **Follow-up Questions:** How do you keep a sharded suite's database state isolated? What breaks when the image tag floats?

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q19TestcontainersInCiAtScale.java"
        ```

### 20. How does mutation testing with PIT expose weak assertions?

??? question "Reveal answer"
    **Short Answer:** It rewrites the bytecode one small mutation at a time and asks whether the suite notices; a mutant the suite still passes has survived.

    **Deep Explanation:** A survivor means either the assertion was too weak to notice the change or no test reached the code, and coverage cannot tell the two apart — a line can be executed by a test that asserts nothing about it. The score is `killed / (generated - noCoverage)`; the useful output is the list of survivors, not the percentage.

    **Internal Mechanism:** PIT instruments the classes under test and generates mutants — drop the `+ 50` half-up rounding term, flip a comparison, remove a call — then runs the suite against each one. Dropping the rounding term turns 838 cents into 837, which an assertion on the exact amount kills and a "the amount is not negative" assertion does not, so the suite stays green with the bug in place.

    **Example:** [Concept](/topics/testing/concepts.md#17-mutation-testing-coverages-honest-counterpart).

    **Common Mistake:** Chasing a score instead of reading the survivors, or running PIT over the whole codebase on every commit — it multiplies suite time by the number of generated mutants.

    **Production Consideration:** Run PIT on the changed packages per pull request, or nightly over the domain layer, and treat a new survivor in changed code as a review finding with an owner.

    **Follow-up Questions:** When is a surviving mutant acceptable? How do equivalent mutants distort the score?

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q20MutationTestingWithPit.java"
        ```

### 21. How do consumer-driven contracts stop a provider change from breaking its consumers?

??? question "Reveal answer"
    **Short Answer:** The consumer publishes the interactions it relies on, and the provider verifies them in its own pipeline before either side is allowed to deploy.

    **Deep Explanation:** The broker stores one contract per consumer and version and answers `can-i-deploy`: a provider version may be promoted only if every consumer contract it must satisfy has been verified against it. A rename on the provider side therefore fails before the consumer breaks. A provider-side schema test cannot do this, because only the consumer knows which fields it actually reads.

    **Internal Mechanism:** Each interaction is a request plus the response the consumer needs. Verification replays the interaction against the provider build and records a result per contract version, so the deploy gate is a query over stored results rather than a test living inside either service. The contract is versioned and published from the consumer, which is what makes the direction of the guarantee explicit.

    **Example:** [Review](/topics/testing/code-review.md#mocking-away-the-integration) — the interaction the consumer really depends on.

    **Common Mistake:** Publishing "contracts" from the provider, which is a schema test under another name, or gating deploys on the provider's own suite rather than on the consumers' contracts.

    **Production Consideration:** Keep the contracts in the consumers' repositories so a provider can see its real consumers, version them, and make `can-i-deploy` the actual gate. Contract tests do not replace integration tests: they say nothing about the store's semantics. [Concept](/topics/testing/concepts.md#18-consumer-driven-contracts)

    **Follow-up Questions:** How do you handle a consumer that is not in the broker? What happens when a breaking change is intended?

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q21ConsumerDrivenContractsInMicroservices.java"
        ```
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenarios

### 22. A test class is green locally but red in 3 of 20 CI runs, always with `Address already in use`. How do you diagnose and fix it?

??? question "Reveal answer"
    **Short Answer:** Find the attribute that separates the red runs from the green ones, then remove the shared resource rather than the reproduction.

    **Deep Explanation:** The failure is resource contention, not an assertion: 3 of 20 runs fail and the log carries `Address already in use` rather than an assertion message. The failing runs are the ones that ran with parallelism while another job landed on the same agent, because the stub server or container was bound to a fixed port. Test each attribute on its own first — parallelism alone and a shared agent alone do not explain all 20 runs; only the combination does.

    **Internal Mechanism:** A fixed port is a process-global resource, so any parallel fork or co-tenant job can take it first. A dynamic port (WireMock's `dynamicPort()`, Testcontainers' random mapped port) removes the collision, and a fixture created per test removes the second half of the leak. Keeping the parallel reproduction in place after the fix is what makes the verification honest.

    **Example:** [Review](/topics/testing/code-review.md#sleep-based-async-assertions) — a wait that is a guess, the same class of ambient assumption.

    **Common Mistake:** Rerunning the job until it goes green, or pinning the suite to a single fork. Both hide the contention and leave it for a busier agent, where it will look like a new problem.

    **Production Consideration:** After the fix, verify with 200 runs at 0 failures while `forks = 4` is still enabled, and record the flake rate as a metric so a regression is visible. [Concept](/topics/testing/concepts.md#14-determinism-and-flaky-tests) · [Internals](/topics/testing/internals.md#6-testcontainers-lifecycle-and-ryuk)

    **Follow-up Questions:** How would you find the process that owns the port from the CI log? Which other process-global resources can a parallel suite collide on?

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q22CiIntermittentFailureIncident.java"
        ```

### 23. Integration tests are green, but one account lookup fails in production after a PostgreSQL upgrade. What went wrong, and how do you prevent it?

??? question "Reveal answer"
    **Short Answer:** The test asserted the substitute's semantics instead of the store's contract, so the suite was green about a database that does not exist.

    **Deep Explanation:** The suite ran against an embedded substitute that answered case-insensitively and in insertion order. PostgreSQL compares `text` case-sensitively and returns rows in whatever order it likes, so an address typed with a capital letter stopped matching after the upgrade. The test was not wrong about the code — it was wrong about the database. The schema diverged too: the slice built it from the entity mappings while production was migrated by Flyway, so the collation and the unique index were never the same objects.

    **Internal Mechanism:** The unique index and the derived query are case-sensitive objects on PostgreSQL, and a map with a case-insensitive comparator cannot reproduce either. The fix belongs in the service, not the database: normalise the address before the query, which is what the module's `AccountService` already does. Note that the substitute was forgiving in only one dimension — whitespace defeats it too, which shows normalisation was always the application's job.

    **Example:** [Review](/topics/testing/code-review.md#embedded-substitute-hides-postgres-semantics) and the corrected suite in [Solution](/topics/testing/solutions.md#embedded-substitute-hides-postgres-semantics).

    **Common Mistake:** Adding `LOWER(email)` to the query or changing the collation to rescue production while the test keeps asserting the substitute, so the next divergence is just as invisible.

    **Production Consideration:** Run the slice against a real container with `@ServiceConnection` and `replace = NONE`, and migrate the test schema with the same Flyway scripts production uses, so the collation, the index and the types under test are the objects that run in production. [Concept](/topics/testing/concepts.md#12-database-integration-tests) · [Internals](/topics/testing/internals.md#5-property-resolution-dynamicpropertysource-vs-serviceconnection)

    **Follow-up Questions:** Which other semantics does an embedded substitute get wrong? How would you catch a collation change before production?

    ??? example "Example"
        ```java
        --8<-- "modules/12-testing/src/examples/java/lab/testing/questions/Q23PostUpgradeProductionFailureIncident.java"
        ```
<!-- --8<-- [end:scenarios] -->

## Related

- [Testing concepts](/topics/testing/concepts.md)
- [Testing internals](/topics/testing/internals.md)
- [Testing code review](/topics/testing/code-review.md)
- [Testing solutions](/topics/testing/solutions.md)
