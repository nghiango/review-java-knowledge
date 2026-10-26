# System Design — Practice Q&A

Study **one complete system at a time**, not isolated questions from different systems.
For each case, answer every step before moving to the next case. Each answer is collapsed;
state your assumptions and reasoning aloud before revealing it.

The canonical [Interview Questions](questions.md) remain a mixed reference bank. The case-first
sequence below is the primary guided practice path. These are design proposals and interview
assumptions, not claims of deployed lab capacity.

## Case-by-case sequence

| Order | Complete case | Questions | Distinctive senior challenge |
|---|---|---|---|
| 1 | [URL Shortener](practice-url-shortener.md) | 15 | Alias races, revocation/stale fills, HTTP caching, analytics ambiguity, regional ownership |
| 2 | [Payments](practice-payments.md) | 15 | Provider commit gap, ledger postings, concurrent refunds, cancellation, settlement reconciliation |
| 3 | [Order Management](practice-orders.md) | 15 | Durable acceptance, saga/cancellation races, poison events, read freshness, shadow rebuild, DR |
| 4 | [Notification Engine](practice-notifications.md) | 15 | Audience checkpoints, consent/send races, provider ambiguity, fairness, fallback, content evolution |
| 5 | [Flash Sale Inventory](practice-flash-sale.md) | 21 | Commit gaps, payment races, durable grants, CDC recovery, migrations, regional fencing |
| 6 | [Distributed Rate Limiter](practice-rate-limiter.md) | 15 | Algorithm bounds, hierarchical accounting, grants/restarts, time, failover, policy rollout |
| 7 | [Large File Processing](practice-files.md) | 15 | Multipart/finalization gaps, immutable input, worker fencing, manifest publication, cleanup races |
| 8 | [Hotel / Ticket Booking](practice-booking.md) | 15 | Group/date atomicity, renewal generations, payment/expiry, durable timers, modification, regional authority |
| **Total** | **8 complete interview walkthroughs** | **126** | **Finish one case before moving to the next** |

## The complete loop inside every case

1. **Requirements:** clarify scope, user contract, invariants, and consistency.
2. **Capacity:** calculate average/peak demand, skew, bytes, and recovery headroom.
3. **API and data:** define identities, states, constraints, indexes, and authorization boundaries.
4. **Baseline flow:** trace reads/writes and acknowledgment points end to end.
5. **Alternatives:** compare realistic designs and explain why one fits these assumptions.
6. **Concurrency:** reason about duplicates, races, ownership, ordering, and expiry.
7. **Failure/recovery:** cover crash windows, ambiguous outcomes, retries, and reconciliation.
8. **Scaling:** identify the bottleneck before adding caches, shards, workers, or regions.
9. **Security/operations:** define controls, business SLIs, bounded telemetry, and operator actions.
10. **Verification:** test invariants, fault recovery, load, and the assumptions behind the guarantee.

Some cases place rebuild or scaling alternatives in a different step to preserve the natural
business flow. Finish the whole lifecycle before the next system begins.

**All eight cases use the expanded interview format:** an explicit scenario and assumptions,
progressive interviewer prompts, guided reasoning, concrete failure histories, calculations and
comparison/recovery tables, diagrams, interview-ready spoken answers, follow-ups, and a review gate.
The ten dimensions above are a study loop, not a limit of ten questions. Seven cases have 15 rounds;
flash sale has 21. The workload and deep dives stay specific to each system.

[Flash sale's progressive conversation](practice-flash-sale.md) also carries one design through
candidate proposals, interviewer counterexamples, revisions, and a build/verification checkpoint
per round. Its staged build plan explains what to implement first and which later infrastructure
is justified only by stronger requirements or measured limits.

## Source coverage

The commerce scenarios from interview-prep §8 and §13 now live within complete cases:

- Order acceptance, read-model rebuild, scaling past one primary, and multi-region ownership:
  [Order Management](practice-orders.md).
- Payment added to an order saga: [Payments](practice-payments.md), with order integration in
  [Order Management](practice-orders.md).
- Hot-product traffic and stock contention: [Flash Sale Inventory](practice-flash-sale.md).

The other cases extend the curriculum's eight canonical blueprints. Detailed algorithm and
framework explanations remain canonical in their respective topic pages; these walkthroughs
focus on composing them into one defensible system.

## Completion gate — before moving to the next case

- Can you draw the complete request and event flow from memory?
- Can you state the source of truth and the invariant at each write boundary?
- Can you compare at least two alternatives without claiming one is universally best?
- Can you explain what a timeout means and how uncertain outcomes are resolved?
- Can you identify the first bottleneck and calculate a plausible capacity bound?
- Can you name the fault tests and measurements needed to support your claims?
- Can you summarize the design, accepted costs, and remaining risk in two minutes?

## Related

- [Start: URL Shortener](practice-url-shortener.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Exercises](exercises.md)
- [Solutions](solutions.md)
- [Architecture practice](../architecture/practice.md)
- [Distributed data practice](../distributed-data-patterns/practice.md)
