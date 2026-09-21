# Core Java Delta — Production

!!! info "Delta from baseline"
    Unchanged: heap, threads, GC and latency diagnostics →
    [baseline Core Java production](../../../topics/core-java/production.md)
    Changed: pinning is no longer a valid explanation for carrier starvation
    New: lock leaks introduced by obsolete workarounds, silent truncation from primitive patterns

## Symptoms and causes

| Symptom | Likely cause | First check |
|---|---|---|
| Requests stall, CPU **low**, error rate flat | Threads parked on one lock | Thread dump: many callers on the same monitor |
| Requests stall, CPU **high** | Genuine saturation, not locking | CPU profile, GC log |
| Carrier starvation after moving to virtual threads | A blocking call outside `synchronized` — *not* pinning | JFR virtual-thread events; re-check the Java 21 assumption |
| Wrong metric values, no errors | Unchecked narrowing after a primitive pattern | Trace the value through the conversion |
| Missing request context in logs | `ThreadLocal` not cleared on a reused carrier | Correlation-id coverage per request |

!!! danger "The expensive wrong fix"
    "Carrier starvation → add `ReentrantLock`" was correct on Java 21 and is wrong on Java 25. It
    trades a performance risk for an outage risk: a lock acquired without `finally` is never
    released. Confirm the JDK version before applying the old remedy.

## Diagnostics

### Thread dump

```bash
jcmd <pid> Thread.dump_to_file -format=json /tmp/dump.json
jstack <pid> | grep -A 20 "parking to wait for"
```

Look for **many** threads parked on the **same** lock object. One thread that will never release it
plus a growing queue of waiters is a leak, not contention.

### JFR

```bash
jcmd <pid> JFR.start name=vt settings=profile duration=60s filename=/tmp/vt.jfr
jcmd <pid> JFR.dump name=vt filename=/tmp/vt.jfr
```

Inspect virtual-thread park/unpark and monitor-blocked events. On Java 24+ a monitor block should
**not** be accompanied by a pinned carrier.

### GC and runtime

```bash
java -XX:+PrintFlagsFinal -version | grep -E "UseZGC|ZGenerational"
jcmd <pid> GC.heap_info
```

Generational ZGC is the default mode on Java 25; flags inherited from a Java 21 deployment may now
be redundant or conflicting.

## Production checklist

- No lock exists only to avoid carrier pinning.
- Every `ReentrantLock` releases in `finally`, or has been replaced by `synchronized`.
- Alerts exist for "many threads parked on one lock" and for correlation-id gaps.
- Primitive-pattern conversions either guard the range or throw.
- Preview features are isolated and re-verified on every JDK upgrade.
- Request context uses scoped values; no mutable `ThreadLocal` survives a request.

## Related

- [Migration guide](../migration.md)
- [Concepts](concepts.md)
- [Baseline Core Java production](../../../topics/core-java/production.md)
- [Baseline Concurrency production](../../../topics/concurrency/production.md)
