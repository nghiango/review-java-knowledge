# Architecture Review — Wall-Clock Ordering in Multi-Region Ledger Replication

## Context

A cross-border digital wallet platform operates an active-active multi-region deployment across US-East, EU-Central, and AP-Southeast. The engineering team submitted design document `DESIGN-ledger-replication.md` proposing Last-Write-Wins (LWW) conflict resolution for account balance updates using server timestamps (`System.currentTimeMillis()`).

Review `DESIGN-ledger-replication.md` for clock synchronization assumptions, NTP drift vulnerabilities, and ledger data loss hazards.

## What to look for

- Synchronized physical wall-clock assumptions in distributed systems
- NTP drift, clock skew, and leap second step adjustments
- Last-Write-Wins (LWW) conflict resolution hazards
- Logical clocks (Lamport, Vector Clocks, Hybrid Logical Clocks) vs physical clocks
