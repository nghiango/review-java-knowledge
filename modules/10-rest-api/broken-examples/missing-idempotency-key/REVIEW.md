# Code Review: Missing Idempotency Key in Payment Processing

## Context
A payment service charges credit cards via `POST /api/payments/charge`. Clients retry failed or timed-out requests automatically with exponential backoff.

## Code Under Review
- `PaymentService.java` — Core credit card gateway integration.
- `PaymentCheckoutController.java` — REST controller invoking `executeCharge` on every `POST` invocation.

## Review Questions
1. What happens when a network timeout occurs after the payment gateway completes the charge but before the HTTP response reaches the client?
2. How does the standard `Idempotency-Key` HTTP header work in distributed payment APIs (e.g. Stripe, PayPal, Square)?
3. What is the storage and caching strategy for cached idempotency responses (including in-flight locks)?
