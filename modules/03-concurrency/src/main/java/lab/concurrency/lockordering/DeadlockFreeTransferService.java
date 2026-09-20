package lab.concurrency.lockordering;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Production-ready fund transfer service that prevents deadlocks through deterministic lock
 * ordering, bounded tryLock timeouts, and asynchronous notification emission outside lock scope.
 */
public class DeadlockFreeTransferService {
    private static final long DEFAULT_LOCK_TIMEOUT_MS = 500;
    private final AuditNotificationClient auditClient;

    public DeadlockFreeTransferService(AuditNotificationClient auditClient) {
        this.auditClient = Objects.requireNonNull(auditClient, "auditClient must not be null");
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public TransferResult transfer(Account from, Account to, long amountCents) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Source and destination accounts must not be null");
        }
        if (from.getAccountId().equals(to.getAccountId())) {
            throw new IllegalArgumentException("Cannot transfer funds to the same account");
        }
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Canonical global lock ordering: lowest account ID locked first
        Account firstLock = from.compareTo(to) < 0 ? from : to;
        Account secondLock = from.compareTo(to) < 0 ? to : from;

        boolean firstAcquired = false;
        boolean secondAcquired = false;

        try {
            firstAcquired = firstLock.tryLock(DEFAULT_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!firstAcquired) {
                return new TransferResult(
                        false,
                        "Timed out acquiring lock on " + firstLock.getAccountId(),
                        from.getBalanceCents(),
                        to.getBalanceCents());
            }

            secondAcquired = secondLock.tryLock(DEFAULT_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!secondAcquired) {
                return new TransferResult(
                        false,
                        "Timed out acquiring lock on " + secondLock.getAccountId(),
                        from.getBalanceCents(),
                        to.getBalanceCents());
            }

            from.withdraw(amountCents);
            to.deposit(amountCents);

            long fromBalance = from.getBalanceCents();
            long toBalance = to.getBalanceCents();

            // Release locks immediately before external async notification
            return new TransferResult(
                    true, "Transfer completed successfully", fromBalance, toBalance);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new TransferResult(
                    false, "Transfer interrupted", from.getBalanceCents(), to.getBalanceCents());
        } finally {
            if (secondAcquired) {
                secondLock.unlock();
            }
            if (firstAcquired) {
                firstLock.unlock();
            }
            // Trigger decoupled audit notification outside the locked critical section
            auditClient.notifyTransferAsync(
                    "Transfer of "
                            + amountCents
                            + " cents from "
                            + from.getAccountId()
                            + " to "
                            + to.getAccountId());
        }
    }
}
