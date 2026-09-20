package lab.distributeddata.questions;

public class Q11OutboxPollingPublisherSkipLockedQueryExample {

    record SqlQueryPattern(
            String sqlClause, boolean enablesConcurrentPollersWithoutLockCollisions) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // FOR UPDATE SKIP LOCKED skips rows already locked by other transactions,
        // allowing multiple worker pods to poll the outbox table concurrently without deadlocks.
        SqlQueryPattern pattern = new SqlQueryPattern("FOR UPDATE SKIP LOCKED", true);

        boolean concurrencyFriendly =
                pattern.enablesConcurrentPollersWithoutLockCollisions(); // true
        System.out.println(
                "SKIP LOCKED eliminates contention across parallel outbox pollers: "
                        + concurrencyFriendly);
    }
}
