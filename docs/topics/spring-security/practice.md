# Spring Security — Practice Q&A

Senior-depth defensive practice from interview-prep §12 and §14. The source's lack of authentication
is a case-study limitation, not a statement about the lab's security module.

### Q1. Which layered controls should protect an order API and its admin operations?

??? question "Reveal answer"
    - **Authenticate identity and authorize every sensitive operation at the application boundary.**
    - **Mechanism:** edge filtering, Spring Security, method/object-level authorization, scoped database access, network policies, and restricted runtime identities protect different boundaries.
    - **Failure:** blocking `/admin` only at ingress leaves internal callers or alternative routes exposed. JWT validation without order ownership checks still permits object-level authorization failures.
    - **Fix:** validate issuer/audience/signature/expiry, enforce tenant/owner constraints in queries and operations, authorize replay separately, and expose only required Actuator endpoints.
    - **Trade-off:** least privilege and segmentation add administration; test both expected allow and deny paths, including direct app access.
    - **Follow-up:** How does a support operator replay events without acquiring unrestricted data access?
    - **Takeaway:** Network location is not business authorization.

### Q2. How do you prioritize hardening work for a commerce service?

??? question "Reveal answer"
    - **Prioritize exploitable correctness and data exposure risks before cosmetic controls.**
    - **Mechanism:** threat-model actors, trust boundaries, assets, and privilege paths. Cover identity/object authorization, secret lifecycle, transport, dependency privileges, and abuse limits.
    - **Failure:** default credentials survive into deployment; secrets leak through logs or config endpoints; globally scoped idempotency keys replay another customer's response.
    - **Fix:** fail startup without required secrets, rotate through approved secret management, apply Kafka ACLs/TLS and protected Redis access, restrict runtime privileges, and cap payloads/pagination.
    - **Trade-off:** transport encryption does not replace authorization; secret injection through environment is not inherently forbidden but has exposure/lifecycle considerations. Test rotation and revocation.
    - **Follow-up:** Which control prevents the highest-impact realistic attack in this deployment?
    - **Takeaway:** Hardening should follow risk and verifiable enforcement.

### Q3. Which OWASP API risks remain despite validation and rate limiting?

??? question "Reveal answer"
    - **Validation cannot substitute for authentication, object authorization, and resource governance.**
    - **Mechanism:** a syntactically valid order ID may belong to another tenant; a valid large request can still exhaust memory; trusted-looking events can carry unsafe data.
    - **Failure:** body-supplied identity overrides the principal, event replays invoke privileged work, or a pagination cap leaves another unbounded export endpoint.
    - **Fix:** derive identity from verified credentials, enforce ownership at every access path, use parameterized SQL, validate event schemas, and bound work by cost as well as request count.
    - **Trade-off:** quotas can affect legitimate bulk clients; provide authorized asynchronous bulk workflows rather than removing all limits.
    - **Follow-up:** How do you prove a caller cannot enumerate another tenant's cached orders?
    - **Takeaway:** Test authorization and abuse as end-to-end properties.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [API practice](../rest-api/practice.md)
