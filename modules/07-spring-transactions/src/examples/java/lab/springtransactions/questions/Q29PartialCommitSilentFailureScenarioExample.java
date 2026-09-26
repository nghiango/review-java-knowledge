package lab.springtransactions.questions;

@SuppressWarnings("unused")
public final class Q29PartialCommitSilentFailureScenarioExample {
    private Q29PartialCommitSilentFailureScenarioExample() {}

    public static class TransactionStatusSimulator {
        private boolean rollbackOnly = false;
        private boolean committed = false;

        public void setRollbackOnly() {
            this.rollbackOnly = true;
        }

        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        public void commit() {
            if (rollbackOnly) {
                // If a participating sub-transaction marked the physical transaction as
                // rollbackOnly,
                // and the outer caller tries to commit, Spring throws UnexpectedRollbackException!
                throw new RuntimeException(
                        "UnexpectedRollbackException: Transaction marked as rollbackOnly");
            }
            this.committed = true;
        }

        public boolean isCommitted() {
            return committed;
        }
    }

    public static void main(String[] args) {
        TransactionStatusSimulator tx = new TransactionStatusSimulator();

        // Inner service fails with RuntimeException and marks transaction as rollbackOnly:
        tx.setRollbackOnly();

        // Flawed outer service catches the exception and attempts to commit anyway:
        boolean failedAsExpected = false;
        try {
            tx.commit();
        } catch (RuntimeException e) {
            // Catches UnexpectedRollbackException!
            failedAsExpected = true;
        }

        boolean caughtRollbackException = failedAsExpected; // true
    }
}
