# Performance Concepts

## Latency, throughput, and percentiles

Latency describes one operation; throughput describes completed work per unit time. Always report
the workload and concurrency beside them. Averages conceal skew, so service objectives normally use
p50, p95, p99, and a maximum or timeout rate. Percentiles from different instances cannot be
averaged; aggregate histogram buckets instead.

## Utilization and saturation

Utilization says how busy a resource is. Saturation says whether work is waiting. CPU near 100%, a
nonempty executor queue, Hikari pending callers, or database lock waits are saturation signals.
Latency rises sharply near a resource's service limit because queue time compounds.

## Capacity and Little's Law

For a stable system, average in-flight work is approximately throughput multiplied by average time:
`L = λW`. At 200 requests/second and 50 ms service time, about 10 requests are in flight. This is a
starting model, then bursts, tail latency, headroom, and downstream limits must be measured.

## Resource budgets

A local setting participates in a global budget. If PostgreSQL permits 100 connections, 10 are
reserved, and six replicas run, each replica cannot safely configure 50 connections. Queue and pool
sizes also need timeouts, rejection behavior, and metrics.

## Experimental method

1. State the user symptom and service objective.
2. Capture a baseline with a representative workload.
3. Find the saturated resource using metrics and a profile.
4. Change one mechanism.
5. repeat the same workload and compare distributions, errors, and resource use.

Microbenchmarks answer narrow code questions. End-to-end load tests answer system questions. Neither
substitutes for production telemetry.

## Related

- [Internals](internals.md)
- [Production diagnostics](production.md)
- [Observability concepts](../observability/concepts.md)
