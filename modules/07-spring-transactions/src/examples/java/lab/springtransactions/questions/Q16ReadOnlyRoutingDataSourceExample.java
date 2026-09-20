package lab.springtransactions.questions;

public class Q16ReadOnlyRoutingDataSourceExample {

    record DataSourceTarget(String name, boolean isReplica) {}

    public static void main(String[] args) {
        // AbstractRoutingDataSource inspects
        // TransactionSynchronizationManager.isCurrentTransactionReadOnly()
        // to dynamically route reads to read-replicas and writes to master.
        boolean isReadOnly = false;
        DataSourceTarget target =
                isReadOnly
                        ? new DataSourceTarget("replica-ds", true)
                        : new DataSourceTarget("master-ds", false);

        boolean routesToMaster = "master-ds".equals(target.name()); // true
        boolean isMasterWrite = !target.isReplica(); // true

        System.out.println(
                "Target DataSource: "
                        + target.name()
                        + ", is replica: "
                        + !isMasterWrite
                        + ", routing logic: "
                        + routesToMaster);
    }
}
