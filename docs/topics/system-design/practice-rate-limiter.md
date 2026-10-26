# Case 6 — Distributed Rate Limiter, Step by Step

Finish [Flash Sales](practice-flash-sale.md), then complete this case before [File Processing](practice-files.md).
Algorithm mechanics are linked from [Resilience practice](../resilience/practice.md); this case designs the complete service.

## Step 1 — Requirements

### Q1. Which questions define the limiter's actual contract?

??? question "Reveal answer"
    - **Specify identity, scope, rate, burst, cost, and outage behavior.**
    - Clarify per-user/tenant/API/region rules, paid tiers, exact versus approximate limits, request weights, and whether limits apply to attempts or successful completion.
    - Distinguish fairness quotas from overload protection. Strong global enforcement and disconnected regional availability conflict during partitions.
    - Set a decision-latency budget as part of the API budget, plus documented response and Retry-After behavior.
    - **Trade-off:** hard financial quotas need different fail policies than best-effort anti-abuse controls.
    - **Follow-up:** Is a small bounded overshoot acceptable, and how large?
    - **Takeaway:** '100 requests per minute' is an incomplete specification.

## Step 2 — Capacity

### Q2. What determines limiter storage and throughput requirements?

??? question "Reveal answer"
    - **Count active identities, dimensions, state size, and operations per request.**
    - At 100k requests/s, one remote atomic decision means 100k decisions/s plus replication and expiry work; multiple independent limits multiply the operation budget.
    - Ten million active keys at an assumed 100 bytes already imply roughly 1 GB before server/key overhead and replication; sliding logs add per-event state.
    - Estimate hot-tenant skew and connection concurrency, not only aggregate requests. TTL churn and identity attacks can dominate memory.
    - **Trade-off:** local quota leases reduce remote calls but reserve permits and permit bounded inconsistency.
    - **Follow-up:** What happens when most requests belong to one tenant key?
    - **Takeaway:** Cardinality and skew drive capacity as much as QPS.

## Step 3 — API and model

### Q3. Which data and response fields make policy enforceable?

??? question "Reveal answer"
    - **Version policy separately from ephemeral usage and derive identity from trusted context.**
    - Model policy ID/version, scope, rate/burst/cost, enforcement mode, and failure policy; state contains tokens/window accounting plus expiry or lease ownership.
    - A decision returns allowed/denied, safe retry guidance, and policy metadata as appropriate; avoid exposing other tenants' usage.
    - Define whether retries of the same operation consume another permit and whether cancellation returns permits; a rate token is not necessarily an in-flight concurrency permit.
    - **Trade-off:** rich hierarchical policies require atomic interaction rules, not merely several independent decrements.
    - **Follow-up:** If the tenant limit denies after the user limit debits, is the debit retained?
    - **Takeaway:** The decision protocol must define partial accounting.

## Step 4 — Baseline architecture

### Q4. How does a gateway enforce a cluster-wide token bucket safely?

??? question "Reveal answer"
    - **Perform refill, capacity clamp, cost check, and debit atomically at the authoritative store.**
    - Gateway authenticates/normalizes identity, selects a versioned policy, and calls a bounded shared operation. Denials do not proceed to the protected expensive resource.
    - Set TTL so state can expire only when resetting it does not create an unintended fresh burst; validate positive rates, capacities, and request costs.
    - Use one defined time source for shared accounting; monotonic local elapsed time does not synchronize separate hosts.
    - **Trade-off:** the store is now on the critical path and needs deadlines, isolation, capacity, and an explicit fallback policy.
    - **Follow-up:** What if the decision succeeded but the network response was lost?
    - **Takeaway:** Distributed atomicity and ambiguous decisions must be explicit.

## Step 5 — Algorithm comparison

### Q5. Why choose token bucket over fixed window, sliding log, or leaky bucket?

??? question "Reveal answer"
    - **Match the algorithm to burst semantics and acceptable state/precision costs.**
    - Fixed windows are compact but allow boundary bursts; sliding logs retain exact arrivals but cost memory and cleanup; sliding counters approximate; token buckets permit a bounded burst at a sustained refill.
    - Leaky-bucket scheduling smooths output but creates waiting rather than merely admitting/rejecting; bounded queue and deadlines remain necessary.
    - Compare weighted cost support, eviction semantics, clock behavior, and implementation atomicity, not just a textbook shape.
    - **Trade-off:** exactness can cost memory/latency without improving the product's real fairness objective.
    - **Follow-up:** Does a 60-second token refill policy mean no more than N in every rolling minute?
    - **Takeaway:** Algorithms implement different promises.

## Step 6 — Concurrent and regional enforcement

### Q6. How can global limits work across shards and regions?

??? question "Reveal answer"
    - **Choose strong shared accounting or explicitly bounded regional allocations.**
    - A single authority serializes a key but adds cross-region latency and partition dependence; regional quota leases distribute disjoint budgets with durable ownership.
    - Multi-key atomic scripts in clustered stores may require co-location; arbitrary user/tenant/global keys cannot be assumed atomically updatable across shards.
    - Lease reassignment must fence prior owners; failover that loses usage can create extra allowance. Quantify the largest possible overshoot.
    - **Trade-off:** conservative local quotas strand unused capacity; optimistic local limits improve utilization but weaken the global cap.
    - **Follow-up:** Can two disconnected regions each spend the complete remaining tenant budget?
    - **Takeaway:** State the consistency boundary of the quota.

## Step 7 — Failures

### Q7. Should the limiter fail open or fail closed when its store is slow?

??? question "Reveal answer"
    - **Use a policy-specific fallback rather than one universal rule.**
    - Best-effort protection may use a conservative local limiter when shared state fails; a hard billable quota may reject or stop accepting new work.
    - Apply a short total decision deadline, breaker, and bounded clients; otherwise protection adds queueing and causes the very outage it should prevent.
    - Recovery must avoid synchronized fresh buckets or unlimited fallback transitions. Include failover/replication-loss assumptions.
    - **Trade-off:** fail-closed harms legitimate availability; fail-open can amplify abuse and financial exposure.
    - **Follow-up:** How long may fallback continue before operators are alerted or policy tightens?
    - **Takeaway:** Outage behavior is part of the quota contract.

## Step 8 — Scaling

### Q8. How do you reduce shared-store cost without losing control of hot tenants?

??? question "Reveal answer"
    - **Use bounded local allocations and hierarchical admission where their error bounds are acceptable.**
    - Cache policy, not unchecked decisions; allocate short-lived disjoint permit blocks to instances and account for unused reservation on failure.
    - Shard independent identities, isolate high-cost tenants, and use weighted request cost. A single strict global key cannot be made independent by hashing it randomly.
    - Bound active-key state and reject malformed identity churn before expensive accounting.
    - **Trade-off:** larger leases reduce calls but increase stranded allowance and recovery complexity; smaller leases improve precision at higher coordination cost.
    - **Follow-up:** What happens to unused permits when an autoscaled pod disappears?
    - **Takeaway:** Optimization must expose its precision and ownership costs.

## Step 9 — Security and telemetry

### Q9. How do you prevent identity spoofing and know the limiter is helping?

??? question "Reveal answer"
    - **Derive identity from verified credentials or a trusted, normalized proxy boundary.**
    - Do not trust caller-supplied API-key strings or forwarding headers without validation; separate administrative policy changes and audit them.
    - Measure decision latency, allow/deny/fallback, key cardinality, store saturation, and protected-service outcomes with bounded policy/tier labels.
    - Detect users bypassing limits through alternate routes; apply separate ingress abuse controls before costly authentication where appropriate.
    - **Trade-off:** per-IP limits affect shared NAT; per-account limits permit account farms. Neither alone proves fairness.
    - **Follow-up:** How is a bad policy rolled back without granting a new burst to everyone?
    - **Takeaway:** Identity and policy lifecycle are security boundaries.

## Step 10 — Verification

### Q10. What tests establish the claimed rate and overshoot bounds?

??? question "Reveal answer"
    - **Test concurrent histories, clock changes, expiry, topology changes, and outage transitions.**
    - Exercise window boundaries, fractional refill, weighted cost, key expiry, duplicate decision requests, multi-pod races, quota leases, policy updates, and store failover.
    - Verify actual admitted count against the algorithm's promised bound, not an incompatible fixed-window interpretation.
    - Load-test uniform and hot-tenant traffic, recording decision latency and protected-service saturation during limiter failures.
    - **Trade-off:** throughput benchmarks without adversarial concurrency can hide over-admission; strong global guarantees require strong assumptions in the backing system.
    - **Follow-up:** Which failure invalidates your overshoot calculation?
    - **Takeaway:** Finish with a measurable enforcement bound.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Resilience algorithms](../resilience/practice.md)
- [Next: File Processing](practice-files.md)
