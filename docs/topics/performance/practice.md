# Performance — Practice Q&A

Senior-depth practice from interview-prep §8, §11, and §15.
The source's throughput measurements are external case-study claims, not reproduced lab results.

### Q1. How do you translate 10,000 users and a 5× spike into capacity requirements?

??? question "Reveal answer"
    - **Convert a workload model into arrival rate, resource demand, and recovery headroom.**
    - **Mechanism:** users are not in-flight requests; think time and endpoint mix determine RPS. Little's law uses average in-flight work = throughput × average time in a stable system.
    - **Failure:** substituting p95 for mean or assuming latency stays flat during overload underestimates queue growth. A cache-hit benchmark does not qualify checkout writes.
    - **Fix:** measure per-path demand at realistic data/skew, find the saturation knee, budget shared DB/broker limits, and include failed-node and cold-cache capacity.
    - **Trade-off:** autoscaling reacts after load changes; admission control and pre-provisioned headroom cover the reaction interval.
    - **Follow-up:** How much offered traffic is rejected while the admitted workload meets its SLO?
    - **Takeaway:** Capacity is a workload-specific envelope, not a user-count label.

### Q2. What should drive autoscaling a JVM service?

Merged from: §8 HPA + §15 HPA follow-up

??? question "Reveal answer"
    - **Scale on a demand signal that predicts useful capacity, constrained by downstream budgets.**
    - **Mechanism:** CPU fits CPU-bound work; concurrency or queue age can fit request workers; lag and drain rate can fit consumers up to partition parallelism.
    - **Failure:** JVM memory can remain high without more demand; latency scaling into a slow shared DB worsens overload. New pods are initially cold.
    - **Fix:** measure signal-to-capacity relationships, use stabilization and ceilings, reserve rollout surge connections, and qualify scale-in drain behavior.
    - **Trade-off:** custom metrics add monitoring/adapter dependencies; CPU is simpler but misses blocking saturation. Memory behavior varies by collector rather than never returning memory universally.
    - **Follow-up:** When should rising latency cause shedding rather than scale-out?
    - **Takeaway:** Add replicas only when they can complete more useful work.

### Q3. How do you load-test latency without hiding overload?

??? question "Reveal answer"
    - **Report offered load, achieved throughput, failures, dropped work, and latency together.**
    - **Mechanism:** open arrival models maintain offered rate; closed models reduce arrival when responses slow and can hide queueing through coordinated omission.
    - **Failure:** an overloaded generator drops scheduled iterations; a 100% cached dataset or tiny DB makes production comparisons meaningless.
    - **Fix:** define endpoint mix, think time, skew, sizes, warm-up, test duration, and thresholds; monitor generator health and production-like bottlenecks. Record raw evidence and configuration.
    - **Trade-off:** closed models still suit fixed-concurrent-user questions; neither model alone proves all workload behavior.
    - **Follow-up:** How do you distinguish server improvement from a reduced completed-request denominator?
    - **Takeaway:** A good percentile without workload evidence is not a qualification result.

### Q4. p99 is acceptable but p99.9 is terrible: where do you investigate?

??? question "Reveal answer"
    - **Find rare paths and stalls with enough samples to make the tail meaningful.**
    - **Diagnosis:** correlate slow traces with GC, throttling, cold starts, cache misses, connection waits, locks, retries, and partition head-of-line blocking.
    - **Failure:** sparse sampling and broad histogram buckets produce unstable extreme quantiles; looking only at successful requests hides timeout failures.
    - **Fix:** inspect sample counts and distributions, capture slow/error traces, separate cohorts, and repair the measured outlier source under an unchanged offered workload.
    - **Trade-off:** reducing tail latency can require headroom or bounded rejection rather than maximum throughput. Include all-attempt client timing and retry cost.
    - **Follow-up:** How many requests lie beyond p99.9 in the measurement window?
    - **Takeaway:** Extreme tails need evidence, not a longer list of JVM flags.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Database pool practice](../database-sql/practice.md)
- [JVM practice](../jvm/practice.md)
