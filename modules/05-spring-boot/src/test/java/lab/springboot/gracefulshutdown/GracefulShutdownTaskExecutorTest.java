package lab.springboot.gracefulshutdown;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class GracefulShutdownTaskExecutorTest {

    @Test
    @DisplayName("Task executor should be configured with wait-for-tasks and timeout")
    void executorConfiguration_hasGracefulShutdownSettings() {
        ShutdownConfig config = new ShutdownConfig();
        ThreadPoolTaskExecutor executor = config.gracefulTaskExecutor();

        try {
            assertThat(executor.getCorePoolSize()).isEqualTo(4);
            assertThat(executor.getThreadPoolExecutor()).isNotNull();

            GracefulTaskProcessor processor = new GracefulTaskProcessor(executor);
            CountDownLatch latch = new CountDownLatch(1);

            processor.submitJob(latch::countDown);

            boolean completed = false;
            try {
                completed = latch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertThat(completed).isTrue();
            assertThat(processor.getProcessedCount()).isEqualTo(1);
        } finally {
            executor.destroy();
        }
    }
}
