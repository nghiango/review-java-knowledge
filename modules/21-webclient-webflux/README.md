# Module 21 — WebClient / WebFlux

This module covers non-blocking reactive programming with Spring WebFlux, Project Reactor (`Mono` / `Flux`), and `WebClient`:
blocking vs non-blocking event loops, Reactive Streams subscription lifecycle, backpressure strategies, WebClient connection pooling and timeouts, Reactor threading and `Schedulers`, diagnosing and avoiding blocking calls in reactive pipelines (BlockHound), reactive error handling, and concurrency control in flatMap pipelines.

The canonical prose, architecture blueprints, interview Q&A, and operational incident guides live in the documentation:

👉 **[WebClient / WebFlux Documentation](../../docs/topics/webclient-webflux/index.md)**

## Broken Review Examples

This module provides 5 realistic broken code review targets under `broken-examples/`:

1. `block-in-request-flow/` — Invoking `.block()` or `toFuture().get()` directly inside WebFlux controller/handler pipelines, freezing Netty event loop threads.
2. `blocking-jdbc-in-webflux/` — Calling synchronous blocking JDBC repositories (`JdbcTemplate`) directly on the reactive event loop without offloading to a bounded elastic scheduler.
3. `uncontrolled-flatmap-concurrency/` — Unbounded `flatMap` concurrency fanning out thousands of parallel requests simultaneously, exhausting connection pools and downstream sockets.
4. `missing-webclient-timeout/` — WebClient configured without Netty channel connect and response timeouts, allowing hanging connections to pin resources indefinitely.
5. `chain-without-error-handling/` — Reactive stream pipeline lacking `.onErrorResume()` or `.onErrorReturn()`, terminating the stream on the first error and leaking unhandled 500s.
