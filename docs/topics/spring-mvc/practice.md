# Spring MVC — Practice Q&A

Senior-depth practice from interview-prep §4 filter ordering.

### Q1. How should correlation, security, rate limiting, and shedding filters be ordered?

??? question "Reveal answer"
    - **Order filters according to identity, cost, observability, and dispatch semantics.**
    - **Mechanism:** servlet filters wrap the DispatcherServlet; Spring Security has its own ordered chain. A filter registered in both places can run unexpectedly or twice.
    - **Failure:** shedding before correlation makes rejected calls untraceable; rate-limiting before verified identity trusts spoofable headers; putting expensive work first defeats protection.
    - **Fix:** establish cheap validated correlation early, enforce trusted proxy/identity rules, and apply capacity checks before expensive work. Test early returns, exceptions, async/error dispatch, and MDC cleanup.
    - **Trade-off:** fully authenticated limits cost more than edge prefilters; use layers. OncePerRequestFilter behavior depends on dispatch configuration, not simply its name.
    - **Follow-up:** Which handler formats a security/filter error that never reaches controller advice?
    - **Takeaway:** Filter order is a tested request-path contract.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [API error practice](../rest-api/practice.md)
