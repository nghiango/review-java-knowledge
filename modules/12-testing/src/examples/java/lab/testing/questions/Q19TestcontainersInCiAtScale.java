package lab.testing.questions;

/**
 * Q19: Testcontainers in CI at scale.
 *
 * <p>Two decisions set the CI bill: how many containers the suite starts, and how the work is
 * sharded. A container per test class on a 24-class suite starts 24 PostgreSQL instances in one job
 * — 12 GB of memory on a 4 GB runner, which is an OOM kill rather than a slow build. One singleton
 * container per job plus eight shards starts eight containers in total and cuts the wall clock from
 * five minutes to under half a minute, because the shards run in parallel and each pays for exactly
 * one container. Ryuk reaps the leftovers, but it needs the Docker socket, so an agent that hides
 * the socket has to disable it and clean up per job instead.
 */
public class Q19TestcontainersInCiAtScale {

    public static void main(String[] args) {
        long testMillisPerClass = 5_000;
        long containerStartupMillis = 8_000;
        int memoryPerContainerMb = 512;

        int suiteClasses = 24;
        int shards = 8;
        int classesPerShard = suiteClasses / shards; // 3

        long singleJobMillis =
                suiteMillis(suiteClasses, suiteClasses, testMillisPerClass, containerStartupMillis);
        long shardedJobMillis =
                suiteMillis(classesPerShard, 1, testMillisPerClass, containerStartupMillis);
        long warmAgentMillis = classesPerShard * testMillisPerClass; // the container is reused
        long savedMillis = singleJobMillis - shardedJobMillis; // 289_000

        int singleJobContainers = suiteClasses; // one per test class
        int shardedContainers = shards; // one per shard
        long singleJobMemoryMb = (long) singleJobContainers * memoryPerContainerMb; // 12_288
        long shardedMemoryMb = (long) shardedContainers * memoryPerContainerMb; // 4_096
        boolean singleJobExceedsAFourGigRunner = singleJobMemoryMb > 4_096; // true

        System.out.println("Single job: " + singleJobMillis + " ms"); // Single job: 312000 ms
        System.out.println("Sharded job: " + shardedJobMillis + " ms"); // Sharded job: 23000 ms
        System.out.println("Saved: " + savedMillis + " ms"); // Saved: 289000 ms
        System.out.println("Warm agent: " + warmAgentMillis + " ms"); // Warm agent: 15000 ms
        System.out.println(
                "Containers (single): " + singleJobContainers); // Containers (single): 24
        System.out.println("Containers (sharded): " + shardedContainers); // Containers (sharded): 8
        System.out.println(
                "Single job memory: " + singleJobMemoryMb + " MB"); // Single job memory: 12288 MB
        System.out.println("Sharded memory: " + shardedMemoryMb + " MB"); // Sharded memory: 4096 MB
        System.out.println("Exceeds 4 GB: " + singleJobExceedsAFourGigRunner); // Exceeds 4 GB: true
    }

    /** Test time for {@code classes} plus {@code starts} container startups and migrations. */
    private static long suiteMillis(
            int classes, int starts, long testMillisPerClass, long containerStartupMillis) {
        return classes * testMillisPerClass + starts * containerStartupMillis;
    }
}
