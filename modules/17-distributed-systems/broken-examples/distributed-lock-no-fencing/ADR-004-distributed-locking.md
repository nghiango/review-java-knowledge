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

## Consequences

- Lightweight and fast: lock operations execute in sub-millisecond Redis memory operations.
- Fault tolerant: if a worker node dies unexpectedly, the 10-second TTL (`PX 10000`) guarantees the lock automatically expires without operator intervention.
- Mutual exclusion is assumed to be guaranteed by Redis single-threaded command evaluation.
