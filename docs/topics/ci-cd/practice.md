# CI/CD — Practice Q&A

Senior-depth practice from interview-prep §8 deployment and §19.2.

### Q1. What must be true for a rolling Java deployment to avoid request failures?

??? question "Reveal answer"
    - **Old/new versions, traffic transitions, and schema changes must remain mutually compatible.**
    - **Mechanism:** surge starts new capacity, readiness admits traffic, and termination drains old capacity. Endpoint propagation and existing keep-alive connections are not instantaneous.
    - **Failure:** readiness passes before critical initialization; termination ends while callbacks are active; new schema or event types break old replicas.
    - **Fix:** qualify startup and SIGTERM under load, include preStop plus shutdown phases in the termination budget, use compatible expand/contract migrations, and verify no lost accepted work.
    - **Trade-off:** zero unavailable replicas requires spare cluster and DB-connection capacity. A fixed preStop delay is only a mitigation, not universal proof of drain correctness.
    - **Follow-up:** What happens if the new version never becomes ready?
    - **Takeaway:** Zero downtime is an observed outcome, not one rollout setting.

### Q2. 5xx spikes immediately after release: when is rollback safe?

??? question "Reveal answer"
    - **Mitigate quickly only after checking data and contract compatibility of the rollback target.**
    - **Diagnosis:** compare new/old revisions, exceptions, effective configuration, migrations, and changed traffic; confirm the spike aligns with affected paths rather than a shared dependency.
    - **Fix:** pause promotion, roll back or disable the feature through the approved mechanism, and verify errors and business effects recover. Record actions and preserve evidence from the failed revision.
    - **Failure:** reverting a binary after an incompatible migration can worsen the outage; retries may duplicate writes that committed before responses failed.
    - **Trade-off:** roll-forward may be safer than rollback for irreversible changes. Progressive delivery reduces blast radius but needs correct SLI gates and no-data handling.
    - **Follow-up:** Which checks automatically stop a canary before full promotion?
    - **Takeaway:** Every release needs a compatibility-aware recovery path.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Solutions](solutions.md)
- [Database migration practice](../database-sql/practice.md)
