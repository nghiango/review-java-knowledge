# Code Review — Retry Without Timeout

## Context

An internal warehouse inventory service is polled by the order fulfillment pipeline to verify physical item availability before reservation. The developer implemented a bounded retry loop with linear backoff.

Review `InventorySyncService.java` for network timeouts, thread pool health, and connection management.

## What to look for

- Default connect and socket read timeouts on `RestClient`
- Thread blockage behavior when the remote service accepts TCP connections but hangs indefinitely
- Linear backoff versus exponential backoff with jitter
- Interruption handling and exception propagation
