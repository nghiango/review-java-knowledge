package lab.java25boot4.springtransactions.questions;

public class Q13ScenarioHikariPoolExhaustionVirtualThreadsExample {

    public static void main(String[] args) {
        // Incident Scenario: 10,000 virtual threads spawned to process web requests.
        // Each opens a @Transactional method that blocks on a remote HTTP call.
        // The HikariCP connection pool (size 20) exhausts within 50ms, causing cascading timeouts.
        int activeVirtualThreads = 10_000;
        int maxHikariPool = 20;

        boolean poolExhaustionRisk = activeVirtualThreads > maxHikariPool;
        System.out.println(
                "Pool exhaustion risk: " + poolExhaustionRisk); // Pool exhaustion risk: true
        System.out.println("Mitigation: Move remote HTTP calls outside @Transactional boundary");
    }
}
