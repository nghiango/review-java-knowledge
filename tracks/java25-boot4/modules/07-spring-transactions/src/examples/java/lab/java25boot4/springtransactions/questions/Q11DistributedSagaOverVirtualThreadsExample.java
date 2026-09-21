package lab.java25boot4.springtransactions.questions;

public class Q11DistributedSagaOverVirtualThreadsExample {

    public static void main(String[] args) {
        // Across distributed services or long-running virtual tasks,
        // use Saga patterns (orchestration with compensating transactions) instead of long-held DB
        // transactions.
        boolean useSagaOverLongTransactions = true;
        System.out.println(
                "Use Saga for distributed workflows: "
                        + useSagaOverLongTransactions); // Use Saga for distributed workflows: true
    }
}
