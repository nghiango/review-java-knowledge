package lab.concurrency.lockordering;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("FutureReturnValueIgnored")
class DeadlockFreeTransferServiceTest {

    @Test
    @DisplayName(
            "bidirectional concurrent transfers complete without deadlocking and preserve total balance")
    void bidirectionalTransfers_completeWithoutDeadlock() throws InterruptedException {
        ExecutorService asyncAuditPool = Executors.newSingleThreadExecutor();
        AuditNotificationClient auditClient = new AuditNotificationClient(asyncAuditPool);
        DeadlockFreeTransferService service = new DeadlockFreeTransferService(auditClient);

        Account accountA = new Account("ACC-001", 100_000);
        Account accountB = new Account("ACC-002", 100_000);
        long initialTotalBalance = accountA.getBalanceCents() + accountB.getBalanceCents();

        int threadCount = 10;
        int transfersPerThread = 200;
        long transferAmount = 50;

        ExecutorService transferExecutor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final boolean forward = (i % 2 == 0);
            transferExecutor.submit(
                    () -> {
                        try {
                            startGate.await();
                            for (int j = 0; j < transfersPerThread; j++) {
                                if (forward) {
                                    service.transfer(accountA, accountB, transferAmount);
                                } else {
                                    service.transfer(accountB, accountA, transferAmount);
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            endGate.countDown();
                        }
                    });
        }

        startGate.countDown();
        boolean completedInTime = endGate.await(10, TimeUnit.SECONDS);

        transferExecutor.shutdown();
        asyncAuditPool.shutdown();

        assertThat(completedInTime)
                .as("Transfers must complete within deadline without deadlock")
                .isTrue();
        assertThat(accountA.getBalanceCents() + accountB.getBalanceCents())
                .as("Total balance across accounts must remain strictly constant")
                .isEqualTo(initialTotalBalance);
    }
}
