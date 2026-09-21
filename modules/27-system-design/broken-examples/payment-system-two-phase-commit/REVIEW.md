# System Design Review: Payment Processing Service

## Context
This design proposal was submitted by a team proposing an updated Payment Processing architecture for an enterprise e-commerce platform processing $10M in daily GMV.
The proposal aims to guarantee financial consistency between customer checkouts, database records, and external payment processors.

## Files Under Review
- `design.md` — Architectural specification and transaction workflow.

## Review Questions
1. Why is Two-Phase Commit (2PC) infeasible across external SaaS payment gateways (Stripe, Adyen)?
2. What happens to database connection pools (HikariCP) when external HTTP calls are made inside an active database transaction?
3. What catastrophic financial error occurs if Stripe charges the card successfully, but the network drops before the Payment Service can execute `COMMIT`?
4. What happens when the client retries a timed-out payment request?
5. How would you redesign this system using the **Transactional Outbox pattern**, **Idempotency Keys**, and an **Asynchronous Reconciliation Loop**?
