# REST API — Practice Q&A

Senior-depth practice from interview-prep §4, §7, §14, and §18.

### Q1. How do you implement an idempotency state machine without a commit gap?

??? question "Reveal answer"
    - **Atomically connect a scoped operation identity to its committed business result.**
    - **Mechanism:** a unique `(principal/tenant, key)` reservation excludes duplicate execution; a canonical request fingerprint rejects key reuse for different intent; completed requests replay a stable result.
    - **Failure:** business commit followed by independent completion can leave IN_PROGRESS forever. A lease takeover without checking prior business outcome can create another order.
    - **Fix:** commit business state and durable completion together where possible; otherwise persist operation identity in the business record and reconcile unknown outcomes before retrying. Fence competing owners.
    - **Trade-off:** define retention, in-progress responses, result replay authorization, and which failures may be retried. A lease alone does not close the business-effect gap.
    - **Follow-up:** How does a retry after credential or ownership changes avoid exposing an old response?
    - **Takeaway:** Idempotency is durable identity plus atomic outcome, not just a cached response.

### Q2. How should an API expose stable, safe error details?

Merged from: §4 REST errors + §18.1
Also relevant: [Spring MVC](../spring-mvc/index.md)

??? question "Reveal answer"
    - **Map domain failures to documented HTTP semantics using a stable Problem Details contract.**
    - **Mechanism:** `@RestControllerAdvice` maps controller/service errors; security/filter errors may need their own entry point or handler. RFC 9457 defines standard fields and allows extensions.
    - **Failure:** raw exception messages expose SQL or secrets; returning 200 prevents meaningful retries and monitoring. Not every exception is a safe client error.
    - **Fix:** classify validation, authentication, authorization, conflict, dependency failure, and defects; expose safe codes and correlation IDs; log server failures once with protected detail.
    - **Trade-off:** 409 versus 422 for idempotency mismatch is a contract choice requiring documentation; correlation identifiers must be bounded and sanitized.
    - **Follow-up:** How do clients evolve without parsing human-readable error text?
    - **Takeaway:** Error responses are a versioned public API.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Security practice](../spring-security/practice.md)
