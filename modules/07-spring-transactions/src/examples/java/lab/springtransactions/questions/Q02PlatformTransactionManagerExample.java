package lab.springtransactions.questions;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class Q02PlatformTransactionManagerExample {

    public static void main(String[] args) {
        // TransactionDefinition encapsulates propagation, isolation, timeout, and read-only flags
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        def.setTimeout(30);
        def.setReadOnly(false);

        boolean isRequired =
                def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED; // true
        boolean isReadCommitted =
                def.getIsolationLevel() == TransactionDefinition.ISOLATION_READ_COMMITTED; // true
        int timeoutSeconds = def.getTimeout(); // 30

        System.out.println(
                "Required: "
                        + isRequired
                        + ", ReadCommitted: "
                        + isReadCommitted
                        + ", Timeout: "
                        + timeoutSeconds);
    }
}
