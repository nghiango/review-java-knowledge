package lab.distributeddata.questions;

public class Q23IncidentSagaCompensationLoopDoubleRefundExample {

    record IncidentReport(
            int initialTransactions,
            int redeliveredEvents,
            double originalRefund,
            double actualRefunded) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Post-mortem: Consumer offset commit timed out after applying refund.
        // Redelivered compensation event refunded customer account a second time.
        IncidentReport incident = new IncidentReport(100, 100, 5000.0, 10000.0);

        boolean financialLoss = incident.actualRefunded() > incident.originalRefund(); // true
        System.out.println("Non-idempotent compensation caused 200% payout: " + financialLoss);
    }
}
