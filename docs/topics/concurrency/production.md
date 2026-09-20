# Concurrency in Production

## 1. Deadlock Diagnosis with Thread Dumps

When deadlocks occur, JVM threads remain in `BLOCKED` or `WAITING` state while CPU utilization drops to near zero.

### Capturing Thread Dumps

```bash
# Capture full JVM thread dump
jcmd <PID> Thread.print > threaddump.txt

# Or using jstack with lock inspection
jstack -l <PID> > threaddump.txt
```

### Analyzing Deadlock in Thread Dumps

HotSpot's thread dump engine automatically detects cyclic monitor deadlocks at the end of the dump:

```text
Found one Java-level deadlock:
=============================
"pool-1-thread-1":
  waiting to lock monitor 0x00007f9e4c004200 (object 0x0000000715b10020, a lab.concurrency.Account),
  which is held by "pool-1-thread-2"
"pool-1-thread-2":
  waiting to lock monitor 0x00007f9e4c004400 (object 0x0000000715b10010, a lab.concurrency.Account),
  which is held by "pool-1-thread-1"

Java stack information for the threads listed above:
===================================================
"pool-1-thread-1":
        at lab.concurrency.broken.lockordering.AccountTransferService.transfer(AccountTransferService.java:18)
        - waiting to lock <0x0000000715b10020> (a lab.concurrency.Account)
        - locked <0x0000000715b10010> (a lab.concurrency.Account)
```

## 2. Diagnosing Virtual Thread Carrier Pinning

In Java 21, executing blocking I/O inside a `synchronized` block pins the underlying OS carrier thread, preventing the JVM from unmounting the virtual thread.

### Enabling Pinning Trace Logging

Start the JVM with the system property:

```bash
java -Djdk.tracePinnedThreads=full -jar app.jar
```

When a virtual thread is pinned while parking, HotSpot prints the exact offending stack trace:

```text
Thread[#42,ForkJoinPool-1-worker-3,5,CarrierThreads]
    java.base/java.lang.VirtualThread$VThreadContinuation.onPinned(VirtualThread.java:180)
    java.base/jdk.internal.vm.Continuation.onPinned0(Native Method)
    java.base/java.lang.VirtualThread.park(VirtualThread.java:582)
    java.base/java.lang.System$LoggerFinder.getLogger(System.java:1234)
    lab.concurrency.LegacySyncService.process(LegacySyncService.java:45) <== PINNED HERE ON SYNCHRONIZED
```

### Java Flight Recorder (JFR) Event

Record pinning events in production with minimal overhead:

```bash
jcmd <PID> JFR.start name=pinning settings=profile duration=60s filename=pinning.jfr
```
Inspect events with JDK Mission Control or `jfr print --events jdk.VirtualThreadPinned pinning.jfr`.

## 3. Thread Pool Starvation & Queue Monitoring

### Metrics to Alert On (Micrometer / Actuator)

| Metric Name | Threshold / Signal | Root Cause |
|---|---|---|
| `executor.active` == `executor.pool.max` | Saturated workers | Slow downstream calls or undersized pool |
| `executor.queue.remaining` == `0` | Full queue | Producer submitting faster than workers can process |
| `executor.rejected` > `0` | Tasks being rejected or dropped | Capacity ceiling reached |

## 4. Production Concurrency Checklist

- [ ] All mutable state accessed across threads is guarded by atomic variables, explicit locks, or concurrent collections.
- [ ] No unbounded thread pools (`Executors.newCachedThreadPool()`) exist in production services.
- [ ] Every `BlockingQueue` has an explicit capacity limit to avoid memory leaks.
- [ ] Thread pools define descriptive names via `ThreadFactory` and register uncaught exception handlers.
- [ ] Multiple lock acquisitions follow a strict, global deterministic order to avoid deadlocks.
- [ ] Locks are never held across external network, database, or disk I/O operations.
- [ ] `CompletableFuture` asynchronous pipelines specify a dedicated I/O `ExecutorService` and explicit timeouts.
- [ ] Virtual thread applications avoid `synchronized` on blocking paths, preferring `ReentrantLock`.

## Related

- [Concepts](concepts.md)
- [Questions](questions.md)
- [Solutions](solutions.md)
