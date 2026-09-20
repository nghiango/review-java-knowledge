package lab.concurrency.lockordering;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe bank account encapsulating balance mutations protected by an explicit {@link
 * ReentrantLock}.
 */
public class Account implements Comparable<Account> {
    private final String accountId;
    private final ReentrantLock lock = new ReentrantLock(true); // Fair lock
    private long balanceCents;

    public Account(String accountId, long initialBalanceCents) {
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        if (initialBalanceCents < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        this.balanceCents = initialBalanceCents;
    }

    public String getAccountId() {
        return accountId;
    }

    public long getBalanceCents() {
        lock.lock();
        try {
            return balanceCents;
        } finally {
            lock.unlock();
        }
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return lock.tryLock(timeout, unit);
    }

    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public void deposit(long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.balanceCents += amountCents;
    }

    public void withdraw(long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (this.balanceCents < amountCents) {
            throw new IllegalStateException("Insufficient funds for account: " + accountId);
        }
        this.balanceCents -= amountCents;
    }

    @Override
    public int compareTo(Account other) {
        return this.accountId.compareTo(other.accountId);
    }
}
