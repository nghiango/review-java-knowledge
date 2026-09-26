package lab.jpahibernate.questions;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class Q29PersistenceContextBatchLeakScenarioExample {
    private Q29PersistenceContextBatchLeakScenarioExample() {}

    public static class PersistenceContextSimulator {
        private final Map<Long, String> firstLevelCache = new HashMap<>();

        public void loadEntity(Long id, String data) {
            firstLevelCache.put(id, data);
        }

        public void flushAndClear() {
            // em.flush(); synchronizes SQL to database socket
            // em.clear(); detaches all managed entities, evicting the entire 1st-level cache map!
            firstLevelCache.clear();
        }

        public int getCacheSize() {
            return firstLevelCache.size();
        }
    }

    public static void main(String[] args) {
        PersistenceContextSimulator em = new PersistenceContextSimulator();
        int batchSize = 50;

        for (long i = 1; i <= 200; i++) {
            em.loadEntity(i, "EntityData-" + i);

            if (i % batchSize == 0) {
                // Periodic flush and clear prevents OutOfMemoryError in batch processing
                em.flushAndClear();
            }
        }

        int finalCacheSize = em.getCacheSize(); // 0 (all entities successfully detached and freed for GC!)
        boolean memorySafe = (finalCacheSize == 0); // true
    }
}
