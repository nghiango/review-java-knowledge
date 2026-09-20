# Code Review — Non-Idempotent Consumer Processing

## Context

A core banking credit service consumes credit instructions from Kafka topic `account-credits`. For each message received, it adds funds to the target user account balance and appends an entry to the audit log.

Review `AccountCreditConsumer.java` for data integrity risks, duplicate processing issues, and at-least-once delivery implications.

## What to look for

- How duplicate messages (due to consumer rebalance, network timeouts, or producer retries) affect state
- Protection against replaying already processed messages
- Deduplication mechanisms and idempotency guarantees
- Atomicity between the business mutation and deduplication check
