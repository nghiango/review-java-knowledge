package lab.cachingredis.stampede;

import java.util.List;

public record LeaderboardEntry(
        String category, List<String> topPlayerUsernames, long calculatedAtEpochMs) {}
