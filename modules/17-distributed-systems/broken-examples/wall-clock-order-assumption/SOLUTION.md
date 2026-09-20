# Solution — Wall-Clock Ordering in Multi-Region Ledger Replication

## Annotated code

```markdown
# System Design: Multi-Region Active-Active Ledger Replication

## Overview
To provide sub-50ms transaction latency worldwide, user account balances are replicated across three cloud regions (`us-east`, `eu-central`, `ap-southeast`). Users can deposit, transfer, and withdraw funds against their nearest regional API gateway.

## Data Model & Replication Flow

Each ledger entry contains an account balance mutation:

```json
{
  "transactionId": "tx-883192",
  "accountId": "acc-9921",
  "amountDelta": -50.00,
  "resultingBalance": 450.00,
  "clientTimestamp": 1726880000100,
  "serverTimestamp": 1726880000105
}
```

### Conflict Resolution Strategy: Last-Write-Wins (LWW)
When concurrent transactions execute across different regions for the same account:
1. Each regional application node timestamps the ledger entry with its local system clock:
   ```java
   long timestamp = System.currentTimeMillis();
   ```
   <!-- Data consistency issue: Unsynchronized physical wall-clock timestamps for conflict resolution.
   Physical quartz oscillators drift due to thermal fluctuations, virtualization scheduling, and network latency.
   NTP synchronization cannot guarantee bounded skew; clock drift of tens to hundreds of milliseconds is common,
   and NTP step adjustments or leap seconds can move time backward. If Node A's clock is 50ms ahead of Node B's clock,
   a transaction on Node B executed after Node A will be assigned a smaller timestamp and silently dropped by LWW,
   causing silent ledger balance divergence and lost updates. -->

2. Transactions are asynchronously replicated to peer regions via Apache Kafka MirrorMaker.
3. Upon receiving a replicated transaction from a remote region, the regional database evaluates:
   ```sql
   UPDATE accounts
   SET balance = :newBalance,
       last_updated = :serverTimestamp
   WHERE account_id = :accountId
     AND last_updated < :serverTimestamp;
   ```
4. If a local balance record has `last_updated >= :serverTimestamp`, the incoming replicated transaction is discarded as an older write.
   <!-- Architecture issue: Multi-master active-active balance mutations with Last-Write-Wins.
   A bank account balance is not an idempotent register; it is an accumulative ledger. Overwriting balance state
   via Last-Write-Wins (LWW) drops intermediate debits and credits. Even with perfect clocks, two concurrent
   withdrawals of $50 from a $100 account in different regions would both succeed (resulting in an illegal overdraft)
   and one would overwrite the other's balance, violating double-entry accounting invariants. -->

## Assumptions
- AWS / GCP cloud host VMs run standard NTP (Network Time Protocol) daemons, maintaining server clock synchronization within 5–10 milliseconds.
- Physical server timestamps provide a strict total ordering of financial transactions across regions.
```

## Issue list

### Data consistency issue: Physical wall-clock LWW causes lost updates due to clock drift

- **Location:** `DESIGN-ledger-replication.md:23`
- **Description:** Using `System.currentTimeMillis()` for Last-Write-Wins (LWW) conflict resolution across distributed servers.
- **Impact:** Physical clocks drift continuously. An NTP daemon can slew or step the clock backward, or lag by dozens of milliseconds across geographic continents. A transaction that causally occurred *after* another transaction will be permanently discarded if its host node's clock happens to lag behind, causing silent data loss and balance discrepancies.
- **Remediation:** Replace wall clocks with Hybrid Logical Clocks (HLC) or Lamport/Vector timestamps to preserve causal ordering, or adopt Google TrueTime with bounded uncertainty intervals ($\epsilon$).

### Architecture issue: LWW overwrites accumulative ledger state and allows double-spending

- **Location:** `DESIGN-ledger-replication.md:32`
- **Description:** Mutating account balances across active-active multi-master regions using LWW register replacement.
- **Impact:** Balances are commutative aggregations of ledger events, not single-value registers. LWW overwrites earlier mutations rather than applying deltas. Furthermore, active-active multi-region writes on balance accounts allow concurrent withdrawals exceeding available funds (double-spending / overdraft).
- **Remediation:** Enforce home-region affinity for accounts (single-writer per account), or record immutable delta events (`amountDelta`) replicating via an append-only log with deterministic CRDT (Conflict-Free Replicated Data Type) or consensus-based sequence numbers.

## Correct implementation

See [`correct/DESIGN-ledger-replication.md`](correct/DESIGN-ledger-replication.md).

Detailed discussion in [Solutions](../../../docs/topics/distributed-systems/solutions.md#logical-clocks-and-causal-ordering-for-ledger-replication).
