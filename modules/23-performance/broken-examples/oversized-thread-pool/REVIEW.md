# Review: report executor sizing

Review `ReportExecutor.java`. Consider CPU capacity, queue behavior, memory, overload feedback,
task failures, and shutdown. Write findings before opening `SOLUTION.md`.

Reproduce with 20,000 CPU-bound jobs returning 256 KiB and compare runnable threads, queue depth,
heap use, throughput, and p99 latency against a pool sized near the processor count.
