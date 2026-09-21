# Production System Design: Resilient Payment Processing Architecture

## 1. Architectural Principles
1. **Never Hold Database Transactions Across Remote Network Calls**: Separate local state persistence from remote HTTP calls.
2. **End-to-End Idempotency**: Propagate client-supplied `Idempotency-Key` headers through internal services down to the payment gateway (Stripe).
3. **Event-Driven Asynchronous Settlement**: Leverage the **Transactional Outbox Pattern** and double-entry accounting ledgers.
4. **Active Reconciliation Engine**: Periodically reconcile internal transaction states against payment provider settlement reports (webhook + batch reconciliation).

## 2. Component Architecture
```mermaid
sequenceDiagram
    autonumber
    participant Client as Web / Mobile Client
    participant Order as Order Service
    participant Pay as Payment Service
    participant DB as PostgreSQL (Ledger & Outbox)
    participant Worker as Outbox Relay Worker
    participant Stripe as External PSP (Stripe)
    participant Recon as Reconciliation Cron Worker

    Client->>Order: Checkout (Idempotency-Key: K1)
    Order->>Pay: Create Payment (Idempotency-Key: K1)
    
    Note over Pay,DB: Phase 1: Local ACID Transaction (< 5ms)<br/>No external network calls!
    Pay->>DB: BEGIN;
    Pay->>DB: INSERT INTO payment_intents (id, order_id, amount, status='INITIATED', idempotency_key=K1);
    Pay->>DB: INSERT INTO outbox_events (aggregate_id, event_type='PAYMENT_REQUESTED', payload);
    Pay->>DB: COMMIT;
    Pay-->>Order: 202 Accepted (Payment Intent ID: P1)
    Order-->>Client: Order Pending

    Note over Worker,Stripe: Phase 2: Asynchronous PSP Execution
    Worker->>DB: Poll unhandled outbox events
    Worker->>Stripe: POST /v1/payment_intents (Idempotency-Key: K1, Amount: 49.99)
    Stripe-->>Worker: 200 OK (Status: Succeeded, PSP_Ref: ch_123)

    Worker->>DB: BEGIN;
    Worker->>DB: UPDATE payment_intents SET status='SUCCEEDED', psp_ref='ch_123';
    Worker->>DB: INSERT INTO double_entry_ledger (debit='CASH', credit='CUSTOMER_AR', 49.99);
    Worker->>DB: DELETE FROM outbox_events WHERE id=E1;
    Worker->>DB: COMMIT;

    Note over Recon,Stripe: Phase 3: Nightly Batch Reconciliation
    Recon->>Stripe: Fetch Settlement Report (CSV/API)
    Recon->>DB: Compare PSP settlements against double_entry_ledger
    Note over Recon: Reconciles any orphaned or timed-out transactions
```

## 3. Key Invariants & Trade-offs
- **Idempotency Table**: The `payment_intents` table enforces a `UNIQUE(idempotency_key)` constraint. Repeated requests within 24 hours immediately return the previously recorded payment status without re-charging the customer.
- **Connection Isolation**: DB transactions commit before contacting Stripe. HikariCP pool slots are held for $< 5\text{ms}$ instead of seconds.
- **Fault-Tolerant Retries**: If the worker crashes mid-flight, the outbox record is re-polled by another worker instance. The retry passes the same `Idempotency-Key: K1` to Stripe, which guarantees exactly-once charge execution at the banking gateway layer.
