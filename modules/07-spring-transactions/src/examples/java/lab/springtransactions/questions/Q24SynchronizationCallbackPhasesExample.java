package lab.springtransactions.questions;

import org.springframework.transaction.support.TransactionSynchronization;

@SuppressWarnings("unused")
public final class Q24SynchronizationCallbackPhasesExample {
    private Q24SynchronizationCallbackPhasesExample() {}

    public static class AuditLogSynchronization implements TransactionSynchronization {
        private String eventStatus = "PENDING";

        @Override
        public void beforeCommit(boolean readOnly) {
            // Invoked before SQL COMMIT; any exception thrown here causes the transaction to roll
            // back!
        }

        @Override
        public void afterCommit() {
            // Invoked strictly AFTER the underlying database transaction commits successfully.
            // Ideal for non-transactional side-effects (e.g. sending emails, enqueuing message
            // broker tasks).
            this.eventStatus = "COMMITTED";
        }

        @Override
        public void afterCompletion(int status) {
            // Invoked after commit or rollback (status == STATUS_COMMITTED or STATUS_ROLLED_BACK).
            // Essential for resource cleanup, clearing ThreadLocals, or MDC context.
            if (status == STATUS_ROLLED_BACK) {
                this.eventStatus = "ROLLED_BACK";
            }
        }

        public String getEventStatus() {
            return eventStatus;
        }
    }

    public static void main(String[] args) {
        AuditLogSynchronization sync = new AuditLogSynchronization();
        sync.afterCommit();
        sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        String status = sync.getEventStatus(); // "COMMITTED"
        boolean isCommitted = "COMMITTED".equals(status); // true
    }
}
