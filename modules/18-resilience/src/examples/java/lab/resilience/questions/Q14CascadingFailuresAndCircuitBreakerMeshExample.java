package lab.resilience.questions;

public class Q14CascadingFailuresAndCircuitBreakerMeshExample {

    record ServiceNode(String name, boolean circuitOpen, boolean isCascadingBlocked) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Upstream Service A calls Downstream Service B. Service B is completely down.
        // With CircuitBreaker OPEN on Service A -> B, Service A fast-fails B's calls in 0ms.
        ServiceNode nodeA = new ServiceNode("OrderService", true, true);

        boolean protectsTomcatThreads = nodeA.circuitOpen(); // true
        boolean preventsCascadingExhaustion = nodeA.isCascadingBlocked(); // true

        System.out.println("Cascading failure prevented: " + preventsCascadingExhaustion);
    }
}
