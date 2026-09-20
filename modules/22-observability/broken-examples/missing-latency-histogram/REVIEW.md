# Code Review — Missing Latency Histogram and Tail Outliers

## Context

A checkout processing service measures response times for payment completion. An engineer authored `CheckoutLatencyTracker.java` to compute latency metrics and expose average execution times.

Review `CheckoutLatencyTracker.java` for statistical distribution validity, percentile aggregation, and SLA/SLO monitoring capabilities.

## What to look for

- Flaws of arithmetic mean/average latency measurements under skewed distributions
- Visibility of tail latency (p95, p99, p999) and GC pauses
- Configuration of Micrometer `Timer` with histogram buckets and percentiles
- Service Level Objective (SLO) boundary enforcement
