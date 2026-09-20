# Code Review — High-Cardinality Metric Tags

## Context

A high-volume e-commerce checkout service records transaction volume using Micrometer metrics. An engineer authored `OrderPaymentMetricsService.java` to track processed payments for business dashboards.

Review `OrderPaymentMetricsService.java` for time-series cardinality, memory footprint, and metric registry stability.

## What to look for

- Inclusion of high-cardinality attributes (UUIDs, user IDs, emails, timestamps) as metric tags
- Metric registry memory growth and heap starvation
- Scraping performance degradation on Prometheus endpoints (`/actuator/prometheus`)
- Appropriate separation of metrics vs structured logs vs distributed traces
