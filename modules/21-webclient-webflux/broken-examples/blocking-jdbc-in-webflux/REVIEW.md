# Code Review — Blocking JDBC in WebFlux

## Context

An analytics data service exposes customer financial summaries via Spring WebFlux. An engineer wrote `CustomerSummaryService.java` querying a legacy relational database via blocking Spring JDBC.

Review `CustomerSummaryService.java` for event loop thread pinning, connection blocking, and scheduler delegation.

## What to look for

- Blocking JDBC driver calls executed on the Reactor Netty event loop
- Schedulers isolation (`Schedulers.boundedElastic()`)
- Backpressure and thread pool bounding
