package lab.springmvc.questions;

public class Q22TomcatThreadStarvationScenarioExample {

    record IncidentReport(
            String incidentId, String symptom, String rootCause, String remediation) {}

    public static void main(String[] args) {
        // Incident Simulation: HTTP Thread Pool Exhaustion from Synchronous Blocking Calls
        IncidentReport report =
                new IncidentReport(
                        "INC-5021",
                        "Tomcat HTTP thread pool (200 threads) saturated at 100% with connection timeouts on health checks",
                        "Synchronous downstream legacy reporting endpoint blocked Tomcat worker threads for 25s without timeout",
                        "Migrated to DeferredResult / CompletableFuture with dedicated bounded thread pool and 5s timeout");

        int activeThreads = 200;
        boolean isSaturated = activeThreads >= 200; // true

        System.out.println(
                "Incident: "
                        + report.incidentId()
                        + ", saturated: "
                        + isSaturated
                        + ", fix: "
                        + report.remediation());
    }
}
