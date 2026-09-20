# System Design: Multi-Region Ledger Replication with Home-Region Affinity & Hybrid Logical Clocks

## Overview
To guarantee strict linearizability and eliminate double-spending while serving global users with low latency, the ledger architecture separates account mastership from global read distribution.

## Architecture & Data Model

### 1. Home-Region Account Affinity (Single Master per Account)
- Each customer account is pinned to a primary **home region** based on customer residency (e.g. `acc-9921` is mastered in `eu-central`).
- **Write Path**: Any mutating transaction (deposit, withdrawal, transfer) targeting an account is routed directly to its home region via global Anycast / Route 53 latency routing or internal service mesh proxies.
- **Read Path**: Non-mutating read requests (balance check, transaction history) can be served from local regional read replicas with bounded staleness or read-your-writes guarantees.

### 2. Append-Only Event Log & Hybrid Logical Clocks (HLC)
Rather than overwriting balance registers with Last-Write-Wins, all mutations are structured as immutable, append-only journal entries sequenced by a **Hybrid Logical Clock (HLC)**:

$$HLC = \langle l, c \rangle$$

- $l$: Physical component (monotonic forward drift of physical clock).
- $c$: Logical component (incremented on events occurring within the same millisecond or upon receiving a message with equal/higher physical time).

```json
{
  "journalEntryId": "jnl-550192",
  "accountId": "acc-9921",
  "amountDelta": -50.00,
  "hlcTime": "2026-09-20T17:00:00.105Z",
  "logicalCounter": 3,
  "sequenceNumber": 10452,
  "prevHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

### 3. Conflict-Free Deterministic Replication
1. The home region writes the journal entry to an immutable transactional database (Aurora PostgreSQL / DynamoDB).
2. Journal entries are replicated asynchronously to follower regions via a replicated commit log (Kafka / AWS Kinesis).
3. Follower regions apply deltas in strict sequential order dictated by the monotonic `sequenceNumber` and HLC ordering, guaranteeing identical ledger balances across all global data centers.

## Consequences
- Guarantees zero lost updates and complete prevention of overdraft / double-spending.
- Robust against NTP drift, leap seconds, and physical clock skew.
- Cross-region writes incur WAN roundtrip latency if initiated outside the account's home region, which is mitigated by edge token verification and optimistic UI confirmation.
