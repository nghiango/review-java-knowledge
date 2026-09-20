# ADR-004: Fencing-Token-Guarded Distributed Locking for Shared Storage Mutation

## Status
Accepted

## Context
Multiple background worker pods concurrently process and transform analytics partition files stored in cloud object storage (S3). Because client nodes are subject to arbitrary Stop-The-World (STW) garbage collection pauses, hypervisor stalls, and network latency, a lock lease can expire in the coordination service while the client remains unaware.

To guarantee mutual exclusion and prevent split-brain dual writes, locking must be paired with **fencing tokens** enforced at the storage persistence boundary.

## Decision
We will implement distributed locking using an atomic coordination lease backed by Redis / etcd with monotonically increasing fencing tokens:

1. **Lock Acquisition with Monotonic Fencing Token:**
   When a worker acquires a lock on partition $P$, the coordination service atomically:
   - Sets the lock key with lease TTL and client ID.
   - Atomically increments a monotonic 64-bit integer counter `fencing:token:partition:P`.
   - Returns the acquired lease and the generated `fencing_token` (e.g. `1042`).

2. **Active Lease Heartbeat:**
   While the worker thread executes the critical section, a background watchdog thread periodically renews the lease TTL every $1/3$ of the lease duration (e.g. every 3 seconds for a 10-second TTL).

3. **Storage-Layer Fencing Verification:**
   Before committing partition files to storage:
   - The worker records its `fencing_token` into the partition metadata header and state manifest in PostgreSQL / S3 metadata.
   - The storage layer verifies:
     ```sql
     UPDATE partition_manifest
     SET last_fencing_token = :current_token,
         status = 'COMMITTED',
         file_location = :s3_uri
     WHERE partition_id = :partition_id
       AND last_fencing_token < :current_token;
     ```
   - If `last_fencing_token >= :current_token`, the write is rejected as stale. Any paused worker that wakes up late cannot corrupt partition state.

4. **Graceful Release:**
   Upon successful write, the worker releases the lease key.

## Consequences

- Absolute safety against client GC pauses and delayed network packets.
- Eliminates dependence on synchronized physical clocks or uncalibrated TTL timeouts.
- Requires target storage or manifest database to support atomic conditional compare-and-swap (CAS) on the fencing token.
