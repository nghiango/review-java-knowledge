# Solution: Payment Processing Architecture

## Annotated Target Review

```markdown
# System Design Proposal: High-Volume Global Payment Service

## 1. System Overview
The Payment Service processes customer card transactions, interfaces with third-party Payment Service Providers (PSP: Stripe, Adyen), and updates the internal financial ledger.

## 2. Proposed Architecture & Workflow
```
[Client] ---> [Order Service] ---> [Payment Service] ---> [External PSP (Stripe)]
                                           │
                                           ▼
                                [PostgreSQL Ledger DB]
```

1. **Transaction Flow**:
   # Reliability issue: Distributed Two-Phase Commit (2PC) cannot be coordinated with external third-party SaaS APIs (Stripe has no prepare/commit XA interface).
   - The Order Service receives a checkout request and initiates a distributed Two-Phase Commit (2PC) transaction involving Order Service and Payment Service.
   - The Payment Service starts a local database transaction:
     `BEGIN TRANSACTION;`
     `INSERT INTO payments (id, order_id, amount, status) VALUES (uuid, 101, 49.99, 'PROCESSING');`
     # Performance issue: Holding open an active database transaction and connection while executing external network I/O exhausts HikariCP connections in seconds.
   - While holding the database transaction open, Payment Service issues an HTTP POST request to `https://api.stripe.com/v1/charges` with the credit card payload.
     # Reliability issue: Dual-write hazard: if Stripe charges the card successfully but the DB server crashes or connection drops before COMMIT, the customer is billed but the database marks it as failed or aborted.
   - Upon receiving HTTP 200 OK from Stripe, Payment Service executes:
     `UPDATE payments SET status = 'SUCCESS' WHERE id = uuid;`
     `COMMIT;`
   - Payment Service returns 200 OK to Order Service, which commits its local transaction.

2. **Failure Handling**:
   - If Stripe returns an error or times out, Payment Service executes `ROLLBACK` and throws an HTTP 500 error to the client.
     # Resilience issue: Blindly retrying without an Idempotency-Key header causes duplicate credit card charges whenever timeouts occur.
     # Reliability issue: Lack of an asynchronous background reconciliation loop leaves ambiguous in-flight or timed-out transactions permanently unresolved.
   - If the client times out, the client immediately retries the HTTP request to Payment Service.
```

---

## Discovered Issues

### 1. Infeasibility of 2PC with External Gateways
Third-party APIs (Stripe, Adyen, PayPal) do not support the X/Open XA distributed transaction standard or two-phase commit protocols. You cannot "prepare" a credit card charge and hold a lock on the Visa/Mastercard network while waiting for an internal database commit.

### 2. Connection Pool Starvation via Network I/O in Transactions
Holding an active database connection while waiting for an external HTTP roundtrip ($500\text{ms} - 3000\text{ms}$) ties up HikariCP pool slots. Under a modest load of 50 requests/sec, a pool of 20 connections is completely exhausted, stalling all other database operations across the microservice.

### 3. Dual-Write Inconsistency & Money Lost
If the HTTP call to Stripe succeeds, but the Payment Service crashes or network drops before PostgreSQL executes `COMMIT`:
- The customer's credit card is charged $49.99.
- The internal database has rolled back the transaction.
- The merchant ledger shows no record of payment, and the customer receives an error screen, resulting in customer fury, chargebacks, and legal compliance violations.

### 4. Duplicate Charges Due to Missing Idempotency Keys
When a network timeout occurs between the client and Payment Service (or Payment Service and Stripe), the client retries. Without an `Idempotency-Key` passed to Stripe, Stripe treats the retry as a brand-new transaction and charges the customer twice.

---

## Correct Implementation

See [`correct/design.md`](correct/design.md) for the production-grade payment architecture leveraging **Idempotency Keys**, the **Transactional Outbox Pattern**, and **Asynchronous Ledger Reconciliation**.
