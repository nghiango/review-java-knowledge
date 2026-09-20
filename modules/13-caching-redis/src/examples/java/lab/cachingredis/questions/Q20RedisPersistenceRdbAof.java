package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q20: What are the differences and operational trade-offs between RDB snapshots and AOF
 * (Append-Only File) in Redis?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q20RedisPersistenceRdbAof {

    public static void main(String[] args) {
        // RDB (Redis Database Snapshot):
        // Point-in-time snapshot created via background fork() (BGSAVE).
        // Pros: Extremely compact single file; lightning fast restarts/recovery.
        // Cons: Risk of losing minutes of data between snapshots if server crashes; fork() memory
        // spike on huge heaps.
        boolean rdbFastRecovery = true; // true

        // AOF (Append-Only File):
        // Logs every write command received by the server.
        // Fsync policies:
        // - appendfsync always: Fsync after every write (maximum durability, lowest throughput)
        // - appendfsync everysec: Fsync every second (standard baseline: lose at most 1 second of
        // writes)
        // - appendfsync no: OS decides when to flush (fastest, higher data loss risk)
        boolean aofEverysecStandard = true; // true

        Map<String, String> tradeOffs =
                Map.of(
                        "RDB", "Compact snapshots for backups and disaster recovery",
                        "AOF",
                                "Granular write journal with minimal data loss (appendfsync everysec)",
                        "Hybrid",
                                "Redis 4.0+ combines RDB preamble with AOF tail for fast rewrite and durability");

        boolean hybridAvailable = tradeOffs.containsKey("Hybrid"); // true
    }
}
