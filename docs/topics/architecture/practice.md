# Architecture — Practice Q&A

Senior-depth practice from interview-prep §1 and §10. HyperScale is the supplied case study,
not the architecture implemented by this interview lab.

### Q1. How would you explain an evolving commerce architecture in thirty seconds and defend it?

??? question "Reveal answer"
    - **Lead with the business problem, invariant, architecture choice, and measured limitation.**
    - **Mechanism:** a modular command side can commit order plus outbox; consumers maintain inventory/workflow and a query projection; caches optimize permitted reads.
    - **Failure:** reciting Kubernetes components or unverified benchmark numbers obscures ownership and overstates evidence. Extracting services prematurely turns local calls into failure boundaries.
    - **Fix:** explain one order's commit path, eventual read visibility, duplicate handling, and operational recovery. Separate implemented, experimentally qualified, and proposed capabilities.
    - **Trade-off:** a modular monolith simplifies transactions/deployment; separate query services offer independent scaling but add coherence and recovery costs.
    - **Follow-up:** Which measured bottleneck would justify the next service extraction?
    - **Takeaway:** Describe decisions and guarantees, not a technology inventory.

### Q2. How does schema-per-context support modularity without guaranteeing it?

??? question "Reveal answer"
    - **Schemas express ownership; permissions and code boundaries enforce it.**
    - **Mechanism:** separate namespaces and repository interfaces keep each context responsible for its writes; roles and architecture tests restrict access and imports.
    - **Failure:** one service account with broad grants can still join or mutate every schema. A shared DB remains a resource and failure domain.
    - **Fix:** use least-privilege roles, explicit integration contracts, reviewed cross-context reads where justified, and fitness tests. Plan data migration, transaction removal, and reconciliation before physical extraction.
    - **Trade-off:** avoiding all cross-context joins aids extraction but may add async projections; moving schemas into separate databases is not merely a deployment toggle.
    - **Follow-up:** Which hidden foreign keys, reports, and migrations would break during extraction?
    - **Takeaway:** Logical separation reduces coupling only when enforced and tested.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [System design practice](../system-design/practice.md)
