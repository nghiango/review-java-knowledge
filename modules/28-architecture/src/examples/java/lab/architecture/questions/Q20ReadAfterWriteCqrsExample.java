package lab.architecture.questions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Q20: Read-After-Write Consistency in CQRS Architectures.
 * Demonstrates version checking to ensure client doesn't see stale read projection.
 */
public class Q20ReadAfterWriteCqrsExample {

    public static class ReadModelWithVersion {
        private final Map<String, Long> projectionVersions = new ConcurrentHashMap<>();

        public void updateProjection(String id, long version) {
            projectionVersions.put(id, version);
        }

        public boolean isConsistentWithWrittenVersion(String id, long expectedVersion) {
            long current = projectionVersions.getOrDefault(id, 0L);
            return current >= expectedVersion;
        }
    }

    public static void main(String[] args) {
        ReadModelWithVersion readModel = new ReadModelWithVersion();
        readModel.updateProjection("ORD-1", 1L);

        boolean isStale = !readModel.isConsistentWithWrittenVersion("ORD-1", 2L); // true (client waits or polls)
        readModel.updateProjection("ORD-1", 2L);
        boolean isConsistentNow = readModel.isConsistentWithWrittenVersion("ORD-1", 2L); // true

        System.out.println("Q20 isStale: " + isStale + ", consistentNow: " + isConsistentNow);
    }
}
