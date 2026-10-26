# Case 6 — Distributed Rate Limiter: Senior Interview Walkthrough

Finish [Flash Sales](practice-flash-sale.md), then complete this enforcement lifecycle before
[File Processing](practice-files.md). Answer the prompt before revealing the reasoning and spoken
answer. Algorithm mechanics are canonical in [Resilience](../resilience/practice.md); this case
composes them into a service with explicit failure and ownership semantics.

## Scenario and progression

| Dimension | Working assumption |
|---|---|
| Traffic | Illustrative 100k decisions/s across autoscaled gateways |
| Identity | Verified tenant/user/API policy; weighted operation costs |
| Policy | Distinguish average rate/burst, fixed window, rolling cap, and concurrent work |
| Scale challenge | 100 pods sharing 5,000 permits per identified second |
| Failure contract | Advisory abuse protection and hard financial quota may differ |

Work through contract → sizing → algorithm → atomic decision → hierarchy → ambiguity → grants →
failover → clocks → regions → policy changes → fallback → cardinality/security → telemetry → evidence.
No rate or latency figure is a measured limit of this lab.

## Round 1 — State an enforceable promise

### Q1. What is missing from the requirement '100 requests per minute'?

??? question "Reveal answer"

    **Verdict:** Identity, time semantics, cost, and failure scope define the real guarantee.

    | Clarification | Consequence |
    |---|---|
    | Tenant, user, IP, API, or combination? | Key derivation and hierarchy |
    | Fixed minute or every rolling minute? | Different boundary behavior |
    | Weighted calls or equal requests? | Debit validation and accounting |
    | Attempts or successful business operations? | Admission versus completion accounting |
    | Global or per-region? | Coordination and partition policy |
    | Exact hard cap or bounded overshoot? | Durability, fallback, and lease design |

    Rate limiting controls admission over time; a concurrency limiter bounds active work; load
    shedding protects current capacity. None replaces the others. Define denial/unavailability
    responses and safe Retry-After guidance, plus the decision latency budget within the protected API.

    **Interview-ready answer:** “I would turn the slogan into a measurable policy: trusted identity,
    rate/burst/cost, time window, geographic scope, and outage behavior. A hard billable quota needs
    stronger assumptions than best-effort abuse protection.”

    **Follow-up:** Is admitting twice the nominal count around a calendar boundary allowed?

## Round 2 — State and operation sizing

### Q2. How do decision QPS, active-key count, and skew determine capacity?

??? question "Reveal answer"

    **Verdict:** Count remote operations and retained state per policy dimension, not only HTTP traffic.

    | Assumption | Implication |
    |---|---|
    | 100k requests/s; one remote decision each | 100k operations/s plus replication/expiry |
    | Three independent remote limits | Up to 300k operations/s before batching |
    | 10M active keys at 100 bytes logical state | About 1 GB before key/server overhead and replicas |
    | Sliding log retaining many arrivals/key | State grows with events, not just identities |
    | One tenant dominates half the load | Hot key remains a serialization point |

    Measure script execution, connections, network, CPU, TTL churn, and eviction. Identity churn
    can exhaust memory even with modest legitimate traffic. A tiny local decision benchmark says
    little about network tail latency or a large cardinality workload.

    **Interview-ready answer:** “I size policy dimensions, active identities, hot keys, and exact
    operations per admission. Then I qualify uniform, concentrated, and adversarial cardinality
    workloads with the same protection latency budget.”

    **Follow-up:** What behavior is safe when the store evicts a still-active bucket?

## Round 3 — Algorithm comparison

### Q3. Which algorithm fits fixed caps, smooth traffic, and bounded bursts?

??? question "Reveal answer"

    **Verdict:** Compare the promise each algorithm implements rather than ranking them universally.

    | Algorithm | Benefit | Cost / caveat |
    |---|---|---|
    | Fixed window | Compact counter | Adjacent-window burst |
    | Sliding event log | Precise retained-window accounting | Memory and pruning cost |
    | Sliding counter | Compact approximation | Error bound must fit policy |
    | Token bucket | Sustained refill with bounded burst | Not a strict calendar/rolling count promise |
    | Leaky-bucket scheduling | Smooth output | Waiting queue, deadlines, bounded capacity |

    A bucket with capacity B and refill r admits at most roughly B + r×duration over an interval,
    under its initialization and time assumptions. This differs from “N in every rolling minute.”
    Weighted costs must be positive, bounded, and derived from trusted policy.

    **Interview-ready answer:** “I choose token bucket for an average-rate/burst contract, a precise
    sliding mechanism only when its precision is worth the cost, and scheduling only when waiting
    is allowed. I test the algorithm's actual bound rather than another algorithm's expectation.”

    **Follow-up:** What happens if a request's cost exceeds the maximum bucket capacity?

## Round 4 — Atomic baseline and API

### Q4. Trace one cluster-wide token-bucket decision and its durable or advisory state.

??? question "Reveal answer"

    **Verdict:** Refill, clamp, check, and debit must be one authoritative atomic operation.

    ```mermaid
    flowchart LR
        Request[Request] --> Identity[Verified normalized identity]
        Identity --> Policy[Versioned policy]
        Policy --> Decision[Bounded atomic decision]
        Decision --> Store[(Bucket authority)]
        Decision --> Allow[Allow protected work]
        Decision --> Deny[Deny or explicit fallback]
        Admin[Audited policy control] --> Policy
    ```

    State includes policy/version, tokens or usage, last authoritative time, and expiry. The
    decision returns allowed/denied and safe retry metadata. A read-token/then-write sequence races
    across gateways. Expiry must not reset a partially refilled bucket into an unintended full burst;
    choose TTL relative to safe idle recovery or retain authority for hard quotas.

    **Trade-off:** a shared operation adds a critical-path dependency. Deadlines and isolation are
    part of the design, not optional optimizations after deployment.

    **Interview-ready answer:** “The gateway derives identity and policy, then performs one bounded
    atomic decision. Store initialization, expiry, cost validation, and unavailable outcomes are
    explicitly defined; a timeout is not treated as a proven denial.”

    **Follow-up:** What does the caller do if a debit committed but its response was lost?

## Round 5 — Hierarchical limits

### Q5. User, tenant, and global limits apply together. What if one allows and another denies?

??? question "Reveal answer"

    **Verdict:** Specify partial accounting and cross-key atomicity instead of assuming three decrements are one decision.

    Options include co-locating compatible keys for one atomic decision, conservative hierarchical
    budgets, or explicitly charging attempted admissions even when a later limit denies. Refunds
    across independent counters need identity and race handling; an unconditional increment can
    over-credit after retries or window changes.

    | Design | Benefit | Cost |
    |---|---|---|
    | Co-located atomic hierarchy | Clear combined decision | Hot shard/key-placement constraints |
    | Independent conservative checks | Simpler distribution | False rejections or retained debits |
    | Allocated subtree budgets | Fewer global decisions | Stranded capacity and issuance protocol |

    Clustered multi-key scripts usually have key-placement restrictions; arbitrary tenant/user/global
    keys cannot be presumed atomic across shards. One universal global counter can negate sharding.

    **Interview-ready answer:** “I choose a documented combined-accounting policy and an authority
    that can enforce it. If exact cross-shard atomicity is too expensive, I expose conservative
    rejection or bounded approximation rather than silently inventing refunds.”

    **Follow-up:** Can an attacker force legitimate users to lose quota by triggering late-stage denials?

## Round 6 — Decision-response loss and charging

### Q6. Should a retry after a lost decision response consume another permit?

??? question "Reveal answer"

    **Verdict:** Decide whether quota counts transport attempts, admitted work, or logical business operations.

    Attempt-based protection may intentionally charge a retry again. A billable logical-operation
    quota may require decision/request identity and durable result recovery. This adds state and
    retention that a bare token bucket does not provide.

    A permit does not prove the business operation completed. If failed/cancelled work is refundable,
    its refund must be bounded, once-only, tied to the original policy/window, and consistent with
    any successor window. Returning concurrency permits is another lifecycle, not rate-token refill.

    **Trade-off:** deduplicated decisions improve logical accounting but increase cardinality and
    can themselves become an attack surface. Expired decision IDs need an explicit retry horizon.

    **Interview-ready answer:** “I would make ambiguous response behavior part of the API. Some
    policies charge attempts; others recover a stable logical decision. I would not infer successful
    business completion from an allow response or refund arbitrary counters blindly.”

    **Follow-up:** Could reusing one allowed decision ID authorize several different business operations?

## Round 7 — Local permit grants

### Q7. How can 100 pods share 5,000 permits/s without a remote call for every request?

??? question "Reveal answer"

    **Verdict:** Issue small demand-based grants and bound local consumption under explicit window identity.

    Static 50 permits per pod wastes capacity under uneven traffic. A coordinator can issue disjoint
    batches whose total is bounded for one identified policy/window; each holder atomically consumes
    no more than its grant and stops before authoritative expiry.

    | Grant field | Purpose |
    |---|---|
    | Stable allocation ID | Lost-response retry returns the same grant |
    | Owner/window/policy version | Prevents reuse under another authority or interval |
    | Size and validity | Bound spend and time |
    | Generation when enforced | Supports stale-owner rejection |

    Large batches reduce coordination but strand unused quota. After a holder restart, do not
    reload the full original grant and forget previous consumption. Burn uncertain remaining permits
    or recover consumption through a protocol that prevents reuse. Short-window non-reclamation is
    often the simpler safe starting point.

    **Interview-ready answer:** “The coordinator bounds issuance and pods bound spend. I would
    prefer small grants, idempotent issuance, conservative expiry, and no unsafe reclamation. Local
    speed is useful only while the accounting contract remains clear.”

    **Follow-up:** What proves an unreachable pod cannot spend a reclaimed grant?

## Round 8 — Durable failover

### Q8. Redis loses acknowledged usage during promotion. Can Lua, WAIT, or an epoch fix a hard cap alone?

??? question "Reveal answer"

    **Verdict:** Primary atomicity is not a failover-safe issuance ledger.

    | Mechanism | Limit |
    |---|---|
    | Lua on primary | Serializes its operations, not state lost during promotion |
    | Replica acknowledgment | Improves replication evidence; not every consensus/promotion guarantee |
    | Lease/lock expiry | Does not prove old holder stopped |
    | Epoch field | Useful only where stale authority is actually rejected |
    | Durable ledger | Must preserve acknowledged commits under its real failure/promotion policy |

    Hard grants require atomic identity claim, bounded budget allocation, persisted grant, and
    return after commit. Recover remaining budget from that ledger, treating outstanding issued
    grants as accounted for. A SQL ledger with asynchronous data-loss failover can violate the same
    guarantee if its deployment assumptions are ignored.

    **Interview-ready answer:** “I would qualify durability and promotion, not just script atomicity.
    Strict recovery starts from committed issuance and enforceable ownership. If authority is
    uncertain, new hard grants stop; advisory policies may choose a different bounded fallback.”

    **Follow-up:** If old grants remain locally spendable, what exactly does a new coordinator epoch prevent?

## Round 9 — Time and expiration

### Q9. What happens at window boundaries, clock jumps, or delayed grant delivery?

??? question "Reveal answer"

    **Verdict:** Time source and expiration are part of the enforcement protocol.

    Use a defined authority time/window identity. Monotonic elapsed time is useful locally but
    does not synchronize hosts. Clamp or otherwise handle unexpected clock movement according to
    the algorithm; a backwards timestamp must not mint tokens, and a forward jump can change refill.

    Fixed-window permits near two adjacent boundaries may double a rolling-interval count without
    violating either fixed window. Repeated full-TTL writes can accidentally extend a window.
    Grant recipients must reject obsolete windows and stop conservatively before expiry, accounting
    for transport delay and clock uncertainty; delayed delivery cannot reset validity locally.

    **Trade-off:** conservative margins strand quota; exact synchronized global timing costs
    coordination and still depends on stated clock assumptions.

    **Interview-ready answer:** “I would identify the authoritative interval and test boundary,
    delay, and clock histories. TTL is cleanup, not a substitute for a precise validity contract.
    I would never describe a fixed-window test as proof of a rolling-rate guarantee.”

    **Follow-up:** Can a grant arriving just after its window closes be safely used for the next one?

## Round 10 — Multi-region policy

### Q10. Can both partitioned regions enforce one strict global quota while accepting arbitrary new work?

??? question "Reveal answer"

    **Verdict:** Not without a restriction such as exclusive preallocated budgets or unavailable coordinated decisions.

    | Design | Benefit | Trade-off |
    |---|---|---|
    | Shared global authority | One exact accounting boundary | Cross-region latency and partition dependence |
    | Exclusive regional budgets | Local progress within owned allowance | Stranded capacity and transfer coordination |
    | Independent approximate limits | Local availability | Explicit overshoot bound, not strict global cap |

    Transfers remove source spend authority before destination use. A health check does not reveal
    consumed quota or stop isolated gateways. Different policies may tolerate different error
    bounds; separate hard financial quota from soft anti-abuse protection.

    For approximate local buckets, quantify aggregate burst capacity and fallback duration across
    the maximum holder count, including autoscaling. “Eventually consistent” without a numerical
    accepted exposure is not a design contract.

    **Interview-ready answer:** “I would choose the global consistency boundary explicitly. During
    partitions, regions can spend exclusive existing rights, accept bounded approximation, or
    refuse new coordinated admission. They cannot each assume the full global remainder.”

    **Follow-up:** How does adding fifty gateways change your worst-case overshoot calculation?

## Round 11 — Policy rollout and rollback

### Q11. A tenant changes from 1,000 to 100 RPS while half the pods still use old policy. What happens?

??? question "Reveal answer"

    **Verdict:** Version configuration and usage together with an explicit transition rule.

    Define effective time, cache propagation, token clamp/carry policy, and treatment of outstanding
    old grants. Simply using a new key gives everyone a fresh burst; retaining old tokens unchanged
    may violate the intended downgrade. Strict immediate changes require enforceable revocation or
    waiting for old rights to expire before the stronger guarantee begins.

    Support shadow evaluation before enforcement, audited changes, bounded rollout, and rollback
    that does not mint additional allowance. Different algorithm versions may interpret stored
    counters differently; migrate state or maintain a conservative compatibility interval.

    **Trade-off:** instantly tighter enforcement may require coordination and customer-visible
    denial; eventual rollout must expose the temporary mixed-policy semantics.

    **Interview-ready answer:** “I would treat policy change as an accounting migration. Version,
    effective time, outstanding grants, token carry, and rollback are specified—not hidden inside
    cache refresh. Shadow mode validates a new policy without counting it as enforcement.”

    **Follow-up:** Can a customer oscillate tiers to repeatedly receive a full fresh bucket?

## Round 12 — Limiter outage and fallback

### Q12. The limiter is slow. Should it fail open, closed, or use local protection?

??? question "Reveal answer"

    **Verdict:** Choose fallback per policy and keep the protection layer from becoming an unbounded wait queue.

    | Policy | Possible behavior |
    |---|---|
    | Best-effort abuse control | Conservative local limit with explicit exposure |
    | Hard billable quota | Stop new unverified admission |
    | Service-overload protection | Local concurrency/saturation shedding plus safe existing grants |
    | Safety-sensitive operation | Explicit unavailable response when authority is missing |

    Use a short total decision deadline, bounded client concurrency, and breaker. Do not retry
    ambiguous decisions repeatedly. Recovery must avoid synchronized new full buckets and a sudden
    flood from all fallback holders. Already-issued rights may be used only within their still-valid
    authority; fallback is not permission to reset accounting.

    **Interview-ready answer:** “I would not adopt one blanket fail-open rule. The policy specifies
    financial/abuse exposure, latency, and local fallback bounds. Limiter recovery is staged and
    monitored against the protected service's actual health.”

    **Follow-up:** Which metric tells you a low-latency fallback is silently violating a hard quota?

## Round 13 — Identity and cardinality attacks

### Q13. How do you prevent forged identities, bypass routes, and memory exhaustion?

??? question "Reveal answer"

    **Verdict:** Derive trusted bounded keys and apply controls before expensive accounting where appropriate.

    Normalize authenticated tenant/user/API identity; validate forwarding headers only at trusted
    proxy boundaries. A caller-provided “premium” label or arbitrary API-key string must not choose
    policy. Cover alternate routes and internal calls according to the actual protection boundary.

    IP limits affect shared NAT and can be evaded; account limits permit account farms. Use layered
    controls with explicit privacy policy, not a claim that either proves fairness. Bound unknown
    identity allocation and retention; eviction of active hard state cannot safely recreate full quota.

    Administrative policy access needs audit and least privilege. Avoid per-user metric labels or
    logging credentials; sampled/protected diagnostics can carry operation correlation.

    **Interview-ready answer:** “I would secure identity and policy selection, limit state creation,
    and verify coverage of every protected path. Cardinality and eviction behavior are correctness
    concerns, not only memory tuning.”

    **Follow-up:** Can an unauthenticated attacker make the limiter allocate millions of durable decision IDs?

## Round 14 — Diagnosis and observability

### Q14. Denials rise while backend latency is healthy. How do you tell protection from a defect?

??? question "Reveal answer"

    **Verdict:** Correlate policy/accounting evidence with demand and protected-service outcomes.

    | Signal | Interpretation to investigate |
    |---|---|
    | Allow/deny by bounded policy/tier | Expected quota use versus bad rollout |
    | Decision p95/p99 and timeout/fallback | Store/network saturation or fail-policy activation |
    | Issued/used/wasted grants | Stranding and uneven allocation |
    | Key churn/eviction | Identity attack or unsafe lifecycle |
    | Backend admitted load and latency | Whether protection is actually effective |

    Inspect time/window identity, policy versions, retry charges, and hierarchical retained debits.
    Healthy backend latency can mean successful protection or wrongful rejection; it cannot decide
    alone. Compare shadow policy and sampled accounting histories while protecting customer data.

    **Interview-ready answer:** “I would investigate both fairness and protection. I compare admitted
    demand, quota usage, versions, decision failures, and backend saturation. A graph of denials
    without accounting context cannot justify raising limits or disabling enforcement.”

    **Follow-up:** Which evidence distinguishes quota stranding from genuinely exhausted global capacity?

## Round 15 — Verification and final defense

### Q15. How do you prove the actual admission bound and summarize the design?

??? question "Reveal answer"

    **Verdict:** Test concurrent histories against the promised algorithm and failure model.

    | Test | Expected evidence |
    |---|---|
    | Boundary/clock/TTL histories | Correct time and burst semantics |
    | Concurrent weighted debits | No race-based over-admission |
    | Multi-limit denial and decision retry | Defined partial accounting |
    | Grant response loss/restart/reclaim | No repeated issuance or unsafe spend |
    | Promotion/partition/policy rollout | Qualified durability and exposure bounds |
    | Hot key/cardinality/outage load | Bounded latency/resources and protected backend |

    Use controlled store failover and representative traffic, not only a single-process counter
    test. Distinguish empirical qualification from proof under untested distributed schedules.

    **Interview-ready answer:** “I specify identity, rate/burst/cost, scope, and outage semantics.
    Atomic authority handles decisions; local grants reduce coordination under bounded ownership.
    Policy changes, time, failover, and hierarchy have explicit accounting rules. I separate hard
    quota from advisory protection and validate both admission histories and backend health.”

    **Follow-up:** Which assumption would invalidate the strongest overshoot bound you quoted?

## Review gate

Defend rate versus concurrency, fixed versus rolling windows, hierarchy accounting, grant restart,
clock behavior, durable failover, policy transition, and fallback before the file-processing case.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Resilience algorithms](../resilience/practice.md)
- [Flash-sale grant/ownership application](practice-flash-sale.md)
- [Next: File Processing](practice-files.md)
