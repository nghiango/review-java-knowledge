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

## Assumptions
- AWS / GCP cloud host VMs run standard NTP (Network Time Protocol) daemons, maintaining server clock synchronization within 5–10 milliseconds.
- Physical server timestamps provide a strict total ordering of financial transactions across regions.
