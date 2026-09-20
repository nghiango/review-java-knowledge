package lab.distributeddata.questions;

public class Q04ChangeDataCaptureDebeziumVsPollingOutboxExample {

    record RelayMechanism(String type, boolean readsWalLog, double latencyMs) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Debezium CDC reads the database Write-Ahead Log (WAL / pg_output / binlog) directly
        RelayMechanism cdc = new RelayMechanism("Debezium", true, 5.0);

        // Polling Outbox queries the outbox table via SQL SELECT FOR UPDATE SKIP LOCKED
        RelayMechanism polling = new RelayMechanism("PollingWorker", false, 500.0);

        boolean cdcZeroTableLocking = cdc.readsWalLog(); // true
        boolean pollingHasPeriodicLatency = !polling.readsWalLog(); // true

        System.out.println(
                "CDC reads WAL directly: "
                        + cdcZeroTableLocking
                        + ", Polling uses periodic queries: "
                        + pollingHasPeriodicLatency);
    }
}
