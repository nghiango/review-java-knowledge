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
| 1 | [URL Shortener](practice-url-shortener.md) | 10 | Identifier space, redirect semantics, hot reads, revocation |
| 2 | [Payments](practice-payments.md) | 10 | Unknown external outcomes, ledger consistency, reconciliation |
| 3 | [Order Management](practice-orders.md) | 10 | Durable acceptance, saga transitions, projection rebuild, regional ownership |
| 4 | [Notification Engine](practice-notifications.md) | 10 | Fan-out, preferences, provider quotas, delivery ambiguity |
| 5 | [Flash Sale Inventory — expanded pilot](practice-flash-sale.md) | 21 | Full interview progression: commit gaps, payment races, durable grants, CDC recovery, migrations, regional fencing |
| 6 | [Distributed Rate Limiter](practice-rate-limiter.md) | 10 | Exact versus approximate quotas, atomicity, regional budgets |
| 7 | [Large File Processing](practice-files.md) | 10 | Resumable upload, resource isolation, stale workers, output publication |
| 8 | [Hotel / Ticket Booking](practice-booking.md) | 10 | Multi-resource holds, authoritative expiry, no double booking |

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

**Full-depth format pilot:** [Flash Sale Inventory](practice-flash-sale.md) expands this loop into
21 progressive interview rounds, with guided reasoning, concrete failure histories, comparisons,
recovery tables, and spoken answers. Review that format first; the other seven cases remain
compact ten-question walkthroughs until the deeper format is approved.

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
