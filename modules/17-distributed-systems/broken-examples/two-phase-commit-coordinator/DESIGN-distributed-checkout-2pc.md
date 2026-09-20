# Architecture Design: Synchronous Two-Phase Commit Across Microservices

## Objective
Provide ACID transactional guarantees across distributed microservices during customer checkout so that payment deduction, inventory reservation, and order record persistence either all commit atomically or all roll back together.

## Architecture

A dedicated `TransactionCoordinatorService` manages checkout transactions using an XA-style Two-Phase Commit protocol over synchronous gRPC.

```
       +------------------------------------+
       |   Transaction Coordinator Service  |
       +------------------------------------+
           |               |              |
    Prepare / Commit  Prepare / Commit  Prepare / Commit
           |               |              |
           v               v              v
     +-----------+   +-----------+   +-----------+
     |   Order   |   |  Payment  |   | Inventory |
     |  Service  |   |  Service  |   |  Service  |
     +-----------+   +-----------+   +-----------+
```

### Protocol Execution

1. **Phase 1 (Prepare):**
   - Coordinator issues `Prepare(txId)` RPC to `OrderService`, `PaymentService`, and `InventoryService`.
   - Each participant starts a local database transaction, acquires pessimistic row locks on affected entities (`SELECT ... FOR UPDATE`), performs validation, writes changes to local WAL, and responds `VOTE_COMMIT`.

2. **Phase 2 (Commit or Abort):**
   - If all three participants vote `VOTE_COMMIT`, Coordinator writes `COMMIT` to its local log and broadcasts `Commit(txId)` RPCs.
   - Participants commit their local database transactions, release row locks, and acknowledge `COMMITTED`.
   - If any participant votes `VOTE_ABORT` or times out, Coordinator broadcasts `Abort(txId)`. Participants roll back local transactions and release locks.

## Stated Benefits
- Strict consistency: Users never see reserved inventory without a corresponding completed payment.
- Developer simplicity: Programmers reason about distributed checkout as a single atomic transaction.
