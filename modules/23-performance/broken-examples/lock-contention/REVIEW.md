# Review: metric accumulator contention

Review `MetricAccumulator.java` as a shared component updated by every request. Consider lock
scope, key independence, boxing, throughput, read semantics, and how contention is diagnosed.
Write findings before opening `SOLUTION.md`.

Reproduce with 2, 8, 32, and 128 writers over at least four metric names; record JFR monitor-blocked
events and throughput rather than asserting timing in a unit test.
