# Distributed Systems — Practice Q&A

Senior-depth practice from interview-prep §7 CAP/PACELC.

### Q1. How should CAP and PACELC be applied to a mixed order, cache, and projection system?

??? question "Reveal answer"
    - **Analyze guarantees per operation and boundary instead of labeling the whole application CP or AP.**
    - **Mechanism:** CAP addresses linearizable consistency versus availability during partition; PACELC also highlights latency/consistency trade-offs without a partition.
    - **Failure:** synchronous replication alone does not establish every CP property; a cached read that returns a response is not necessarily highly available under every failure. Outbox buffering depends on an available database and finite storage.
    - **Fix:** specify which writes refuse service without a qualifying leader/quorum, which reads permit stale state, and how clients observe pending outcomes. Include failover fencing and unknown commit results.
    - **Trade-off:** strict decisions may sacrifice latency or availability; product browsing can tolerate staleness that stock allocation cannot.
    - **Follow-up:** Does a primary read bypass every stale layer in the request path?
    - **Takeaway:** Name the operation, consistency model, and failure assumptions.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Solutions](solutions.md)
- [Distributed data practice](../distributed-data-patterns/practice.md)
