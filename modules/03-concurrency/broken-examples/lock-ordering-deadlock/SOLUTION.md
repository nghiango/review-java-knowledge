# Solution: Lock ordering deadlock and blocking network calls in account transfers

## Annotated code

```java
public class AccountTransferService {
    private final AuditNotificationClient auditClient;

    public AccountTransferService(AuditNotificationClient auditClient) {
        this.auditClient = auditClient;
    }

    public void transfer(Account from, Account to, long amountCents) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Accounts must not be null");
        }

        // Bug: If from and to refer to the same account instance, locking twice on the same object
        // succeeds due to reentrancy, but withdrawing and depositing to self without validation is a logical error.

        // Concurrency issue: Acquiring locks based on parameter order without deterministic ordering
        // causes deadlock under concurrent opposite transfers (Thread 1: A -> B, Thread 2: B -> A).
        synchronized (from) {
            // Concurrency issue: synchronized monitors lack timeouts. When deadlock occurs,
            // blocked threads wait indefinitely with no recovery mechanism.
            synchronized (to) {
                from.withdraw(amountCents);
                to.deposit(amountCents);

                // Performance issue: Invoking a remote/slow network service while holding critical resource locks
                // extends the lock hold duration, causing thread pool starvation across the entire application.
                auditClient.sendNotification(
                        "Transferred " + amountCents + " from " + from.getAccountId() + " to " + to.getAccountId());
            }
        }
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Concurrency issue | Critical | `AccountTransferService.transfer()` | Inconsistent lock ordering causes cyclic deadlock |
| 2 | Concurrency issue | High | `AccountTransferService.transfer()` | Intrinsic `synchronized` lacks timeout or cancellation |
| 3 | Performance issue | Critical | `AccountTransferService.transfer()` | Remote network I/O executed inside lock scope |
| 4 | Edge case issue | Medium | `AccountTransferService.transfer()` | Missing check for identical source and destination accounts |

## Issue details

### Deadlock from inconsistent lock acquisition order

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

When two threads attempt to transfer funds between the same pair of accounts in opposite directions (Thread 1: Account A → Account B, Thread 2: Account B → Account A), Thread 1 locks A and waits for B, while Thread 2 locks B and waits for A. This satisfies the four Coffman conditions (mutual exclusion, hold and wait, no preemption, circular wait) resulting in a permanent deadlock.

Fix: Enforce a global canonical ordering on lock acquisition based on natural account IDs (`from.getAccountId().compareTo(to.getAccountId())`) or `System.identityHashCode`. Always lock the lower ID first, followed by the higher ID.

### Holding locks across network boundaries

**Type:** Performance issue · **Severity:** Critical · **Difficulty:** Senior

Locking is meant for microsecond-level in-memory state coordination. Executing HTTP, gRPC, JDBC, or message broker calls while holding object locks artificially inflates lock contention time from microseconds to tens or hundreds of milliseconds. Any latency spike in the external service will instantly stall all incoming transactions competing for either account.

Fix: Complete the balance mutation inside the locked section, release the locks, and execute external notifications asynchronously or outside the synchronization block.

## Correct implementation

The production-ready fix lives in `lab.concurrency.lockordering`:
- `Account.java` using `ReentrantLock` with explicit tryLock timeouts and atomic balance operations.
- `DeadlockFreeTransferService.java` implementing deterministic resource ordering (`Comparable` ID ordering / identity hash fallback) and post-transfer decoupled asynchronous auditing.
