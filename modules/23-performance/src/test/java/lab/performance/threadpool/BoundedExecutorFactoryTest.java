package lab.performance.threadpool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;

class BoundedExecutorFactoryTest {
    @Test
    void workerCount_blockingWorkload_accountsForWaitTime() {
        assertThat(new WorkloadProfile(4, 0.5, 20).workerCount()).isEqualTo(6);
    }

    @Test
    void execute_workersAndQueueAreSaturated_rejectsLoad() throws Exception {
        ThreadPoolExecutor executor =
                new BoundedExecutorFactory().create(new WorkloadProfile(1, 0, 1));
        CountDownLatch release = new CountDownLatch(1);
        try {
            executor.execute(() -> await(release));
            executor.execute(() -> await(release));
            assertThatThrownBy(() -> executor.execute(() -> {}))
                    .isInstanceOf(RejectedExecutionException.class);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
