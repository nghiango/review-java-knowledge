# Code Review — Blocking in Request Flow

## Context

A payment gateway aggregator routes transaction settlement requests to third-party bank providers using Spring WebFlux. An engineer authored `ReactivePaymentGateway.java` to coordinate token verification and payment charging.

Review `ReactivePaymentGateway.java` for event loop blocking, thread starvation hazards, and reactive lifecycle misuse.

## What to look for

- Synchronous blocking method calls (`.block()`, `CompletableFuture.get()`, `join()`) inside reactive pipelines
- Netty worker event loop thread starvation and throughput collapse
- Preservation of non-blocking reactive chains (`flatMap`, `zipWith`, `then`)
- Error propagation and fallback handling
