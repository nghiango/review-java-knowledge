package lab.distributeddata.questions;

public class Q16ListenToYourselfPatternExample {

    record SelfEventContext(String serviceName, boolean updatesLocalStateOnlyOnEventConsumption) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // "Listen to Yourself": The service publishing the command DOES NOT mutate its internal
        // state directly;
        // it publishes the intent to the broker, and updates its local state ONLY when it consumes
        // its own event.
        SelfEventContext context = new SelfEventContext("OrderService", true);

        boolean decoupledStateMutation = context.updatesLocalStateOnlyOnEventConsumption(); // true
        System.out.println(
                "Listen to yourself decouples command submission from state change: "
                        + decoupledStateMutation);
    }
}
