package lab.jpahibernate.questions;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class Q25SecondLevelCacheRegionsExample {
    private Q25SecondLevelCacheRegionsExample() {}

    // Hibernate 2nd Level Cache (L2C) partitions into 4 distinct cache regions:
    // 1. Entity Cache: Stores dehydrated entity state (array of disassembled property values) keyed
    // by @Id
    // 2. Collection Cache: Stores list of foreign key @Id values for associations, NOT full
    // entities
    // 3. NaturalId Cache: Maps business key / natural ID values to primary keys (@Id)
    // 4. Query Cache: Stores query parameter hash -> list of matching @Id results (requires Entity
    // cache to rehydrate)
    public static class CacheRegionsSimulator {
        private final Map<Long, Object[]> entityRegion = new HashMap<>();
        private final Map<String, Long[]> queryRegion = new HashMap<>();
        private long tableModificationTimestamp = 0L;

        public void putEntity(Long id, Object[] dehydratedState) {
            entityRegion.put(id, dehydratedState);
        }

        public void putQuery(String queryHash, Long[] ids) {
            queryRegion.put(queryHash, ids);
            this.tableModificationTimestamp = System.currentTimeMillis();
        }

        public boolean isQueryCacheValid(long cachedTimestamp) {
            // Any INSERT, UPDATE, or DELETE on the underlying table increments
            // tableModificationTimestamp,
            // immediately invalidating ALL query cache results referencing that entity table!
            return cachedTimestamp >= tableModificationTimestamp;
        }
    }

    public static void main(String[] args) {
        CacheRegionsSimulator cache = new CacheRegionsSimulator();
        cache.putEntity(101L, new Object[] {"Alice", "VIP"});
        cache.putQuery("SELECT u.id FROM User u WHERE u.tier='VIP'", new Long[] {101L});

        boolean valid = cache.isQueryCacheValid(System.currentTimeMillis() + 10); // true
    }
}
