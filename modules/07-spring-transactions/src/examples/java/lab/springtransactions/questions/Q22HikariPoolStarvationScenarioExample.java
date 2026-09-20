package lab.springtransactions.questions;

public class Q22HikariPoolStarvationScenarioExample {

    record IncidentReport(
            String incidentId, String symptom, String rootCause, String remediation) {}

    public static void main(String[] args) {
        // Incident Simulation: HikariCP Connection Pool Exhaustion from External API Latency
        IncidentReport report =
                new IncidentReport(
                        "INC-7041",
                        "HikariPool-1 - Connection is not available, request timed out after 30000ms across all microservice endpoints",
                        "Payment gateway API latency degraded to 4500ms; remote HTTP call executed inside @Transactional method holding DB connections",
                        "Extracted payment HTTP call outside @Transactional; reduced DB transaction duration from 4500ms to 4ms");

        int poolSize = 50;
        int activeConnections = 50;
        boolean isSaturated = activeConnections >= poolSize; // true

        System.out.println(
                "Incident: "
                        + report.incidentId()
                        + ", pool saturated: "
                        + isSaturated
                        + ", remediation: "
                        + report.remediation());
    }
}
