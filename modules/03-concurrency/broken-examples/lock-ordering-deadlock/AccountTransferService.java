package lab.concurrency.broken.lockordering;

public class AccountTransferService {
    private final AuditNotificationClient auditClient;

    public AccountTransferService(AuditNotificationClient auditClient) {
        this.auditClient = auditClient;
    }

    public void transfer(Account from, Account to, long amountCents) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Accounts must not be null");
        }

        // Lock from account first, then lock to account
        synchronized (from) {
            synchronized (to) {
                from.withdraw(amountCents);
                to.deposit(amountCents);

                // Call external notification service while holding both locks
                auditClient.sendNotification(
                        "Transferred " + amountCents + " from " + from.getAccountId() + " to " + to.getAccountId());
            }
        }
    }
}
