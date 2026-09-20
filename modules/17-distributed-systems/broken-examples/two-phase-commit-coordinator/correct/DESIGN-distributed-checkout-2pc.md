# Architecture Design: Asynchronous Saga Orchestration for Distributed Checkout

## Objective
Provide atomic-like business workflows across distributed microservices (`OrderService`, `PaymentService`, `InventoryService`) while preserving microservice autonomy, high availability, and non-blocking execution.

## Architecture

We implement the **Saga Pattern with Orchestration** and the **Transactional Outbox Pattern**. Each service executes strictly local, sub-10ms database transactions and releases row locks immediately.

```
       +------------------------------------+
       |       Checkout Saga Orchestrator   |
       +------------------------------------+
           |                  ^             |
   1. ReserveInventory        |     3. ProcessPayment
           |        2. InventoryReserved    |
           v                  |             v
    +--------------+          |      +--------------+
    |  Inventory   |----------+      |   Payment    |
    |   Service    |                 |   Service    |
    +--------------+                 +--------------+
```

### Protocol Execution Flow

1. **Step 1: Create Pending Order (Local Transaction):**
   - `OrderService` inserts an order with status `PENDING` and writes an `OrderPlaced` event to its local outbox table atomically.
   - The transaction commits immediately and releases database connections.

2. **Step 2: Inventory Reservation with Timeout:**
   - The Saga Orchestrator triggers `ReserveInventory`.
   - `InventoryService` creates an inventory reservation record with a 15-minute lease and commits. No cross-service lock is held.

3. **Step 3: Payment Processing:**
   - The Saga Orchestrator requests payment authorization from `PaymentService`.
   - **Happy Path**: Payment succeeds. The Orchestrator commands `OrderService` to mark the order `CONFIRMED` and `InventoryService` to commit the reservation permanently.
   - **Failure Path (Compensating Transaction)**: If payment fails or times out:
     - The Orchestrator initiates compensating actions:
       - Calls `InventoryService.releaseReservation(reservationId)`.
       - Calls `OrderService.markCancelled(orderId, "PAYMENT_DECLINED")`.
     - Compensating transactions are strictly idempotent and retried until confirmed.

## Stated Benefits
- **Non-Blocking**: No service holds database locks while waiting for remote network calls.
- **High Availability**: If `PaymentService` is temporarily down, reservations can be held or queued without blocking other inventory operations.
- **Service Autonomy**: Services remain decoupled behind asynchronous message queues.
