# Solution — Distributed Lock Without Fencing Tokens

## Annotated code

```markdown
# ADR-004: Distributed Locking for Shared Storage Mutation

## Status
Proposed

## Context
Multiple background worker pods concurrently process and transform analytics partition files stored in cloud object storage (S3). To prevent concurrent writes from overwriting or corrupting partitioned files, we require mutual exclusion across worker nodes.

The target storage layer (Amazon S3) provides atomic put-object semantics but lacks conditional version locking across multi-file partition updates.

## Decision
We will implement a distributed mutual exclusion lock backed by our shared Redis cluster using the standard atomic `SET ... NX PX` pattern:

1. **Lock Acquisition:**
   A worker requests exclusive access before touching partition $P$:
   ```redis
   SET lock:partition:P <random_uuid> NX PX 10000
   ```
   <!-- Concurrency issue: Distributed lock lease expiration during client pause (GC/network delay).
   If Worker 1 acquires the lock with a 10s TTL and immediately enters a Stop-The-World (STW) JVM GC pause,
   page fault, or network delay lasting 11 seconds, Redis expires the lock. Worker 2 requests the lock,
   receives OK, and begins modifying partition P. Worker 1 wakes up from its GC pause believing it still holds
   a valid lock, and proceeds to write to S3. Both Worker 1 and Worker 2 write concurrently, causing data corruption. -->
   If Redis returns `OK`, the worker holds the lock for up to 10,000 milliseconds (10 seconds). If Redis returns `nil`, the worker backs off and retries.

2. **Critical Section Execution:**
   The worker downloads source data, transforms rows in memory, and writes output files to S3 bucket `s3://analytics-warehouse/partition=P/`.

3. **Lock Release:**
   Upon completing S3 writes, the worker executes a Lua script to release the lock only if the value still matches its `<random_uuid>`:
   ```lua
   if redis.call("get", KEYS[1]) == ARGV[1] then
       return redis.call("del", KEYS[1])
   else
       return 0
   end
   ```
   <!-- Architecture issue: Missing fencing tokens at the shared resource boundary.
   A distributed lock cannot guarantee safety on its own if the lock server and the storage layer are decoupled.
   As proven in Martin Kleppmann's analysis of Redlock, mutual exclusion requires the lock service to return a
   monotonically increasing fencing token (e.g. 101, 102, 103) upon lock acquisition. The storage service must
   validate that incoming write requests carry a higher fencing token than the latest observed write, rejecting
   writes from stale lock holders (like Worker 1) whose tokens are outdated. -->

## Consequences

- Lightweight and fast: lock operations execute in sub-millisecond Redis memory operations.
- Fault tolerant: if a worker node dies unexpectedly, the 10-second TTL (`PX 10000`) guarantees the lock automatically expires without operator intervention.
- Mutual exclusion is assumed to be guaranteed by Redis single-threaded command evaluation.
```

## Issue list

### Concurrency issue: Client pause outliving lock TTL causes concurrent dual writes

- **Location:** `ADR-004-distributed-locking.md:19`
- **Description:** Relying on Redis TTL without accounting for client-side delays (JVM GC pause, hypervisor scheduling latency, swap thrashing, network buffer bloat).
- **Impact:** When a worker suffers a pause exceeding 10 seconds, its lock is silently expired by Redis and granted to another worker. When the paused worker resumes, it executes writes concurrently with the new lock holder, corrupting partitioned S3 files.
- **Remediation:** Implement a lock renewal heartbeat daemon (e.g. Redisson lease renewer) to extend TTL while active, and never rely solely on client-side time checks.

### Architecture issue: Missing monotonically increasing fencing tokens at the storage boundary

- **Location:** `ADR-004-distributed-locking.md:31`
- **Description:** The lock service does not issue monotonically increasing sequence numbers (fencing tokens), and the storage layer does not reject requests with stale tokens.
- **Impact:** Even with heartbeating, network delays can delay write packets so they reach S3 *after* another worker has already acquired the lock and completed newer writes. Without server-side fencing, out-of-order writes overwrite fresh data.
- **Remediation:** Use an atomic consensus store (etcd / ZooKeeper / Redis `INCR`) to return an incrementing fencing token upon lock grant. Require storage write operations to verify and record the fencing token (e.g. conditional write `WHERE fencing_token > :last_token` or S3 metadata conditional put), rejecting writes from previous lock owners.

## Correct implementation

See [`correct/ADR-004-distributed-locking.md`](correct/ADR-004-distributed-locking.md).

Detailed discussion in [Solutions](../../../docs/topics/distributed-systems/solutions.md#fencing-tokens-for-distributed-locking).
