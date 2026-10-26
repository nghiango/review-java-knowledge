# Observability — Practice Q&A

Senior-depth practice from interview-prep §9 and §19. Source metric names are illustrative;
verify emitted series and labels before applying any query or alert to the lab.

### Q1. How does observability differ from monitoring?

??? question "Reveal answer"
    - **Monitoring checks known conditions; observability supports new questions about behavior.**
    - **Mechanism:** metrics summarize trends, logs record discrete context, traces connect causality, and profiles explain resource cost. RED and USE organize service and resource signals.
    - **Failure:** many dashboards without correlation or bounded labels still cannot explain one slow request. More logs can obscure evidence and increase cost.
    - **Fix:** instrument critical boundaries, latency distributions, queue waits, and business outcomes; link requests, events, and transitions through safe identifiers.
    - **Trade-off:** sampling, storage, and privacy constrain detail. Define what must remain measurable when exporters or backends fail.
    - **Follow-up:** Which unknown production question can your current telemetry answer without a redeploy?
    - **Takeaway:** Optimize investigative capability, not telemetry volume.

### Q2. What should a commerce service measure through Micrometer and Prometheus?

??? question "Reveal answer"
    - **Cover user outcomes, asynchronous progress, and shared resource saturation.**
    - **Mechanism:** HTTP count/duration/error histograms, Hikari acquisition/pending, outbox age/rate, consumer lag, compensation outcomes, and cache loads explain critical paths.
    - **Failure:** a successful HTTP response can hide a stuck order; raw URIs or user IDs create unbounded series. A gauge without a live strongly retained source may disappear.
    - **Fix:** choose bounded label sets and units, register metrics predictably, and verify actual scraped names; distinguish empty backlog from absent telemetry.
    - **Trade-off:** histograms multiply series by buckets and label combinations; budget cardinality before adding dimensions.
    - **Follow-up:** Which metrics still reveal stalled processing when no client instance is alive?
    - **Takeaway:** Measure the business pipeline, not only the web server.

### Q3. How do SLOs and burn-rate alerts improve on short static thresholds?

??? question "Reveal answer"
    - **Page on user-impacting budget consumption with fast and slow confirmation windows.**
    - **Mechanism:** define eligible events, good outcomes, target, and window; burn rate compares bad-event ratio to the allowed error ratio. Latency SLOs can classify requests at a fixed threshold.
    - **Failure:** a one-minute p95 alert is not a monthly availability SLO; low traffic and missing data can give misleading ratios.
    - **Fix:** use multi-window alerts, explicit no-data behavior, actionable runbooks, and ownership. Separate asynchronous freshness SLOs from HTTP success.
    - **Trade-off:** symptom alerts reduce noise but need cause dashboards for diagnosis; test quiet and low-volume cases too.
    - **Follow-up:** How are 429/503 responses and planned maintenance counted?
    - **Takeaway:** Define the denominator before promising reliability.

### Q4. Why can you not average pod p95 values?

??? question "Reveal answer"
    - **Quantiles are not additive and averaging loses traffic weighting and distribution shape.**
    - **Mechanism:** for compatible classic histograms, aggregate bucket rates across relevant pods, retaining `le`, then compute the quantile; preserve route/service dimensions as needed.
    - **Failure:** averaging a low-traffic slow pod and a high-traffic fast pod invents a percentile. Incompatible bucket layouts or sparse samples distort interpretation.
    - **Fix:** use histogram-based aggregation and inspect sample volume, tails, errors, and saturation together. Prefer SLO-aligned bucket boundaries for threshold SLIs.
    - **Trade-off:** histograms approximate values; more buckets improve resolution but increase series cost.
    - **Follow-up:** Why does an arrival-rate load test still need dropped-iteration accounting?
    - **Takeaway:** Aggregate observations before calculating percentiles.

### Q5. How do you detect an alert that queries a nonexistent metric?

??? question "Reveal answer"
    - **Test telemetry and rules as a contract, including missing-series behavior.**
    - **Mechanism:** PromQL often returns an empty vector for missing data instead of a failing alert. Exported names can differ from registration names through naming conventions.
    - **Failure:** dashboards and runbooks refer to different backlog gauges, so a critical failure is invisible; a configured alert depends on an instrumentation feature that is disabled.
    - **Fix:** scrape a representative application, validate expected names/labels, exercise alert rules with fixtures, and add appropriately scoped absence checks and scrape-health alerts.
    - **Trade-off:** absence can be normal for lazy metrics or empty workloads; initialize or model that state deliberately.
    - **Follow-up:** How do you test the notification path as well as the rule expression?
    - **Takeaway:** An alert that never fires may be broken, not reassuring.

### Q6. Is propagating trace IDs equivalent to distributed tracing?

??? question "Reveal answer"
    - **Correlation without exported spans is useful but does not provide a searchable trace graph.**
    - **Mechanism:** observations create spans; propagation carries context; a configured exporter and collector deliver retained data. Async outbox publication requires persisted context or a deliberate span link.
    - **Failure:** IDs appear in logs but no exporter is active; sampling drops all interesting failures; baggage carries secrets or unbounded values.
    - **Fix:** verify HTTP/message propagation, exporter connectivity, sampling, and queue delays in an end-to-end trace. Prefer bounded metadata and appropriate links for long-lived asynchronous work.
    - **Trade-off:** head sampling is cheap but may miss errors; tail sampling needs collector capacity and delayed decisions. This lab's bridge differs from the case study's Brave example.
    - **Follow-up:** Should a replayed historical event become a child of a months-old request span?
    - **Takeaway:** Creation, propagation, export, and retention are separate checks.

### Q7. What makes structured logs useful without leaking data?

??? question "Reveal answer"
    - **Record decisions and safe context once at the boundary with stable fields.**
    - **Mechanism:** JSON and scoped MDC support filtering by service, version, trace, operation, and bounded outcome. Async boundaries require explicit context handling.
    - **Failure:** raw payloads expose PII; repeated exception logging multiplies noise; attacker-controlled fields permit log injection or storage abuse.
    - **Fix:** redact allowlisted fields, cap lengths, restore context in finally, and avoid logging credentials or authorization tokens. Preserve causes and suppressed failures in protected server logs.
    - **Trade-off:** sampling reduces cost but must retain meaningful counts elsewhere; parameterized logging avoids formatting costs, not eager evaluation of expensive arguments.
    - **Follow-up:** How is sensitive diagnostic access audited and retention limited?
    - **Takeaway:** Good logs explain decisions without reproducing the user's secrets.

### Q8. Orders are missing and the outbox is aging: where do you look?

??? question "Reveal answer"
    - **Locate the first durable hop that stopped progressing.**
    - **Diagnosis:** confirm the command row and outbox identity, compare oldest unpublished age and drain rate, then inspect claim locks/leases, scheduler progress, broker acknowledgments, and ISR errors.
    - **Fix:** repair the failed dependency or worker, then drain within database and broker limits. Increasing batch size or replicas before understanding claims can amplify duplicates and pressure.
    - **Verify:** reconcile event identities and business effects, not only backlog depth; confirm the oldest age and user freshness recover.
    - **Trade-off:** deleting stuck rows hides symptoms and loses intent. Preserve evidence and use reviewed repair/replay tools.
    - **Follow-up:** How would you distinguish a stopped relay from a relay that publishes but cannot mark completion?
    - **Takeaway:** Follow durable evidence before tuning throughput.

### Q9. A read endpoint's p95 regresses: how do you narrow it down?

??? question "Reveal answer"
    - **Split by revision, pod, route, and dependency while preserving the timeline.**
    - **Diagnosis:** compare cache-hit distributions, connection wait, replica fallback, SQL plans, CPU throttling, GC, and ingress retries. A single trace shows path cost; distributions show prevalence.
    - **Fix:** roll back a correlated change if safe, reduce harmful traffic, then repair the measured bottleneck. Do not scale the app into a saturated DB.
    - **Verify:** replay the representative workload and compare p50/p95/p99 plus errors and saturation at the same offered rate.
    - **Trade-off:** warm caches or reduced traffic can conceal a regression; record both offered and completed requests.
    - **Follow-up:** Why might p99.9 be terrible while p99 appears healthy?
    - **Takeaway:** Correlation narrows hypotheses; controlled evidence establishes cause.

### Q10. Something is wrong in production: what is your incident method?

??? question "Reveal answer"
    - **Scope impact, stabilize service, test hypotheses, and preserve evidence.**
    - **Mechanism:** establish incident ownership and timeline; compare changes and dimensions; follow one concrete request across edge, app, queues, and dependencies.
    - **Failure:** random restarts erase evidence; concurrent uncoordinated changes make results uninterpretable. Correlation with a deploy is evidence, not proof.
    - **Fix:** prefer reversible mitigations, record actions and expected outcomes, communicate uncertainty, and collect minimally invasive diagnostics before disruptive actions when safe.
    - **Trade-off:** urgent mitigation can precede full diagnosis, but requires an impact-aware rollback plan and verification of business correctness.
    - **Follow-up:** Which observation would falsify your leading hypothesis?
    - **Takeaway:** Run an evidence-driven incident, not a guessing contest.

### Q11. Latency is high but CPU is low: what does that suggest?

??? question "Reveal answer"
    - **Look for queueing or waiting rather than assuming insufficient CPU.**
    - **Diagnosis:** compare repeated thread dumps, pool pending, dependency timing, locks, scheduler delays, and network waits. Thread state alone does not identify the responsible resource.
    - **Failure:** more threads deepen connection queues; retries turn slow dependencies into overload; a blocked event loop may show little overall CPU usage.
    - **Fix:** bound waiting, shorten transaction/resource hold time, isolate dependencies, and remove the observed bottleneck before scaling.
    - **Trade-off:** fail-fast rejection lowers successful throughput during overload but can preserve latency and recovery for admitted work.
    - **Follow-up:** What distinguishes low load from a system stalled before it can accept requests?
    - **Takeaway:** Waiting time is often the missing latency budget.

### Q12. An accepted order is missing from GET: how do you investigate its data flow?

??? question "Reveal answer"
    - **Trace one operation identity through durable state, publication, projection, and cache.**
    - **Diagnosis:** verify the committed command, outbox row and publication outcome, topic/partition/offset, DLQ, consumer progress, read-model version, replica visibility, and each cache tier.
    - **Failure:** replay before finding the missing hop can create external duplicates; reading a lagging replica can falsely suggest data loss.
    - **Fix:** repair the failed hop and use audited idempotent replay/reconciliation. Clear only the affected cache scope after confirming authoritative state.
    - **Trade-off:** broad cache flushes and consumer rewinds are quick but impose load and replay effects; prefer targeted recovery.
    - **Follow-up:** How does the client distinguish projection pending from an order that never committed?
    - **Takeaway:** Prove where progress stopped before restarting the pipeline.

### Q13. How do you diagnose intermittent errors that cannot be reproduced?

??? question "Reveal answer"
    - **Improve discrimination before increasing log volume indiscriminately.**
    - **Diagnosis:** group by revision, pod/node, endpoint, payload size, tenant class, and timing around scheduled work. Compare latency spikes with configured deadline values and retry activity.
    - **Failure:** averages hide a rare bad pod; sampling drops the only useful trace; logs expose high-cardinality or personal data while adding little causal evidence.
    - **Fix:** add bounded targeted instrumentation, preserve failing inputs through approved redaction, capture slow/error traces, and reproduce the minimal conditions in staging.
    - **Trade-off:** temporary diagnostics require expiry, access control, overhead monitoring, and a rollback path.
    - **Follow-up:** Could shared dependency failover or shutdown races explain the timing?
    - **Takeaway:** Turn an intermittent symptom into a testable cohort.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [JVM diagnosis practice](../jvm/practice.md)
- [Kafka lag practice](../kafka/practice.md)
