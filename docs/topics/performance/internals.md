# Performance Internals

## Queues and tail latency

When arrivals approach service capacity, work waits before it runs. An unbounded queue preserves
acceptance while destroying deadlines. A bounded queue makes overload visible through rejection,
which lets upstream systems shed work or retry within a budget.

## HikariCP

Connections are concurrency permits for the database. A larger pool cannot make a saturated DB
faster; it can increase lock contention and memory. Inspect active, idle, pending, acquisition time,
transaction duration, and DB CPU together. Low DB CPU plus a full pool often means connections are
held during remote calls or blocked on locks.

## Executor sizing

CPU-bound work starts near the processor count. Work that blocks can use more workers, estimated as
`processors × (1 + wait/service)`, then validated. Queue capacity controls admitted latency and
memory. Virtual threads reduce thread-management cost for blocking code but do not increase DB,
socket, or CPU capacity.

## Allocation and GC

Allocation rate determines how often young collections occur. Live-set size and promotion determine
old-generation pressure. JFR allocation samples identify allocating call paths; GC logs show pause,
frequency, and cause. Escape analysis may remove some allocations after warmup, so source inspection
alone is insufficient.

## Locks and striped counters

A single monitor creates a serialization point even when keys are independent. `LongAdder` spreads
updates across cells and combines them on read. It improves contended counters but its sum is not a
linearizable snapshot during concurrent updates.

## JMH and profiling

JMH supplies warmup, forks, state scopes, and result consumption to reduce common benchmark errors.
Use it for isolated mechanisms. Use JFR or async-profiler with a controlled application load to
connect CPU, allocation, locks, and wall-clock behavior to the real request path.

## Related

- [Concepts](concepts.md)
- [Tests](tests.md)
- [Production](production.md)
