# Case 7 — Large File Processing, Step by Step

Complete [Distributed Rate Limiter](practice-rate-limiter.md), then finish this case before
[Booking](practice-booking.md). Work from acceptance through publication and cleanup.

## Step 1 — Requirements

### Q1. What does a file-processing service promise at upload and completion?

??? question "Reveal answer"
    - **Separate upload completion, durable job acceptance, processing, and published output.**
    - Clarify file size/types, resumability, transformation variants, latency classes, cancellation, retention, and who may read originals/results.
    - Invariants: one logical job per accepted operation, no unauthorized object access, and no published partial output.
    - Define whether the API returns an upload session or an accepted processing job; these are different acknowledgments.
    - **Trade-off:** asynchronous completion fits long work but needs status, expiry, and recovery ownership.
    - **Follow-up:** Can clients replace input after a job has started?
    - **Takeaway:** Pin the input and output lifecycle before selecting workers.

## Step 2 — Capacity

### Q2. Which estimates matter for multi-gigabyte inputs?

??? question "Reveal answer"
    - **Calculate bytes, compute cost, and storage duration—not only request QPS.**
    - An illustrative 10k uploads/day at 1 GB means about 10 TB/day before outputs and replication; peak bandwidth can be far above the daily average.
    - If average processing takes two minutes at one job/minute, about two workers are occupied on average before headroom, retries, and multi-variant fan-out.
    - Budget temporary disk, object storage requests, CPU/GPU/memory per format, queue age, and abandoned multipart storage.
    - **Trade-off:** worker count does not fix an object-store/network quota or one oversized decompression workload.
    - **Follow-up:** Which input shape has the highest compute-to-byte ratio?
    - **Takeaway:** Processing capacity is workload-dependent resource demand.

## Step 3 — API and model

### Q3. What records and APIs make uploads resumable and jobs auditable?

??? question "Reveal answer"
    - **Use explicit upload sessions and versioned job identity.**
    - APIs create an authorized session, request scoped upload credentials/parts, finalize verified input, create/query/cancel a job, and authorize result download.
    - Store owner, immutable object version/key, expected size/checksum, transform/version, job state/deadline, attempt/lease generation, and output manifest.
    - A unique scoped operation key prevents duplicate jobs; an object-existence notification is not sufficient proof of authorized complete input.
    - **Trade-off:** checksum verification costs work but protects integrity; storing only a mutable object key permits time-of-check/time-of-use changes.
    - **Follow-up:** How does a retry resume parts without creating another billable job?
    - **Takeaway:** A storage object and a business job need separate identities.

## Step 4 — Architecture

### Q4. How should data and control planes be separated?

??? question "Reveal answer"
    - **Upload bytes directly to authorized object storage; keep application requests focused on metadata and control.**
    - Short-lived scoped upload authorization avoids proxying gigabytes through request threads. Finalization verifies ownership, version, size, and checksum before job acceptance.
    - Persist job plus outbox; workers consume recoverably, process bounded streams/temp files, and upload outputs to attempt-specific staging paths.
    - Publish an output manifest through a guarded durable transition only after all required outputs are complete.
    - **Trade-off:** direct uploads reduce application bandwidth but require lifecycle cleanup and careful authorization constraints.
    - **Follow-up:** What if upload completion occurs but the client never finalizes the session?
    - **Takeaway:** Separate bulk transfer from durable orchestration.

## Step 5 — Alternatives

### Q5. Synchronous endpoint, one queue, or a durable multi-stage workflow?

??? question "Reveal answer"
    - **Choose based on duration, resource isolation, dependencies, and retry granularity.**
    - A short bounded transform may fit a synchronous endpoint; long transcodes/OCR need durable status and workers independent of HTTP lifetime.
    - One queue is simple; distinct CPU/GPU/large-memory classes avoid one expensive format starving small jobs. A DAG is justified for real stage dependencies and reusable intermediate outputs.
    - Queue messages carry object references and identity, not huge payload bytes.
    - **Trade-off:** multi-stage workflows improve partial recovery but add state, intermediate retention, and orchestration cost.
    - **Follow-up:** Which stages can be safely replayed independently?
    - **Takeaway:** Match workflow complexity to actual failure and scheduling needs.

## Step 6 — Concurrency

### Q6. How do duplicate storage events and expired worker leases avoid duplicate publication?

??? question "Reveal answer"
    - **Deduplicate jobs and fence final publication using attempt ownership.**
    - Storage notifications can duplicate or reorder; resolve against the pinned upload session/version and create one job per accepted operation.
    - Workers write attempt-specific outputs; completion checks current generation/version before publishing the manifest. A late old worker cannot overwrite the winning manifest.
    - Cancellation and completion compete through legal state transitions; cleanup removes only unreferenced attempt artifacts after safe retention.
    - **Trade-off:** duplicate computation may remain possible after a crash; correctness can still ensure one published result.
    - **Follow-up:** Can a cancellation acknowledgment race with an already-authorized download?
    - **Takeaway:** Once-only publication is different from once-only computation.

## Step 7 — Failure and recovery

### Q7. What happens when a worker crashes halfway through a large file?

??? question "Reveal answer"
    - **Retry or resume from durable stage checkpoints without trusting partial outputs.**
    - Renew bounded leases, record progress only at recoverable boundaries, and reassign expired attempts with a new generation.
    - Classify invalid input separately from transient storage failure; cap retries and total compute budget. Persist terminal failure with a safe explanation.
    - Apply timeouts to storage and tools; child processes need resource limits and termination, not merely a timed-out future.
    - **Trade-off:** checkpoints save compute but require compatible transform versions and validated partial state.
    - **Follow-up:** How are orphaned multipart uploads and staged outputs reclaimed safely?
    - **Takeaway:** Recovery includes both useful work and abandoned resources.

## Step 8 — Scaling

### Q8. How do you schedule jobs fairly under a backlog?

??? question "Reveal answer"
    - **Scale by resource class, oldest eligible age, and estimated work, with tenant fairness.**
    - Use bounded admission, per-tenant concurrency/storage quotas, and weighted scheduling; a million tiny jobs and a thousand multi-hour jobs need different capacity estimates.
    - Autoscale against net drain rate and execution resources, preserving storage/API quota and shutdown behavior.
    - Expire work that no longer meets the product contract; large jobs should not indefinitely block interactive small jobs.
    - **Trade-off:** strict priority can starve bulk tenants; reserved shares or aging improve fairness at efficiency cost.
    - **Follow-up:** What happens when the autoscaler is at max capacity and deadlines are still missed?
    - **Takeaway:** Backlog control includes admission, not just more workers.

## Step 9 — Security and telemetry

### Q9. How do untrusted files change the threat model and diagnostics?

??? question "Reveal answer"
    - **Sandbox parsers and minimize object permissions; uploaded content is hostile input.**
    - Enforce size/type constraints, scan/quarantine as required, guard archive expansion and parser CPU/memory, and restrict worker network/credential privileges.
    - Scope signed URLs to owner, operation, object, and lifetime; never log credentials or publicize private object paths. Validate callback/event authenticity.
    - Measure upload-finalization failures, job age, resource cost, retry causes, orphan bytes, and publication success with bounded labels.
    - **Trade-off:** scanning adds latency/cost but should not be bypassed by direct-download paths.
    - **Follow-up:** Can a malicious document reach internal network services through a parser?
    - **Takeaway:** Performance limits are also security controls.

## Step 10 — Verification

### Q10. How do you verify correctness and capacity for the whole pipeline?

??? question "Reveal answer"
    - **Test durable lifecycle and published artifacts under failures, not only successful worker execution.**
    - Cover interrupted multipart uploads, checksum mismatch, duplicate notifications, worker death, stale completion, cancellation, corrupt files, and cleanup races.
    - Verify every published manifest references complete authorized objects from the correct transform/input version; reconcile accepted jobs with terminal/pending state.
    - Load-test realistic format/size distributions and tenant skew, including temporary disk and storage-request ceilings.
    - **Trade-off:** tiny fixture tests validate logic but not resource safety; hostile large-input qualification belongs in controlled environments.
    - **Follow-up:** Which test proves an obsolete worker cannot publish after reassignment?
    - **Takeaway:** A complete pipeline owns input, work, output, and cleanup.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Next: Booking](practice-booking.md)
