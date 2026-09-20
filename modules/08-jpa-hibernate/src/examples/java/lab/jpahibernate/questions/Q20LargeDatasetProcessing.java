package lab.jpahibernate.questions;

public class Q20LargeDatasetProcessing {

    public static void main(String[] args) {
        // Standard EntityManager loads all entities into 1st-level cache, causing OutOfMemoryError
        // for 1,000,000 records.
        // Solution 1: Periodic em.flush() and em.clear() every N items (e.g. 50 items).
        // Solution 2: Hibernate StatelessSession (bypasses L1 cache, dirty checking, cascades, and
        // interceptors entirely).
        boolean statelessSessionHasNoL1Cache = true; // true
        boolean statelessSessionHasNoDirtyChecking = true; // true

        int recommendedChunkSize = 50; // 50

        System.out.println(
                "StatelessSession bypasses L1 cache: "
                        + statelessSessionHasNoL1Cache); // StatelessSession bypasses L1 cache: true
        System.out.println(
                "StatelessSession bypasses dirty checking: "
                        + statelessSessionHasNoDirtyChecking); // StatelessSession bypasses dirty
        // checking: true
        System.out.println(
                "Recommended chunk size: " + recommendedChunkSize); // Recommended chunk size: 50
    }
}
