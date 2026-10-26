# Case 4 — Notification Engine, Step by Step

Complete [Orders](practice-orders.md) first, then finish this case before [Flash Sales](practice-flash-sale.md).
Follow the entire request lifecycle, not just the queue choice.

## Step 1 — Requirements

### Q1. What does 'notification delivered' mean for email, SMS, and push?

??? question "Reveal answer"
    - **Separate accepted intent, provider acceptance, device delivery, and user reading.**
    - Clarify transactional versus marketing traffic, deadlines, preference rules, quiet hours, channels, cancellation, and legal obligations.
    - Define which categories must be durable and which can expire; a stale OTP may be worse than a dropped message.
    - Duplicate suppression cannot guarantee a human sees exactly one message unless every provider boundary supports the necessary contract.
    - **Trade-off:** provider acknowledgments are often the strongest directly observable outcome, not proof of user receipt.
    - **Follow-up:** Is channel fallback permitted for sensitive content?
    - **Takeaway:** Delivery semantics must match what the system can observe.

## Step 2 — Capacity

### Q2. How do campaigns and provider quotas affect sizing?

??? question "Reveal answer"
    - **Size fan-out, bytes, provider capacity, and deadline backlog independently.**
    - One million recipients over ten minutes requires about 1,667 attempts/s before channel fan-out or retries; a 500/s quota cannot meet that schedule without changed requirements.
    - Model template rendering, preference lookup, enqueue rate, callback volume, attachment sizes, and per-tenant bursts.
    - Prioritize OTPs and critical alerts separately from campaigns; measure oldest eligible work age, not only queue depth.
    - **Trade-off:** more workers cannot overcome contractual provider quotas and may trigger throttling.
    - **Follow-up:** What happens to low-priority work during a provider outage?
    - **Takeaway:** Queue capacity must be measured against deadlines and service rate.

## Step 3 — API and model

### Q3. What data must survive retries and preference changes?

??? question "Reveal answer"
    - **Persist notification intent separately from per-recipient/channel attempts.**
    - API requests contain category, recipient reference, template/version, allowed channels, deadline, and scoped operation identity; avoid trusting arbitrary template execution from callers.
    - Store intent, delivery identity, attempt/provider reference, status, preference provenance, and outbox/inbox. Deduplication includes the business operation and channel policy.
    - Decide whether payload/template content is frozen at acceptance or resolved at delivery; apply required consent checks at the appropriate boundary.
    - **Trade-off:** freezing simplifies audit but retains personal data; late resolution reflects current preferences but changes replay results.
    - **Follow-up:** Does a replayed password-reset event create a fresh valid token?
    - **Takeaway:** Durable identity and content lifecycle prevent unsafe replay.

## Step 4 — Baseline architecture

### Q4. How do you separate fan-out from provider delivery?

??? question "Reveal answer"
    - **Use durable acceptance, bounded fan-out, and isolated channel workers.**
    - Commit intent plus outbox; a planner resolves eligible recipients and creates deduplicated deliveries in bounded pages.
    - Channel workers apply quotas, render safely, call providers with deadlines, and persist outcomes; verified callbacks update delivery state idempotently.
    - Separate priority queues or schedules prevent bulk campaigns blocking critical messages. Apply backpressure to acceptance when durable capacity is exhausted.
    - **Trade-off:** multiple stages add latency but make fan-out and provider failures independently recoverable.
    - **Follow-up:** How does a planner resume after creating half a campaign's deliveries?
    - **Takeaway:** Do not materialize unlimited recipients in one transaction or heap.

## Step 5 — Alternatives

### Q5. One queue, per-channel queues, or synchronous send?

??? question "Reveal answer"
    - **Choose isolation and ordering from category/channel requirements.**
    - Synchronous sending is simple but couples user latency to providers and still has ambiguous-result retries.
    - A shared queue is easy to operate but creates head-of-line and noisy-neighbor risk; channel/priority isolation adds independent capacity and quotas.
    - Partition by a recipient when sequence matters; ordering all recipients globally is usually unnecessary.
    - **Trade-off:** too many queues multiply operations; a small number of bounded workload classes is often enough.
    - **Follow-up:** Must a cancellation message arrive after a confirmation for the same order?
    - **Takeaway:** Isolation should follow business urgency and failure boundaries.

## Step 6 — Concurrency and deduplication

### Q6. How do you prevent duplicate sends without pretending the provider call is atomic?

??? question "Reveal answer"
    - **Deduplicate local deliveries and use provider idempotency where supported.**
    - One durable delivery identity excludes duplicate planning; workers claim with ownership generations and persist provider references.
    - A crash after send but before recording success can still cause duplicates if the provider lacks idempotency or queryable correlation.
    - Reconcile uncertain attempts before resend; make the residual duplicate risk explicit and ensure OTP/token use itself is safe.
    - **Trade-off:** suppressing all uncertain retries risks loss; retrying favors delivery but may duplicate. Categories may choose differently.
    - **Follow-up:** How long must delivery dedup records survive historical replay?
    - **Takeaway:** At-least-once work plus a non-idempotent provider has an unavoidable ambiguity to manage.

## Step 7 — Retry, expiry, and failure

### Q7. What happens during provider throttling or prolonged outage?

??? question "Reveal answer"
    - **Honor category deadlines and provider retry guidance with bounded backoff and jitter.**
    - Classify invalid addresses/tokens separately from transient throttling/timeouts; stop expired work and persist the reason.
    - Open breakers, cap pending attempts, and let safe priority work consume available quota. Recovery must not unleash every delayed retry simultaneously.
    - Fallback channels require policy and duplicate coordination; do not leak sensitive information through an unapproved channel.
    - **Trade-off:** delayed delivery may be acceptable for receipts but not expiring credentials; DLT recovery must revalidate eligibility.
    - **Follow-up:** Should a week-old campaign in the DLT ever be resent automatically?
    - **Takeaway:** A retry after the useful deadline is not recovery.

## Step 8 — Scaling

### Q8. How do you scale a campaign to tens of millions of recipients fairly?

??? question "Reveal answer"
    - **Shard planning and delivery while keeping provider and tenant budgets authoritative.**
    - Use paginated recipient snapshots/checkpoints, bounded batches, and distributed quota allocation; retain exact progress and avoid a hot global campaign counter.
    - Weight scheduling by tenant/service tier and urgency; cap fan-out generation so persistence and queues remain bounded.
    - Scale on eligible backlog age and drain rate, not raw queue length alone; cold workers still need provider permissions and connection budgets.
    - **Trade-off:** local quota leases reduce coordination but allow bounded overshoot; strict global quota requires shared state.
    - **Follow-up:** Can one campaign monopolize all SMS capacity?
    - **Takeaway:** Fair scheduling matters as much as worker count.

## Step 9 — Security and observability

### Q9. How do you secure callbacks, content, and recipient data while measuring delivery?

??? question "Reveal answer"
    - **Treat templates, destinations, and callbacks as untrusted boundaries.**
    - Verify callback authenticity/replay bounds; enforce caller permission to send each category and recipient scope; sanitize templates and avoid rendering arbitrary executable content.
    - Minimize and encrypt sensitive destinations/content; keep tokens out of logs and metric labels, with retention appropriate to category and audit needs.
    - Measure acceptance-to-provider latency, expiry, unknown attempts, bounce/complaint rates, quota saturation, and priority starvation.
    - **Trade-off:** callbacks may arrive late or out of order; use monotonic legal transitions rather than overwriting status blindly.
    - **Follow-up:** Which metric detects delivery degradation when enqueue success remains perfect?
    - **Takeaway:** Provider success and responsible user communication are different outcomes.

## Step 10 — Verification

### Q10. Which tests complete the design rather than only testing a mock send?

??? question "Reveal answer"
    - **Test recovery, policy, and ambiguity as well as happy-path routing.**
    - Exercise duplicate intents, planner crash, send-before-crash, late callbacks, consent changes, expiry, throttling, and priority starvation with controlled provider simulations.
    - Use approved provider sandboxes for contract behavior; never send real messages as an unapproved verification step.
    - Reconcile accepted intents with delivered, expired, suppressed, and explicitly unresolved outcomes under representative fan-out load.
    - **Trade-off:** a mock proves classification but not provider idempotency or rate-limit contracts; state those assumptions explicitly.
    - **Follow-up:** What evidence justifies adding another queue or provider?
    - **Takeaway:** Finish with an accountable outcome for every accepted intent.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Resilience practice](../resilience/practice.md)
- [Next: Flash Sales](practice-flash-sale.md)
