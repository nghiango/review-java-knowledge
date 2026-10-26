# Testing — Practice Q&A

Senior-depth practice from interview-prep §8, §11, and §18.6. Refer to [Tests](tests.md) for actual lab commands.

### Q1. How do you choose unit, integration, contract, architecture, and end-to-end tests?

??? question "Reveal answer"
    - **Place each assertion at the cheapest layer that faithfully exercises its risk.**
    - **Mechanism:** units check local decisions; slices check framework boundaries; real infrastructure verifies persistence/broker semantics; contracts check wire compatibility; end-to-end tests check selected journeys.
    - **Failure:** mocks prove configured interactions, not unique constraints or offset/commit behavior. A giant Spring context for every rule slows feedback and obscures failures.
    - **Fix:** map invariants and failure windows to explicit tests; enforce dependency boundaries through ArchUnit; retain a small critical journey suite.
    - **Trade-off:** coverage percentage does not measure semantic confidence; infrastructure tests cost more but are indispensable where correctness lives there.
    - **Follow-up:** Which single missing test could permit a duplicate charge?
    - **Takeaway:** Test the risk at its real boundary.

### Q2. Why use PostgreSQL/Kafka Testcontainers rather than substitutes?

??? question "Reveal answer"
    - **Use the actual engine when isolation, SQL dialect, or broker semantics determine correctness.**
    - **Mechanism:** containers exercise constraints, JSONB, locking, upserts, and acknowledgment behavior. Service connections can supply supported connection details to Boot auto-configuration.
    - **Failure:** another database may support similarly named syntax with different semantics; mocked clients cannot reveal crash windows. Custom DataSources may ignore automatically supplied details.
    - **Fix:** pin supported image versions, run real migrations, verify effective connection wiring, and separate fast tests from Docker-dependent tests.
    - **Trade-off:** matching an engine version does not reproduce production storage, topology, load, or failover; retain additional qualification tests.
    - **Follow-up:** How do shared containers avoid test state and lifecycle leakage?
    - **Takeaway:** Similar APIs are not equivalent infrastructure semantics.

### Q3. How do you verify asynchronous flows without sleeps?

??? question "Reveal answer"
    - **Await durable observable effects with bounded deadlines and useful failure diagnostics.**
    - **Mechanism:** Awaitility repeatedly checks eventual conditions; unique identities isolate each test's records. Assert final state, emitted events, and dedup outcomes rather than internal call counts alone.
    - **Failure:** the first successful assertion can pass before an erroneous second effect; a timeout alone does not explain which hop stalled.
    - **Fix:** include duplicate, reorder, crash-after-commit, broker outage, poison message, and failed recovery-publication cases. Use consistency assertions over an observation window where needed.
    - **Trade-off:** longer deadlines reduce false failures but slow diagnosis; deadlines should reflect a bounded test environment, not hide stuck logic.
    - **Follow-up:** How do you prove exactly one durable effect after duplicate delivery?
    - **Takeaway:** Await behavior, then verify its invariant.

### Q4. How do you reproduce an idempotency race reliably?

??? question "Reveal answer"
    - **Coordinate contenders at the vulnerable boundary and inspect committed state.**
    - **Mechanism:** latches/barriers release independent transactions against the same scoped key; database constraints resolve ownership across connections.
    - **Failure:** a single connection or test-wide wrapping transaction serializes access unintentionally; stress repetition alone may never hit the crash gap.
    - **Fix:** use separate sessions, start gates, deterministic fault hooks where available, and assertions for one business operation plus compatible responses. Test same key/different payload and different tenants too.
    - **Trade-off:** deterministic interleavings target known bugs; repeated stress explores others but cannot prove all schedules. JVM memory-model risks need specialized reasoning/testing beyond DB races.
    - **Follow-up:** What happens when the winning connection dies during commit?
    - **Takeaway:** Force the interleaving and verify the durable invariant.

### Q5. What contract tests are missing when services share Java event records?

??? question "Reveal answer"
    - **Shared source types do not prove independently deployed wire compatibility.**
    - **Mechanism:** producer serialization and consumer deserialization vary with defaults, naming, enum handling, nullability, and library versions.
    - **Failure:** the current monorepo compiles while an old deployed consumer rejects a new payload. Structural compatibility can still change business meaning.
    - **Fix:** retain historical golden payloads, test old/new combinations, and enforce schema or consumer-driven contracts where appropriate; include malformed and unknown-field cases.
    - **Trade-off:** shared contracts simplify development but encourage lockstep upgrades; explicit compatibility policy supports independent releases at extra test cost.
    - **Follow-up:** How do retained events from three versions ago deserialize today?
    - **Takeaway:** Test the wire contract across time.

### Q6. How do you remove flakiness without weakening assertions?

??? question "Reveal answer"
    - **Control time, state, ordering, and dependencies instead of retrying tests blindly.**
    - **Mechanism:** inject Clock, isolate topics/keys/schemas, seed randomness, bound waits, and own executor/container lifecycle.
    - **Failure:** parallel tests share counters, fixed ports, or mutable fixtures; automatic retries make genuine races appear green.
    - **Fix:** capture seeds, container output, thread dumps, and last observed state; reproduce the failure and fix the underlying nondeterminism. Quarantine only with tracked ownership and preserved coverage.
    - **Trade-off:** deterministic tests are easier to diagnose but still need separate concurrency and fault qualification.
    - **Follow-up:** Which test assumptions change when CI runs suites concurrently?
    - **Takeaway:** A flaky test is a reliability defect, not harmless noise.

### Q7. How do you design a safe and informative chaos experiment?

??? question "Reveal answer"
    - **Define steady state, a falsifiable hypothesis, blast radius, and stop conditions first.**
    - **Mechanism:** introduce one controlled fault such as broker unavailability or dependency delay, then observe both user SLIs and durable business invariants through recovery.
    - **Failure:** claiming no loss because HTTP stayed green ignores missing projections or compensation. Multiple uncontrolled faults make causality unclear.
    - **Fix:** rehearse in isolated environments, constrain duration, record accepted operation identities, and reconcile after recovery. Obtain separate approval for disruptive production actions.
    - **Trade-off:** realistic topology improves evidence but raises cost and risk; a single-node container outage cannot prove multi-node failover safety.
    - **Follow-up:** How much recovery headroom is required to drain the backlog while new work continues?
    - **Takeaway:** Chaos testing must prove recovery and correctness, not only survival.

### Q8. How should web slices and full integration tests divide responsibility?

??? question "Reveal answer"
    - **Use slices for HTTP contracts and full infrastructure tests for transactional workflows.**
    - **Mechanism:** `@WebMvcTest` focuses MVC wiring with explicit collaborators; `@SpringBootTest` can exercise the complete app plus container-backed dependencies.
    - **Failure:** mocking the service proves validation but not outbox atomicity; replacing security accidentally makes an authorization test meaningless.
    - **Fix:** assert invalid body/status/Problem Details in slices, with intentional security configuration. Test commit, broker delivery, and recovery separately through durable effects.
    - **Trade-off:** full contexts cost startup time; excessive slicing can omit custom filters or advice unless imported. Supported service-connection integration must be verified for custom wiring.
    - **Follow-up:** Why should a transactional test not hide the real commit boundary?
    - **Takeaway:** Framework scope should match the assertion's promise.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Tests](tests.md)
- [Solutions](solutions.md)
- [Load-testing practice](../performance/practice.md)
