# Distributed Data in Production

Operating distributed data pipelines in high-throughput production environments requires strict telemetry, table bloat prevention, lag alerting, and catastrophe post-mortems.

## 1. Incident Walkthrough: The Broken Dual Write Outage

### Incident Profile
- **Severity**: P1 Data Inconsistency (Trapped Orders)
- **Duration**: 4 hours
- **Impact**: 14,200 orders paid by credit card but trapped in the database without warehouse fulfillment.

### The Trigger & Cascading Failure

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant CheckoutService as Checkout Service
    participant Postgres as PostgreSQL DB
    participant Kafka as Apache Kafka Broker
    participant Warehouse as Warehouse Fulfillment Service

    Customer->>CheckoutService: POST /orders
    CheckoutService->>Postgres: INSERT INTO orders (Status: CREATED)
    Postgres-->>CheckoutService: Transaction Committed
    Note over Kafka: Rolling broker restart causes 15s TCP disconnect
    CheckoutService->>Kafka: kafkaTemplate.send("orders.events")
    Kafka--xCheckoutService: TimeoutException (Broker Unreachable)
    Note over CheckoutService: Exception logged, order status remains CREATED
    Note over Customer: Credit card was charged, but order has no events
    Note over Warehouse: Warehouse receives ZERO events; orders sit idle for days!
```

### Root Cause Analysis
1. **Direct Dual Write**: The application saved the order to PostgreSQL and attempted to publish directly to Kafka in the same flow without an outbox table.
2. **Asymmetric Failure**: The database transaction had already committed before the Kafka network call failed. Rolling back the method threw an error to the user, but the row persisted in PostgreSQL.
3. **Missing Reconciliation**: No background job existed to detect database orders lacking corresponding event publications.

### Remediation & Post-Mortem Actions
- Migrated all event publications to the **Transactional Outbox Pattern**: orders and outbox records commit in the same local database transaction.
- Provisioned a Debezium CDC pipeline reading PostgreSQL WAL directly to stream outbox records to Kafka with sub-millisecond latency.
- Developed an automated backfill reconciliation script that detected unfulfilled orders and re-emitted events to Kafka.

---

## 2. Outbox Lag Monitoring & Alerting

Monitor the health of outbox publishers and CDC replication slots with Prometheus:

### Key Metrics
- `outbox_pending_events_count`: Gauge tracking total unread rows in `outbox_events` where `status = 'PENDING'`.
- `outbox_oldest_event_age_seconds`: Age in seconds of the oldest unconsumed outbox record.
- `pg_replication_slots_confirmed_flush_lsn`: Tracks replication slot lag in bytes for Debezium CDC.

### Critical Prometheus Alerts

```yaml
groups:
  - name: outbox-pipeline-alerts
    rules:
      - alert: OutboxRelayStalled
        expr: outbox_oldest_event_age_seconds > 120
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Outbox publisher relay is stalled"
          description: "Oldest pending outbox event is older than 2 minutes. Check database locks or Kafka broker health."

      - alert: HighOutboxLag
        expr: outbox_pending_events_count > 5000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High outbox pending backlog"
          description: "More than 5,000 outbox events waiting to be published. Consider scaling outbox polling workers."
```

---

## 3. Production Readiness Checklist

Before deploying distributed data pipelines to production, verify:

- [ ] **No Direct Dual Writes**: Every event publication originating from a database mutation uses the Transactional Outbox pattern.
- [ ] **Single Local Transaction**: Business entity mutation and outbox event insertion share the exact same relational transaction.
- [ ] **SKIP LOCKED Enabled**: Polling outbox workers use `FOR UPDATE SKIP LOCKED` to allow multiple concurrent pollers without lock contention.
- [ ] **Broker Delivery Verification**: Outbox publishers verify broker `RecordMetadata` before marking events as `PROCESSED`.
- [ ] **Idempotent Consumers**: Consumers implement atomic inbox deduplication via `INSERT ... ON CONFLICT DO NOTHING`.
- [ ] **Idempotent Saga Compensations**: All Saga rollback actions check historical refund/cancellation ledgers before modifying balances.
- [ ] **Outbox Table Partitioning**: High-throughput outbox tables are partitioned by date to avoid PostgreSQL MVCC dead-tuple bloat.
- [ ] **Replication Slot Monitoring**: CDC replication slot byte lag is monitored and alerted to prevent database disk saturation.
