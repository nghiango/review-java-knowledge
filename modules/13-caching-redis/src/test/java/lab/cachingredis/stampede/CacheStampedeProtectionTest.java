package lab.cachingredis.stampede;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheStampedeProtectionTest {

    private HeavyAnalyticsService analyticsService;
    private SafeLeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        analyticsService = new HeavyAnalyticsService();
        leaderboardService = new SafeLeaderboardService(analyticsService);
    }

    @Test
    @DisplayName("Concurrent cache misses execute heavy recomputation exactly once")
    void concurrentRequests_executeComputationExactlyOnce() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);

        List<Callable<LeaderboardEntry>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(
                    () -> {
                        startGate.await();
                        return leaderboardService.getLeaderboard("pvp-global");
                    });
        }

        // Submit all tasks and unleash threads simultaneously
        List<Future<LeaderboardEntry>> futures = new ArrayList<>();
        for (Callable<LeaderboardEntry> task : tasks) {
            futures.add(executor.submit(task));
        }
        startGate.countDown();

        for (Future<LeaderboardEntry> future : futures) {
            LeaderboardEntry entry = future.get();
            assertThat(entry).isNotNull();
            assertThat(entry.category()).isEqualTo("pvp-global");
        }

        // Despite 20 concurrent requests hitting simultaneously, heavy analytics executed only
        // once!
        assertThat(analyticsService.getExecutionCount()).isEqualTo(1);

        executor.shutdown();
    }
}
