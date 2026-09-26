package lab.testing.questions;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Q30: Production Incident: Concurrent test suite deadlocks due to shared mutable database
 * fixtures. How do you triage and resolve?
 */
public class Q30ParallelTestDeadlockSharedFixturesIncident {

    public static void main(String[] args) {
        // Vulnerable anti-pattern: Shared static primary key across parallel tests
        long sharedStaticId = 42L;

        // Solution 1: Dynamic unique test data builders with random UUIDs/sequences
        String testExecutionIdA = "test-run-" + java.util.UUID.randomUUID();
        String testExecutionIdB = "test-run-" + java.util.UUID.randomUUID();
        boolean uniqueTenantPerTest = !testExecutionIdA.equals(testExecutionIdB); // true

        // Solution 2: Lock-free per-thread fixture tracking
        Set<String> activeFixtures = ConcurrentHashMap.newKeySet();
        activeFixtures.add(testExecutionIdA);
        activeFixtures.add(testExecutionIdB);

        boolean parallelFixturesIsolated = (activeFixtures.size() == 2); // true

        System.out.println("Shared Static ID Hazard: " + sharedStaticId); // 42
        System.out.println("Unique Tenant IDs Prevent Collision: " + uniqueTenantPerTest); // true
        System.out.println("Parallel Fixtures Concurrently Isolated: " + parallelFixturesIsolated); // true
    }
}
