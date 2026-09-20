package lab.observability.questions;

public class Q23IncidentSilentFailureMdcLossExample {

    record IncidentTriage(
            String symptom, int failedTransactions, int reportedErrorsOnDashboard, String defect) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Incident: 10,000 customers report orders failing silently during checkout,
        // yet the executive Grafana error dashboard shows 0.00% error rate.
        // Root Cause: The payment execution service wrapped downstream gateway calls in try/catch,
        // caught Exception, logged e.getMessage() without stack trace, and returned boolean false
        // without incrementing the error counter metric.
        // Remediation:
        // 1. Increment symmetric 'payments.processed.total' counter with status='FAILED' and
        // exception tag.
        // 2. Propagate full Throwable stack trace in log statements.
        // 3. Propagate domain exception or structured Result to caller.
        IncidentTriage triage =
                new IncidentTriage(
                        "Customer payment failures invisible on dashboards",
                        10000,
                        0,
                        "Swallowed exceptions without metric increments or causal stack traces");

        boolean dashboardConcealedFailure =
                triage.failedTransactions() > 0 && triage.reportedErrorsOnDashboard() == 0; // true
        int actualFailures = triage.failedTransactions(); // 10000

        System.out.println("Dashboard concealed production failures: " + dashboardConcealedFailure);
        System.out.println("Undetected customer transaction failures: " + actualFailures);
    }
}
