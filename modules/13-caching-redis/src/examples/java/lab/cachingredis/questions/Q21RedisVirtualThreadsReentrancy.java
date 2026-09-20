package lab.cachingredis.questions;

import java.util.Map;

/**
 * Q21: How do Lettuce and Jedis Redis drivers interact with Project Loom Java 21 Virtual Threads?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q21RedisVirtualThreadsReentrancy {

    public static void main(String[] args) {
        // Jedis:
        // Traditional synchronous blocking client.
        // Requires JedisPool. Each virtual thread takes a dedicated physical socket connection.
        // If 10,000 virtual threads run concurrently, a connection pool of 50-100 will bottleneck,
        // causing pool starvation and thread queueing.
        boolean jedisRequiresLargePoolOnVirtualThreads = true; // true

        // Lettuce (Spring Boot Default):
        // Built on Netty asynchronous event-driven I/O.
        // Shares a single thread-safe connection across unlimited concurrent threads via request
        // pipelining/multiplexing.
        // Virtual threads performing Lettuce sync() calls unmount cleanly from carrier threads
        // during network I/O.
        boolean lettuceMultiplexesSingleConnection = true; // true

        Map<String, String> comparison =
                Map.of(
                        "Jedis",
                                "Connection-per-thread model; connection pool contention under virtual threads",
                        "Lettuce",
                                "Non-blocking Netty multiplexing; naturally compatible with Virtual Threads");

        boolean lettucePreferredOnVirtualThreads =
                comparison.get("Lettuce").contains("naturally compatible"); // true
    }
}
