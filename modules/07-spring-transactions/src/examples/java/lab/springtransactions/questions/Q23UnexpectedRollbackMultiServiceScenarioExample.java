package lab.springtransactions.questions;

public class Q23UnexpectedRollbackMultiServiceScenarioExample {

    record IncidentReport(
            String incidentId, String symptom, String rootCause, String remediation) {}

    public static void main(String[] args) {
        // Incident Simulation: UnexpectedRollbackException in Multi-Service Transaction Propagation
        IncidentReport report =
                new IncidentReport(
                        "INC-7099",
                        "org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only",
                        "Outer OrderService caught runtime exception from inner InventoryService (Propagation.REQUIRED) expecting to proceed, but inner failure marked global transaction rollback-only",
                        "Switched InventoryService to Propagation.REQUIRES_NEW or programmatic savepoints, or allowed exception to bubble up naturally");

        boolean throwsUnexpectedRollback = true; // true
        boolean innerMarkedRollbackOnly = true; // true

        System.out.println(
                "Incident: "
                        + report.incidentId()
                        + ", throws UnexpectedRollbackException: "
                        + (throwsUnexpectedRollback && innerMarkedRollbackOnly)
                        + ", fix: "
                        + report.remediation());
    }
}
