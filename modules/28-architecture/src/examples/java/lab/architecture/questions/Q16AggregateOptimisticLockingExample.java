package lab.architecture.questions;

/**
 * Q16: Optimistic Concurrency Control in DDD Aggregates. Demonstrates version-based optimistic
 * concurrency control preventing lost updates.
 */
public class Q16AggregateOptimisticLockingExample {

    public static class VersionedAggregate {
        private final String id;
        private long version;
        private String data;

        public VersionedAggregate(String id, long version, String data) {
            this.id = id;
            this.version = version;
            this.data = data;
        }

        public void update(String newData, long expectedVersion) {
            if (this.version != expectedVersion) {
                throw new IllegalStateException(
                        "OptimisticLockingFailure: Expected version "
                                + expectedVersion
                                + " but current is "
                                + this.version);
            }
            this.data = newData;
            this.version++;
        }

        public long getVersion() {
            return version;
        }
    }

    public static void main(String[] args) {
        VersionedAggregate aggregate = new VersionedAggregate("AGG-1", 1L, "initial");

        aggregate.update("first update", 1L);
        long newVersion = aggregate.getVersion(); // 2L

        boolean conflictDetected = false;
        try {
            // Stale thread attempting update with old version 1L
            aggregate.update("concurrent update", 1L);
        } catch (IllegalStateException e) {
            conflictDetected = true; // true (prevented lost update)
        }

        System.out.println(
                "Q16 newVersion: " + newVersion + ", conflictDetected: " + conflictDetected);
    }
}
