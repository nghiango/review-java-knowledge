package lab.springmvc.questions;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public final class Q29TomcatWorkerExhaustionScenarioExample {
    private Q29TomcatWorkerExhaustionScenarioExample() {}

    public static void main(String[] args) {
        // Embedded Tomcat utilizes a bounded thread pool (default maxThreads = 200).
        // If incoming HTTP requests invoke slow, un-timed downstream services (e.g. 5-second
        // latency),
        // 200 concurrent requests completely saturate all worker threads!
        ThreadPoolExecutor tomcatSimulationPool =
                new ThreadPoolExecutor(
                        10, 10, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));

        int maxThreads = tomcatSimulationPool.getMaximumPoolSize(); // 10
        int activeThreads = tomcatSimulationPool.getActiveCount();

        // Mitigation:
        // 1. Configure explicit timeouts on all RestClient/WebClient HTTP calls (connectTimeout,
        // readTimeout)
        // 2. Enable virtual threads (spring.threads.virtual.enabled=true) in Java 21+
        // 3. Apply Bulkhead patterns to isolate downstream dependencies
        tomcatSimulationPool.shutdown();
    }
}
