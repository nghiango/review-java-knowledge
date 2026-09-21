package lab.architecture.questions;

import java.util.List;

/**
 * Q19: Event Sourcing Schema Evolution and Snapshotting Strategies.
 * Demonstrates optimizing state rehydration using a point-in-time snapshot.
 */
public class Q19EventSourcingSnapshottingExample {

    public record Snapshot(int version, int balance) {}
    public record DeltaEvent(int amount) {}

    public static class SnapshotRehydrator {
        public static int rehydrate(Snapshot snapshot, List<DeltaEvent> subsequentEvents) {
            int currentBalance = snapshot.balance();
            for (DeltaEvent event : subsequentEvents) {
                currentBalance += event.amount();
            }
            return currentBalance;
        }
    }

    public static void main(String[] args) {
        // Snapshot taken at version 1000 with balance 5000
        Snapshot snapshot = new Snapshot(1000, 5000);
        // Only replay events since snapshot (events 1001 to 1003)
        List<DeltaEvent> delta = List.of(new DeltaEvent(100), new DeltaEvent(-50), new DeltaEvent(200));

        int finalBalance = SnapshotRehydrator.rehydrate(snapshot, delta); // 5250 (avoided replaying 1000 events)
        boolean valid = (finalBalance == 5250); // true

        System.out.println("Q19 finalBalance: " + finalBalance + ", valid: " + valid);
    }
}
