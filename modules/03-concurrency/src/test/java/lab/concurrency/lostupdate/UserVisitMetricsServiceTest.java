package lab.concurrency.lostupdate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("FutureReturnValueIgnored")
class UserVisitMetricsServiceTest {

    @Test
    @DisplayName(
            "concurrent increments across multiple threads preserve total hits without lost updates")
    void concurrentIncrements_preserveExactCount() throws InterruptedException {
        UserVisitMetricsService service = new UserVisitMetricsService();
        int threadCount = 16;
        int incrementsPerThread = 1_000;
        int totalExpectedHits = threadCount * incrementsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String userId = "user-" + (i % 4);
            executor.submit(
                    () -> {
                        try {
                            startGate.await();
                            for (int j = 0; j < incrementsPerThread; j++) {
                                service.recordUserHit(userId);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            endGate.countDown();
                        }
                    });
        }

        startGate.countDown();
        boolean finished = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(service.getTotalHits()).isEqualTo(totalExpectedHits);
        assertThat(service.getUserHits("user-0")).isEqualTo((totalExpectedHits / 4));
    }

    @Test
    @DisplayName("shutdown state stops recording hits immediately")
    void shutdown_preventsNewHits() {
        UserVisitMetricsService service = new UserVisitMetricsService();
        service.recordUserHit("user-1");
        assertThat(service.getTotalHits()).isEqualTo(1L);

        service.shutdown();
        boolean recorded = service.recordUserHit("user-1");

        assertThat(recorded).isFalse();
        assertThat(service.getTotalHits()).isEqualTo(1L);
        assertThat(service.isActive()).isFalse();
    }
}
