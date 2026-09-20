package lab.distributeddata.questions;

public class Q13EventSourcingVsTransactionalOutboxExample {

    record ArchitecturalPattern(
            String name,
            boolean stateIsDerivedFromEventStream,
            boolean storesCurrentMutableState) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Transactional Outbox: relational table holds current mutable state; outbox table holds
        // publication events
        ArchitecturalPattern outbox = new ArchitecturalPattern("TransactionalOutbox", false, true);

        // Event Sourcing: event log IS the primary source of truth; entity state is reconstructed
        // by replaying events
        ArchitecturalPattern eventSourcing = new ArchitecturalPattern("EventSourcing", true, false);

        boolean outboxStoresCurrentState = outbox.storesCurrentMutableState(); // true
        boolean esDerivesFromEvents = eventSourcing.stateIsDerivedFromEventStream(); // true

        System.out.println(
                "Outbox maintains relational state: "
                        + outboxStoresCurrentState
                        + ", Event Sourcing rebuilds state from stream: "
                        + esDerivesFromEvents);
    }
}
