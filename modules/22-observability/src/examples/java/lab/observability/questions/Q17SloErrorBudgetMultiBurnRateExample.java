package lab.observability.questions;

public class Q17SloErrorBudgetMultiBurnRateExample {

    record BurnRateAlert(
            String window,
            double burnRateMultiplier,
            double budgetConsumedPct,
            int alertSeverityMinutes) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Google SRE Multiwindow, Multi-Burn-Rate Alerting:
        // - SLO: 99.9% availability over 30 days -> Error Budget = 0.1% (43.2 minutes
        // downtime/month).
        // - 14.4x burn rate: Consumes 100% of error budget in 2 days (Page on-call within 1 hour).
        // - 6x burn rate: Consumes 100% of budget in 5 days (Page within 6 hours).
        // - 1x burn rate: Consumes 100% of budget in 30 days (Ticket alert; no page).
        BurnRateAlert criticalPage = new BurnRateAlert("1 hour", 14.4, 2.0, 12);
        BurnRateAlert slowBurnTicket = new BurnRateAlert("3 days", 1.0, 10.0, 360);

        boolean burnsFast = criticalPage.burnRateMultiplier() > 10.0; // true
        boolean triggersPage = criticalPage.alertSeverityMinutes() <= 15; // true

        System.out.println("Fast burn rate multiplier: " + criticalPage.burnRateMultiplier());
        System.out.println("Triggers immediate page: " + triggersPage);
    }
}
