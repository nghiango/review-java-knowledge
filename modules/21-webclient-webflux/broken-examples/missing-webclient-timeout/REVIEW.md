# Code Review — Missing WebClient Timeout

## Context

A logistics shipment tracker queries an external carrier API to compute delivery ETAs. An engineer authored `CarrierTrackingClient.java` configuring `WebClient` using standard default builders.

Review `CarrierTrackingClient.java` for connection hangs, read timeouts, socket descriptor leaks, and resource bounding.

## What to look for

- TCP connect timeouts on the underlying Netty `HttpClient`
- Response and read timeouts on the reactive channel
- Operator-level `.timeout()` boundaries
- Handling of `TimeoutException`
