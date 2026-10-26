# Spring Transactions — Practice Q&A

Senior-depth practice from interview-prep §4, §15, and §18. Consult the lab's
[broken boundaries](code-review.md) and [correct implementations](solutions.md).

### Q1. What does `@Transactional` do, and how should propagation be chosen?

Merged from: §4 propagation + §15 REQUIRES_NEW risks

??? question "Reveal answer"
    - **A transaction interceptor applies a local resource boundary according to the selected manager.**
    - **Mechanism:** REQUIRED joins or creates; REQUIRES_NEW suspends the outer scope and starts independently; NESTED usually uses savepoints where supported. SUPPORTS, MANDATORY, NOT_SUPPORTED, and NEVER constrain participation.
    - **Failure:** an inner independent commit survives outer rollback; a suspended transaction may retain its connection while another is acquired, exhausting the pool.
    - **Fix:** choose propagation from business atomicity, not annotation convenience. Test outer/inner failure combinations and manager capabilities against PostgreSQL.
    - **Trade-off:** savepoints do not supply independent durability; not every manager supports nested semantics. Reactive transaction context differs from thread-bound JDBC.
    - **Follow-up:** When can REQUIRED return normally inside but produce `UnexpectedRollbackException` outside?
    - **Takeaway:** Draw commit boundaries before writing annotations.

### Q2. Does `readOnly=true` guarantee fresh reads or prevent writes?

Merged from: §4 read-only routing + §15 lazy-connection follow-up

??? question "Reveal answer"
    - **Read-only is a framework/driver hint and possibly a routing signal, not a universal write prohibition.**
    - **Mechanism:** managers/providers may adjust JDBC settings and ORM flush behavior. A routing DataSource can choose a replica using transaction metadata, but must defer physical acquisition until that metadata exists.
    - **Failure:** eager acquisition chooses the primary before the flag is established; replica reads violate read-your-writes even when monitored lag is small.
    - **Fix:** verify acquisition timing and routing with integration tests, use a lazy connection proxy where needed, and keep correctness-critical reads on an appropriate consistency path.
    - **Trade-off:** provider-specific read-only optimizations are not equivalent to skipping every dirty check; immutable reads still need freshness guarantees.
    - **Follow-up:** What happens when a read-only method joins an existing write transaction?
    - **Takeaway:** Routing and consistency are separate from the annotation's name.

### Q3. Why does self-invocation bypass transactional advice?

??? question "Reveal answer"
    - **Proxy advice runs when a call crosses the proxy, not an internal `this` call.**
    - **Mechanism:** JDK or class proxies intercept eligible external calls; private methods are not overridden, and final methods cannot receive class-proxy advice.
    - **Failure:** a requested REQUIRES_NEW boundary silently does not occur. The same structural trap affects proxy-based caching and async execution.
    - **Fix:** extract a separate service with an explicit responsibility or use `TransactionTemplate`. Self-proxy injection and weaving are options, but add coupling or operational complexity.
    - **Verify:** assert observable commit/rollback behavior rather than merely checking that an annotation exists.
    - **Follow-up:** Why might a mock-based unit test never detect this defect?
    - **Takeaway:** Test the call path through the actual container boundary.

### Q4. How do you guarantee rollback for a checked business failure?

Merged from: §2 checked exceptions + §18.2

??? question "Reveal answer"
    - **Configure the rollback rule explicitly and let the relevant failure cross the boundary.**
    - **Mechanism:** default Spring declarative behavior rolls back for unchecked exceptions and Error, not ordinary checked exceptions; explicit rules or configured global defaults can change this.
    - **Failure:** catching and returning success can commit partial work; an already rollback-only transaction cannot be repaired by swallowing the exception.
    - **Fix:** choose a narrow `rollbackFor` or a deliberate consistent policy; test the resulting database state after the proxied method exits.
    - **Trade-off:** broad rollback on every Exception can roll back work for expected non-fatal outcomes. Compensating external effects remain necessary because local rollback cannot undo HTTP calls.
    - **Follow-up:** Which exception rule wins when broad rollback and narrow no-rollback rules overlap?
    - **Takeaway:** Rollback is a business contract, not an exception naming convention.

### Q5. Are after-commit listeners a reliable replacement for an outbox?

??? question "Reveal answer"
    - **After-commit ordering prevents premature effects but does not make delivery durable.**
    - **Mechanism:** a transactional listener runs at its configured completion phase; without a transaction it normally does not run unless fallback execution is enabled.
    - **Failure:** a process dies after commit but before cache invalidation or event publication. A listener failure cannot undo the committed business transaction.
    - **Fix:** use after-commit for best-effort local actions; persist must-deliver intent in the same transaction through an outbox. Any new transactional write after completion needs an appropriate new boundary.
    - **Trade-off:** synchronous listeners add response latency; async listeners require bounded execution and context propagation. Neither is inherently recoverable after a crash.
    - **Follow-up:** How do you prevent a failed listener from making the client retry an already-committed order?
    - **Takeaway:** Correct timing is not durable delivery.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Outbox practice](../distributed-data-patterns/practice.md)
