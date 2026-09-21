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
   - The Order Service receives a checkout request and initiates a distributed Two-Phase Commit (2PC) transaction involving Order Service and Payment Service.
   - The Payment Service starts a local database transaction:
     `BEGIN TRANSACTION;`
     `INSERT INTO payments (id, order_id, amount, status) VALUES (uuid, 101, 49.99, 'PROCESSING');`
   - While holding the database transaction open, Payment Service issues an HTTP POST request to `https://api.stripe.com/v1/charges` with the credit card payload.
   - Upon receiving HTTP 200 OK from Stripe, Payment Service executes:
     `UPDATE payments SET status = 'SUCCESS' WHERE id = uuid;`
     `COMMIT;`
   - Payment Service returns 200 OK to Order Service, which commits its local transaction.

2. **Failure Handling**:
   - If Stripe returns an error or times out, Payment Service executes `ROLLBACK` and throws an HTTP 500 error to the client.
   - If the client times out, the client immediately retries the HTTP request to Payment Service.
