package lab.concurrency.unboundedthreads;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("FutureReturnValueIgnored")
class BoundedJobProcessorTest {

    @Test
    @DisplayName("bounded pool accepts and processes jobs up to capacity")
    void boundedPool_processesJobs() throws Exception {
        AtomicInteger processed = new AtomicInteger(0);
        ReportGenerationWorker worker =
                new ReportGenerationWorker() {
                    @Override
                    public void generateReport(String reportId) {
                        processed.incrementAndGet();
                    }
                };

        ThreadPoolExecutor executor =
                CustomThreadPoolFactory.createBoundedPool(
                        "test-worker", 2, 4, 10, new ThreadPoolExecutor.CallerRunsPolicy());

        try (BoundedJobProcessor processor = new BoundedJobProcessor(worker, executor)) {
            Future<?> f1 = processor.submitJob("REP-1");
            Future<?> f2 = processor.submitJob("REP-2");

            f1.get(2, TimeUnit.SECONDS);
            f2.get(2, TimeUnit.SECONDS);

            assertThat(processed.get()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("graceful shutdown drains remaining tasks before terminating")
    void gracefulShutdown_drainsWork() throws Exception {
        AtomicInteger processed = new AtomicInteger(0);
        ReportGenerationWorker worker =
                new ReportGenerationWorker() {
                    @Override
                    public void generateReport(String reportId) {
                        processed.incrementAndGet();
                    }
                };

        ThreadPoolExecutor executor =
                CustomThreadPoolFactory.createBoundedPool(
                        "shutdown-test", 2, 2, 20, new ThreadPoolExecutor.CallerRunsPolicy());

        BoundedJobProcessor processor = new BoundedJobProcessor(worker, executor);
        for (int i = 0; i < 10; i++) {
            processor.submitJob("REP-" + i);
        }

        boolean clean = processor.gracefulShutdown(5, TimeUnit.SECONDS);

        assertThat(clean).isTrue();
        assertThat(executor.isTerminated()).isTrue();
        assertThat(processed.get()).isEqualTo(10);
    }
}
