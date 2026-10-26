# Spring Core — Practice Q&A

Senior-depth practice from interview-prep §4. Case-study class names are not claims about this lab.

### Q1. Why prefer constructor injection, and when should a dependency be optional?

??? question "Reveal answer"
    - **Make required collaborators explicit and reject invalid wiring at startup.**
    - **Mechanism:** constructor dependencies allow final fields and ordinary unit construction. Cycles expose unclear responsibilities rather than being hidden by late field injection.
    - **Failure:** nullable optional beans scatter missing-dependency branches; disabling Redis may accidentally disable correctness rather than only performance.
    - **Fix:** use a small interface with an explicit no-op or fallback implementation for genuinely optional behavior. Keep correctness-critical dependencies mandatory.
    - **Trade-off:** too many constructor parameters usually reveal an oversized service; a dependency bag merely hides the design problem.
    - **Follow-up:** When would `ObjectProvider` be better than resolving a bean during construction?
    - **Takeaway:** Dependency structure should reveal operational requirements.

### Q2. How do lifecycle callbacks interact with proxies?

??? question "Reveal answer"
    - **Initialization prepares a target; advice generally applies through the published proxy.**
    - **Mechanism:** instantiation, dependency population, aware callbacks, and initialization processing precede normal use; bean post-processors can wrap targets. Some cycles involve early proxy references.
    - **Failure:** expecting transactional advice on self-calls or initialization methods bypasses the intended boundary. Heavy startup I/O can stall the whole context.
    - **Fix:** place advised work in a separate bean invoked after startup, with explicit readiness and failure handling. Manage shutdown via supported lifecycle callbacks.
    - **Trade-off:** JDK proxies expose interface methods; class proxies cannot override final methods. Exact proxy configuration depends on framework and application settings.
    - **Follow-up:** Why might an injected proxy not have the runtime type of the concrete class?
    - **Takeaway:** Lifecycle execution and intercepted application calls are not interchangeable.

### Q3. Why does injecting a prototype into a singleton not create a new object per call?

??? question "Reveal answer"
    - **Ordinary injection resolves a dependency once when the singleton is constructed.**
    - **Mechanism:** prototype means a new instance per container lookup, not per method invocation; request scope requires an active web request.
    - **Failure:** mutable prototype state becomes effectively shared; request-scoped work invoked from a scheduler has no request context. Prototype destruction is not fully container-managed.
    - **Fix:** use `ObjectProvider`, a factory, or a scoped proxy when repeated contextual resolution is intentional; explicitly own cleanup for created resources.
    - **Trade-off:** deferred lookup moves some errors to runtime and introduces lifecycle indirection. Prefer stateless singleton services where possible.
    - **Follow-up:** How should async work receive request data without retaining a request-scoped bean?
    - **Takeaway:** Scope is a lookup and lifetime contract, not a concurrency guarantee.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Transaction practice](../spring-transactions/practice.md)
