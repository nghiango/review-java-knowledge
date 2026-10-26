# Case 1 — URL Shortener: Senior Interview Walkthrough

Finish this entire case before [Payments](practice-payments.md). Explain the visible prompt before
opening each answer; then defend its follow-up. This is an architectural exercise, not a measured
lab deployment. Mechanisms remain canonical in the linked topic pages.

## Scenario and progression

| Dimension | Working assumption |
|---|---|
| Demand | 100 million creations and 10 billion redirects per 30-day month |
| Features | Public links, optional custom aliases, expiry, owner edits/deletion, asynchronous analytics |
| Priority | Redirect availability and latency; creation may have a different SLO |
| Consistency | Unique code ownership; explicit propagation and revocation windows |
| Latency | Clarify whether sub-10 ms means service time, edge time, or global client latency |

Work through requirements → sizing → API/model → baseline → IDs → concurrent creation → HTTP
semantics → revocation → hot keys → outage → analytics → abuse → regions → evolution → evidence.
Numbers below are assumptions to validate, not guaranteed technology limits.

## Round 1 — Requirements

### Q1. What would you clarify before drawing a cache and database?

??? question "Reveal answer"

    **Verdict:** The destination lifecycle and privacy contract determine whether redirects may be stale.

    | Question | Design consequence |
    |---|---|
    | Can destinations change? | Versioning and cache invalidation |
    | How fast must takedown apply? | Limits browser/CDN caching |
    | Are links private? | Authorization independent of code entropy |
    | Are analytics best-effort or billable? | Different ingestion durability |
    | Can aliases be reused? | Old caches and bookmarks can target a new owner |

    Distinguish redirect availability from management availability. A public immutable destination
    can be cached during a database outage; a revoked private destination may require refusal.
    Ask about owner quotas, retention, custom domains, geography, and abuse handling. Define missing,
    expired, and forbidden responses without leaking private-link existence.

    **Interview-ready answer:** “I would first clarify mutability, privacy, expiry, revocation,
    and analytics accuracy. Then I would set separate read and write SLOs. A global client cannot
    be promised sub-10 ms merely because the origin handler is fast.”

    **Follow-up:** Is the code a public locator or a bearer credential?

## Round 2 — Capacity and working set

### Q2. How do monthly totals become QPS, storage, and cache estimates?

??? question "Reveal answer"

    **Verdict:** Calculate requests, distinct mappings, and event bytes separately.

    | Quantity | Calculation | Caveat |
    |---|---|---|
    | Mean writes | 100M / 2,592,000 ≈ 38.6/s | Burst shape remains unknown |
    | Mean reads | 10B / 2,592,000 ≈ 3,858/s | 100:1 read/write ratio |
    | Illustrative 10× peak | 386 writes/s; 38,580 reads/s | Load-test assumption |
    | Mapping growth at 1 KB | About 100 GB/month | Excludes indexes, replicas, backup |
    | Click events at 200 bytes | About 2 TB/month | Excludes envelopes and replication |

    A cache estimate requires the number of distinct hot mappings and real entry overhead. Twenty
    percent of requests is not twenty percent of keys. One viral code can dominate clicks while
    occupying one cache entry. Include network bytes, expired-link cleanup, and cold-start demand.

    **Interview-ready answer:** “Average creation volume permits a simple baseline, but redirect
    skew and analytics dominate different resources. I would estimate the distinct working set and
    qualify peak, cold-cache, and hot-key behavior independently.”

    **Follow-up:** How would one code receiving half the reads change your cache sizing?

## Round 3 — API and model

### Q3. Which minimal APIs, fields, and indexes support the actual contract?

??? question "Reveal answer"

    **Verdict:** Make code ownership, operation identity, and destination version durable.

    Creation accepts a destination, optional alias, expiry, and scoped idempotency key. Management
    changes require owner authorization and an expected version. Lookup resolves an active mapping
    before returning Location under an explicit HTTP cache policy.

    | Record / index | Purpose |
    |---|---|
    | Code primary/unique key | Fast lookup and concurrent alias arbitration |
    | Owner, destination, state, expiry, version | Authorization and lifecycle |
    | Owner/creation-time index | Paginated management queries |
    | Principal/key operation record | Recover creation after a lost response |
    | Change intent / tombstone | Recover propagation and prevent stale resurrection |

    Do not globally deduplicate identical destinations without discussing ownership and expiry:
    two customers may need different lifetimes and analytics. Validate scheme, size, and normalized
    alias rules before persistence; normalization changes are themselves compatibility changes.

    **Interview-ready answer:** “I would index the redirect key and actual management queries,
    retain versioned lifecycle state, and make creation retries recoverable. Current destination
    text alone is insufficient to authorize updates or reason about revocation.”

    **Follow-up:** What happens when two visually similar Unicode aliases normalize identically?

## Round 4 — Baseline architecture

### Q4. Trace creation and redirect end to end before adding sharding.

??? question "Reveal answer"

    **Verdict:** Start with a durable indexed authority and stateless services; cache repeated reads.

    ```mermaid
    flowchart LR
        Owner[Authenticated owner] --> Write[Management API]
        Write --> DB[(Mapping and operation authority)]
        Write --> Changes[Durable invalidation intent]
        Reader[Reader] --> Edge[Edge cache]
        Edge --> Resolve[Redirect service]
        Resolve --> Cache[Bounded local and shared cache]
        Resolve --> DB
        Resolve -.-> Clicks[Bounded analytics ingestion]
        Changes --> Cache
    ```

    Creation acknowledges only after its mapping and recoverable result commit. A cache miss reads
    the authority, checks expiry/state, then fills a bounded entry. Publication of changes must be
    recoverable; a successful edit followed by a crashed best-effort eviction leaves stale readers.

    **Trade-off:** a relational primary is credible at the stated average writes. A distributed
    key-value store may become appropriate for geographic placement or volume, not merely because
    the service is called “large-scale.” Analytics should not silently block redirects.

    **Interview-ready answer:** “I establish correct creation, lookup, and update flows first.
    Caches accelerate them without becoming ownership authority, and optional analytics has an
    explicit loss or durability policy.”

    **Follow-up:** Which state lets a retry determine whether a timed-out creation committed?

## Round 5 — Identifier comparison

### Q5. Random codes, hashes, sequences, or allocated ID ranges?

??? question "Reveal answer"

    **Verdict:** Uniqueness, unpredictability, and encoding length are separate properties.

    Seven Base62 characters give 62^7 ≈ 3.52 trillion possibilities. Base62 does not encrypt a
    sequential number. Uniform random codes still need unique-constraint collision retries; the
    birthday effect matters well before the space is full.

    | Strategy | Strength | Cost |
    |---|---|---|
    | Random code | No central sequence; less enumerable | Collision retries and entropy budget |
    | Truncated destination hash | Deterministic candidate | Collision handling and canonicalization/privacy semantics |
    | Encoded sequence | Simple collision-free issuance | Enumerable; allocator authority |
    | Disjoint durable ranges | Local issuance without per-ID coordination | Range ownership, crash gaps, failover |

    Not every 64-bit number fits in seven Base62 characters. Constrain the ID domain or allow longer
    codes. Avoid claiming a shuffled sequence is a private-link authorization scheme.

    **Interview-ready answer:** “I would choose code length from namespace lifetime and guessing
    risk, then enforce uniqueness in storage. Range allocation reduces coordination but still needs
    durable disjoint ownership. Encoding alone gives neither secrecy nor unlimited capacity.”

    **Follow-up:** Is abandoning unused IDs after a range-holder crash acceptable?

## Round 6 — Concurrent creation and retries

### Q6. Two customers claim the same alias while one creation response is lost. What happens?

??? question "Reveal answer"

    **Verdict:** Let a durable uniqueness boundary choose ownership, and recover retries through operation identity.

    | History | Correct response |
    |---|---|
    | A and B both observe alias absent | Observation grants no right; unique insert arbitrates |
    | A commits, B collides | B receives documented alias conflict |
    | A commits, its response is lost | A retries the same key and retrieves its result |
    | Same key, different destination | Reject fingerprint mismatch |
    | New code candidate randomly collides | Bounded generation retry; not an owner conflict |

    Store the operation result with the mapping transaction where possible. If a unique insert
    aborts a PostgreSQL transaction, do not continue querying inside the failed transaction; roll
    back and resolve the existing operation in a valid scope. Scope keys to authenticated ownership.

    **Interview-ready answer:** “I would not use a distributed lock to replace the unique alias
    constraint. The database arbitrates concurrent ownership, and a durable scoped operation lets
    the winning customer recover a lost response without creating another link.”

    **Follow-up:** What if an alias is deleted and immediately claimed by someone else?

## Round 7 — HTTP semantics

### Q7. Does using 302 instead of 301 guarantee accurate click analytics?

??? question "Reveal answer"

    **Verdict:** No; status code, Cache-Control, and actual observation points jointly determine behavior.

    Permanent redirects may be reused by clients without contacting you. Temporary redirects can
    also be cached when permitted by explicit policy. A browser or CDN hit therefore may bypass
    origin-level analytics regardless of the simplified “301 versus 302” interview slogan.

    | Goal | Consequence |
    |---|---|
    | Immutable fastest redirect | Cache aggressively; origin sees fewer clicks |
    | Editable destination | Bound freshness and provide purge/version propagation |
    | Every origin-observed request counted | Requires origin contact; not every human click |
    | Edge-served analytics | Edge ingestion adds its own delivery and privacy contract |

    Cap cache lifetime at link expiry. Consider method preservation and the supported GET/HEAD
    contract explicitly; do not turn a shortener into arbitrary request forwarding.

    **Interview-ready answer:** “I would select redirect and cache headers together. I would
    measure where events are observed and distinguish requests from unique people. Stronger
    analytics or revocation may intentionally sacrifice browser caching.”

    **Follow-up:** Can a cache purge remove a redirect already stored by an uncontrolled browser?

## Round 8 — Revocation race

### Q8. A reader fills an old destination after an owner deletes the link. Why is eviction insufficient?

??? question "Reveal answer"

    **Verdict:** Eviction does not fence an in-flight stale reader.

    **Concrete history:** reader R loads version 7; owner commits deletion/version 8 and evicts;
    R then writes version 7 into the empty cache. The stale mapping survives despite successful
    eviction. Delaying a second delete reduces some races but is not a general correctness proof.

    Retain generation-aware invalidation/tombstones where the contract requires it, reject older
    cache writes against the current generation, and define expiry/refresh behavior across local,
    shared, edge, and browser layers. A cache write check protects only the layers enforcing it.
    Immediate revocation may require authoritative validation or refusing long-lived caching.

    **Trade-off:** stricter revocation adds coordination and availability dependence. Never reusing
    codes simplifies old-bookmark and old-cache safety at namespace cost.

    **Interview-ready answer:** “I would specify a revocation window and ensure stale fills cannot
    overrule newer lifecycle state inside controlled caches. I cannot promise immediate global
    browser revocation after previously allowing permanent cached redirects.”

    **Follow-up:** How long must a deletion tombstone outlive cached mappings and replayed changes?

## Round 9 — Viral keys and cold-cache bursts

### Q9. One celebrity link receives a million reads/s. Does sharding by code fix the hot key?

??? question "Reveal answer"

    **Verdict:** Sharding distributes different keys, not one key's load.

    Cache immutable hot mappings near clients and replicate them across bounded local/edge caches.
    Use request coalescing, bounded source concurrency, staggered refresh, and allowed stale serving
    to avoid a stampede on expiry. A per-process coalescer does not elect one filler across the fleet.

    | Failure | Mitigation | Remaining cost |
    |---|---|---|
    | Hot shared-cache key | Local/edge replication | Invalidation complexity |
    | Simultaneous cold start | Admission and staged warm-up | Initial misses stay slower |
    | Random missing-code scans | Negative cache, quotas, validation | Cardinality/memory pressure |
    | Analytics hot counter | Partition events and aggregate later | Delayed totals |

    Bound negative-cache TTL so new creation does not remain invisible unnecessarily. Avoid a
    synchronous counter write to the mapping row on every redirect.

    **Interview-ready answer:** “I would replicate hot reads and bound misses before changing the
    storage shard map. A single-key benchmark and fleet cold-start test are distinct from uniform
    random lookup throughput.”

    **Follow-up:** What happens when every application instance restarts during the viral spike?

## Round 10 — Cache and source outages

### Q10. Redis fails while the mapping database is near capacity. Should every request fall back?

??? question "Reveal answer"

    **Verdict:** Fallback must have admission limits or it transfers the outage to the authority.

    | Operation | Possible degradation |
    |---|---|
    | Valid cached public immutable redirect | Serve within permitted lifetime |
    | Unknown mapping | Bounded source lookup or explicit unavailable result |
    | Revoked/private mapping | Respect safety policy; do not invent authorization |
    | Management write | Fail/persist only under its documented durability contract |
    | Analytics | Bounded buffer/drop or durable backpressure, according to contract |

    Use short client deadlines, breakers, and source bulkheads. Expired unknowns are not proven
    missing: distinguish authoritative 404 from lookup unavailability. After recovery, reopen
    gradually rather than flooding the source with all misses and retrying clients.

    **Interview-ready answer:** “I would preserve safe cached reads while capping source fallback.
    Public redirect availability and strict lifecycle enforcement can have different degradation
    policies. An unavailable database must not turn every existing link into a cached 404.”

    **Follow-up:** Which metric detects successful stale responses that violate the promised revocation window?

## Round 11 — Analytics delivery and rebuild

### Q11. Kafka is down, then an analytics replay duplicates clicks. How accurate can counts be?

??? question "Reveal answer"

    **Verdict:** Declare what was durably observed and deduplicate that identity—not an imagined universal click.

    Assign stable ingestion IDs where the event is recorded, preserve them through retries, and
    aggregate with an idempotent window/batch protocol. Edge hits, client retries, bots, and browser
    caches complicate “unique click” semantics independently of broker delivery.

    Best-effort in-memory ingestion may lose events on process death; a durable acceptance path
    costs latency/storage and can require backpressure. A request log can be a recovery source
    only if its completeness, retention, privacy, and identity are defined.

    For rebuild, use a separate projection and verified input boundary; isolate it from customer
    notifications or billing. Compare totals and sampled partitions, account for late events and
    deletions, and cut over with rollback capacity.

    **Interview-ready answer:** “I would separate redirect success from analytics guarantees,
    preserve ingestion identity through replay, and document observation gaps. Exactly-once broker
    processing does not make cached or unobserved browser clicks measurable.”

    **Follow-up:** If analytics determines billing, which earlier architecture decisions must change?

## Round 12 — Abuse and private links

### Q12. How do you control malicious destinations, enumeration, and preview-fetch SSRF?

??? question "Reveal answer"

    **Verdict:** Validate at each trust boundary; random identifiers are not access control.

    Enforce destination scheme/length policy, authenticated creation quotas, owner authorization,
    and abuse review/takedown. A public redirect necessarily redirects externally; the product
    needs an explicit acceptable-use policy rather than an accidental arbitrary-protocol launcher.

    Preview/unfurl services fetch server-side and require separate SSRF defenses: destination/network
    restrictions, revalidation after resolution/redirects, resource caps, and restricted egress.
    Merely returning Location does not perform that server fetch. Private links require a defined
    authorization boundary and cache partitioning; do not share an authorized response publicly.

    Protect raw destination/query data from routine logs and high-cardinality metrics. Link previews
    and scanners may trigger analytics without a person intentionally opening the link.

    **Interview-ready answer:** “I would secure management and any fetcher separately, keep private
    redirects out of shared public caches, and provide an auditable takedown path. Entropy limits
    guessing but does not replace owner checks or authorized access.”

    **Follow-up:** Can a preview worker access an internal address after following a public redirect?

## Round 13 — Regional ownership

### Q13. A link is created in Europe and immediately read in Asia. What consistency is promised?

??? question "Reveal answer"

    **Verdict:** Define creation visibility and alias write ownership before deploying regional replicas.

    Asynchronous replication may return missing immediately after creation. Options include a
    bounded primary fallback, a creation/version token, home-region routing, or an explicit
    propagation interval. Negative caches must not extend the delay indefinitely.

    | Write design | Benefit | Cost |
    |---|---|---|
    | One/home-region authority | Straightforward alias uniqueness | Cross-region management latency/failover |
    | Strong global alias reservation | One global conflict decision | Coordination latency and partition dependence |
    | Independent namespaces | Local creation availability | Namespace/routing and product constraints |

    Region-independent random generation lowers collision probability but does not enforce custom
    alias ownership. Promotion must fence old writers and preserve the acknowledged commit
    guarantees actually promised; read cache availability is a separate question.

    **Interview-ready answer:** “I would explicitly handle read-after-create and route alias changes
    to an authority. Global read replication does not automatically provide global unique writes
    or immediate revocation during partitions.”

    **Follow-up:** What does failover do with a creation acknowledged but not replicated?

## Round 14 — Evolution and retention

### Q14. How do you move to longer codes or another shard layout without breaking old links?

??? question "Reveal answer"

    **Verdict:** Preserve stable external identifiers while migrating internal routing compatibly.

    Deploy readers that accept both code lengths before issuing the new format. Keep reserved
    routes, custom domains, case sensitivity, and normalization consistent across old/new versions.
    Never reinterpret an old code as a different destination to save namespace.

    For storage movement, establish routing ownership/version, copy a consistent baseline, apply
    concurrent changes, verify counts/versions/tombstones, then cut over reads/writes with fencing
    and rollback. Blind dual writes create another partial-failure problem. Backfills must not
    resurrect revoked mappings or override newer owner edits.

    Expired mapping cleanup, cache expiry, analytics retention, and operation deduplication have
    different horizons. A backup restore must replay deletions before exposing old data.

    **Interview-ready answer:** “I would evolve readers first and keep the public code contract
    immutable. Migration needs a verified change boundary and one current write owner; cleanup
    cannot discard the tombstones and history required for safe restore.”

    **Follow-up:** How would you test an emergency rollback after new-length codes have been issued?

## Round 15 — Evidence and final defense

### Q15. What proves this complete design is credible, and how would you summarize it?

??? question "Reveal answer"

    **Verdict:** Qualify lifecycle correctness and cold/failure behavior, not only warm-cache latency.

    | Test | Evidence |
    |---|---|
    | Alias and request races | One owner, one recoverable creation result |
    | Stale fill after revoke | Observed revocation window and no forbidden resurrection |
    | Hot/cold skew | Tail latency, bounded source load, controlled rejection |
    | Cache/source outage | Documented safe degradation and recovery |
    | Analytics replay | Accountable loss/deduplication under the chosen contract |
    | Regional promotion/restore | Ownership exclusion, missing-write policy, tombstone preservation |

    Report offered/completed traffic, incorrect redirects, cache/source waits, revocation lag,
    creation conflicts, analytics loss, and bounded resource usage. Keep raw codes/destinations
    out of metric dimensions. State which behavior is proposed versus actually tested in this lab.

    **Interview-ready answer:** “I start with unique durable mappings and recoverable creation.
    Edge/local caches handle hot reads under explicit HTTP freshness policy. Revocation is versioned,
    fallback is bounded, analytics has a stated observation contract, and write ownership remains
    explicit across regions. I accept coordination or lower cacheability where correctness requires
    it, and validate the full lifecycle with races, outages, restore, and representative skew.”

    **Follow-up:** Which requirement would force the largest change to this design?

## Review gate

Draw the complete flow; explain the creation commit, stale-fill race, cache contract, analytics
ambiguity, and regional write boundary. Finish those arguments before the next case.

## Related

- [Practice sequence](practice.md)
- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Cache consistency mechanisms](../caching-redis/practice.md)
- [API identity and response mechanisms](../rest-api/practice.md)
- [Next: Payments](practice-payments.md)
