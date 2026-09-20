# Review: checkout connection lifetime

Review `CheckoutService.java` as a production checkout path. Consider transaction duration,
connection ownership, downstream latency, failure handling, and how the behavior changes under
concurrent load. Write findings before opening `SOLUTION.md`.

Reproduce with 40 concurrent calls, a 10-connection pool, and a payment gateway delayed by 500 ms.
Observe active, idle, and pending connection counts.
