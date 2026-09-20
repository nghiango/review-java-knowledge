package lab.resilience.questions;

public class Q21DistributedRateLimitingRedisVsResilience4jExample {

    record RateLimiterScope(String mechanism, boolean isClusterWide, long latencyCostUs) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Resilience4j RateLimiter: in-memory, per-JVM, sub-microsecond latency, does NOT
        // coordinate across replicas
        RateLimiterScope inMemory = new RateLimiterScope("Resilience4j", false, 1);

        // Redis Token Bucket / Sliding Window Log: cluster-wide shared state, introduces network
        // roundtrip (~1ms)
        RateLimiterScope distributed = new RateLimiterScope("Redis", true, 1000);

        boolean inMemoryIsLocal = !inMemory.isClusterWide(); // true
        boolean redisIsDistributed = distributed.isClusterWide(); // true

        System.out.println(
                "Resilience4j is local: "
                        + inMemoryIsLocal
                        + ", Redis is distributed: "
                        + redisIsDistributed);
    }
}
