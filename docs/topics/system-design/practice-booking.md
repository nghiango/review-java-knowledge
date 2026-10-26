# Case 8 — Hotel / Ticket Booking, Step by Step

Complete [File Processing](practice-files.md) first. Finish the entire booking lifecycle before
returning to the [mixed question bank](questions.md). Assigned seats and pooled room inventory
have different invariants; do not silently use one model for both.

## Step 1 — Requirements

### Q1. How do assigned seats differ from hotel-room availability?

??? question "Reveal answer"
    - **An exact seat is an exclusive resource; room-type inventory is a bounded quantity across dates.**
    - Clarify all-or-nothing group booking, hold duration, renewals, cancellation, overbooking policy, payment timing, and accessibility/seat adjacency requirements.
    - One seat cannot be confirmed twice; a multi-night room booking must respect capacity for every relevant date. A displayed map can lag without authorizing a second allocation.
    - Define the authority's expiry rule rather than promising timers fire at exactly ten minutes.
    - **Trade-off:** strict booking consistency may require rejecting service during authority failure.
    - **Follow-up:** Can two individually valid single-night allocations invalidate a requested multi-night stay?
    - **Takeaway:** Identify the whole resource/time invariant first.

## Step 2 — Capacity

### Q2. How do 20,000 simultaneous buyers affect the design?

??? question "Reveal answer"
    - **Distinguish broad browsing from concentrated allocation contention.**
    - Estimate event-level arrivals, seat-map frequency, targeted seat skew, group size, payment duration, expiration rate, and the finite number of sellable resources.
    - Waiting rooms and bounded admission reduce allocation contention; stale availability must return a clear conflict rather than become an unbounded retry loop.
    - Budget aggregate updates and expiry scans, not just GET throughput. A shared payment provider can dominate hold duration.
    - **Trade-off:** pushing every seat change to every viewer may cost more than bounded polling or section-level updates.
    - **Follow-up:** What happens when most buyers request the same front-row seats?
    - **Takeaway:** Contention follows the resource, not the number of app replicas.

## Step 3 — API and model

### Q3. Which durable records establish hold ownership and prevent double booking?

??? question "Reveal answer"
    - **Use an authoritative reservation state machine tied to immutable resource identity.**
    - APIs search availability, create/query/renew a hold, confirm payment, and cancel. Each mutation has authorized ownership and scoped operation identity.
    - Assigned seats need a unique event/seat allocation record with state, owner, hold ID, version, and expiry. Pooled rooms need capacity accounting by room type/date plus reservation lines.
    - Store payment intent/reference, transition audit, outbox, and dedup identities. Enforce positive quantities and unique logical requests.
    - **Trade-off:** one row per seat is simple; multi-resource bookings require transactional coordination and consistent lock order.
    - **Follow-up:** Does retrying the same hold request extend its expiration or return the original result?
    - **Takeaway:** Ownership, identity, and expiry must survive process restart.

## Step 4 — Baseline flow

### Q4. How does hold → pay → confirm work end to end?

??? question "Reveal answer"
    - **Reserve atomically, pay outside the transaction, then confirm through a guarded durable transition.**
    - Authenticate and admit; atomically acquire all required resources for a hold and record expiry/outbox. Return the hold identity and authoritative deadline.
    - Start payment using stable provider identity; confirm only if the hold transition is still valid, or enter a defined recovery path if payment is late.
    - A reaper or durable timer conditionally expires only the same active hold/version; client cancellation uses the same transition discipline.
    - **Trade-off:** external payment and local booking cannot be one ordinary ACID transaction; reconciliation covers ambiguous outcomes.
    - **Follow-up:** What if the confirmation response is lost after the booking commits?
    - **Takeaway:** Stable identity joins independent failure boundaries.

## Step 5 — Alternatives

### Q5. Database locking, optimistic updates, or a Redis TTL barrier?

??? question "Reveal answer"
    - **Pick a durable authority before choosing a fast contention barrier.**
    - Conditional/versioned updates detect races; row locking can serialize short multi-resource checks safely with consistent ordering. Locks are not held during human checkout or provider calls.
    - Redis can accelerate availability or admission, but TTL expiry/replication loss cannot be the sole proof that a seat is unbooked.
    - If Redis owns allocation, articulate its durability, failover, fencing, reconciliation, and acknowledged-loss assumptions explicitly.
    - **Trade-off:** DB authority may limit hot allocation throughput; caches reduce reads while bounded admission protects writes without weakening invariants.
    - **Follow-up:** Which store decides when Redis says AVAILABLE but the database says BOOKED?
    - **Takeaway:** Atomic in-memory execution is not automatically durable booking correctness.

## Step 6 — Concurrency

### Q6. How do group holds, renewals, and expiry races remain correct?

??? question "Reveal answer"
    - **Make each transition conditional on current ownership, generation, and legal state.**
    - Acquire multiple seats/dates in one bounded transaction with consistent lock order or an equivalent proven protocol; either all succeed or none do when the product requires atomic groups.
    - Renew only an authorized current hold and cap extensions. Expiry messages carry hold identity/version so old timers cannot release a renewed or reassigned resource.
    - Confirmation competing with expiry has one winner; late payment is reconciled through rebooking/refund policy rather than overwriting a new owner's booking.
    - **Trade-off:** large group atomicity increases lock scope; cap group size or explicitly support partial acquisition with compensation.
    - **Follow-up:** Can checking only the current user ID distinguish two separate holds by the same person?
    - **Takeaway:** A timer targets a particular reservation generation.

## Step 7 — Failure recovery

### Q7. What recovers abandoned holds and unknown payments after restart?

??? question "Reveal answer"
    - **Durable deadlines and reconciliation, not ephemeral expiry notifications alone.**
    - Scan indexed due holds in bounded batches or use durable scheduled work, then conditionally expire; delayed message redelivery must be harmless.
    - Ordinary Kafka topics do not natively schedule arbitrary per-record delivery timestamps; any timer service/retry mechanism must be designed explicitly.
    - After provider ambiguity, reconcile confirmed payment before releasing/reallocating where required. Restore workflows from durable state rather than assuming client sessions survive.
    - **Trade-off:** expiration processing may be delayed, but an authoritative deadline check can prevent invalid confirmation while cleanup catches up.
    - **Follow-up:** What happens if the expiry worker is unavailable for an hour?
    - **Takeaway:** Expiry is durable business logic with observable backlog.

## Step 8 — Scaling

### Q8. How should booking data be partitioned and cached?

??? question "Reveal answer"
    - **Partition around resource ownership and keep strict allocations near their authority.**
    - Event or property identity keeps many transactions local; cross-event itineraries need explicit distributed workflow and partial-failure semantics.
    - Cache venue/catalog/map snapshots with versions and stale indicators, but validate every allocation at the authority. Separate read update fan-out from booking writes.
    - A hot event remains hot after hashing by event; admission, sections, or disjoint capacity budgets may be needed without duplicating ownership.
    - **Trade-off:** regional reads improve latency; multi-region write ownership and failover must fence old writers before accepting new holds.
    - **Follow-up:** Can the shard key support one atomic booking for several room dates?
    - **Takeaway:** Partition the invariant intentionally.

## Step 9 — Security and observability

### Q9. Which controls and signals protect buyers and detect booking drift?

??? question "Reveal answer"
    - **Treat holds as valuable resources with authenticated ownership and auditable transitions.**
    - Enforce per-buyer hold quotas/renewal bounds, bot controls, tenant authorization, and signed/verified payment callbacks. Do not accept seat price or ownership solely from client input.
    - Track hold age/expiry delay, confirmation conflicts, payment unknowns, duplicate suppression, availability mismatch, pool/lock waits, and double-booking invariant alarms.
    - Expose safe pending/conflict responses and avoid personal data in metric labels; retain business audit independently from sampled traces.
    - **Trade-off:** aggressive hold expiry improves utilization but can harm users during provider slowness; policy changes need clear communication.
    - **Follow-up:** How do you identify systematic hold-hoarding without penalizing shared networks?
    - **Takeaway:** Correctness, fairness, and operational visibility must reinforce each other.

## Step 10 — Verification and final defense

### Q10. Which tests demonstrate that the entire reservation lifecycle is safe?

??? question "Reveal answer"
    - **Test the resource invariant through concurrent transitions and recovery.**
    - Race two owners, overlapping groups/dates, renewal versus expiry, late confirmation, duplicate callbacks, stale workers, process death, cache drift, and authority failover.
    - Assert at most one confirmed owner per assigned seat and bounded allocated room quantity per date; reconcile payment outcomes with booking outcomes.
    - Test cold-cache bursts, hot-event skew, expiry backlog recovery, backup restore, and honest client status under partial failure.
    - **Trade-off:** successful UI checkout tests do not establish concurrency safety; combine real-engine integration tests, controlled fault tests, and explicit consistency assumptions.
    - **Follow-up:** Can you defend the DB-versus-Redis choice and name its weakest failure assumption?
    - **Takeaway:** Finish the complete booking argument before returning to mixed practice.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Payment walkthrough](practice-payments.md)
- [Database concurrency practice](../database-sql/practice.md)
