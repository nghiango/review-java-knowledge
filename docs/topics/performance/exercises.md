# Performance Exercises

## 1. Build a capacity model

Given 300 requests/second, 80 ms average DB time, six replicas, and a 120-connection database limit
with 20 reserved connections, estimate in-flight DB work and a safe pool cap. State the assumptions.

??? success "Reveal solution"

    Little's Law estimates `300 × 0.080 = 24` concurrent DB operations globally. The hard cap from
    the database budget is `(120 - 20) / 6 = 16` per replica. Start below the hard cap, preserve
    headroom, and load test tail latency and bursts.

## 2. Profile allocation

Run both metric encoders for a finite million iterations under a 60-second JFR. Compare allocation
flame graphs, GC frequency, CPU, and output correctness.

??? success "Reveal solution"

    Expect regex compilation, collectors, intermediate maps, formatting, and logging in the broken
    path. Keep the optimized version only if the representative measurement is material and its
    protocol tests remain green.

## 3. Design overload behavior

Choose a worker count, queue capacity, rejection behavior, and metrics for a CPU-heavy report API.

??? success "Reveal solution"

    Start near processor count, choose queue capacity from allowed queue delay and service rate,
    reject with a clear temporary-overload response, and expose active workers, queue depth, oldest
    task, rejections, completion latency, and cancellation.

## Related

- [Concepts](concepts.md)
- [Tests](tests.md)
- [Questions](questions.md)
