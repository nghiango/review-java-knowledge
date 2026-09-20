package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q15: In Cache-Aside architecture, why is "Write to DB, then Delete Cache" preferred over "Write
 * to DB, then Update Cache"?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q15DualWriteConsistencyTradeoffs {

    public static void main(String[] args) {
        // Strategy A: Update DB, then Update Cache:
        // Race Condition:
        // Thread 1 writes DB (val=10).
        // Thread 2 writes DB (val=20).
        // Thread 2 writes Cache (val=20).
        // Thread 1 writes Cache (val=10).
        // DB holds 20, but Cache permanently holds 10 (Stale Data!).
        boolean updateCacheHasRaceCondition = true; // true

        // Strategy B: Update DB, then Delete Cache (Recommended Cache-Aside):
        // 1. Thread updates DB and commits.
        // 2. Thread deletes (evicts) the cache entry.
        // 3. Subsequent reader fetches fresh data from DB and repopulates cache.
        boolean deleteCacheEliminatesUpdateRace = true; // true

        // Strategy C: Delete Cache, then Update DB:
        // Fatal Race: Reader reads old DB value while writer is updating DB, then caches the old
        // value!
        Map<String, String> analysis =
                Map.of(
                        "Delete-Then-Update-DB",
                                "Vulnerable to stale write if concurrent read occurs during DB write",
                        "Update-DB-Then-Update-Cache",
                                "Vulnerable to concurrent writer race conditions",
                        "Update-DB-Then-Delete-Cache",
                                "Standard production pattern (minimal race probability)");

        boolean standardPatternIsUpdateDbThenDelete =
                analysis.get("Update-DB-Then-Delete-Cache").contains("Standard"); // true
    }
}
