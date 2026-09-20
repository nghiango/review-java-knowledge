package lab.testsupport;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A single Redis container shared by every integration test in the JVM.
 *
 * <p>Starting a container per test class is the slowest part of a Docker-backed suite, so the
 * modules share one started instance instead. The container is started lazily on the first call to
 * {@link #instance()} and stopped by Testcontainers' Ryuk reaper when the JVM exits.
 *
 * <p>Because this class starts the container itself, a consumer must <strong>not</strong> annotate
 * it with {@code @Container}/{@code @Testcontainers}. Publish it from the test instead:
 *
 * <pre>{@code
 * @TestConfiguration(proxyBeanMethods = false)
 * class RedisConfiguration {
 *     @Bean
 *     @ServiceConnection(name = "redis")
 *     GenericContainer<?> redis() {
 *         return SharedRedisContainer.instance();
 *     }
 * }
 * }</pre>
 *
 * <p>The image is pinned to {@code redis:7.4-alpine}.
 */
public final class SharedRedisContainer {

    private static final String IMAGE = "redis:7.4-alpine";
    private static final int REDIS_PORT = 6379;

    private static GenericContainer<?> instance;

    private SharedRedisContainer() {}

    /**
     * Returns the shared, started Redis container, starting it on first use.
     *
     * <p>Synchronised so that parallel test execution cannot start two containers or observe a
     * half-started one.
     */
    public static synchronized GenericContainer<?> instance() {
        if (instance == null) {
            @SuppressWarnings("resource")
            GenericContainer<?> container =
                    new GenericContainer<>(DockerImageName.parse(IMAGE))
                            .withExposedPorts(REDIS_PORT);
            container.start();
            instance = container;
        }
        return instance;
    }
}
