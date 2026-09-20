# Spring Transactions Exercises

Hands-on exercises to practice Spring transaction boundaries, programmatic savepoints, and after-commit synchronizations.

## Exercise 1: Build a Custom Transaction Synchronization for Auditing

Implement a `TransactionSynchronization` that records audit logs only when the active Spring transaction successfully commits, and discards them if rolled back.

### Requirements
- Check `TransactionSynchronizationManager.isActualTransactionActive()`.
- Register an anonymous `TransactionSynchronization` implementation.
- Override `afterCommit()` to write the audit trail to an external audit logger.

??? question "Reveal solution"
    ```java
    @Service
    public class AuditSynchronizationService {

        private final AuditLogClient auditLogClient;

        public AuditSynchronizationService(AuditLogClient auditLogClient) {
            this.auditLogClient = auditLogClient;
        }

        public void registerAuditOnCommit(String action, String entityId) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                auditLogClient.sendAudit(action, entityId);
                            }
                        });
            } else {
                auditLogClient.sendAudit(action, entityId);
            }
        }
    }
    ```

---

## Exercise 2: Implement Programmatic Batch Processing with Savepoints

Implement a batch processing service using `TransactionTemplate` and `TransactionStatus.createSavepoint()` to process a list of records. If an individual record fails, roll back to its savepoint while allowing preceding and subsequent items in the batch to commit.

### Requirements
- Execute within `TransactionTemplate.execute()`.
- Create a savepoint before processing each batch item.
- If processing fails, call `status.rollbackToSavepoint(savepoint)`.

??? question "Reveal solution"
    ```java
    @Service
    public class BatchRecordProcessor {

        private final TransactionTemplate transactionTemplate;
        private final RecordRepository recordRepository;

        public BatchRecordProcessor(
                PlatformTransactionManager transactionManager,
                RecordRepository recordRepository) {
            this.transactionTemplate = new TransactionTemplate(transactionManager);
            this.recordRepository = recordRepository;
        }

        public int processBatch(List<BatchItem> items) {
            return transactionTemplate.execute(
                    status -> {
                        int successCount = 0;
                        for (BatchItem item : items) {
                            Object savepoint = status.createSavepoint();
                            try {
                                recordRepository.save(item);
                                status.releaseSavepoint(savepoint);
                                successCount++;
                            } catch (Exception e) {
                                status.rollbackToSavepoint(savepoint);
                            }
                        }
                        return successCount;
                    });
        }
    }
    ```
