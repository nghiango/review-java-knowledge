package lab.cachingredis.broken.stampede;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Repository;

@Repository
public class HeavyAnalyticsRepository {

    private final AtomicInteger executionCount = new AtomicInteger();

    public LeaderboardEntry computeTopPlayers(String category) {
        executionCount.incrementAndGet();
        // Simulates an expensive database query aggregating millions of player match records
        return new LeaderboardEntry(
                category,
                List.of("player_ace", "player_blitz", "player_cyber"),
                System.currentTimeMillis());
    }

    public int getExecutionCount() {
        return executionCount.get();
    }
}
