# Performance in Production

## Diagnostic sequence

1. Confirm the user symptom with rate, errors, p50, p95, p99, and timeout rate.
2. Find saturation: CPU, run queue, heap/GC, executor queue, Hikari pending, DB locks, or downstream
   concurrency.
3. Capture a time-bounded JFR during representative load.
4. Correlate hot methods, allocation sites, locks, and socket/file waits with traces.
5. Reproduce with a finite load test and one controlled change.
6. Verify the objective and check that errors or another resource did not worsen.

## JFR recipes

```bash
java -XX:StartFlightRecording=filename=performance.jfr,settings=profile,duration=60s \
  -jar application.jar
jcmd <pid> JFR.start name=incident settings=profile duration=60s filename=incident.jfr
```

Record environment, commit, JVM flags, request mix, payload sizes, arrival model, warmup, and test
duration. Keep dangerous or high-load experiments away from shared production dependencies.

## Load models

- **Open model:** arrivals continue at the target rate and expose queueing or rejection.
- **Closed model:** a fixed user population waits for responses and can hide coordinated omission.
- **Soak:** reveals leaks, promotion, cache growth, and thermal throttling.
- **Spike:** validates overload policy and recovery.

## Hikari exhaustion signal

`active == maximum`, `idle == 0`, and rising `pending` confirms pool saturation. Low DB CPU narrows
the hypotheses toward long transactions, external calls inside resource scopes, or lock waits.
Increasing the pool before finding the holder can push the bottleneck into PostgreSQL.

## Incident walkthrough

**Symptoms:** checkout p95 rises from 180 ms to 8 seconds while throughput stops growing.  
**Metrics:** Hikari active 20, idle 0, pending 75; PostgreSQL CPU 20%; payment p95 2 seconds.  
**Logs:** connection acquisition timeouts appear after payment latency rises.  
**Initial hypotheses:** slow SQL, database locks, a connection leak, or remote I/O inside a
connection scope.

??? note "Root cause and recovery"

    **Root cause:** checkout holds its JDBC connection while waiting for payment. The database is
    idle because checked-out connections are waiting on another system.

    **Investigation:** capture pending-thread stacks, transaction spans, Hikari metrics, payment
    latency, and a bounded JFR. Confirm that stacks holding connections wait in the gateway.

    **Fix:** persist pending state in a short transaction, release the connection, call payment with
    an idempotency key, then finalize in another short transaction. Add reconciliation.

    **Verification:** repeat the same open-model load. Pending stays near zero, acquisition time
    remains below its budget, and p99 recovers without raising the pool cap.

    **Prevention:** review resource scopes, budget connections across replicas, and alert on pending
    callers plus transaction age.

## Related

- [Observability production guide](../observability/production.md)
- [JVM production guide](../jvm/production.md)
- [Code review](code-review.md)
