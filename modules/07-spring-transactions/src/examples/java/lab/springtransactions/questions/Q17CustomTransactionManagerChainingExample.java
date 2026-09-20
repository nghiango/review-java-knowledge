package lab.springtransactions.questions;

import java.util.List;

public class Q17CustomTransactionManagerChainingExample {

    record TxManagerChain(List<String> managers, boolean isStrict2Pc) {}

    public static void main(String[] args) {
        // ChainedTransactionManager executes best-effort 1PC commits across multiple resources.
        // It is NOT genuine XA/2PC; if the 2nd commit fails, the 1st cannot be rolled back!
        TxManagerChain chain =
                new TxManagerChain(List.of("postgresTxManager", "jmsTxManager"), false);

        int totalManagers = chain.managers().size(); // 2
        boolean isStrictXa =
                chain.isStrict2Pc(); // false (Best-effort 1PC, prone to partial commits)

        System.out.println("Chained managers: " + totalManagers + ", strict 2PC: " + isStrictXa);
    }
}
