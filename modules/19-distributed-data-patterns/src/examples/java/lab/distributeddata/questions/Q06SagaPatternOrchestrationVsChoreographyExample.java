package lab.distributeddata.questions;

public class Q06SagaPatternOrchestrationVsChoreographyExample {

    record SagaArchitecture(
            String style, boolean hasCentralCoordinator, boolean eventDrivenCoupling) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Orchestration: Central state machine coordinates step commands and compensation
        SagaArchitecture orchestration = new SagaArchitecture("Orchestration", true, false);

        // Choreography: Microservices react to domain events and emit follow-on events
        SagaArchitecture choreography = new SagaArchitecture("Choreography", false, true);

        boolean orchestrationHasBrain = orchestration.hasCentralCoordinator(); // true
        boolean choreographyIsDecentralized = !choreography.hasCentralCoordinator(); // true

        System.out.println(
                "Orchestration uses central coordinator: "
                        + orchestrationHasBrain
                        + ", Choreography is decentralized: "
                        + choreographyIsDecentralized);
    }
}
