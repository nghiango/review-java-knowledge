# Case 4 — Notification Engine: Senior Interview Walkthrough

Finish [Orders](practice-orders.md), then complete this notification lifecycle before
[Flash Sales](practice-flash-sale.md). Explain each prompt before revealing the guided answer.
This case designs a service; it does not claim production provider guarantees or measured lab scale.

## Scenario and progression

| Dimension | Working assumption |
|---|---|
| Channels | Email, SMS, mobile push; transactional and marketing categories |
| Burst example | One million campaign recipients in ten minutes |
| Priority | Expiring credentials and critical receipts must not sit behind bulk marketing |
| Provider constraint | Illustrative 500 attempts/s on one channel; actual quotas vary |
| Correctness | Durable accepted intent; authorized preferences; explicit duplicate/loss policy |

Follow requirements → capacity → identity/model → architecture → fan-out recovery → preferences →
external ambiguity → ordering → deadlines → fair scheduling → fallback → security → rollout →
regional recovery → evidence. Delivery, provider acceptance, and human reading remain different facts.

## Round 1 — Delivery promises

### Q1. What does a successful notification API response actually mean?

??? question "Reveal answer"

    **Verdict:** Define durable acceptance separately from provider acceptance, device delivery, and reading.

    | Category | Questions that change the design |
    |---|---|
    | OTP / reset link | Useful lifetime, token validity, duplicate tolerance |
    | Transaction receipt | Durability and acceptable delay |
    | Marketing campaign | Consent, quiet hours, cancellation, expiration |
    | Security alert | Allowed channels and escalation policy |

    A provider 2xx is not proof the recipient received or read anything. Some channels expose
    delivery callbacks, others limited evidence; even a read signal may be incomplete. Clarify
    whether fallback is permitted and whether every accepted recipient must reach an accounted
    terminal or unresolved state. Never equate an enqueue metric with a delivery SLO.

    **Interview-ready answer:** “I would specify an observable promise per category and channel.
    Acceptance persists intent; delivery evidence is recorded later. Critical and marketing work
    can have different deadlines, loss tolerance, duplicate policy, and fallback rules.”

    **Follow-up:** Is a message that arrives after its useful deadline considered successful?

## Round 2 — Fan-out and quota sizing

### Q2. Can one million recipients be notified in ten minutes with a 500/s provider quota?

??? question "Reveal answer"

    **Verdict:** Not through that quota with one attempt per recipient, even before retries.

    | Calculation | Result |
    |---|---|
    | 1M / 600 seconds | About 1,667 attempts/s needed |
    | 500 × 600 | At most 300k attempts in ten minutes |
    | 1M / 500 | About 33.3 minutes ideal minimum |
    | 100k backlog, 500/s capacity, 300/s new load | 200/s net drain; about 8.3 minutes |

    Multi-channel fan-out, SMS segmentation, callbacks, preferences, rendering, and retries add
    different costs. Request more quota, extend the deadline, reduce recipients, or use permitted
    alternate providers; worker autoscaling alone cannot change the arithmetic.

    Estimate eligible distinct recipients and payload/reference size. One million-recipient intent
    is not one small worker operation. Measure oldest eligible age and deadline misses by priority,
    not only total queue length.

    **Interview-ready answer:** “I would expose the infeasible deadline early and budget provider
    work separately from queue ingestion. Capacity planning includes retries and reconciliation,
    with a reserved share for critical messages.”

    **Follow-up:** Does a second provider add usable quota if policy forbids cross-provider fallback?

## Round 3 — Identities and data model

### Q3. What must be durable to distinguish intent, recipient delivery, and a provider attempt?

??? question "Reveal answer"

    **Verdict:** One campaign can create many channel deliveries, each with recoverable attempts.

    | Record | Durable meaning |
    |---|---|
    | Intent/campaign | Authorized category, operation key, template/version, audience definition |
    | Recipient snapshot/checkpoint | Stable fan-out boundary and resumable progress |
    | Delivery | Recipient/channel/business identity, deadline, current state |
    | Provider attempt | Stable operation correlation, provider reference, observation |
    | Preference provenance | Applicable consent/category/channel decision |
    | Inbox/outbox | Safe input replay and recoverable next work |

    Creation accepts references and permitted structured variables—not arbitrary executable templates
    or another tenant's recipient list. Status distinguishes suppressed, expired, provider-accepted,
    delivered where observable, and unknown. Define key/fingerprint scope and whether duplicate
    business events with different envelope IDs still represent the same delivery.

    **Interview-ready answer:** “I separate the accepted intent from each recipient/channel action.
    Stable identities and versioned content let the planner and workers recover without duplicating
    fan-out or silently changing what a replay means.”

    **Follow-up:** Should a reset-email replay generate a new token or reuse an already expired business operation?

## Round 4 — Baseline flow and commit boundaries

### Q4. Trace acceptance through planning, provider send, and callback.

??? question "Reveal answer"

    **Verdict:** Commit each required next step durably before acknowledging its predecessor.

    ```mermaid
    flowchart LR
        Caller[Authorized caller] --> Intent[(Intent and outbox)]
        Intent --> Planner[Bounded audience planner]
        Planner --> Deliveries[(Unique deliveries and checkpoints)]
        Deliveries --> Schedule[Priority and quota scheduler]
        Schedule --> Workers[Isolated channel workers]
        Workers --> Provider[External providers]
        Provider --> Callback[Verified callback inbox]
        Callback --> State[(Guarded delivery observations)]
        Workers --> State
    ```

    The planner pages audience work; workers enforce eligibility/deadline and provider budgets.
    Sends occur outside long DB transactions. Worker response and callback update the same logical
    delivery through legal transitions. Required status/recovery intent must not live only in memory.

    **Trade-off:** a synchronous send is simpler for a bounded small workload, but still has an
    external commit gap. Durable staging isolates provider failure at latency/operational cost.

    **Interview-ready answer:** “I would acknowledge only durable intent, then plan and send in
    bounded recoverable stages. Channel failures do not block unrelated classes, and callbacks
    converge on the same delivery identity rather than creating new work.”

    **Follow-up:** Which state recovers a worker that dies after provider acceptance?

## Round 5 — Planner crash and audience mutation

### Q5. A planner creates half a campaign, crashes, and the audience changes. How does it resume?

??? question "Reveal answer"

    **Verdict:** Decide audience snapshot semantics and atomically checkpoint deduplicated fan-out.

    Offset pagination over a changing recipient table can skip or duplicate recipients. Use a stable
    snapshot/list identity or a documented membership boundary, stable ordering, and resumable
    keyset/checkpoint progress. Create bounded batches with unique campaign/recipient/channel identity;
    checkpoint only with the durable batch so crash recovery safely repeats uncertain work.

    | Choice | Benefit | Cost |
    |---|---|---|
    | Frozen membership | Reproducible campaign and audit | Snapshot storage and consent revalidation |
    | Live membership | Current eligibility | Harder definition of completeness and moving boundary |

    Do not create one million rows in one long transaction or materialize the full audience in heap.
    A snapshot of membership does not freeze legal consent indefinitely; sending still applies the
    required policy at the chosen authority boundary.

    **Interview-ready answer:** “I define who belongs to the campaign, then resume bounded unique
    batches from durable progress. A repeated page is harmless; a checkpoint cannot get ahead of
    committed delivery records. Membership and permission to send are separate decisions.”

    **Follow-up:** What if the campaign is cancelled between committing a batch and enqueueing it?

## Round 6 — Consent, quiet hours, and cancellation

### Q6. A user unsubscribes after planning but before send. Can a frozen campaign still send?

??? question "Reveal answer"

    **Verdict:** Apply category-specific eligibility at an explicitly defined send boundary.

    Resolve current consent/quiet-hour rules using authoritative or acceptably fresh policy; record
    the decision/version for audit. A cached preference with a long TTL may violate the required
    withdrawal window. Transactional versus marketing exemptions need actual policy, not an invented
    blanket bypass. Quiet hours require timezone and daylight-saving semantics.

    Cancellation can race an in-flight provider call. A guarded local claim can stop unsent work,
    but cannot recall a message already accepted externally. State that cutoff clearly and account
    for in-flight/unknown deliveries instead of marking every row cancelled blindly.

    **Trade-off:** strict just-in-time eligibility increases dependency and latency; eventual policy
    propagation requires an explicit acceptable delay and may not fit regulatory requirements.

    **Interview-ready answer:** “Planning chooses recipients; current policy decides whether a
    specific send may proceed. I would define the withdrawal/cancellation cutoff, retain evidence,
    and never claim a local flag can unsend an external message.”

    **Follow-up:** What guarantees can the service make if the preference authority is unavailable?

## Round 7 — Ambiguous provider send

### Q7. The provider sends SMS, but the response is lost and the worker lease expires. Can it resend?

??? question "Reveal answer"

    **Verdict:** Local deduplication cannot close a non-idempotent external send gap.

    | Provider capability | Recovery |
    |---|---|
    | Stable idempotency key | Retry under its actual scope/retention contract |
    | Reliable correlation lookup | Reconcile before another send |
    | Neither | Explicit category-specific loss-versus-duplicate decision |

    Keep the same delivery/provider-operation identity through takeover. A lease timeout is not
    evidence the old worker did not send, and a new attempt key can bypass provider deduplication.
    Persist unknown outcomes and classify late callbacks. Critical content should make repeated
    presentation safe where possible; OTP redemption needs its own validity/one-use protection.

    **Trade-off:** never retrying ambiguous sends risks loss; always retrying risks duplicate delivery.
    Do not promise exactly-once human notification without a contract that can support it.

    **Interview-ready answer:** “I would enforce local delivery uniqueness and use provider identity,
    but make residual ambiguity explicit. Recovery preserves correlation rather than pretending a
    worker restart proves the prior external effect did not occur.”

    **Follow-up:** What changes when the provider deduplication horizon is shorter than replay retention?

## Round 8 — Ordering and delayed callbacks

### Q8. Confirmation and cancellation notifications arrive in reverse order. How do you prevent misleading users?

??? question "Reveal answer"

    **Verdict:** Define which messages are sequence-dependent and what current truth may supersede stale work.

    Partitioning by recipient or business aggregate helps only if source sequencing, retries, and
    worker application preserve it. A retry topic or alternate provider can overtake another message.
    Consider versioned business references and eligibility checks that suppress an obsolete unsent
    confirmation after a known cancellation, while preserving audit of what actually happened.

    Delivery callbacks also reorder: an old accepted observation must not erase a later delivered
    or bounced outcome under simplistic last-write-wins. Model compatible observations rather than
    impose an invalid one-dimensional order on distinct provider facts.

    **Trade-off:** strict per-recipient serialization can block unrelated alerts behind one bad
    message. Order only the business sequence that needs it.

    **Interview-ready answer:** “I distinguish business message ordering from provider callback
    ordering. Versions and current eligibility suppress stale work where allowed; preserved observations
    update delivery facts without undoing later evidence.”

    **Follow-up:** Can the system control display order after two providers have accepted the messages?

## Round 9 — Retry, expiry, and DLT

### Q9. The provider is down for an hour. Should every queued message be retried on recovery?

??? question "Reveal answer"

    **Verdict:** A retry after a message's useful deadline may be a defect rather than recovery.

    | Result/category | Action |
    |---|---|
    | Invalid address/token | Terminal classification; clean invalid endpoint under policy |
    | Throttle/transient failure | Bounded jittered retry respecting quota/guidance |
    | Expired OTP | Suppress; do not create a fresh credential from an old replay |
    | Old campaign | Revalidate deadline, cancellation, consent, content |
    | Unknown provider outcome | Reconcile stable identity where possible |

    Store retry eligibility/deadline durably; cap attempts and queue growth. Recovery must not release
    every delayed retry simultaneously. Ordinary Kafka records do not themselves implement arbitrary
    delivery timers; define the scheduling service or retry mechanism explicitly.

    **Interview-ready answer:** “I would classify failures and preserve deadlines through retries
    and DLT handling. Replaying an old event is authorized new execution only if current business
    policy permits it; clearing a queue is not a reason to send obsolete content.”

    **Follow-up:** Who approves retry of a week-old security notification containing stale account information?

## Round 10 — Fair scheduling and quotas

### Q10. How do bulk campaigns coexist with low-latency OTP traffic?

??? question "Reveal answer"

    **Verdict:** Isolate workload classes and allocate real provider capacity, not only queue priority.

    | Technique | Benefit | Cost |
    |---|---|---|
    | Channel/priority isolation | Failure and resource separation | More operational queues/workers |
    | Reserved critical share | Keeps urgent traffic serviceable | Unused capacity policy needed |
    | Weighted fair tenant scheduling | Prevents one campaign monopolizing capacity | State and scheduling complexity |
    | Aging/bulk minimum share | Limits starvation | Can affect urgent latency budget |

    A worker priority flag cannot override a provider quota already consumed by marketing. Coordinate
    provider budgets across pods and reserve capacity for callbacks/reconciliation where those consume
    the same limits. Scale against eligible work age, resource cost, and net drain—not queue length alone.

    **Interview-ready answer:** “I would schedule bounded work classes with tenant shares and explicit
    provider budgets. Critical traffic has reserved service, while bulk has a defined minimum or aging
    policy. Autoscaling is limited by the shared delivery contract.”

    **Follow-up:** Can one tenant label every marketing message urgent and bypass fairness?

## Round 11 — Provider and channel fallback

### Q11. Email provider A times out. Can you immediately use provider B or send SMS instead?

??? question "Reveal answer"

    **Verdict:** Fallback changes duplicate and privacy semantics; it is not just another retry.

    A may already have accepted. B usually does not share A's idempotency namespace, so an immediate
    fallback can deliver twice. Channel changes may expose sensitive data or violate consent/quiet-hour
    rules. Persist fallback ownership and its decision evidence against one logical delivery policy.

    | Option | Accepted cost |
    |---|---|
    | Wait/reconcile A | Delay and unresolved state |
    | Permitted duplicate-tolerant fallback | Possible duplicate, explicitly bounded by category |
    | Strict single-send policy | Potential non-delivery under ambiguity |

    Circuit breakers and health checks indicate capacity risk, not proof of non-delivery. Test
    late callbacks from the first provider after fallback has progressed.

    **Interview-ready answer:** “I would define fallback per category, preserve the original attempt,
    and acknowledge cross-provider deduplication limits. A channel switch must be authorized and
    cannot leak data simply to improve a delivery dashboard.”

    **Follow-up:** Which policy is preferable for a receipt versus a one-time login credential?

## Round 12 — Security and data lifecycle

### Q12. How do templates, callbacks, and recipient data change the threat model?

??? question "Reveal answer"

    **Verdict:** Sending authority and rendering are security boundaries, independent of queue correctness.

    Authorize caller/category/recipient scope and rate/cost budgets. Render trusted versioned templates
    with constrained variables and correct escaping; do not evaluate arbitrary caller-supplied code.
    Verify callback authenticity/replay behavior and restrict administrative resend capability.

    Minimize encrypted destination/content storage, redact tokens and signed links from logs, and
    separate financial/business audit requirements from sampled traces. Retention/deletion must reach
    delivery rows, payload archives, dead letters, replicas, and provider integrations according to
    their real capabilities. Deleting dedup records too early can enable replayed sends.

    **Interview-ready answer:** “I would treat the engine as delegated authority to contact people,
    with constrained content and auditable recipients. Recovery and diagnostics cannot become paths
    for leaking credentials or resending deleted/suppressed content.”

    **Follow-up:** Can an operator safely inspect a failed OTP message without seeing the credential?

## Round 13 — Template and event evolution

### Q13. A template changes during backlog drain and old/new consumers coexist. Which content gets sent?

??? question "Reveal answer"

    **Verdict:** Choose immutable content/version semantics and test historical replay, not just new requests.

    Freeze the template version and required variables at acceptance, or explicitly resolve current
    content under policy. Mixing those choices silently can make identical retries send different
    legal or security wording. Preserve old renderers/schema support for the required horizon or
    migrate unresolved deliveries through a verified compatibility workflow.

    Add optional event fields with semantic defaults; missing does not automatically mean consent
    or marketing permission. In one consumer group versions share partitions; separate groups each
    receive work and can duplicate actual sends unless ownership is controlled.

    **Trade-off:** immutable snapshots improve audit at storage/privacy cost. Dynamic templates
    simplify corrections but change reproducibility and may require revalidation.

    **Interview-ready answer:** “I would make content version and policy explicit in the delivery
    record, test old/new renderers and replay, and coordinate consumer ownership. A template deployment
    must not accidentally resend or reinterpret the backlog.”

    **Follow-up:** How can you withdraw an unsafe template already referenced by a million unsent deliveries?

## Round 14 — Regional outage and delivery accounting

### Q14. How do you fail over delivery workers without resending an entire campaign?

??? question "Reveal answer"

    **Verdict:** Preserve planning checkpoints, delivery identities, and provider ambiguity under fenced ownership.

    Region-local workers may own disjoint delivery partitions, but a takeover must prevent stale
    workers initiating uncontrolled sends and recover unknown attempts. A lost inbox/checkpoint can
    cause repeat planning; a lost provider-operation record can defeat external deduplication.

    Maintain an accounting equation for planned deliveries: pending, suppressed, expired, completed
    under the observable contract, or explicitly unresolved. Provider callback loss and eventual
    status may require reconciliation. Queue lag zero is not proof of completed human delivery.

    **Trade-off:** conservative takeover delays urgent work; aggressive resending increases duplicates.
    State the policy by category and the durability/promotion assumptions that make it credible.

    **Interview-ready answer:** “I would recover the durable delivery ledger and original provider
    identities before resuming, not replay every accepted campaign as new. Old-worker exclusion,
    unknown-attempt policy, and accounting remain part of regional recovery.”

    **Follow-up:** What is the accepted behavior if the old region can still call providers while isolated?

## Round 15 — Evidence and final defense

### Q15. How do you test the complete engine and summarize its guarantees?

??? question "Reveal answer"

    **Verdict:** Verify recoverable intent, policy, fairness, and the provider ambiguity boundary.

    | Test | Evidence |
    |---|---|
    | Planner crash and audience mutation | No skipped/duplicate logical deliveries |
    | Unsubscribe/cancel versus send | Stated cutoff and preserved observations |
    | Send then crash; late duplicate callback | Category-specific recovery without false certainty |
    | Provider throttle/outage | Bounded retries, expired work suppressed, priority preserved |
    | Fallback and regional takeover | Original identities and allowed duplicate/loss policy |
    | Template migration/replay | Stable permitted content and one business owner |

    Measure acceptance-to-provider latency, expiry, unknown age, quota use, fairness/starvation,
    bounce/complaint rates, and reconciliation mismatch. Use approved sandboxes rather than real
    unsolicited sends; mock success does not establish a provider contract.

    **Interview-ready answer:** “I durably accept authorized intent, plan bounded unique deliveries,
    and schedule by channel, urgency, tenant, and provider quota. Workers enforce current policy
    and record verified observations. Unknown sends, fallback, and replay follow explicit category
    contracts. I account for every delivery without pretending provider acceptance proves a human read it.”

    **Follow-up:** Which metric detects failure when API enqueue success remains 100%?

## Review gate

Defend the delivery promise, audience checkpoint, send cutoff, ambiguous external effect,
quota fairness, fallback semantics, and replay/region accounting before the next case.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Resilience mechanisms](../resilience/practice.md)
- [Kafka delivery mechanisms](../kafka/practice.md)
- [Next: Flash Sales](practice-flash-sale.md)
