# Code Review: GET Request Mutating State

## Context
An e-commerce order cancellation flow triggers order cancellation via a link provided in email notifications. The endpoint is mapped to `@GetMapping("/api/orders/{id}/cancel")`.

## Code Under Review
- `OrderCancellationController.java` — REST controller handling cancellations via HTTP GET.

## Review Questions
1. Why does RFC 9110 classify HTTP `GET`, `HEAD`, and `OPTIONS` as "Safe" and "Idempotent" methods?
2. What happens when web browsers prefetch links or enterprise email scanners inspect the cancellation URL?
3. What are the correct RESTful verb and resource modeling options to cancel an order?
