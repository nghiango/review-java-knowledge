# Solution — Synchronous Two-Phase Commit Across Microservices

## Annotated code

```markdown
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
   <!-- Concurrency issue: Holding pessimistic database row locks across network RPCs.
   Holding open database transactions and exclusive row locks while waiting for multiple network RPCs to finish
   causes severe thread pool and connection pool exhaustion. Any network spike on PaymentService blocks inventory
   rows in InventoryService, stalling all other checkout requests across the company. -->

2. **Phase 2 (Commit or Abort):**
   - If all three participants vote `VOTE_COMMIT`, Coordinator writes `COMMIT` to its local log and broadcasts `Commit(txId)` RPCs.
   <!-- Reliability issue: Coordinator crash during Phase 2 causes distributed blocking (indefinite lock holding).
   2PC is fundamentally a BLOCKING protocol. If the coordinator crashes or is partitioned after participants
   vote VOTE_COMMIT but before they receive the Commit/Abort decision, participants CANNOT decide unilaterally.
   They cannot abort (the coordinator might have committed), and they cannot commit (the coordinator might have aborted).
   Participants must hold database row locks INDEFINITELY until the coordinator recovers, crippling system availability. -->
   - Participants commit their local database transactions, release row locks, and acknowledge `COMMITTED`.
   - If any participant votes `VOTE_ABORT` or times out, Coordinator broadcasts `Abort(txId)`. Participants roll back local transactions and release locks.

<!-- Architecture issue: Microservice temporal and availability coupling.
In a 2PC architecture across N microservices, system availability equals the PRODUCT of all participants:
A_total = A_coord * A_order * A_payment * A_inventory. If each service has 99.9% availability, total checkout availability
drops to 99.6%. 2PC violates microservice autonomy by binding their lifecycles into a single synchronous distributed lock. -->

## Stated Benefits
- Strict consistency: Users never see reserved inventory without a corresponding completed payment.
- Developer simplicity: Programmers reason about distributed checkout as a single atomic transaction.
```

## Issue list

### Reliability issue: Coordinator failure in Phase 2 causes distributed blocking and indefinite lock holding

- **Location:** `DESIGN-distributed-checkout-2pc.md:31`
- **Description:** Two-Phase Commit is a blocking consensus protocol; participants that vote `VOTE_COMMIT` surrender autonomy and must await coordinator instruction.
- **Impact:** If the coordinator crashes or experiences a network partition after receiving votes, all participant databases hold open physical transactions and exclusive row locks indefinitely. Subsequent transactions block on those locks, leading to connection pool exhaustion and complete system paralysis.
- **Remediation:** Replace synchronous 2PC with an asynchronous, non-blocking Saga pattern (orchestration or choreography) using local transactions and compensating actions.

### Concurrency issue: Wide transaction holding DB locks across network boundaries

- **Location:** `DESIGN-distributed-checkout-2pc.md:27`
- **Description:** Database row locks (`SELECT ... FOR UPDATE`) are held while waiting for remote network RPCs across independent services.
- **Impact:** Network latency, packet loss, or GC pauses in one service hold database connections and locks open in another service, collapsing throughput under concurrent load.
- **Remediation:** Keep local database transactions strictly sub-10ms, decouple services using asynchronous events, and model intermediate states explicitly (e.g. `INVENTORY_RESERVED`, `PAYMENT_PENDING`).

### Architecture issue: Temporal coupling reduces composite system availability

- **Location:** `DESIGN-distributed-checkout-2pc.md:37`
- **Description:** 2PC couples multiple microservices into a synchronous distributed dependency graph where all must be simultaneously available to complete a transaction.
- **Impact:** Compound availability degrades geometrically ($A_{total} = \prod A_i$). A partial outage in a secondary service completely halts checkout.
- **Remediation:** Adopt eventual consistency: accept the order, publish an outbox event, and process payments and inventory asynchronously.

## Correct implementation

See [`correct/DESIGN-distributed-checkout-2pc.md`](correct/DESIGN-distributed-checkout-2pc.md).

Detailed discussion in [Solutions](../../../docs/topics/distributed-systems/solutions.md#saga-orchestration-vs-two-phase-commit).
