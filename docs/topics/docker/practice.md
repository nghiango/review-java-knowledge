# Docker / Runtime Operations — Practice Q&A

Senior-depth practice from interview-prep §8 and §19. Kubernetes questions are routed here as
container-runtime operations; the source Helm chart is not a deployed lab artifact.

### Q1. Which Kubernetes resources and guarantees matter to a stateful-backed Java service?

??? question "Reveal answer"
    - **Separate application rollout, durable storage, traffic health, and failure-domain resilience.**
    - **Mechanism:** Deployments replace stateless replicas; StatefulSets provide stable identity/storage association; services route eligible endpoints; probes and disruption budgets govern different actions.
    - **Failure:** StatefulSet identity is not database replication or backup. A PDB constrains supported voluntary evictions, not every rollout, crash, or node failure.
    - **Fix:** budget requests/limits, spread across real failure domains, persist required data, restrict privileges/network access, and test dependency failover plus restore.
    - **Trade-off:** soft ScheduleAnyway placement may not achieve required resilience; hard constraints can prevent scheduling under capacity loss.
    - **Follow-up:** Does running three replicas on one failure domain actually meet the availability requirement?
    - **Takeaway:** Replica count is not a resilience proof.

### Q2. One pod misbehaves while others are healthy: how do you investigate safely?

??? question "Reveal answer"
    - **Compare a bad and good instance before discarding the evidence.**
    - **Diagnosis:** inspect revision, node, restart reason, resource pressure, GC, dependency routes, cache versions, and clock behavior. Capture prior-container logs when relevant.
    - **Fix:** use approved traffic-drain/isolation mechanisms, preserve diagnostics, and replace the instance only after checking workload ownership and disruption effects.
    - **Failure:** changing controller selector labels can orphan a pod or cause a replacement and unexpected routing; draining a node affects unrelated workloads and requires approval.
    - **Trade-off:** isolating a replica reduces capacity; verify the remaining fleet can meet admitted demand without cascading failure.
    - **Follow-up:** Do all unhealthy instances share a node, zone, or configuration revision?
    - **Takeaway:** Isolation should be controlled, reversible, and evidence-preserving.

### Q3. A pod crash-loops or never becomes ready: what is your investigation order?

??? question "Reveal answer"
    - **Distinguish application startup failure, resource kill, and health-check failure.**
    - **Diagnosis:** inspect events, termination reason, previous logs, config/secret availability, migration state, startup duration, and the exact readiness response.
    - **Failure:** aggressive liveness kills a warming JVM; shared dependency readiness removes every replica; a failed migration makes every new pod fail identically.
    - **Fix:** stop or roll back the unsafe rollout when compatible, correct configuration/probe budgets, and preserve old healthy capacity. Check scheduling/quota failures separately from application health.
    - **Trade-off:** startup probes allow initialization, not an unlimited hang. A rollout can stall safely due to readiness and surge settings; PDBs are not the general mechanism controlling Deployment rollouts.
    - **Follow-up:** Can old code still operate after the migration that accompanied the failed release?
    - **Takeaway:** Identify who terminated or rejected the pod before changing thresholds.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Solutions](solutions.md)
- [Boot lifecycle practice](../spring-boot/practice.md)
- [Deployment practice](../ci-cd/practice.md)
