package lab.cachingredis.stampede;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class HeavyAnalyticsService {

    private final AtomicInteger executionCount = new AtomicInteger();

    public LeaderboardEntry computeTopPlayers(String category) {
        executionCount.incrementAndGet();
        try {
            // Simulate 50ms compute latency
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new LeaderboardEntry(
                category,
                List.of("player_alpha", "player_bravo", "player_charlie"),
                System.currentTimeMillis());
    }

    public int getExecutionCount() {
        return executionCount.get();
    }

    public void reset() {
        executionCount.set(0);
    }
}
