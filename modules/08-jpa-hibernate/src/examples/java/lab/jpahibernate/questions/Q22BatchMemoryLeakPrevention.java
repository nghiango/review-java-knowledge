package lab.jpahibernate.questions;

public class Q22BatchMemoryLeakPrevention {

    public static void main(String[] args) {
        int totalRecords = 100_000;
        int batchSize = 50;

        int totalFlushes = totalRecords / batchSize; // 2000

        // In long-running batch job without clear(), 100k entities remain in L1 cache snapshot map
        // -> OOM.
        // Calling entityManager.flush() then entityManager.clear() every batchSize items empties L1
        // cache.
        boolean callingClearPreventsOOM = true; // true

        System.out.println("Total records: " + totalRecords); // Total records: 100000
        System.out.println("Batch size: " + batchSize); // Batch size: 50
        System.out.println("Total flushes: " + totalFlushes); // Total flushes: 2000
        System.out.println(
                "Calling clear() prevents OOM: "
                        + callingClearPreventsOOM); // Calling clear() prevents OOM: true
    }
}
