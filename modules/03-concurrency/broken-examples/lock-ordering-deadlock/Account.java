package lab.concurrency.broken.lockordering;

public class Account {
    private final String accountId;
    private long balanceCents;

    public Account(String accountId, long initialBalanceCents) {
        this.accountId = accountId;
        this.balanceCents = initialBalanceCents;
    }

    public String getAccountId() {
        return accountId;
    }

    public long getBalanceCents() {
        return balanceCents;
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
            throw new IllegalStateException("Insufficient funds for account " + accountId);
        }
        this.balanceCents -= amountCents;
    }
}
