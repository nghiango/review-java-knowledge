package lab.cachingredis.broken.stampede;

import java.util.List;

public record LeaderboardEntry(String category, List<String> topPlayerUsernames, long calculatedAtEpochMs) {}
