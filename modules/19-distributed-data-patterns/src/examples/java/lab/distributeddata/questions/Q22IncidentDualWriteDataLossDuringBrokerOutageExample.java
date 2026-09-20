package lab.distributeddata.questions;

public class Q22IncidentDualWriteDataLossDuringBrokerOutageExample {

    record IncidentReport(
            int dbOrdersCommitted, int kafkaEventsDispatched, int lostFulfillmentEvents) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Post-mortem: 5,000 orders committed in Postgres while Kafka broker was down.
        // Direct dual write without outbox resulted in 5,000 orders trapped with zero fulfillment
        // events.
        IncidentReport report = new IncidentReport(5000, 0, 5000);

        boolean totalDiscrepancy = report.lostFulfillmentEvents() == 5000; // true
        System.out.println(
                "Dual write failure resulted in unfulfilled orders: " + totalDiscrepancy);
    }
}
