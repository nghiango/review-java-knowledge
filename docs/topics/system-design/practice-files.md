# Case 7 — Large File Processing: Senior Interview Walkthrough

Finish [Distributed Rate Limiter](practice-rate-limiter.md), then complete this pipeline before
[Booking](practice-booking.md). Explain each prompt before revealing the reasoning and spoken
answer. Capacity figures and protocols are design assumptions, not measured lab capabilities.

## Scenario and progression

| Dimension | Working assumption |
|---|---|
| Product | Private large-file upload; OCR/transcoding; status, cancellation, authorized download |
| Input | Illustrative 10k uploads/day averaging 1 GB, with much larger outliers |
| Processing | Asynchronous, several output variants, CPU/GPU/memory-dependent classes |
| Storage | Object storage for bytes; durable metadata/workflow authority |
| Invariants | Pinned input; recoverable accepted jobs; no unauthorized or partial published result |

Proceed through scope → sizing → model → architecture → multipart → finalization → workflow →
leases → publication → cancellation → cleanup → fairness → sandboxing → migration/regions → evidence.
Treat upload, accepted job, attempted computation, and published output as separate lifecycles.

## Round 1 — Requirements and acknowledgment

### Q1. What does the service promise when it returns success for an upload or processing request?

??? question "Reveal answer"

    **Verdict:** Upload-session creation, completed bytes, durable job acceptance, and usable output are different acknowledgments.

    | Clarification | Design consequence |
    |---|---|
    | Maximum size/type and variants? | Admission, parser/resource limits, DAG shape |
    | Resumable across client failures? | Durable session and multipart identity |
    | Can input be replaced? | Immutable version pinning |
    | Processing deadline and cancellation? | Durable timers, state races, billing policy |
    | Private sharing/deletion? | Access checks and retention across artifacts |

    Define checksum/integrity expectations and whether outputs require quarantine/scanning. A storage
    notification that an object exists does not prove authorization, completeness, or processing
    acceptance. A published manifest must point only to complete permitted results for the correct input.

    **Interview-ready answer:** “I would define separate upload and job contracts, maximum resource
    cost, useful deadlines, and input/output ownership. Clients receive a durable status resource
    for long work; uploaded bytes alone are not a promise that processing is accepted.”

    **Follow-up:** Can a customer overwrite a file after the platform has authorized a job for it?

## Round 2 — Byte and compute sizing

### Q2. What do 10k daily 1 GB uploads imply beyond request QPS?

??? question "Reveal answer"

    **Verdict:** Bandwidth, retained bytes, resource-seconds, and outlier cost dominate control-plane QPS.

    | Assumption | Calculation |
    |---|---|
    | 10k × 1 GB/day | About 10 TB/day input |
    | Uniform daily arrival | About 116 MB/s mean input in decimal units |
    | 30-day input retention | About 300 TB before outputs/replication |
    | 10k jobs/day, 2 minutes mean exclusive worker use | About 13.9 workers occupied on average |
    | One job/minute in a separate example, 2-minute mean | About two occupied workers before headroom |

    Actual CPU/GPU parallelism, output multiplicity, compression, temp disk, object requests, and
    network egress change these numbers. A 1 GB archive can expand vastly more than a 1 GB video;
    bytes alone are not compute cost. Model peak arrivals and skew, not uniform averages only.

    **Interview-ready answer:** “I estimate data-plane bytes and resource demand by format/class,
    then reserve failure/retry headroom. More HTTP instances do not solve temp-disk exhaustion,
    parser memory blowups, or a storage request quota.”

    **Follow-up:** Which metric lets you autoscale a mixed queue of small documents and multi-hour videos?

## Round 3 — API and durable records

### Q3. What identities connect resumable input, a job, an attempt, and a downloadable result?

??? question "Reveal answer"

    **Verdict:** Pin immutable input and transform identity before admitting work.

    | Record | Durable fields |
    |---|---|
    | Upload session | Owner, object location/version, multipart ID, expected bounds/checksum, expiry |
    | Job | Scoped operation key/fingerprint, pinned input, transform/version, deadline, state |
    | Stage/attempt | Dependencies, resource class, attempt ID, lease generation/checkpoint |
    | Output manifest | Winning attempt, complete object versions/checksums, access policy |
    | Lifecycle work | Outbox, retry/cleanup intent, audit/billing references |

    APIs create a session, authorize parts, finalize, accept/query/cancel a job, and authorize
    download. Repeating finalization returns the same result under a stable operation identity.
    One mutable object key is insufficient: a later upload must not change what an existing job reads.

    **Interview-ready answer:** “I distinguish the uploaded object from the accepted job and each
    execution attempt. Immutable input and transform versions make retries reproducible; a guarded
    manifest identifies the published result rather than whichever object was written last.”

    **Follow-up:** Should identical input bytes always reuse another tenant's output?

## Round 4 — Data plane versus control plane

### Q4. Trace the full pipeline without proxying every byte through application request threads.

??? question "Reveal answer"

    **Verdict:** Direct scoped object transfer carries bytes; durable metadata controls authorized work.

    ```mermaid
    flowchart LR
        Client --> API[Authorized session and job API]
        API --> Metadata[(Session, job, outbox)]
        Client --> Input[(Scoped multipart input storage)]
        Input --> Finalize[Verify and pin completed version]
        Finalize --> Metadata
        Metadata --> Queue[Bounded resource-aware scheduling]
        Queue --> Worker[Sandboxed worker]
        Worker --> Staging[(Attempt-specific outputs)]
        Worker --> Publish[Guarded manifest publication]
        Publish --> Metadata
        Reader[Authorized reader] --> Download[Manifest-based access]
        Download --> Staging
    ```

    Short-lived scoped transfer credentials reduce application bandwidth but do not automatically
    enforce every product size/ownership condition; enforce supported restrictions and verify again
    at finalization. Persist accepted job plus required next intent atomically. Messages carry stable
    references, not multi-gigabyte payloads.

    **Interview-ready answer:** “I separate bulk transfer from orchestration. The API authorizes
    and records lifecycle state, storage receives bytes, and workers publish through a guarded
    metadata decision. Unreferenced staged objects are not customer-visible success.”

    **Follow-up:** What if object completion succeeds but the client never calls finalization?

## Round 5 — Multipart retries and integrity

### Q5. A client loses connectivity halfway through a huge upload. How does it resume safely?

??? question "Reveal answer"

    **Verdict:** Resume the same authorized session and verify the completed immutable object before job acceptance.

    Persist multipart/session identity and expose authorized part progress. Reissuing permission must
    remain scoped to the owner, object, parts/actions, and expiry—not grant arbitrary bucket access.
    Handle repeated part uploads and lost completion responses under the storage API's actual contract.

    | Check | Why |
    |---|---|
    | Session and owner | Prevent another tenant completing the upload |
    | Object version and size | Prevent replacement or unexpected resource use |
    | Supported checksum | Verify integrity; a multipart ETag is not universally a content hash |
    | Expiry and quota | Prevent endless abandoned storage consumption |

    A lost response is not proof completion failed. Inspect authorized durable/storage state before
    starting a different upload. Preserve enough progress to resume without trusting arbitrary
    client claims about existing parts.

    **Interview-ready answer:** “I would make resumability a durable session protocol, not a new
    upload on every retry. Completion is verified against the pinned object identity and expected
    bounds; cleanup eventually handles sessions that never finalize.”

    **Follow-up:** Can a client keep an upload alive indefinitely by repeatedly requesting new part credentials?

## Round 6 — Finalization and duplicate events

### Q6. Storage notifications duplicate or arrive before API finalization. Can they directly start jobs?

??? question "Reveal answer"

    **Verdict:** Notifications are observations, not sole authorization or acceptance authority.

    Resolve the event against an owned session and immutable object version. Finalization verifies
    policy/integrity and conditionally creates one accepted job with outbox. A unique business
    identity protects against different event envelopes describing the same completed input/operation.

    | History | Recovery |
    |---|---|
    | Object complete, DB not finalized | Reconcile eligible session; do not silently discard bytes |
    | DB accepts job, queue send fails | Outbox recovers required work |
    | Notification delivered twice | Inbox plus job uniqueness |
    | Mutable key overwritten after check | Reject mismatch or read only pinned immutable version |

    A transaction cannot ordinarily include an independent storage API commit. Reconciliation
    closes the completion/finalization gap, while immutable references prevent time-of-check/time-of-use
    input substitution. Incomplete or quarantined objects are not eligible inputs.

    **Interview-ready answer:** “I use storage events to discover work, then enforce the session
    contract at a durable acceptance boundary. Duplicate or reordered observations converge on
    one job, and the outbox protects the handoff after that commit.”

    **Follow-up:** Which component finds completed uploads when their notification is permanently missing?

## Round 7 — Workflow alternatives

### Q7. When is one queue sufficient, and when does the pipeline need a multi-stage workflow?

??? question "Reveal answer"

    **Verdict:** Add stages for real dependencies, isolation, or recovery savings—not architectural decoration.

    | Choice | Benefit | Cost |
    |---|---|---|
    | Synchronous short transform | Simple direct result | Request lifetime and resource bounds |
    | One durable job/worker | Simple long-work recovery | Whole-job retry and coarse resource scheduling |
    | Stage DAG | Reuse intermediates; partial retries | Dependency state, versioning, retention, orchestration |
    | Resource-class queues | CPU/GPU/memory isolation | Capacity fragmentation and scheduling complexity |

    For video, probe/validate may precede parallel variants and a final manifest stage. A durable
    parent records required outputs and completion; one variant finishing does not publish partial
    success unless the product explicitly permits it. Stages reference immutable predecessor outputs.

    **Interview-ready answer:** “I start with a bounded durable job, then split expensive independently
    recoverable stages where justified. The workflow owns dependency/version semantics and only
    publishes when its declared output contract is satisfied.”

    **Follow-up:** Can an old stage output be reused after the transform implementation changes?

## Round 8 — Worker crash and stale leases

### Q8. A worker pauses, its lease expires, and another finishes. Can the old worker publish later?

??? question "Reveal answer"

    **Verdict:** Lease expiry permits reassignment; guarded publication rejects stale completion.

    Give attempts distinct IDs/generations and attempt-specific output paths. Heartbeats extend
    bounded authority only under current ownership. Publication checks active generation, job state,
    input/transform version, and output completeness in the metadata authority.

    | Action | Safety boundary |
    |---|---|
    | Old worker still computes | Duplicate cost may occur |
    | Old worker writes staged objects | Isolated namespace prevents overwrite |
    | Old worker requests publication | Current-generation predicate rejects it |
    | Old cleanup request arrives | Must not remove referenced winning output |

    A fence stored only inside the old worker is ineffective. Terminating a timed-out future does
    not necessarily stop a parser process; enforce process/resource lifecycle separately.

    **Interview-ready answer:** “I cannot guarantee computation runs once, but I can fence the
    authoritative publication decision. Attempt isolation and generation checks prevent stale work
    from replacing a valid result, while budgets contain duplicate execution cost.”

    **Follow-up:** What if all workers write to the same predictable output key despite checking a DB lease?

## Round 9 — Output publication

### Q9. Outputs finish uploading, then the worker crashes before saving SUCCEEDED. How are partial results hidden?

??? question "Reveal answer"

    **Verdict:** Publish through one durable manifest decision after verifying all required immutable artifacts.

    Stage objects first; record exact version/checksum and required set. A guarded metadata commit
    installs the winning manifest and final job state plus completion intent. Downloads resolve that
    manifest, not an object-prefix listing that can expose partially uploaded variants.

    | Crash point | Result |
    |---|---|
    | Before all objects complete | No published result; resume/retry or clean later |
    | Objects complete, manifest not committed | Recover or abandon this isolated attempt |
    | Manifest committed, response lost | Status/download retrieves the winning result |
    | Completion event not sent | Outbox recovers notification |

    Do not pretend object storage and SQL share one transaction. Reference verification, immutable
    objects, guarded metadata, and orphan cleanup form the protocol. Avoid deletion of input or
    intermediates still needed by recovery.

    **Interview-ready answer:** “The manifest is the publication authority. Bytes may exist without
    being visible as a result. Retrying completion recovers the current durable decision rather
    than publishing whichever directory happens to contain files.”

    **Follow-up:** What prevents a lifecycle rule deleting an object that a committed manifest references?

## Round 10 — Cancellation and deadlines

### Q10. Cancellation races successful publication and an expensive child process. What can you guarantee?

??? question "Reveal answer"

    **Verdict:** Define cancellation acceptance, publication exclusion, and actual resource termination separately.

    A guarded transition arbitrates cancellation versus publication at the job authority. If
    cancellation wins, later completion must not expose a result under the cancelled contract.
    Persist cancellation intent and signal workers, revoke renewable ownership, and terminate
    child processes/containers under bounded escalation. Network disconnect alone does not cancel jobs.

    Already-issued download credentials may remain usable until expiry unless the access architecture
    supports stronger revocation. Billing policy distinguishes accepted cancellation from computation
    already consumed. Deadlines can expire queued work before it starts rather than waste resources.

    **Trade-off:** immediate resource termination can corrupt reusable checkpoints; graceful stopping
    needs a finite limit. Strong download revocation may require mediated access rather than long-lived URLs.

    **Interview-ready answer:** “I make cancellation a durable state decision and separately bound
    worker/process shutdown. I do not promise that a timeout or cancelled future unsends downloads,
    kills native tools, or erases already consumed compute.”

    **Follow-up:** Which side wins when publication committed just before the cancel request arrived?

## Round 11 — Cleanup and retention

### Q11. How are abandoned uploads and stale attempts reclaimed without deleting useful results?

??? question "Reveal answer"

    **Verdict:** Cleanup is a recoverable, version-aware lifecycle operation—not a blind age-based delete.

    | Artifact | Eligibility evidence |
    |---|---|
    | Incomplete multipart upload | Expired session; storage reconciliation and safe grace |
    | Completed unfinalized input | Session/job reconciliation and retention policy |
    | Failed/obsolete attempt outputs | No winning manifest/reference; stale owner excluded |
    | Published result | Product expiry/deletion authorization and active-reference policy |
    | Intermediate checkpoint | No remaining dependent/retry consumer |

    Mark candidates and revalidate references/generation before deletion. Prevent late workers
    recreating artifacts after cleanup, or arrange repeated safe sweeping. Record progress and
    tolerate partial storage deletion. Deletion may have compliance/versioning/backup implications
    beyond removing the current object name.

    **Interview-ready answer:** “I account for every artifact's owner and live references, then
    clean through idempotent guarded work. Object age alone does not prove it is unused, and
    restoring metadata must not revive references to permanently deleted bytes.”

    **Follow-up:** How do you avoid a race between cleanup's reference check and a new manifest commit?

## Round 12 — Fair scheduling and saturation

### Q12. Large jobs monopolize GPUs while short jobs miss deadlines. How do you scale fairly?

??? question "Reveal answer"

    **Verdict:** Schedule by resource class and estimated work, with bounded tenant admission.

    Use CPU/GPU/high-memory classes, per-tenant active work and byte quotas, weighted shares, and
    a policy for aging or small-job service. A queue length of 1,000 cannot distinguish minutes from
    weeks of compute. Track remaining resource-seconds and oldest eligible age by bounded class.

    Autoscaling still respects storage bandwidth, temporary disk, device startup, provider quotas,
    and maximum fleet cost. A queue should not accept unbounded work just because bytes fit in storage.
    Reject, defer, or revise deadlines when service rate cannot catch up.

    **Trade-off:** preemption can improve fairness if checkpoints are valid, but wastes compute or
    risks corrupt intermediate state. Separate capacity classes reduce starvation at utilization cost.

    **Interview-ready answer:** “I scale workers from measured work and resource demand, not raw
    messages. Admission and tenant budgets keep deadlines and cost accountable, while class isolation
    protects interactive jobs from expensive outliers.”

    **Follow-up:** How does the estimate adapt when a small compressed file expands into enormous work?

## Round 13 — Hostile content and access

### Q13. How do you process untrusted archives, documents, and media safely?

??? question "Reveal answer"

    **Verdict:** Sandbox parsers and enforce resource/egress limits; byte validation alone is insufficient.

    Restrict worker credentials to required objects/actions, isolate tenants appropriately, and
    limit CPU, memory, wall time, expansion ratio, file count, temp disk, and child processes. Disable
    unnecessary network access and protect against server-side fetches to internal services. Scan
    and quarantine according to category; downloads cannot bypass the required release decision.

    Signed URLs are scoped temporary credentials: authorize issuance, minimize lifetime/actions,
    and keep them out of logs. Content type/extension supplied by the client is not proof of format.
    Provider checksum or MIME checks do not establish that the parser input is safe.

    **Interview-ready answer:** “I treat every parser as a hostile-input boundary and use least
    privilege plus enforceable resource and network isolation. Private object ownership is verified
    on upload, job submission, publication, and download—not just when creating the session.”

    **Follow-up:** Can a document converter resolve an external image URL into an internal metadata endpoint?

## Round 14 — Evolution and regional recovery

### Q14. How do transform upgrades and regional failover preserve reproducibility and output ownership?

??? question "Reveal answer"

    **Verdict:** Pin versions and recover metadata plus bytes before granting new execution/publication authority.

    Store transform/model/config version and immutable input with the job. New workers must support
    eligible old jobs or route them to compatible executors. Reusing a checkpoint requires compatible
    format/version and verified integrity; rolling deployment must not mix incompatible stage outputs.

    Object replication may lag independently of database replication. A promoted job record can
    reference input or outputs not yet available in that region. Verify versions, keys/encryption,
    storage permissions, and publication state before resume. Fence old publication authority and
    keep provider/storage event identities stable through replay.

    **Trade-off:** waiting for byte replication increases recovery time; reprocessing may be safer
    than exposing missing/corrupt output but requires cost, identity, and access policy.

    **Interview-ready answer:** “I fail over the entire artifact/workflow graph, not merely the job
    table. Versions and manifests preserve reproducibility; one current publication owner and verified
    object availability prevent stale or missing results from being labeled successful.”

    **Follow-up:** Can a job be reported SUCCEEDED when its replicated manifest arrives before its output bytes?

## Round 15 — Evidence and final defense

### Q15. Which tests qualify the full pipeline rather than only a successful transform function?

??? question "Reveal answer"

    **Verdict:** Verify authorized immutable input, recoverable work, guarded publication, and safe cleanup.

    | Failure / race | Evidence |
    |---|---|
    | Multipart interruption, checksum mismatch | Correct resume/rejection and bounded orphan storage |
    | Duplicate/missing completion events | One authorized accepted job or accountable session |
    | Worker death/stale lease | Safe reassignment; old attempt cannot publish |
    | Upload/manifest commit gap | No partial visible result; recovered durable status |
    | Cancel/cleanup/publication race | Legal winner and no deletion of referenced data |
    | Hostile formats/region recovery | Enforced resource/access bounds and verified versions |

    Measure queue age, resource-seconds, retry cost, temp-disk peaks, orphan bytes, checksum failures,
    publication latency, and per-class deadline misses. Controlled large/hostile fixtures qualify
    limits; tiny unit fixtures cannot establish resource safety or a storage provider contract.

    **Interview-ready answer:** “I separate bytes from control, finalize owned immutable input,
    persist durable jobs, and execute sandboxed versioned attempts. A guarded manifest publishes
    complete results once, while retries, cancellation, and cleanup remain explicit lifecycles.
    Capacity follows bytes and compute cost; recovery includes metadata, artifacts, and authority.”

    **Follow-up:** Which fault proves an obsolete worker cannot overwrite or delete the winning result?

## Review gate

Defend resumability, immutable input, finalization gaps, attempt fencing, manifest publication,
process cancellation, cleanup races, sandboxing, and regional byte consistency before booking.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Backpressure and recovery mechanisms](../resilience/practice.md)
- [Event identity and durable intent](../distributed-data-patterns/practice.md)
- [Next: Booking](practice-booking.md)
